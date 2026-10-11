package ru.fo6osik.workjournal.documents.sources

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Private, no-backup account/file bindings. No OAuth token is stored by the app. */
internal class GoogleDriveCredentialStore(context: Context) {
    private val alias = "agronaliz.google_drive.bindings.v2"
    private val encrypted = DriveEncryptedFile(File(context.applicationContext.noBackupFilesDir, "google_drive_bindings.enc")) { create ->
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = store.getKey(alias, null) as? SecretKey
        if (key != null || !create) key else {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).build())
            }.generateKey()
        }
    }
    fun read(): DrivePrivateState {
        val bytes = encrypted.read() ?: return DrivePrivateState()
        try {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            require(json.getInt("schema") == 1)
            val rows = json.getJSONArray("bindings")
            val bindings = (0 until rows.length()).map { position ->
                val row = rows.getJSONObject(position)
                DriveBinding(row.getString("documentId"), DriveSelection.fileId(row.getString("pdfId")),
                    DriveSelection.fileId(row.getString("indexId")), nullable(row, "pdfVersion"), nullable(row, "indexVersion"),
                    if (row.isNull("lastUpdated")) null else row.getLong("lastUpdated"))
            }
            require(bindings.size <= 20)
            return DrivePrivateState(nullable(json, "account"), nullable(json, "pendingRevoke"), bindings)
        } catch (_: Exception) { throw IOException("Drive settings are invalid; reconnect required") }
    }
    fun write(state: DrivePrivateState) {
        val json = JSONObject().put("schema", 1).put("account", state.account ?: JSONObject.NULL)
            .put("pendingRevoke", state.pendingRevoke ?: JSONObject.NULL)
        val rows = JSONArray()
        state.bindings.forEach { rows.put(JSONObject().put("documentId", it.documentId).put("pdfId", it.pdfId)
            .put("indexId", it.indexId).put("pdfVersion", it.pdfVersion ?: JSONObject.NULL)
            .put("indexVersion", it.indexVersion ?: JSONObject.NULL).put("lastUpdated", it.lastUpdated ?: JSONObject.NULL)) }
        json.put("bindings", rows)
        encrypted.write(json.toString().toByteArray(Charsets.UTF_8))
    }
    fun clear() {
        encrypted.clear()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
    }
    private fun nullable(json: JSONObject, field: String): String? = if (json.isNull(field)) null else json.getString(field)
}

/** Atomic ciphertext replacement; missing key during read never creates a replacement key. */
internal class DriveEncryptedFile(private val file: File, private val key: (create: Boolean) -> SecretKey?) {
    companion object { private val locks = ConcurrentHashMap<String, Any>() }
    private val lock = locks.getOrPut(file.absoluteFile.normalize().path) { Any() }
    private val aad = "AgroNaliz Drive bindings v1".toByteArray(Charsets.US_ASCII)
    fun read(): ByteArray? = synchronized(lock) {
        if (!file.exists()) return@synchronized null
        try {
            if (file.length() !in 30..262144) throw IOException("Invalid encrypted settings")
            val bytes = file.readBytes()
            if (bytes[0] != 1.toByte()) throw IOException("Invalid encrypted settings")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(false) ?: throw IOException("Drive settings key unavailable"),
                GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            cipher.updateAAD(aad)
            cipher.doFinal(bytes.copyOfRange(13, bytes.size))
        } catch (_: Exception) { throw IOException("Drive settings cannot be decrypted; reconnect required") }
    }
    fun write(bytes: ByteArray) = synchronized(lock) {
        require(bytes.isNotEmpty() && bytes.size <= 262000) { "Invalid Drive settings size" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(true) ?: throw IOException("Drive settings key unavailable"))
        cipher.updateAAD(aad)
        val encrypted = cipher.doFinal(bytes)
        require(cipher.iv.size == 12)
        file.parentFile?.let { if (!it.isDirectory && !it.mkdirs()) throw IOException("Cannot create private settings directory") }
        val temporary = File(file.parentFile, file.name + ".tmp-" + UUID.randomUUID())
        try {
            FileOutputStream(temporary).use { output ->
                output.write(byteArrayOf(1)); output.write(cipher.iv); output.write(encrypted); output.fd.sync()
            }
            if (!temporary.renameTo(file)) throw IOException("Cannot commit Drive settings")
        } finally { temporary.delete() }
    }
    fun clear() = synchronized(lock) {
        if (file.exists() && !file.delete()) throw IOException("Cannot clear Drive settings")
    }
}
