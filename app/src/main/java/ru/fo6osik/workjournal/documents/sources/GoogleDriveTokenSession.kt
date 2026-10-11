package ru.fo6osik.workjournal.documents.sources

import java.io.IOException

internal const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
internal class DriveAuthorizationRequired : IOException("Google Drive authorization required")

/** Tokens never leave process memory; refresh credentials belong to Google Play services. */
internal class DriveGrant(val accessToken: String, val scopes: Set<String>, val pickedIds: String? = null) {
    init {
        require(accessToken.isNotBlank() && accessToken.length <= 16384 && accessToken.none { it.isWhitespace() || it.code < 32 }) { "Invalid authorization result" }
        require(scopes == setOf(DRIVE_FILE_SCOPE)) { "Unexpected authorization scopes" }
    }
    override fun toString() = "DriveGrant(redacted)"
}

internal interface DriveAuthorizationBackend {
    fun authorize(account: String): DriveGrant
    fun clearToken(token: String)
    fun revoke(account: String)
}
internal interface DriveTokenProvider {
    fun accessToken(): String
    fun invalidate(rejectedToken: String)
}

/** Blocking worker-thread session; refresh/invalidation/sign-out are serialized. */
internal class GoogleDriveTokenSession(
    private val backend: DriveAuthorizationBackend,
    private val currentAccount: () -> String?,
    private val elapsedMillis: () -> Long,
) : DriveTokenProvider {
    private var token: String? = null
    private var account: String? = null
    private var acceptedAt = 0L
    private val lock = Any()

    fun accept(accountName: String, grant: DriveGrant) = synchronized(lock) {
        require(currentAccount() == accountName) { "Account changed" }
        account = accountName
        token = grant.accessToken
        acceptedAt = elapsedMillis()
    }

    override fun accessToken(): String = synchronized(lock) {
        val selected = currentAccount() ?: throw DriveAuthorizationRequired()
        val now = elapsedMillis()
        val age = now - acceptedAt
        if (account == selected && token != null && now >= acceptedAt && age in 0 until 300_000L) return@synchronized token!!
        token = null
        val grant = backend.authorize(selected) // SDK manages expiry/refresh, never a custom refresh grant.
        if (currentAccount() != selected) throw DriveAuthorizationRequired()
        accept(selected, grant)
        grant.accessToken
    }

    override fun invalidate(rejectedToken: String) = synchronized(lock) {
        // Another concurrent 401 must not discard a token already renewed by a peer.
        if (token == rejectedToken) {
            token = null
            backend.clearToken(rejectedToken)
        }
    }

    fun disconnect(accountName: String?) = synchronized(lock) {
        val old = token
        token = null
        account = null
        if (accountName != null) backend.revoke(accountName)
        if (old != null) backend.clearToken(old)
    }
}
