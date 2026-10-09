package ru.fo6osik.workjournal.documents

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import ru.fo6osik.workjournal.documents.sources.DocumentSource as Adapter
import ru.fo6osik.workjournal.documents.sources.SourceMetadata
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class DefaultDocumentRepository(
    catalog: DocumentCatalogData,
    private val store: LocalDocumentStore,
    private val sources: List<Adapter>
) : DocumentRepository {
    init { DocumentCatalog.validate(catalog) }
    private val documents = catalog.documents.associateBy { it.documentId }
    private val states = ConcurrentHashMap<String, MutableStateFlow<DocumentState>>()
    companion object { private val locks = ConcurrentHashMap<String, Mutex>() }

    override fun observeState(documentId: String): StateFlow<DocumentState> {
        descriptor(documentId)
        return state(documentId).asStateFlow()
    }

    override suspend fun open(documentId: String): LocalDocument = operation(documentId) {
        store.acquire(documentId)?.also { state(documentId).value = DocumentState.ReadyOffline(it.revision) }
            ?: fetch(descriptor(documentId))
    }

    override suspend fun download(documentId: String): LocalDocument = operation(documentId) {
        fetch(descriptor(documentId))
    }

    override suspend fun checkForUpdate(documentId: String): UpdateStatus = operation(documentId) {
        val document = descriptor(documentId)
        val (adapter, location) = selectSource(document)
        val local = store.acquire(documentId)
        if (local == null) {
            state(documentId).value = DocumentState.Missing
            UpdateStatus.UNKNOWN
        } else local.use {
            val metadata = adapter.metadata(document, location)
            val target = effectiveDescriptor(document, metadata)
            val changed = target.revision?.let { it != local.revision } == true ||
                target.pdfSha256?.let { it != local.pdfSha256 } == true ||
                target.searchIndexSha256?.let { it != local.searchIndexSha256 } == true
            if (changed) {
                if (target.revision != null && target.revision == local.revision) throw IOException("Source changed content without changing revision")
                state(documentId).value = DocumentState.UpdateAvailable(local.revision, target.revision)
                UpdateStatus.AVAILABLE
            } else {
                state(documentId).value = DocumentState.ReadyOffline(local.revision)
                if (target.pdfSha256 != null && target.searchIndexSha256 != null) UpdateStatus.CURRENT else UpdateStatus.UNKNOWN
            }
        }
    }

    private suspend fun fetch(document: DocumentDescriptor): LocalDocument {
        state(document.documentId).value = DocumentState.Loading
        val (adapter, location) = selectSource(document)
        val target = effectiveDescriptor(document, adapter.metadata(document, location))
        // A declared revision must identify one immutable pair, including across downloads.
        val previousCopy = try { store.acquire(document.documentId) } catch (_: IOException) { null }
        previousCopy?.use { previous ->
            if (target.revision != null && target.revision == previous.revision &&
                (target.pdfSha256 != previous.pdfSha256 || target.searchIndexSha256 != previous.searchIndexSha256)) {
                throw IOException("Source changed content without changing revision")
            }
        }
        return store.install(target, { adapter.openPdf(location) }, { adapter.openSearchIndex(location) }).also {
            state(document.documentId).value = DocumentState.ReadyOffline(it.revision)
        }
    }

    private fun effectiveDescriptor(document: DocumentDescriptor, metadata: SourceMetadata): DocumentDescriptor {
        val differentRevision = metadata.revision != null && metadata.revision != document.revision
        if (!differentRevision) {
            require(document.pdfSha256 == null || metadata.pdfSha256 == null || document.pdfSha256 == metadata.pdfSha256) { "Catalog/source PDF checksum conflict" }
            require(document.searchIndexSha256 == null || metadata.searchIndexSha256 == null || document.searchIndexSha256 == metadata.searchIndexSha256) { "Catalog/source index checksum conflict" }
        }
        val target = document.copy(
            revision = metadata.revision ?: document.revision,
            pdfSha256 = metadata.pdfSha256 ?: document.pdfSha256.takeUnless { differentRevision },
            searchIndexSha256 = metadata.searchIndexSha256 ?: document.searchIndexSha256.takeUnless { differentRevision }
        )
        DocumentCatalog.validate(DocumentCatalogData(DocumentCatalog.SCHEMA_VERSION, listOf(target)))
        require(target.revision == null || target.pdfSha256 != null && target.searchIndexSha256 != null) { "A known revision requires both checksums" }
        return target
    }

    private fun selectSource(document: DocumentDescriptor): Pair<Adapter, DocumentSource> {
        document.sources.forEach { location -> sources.firstOrNull { it.supports(location) }?.let { return it to location } }
        throw UnsupportedDocumentSourceException(document.documentId)
    }
    private fun descriptor(id: String) = documents[id] ?: throw IllegalArgumentException("Unknown documentId: $id")
    private fun state(id: String) = states.getOrPut(id) { MutableStateFlow<DocumentState>(DocumentState.Unchecked) }

    private suspend fun <T> operation(id: String, block: suspend () -> T): T {
        var lease: LocalDocument? = null
        try {
            return withContext(Dispatchers.IO) {
                descriptor(id)
                val mutex = locks.getOrPut(store.storageKey + ":" + id) { Mutex() }
                if (!mutex.tryLock()) throw DocumentBusyException(id)
                try {
                    block().also { if (it is LocalDocument) lease = it }
                } catch (cancelled: CancellationException) {
                    withContext(NonCancellable) {
                        val previous = try { store.acquire(id) } catch (_: Exception) { null }
                        previous?.use { state(id).value = DocumentState.ReadyOffline(it.revision) }
                            ?: run { state(id).value = DocumentState.Missing }
                    }
                    throw cancelled
                } catch (error: Exception) {
                    val previous = try { store.acquire(id)?.use { true } ?: false } catch (_: Exception) { false }
                    state(id).value = DocumentState.Error(error, previous)
                    throw error
                } finally {
                    mutex.unlock()
                }
            }
        } catch (cancelled: CancellationException) {
            lease?.close()
            throw cancelled
        }
    }
}
