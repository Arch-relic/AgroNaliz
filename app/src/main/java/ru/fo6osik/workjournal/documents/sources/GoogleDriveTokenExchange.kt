package ru.fo6osik.workjournal.documents.sources

/**
 * Token exchange boundary for the authorization-code + PKCE flow.
 * Implementations must call Google's HTTPS token endpoint, authenticate the public
 * client using client_id (not a bundled client secret), and submit the exact
 * code_verifier from the one-time request.
 *
 * Never log the request or response, and never persist an authorization code.
 * This interface does not itself perform network requests or store tokens.
 */
internal interface GoogleDriveTokenExchange {
    suspend fun exchange(
        clientId: String,
        request: GoogleDriveOAuthAttempt.ExchangeRequest
    ): Tokens

    data class Tokens(
        val accessToken: String,
        val expiresInSeconds: Long,
        val refreshToken: String?
    ) {
        init {
            require(accessToken.isNotBlank()) { "Empty access token" }
            require(expiresInSeconds > 0) { "Invalid token lifetime" }
            require(refreshToken == null || refreshToken.isNotBlank()) { "Empty refresh token" }
        }

        override fun toString(): String = "Tokens(redacted)"
    }
}
