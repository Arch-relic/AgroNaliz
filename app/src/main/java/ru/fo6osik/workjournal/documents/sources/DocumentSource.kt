package ru.fo6osik.workjournal.documents.sources

import ru.fo6osik.workjournal.documents.DocumentDescriptor
import ru.fo6osik.workjournal.documents.DocumentSource as Location
import java.io.InputStream

/** Immutable metadata for one PDF/index pair. Revision is opaque, never ordered as a number. */
data class SourceMetadata(val revision: String?, val pdfSha256: String?, val searchIndexSha256: String?)

/** Suspend operations are called on Dispatchers.IO. Streams are owned and closed by the store. */
interface DocumentSource {
    fun supports(location: Location): Boolean
    suspend fun metadata(document: DocumentDescriptor, location: Location): SourceMetadata
    fun openPdf(location: Location): InputStream
    fun openSearchIndex(location: Location): InputStream
}
