package ru.fo6osik.workjournal.documents

import android.content.res.AssetManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.URI
import java.security.MessageDigest

/** Opt-in catalog: existing screens continue to use their current asset paths. */
object DocumentCatalog {
    const val ASSET_PATH = "documents/catalog.json"
    const val SCHEMA_VERSION = 1
    private val idPattern = Regex("[a-z][a-z0-9_]*")
    private val sha256Pattern = Regex("[0-9a-f]{64}")

    fun load(assets: AssetManager): DocumentCatalogData {
        val catalog = assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).use {
            parse(it.readText())
        }
        verifyAssetIndexes(catalog) { path -> assets.open(path) }
        return catalog
    }

    fun parse(json: String): DocumentCatalogData {
        val root = JSONObject(json)
        require(root.get("schemaVersion") is Int) { "schemaVersion must be an integer" }
        val documents = root.getJSONArray("documents")
        val catalog = DocumentCatalogData(
            schemaVersion = root.getInt("schemaVersion"),
            documents = (0 until documents.length()).map { position ->
                val item = documents.getJSONObject(position)
                DocumentDescriptor(
                    documentId = requiredString(item, "documentId"),
                    title = requiredString(item, "title"),
                    revision = nullableString(item, "revision"),
                    pdfSha256 = nullableString(item, "pdfSha256"),
                    searchIndexSha256 = nullableString(item, "searchIndexSha256"),
                    sources = parseSources(item.getJSONArray("sources"))
                )
            }
        )
        validate(catalog)
        return catalog
    }

    /** Pure validation is also usable in JVM tests, without Android or provider SDKs. */
    fun validate(catalog: DocumentCatalogData) {
        require(catalog.schemaVersion == SCHEMA_VERSION) { "Unsupported catalog schema" }
        require(catalog.documents.isNotEmpty()) { "Empty document catalog" }
        val ids = mutableSetOf<String>()
        val assetPdfPaths = mutableSetOf<String>()
        val assetIndexPaths = mutableSetOf<String>()
        catalog.documents.forEach { document ->
            require(idPattern.matches(document.documentId)) { "Invalid documentId: ${document.documentId}" }
            require(ids.add(document.documentId)) { "Duplicate documentId: ${document.documentId}" }
            require(document.title.isNotBlank()) { "Missing title: ${document.documentId}" }
            require(document.revision == null || document.revision.isNotBlank()) { "Empty revision" }
            listOf(document.pdfSha256, document.searchIndexSha256).forEach { hash ->
                require(hash == null || sha256Pattern.matches(hash)) { "Invalid SHA-256: ${document.documentId}" }
            }
            require(document.sources.isNotEmpty()) { "Missing sources: ${document.documentId}" }
            require(document.sources.distinct().size == document.sources.size) { "Duplicate source" }
            require(document.sources.filterIsInstance<DocumentSource.Assets>().size <= 1) { "Multiple asset sources" }
            document.sources.forEach { source ->
                when (source) {
                    is DocumentSource.Assets -> {
                        require(isRelativePath(source.pdfPath) && source.pdfPath.startsWith("manuals/") && source.pdfPath.endsWith(".pdf")) { "Invalid PDF asset path" }
                        require(isRelativePath(source.searchIndexPath)) { "Invalid index asset path" }
                        require(source.searchIndexPath == expectedIndexPath(source.pdfPath)) { "PDF/index path mismatch: ${document.documentId}" }
                        require(assetPdfPaths.add(source.pdfPath)) { "Duplicate PDF asset path" }
                        require(assetIndexPaths.add(source.searchIndexPath)) { "Duplicate index asset path" }
                    }
                    is DocumentSource.AndroidSaf -> {
                        require(isContentUri(source.pdfUri) && isContentUri(source.searchIndexUri)) { "SAF requires content URIs" }
                        require(source.pdfUri != source.searchIndexUri) { "PDF and index must be separate files" }
                    }
                    is DocumentSource.GoogleDrive -> {
                        val fileIdPattern = Regex("[A-Za-z0-9_-]+")
                        require(fileIdPattern.matches(source.pdfFileId) && fileIdPattern.matches(source.searchIndexFileId)) { "Drive requires file IDs, not URLs" }
                        require(source.pdfFileId != source.searchIndexFileId) { "PDF and index must be separate files" }
                    }
                    is DocumentSource.FirebaseStorage -> {
                        require(Regex("[a-z0-9][a-z0-9.-]*[a-z0-9]").matches(source.bucket)) { "Firebase requires a bucket name, not a URL" }
                        require(isRelativePath(source.pdfObjectPath) && isRelativePath(source.searchIndexObjectPath)) { "Invalid Firebase object path" }
                        require(source.pdfObjectPath != source.searchIndexObjectPath) { "PDF and index must be separate files" }
                    }
                }
            }
        }
    }

    /** Checks index existence and known hashes. Missing original PDFs are deliberately allowed. */
    fun verifyAssetIndexes(catalog: DocumentCatalogData, openAsset: (String) -> InputStream) {
        validate(catalog)
        catalog.documents.forEach { document ->
            document.assetsSource?.let { source ->
                val actualHash = openAsset(source.searchIndexPath).use { sha256(it) }
                require(document.searchIndexSha256 == null || actualHash == document.searchIndexSha256) {
                    "Index SHA-256 mismatch: ${document.documentId}"
                }
            }
        }
    }

    fun expectedIndexPath(pdfPath: String): String {
        require(pdfPath.endsWith(".pdf")) { "Expected a PDF path" }
        return pdfPath.removeSuffix(".pdf") + "_search.json"
    }

    private fun parseSources(items: JSONArray): List<DocumentSource> =
        (0 until items.length()).map { position ->
            val source = items.getJSONObject(position)
            when (val type = requiredString(source, "type")) {
                "assets" -> DocumentSource.Assets(requiredString(source, "pdfPath"), requiredString(source, "searchIndexPath"))
                "androidSaf" -> DocumentSource.AndroidSaf(requiredString(source, "pdfUri"), requiredString(source, "searchIndexUri"))
                "googleDrive" -> DocumentSource.GoogleDrive(requiredString(source, "pdfFileId"), requiredString(source, "searchIndexFileId"))
                "firebaseStorage" -> DocumentSource.FirebaseStorage(requiredString(source, "bucket"), requiredString(source, "pdfObjectPath"), requiredString(source, "searchIndexObjectPath"))
                else -> throw IllegalArgumentException("Unknown document source: $type")
            }
        }

    private fun requiredString(item: JSONObject, key: String): String {
        val value = item.get(key)
        require(value is String) { "Expected a string: $key" }
        return value
    }

    private fun nullableString(item: JSONObject, key: String): String? {
        require(item.has(key)) { "Missing field: $key (use null for unknown values)" }
        if (item.isNull(key)) return null
        val value = item.get(key)
        require(value is String) { "Expected a string or null: $key" }
        return value
    }

    private fun isRelativePath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith("/") && !path.contains('\\') &&
            !path.contains(':') && path.none { it.code < 32 } &&
            path.split('/').all { it.isNotBlank() && it != "." && it != ".." }

    private fun isContentUri(value: String): Boolean = try {
        val uri = URI(value)
        uri.scheme == "content" && !uri.authority.isNullOrBlank() && !uri.path.isNullOrBlank()
    } catch (_: Exception) {
        false
    }

    private fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count > 0) digest.update(buffer, 0, count)
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
