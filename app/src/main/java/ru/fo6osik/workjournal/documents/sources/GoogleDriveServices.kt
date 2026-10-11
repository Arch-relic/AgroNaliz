package ru.fo6osik.workjournal.documents.sources

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.fo6osik.workjournal.documents.*
import java.io.IOException

/** Process-wide opt-in composition. Binding writes and UI synchronization are serialized. */
internal class GoogleDriveServices private constructor(private val context: Context) {
    companion object {
        @Volatile private var instance: GoogleDriveServices? = null
        fun get(context: Context): GoogleDriveServices = instance ?: synchronized(this) {
            instance ?: GoogleDriveServices(context.applicationContext).also { instance = it }
        }
    }
    val authorization by lazy { GoogleDriveNativeAuthorization(context) }
    private val storage = GoogleDriveCredentialStore(context)
    private val operations = Mutex()
    @Volatile private var loaded = false
    @Volatile var state = DrivePrivateState(); private set
    @Volatile private var generation = 0L
    private val session = GoogleDriveTokenSession(object : DriveAuthorizationBackend {
        override fun authorize(account: String) = authorization.authorize(account)
        override fun clearToken(token: String) = authorization.clearToken(token)
        override fun revoke(account: String) = authorization.revoke(account)
    }, { state.account }, SystemClock::elapsedRealtime)
    private fun save(value: DrivePrivateState) { storage.write(value); state = value }
    suspend fun initialize() = withContext(Dispatchers.IO) {
        operations.withLock {
            if (!loaded) {
                val value = storage.read()
                DriveSelection.apply(DocumentCatalog.load(context.assets), value.bindings)
                state = value
                loaded = true
            }
        }
    }
    suspend fun connect(account: String, grant: DriveGrant) = withContext(Dispatchers.IO) {
        operations.withLock {
            check(loaded)
            val old = state.account
            require(state.pendingRevoke == null && (old == null || old == account)) { "Disconnect the previous account first" }
            val next = if (old == account) state.copy(pendingRevoke = null) else DrivePrivateState(account = account)
            if (old != account) generation++
            save(next.copy(account = account))
            session.accept(account, grant)
        }
    }
    suspend fun disconnect(): Boolean = withContext(Dispatchers.IO) {
        operations.withLock {
            val old = state.account ?: state.pendingRevoke
            generation++
            save(DrivePrivateState(pendingRevoke = old)) // Local disable first, even offline.
            try {
                session.disconnect(old)
                save(DrivePrivateState())
                true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { false } // Persist pending revoke; UI explains retry/manual console revoke.
        }
    }
    suspend fun resetInvalidSettings() = withContext(Dispatchers.IO) {
        operations.withLock {
            generation++
            state = DrivePrivateState()
            storage.clear()
            loaded = true
        }
    }
    private fun transport(catalog: DocumentCatalogData, bindings: List<DriveBinding>): GoogleDriveHttpTransport {
        val account = state.account ?: throw DriveAuthorizationRequired()
        val epoch = generation
        val known = catalog.documents.associateBy { it.documentId }
        return GoogleDriveHttpTransport(GoogleDriveHttpsClient(), session, bindings.map { known.getValue(it.documentId) to it }, {
            if (state.account != account || generation != epoch) throw DriveAuthorizationRequired()
        })
    }
    private fun repository(catalog: DocumentCatalogData, bindings: List<DriveBinding>): DocumentRepository {
        val adapters = mutableListOf<DocumentSource>(AssetsDocumentSource(context.assets))
        if (state.account != null && bindings.isNotEmpty()) adapters.add(GoogleDriveDocumentSource(transport(catalog, bindings)))
        return DefaultDocumentRepository(DriveSelection.apply(catalog, if (state.account == null) emptyList() else bindings),
            LocalDocumentStore(context), adapters)
    }
    suspend fun openCached(documentId: String): LocalDocument {
        val document = withContext(Dispatchers.IO) {
            DocumentCatalog.load(context.assets).documents.single { it.documentId == documentId }
        }
        val local = LocalDocumentStore(context).acquire(documentId) ?: throw IOException("Document is not downloaded")
        if (local.pdfSha256 != document.pdfSha256 || local.searchIndexSha256 != document.searchIndexSha256 || local.revision != document.revision) {
            local.close()
            throw IOException("Cached document no longer matches the trusted catalog")
        }
        return local
    }
    suspend fun repository(): DocumentRepository {
        initialize()
        return withContext(Dispatchers.IO) { operations.withLock { repository(DocumentCatalog.load(context.assets), state.bindings) } }
    }
    suspend fun downloadSelection(binding: DriveBinding, expectedAccount: String) = withContext(Dispatchers.IO) {
        operations.withLock {
            require(state.account == expectedAccount && state.pendingRevoke == null) { "Account changed" }
            val catalog = DocumentCatalog.load(context.assets)
            val bindings = state.bindings.filterNot { it.documentId == binding.documentId } + binding
            DriveSelection.apply(catalog, bindings)
            val transport = transport(catalog, bindings)
            val metadata = transport.inspect(binding.pdfId, binding.indexId)
            val required = metadata.first.size + metadata.second.size
            if (context.noBackupFilesDir.usableSpace < required + 8L * 1024 * 1024) throw IOException("Insufficient document storage")
            val repository = DefaultDocumentRepository(DriveSelection.apply(catalog, bindings), LocalDocumentStore(context),
                listOf(AssetsDocumentSource(context.assets), GoogleDriveDocumentSource(transport)))
            repository.download(binding.documentId, forceRefresh = true).close()
            currentCoroutineContext().ensureActive()
            val downloaded = transport.inspected(binding.pdfId, binding.indexId)
            val validated = binding.copy(pdfVersion = downloaded.first.version, indexVersion = downloaded.second.version,
                lastUpdated = System.currentTimeMillis())
            save(state.copy(bindings = bindings.map { if (it.documentId == binding.documentId) validated else it }))
        }
    }
    suspend fun checkUpdates(documentId: String): Boolean = withContext(Dispatchers.IO) {
        operations.withLock {
            val binding = state.bindings.singleOrNull { it.documentId == documentId } ?: throw IOException("Select document files first")
            val catalog = DocumentCatalog.load(context.assets)
            val remote = transport(catalog, state.bindings).inspect(binding.pdfId, binding.indexId)
            remote.first.version != binding.pdfVersion || remote.second.version != binding.indexVersion
        }
    }
    suspend fun synchronize(documentId: String) {
        val binding = state.bindings.singleOrNull { it.documentId == documentId } ?: throw IOException("Select document files first")
        downloadSelection(binding, state.account ?: throw DriveAuthorizationRequired())
    }
}
