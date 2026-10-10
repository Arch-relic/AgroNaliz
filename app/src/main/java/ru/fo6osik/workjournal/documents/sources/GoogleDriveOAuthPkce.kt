package ru.fo6osik.workjournal.documents.sources

import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

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
    // Pure JVM implementation: java.util.Base64 is unavailable below Android API 26.
    private fun base64Url(bytes: ByteArray): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val out = StringBuilder((bytes.size * 4 + 2) / 3)
        var index = 0
        while (index < bytes.size) {
            val first = bytes[index++].toInt() and 255
            val second = if (index < bytes.size) bytes[index++].toInt() and 255 else -1
            val third = if (index < bytes.size) bytes[index++].toInt() and 255 else -1
            out.append(alphabet[first ushr 2])
            out.append(alphabet[((first and 3) shl 4) or (if (second < 0) 0 else second ushr 4)])
            if (second >= 0) out.append(alphabet[((second and 15) shl 2) or (if (third < 0) 0 else third ushr 6)])
            if (third >= 0) out.append(alphabet[third and 63])
        }
        return out.toString()
    }
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
