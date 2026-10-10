package ru.fo6osik.workjournal.documents.sources

import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * OAuth authorization-code + PKCE request preparation only.
 * No tokens, client secret, browser launch or callback registration are handled here.
 */
internal object GoogleDriveOAuthPkce {
    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    private const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
    private val random = SecureRandom()

    data class PendingAuthorization(
        val authorizationUrl: String,
        val state: String,
        val codeVerifier: String,
        val redirectUri: String
    ) {
        /** Validate a callback before exchanging its authorization code. */
        fun validateCallback(returnedState: String?, returnedRedirectUri: String) {
            require(returnedRedirectUri == redirectUri) { "Unexpected OAuth redirect URI" }
            require(returnedState != null && MessageDigest.isEqual(
                returnedState.toByteArray(Charsets.UTF_8), state.toByteArray(Charsets.UTF_8)
            )) { "OAuth state mismatch" }
        }
    }

    fun prepare(clientId: String, redirectUri: String): PendingAuthorization {
        require(clientId.isNotBlank() && !clientId.contains(' ')) { "OAuth client ID is required" }
        require(redirectUri.isNotBlank() && !redirectUri.contains('#')) { "Invalid OAuth redirect URI" }
        val verifier = randomUrlSafe(32)
        val state = randomUrlSafe(32)
        val challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        val params = linkedMapOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to DRIVE_FILE_SCOPE,
            "state" to state,
            "code_challenge" to challenge,
            "code_challenge_method" to "S256"
        )
        val url = AUTH_ENDPOINT + "?" + params.entries.joinToString("&") {
            encode(it.key) + "=" + encode(it.value)
        }
        return PendingAuthorization(url, state, verifier, redirectUri)
    }

    private fun randomUrlSafe(size: Int): String = ByteArray(size).also(random::nextBytes).let(::base64Url)
    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
