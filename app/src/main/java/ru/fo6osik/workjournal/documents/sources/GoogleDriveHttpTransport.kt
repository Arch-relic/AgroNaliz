package ru.fo6osik.workjournal.documents.sources

import org.json.JSONObject
import ru.fo6osik.workjournal.documents.DocumentDescriptor
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.FileNotFoundException
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

internal class DriveAccessDenied : IOException("Google Drive access denied")
internal class DriveRemoteConflict : IOException("Google Drive file changed")
internal interface DriveHttpClient {
    fun get(fileId: String, media: Boolean, token: String, etag: String?): DriveHttpResponse
}
internal class DriveHttpResponse(
    val status: Int, val body: InputStream, val etag: String? = null,
    val contentLength: Long? = null, private val release: () -> Unit = {}
) : Closeable {
    private var closed = false
    @Synchronized override fun close() {
        if (!closed) { closed = true; try { body.close() } finally { release() } }
    }
}

/** Fixed HTTPS API origin, no redirects, bounded timeouts, no listing endpoint. */
internal class GoogleDriveHttpsClient : DriveHttpClient {
    override fun get(fileId: String, media: Boolean, token: String, etag: String?): DriveHttpResponse {
        DriveSelection.fileId(fileId)
        val query = if (media) "alt=media&supportsAllDrives=true" else
            "fields=id,mimeType,size,version,trashed,capabilities(canDownload)&supportsAllDrives=true"
        val connection = URL("https://www.googleapis.com/drive/v3/files/$fileId?$query").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept-Encoding", "identity")
            etag?.let { connection.setRequestProperty("If-Match", it) }
            val status = connection.responseCode
            val stream = if (status == 200) connection.inputStream else connection.errorStream ?: java.io.ByteArrayInputStream(byteArrayOf())
            return DriveHttpResponse(status, stream, connection.getHeaderField("ETag"),
                connection.getHeaderField("Content-Length")?.toLongOrNull(), connection::disconnect)
        } catch (_: IOException) {
            connection.disconnect()
            throw IOException("Google Drive network request failed")
        }
    }
}


internal data class DriveFileMetadata(val id: String, val mimeType: String, val size: Long, val version: String, val etag: String?)

/** Hash trust comes only from the bundled catalog, never from remote metadata. */
internal class GoogleDriveHttpTransport(
    private val client: DriveHttpClient, private val tokens: DriveTokenProvider,
    documents: List<Pair<DocumentDescriptor, DriveBinding>>,
    private val assertAccountActive: () -> Unit = {},
    private val decodeMetadata: (ByteArray, String?) -> DriveFileMetadata = ::parseDriveMetadata
) : GoogleDriveTransport {
    private data class PairSpec(val document: DocumentDescriptor, val binding: DriveBinding)
    private val specs = documents.map { PairSpec(it.first, it.second) }
    private val files = ConcurrentHashMap<String, DriveFileMetadata>()
    init {
        require(specs.isNotEmpty()) { "No selected Drive files" }
        val ids = mutableSetOf<String>()
        specs.forEach {
            require(it.document.pdfSha256 != null && it.document.searchIndexSha256 != null) { "Trusted catalog checksums required" }
            require(it.binding.documentId == it.document.documentId) { "Binding/document mismatch" }
            require(ids.add(DriveSelection.fileId(it.binding.pdfId)) && ids.add(DriveSelection.fileId(it.binding.indexId))) { "Duplicate selected file" }
        }
    }
    fun inspect(pdfId: String, indexId: String): Pair<DriveFileMetadata, DriveFileMetadata> {
        val spec = specs.singleOrNull { it.binding.pdfId == pdfId && it.binding.indexId == indexId }
            ?: throw DriveAccessDenied()
        val pdf = inspectFile(spec.binding.pdfId, true)
        val index = inspectFile(spec.binding.indexId, false)
        return pdf to index
    }
    fun inspected(pdfId: String, indexId: String): Pair<DriveFileMetadata, DriveFileMetadata> =
        (files[pdfId] ?: throw IOException("Missing PDF metadata")) to
            (files[indexId] ?: throw IOException("Missing index metadata"))
    override fun metadata(pdfFileId: String, searchIndexFileId: String): GoogleDrivePairMetadata {
        val spec = specs.singleOrNull { it.binding.pdfId == pdfFileId && it.binding.indexId == searchIndexFileId }
            ?: throw DriveAccessDenied()
        inspect(pdfFileId, searchIndexFileId)
        return GoogleDrivePairMetadata(pdfFileId, searchIndexFileId, spec.document.revision,
            spec.document.pdfSha256, spec.document.searchIndexSha256)
    }
    private fun inspectFile(id: String, pdf: Boolean): DriveFileMetadata {
        val value = request(id, false).use { response ->
            decodeMetadata(readBounded(response.body, 128 * 1024), response.etag)
        }
        require(value.id == id && value.size in 1..(if (pdf) 256L * 1024 * 1024 else 64L * 1024 * 1024)) { "Unexpected Drive file metadata" }
        require(if (pdf) value.mimeType == "application/pdf" else value.mimeType in setOf("application/json", "text/json", "text/plain", "application/octet-stream")) { "Unexpected Drive file type" }
        files[id] = value
        return value
    }
    override fun openFile(fileId: String): InputStream {
        if (specs.none { it.binding.pdfId == fileId || it.binding.indexId == fileId }) throw DriveAccessDenied()
        val expected = files[fileId] ?: throw IOException("Metadata must precede download")
        val response = request(fileId, true, expected.etag)
        if (response.contentLength != null && response.contentLength != expected.size) {
            response.close(); throw DriveRemoteConflict()
        }
        return object : FilterInputStream(response.body) {
            private var count = 0L
            private var closed = false
            override fun read(): Int {
                val single = ByteArray(1)
                return if (read(single, 0, 1) < 0) -1 else single[0].toInt() and 255
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                try {
                    assertAccountActive()
                    val read = `in`.read(buffer, offset, length)
                    if (read < 0 && count != expected.size) throw IOException("Truncated Drive download")
                    if (read > 0) {
                        count += read
                        if (count > expected.size) throw IOException("Oversized Drive download")
                    }
                    return read
                } catch (error: Exception) {
                    try { close() } catch (closing: Exception) { error.addSuppressed(closing) }
                    throw error
                }
            }
            override fun skip(n: Long): Long {
                val buffer = ByteArray(8192)
                var left = n.coerceAtLeast(0)
                while (left > 0) {
                    val read = read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                    if (read < 0) break
                    left -= read
                }
                return n.coerceAtLeast(0) - left
            }
            override fun markSupported() = false
            override fun reset(): Unit = throw IOException("Drive stream cannot reset")
            override fun close() { if (!closed) { closed = true; response.close() } }
        }
    }
    private fun request(id: String, media: Boolean, etag: String? = null): DriveHttpResponse {
        repeat(2) { attempt ->
            assertAccountActive()
            val token = tokens.accessToken()
            val response = client.get(id, media, token, etag)
            if (response.status == 200) return response
            response.close()
            when (response.status) {
                401 -> { tokens.invalidate(token); if (attempt == 1) throw DriveAuthorizationRequired() }
                403 -> throw DriveAccessDenied()
                404 -> throw FileNotFoundException("Selected Drive file not found")
                412 -> throw DriveRemoteConflict()
                else -> throw IOException("Google Drive HTTP ${response.status}")
            }
        }
        throw DriveAuthorizationRequired()
    }
}

internal fun readBounded(stream: InputStream, maximum: Int): ByteArray {
    val result = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = stream.read(buffer)
        if (count < 0) return result.toByteArray()
        if (result.size() + count > maximum) throw IOException("Oversized Drive metadata")
        result.write(buffer, 0, count)
    }
}
internal fun parseDriveMetadata(bytes: ByteArray, etag: String?): DriveFileMetadata {
    try {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        if (json.optBoolean("trashed", false)) throw FileNotFoundException("Selected Drive file deleted")
        if (json.optJSONObject("capabilities")?.optBoolean("canDownload", false) != true) throw DriveAccessDenied()
        val id = DriveSelection.fileId(json.getString("id"))
        val size = json.getString("size").toLongOrNull() ?: throw IOException("Invalid Drive file size")
        val version = json.getString("version")
        require(version.isNotBlank() && version.length <= 256) { "Invalid Drive version" }
        require(etag == null || etag.length <= 512 && etag.none { it.code < 32 }) { "Invalid Drive ETag" }
        return DriveFileMetadata(id, json.getString("mimeType"), size, version, etag)
    } catch (error: IOException) { throw error }
    catch (_: Exception) { throw IOException("Malformed Drive metadata") }
}
