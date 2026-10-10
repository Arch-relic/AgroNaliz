package ru.fo6osik.workjournal.documents.sources

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted, no-backup storage for OAuth refresh credentials.
 *
 * Key is non-exportable in Android Keystore; ciphertext is kept under
 * noBackupFilesDir. A missing/invalidated key must force re-authorization.
 * Not intended for multi-process concurrent access.
 */
internal class GoogleDriveCredentialStore(context: Context) {
    private val file = File(context.applicationContext.noBackupFilesDir, "google_drive_refresh_token.enc")
    private val alias = "agronaliz.google_drive.refresh.v1"

    @Synchronized
    fun saveRefreshToken(token: String) {
        require(token.isNotBlank()) { "Empty refresh token" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        require(iv.size == 12) { "Unexpected GCM IV length" }
        val temporary = File(file.parentFile, file.name + ".tmp")
        try {
            temporary.outputStream().use { stream ->
                stream.write(byteArrayOf(1))
                stream.write(iv)
                stream.write(encrypted)
                stream.flush()
                (stream as java.io.FileOutputStream).fd.sync()
            }
            if (!temporary.renameTo(file)) throw IOException("Could not commit encrypted credential")
        } finally {
            temporary.delete()
        }
    }

    @Synchronized
    fun readRefreshToken(): String? {
        if (!file.exists()) return null
        if (file.length() > 16 * 1024) throw IOException("Encrypted credential exceeds size limit")
        val bytes = file.readBytes()
        if (bytes.size < 30 || bytes[0] != 1.toByte()) throw IOException("Invalid encrypted credential format")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        return String(cipher.doFinal(bytes.copyOfRange(13, bytes.size)), Charsets.UTF_8)
    }

    @Synchronized
    fun clear() {
        if (file.exists() && !file.delete()) throw IOException("Could not clear credential")
        File(file.parentFile, file.name + ".tmp").delete()
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
