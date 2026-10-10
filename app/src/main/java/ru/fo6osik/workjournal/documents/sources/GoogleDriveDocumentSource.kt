package ru.fo6osik.workjournal.documents.sources

import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fo6osik.workjournal.documents.DocumentDescriptor
import ru.fo6osik.workjournal.documents.DocumentSource as Location
import java.io.FilterInputStream
import java.io.InputStream

/** Opt-in adapter: no SDK, OAuth, catalog edits, or automatic registration. */
class GoogleDriveDocumentSource internal constructor(
    private val transport: GoogleDriveTransport,
    private val checkWorkerThread: () -> Unit
) : DocumentSource {
    constructor(transport: GoogleDriveTransport) : this(transport, {
        check(Thread.currentThread() !== Looper.getMainLooper().thread) {
            "Google Drive streams must be opened, read, and closed off the main thread"
        }
    })

    override fun supports(location: Location) = location is Location.GoogleDrive

    override suspend fun metadata(document: DocumentDescriptor, location: Location): SourceMetadata {
        val drive = driveLocation(location)
        require(drive in document.sources) { "Drive location does not belong to document" }
        return withContext(Dispatchers.IO) {
            checkWorkerThread()
            val pair = transport.metadata(drive.pdfFileId, drive.searchIndexFileId)
            require(pair.pdfFileId == drive.pdfFileId && pair.searchIndexFileId == drive.searchIndexFileId) {
                "Drive metadata describes a different PDF/index pair"
            }
            require(pair.revision == null || pair.revision.isNotBlank()) { "Empty pair revision" }
            listOf(pair.pdfSha256, pair.searchIndexSha256).forEach { hash ->
                require(hash == null || Regex("[0-9a-f]{64}").matches(hash)) { "Invalid pair SHA-256" }
            }
            require(pair.revision == null || pair.pdfSha256 != null && pair.searchIndexSha256 != null) {
                "A declared pair revision requires both expected SHA-256 values"
            }
            SourceMetadata(pair.revision, pair.pdfSha256, pair.searchIndexSha256)
        }
    }

    override fun openPdf(location: Location): InputStream = open(driveLocation(location).pdfFileId)

    override fun openSearchIndex(location: Location): InputStream = open(driveLocation(location).searchIndexFileId)

    private fun driveLocation(location: Location): Location.GoogleDrive {
        require(location is Location.GoogleDrive) { "Expected Google Drive location" }
        val ids = Regex("[A-Za-z0-9_-]+")
        require(ids.matches(location.pdfFileId) && ids.matches(location.searchIndexFileId)) { "Expected Drive file IDs" }
        require(location.pdfFileId != location.searchIndexFileId) { "PDF and index must be separate files" }
        return location
    }

    private fun open(fileId: String): InputStream {
        checkWorkerThread() // Reject before the transport can connect on the main thread.
        return object : FilterInputStream(transport.openFile(fileId)) {
            override fun read(): Int { checkWorkerThread(); return `in`.read() }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                checkWorkerThread()
                return `in`.read(buffer, offset, length)
            }
            override fun skip(count: Long): Long { checkWorkerThread(); return `in`.skip(count) }
            override fun available(): Int { checkWorkerThread(); return `in`.available() }
            override fun mark(limit: Int) { checkWorkerThread(); `in`.mark(limit) }
            override fun reset() { checkWorkerThread(); `in`.reset() }
            override fun markSupported(): Boolean { checkWorkerThread(); return `in`.markSupported() }
            override fun close() { checkWorkerThread(); `in`.close() }
        }
    }
}
