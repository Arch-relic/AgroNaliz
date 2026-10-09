package ru.fo6osik.workjournal.documents

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.fo6osik.workjournal.documents.sources.AssetsDocumentSource
import ru.fo6osik.workjournal.documents.sources.SourceMetadata
import ru.fo6osik.workjournal.documents.sources.DocumentSource as Adapter
import java.io.*
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DocumentRepositoryTest {
    @get:Rule val temp = TemporaryFolder()
    private val location = DocumentSource.Assets("manuals/example.pdf", "manuals/example_search.json")
    private val descriptor get() = DocumentDescriptor("example", "Example", null, hash(pdf), hash(index), listOf(location))
    // Synthetic bytes exist only under TemporaryFolder, never in application assets/catalog.
    private val pdf = "%PDF-test fixture".toByteArray()
    private val index = "test index fixture".toByteArray()
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun store(root: File = temp.newFolder()) = LocalDocumentStore(root, DocumentIntegrityValidator { _, _ -> })
    private fun revision(value: String, content: ByteArray = pdf) = descriptor.copy(revision = value, pdfSha256 = hash(content), searchIndexSha256 = hash(index))
    private fun repo(store: LocalDocumentStore, source: Adapter, doc: DocumentDescriptor = descriptor) =
        DefaultDocumentRepository(DocumentCatalogData(1, listOf(doc)), store, listOf(source))
    private open inner class Source : Adapter {
        var reads = 0
        var failure: Exception? = null
        var bytes = pdf
        var meta = SourceMetadata(null, null, null)
        override fun supports(location: DocumentSource) = location is DocumentSource.Assets
        override suspend fun metadata(document: DocumentDescriptor, location: DocumentSource) = meta
        override fun openPdf(location: DocumentSource): InputStream { reads++; failure?.let { throw it }; return ByteArrayInputStream(bytes) }
        override fun openSearchIndex(location: DocumentSource): InputStream = ByteArrayInputStream(index)
    }
    private suspend fun fails(block: suspend () -> Unit): Exception {
        try { block(); throw AssertionError("Expected failure") } catch (e: Exception) { return e }
    }

    @Test fun downloadsPairAndOpensOfflineWithoutContactingSource() = runBlocking {
        val store = store(); val source = Source(); val repo = repo(store, source)
        repo.open("example").use { assertArrayEquals(pdf, it.pdfFile.readBytes()); assertArrayEquals(index, it.searchIndexFile.readBytes()); assertNull(it.revision) }
        source.failure = IOException("offline")
        repo.open("example").close()
        assertEquals(1, source.reads)
        assertTrue(repo.observeState("example").value is DocumentState.ReadyOffline)
    }

    @Test fun missingOriginalPdfReportsErrorWithoutCreatingFakeCopy() = runBlocking {
        val store = store()
        val source = AssetsDocumentSource { throw FileNotFoundException(it) }
        val repo = repo(store, source)
        assertTrue(fails { repo.open("example") } is FileNotFoundException)
        assertNull(store.acquire("example"))
        assertFalse((repo.observeState("example").value as DocumentState.Error).previousCopyAvailable)
    }

    @Test fun failedUpdatePreservesPreviousPairAndLease() = runBlocking {
        val store = store(); val source = Source(); val repo = repo(store, source)
        val previous = repo.open("example")
        source.failure = IOException("network failure")
        fails { repo.download("example") }
        assertArrayEquals(pdf, previous.pdfFile.readBytes())
        store.acquire("example")!!.use { assertEquals(previous.pdfFile, it.pdfFile) }
        assertTrue((repo.observeState("example").value as DocumentState.Error).previousCopyAvailable)
        previous.close()
    }

    @Test fun badChecksumCannotReplaceExistingGeneration() = runBlocking {
        val store = store()
        val old = store.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) })
        fails { store.install(descriptor.copy(pdfSha256 = "0".repeat(64)), { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) }) }
        store.acquire("example")!!.use { assertEquals(old.pdfFile, it.pdfFile) }
        old.close()
    }

    @Test fun badIndexChecksumAndValidationFailurePreservePointer() = runBlocking {
        val root = temp.newFolder(); val store = store(root)
        val old = store.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) })
        fails { store.install(descriptor.copy(searchIndexSha256 = "0".repeat(64)), { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) }) }
        val rejecting = LocalDocumentStore(root, DocumentIntegrityValidator { _, _ -> throw IOException("Malformed PDF/index") })
        fails { rejecting.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) }) }
        store.acquire("example")!!.use { assertEquals(old.pdfFile, it.pdfFile) }
        old.close()
    }

    @Test fun cleanupProtectsLeasesAcrossStoreInstances() = runBlocking {
        val root = temp.newFolder(); val store = store(root); val other = store(root)
        val old = store.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) })
        val current = other.install(descriptor.copy(pdfSha256 = hash(pdf + 1)), { ByteArrayInputStream(pdf + 1) }, { ByteArrayInputStream(index) })
        assertEquals(0, other.pruneUnused("example"))
        old.close(); old.close()
        assertEquals(1, other.pruneUnused("example"))
        assertFalse(old.pdfFile.exists()); assertTrue(current.pdfFile.exists())
        current.close()
    }

    @Test fun persistedCopySurvivesNewRepositoryInstance() = runBlocking {
        val root = temp.newFolder(); val store = store(root)
        repo(store, Source()).download("example").close()
        val failing = Source().apply { failure = IOException("offline") }
        repo(store(root), failing).open("example").close()
        assertEquals(0, failing.reads)
    }

    @Test fun localCorruptionIsDetected() = runBlocking {
        val store = store(); val doc = store.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) })
        doc.pdfFile.writeText("corrupt")
        assertTrue(fails { store.acquire("example") } is IOException)
        doc.close()
    }

    @Test fun rejectsPathTraversalAndUnknownIds() = runBlocking {
        val store = store(); val repo = repo(store, Source())
        assertTrue(fails { store.acquire("../data") } is IllegalArgumentException)
        assertTrue(fails { repo.open("unknown") } is IllegalArgumentException)
    }

    @Test fun opaqueRevisionsDetectUpdatesAndRejectReusedRevision() = runBlocking {
        val store = store(); val source = Source()
        source.meta = SourceMetadata("r1", hash(pdf), hash(index))
        val repo = repo(store, source)
        repo.download("example").close()
        assertEquals(UpdateStatus.CURRENT, repo.checkForUpdate("example"))
        source.bytes = pdf + 1
        source.meta = SourceMetadata("r2", hash(source.bytes), hash(index))
        assertEquals(UpdateStatus.AVAILABLE, repo.checkForUpdate("example"))
        assertTrue(repo.observeState("example").value is DocumentState.UpdateAvailable)
        repo.download("example").use { assertEquals("r2", it.revision) }
        source.meta = SourceMetadata("r2", hash(pdf), hash(index))
        assertTrue(fails { repo.checkForUpdate("example") } is IOException)
        fails { repo.download("example") }
        Unit
    }

    @Test fun unknownMetadataDoesNotInventRevisionOrClaimCurrent() = runBlocking {
        val store = store()
        store.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) }).close()
        val repo = repo(store, Source(), descriptor.copy(pdfSha256 = null, searchIndexSha256 = null))
        repo.open("example").use { assertNull(it.revision) }
        assertEquals(UpdateStatus.UNKNOWN, repo.checkForUpdate("example"))
    }

    @Test fun incompleteDeclaredRevisionIsRejected() = runBlocking {
        val source = Source().apply { meta = SourceMetadata("r1", null, null) }
        fails { repo(store(), source).download("example") }
        Unit
    }

    @Test fun concurrentDownloadIsRejectedEvenAcrossRepositories() = runBlocking {
        val root = temp.newFolder(); val started = CountDownLatch(1); val release = CountDownLatch(1)
        val source = object : Source() {
            override fun openPdf(location: DocumentSource): InputStream {
                started.countDown(); check(release.await(10, TimeUnit.SECONDS)); return super.openPdf(location)
            }
        }
        val first = repo(store(root), source); val second = repo(store(root), source)
        val job = async(Dispatchers.Default) { first.download("example").close() }
        try {
            assertTrue(started.await(10, TimeUnit.SECONDS))
            assertTrue(first.observeState("example").value is DocumentState.Loading)
            assertTrue(fails { second.download("example") } is DocumentBusyException)
        } finally { release.countDown() }
        job.await(); assertEquals(1, source.reads)
    }

    @Test fun interruptedStreamClosesAndLeavesPreviousPair() = runBlocking {
        val store = store(); val old = store.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) })
        var closed = false
        fails { store.install(descriptor, { object : InputStream() {
            override fun read(): Int = throw IOException("interrupted")
            override fun close() { closed = true }
        } }, { ByteArrayInputStream(index) }) }
        assertTrue(closed)
        store.acquire("example")!!.use { assertEquals(old.pdfFile, it.pdfFile) }
        old.close()
    }

    @Test fun cancellationDoesNotActivatePartialPair() = runBlocking {
        val store = store(); val old = store.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) })
        val source = object : Source() {
            override fun openSearchIndex(location: DocumentSource): InputStream = throw CancellationException("cancelled")
        }
        val repo = repo(store, source)
        assertTrue(fails { repo.download("example") } is CancellationException)
        assertTrue(repo.observeState("example").value is DocumentState.ReadyOffline)
        store.acquire("example")!!.use { assertEquals(old.pdfFile, it.pdfFile) }
        old.close()
    }

    @Test fun emptyComponentAndMissingAdapterAreRejected() = runBlocking {
        val store = store()
        fails { store.install(descriptor, { ByteArrayInputStream(byteArrayOf()) }, { ByteArrayInputStream(index) }) }
        val repo = DefaultDocumentRepository(DocumentCatalogData(1, listOf(descriptor)), store, emptyList())
        fails { repo.open("example") }
        assertNull(store.acquire("example"))
    }
    @Test fun fileWorkRunsOffCallerThread() = runBlocking {
        val caller = Thread.currentThread()
        val source = object : Source() {
            override fun openPdf(location: DocumentSource): InputStream {
                assertNotSame(caller, Thread.currentThread())
                return super.openPdf(location)
            }
        }
        val store = LocalDocumentStore(temp.newFolder(), DocumentIntegrityValidator { _, _ ->
            assertNotSame(caller, Thread.currentThread())
        })
        repo(store, source).download("example").close()
    }

    @Test fun corruptedCurrentPreventsPruningRecoveryCopyAndCanBeRepaired() = runBlocking {
        val store = store()
        val old = store.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) })
        val current = store.install(descriptor.copy(pdfSha256 = hash(pdf + 1)), { ByteArrayInputStream(pdf + 1) }, { ByteArrayInputStream(index) })
        old.close(); current.close()
        current.pdfFile.writeText("corrupt")
        fails { store.pruneUnused("example") }
        assertTrue(old.pdfFile.exists())
        repo(store, Source()).download("example").close()
        store.acquire("example")!!.use { assertArrayEquals(pdf, it.pdfFile.readBytes()) }
    }

    @Test fun actualCoroutineCancellationWhileCopyingPreservesOldPair() = runBlocking {
        val root = temp.newFolder(); val store = store(root)
        val old = store.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) })
        val started = CountDownLatch(1); val release = CountDownLatch(1)
        var closed = false
        val job = launch(Dispatchers.Default) {
            store.install(descriptor, { object : ByteArrayInputStream(pdf) {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    started.countDown(); check(release.await(10, TimeUnit.SECONDS))
                    return super.read(buffer, offset, length)
                }
                override fun close() { closed = true; super.close() }
            } }, { ByteArrayInputStream(index) }).close()
        }
        try { assertTrue(started.await(10, TimeUnit.SECONDS)); job.cancel() }
        finally { release.countDown() }
        job.join()
        assertTrue(closed)
        store.acquire("example")!!.use { assertEquals(old.pdfFile, it.pdfFile) }
        assertFalse(File(root, "example").listFiles()!!.any { it.name.startsWith(".tmp-") })
        old.close()
    }

    @Test fun catalogHashConflictDoesNotAcceptDifferentSourcePair() = runBlocking {
        val source = Source().apply { meta = SourceMetadata(null, "0".repeat(64), hash(index)) }
        val document = descriptor.copy(pdfSha256 = hash(pdf), searchIndexSha256 = hash(index))
        assertTrue(fails { repo(store(), source, document).download("example") } is IllegalArgumentException)
        assertEquals(0, source.reads)
    }

    @Test fun unknownExpectedPdfCannotActivateOrReplacePair() = runBlocking {
        val store = store()
        val old = store.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) })
        val unknown = descriptor.copy(pdfSha256 = null)
        assertTrue(fails { store.install(unknown, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) }) } is UnverifiedDocumentException)
        store.acquire("example")!!.use { assertEquals(old.pdfFile, it.pdfFile) }
        old.close()
    }

    @Test fun unknownExpectedIndexCannotActivateFirstPair() = runBlocking {
        val store = store()
        assertTrue(fails { store.install(descriptor.copy(searchIndexSha256 = null), { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) }) } is UnverifiedDocumentException)
        assertNull(store.acquire("example"))
    }

    @Test fun unavailableCloudAndSafProvidersAreExplicitlyUnsupported() = runBlocking {
        for (location in listOf(
            DocumentSource.GoogleDrive("pdf_id", "index_id"),
            DocumentSource.AndroidSaf("content://fixture/pdf", "content://fixture/index"),
            DocumentSource.FirebaseStorage("fixture.appspot.com", "manual.pdf", "index.json")
        )) {
            val document = descriptor.copy(sources = listOf(location))
            val repository = repo(store(), AssetsDocumentSource { throw FileNotFoundException(it) }, document)
            assertTrue(fails { repository.open("example") } is UnsupportedDocumentSourceException)
            assertTrue(fails { repository.checkForUpdate("example") } is UnsupportedDocumentSourceException)
            assertTrue(repository.observeState("example").value is DocumentState.Error)
        }
    }

}
