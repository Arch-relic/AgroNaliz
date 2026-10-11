package ru.fo6osik.workjournal.documents.sources

import ru.fo6osik.workjournal.documents.DocumentCatalog
import ru.fo6osik.workjournal.documents.DocumentCatalogData
import ru.fo6osik.workjournal.documents.DocumentSource

internal data class DriveBinding(
    val documentId: String, val pdfId: String, val indexId: String,
    val pdfVersion: String? = null, val indexVersion: String? = null, val lastUpdated: Long? = null
)
internal data class DrivePrivateState(
    val account: String? = null, val pendingRevoke: String? = null, val bindings: List<DriveBinding> = emptyList()
)
internal object DriveSelection {
    fun fileId(id: String): String {
        require(id.length in 1..256 && Regex("[A-Za-z0-9_-]+").matches(id)) { "Invalid selected file ID" }
        return id
    }
    fun pickedFile(value: String?): String {
        require(value != null && !value.contains(',')) { "Select exactly one file" }
        return fileId(value)
    }
    fun apply(catalog: DocumentCatalogData, bindings: List<DriveBinding>): DocumentCatalogData {
        require(bindings.map { it.documentId }.distinct().size == bindings.size) { "Duplicate document binding" }
        val known = catalog.documents.associateBy { it.documentId }
        val allIds = mutableSetOf<String>()
        bindings.forEach {
            val document = known[it.documentId] ?: throw IllegalArgumentException("Unknown document")
            require(document.pdfSha256 != null && document.searchIndexSha256 != null) { "Trusted catalog checksums required" }
            require(fileId(it.pdfId) != fileId(it.indexId) && allIds.add(it.pdfId) && allIds.add(it.indexId)) { "Duplicate selected file" }
        }
        val bound = bindings.associateBy { it.documentId }
        return catalog.copy(documents = catalog.documents.map { document ->
            bound[document.documentId]?.let { binding ->
                document.copy(sources = listOf(DocumentSource.GoogleDrive(binding.pdfId, binding.indexId)) +
                    document.sources.filterNot { it is DocumentSource.GoogleDrive })
            } ?: document
        }).also(DocumentCatalog::validate)
    }
}

/** Local gate for SDK PendingIntent results: one consumption, TTL, no exported callback. */
internal class DriveAuthorizationAttempt<T>(
    private val elapsedMillis: () -> Long, private val ttlMillis: Long = 300_000L
) {
    private var value: T? = null
    private var started = 0L
    @Synchronized fun begin(request: T) {
        check(value == null) { "Authorization already pending" }
        value = request
        started = elapsedMillis()
    }
    @Synchronized fun consume(): T {
        val pending = value ?: throw DriveAuthorizationRequired()
        value = null
        val now = elapsedMillis()
        val age = now - started
        if (now < started || age !in 0..ttlMillis) throw DriveAuthorizationRequired()
        return pending
    }
    @Synchronized fun cancel() { value = null }
    @Synchronized fun isPending() = value != null
}
