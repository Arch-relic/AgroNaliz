package ru.fo6osik.workjournal.documents.sources

/**
 * One-time in-memory authorization attempt.
 *
 * Not persistent by design: process death requires starting a new login. No authorization
 * codes or PKCE verifier are logged or saved in preferences, Room, or backup.
 * The caller must supply the callback URI path separately from its encoded query.
 */
internal class GoogleDriveOAuthAttempt(
    private var pending: GoogleDriveOAuthPkce.PendingAuthorization?
) {
    @Synchronized
    fun consumeCallback(redirectUri: String, encodedQuery: String): ExchangeRequest {
        // Invalidate before parsing: even a malicious callback cannot be replayed.
        val attempt = pending ?: throw IllegalStateException("OAuth attempt already consumed")
        pending = null
        val result = GoogleDriveOAuthCallback.parse(attempt, redirectUri, encodedQuery)
        return when (result) {
            is GoogleDriveOAuthCallback.Result.Code -> ExchangeRequest(
                code = result.authorizationCode,
                codeVerifier = attempt.codeVerifier,
                redirectUri = attempt.redirectUri
            )
            is GoogleDriveOAuthCallback.Result.Denied ->
                throw AuthorizationDeniedException(result.error)
        }
    }

    @Synchronized
    fun cancel() {
        pending = null
    }

    data class ExchangeRequest(
        val code: String,
        val codeVerifier: String,
        val redirectUri: String
    ) {
        override fun toString(): String = "ExchangeRequest(redacted)"
    }

    class AuthorizationDeniedException(val reason: String) :
        Exception("Google authorization was denied")
}
