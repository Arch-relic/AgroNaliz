package ru.fo6osik.workjournal.documents.sources

import org.junit.Assert.*
import org.junit.Test
import java.net.URLEncoder

class GoogleDriveOAuthAttemptTest {
    private fun pending() = GoogleDriveOAuthPkce.prepare("test-client", "app.example:/callback")
    private fun callback(state: String, code: String = "auth-code") =
        "state=${URLEncoder.encode(state, "UTF-8")}&code=${URLEncoder.encode(code, "UTF-8")}"

    @Test fun callbackCanBeConsumedExactlyOnce() {
        val pending = pending()
        val attempt = GoogleDriveOAuthAttempt(pending)
        val request = attempt.consumeCallback(pending.redirectUri, callback(pending.state))
        assertEquals("auth-code", request.code)
        assertEquals(pending.codeVerifier, request.codeVerifier)
        assertEquals(pending.redirectUri, request.redirectUri)
        assertFalse(request.toString().contains("auth-code"))
        assertFalse(request.toString().contains(pending.codeVerifier))
        rejectsState { attempt.consumeCallback(pending.redirectUri, callback(pending.state)) }
    }

    @Test fun invalidCallbackAlsoConsumesAttempt() {
        val pending = pending()
        val attempt = GoogleDriveOAuthAttempt(pending)
        try {
            attempt.consumeCallback(pending.redirectUri, callback("wrong"))
            fail("Expected rejection")
        } catch (_: IllegalArgumentException) { }
        rejectsState { attempt.consumeCallback(pending.redirectUri, callback(pending.state)) }
    }

    @Test fun denialConsumesAttemptWithoutReturningCode() {
        val pending = pending()
        val attempt = GoogleDriveOAuthAttempt(pending)
        try {
            attempt.consumeCallback(pending.redirectUri, "state=${pending.state}&error=access_denied")
            fail("Expected denial")
        } catch (error: GoogleDriveOAuthAttempt.AuthorizationDeniedException) {
            assertEquals("access_denied", error.reason)
        }
        rejectsState { attempt.consumeCallback(pending.redirectUri, callback(pending.state)) }
    }

    @Test fun cancellationPreventsCallback() {
        val pending = pending()
        val attempt = GoogleDriveOAuthAttempt(pending)
        attempt.cancel()
        rejectsState { attempt.consumeCallback(pending.redirectUri, callback(pending.state)) }
    }

    private fun rejectsState(block: () -> Unit) {
        try {
            block()
            fail("Expected already consumed")
        } catch (_: IllegalStateException) { }
    }
}
