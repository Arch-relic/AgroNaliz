package ru.fo6osik.workjournal.documents.sources

import org.junit.Assert.*
import org.junit.Test
import java.net.URLEncoder

class GoogleDriveOAuthCallbackTest {
    private fun pending() = GoogleDriveOAuthPkce.prepare("test-client", "app.example:/oauth2redirect")
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    @Test fun validCodeIsReturnedOnlyAfterStateAndRedirectValidation() {
        val pending = pending()
        val result = GoogleDriveOAuthCallback.parse(
            pending, pending.redirectUri, "state=${encode(pending.state)}&code=abc%2F123"
        )
        assertEquals("abc/123", (result as GoogleDriveOAuthCallback.Result.Code).authorizationCode)
    }

    @Test fun accessDeniedIsExplicitResult() {
        val pending = pending()
        val result = GoogleDriveOAuthCallback.parse(
            pending, pending.redirectUri, "error=access_denied&state=${encode(pending.state)}"
        )
        assertEquals("access_denied", (result as GoogleDriveOAuthCallback.Result.Denied).error)
    }

    @Test fun invalidStateAndRedirectAreRejected() {
        val pending = pending()
        rejects { GoogleDriveOAuthCallback.parse(pending, pending.redirectUri, "state=wrong&code=abc") }
        rejects { GoogleDriveOAuthCallback.parse(pending, "app.example:/other", "state=${encode(pending.state)}&code=abc") }
        rejects { GoogleDriveOAuthCallback.parse(pending, pending.redirectUri, "code=abc") }
    }

    @Test fun duplicateAndAmbiguousParametersAreRejected() {
        val pending = pending()
        val state = encode(pending.state)
        rejects { GoogleDriveOAuthCallback.parse(pending, pending.redirectUri, "state=$state&state=$state&code=abc") }
        rejects { GoogleDriveOAuthCallback.parse(pending, pending.redirectUri, "state=$state&code=abc&code=def") }
        rejects { GoogleDriveOAuthCallback.parse(pending, pending.redirectUri, "state=$state&code=abc&error=access_denied") }
        rejects { GoogleDriveOAuthCallback.parse(pending, pending.redirectUri, "state=$state") }
    }

    private fun rejects(block: () -> Unit) {
        try {
            block()
            fail("Expected callback rejection")
        } catch (_: IllegalArgumentException) { }
    }
}
