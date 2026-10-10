package ru.fo6osik.workjournal.documents

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.fo6osik.workjournal.documents.sources.AssetsDocumentSource
import ru.fo6osik.workjournal.documents.sources.GoogleDriveDocumentSource
import ru.fo6osik.workjournal.documents.sources.GoogleDrivePairMetadata
import ru.fo6osik.workjournal.documents.sources.GoogleDriveTransport
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

class GoogleDriveDocumentSourceTest {
    @get:Rule val temp = TemporaryFolder()
    private val location = DocumentSource.GoogleDrive("fixture_pdf", "fixture_index")
    private val pdf = "%PDF-synthetic".toByteArray()
    private val index = "synthetic index".toByteArray()
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    private val document get() = DocumentDescriptor("fixture", "Fixture", null, hash(pdf), hash(index), listOf(location))

    // Only synthetic IDs/bytes. No account, real Drive client, or PDF assets.
    private inner class FakeTransport : GoogleDriveTransport {
        var pair = GoogleDrivePairMetadata(location.pdfFileId, location.searchIndexFileId, null, hash(pdf), hash(index))
        val files = mutableMapOf(location.pdfFileId to pdf, location.searchIndexFileId to index)
        val calls = mutableListOf<String>()
        val closed = mutableListOf<String>()
        var metadataFailure: Exception? = null
        var openFailure: Exception? = null
        var readFailure: Exception? = null
        var missingAfterMetadata: String? = null
        override fun metadata(pdfFileId: String, searchIndexFileId: String): GoogleDrivePairMetadata {
            calls.add("metadata:$pdfFileId:$searchIndexFileId")
            metadataFailure?.let { throw it }
            if (pdfFileId !in files || searchIndexFileId !in files) throw FileNotFoundException("Missing fixture")
            return pair.also { missingAfterMetadata?.let { files.remove(it) } }
        }
        override fun openFile(fileId: String): InputStream {
            calls.add(fileId)
            openFailure?.let { throw it }
            val bytes = files[fileId] ?: throw FileNotFoundException("Missing fixture")
            return object : ByteArrayInputStream(bytes) {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    readFailure?.let { throw it }
                    return super.read(buffer, offset, length)
                }
                override fun close() { closed.add(fileId); super.close() }
            }
        }
    }

    private fun source(transport: FakeTransport, caller: Thread) = GoogleDriveDocumentSource(transport) {
        check(Thread.currentThread() !== caller) { "Test main thread" }
    }
    private fun store() = LocalDocumentStore(temp.newFolder(), DocumentIntegrityValidator { _, _ -> })
    private fun repo(store: LocalDocumentStore, source: GoogleDriveDocumentSource, descriptor: DocumentDescriptor = document) =
        DefaultDocumentRepository(DocumentCatalogData(1, listOf(descriptor)), store, listOf(source))
    private suspend fun failure(block: suspend () -> Unit): Exception {
        try { block(); throw AssertionError("Expected failure") } catch (e: Exception) { return e }
    }

    @Test fun supportsOnlyDrive() {
        val source = source(FakeTransport(), Thread.currentThread())
        assertTrue(source.supports(location))
        assertFalse(source.supports(DocumentSource.Assets("manuals/fixture.pdf", "manuals/fixture_search.json")))
        assertFalse(source.supports(DocumentSource.AndroidSaf("content://fixture/pdf", "content://fixture/index")))
        assertFalse(source.supports(DocumentSource.FirebaseStorage("fixture.appspot.com", "pdf", "index")))
    }

    @Test fun metadataRunsOnIoAndReturnsExactPair() = runBlocking {
        val transport = FakeTransport()
        val result = source(transport, Thread.currentThread()).metadata(document, location)
        assertNull(result.revision)
        assertEquals(hash(pdf), result.pdfSha256)
        assertEquals(hash(index), result.searchIndexSha256)
        assertEquals(listOf("metadata:fixture_pdf:fixture_index"), transport.calls)
    }

    @Test fun downloadsBothFilesClosesStreamsAndOpensOffline() = runBlocking {
        val transport = FakeTransport()
        val store = store()
        val repo = repo(store, source(transport, Thread.currentThread()))
        repo.download("fixture").use {
            assertArrayEquals(pdf, it.pdfFile.readBytes())
            assertArrayEquals(index, it.searchIndexFile.readBytes())
            assertNull(it.revision)
        }
        assertEquals(listOf("metadata:fixture_pdf:fixture_index", "fixture_pdf", "fixture_index"), transport.calls)
        assertEquals(listOf("fixture_pdf", "fixture_index"), transport.closed)
        transport.metadataFailure = IOException("offline")
        repo.open("fixture").close()
        assertEquals(3, transport.calls.size)
    }

    @Test fun missingFileInMetadataPropagatesWithoutActivation() = runBlocking {
        for (id in listOf(location.pdfFileId, location.searchIndexFileId)) {
            val transport = FakeTransport().apply { files.remove(id) }
            val store = store()
            assertTrue(failure { repo(store, source(transport, Thread.currentThread())).download("fixture") } is FileNotFoundException)
            assertNull(store.acquire("fixture"))
            assertEquals(1, transport.calls.size)
        }
    }

    @Test fun missingFileWhenOpeningStreamPropagates() = runBlocking {
        val transport = FakeTransport()
        val source = source(transport, Thread.currentThread())
        transport.files.clear()
        withContext(Dispatchers.IO) {
            assertTrue(failure { source.openPdf(location) } is FileNotFoundException)
            assertTrue(failure { source.openSearchIndex(location) } is FileNotFoundException)
        }
    }

    @Test fun indexDisappearingAfterMetadataClosesPdfAndDoesNotActivate() = runBlocking {
        val transport = FakeTransport().apply { missingAfterMetadata = location.searchIndexFileId }
        val store = store()
        assertTrue(failure { repo(store, source(transport, Thread.currentThread())).download("fixture") } is FileNotFoundException)
        assertNull(store.acquire("fixture"))
        assertEquals(listOf(location.pdfFileId), transport.closed)
    }

    @Test fun catalogChecksumConflictIsRejectedBeforeDownloading() = runBlocking {
        val transport = FakeTransport().apply { pair = pair.copy(pdfSha256 = "0".repeat(64)) }
        assertTrue(failure { repo(store(), source(transport, Thread.currentThread())).download("fixture") } is IllegalArgumentException)
        assertEquals(listOf("metadata:fixture_pdf:fixture_index"), transport.calls)
    }

    @Test fun metadataAndOpenNetworkErrorsPreserveOriginalException() = runBlocking {
        val transport = FakeTransport()
        val source = source(transport, Thread.currentThread())
        val network = IOException("fixture network error")
        transport.metadataFailure = network
        assertSame(network, failure { source.metadata(document, location) })
        transport.metadataFailure = null
        transport.openFailure = network
        withContext(Dispatchers.IO) {
            assertSame(network, failure { source.openPdf(location) })
            assertSame(network, failure { source.openSearchIndex(location) })
        }
    }

    @Test fun streamNetworkFailureClosesResponseAndPreservesPreviousGeneration() = runBlocking {
        val transport = FakeTransport()
        val store = store()
        val repo = repo(store, source(transport, Thread.currentThread()))
        val previous = repo.download("fixture")
        transport.pair = transport.pair.copy(revision = "pair-r2")
        val network = IOException("fixture read failed")
        transport.readFailure = network
        assertSame(network, failure { repo.download("fixture") })
        store.acquire("fixture")!!.use { assertEquals(previous.pdfFile, it.pdfFile) }
        assertEquals(2, transport.closed.count { it == location.pdfFileId })
        previous.close()
    }

    @Test fun rejectsMalformedHashesAndBlankRevisionBeforeOpeningFiles() = runBlocking {
        val transport = FakeTransport()
        val source = source(transport, Thread.currentThread())
        val valid = transport.pair
        for (bad in listOf("", "abc", "g".repeat(64), "A".repeat(64))) {
            transport.pair = valid.copy(pdfSha256 = bad)
            assertTrue(failure { source.metadata(document, location) } is IllegalArgumentException)
            transport.pair = valid.copy(searchIndexSha256 = bad)
            assertTrue(failure { source.metadata(document, location) } is IllegalArgumentException)
        }
        transport.pair = valid.copy(revision = " ")
        assertTrue(failure { source.metadata(document, location) } is IllegalArgumentException)
        assertTrue(transport.calls.all { it.startsWith("metadata:") })
    }

    @Test fun rejectsWrongPairIdsAndIncompleteDeclaredRevision() = runBlocking {
        val transport = FakeTransport()
        val source = source(transport, Thread.currentThread())
        for (bad in listOf(
            transport.pair.copy(pdfFileId = "other"),
            transport.pair.copy(searchIndexFileId = "other"),
            transport.pair.copy(revision = "r1", pdfSha256 = null),
            transport.pair.copy(revision = "r1", searchIndexSha256 = null)
        )) {
            transport.pair = bad
            assertTrue(failure { source.metadata(document, location) } is IllegalArgumentException)
        }
    }

    @Test fun unknownMetadataStaysUnknownAndCannotActivateWithoutCatalogHashes() = runBlocking {
        val transport = FakeTransport().apply { pair = pair.copy(pdfSha256 = null, searchIndexSha256 = null) }
        val source = source(transport, Thread.currentThread())
        val metadata = source.metadata(document, location)
        assertNull(metadata.pdfSha256); assertNull(metadata.searchIndexSha256); assertNull(metadata.revision)
        val store = store()
        assertTrue(failure { repo(store, source, document.copy(pdfSha256 = null, searchIndexSha256 = null)).download("fixture") } is UnverifiedDocumentException)
        assertNull(store.acquire("fixture"))
    }

    @Test fun directMainThreadOpenFailsBeforeTransportCall() = runBlocking {
        val transport = FakeTransport()
        val source = source(transport, Thread.currentThread())
        assertTrue(failure { source.openPdf(location) } is IllegalStateException)
        assertTrue(failure { source.openSearchIndex(location) } is IllegalStateException)
        assertTrue(transport.calls.isEmpty())
    }

    @Test fun returnedStreamRejectsMainThreadConsumptionAndClose() = runBlocking {
        val transport = FakeTransport()
        val source = source(transport, Thread.currentThread())
        val stream = withContext(Dispatchers.IO) { source.openPdf(location) }
        assertTrue(failure { stream.read() } is IllegalStateException)
        assertTrue(failure { stream.read(ByteArray(4)) } is IllegalStateException)
        assertTrue(failure { stream.skip(1) } is IllegalStateException)
        assertTrue(failure { stream.close() } is IllegalStateException)
        assertTrue(transport.closed.isEmpty())
        withContext(Dispatchers.IO) { stream.close() }
        assertEquals(listOf(location.pdfFileId), transport.closed)
    }

    @Test fun rejectsWrongLocationAndMalformedIdsWithoutTransportCalls() = runBlocking {
        val transport = FakeTransport()
        val source = source(transport, Thread.currentThread())
        for (bad in listOf(
            DocumentSource.Assets("manuals/fixture.pdf", "manuals/fixture_search.json"),
            location.copy(pdfFileId = "https://drive.google.com/file/fixture"),
            location.copy(searchIndexFileId = location.pdfFileId),
            location.copy(pdfFileId = "")
        )) {
            assertTrue(failure { source.metadata(document, bad) } is IllegalArgumentException)
            assertTrue(failure { source.openPdf(bad) } is IllegalArgumentException)
            assertTrue(failure { source.openSearchIndex(bad) } is IllegalArgumentException)
        }
        assertTrue(failure { source.metadata(document.copy(sources = emptyList()), location) } is IllegalArgumentException)
        assertTrue(transport.calls.isEmpty())
    }

    @Test fun changedPairRevisionUpdatesAndReusedRevisionIsRejected() = runBlocking {
        val transport = FakeTransport().apply { pair = pair.copy(revision = "pair-r1") }
        val repo = repo(store(), source(transport, Thread.currentThread()))
        repo.download("fixture").close()
        assertEquals(UpdateStatus.CURRENT, repo.checkForUpdate("fixture"))
        val updated = pdf + 1
        transport.files[location.pdfFileId] = updated
        transport.pair = transport.pair.copy(revision = "pair-r2", pdfSha256 = hash(updated))
        assertEquals(UpdateStatus.AVAILABLE, repo.checkForUpdate("fixture"))
        repo.download("fixture").use { assertEquals("pair-r2", it.revision) }
        transport.pair = transport.pair.copy(pdfSha256 = hash(pdf))
        assertTrue(failure { repo.download("fixture") } is IOException)
    }

    @Test fun changedBytesAfterMetadataFailHashCheckWithoutReplacingPair() = runBlocking {
        val transport = FakeTransport()
        val store = store()
        val repo = repo(store, source(transport, Thread.currentThread()))
        val previous = repo.download("fixture")
        val expected = pdf + 1
        transport.pair = transport.pair.copy(revision = "r2", pdfSha256 = hash(expected))
        transport.files[location.pdfFileId] = pdf + 2
        assertTrue(failure { repo.download("fixture") } is IllegalArgumentException)
        store.acquire("fixture")!!.use { assertEquals(previous.pdfFile, it.pdfFile) }
        previous.close()
    }

    @Test fun assetsSourceStillWorksBesideDriveAdapter() = runBlocking {
        val asset = DocumentSource.Assets("manuals/fixture.pdf", "manuals/fixture_search.json")
        val descriptor = document.copy(sources = listOf(asset, location))
        val transport = FakeTransport()
        val assets = AssetsDocumentSource { path -> ByteArrayInputStream(if (path == asset.pdfPath) pdf else index) }
        val repo = DefaultDocumentRepository(DocumentCatalogData(1, listOf(descriptor)), store(),
            listOf(source(transport, Thread.currentThread()), assets))
        repo.download("fixture").close()
        assertTrue(transport.calls.isEmpty())
    }
}
