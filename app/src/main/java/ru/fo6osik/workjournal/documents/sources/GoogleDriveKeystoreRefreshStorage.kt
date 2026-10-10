package ru.fo6osik.workjournal.documents.sources

/** Adapter to keep the Android Keystore implementation behind a testable interface. */
internal class GoogleDriveKeystoreRefreshStorage(
    private val store: GoogleDriveCredentialStore
) : GoogleDriveTokenSession.RefreshCredentialStorage {
    override fun read(): String? = store.readRefreshToken()
    override fun save(token: String) = store.saveRefreshToken(token)
    override fun clear() = store.clear()
}
