package ru.fo6osik.workjournal.documents.sources

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.fo6osik.workjournal.documents.*
import java.io.*
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.KeyGenerator

class GoogleDriveStage2Test {
    @get:Rule val temporary = TemporaryFolder()
    private val pdf = "%PDF-synthetic".toByteArray()
    private val index = "synthetic-index".toByteArray()
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    private val binding = DriveBinding("fixture", "selected_pdf", "selected_index")
    private val descriptor get() = DocumentDescriptor("fixture", "Fixture", null, hash(pdf), hash(index),
        listOf(ru.fo6osik.workjournal.documents.DocumentSource.GoogleDrive(binding.pdfId, binding.indexId)))
    private val catalog get() = DocumentCatalogData(1, listOf(descriptor))
    private fun grant(token: String = "test-token") = DriveGrant(token, setOf(DRIVE_FILE_SCOPE))
    private class Backend : DriveAuthorizationBackend {
        val calls = AtomicInteger(); val clears = AtomicInteger(); val revokes = AtomicInteger()
        var fail = false
        override fun authorize(account: String): DriveGrant {
            if (fail) throw DriveAuthorizationRequired()
            return DriveGrant("fake-${calls.incrementAndGet()}", setOf(DRIVE_FILE_SCOPE))
        }
        override fun clearToken(token: String) { clears.incrementAndGet() }
        override fun revoke(account: String) { revokes.incrementAndGet() }
    }
    private val tokens = object : DriveTokenProvider {
        var invalidations = 0
        override fun accessToken() = "fake-token"
        override fun invalidate(rejectedToken: String) { invalidations++ }
    }
    private fun metadata(id: String, size: Long, mime: String) =
        """{"id":"$id","mimeType":"$mime","size":"$size","version":"12","trashed":false,"capabilities":{"canDownload":true}}""".toByteArray()
    private inner class Http : DriveHttpClient {
        var pdfBytes = pdf; var indexBytes = index
        var failure: IOException? = null; var statuses = mutableListOf<Int>(); var released = 0
        var etags = mutableListOf<String?>(); var ids = mutableListOf<String>(); var bodyOverride: ByteArray? = null
        var declared: Long? = null
        override fun get(fileId: String, media: Boolean, token: String, etag: String?): DriveHttpResponse {
            failure?.let { throw it }
            ids.add(fileId); etags.add(etag)
            val bytes = if (media) if (fileId == binding.pdfId) pdfBytes else indexBytes else
                bodyOverride ?: metadata(fileId, if (fileId == binding.pdfId) pdf.size.toLong() else index.size.toLong(),
                    if (fileId == binding.pdfId) "application/pdf" else "application/json")
            val status = if (statuses.isEmpty()) 200 else statuses.removeAt(0)
            return DriveHttpResponse(status, ByteArrayInputStream(bytes), "\"test-etag\"", if (media) declared else null) { released++ }
        }
    }
    private fun transport(http: Http, guard: () -> Unit = {}) = GoogleDriveHttpTransport(http, tokens, listOf(descriptor to binding), guard)

    @Test fun tokensOnlyAllowDriveFileAndRedactToString() {
        assertThrows(IllegalArgumentException::class.java) { DriveGrant("test", setOf("https://www.googleapis.com/auth/drive")) }
        assertThrows(IllegalArgumentException::class.java) { DriveGrant("test", setOf(DRIVE_FILE_SCOPE, "openid")) }
        assertThrows(IllegalArgumentException::class.java) { grant("bad\r\ntoken") }
        assertFalse(grant().toString().contains("test-token"))
    }
    @Test fun restoreAndExpiryUseSdkWithoutPersistingToken() {
        val backend = Backend(); var now = 0L
        val session = GoogleDriveTokenSession(backend, { "synthetic-account" }, { now })
        assertEquals("fake-1", session.accessToken()); assertEquals("fake-1", session.accessToken())
        now = 300_000; assertEquals("fake-2", session.accessToken()); assertEquals(2, backend.calls.get())
    }
    @Test fun concurrentRefreshOnlyCallsSdkOnce() {
        val backend = Backend(); val session = GoogleDriveTokenSession(backend, { "synthetic-account" }, { 0L })
        val executor = Executors.newFixedThreadPool(8); val start = CountDownLatch(1)
        try {
            val futures = (1..16).map { executor.submit<String> { start.await(); session.accessToken() } }
            start.countDown(); futures.forEach { assertEquals("fake-1", it.get(5, TimeUnit.SECONDS)) }
            assertEquals(1, backend.calls.get())
        } finally { executor.shutdownNow() }
    }
    @Test fun stale401DoesNotClearRenewedToken() {
        val backend = Backend(); val session = GoogleDriveTokenSession(backend, { "synthetic-account" }, { 0L })
        val old = session.accessToken(); session.invalidate(old)
        assertEquals("fake-2", session.accessToken()); session.invalidate(old)
        assertEquals("fake-2", session.accessToken()); assertEquals(1, backend.clears.get())
    }
    @Test fun signOutDisablesRestoreAndRevokes() {
        var account: String? = "synthetic-account"; val backend = Backend()
        val session = GoogleDriveTokenSession(backend, { account }, { 0L })
        session.accessToken(); account = null; session.disconnect("synthetic-account")
        assertThrows(DriveAuthorizationRequired::class.java) { session.accessToken() }
        assertEquals(1, backend.revokes.get()); assertEquals(1, backend.clears.get())
    }
    @Test fun revokedAccountDoesNotReturnOldExpiredToken() {
        val backend = Backend(); var now = 0L
        val session = GoogleDriveTokenSession(backend, { "synthetic-account" }, { now })
        session.accessToken(); backend.fail = true; now = 300_000
        assertThrows(DriveAuthorizationRequired::class.java) { session.accessToken() }
    }
    @Test fun changedAccountDoesNotReuseOldToken() {
        var account = "first-test-account"; val backend = Backend()
        val session = GoogleDriveTokenSession(backend, { account }, { 0L })
        session.accessToken(); account = "second-test-account"
        assertEquals("fake-2", session.accessToken())
        assertThrows(IllegalArgumentException::class.java) { session.accept("first-test-account", grant()) }
    }
    @Test fun backwardClockForcesTokenRevalidation() {
        var now = 100L; val backend = Backend()
        val session = GoogleDriveTokenSession(backend, { "synthetic-account" }, { now })
        session.accessToken(); now = 90L; assertEquals("fake-2", session.accessToken())
    }
    @Test fun nativeResultGateRejectsReplayCancellationAndDoubleStart() {
        val gate = DriveAuthorizationAttempt<String>({ 0L })
        gate.begin("request"); assertThrows(IllegalStateException::class.java) { gate.begin("other") }
        assertEquals("request", gate.consume()); assertThrows(DriveAuthorizationRequired::class.java) { gate.consume() }
        gate.begin("cancel"); gate.cancel(); assertThrows(DriveAuthorizationRequired::class.java) { gate.consume() }
    }
    @Test fun nativeResultGateRejectsExpiredAndBackwardClock() {
        var now = 100L; val gate = DriveAuthorizationAttempt<String>({ now }, 10)
        gate.begin("expired"); now = 111; assertThrows(DriveAuthorizationRequired::class.java) { gate.consume() }
        gate.begin("backward"); now = 110; assertThrows(DriveAuthorizationRequired::class.java) { gate.consume() }
        assertFalse(gate.isPending())
    }
    @Test fun selectionRejectsUrlsSafAndMultipleIds() {
        listOf(null, "", "a,b", "content://file/id", "https://drive.google.com/a", "bad/id", "x".repeat(257)).forEach {
            assertThrows(IllegalArgumentException::class.java) { DriveSelection.pickedFile(it) }
        }
    }
    @Test fun bindingsRequireKnownDocumentDistinctIdsAndBothTrustedHashes() {
        assertThrows(IllegalArgumentException::class.java) { DriveSelection.apply(catalog, listOf(binding.copy(documentId = "unknown"))) }
        assertThrows(IllegalArgumentException::class.java) { DriveSelection.apply(catalog, listOf(binding.copy(indexId = binding.pdfId))) }
        assertThrows(IllegalArgumentException::class.java) { DriveSelection.apply(catalog, listOf(binding, binding)) }
        assertThrows(IllegalArgumentException::class.java) { DriveSelection.apply(catalog.copy(documents = listOf(descriptor.copy(pdfSha256 = null))), listOf(binding)) }
    }
    @Test fun bindingPreservesAssetsIdsRevisionAndHashes() {
        val assets = ru.fo6osik.workjournal.documents.DocumentSource.Assets("manuals/fixture.pdf", "manuals/fixture_search.json")
        val original = descriptor.copy(sources = listOf(assets), revision = "trusted-release")
        val actual = DriveSelection.apply(DocumentCatalogData(1, listOf(original)), listOf(binding)).documents.single()
        assertEquals(original.copy(sources = listOf(descriptor.sources.single(), assets)), actual)
    }
    @Test fun successfulHttpDownloadUsesSelectedIdsEtagsAndCatalogTrust() {
        val http = Http(); val transport = transport(http)
        val pair = transport.metadata(binding.pdfId, binding.indexId)
        assertNull(pair.revision); assertEquals(descriptor.pdfSha256, pair.pdfSha256)
        transport.openFile(binding.pdfId).use { assertArrayEquals(pdf, it.readBytes()) }
        transport.openFile(binding.indexId).use { assertArrayEquals(index, it.readBytes()) }
        assertEquals(listOf(null, null, "\"test-etag\"", "\"test-etag\""), http.etags)
        assertEquals(4, http.released); assertEquals(setOf(binding.pdfId, binding.indexId), http.ids.toSet())
    }
    @Test fun httpNeverOpensUnselectedFileOrCrossedPair() {
        val http = Http(); val transport = transport(http)
        assertThrows(DriveAccessDenied::class.java) { transport.metadata(binding.pdfId, "other") }
        assertThrows(DriveAccessDenied::class.java) { transport.openFile("other") }
        assertTrue(http.ids.isEmpty())
    }
    @Test fun downloadRequiresMetadataBeforeNetwork() {
        val http = Http(); assertThrows(IOException::class.java) { transport(http).openFile(binding.pdfId) }; assertTrue(http.ids.isEmpty())
    }
    @Test fun missingDeniedConflictAndServerErrorsCloseResponses() {
        listOf(404 to FileNotFoundException::class.java, 403 to DriveAccessDenied::class.java,
            412 to DriveRemoteConflict::class.java, 503 to IOException::class.java).forEach { (code, type) ->
            val http = Http(); http.statuses.add(code)
            assertThrows(type) { transport(http).metadata(binding.pdfId, binding.indexId) }; assertEquals(1, http.released)
        }
    }
    @Test fun unauthorizedRetriesOnceThenRequiresInteractiveAuthorization() {
        val http = Http(); http.statuses.addAll(listOf(401, 401))
        assertThrows(DriveAuthorizationRequired::class.java) { transport(http).metadata(binding.pdfId, binding.indexId) }
        assertEquals(2, http.released); assertEquals(2, tokens.invalidations)
    }
    @Test fun unauthorizedCanRecoverOnSingleSdkRefresh() {
        val http = Http(); http.statuses.addAll(listOf(401, 200, 200))
        transport(http).metadata(binding.pdfId, binding.indexId)
        assertEquals(1, tokens.invalidations); assertEquals(3, http.released)
    }
    @Test fun networkFailurePropagatesWithoutCreatingMetadata() {
        val http = Http(); http.failure = IOException("fake offline")
        assertThrows(IOException::class.java) { transport(http).metadata(binding.pdfId, binding.indexId) }
    }
    @Test fun malformedMetadataAndWrongFileIdAreRejected() {
        val http = Http(); http.bodyOverride = "not-json".toByteArray()
        assertThrows(IOException::class.java) { transport(http).metadata(binding.pdfId, binding.indexId) }
        http.bodyOverride = metadata("other", 1, "application/pdf")
        assertThrows(IllegalArgumentException::class.java) { transport(http).metadata(binding.pdfId, binding.indexId) }
        assertEquals(2, http.released)
    }
    @Test fun trashedNonDownloadableAndBadEtagAreRejected() {
        val bytes = metadata(binding.pdfId, 1, "application/pdf")
        assertThrows(FileNotFoundException::class.java) { parseDriveMetadata(String(bytes).replace("\"trashed\":false", "\"trashed\":true").toByteArray(), null) }
        assertThrows(DriveAccessDenied::class.java) { parseDriveMetadata(String(bytes).replace("\"canDownload\":true", "\"canDownload\":false").toByteArray(), null) }
        assertThrows(IOException::class.java) { parseDriveMetadata(bytes, "bad\r\nheader") }
    }
    @Test fun metadataSizeMimeAndPayloadBoundsAreChecked() {
        val http = Http()
        for (bytes in listOf(metadata(binding.pdfId, 0, "application/pdf"), metadata(binding.pdfId, 300L * 1024 * 1024, "application/pdf"), metadata(binding.pdfId, 1, "text/plain"))) {
            http.bodyOverride = bytes
            assertThrows(IllegalArgumentException::class.java) { transport(http).metadata(binding.pdfId, binding.indexId) }
        }
        http.bodyOverride = ByteArray(128 * 1024 + 1)
        assertThrows(IOException::class.java) { transport(http).metadata(binding.pdfId, binding.indexId) }
    }
    @Test fun truncatedOversizedAndDeclaredLengthMismatchCloseDownloads() {
        for (content in listOf(pdf.dropLast(1).toByteArray(), pdf + 1.toByte())) {
            val http = Http(); val transport = transport(http); transport.metadata(binding.pdfId, binding.indexId); http.pdfBytes = content
            assertThrows(IOException::class.java) { transport.openFile(binding.pdfId).use { it.readBytes() } }
            assertEquals(3, http.released)
        }
        val http = Http(); val transport = transport(http); transport.metadata(binding.pdfId, binding.indexId); http.declared = 1
        assertThrows(DriveRemoteConflict::class.java) { transport.openFile(binding.pdfId) }; assertEquals(3, http.released)
    }
    @Test fun changedAccountAbortsReadingAndClosesConnection() {
        var active = true; val http = Http(); val transport = transport(http) { if (!active) throw DriveAuthorizationRequired() }
        transport.metadata(binding.pdfId, binding.indexId); val stream = transport.openFile(binding.pdfId)
        active = false; assertThrows(DriveAuthorizationRequired::class.java) { stream.read() }; assertEquals(3, http.released)
    }
    @Test fun cryptoRoundTripKeepsBindingsOutOfPlaintext() {
        val file = File(temporary.newFolder(), "settings.enc"); val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val encrypted = DriveEncryptedFile(file) { key }; val content = "synthetic-private-file-binding".toByteArray()
        encrypted.write(content); assertArrayEquals(content, encrypted.read()); assertFalse(String(file.readBytes()).contains(String(content)))
        encrypted.clear(); assertNull(encrypted.read())
    }
    @Test fun keystoreLossDoesNotCreateKeyOrOverwriteCiphertext() {
        val file = File(temporary.newFolder(), "settings.enc"); val key = KeyGenerator.getInstance("AES").generateKey()
        DriveEncryptedFile(file) { key }.write("test-binding".toByteArray()); val previous = file.readBytes()
        val lost = DriveEncryptedFile(file) { create -> assertFalse(create); null }
        assertThrows(IOException::class.java) { lost.read() }; assertArrayEquals(previous, file.readBytes())
    }
    @Test fun modifiedCiphertextIsRejected() {
        val file = File(temporary.newFolder(), "settings.enc"); val key = KeyGenerator.getInstance("AES").generateKey()
        val encrypted = DriveEncryptedFile(file) { key }; encrypted.write("test-binding".toByteArray())
        val data = file.readBytes(); data[data.lastIndex] = (data.last().toInt() xor 1).toByte(); file.writeBytes(data)
        assertThrows(IOException::class.java) { encrypted.read() }
    }
    @Test fun failedEncryptionLeavesPreviousSettingsIntact() {
        val file = File(temporary.newFolder(), "settings.enc"); val key = KeyGenerator.getInstance("AES").generateKey()
        DriveEncryptedFile(file) { key }.write("test-binding".toByteArray()); val previous = file.readBytes()
        assertThrows(IOException::class.java) { DriveEncryptedFile(file) { null }.write("new-test-binding".toByteArray()) }
        assertArrayEquals(previous, file.readBytes())
    }
    @Test fun separateCryptoInstancesSerializeAtomicWrites() {
        val file = File(temporary.newFolder(), "settings.enc"); val key = KeyGenerator.getInstance("AES").generateKey()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (1..20).map { n -> executor.submit { DriveEncryptedFile(file) { key }.write("binding-$n".toByteArray()) } }
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
            assertTrue(String(DriveEncryptedFile(file) { key }.read()!!).startsWith("binding-"))
            assertEquals(listOf("settings.enc"), file.parentFile!!.list()!!.toList())
        } finally { executor.shutdownNow() }
    }
    @Test fun forceRefreshVerifiesRemoteDespiteMatchingLocalHashesAndPreservesOfflineCopy() = runBlocking<Unit> {
        val store = LocalDocumentStore(temporary.newFolder(), DocumentIntegrityValidator { _, _ -> })
        val http = Http(); val transport = transport(http)
        val adapter = GoogleDriveDocumentSource(transport) { }
        val repository = DefaultDocumentRepository(catalog, store, listOf(adapter))
        val previous = repository.download("fixture")
        http.pdfBytes = "wrong-same-size".toByteArray().copyOf(pdf.size)
        try { repository.download("fixture", forceRefresh = true); fail("Corrupt Drive copy accepted") } catch (_: IllegalArgumentException) { }
        store.acquire("fixture")!!.use { assertEquals(previous.pdfFile, it.pdfFile); assertArrayEquals(pdf, it.pdfFile.readBytes()) }
        http.failure = IOException("fake offline")
        repository.open("fixture").use { assertArrayEquals(index, it.searchIndexFile.readBytes()) }
        previous.close()
    }
    @Test fun closingViewerLeaseDoesNotWaitForAnotherDownload() = runBlocking<Unit> {
        val store = LocalDocumentStore(temporary.newFolder(), DocumentIntegrityValidator { _, _ -> })
        val previous = store.install(descriptor, { ByteArrayInputStream(pdf) }, { ByteArrayInputStream(index) })
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val installing = executor.submit {
                runBlocking {
                    store.install(descriptor, {
                        entered.countDown()
                        check(release.await(5, TimeUnit.SECONDS))
                        ByteArrayInputStream(pdf)
                    }, { ByteArrayInputStream(index) }, forceRefresh = true).close()
                }
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            executor.submit { previous.close() }.get(1, TimeUnit.SECONDS)
            release.countDown(); installing.get(5, TimeUnit.SECONDS)
        } finally { release.countDown(); executor.shutdownNow(); previous.close() }
    }

    @Test fun callbackFromPreviousAttemptCannotConsumeNewAttempt() {
        val gate = DriveAuthorizationAttempt<String>({ 0L })
        val previous = gate.begin("old"); assertEquals("old", gate.consume(previous))
        val current = gate.begin("new")
        assertThrows(DriveAuthorizationRequired::class.java) { gate.consume(previous) }
        assertTrue(gate.isPending()); assertEquals("new", gate.consume(current))
    }

}
