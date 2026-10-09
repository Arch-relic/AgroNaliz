package ru.fo6osik.workjournal.documents

/** Provider locations contain identifiers, never credentials or public download URLs. */
sealed class DocumentSource {
    data class Assets(val pdfPath: String, val searchIndexPath: String) : DocumentSource()
    data class AndroidSaf(val pdfUri: String, val searchIndexUri: String) : DocumentSource()
    data class GoogleDrive(val pdfFileId: String, val searchIndexFileId: String) : DocumentSource()
    data class FirebaseStorage(
        val bucket: String,
        val pdfObjectPath: String,
        val searchIndexObjectPath: String
    ) : DocumentSource()
}

/** Null revision/hash means unknown, not an empty value or an invented version. */
data class DocumentDescriptor(
    val documentId: String,
    val title: String,
    val revision: String?,
    val pdfSha256: String?,
    val searchIndexSha256: String?,
    val sources: List<DocumentSource>
) {
    val assetsSource: DocumentSource.Assets?
        get() = sources.filterIsInstance<DocumentSource.Assets>().singleOrNull()
}

/** schemaVersion describes the catalog format; it is not a document revision. */
data class DocumentCatalogData(
    val schemaVersion: Int,
    val documents: List<DocumentDescriptor>
)
