package ru.fo6osik.workjournal.documents.sources

import java.io.InputStream

/** Publisher metadata for one PDF/index pair, not a Drive file version or MD5. */
data class GoogleDrivePairMetadata(
    val pdfFileId: String,
    val searchIndexFileId: String,
    val revision: String?,
    val pdfSha256: String?,
    val searchIndexSha256: String?
)

/**
 * Blocking transport boundary. The source calls it only on a worker thread.
 * Implementations must be thread-safe, use bounded network timeouts, and return
 * streaming bodies without buffering entire PDFs. Ownership transfers to the
 * caller; closing the stream must release the response/connection.
 *
 * Metadata must come from a trusted pair manifest. Never derive SHA-256 from
 * downloaded bytes, Drive MD5, or a file version; unknown values stay null.
 * Missing files throw FileNotFoundException, network failures throw IOException.
 * This stage supplies no HTTP client, SDK, authorization, or credentials.
 */
interface GoogleDriveTransport {
    fun metadata(pdfFileId: String, searchIndexFileId: String): GoogleDrivePairMetadata
    fun openFile(fileId: String): InputStream
}
