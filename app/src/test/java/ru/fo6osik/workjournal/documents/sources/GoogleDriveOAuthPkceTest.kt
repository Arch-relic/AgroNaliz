package ru.fo6osik.workjournal.documents.sources

import org.junit.Assert.*
import org.junit.Test
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Base64

class GoogleDriveOAuthPkceTest {
    private fun parameters(url: String): Map<String, String> = url.substringAfter('?').split('&').associate {
        val (key, value) = it.split('=', limit = 2)
        URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
    }

    @Test fun requestsOnlySelectedFilesScopeAndS256() {
        val request = GoogleDriveOAuthPkce.prepare("example-client.apps.googleusercontent.com", "example.app:/oauth2redirect")
        val params = parameters(request.authorizationUrl)
        assertTrue(request.authorizationUrl.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"))
        assertEquals(GoogleDriveOAuthPkce.DRIVE_FILE_SCOPE, params["scope"])
        assertEquals("code", params["response_type"])
        assertEquals("S256", params["code_challenge_method"])
        assertEquals(request.state, params["state"])
        assertEquals(request.redirectUri, params["redirect_uri"])
        assertEquals(43, request.codeVerifier.length)
        val expected = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(request.codeVerifier.toByteArray(Charsets.US_ASCII))
        )
        assertEquals(expected, params["code_challenge"])
        assertFalse(params.containsKey("client_secret"))
    }

    @Test fun eachRequestHasIndependentUnpredictableValues() {
        val first = GoogleDriveOAuthPkce.prepare("client", "app:/callback")
        val second = GoogleDriveOAuthPkce.prepare("client", "app:/callback")
        assertNotEquals(first.state, second.state)
        assertNotEquals(first.codeVerifier, second.codeVerifier)
    }

    @Test fun rejectsMismatchedCallbackStateOrRedirect() {
        val request = GoogleDriveOAuthPkce.prepare("client", "app:/callback")
        request.validateCallback(request.state, "app:/callback")
        for ((state, redirect) in listOf(
            "wrong" to "app:/callback",
            null to "app:/callback",
            request.state to "app:/other"
        )) {
            try {
                request.validateCallback(state, redirect)
                fail("Expected rejection")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun rejectsMissingClientAndInvalidRedirect() {
        try {
            GoogleDriveOAuthPkce.prepare("", "app:/callback")
            fail("Expected rejection")
        } catch (_: IllegalArgumentException) { }
        try {
            GoogleDriveOAuthPkce.prepare("client", "app:/callback#fragment")
            fail("Expected rejection")
        } catch (_: IllegalArgumentException) { }
    }
}
