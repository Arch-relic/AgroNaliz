package ru.fo6osik.workjournal.documents.sources

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory access-token lifecycle. Never persists or logs access tokens.
 * Refresh operations are serialized so concurrent requests cannot rotate the
 * same refresh credential independently.
 */
internal class GoogleDriveTokenSession(
    private val credentials: RefreshCredentialStorage,
    private val refresher: suspend (String) -> GoogleDriveTokenExchange.Tokens,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    interface RefreshCredentialStorage {
        fun read(): String?
        fun save(token: String)
        fun clear()
    }

    private val mutex = Mutex()
    private var access: String? = null
    private var expiresAtMillis: Long = 0
    private val earlyRefreshMillis = 60_000L

    suspend fun accept(tokens: GoogleDriveTokenExchange.Tokens) = mutex.withLock {
        tokens.refreshToken?.let(credentials::save)
        setAccess(tokens)
    }

    suspend fun accessToken(): String = mutex.withLock {
        val current = access
        if (current != null && expiresAtMillis - nowMillis() > earlyRefreshMillis) {
            return@withLock current
        }
        val refresh = credentials.read() ?: throw IllegalStateException("Google Drive authorization required")
        val renewed = refresher(refresh)
        renewed.refreshToken?.let(credentials::save)
        setAccess(renewed)
        access ?: throw IllegalStateException("Missing access token")
    }

    suspend fun invalidateAccessToken() = mutex.withLock {
        access = null
        expiresAtMillis = 0
    }

    suspend fun signOut() = mutex.withLock {
        credentials.clear()
        access = null
        expiresAtMillis = 0
    }

    private fun setAccess(tokens: GoogleDriveTokenExchange.Tokens) {
        val lifetime = tokens.expiresInSeconds.coerceAtMost(Long.MAX_VALUE / 1000L) * 1000L
        access = tokens.accessToken
        expiresAtMillis = if (Long.MAX_VALUE - nowMillis() < lifetime) Long.MAX_VALUE
        else nowMillis() + lifetime
    }
}
