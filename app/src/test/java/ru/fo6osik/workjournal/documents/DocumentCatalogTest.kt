package ru.fo6osik.workjournal.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.security.MessageDigest

class DocumentCatalogTest {
    private val source = DocumentSource.Assets("manuals/example.pdf", "manuals/example_search.json")
    private val document = DocumentDescriptor("example", "Руководство", null, null, null, listOf(source))
    private fun catalog(vararg documents: DocumentDescriptor) = DocumentCatalogData(1, documents.toList())

    private fun rejects(action: () -> Unit) {
        try {
            action()
            throw AssertionError("Invalid catalog was accepted")
        } catch (_: IllegalArgumentException) {
            // Expected validation failure.
        }
    }

    @Test fun acceptsUnknownPdfHashAndRevision() {
        DocumentCatalog.validate(catalog(document))
        assertNull(document.revision)
        assertNull(document.pdfSha256)
    }

    @Test fun acceptsKnownPdfHashWithUnknownRevisionWithoutOpeningPdf() {
        val bytes = "index".toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        val known = document.copy(pdfSha256 = "a".repeat(64), searchIndexSha256 = hash)
        DocumentCatalog.verifyAssetIndexes(catalog(known)) { path ->
            assertEquals(source.searchIndexPath, path)
            ByteArrayInputStream(bytes)
        }
        assertNull(known.revision)
    }

    @Test fun rejectsDuplicateIds() = rejects {
        val other = document.copy(sources = listOf(
            source.copy(pdfPath = "manuals/other.pdf", searchIndexPath = "manuals/other_search.json")
        ))
        DocumentCatalog.validate(catalog(document, other))
    }

    @Test fun rejectsDuplicateAssetPaths() = rejects {
        DocumentCatalog.validate(catalog(document, document.copy(documentId = "other")))
    }

    @Test fun rejectsInvalidIdsAndBlankTitles() {
        for (id in listOf("", "../example", "Example", "example-id")) {
            rejects { DocumentCatalog.validate(catalog(document.copy(documentId = id))) }
        }
        rejects { DocumentCatalog.validate(catalog(document.copy(title = " "))) }
    }

    @Test fun rejectsUnsupportedSchemaAndEmptyCatalog() {
        rejects { DocumentCatalog.validate(DocumentCatalogData(2, listOf(document))) }
        rejects { DocumentCatalog.validate(DocumentCatalogData(1, emptyList())) }
    }

    @Test fun rejectsInvalidHashesAndEmptyRevision() {
        for (hash in listOf("", "abc", "g".repeat(64), "A".repeat(64), "a".repeat(63), "a".repeat(65))) {
            rejects { DocumentCatalog.validate(catalog(document.copy(pdfSha256 = hash))) }
            rejects { DocumentCatalog.validate(catalog(document.copy(searchIndexSha256 = hash))) }
        }
        rejects { DocumentCatalog.validate(catalog(document.copy(revision = ""))) }
    }

    @Test fun rejectsMissingAndDuplicateSources() {
        rejects { DocumentCatalog.validate(catalog(document.copy(sources = emptyList()))) }
        rejects { DocumentCatalog.validate(catalog(document.copy(sources = listOf(source, source)))) }
    }

    @Test fun rejectsMismatchedIndexAndUnsafePaths() {
        for (pdf in listOf("/manuals/example.pdf", "manuals/../example.pdf", "manuals//example.pdf", "manuals\\example.pdf", "elsewhere/example.pdf")) {
            rejects { DocumentCatalog.validate(catalog(document.copy(sources = listOf(source.copy(pdfPath = pdf))))) }
        }
        rejects { DocumentCatalog.validate(catalog(document.copy(sources = listOf(source.copy(searchIndexPath = "manuals/other_search.json"))))) }
    }

    @Test fun mapsNestedPdfToExistingIndexNamingConvention() {
        assertEquals("manuals/engines/example_search.json", DocumentCatalog.expectedIndexPath("manuals/engines/example.pdf"))
    }

    @Test fun supportsAllFourProvidersWithoutSdk() {
        DocumentCatalog.validate(catalog(document.copy(sources = listOf(
            source,
            DocumentSource.AndroidSaf("content://provider/document/pdf", "content://provider/document/index"),
            DocumentSource.GoogleDrive("pdf_file_id", "index_file_id"),
            DocumentSource.FirebaseStorage("example.firebasestorage.app", "manuals/example.pdf", "manuals/example_search.json")
        ))))
    }

    @Test fun rejectsCloudUrlsAndInvalidSafUris() {
        val invalid = listOf(
            DocumentSource.AndroidSaf("file:///example.pdf", "content://provider/index"),
            DocumentSource.GoogleDrive("https://drive.google.com/file/id", "index"),
            DocumentSource.FirebaseStorage("gs://example.appspot.com", "example.pdf", "index.json"),
            DocumentSource.FirebaseStorage("example.appspot.com", "../example.pdf", "index.json")
        )
        invalid.forEach { rejects { DocumentCatalog.validate(catalog(document.copy(sources = listOf(it)))) } }
    }

    @Test fun verifiesIndexHashAndClosesStreamWithoutOpeningPdf() {
        val bytes = "search index bytes".toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        var closed = false
        val opened = mutableListOf<String>()
        DocumentCatalog.verifyAssetIndexes(catalog(document.copy(searchIndexSha256 = hash))) { path ->
            opened.add(path)
            object : ByteArrayInputStream(bytes) {
                override fun close() { closed = true; super.close() }
            }
        }
        assertEquals(listOf(source.searchIndexPath), opened)
        assertTrue(closed)
    }

    @Test fun rejectsCorruptedIndex() = rejects {
        DocumentCatalog.verifyAssetIndexes(catalog(document.copy(searchIndexSha256 = "0".repeat(64)))) {
            ByteArrayInputStream("changed index".toByteArray())
        }
    }

    @Test(expected = FileNotFoundException::class)
    fun reportsMissingIndex() {
        DocumentCatalog.verifyAssetIndexes(catalog(document)) { throw FileNotFoundException(it) }
    }
}
