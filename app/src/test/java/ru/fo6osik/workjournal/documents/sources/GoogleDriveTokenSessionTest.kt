package ru.fo6osik.workjournal.documents.sources

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GoogleDriveTokenSessionTest {
    private class FakeCredentials : GoogleDriveTokenSession.RefreshCredentialStorage {
        var token: String? = null
        override fun read(): String? = token
        override fun save(token: String) { this.token = token }
        override fun clear() { token = null }
    }

    @Test fun validAccessTokenDoesNotRefresh() = runBlocking {
        val credentials = FakeCredentials()
        var refreshes = 0
        var now = 1000L
        val session = GoogleDriveTokenSession(credentials, {
            refreshes++
            GoogleDriveTokenExchange.Tokens("new", 3600, null)
        }, { now })
        session.accept(GoogleDriveTokenExchange.Tokens("initial", 3600, "refresh"))
        assertEquals("initial", session.accessToken())
        now += 1000
        assertEquals("initial", session.accessToken())
        assertEquals(0, refreshes)
        assertEquals("refresh", credentials.token)
    }

    @Test fun expiryRefreshesAndRotatesCredential() = runBlocking {
        val credentials = FakeCredentials()
        var now = 0L
        var refreshes = 0
        val session = GoogleDriveTokenSession(credentials, { token ->
            assertEquals("original", token)
            refreshes++
            GoogleDriveTokenExchange.Tokens("renewed", 3600, "rotated")
        }, { now })
        session.accept(GoogleDriveTokenExchange.Tokens("initial", 120, "original"))
        now = 61_000
        assertEquals("renewed", session.accessToken())
        assertEquals("rotated", credentials.token)
        assertEquals("renewed", session.accessToken())
        assertEquals(1, refreshes)
    }

    @Test fun concurrentRefreshIsDeduplicated() = runBlocking {
        val credentials = FakeCredentials().apply { token = "refresh" }
        var refreshes = 0
        val session = GoogleDriveTokenSession(credentials, {
            refreshes++
            GoogleDriveTokenExchange.Tokens("renewed", 3600, null)
        }, { 0L })
        val results = (1..20).map { async { session.accessToken() } }.awaitAll()
        assertTrue(results.all { it == "renewed" })
        assertEquals(1, refreshes)
    }

    @Test fun signOutClearsCredentialsAndInvalidatesAccess() = runBlocking {
        val credentials = FakeCredentials()
        val session = GoogleDriveTokenSession(credentials, {
            GoogleDriveTokenExchange.Tokens("renewed", 3600, null)
        }, { 0L })
        session.accept(GoogleDriveTokenExchange.Tokens("initial", 3600, "refresh"))
        session.signOut()
        assertNull(credentials.token)
        try {
            session.accessToken()
            fail("Expected reauthorization")
        } catch (_: IllegalStateException) { }
    }

    @Test fun missingRefreshCredentialRequiresAuthorization() = runBlocking {
        val session = GoogleDriveTokenSession(FakeCredentials(), {
            fail("Should not refresh without credentials")
            GoogleDriveTokenExchange.Tokens("unused", 3600, null)
        })
        try {
            session.accessToken()
            fail("Expected reauthorization")
        } catch (_: IllegalStateException) { }
    }
}
