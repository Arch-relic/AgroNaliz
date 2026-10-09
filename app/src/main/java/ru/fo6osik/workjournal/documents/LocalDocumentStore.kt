package ru.fo6osik.workjournal.documents

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** All public file operations run on IO. Only this dedicated directory is ever modified. */
class LocalDocumentStore(
    private val root: File,
    private val validator: DocumentIntegrityValidator = AndroidDocumentIntegrityValidator()
) {
    constructor(context: Context) : this(File(context.noBackupFilesDir, "documents"))
    internal val storageKey: String = root.absoluteFile.normalize().path
    private val coordinator = coordinators.getOrPut(storageKey) { Coordinator() }
    private class Coordinator { val leases = mutableMapOf<String, Int>() }
    companion object { private val coordinators = ConcurrentHashMap<String, Coordinator>() }

    suspend fun acquire(id: String): LocalDocument? = io {
        synchronized(coordinator) { acquireLocked(id) }
    }

    suspend fun install(
        descriptor: DocumentDescriptor,
        pdf: () -> InputStream,
        index: () -> InputStream
    ): LocalDocument = io {
        val coroutine = currentCoroutineContext()
        DocumentCatalog.validate(DocumentCatalogData(1, listOf(descriptor)))
        synchronized(coordinator) {
            coroutine.ensureActive()
            val directory = directory(descriptor.documentId)
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create document directory")
            val generation = UUID.randomUUID().toString()
            val staging = File(directory, ".tmp-$generation")
            if (!staging.mkdir()) throw IOException("Cannot create staging directory")
            val pointerTemp = File(directory, ".current-$generation")
            try {
                val pdfFile = File(staging, "document.pdf")
                val indexFile = File(staging, "search.json")
                val pdfHash = copy(pdf, pdfFile) { coroutine.ensureActive() }
                val indexHash = copy(index, indexFile) { coroutine.ensureActive() }
                require(descriptor.pdfSha256 == null || descriptor.pdfSha256 == pdfHash) { "PDF SHA-256 mismatch" }
                require(descriptor.searchIndexSha256 == null || descriptor.searchIndexSha256 == indexHash) { "Index SHA-256 mismatch" }
                // Hashes must come from the verified pair metadata, never from this download.
                if (descriptor.pdfSha256 == null || descriptor.searchIndexSha256 == null) {
                    throw UnverifiedDocumentException()
                }
                validator.validate(pdfFile, indexFile)
                val metadata = Properties().apply {
                    setProperty("pdfSha256", pdfHash); setProperty("searchIndexSha256", indexHash)
                    descriptor.revision?.let { setProperty("revision", it) }
                }
                FileOutputStream(File(staging, "metadata.properties")).use { metadata.store(it, null); it.fd.sync() }
                coroutine.ensureActive()
                val committed = File(directory, generation)
                if (!staging.renameTo(committed)) throw IOException("Cannot commit document generation")
                FileOutputStream(pointerTemp).use { it.write(generation.toByteArray(Charsets.UTF_8)); it.fd.sync() }
                // Same filesystem rename atomically replaces the pointer; old generations stay intact.
                coroutine.ensureActive()
                if (!pointerTemp.renameTo(File(directory, "current"))) throw IOException("Cannot activate document generation")
                lease(descriptor.documentId, committed, descriptor.revision, pdfHash, indexHash)
            } finally {
                staging.deleteRecursively()
                pointerTemp.delete()
            }
        }
    }

    /** Explicit cleanup only: never touches the current generation or an active viewer lease. */
    suspend fun pruneUnused(id: String): Int = io {
        synchronized(coordinator) {
            val directory = directory(id)
            val current = currentGeneration(directory)
            // Refuse cleanup if the active pair cannot be verified; preserve recovery copies.
            acquireLocked(id)?.close() ?: return@synchronized 0
            var deleted = 0
            directory.listFiles()?.filter { it.isDirectory && generationPattern.matches(it.name) }?.forEach { file ->
                if (file.name != current && coordinator.leases[file.absolutePath].orZero() == 0) {
                    if (!file.deleteRecursively()) throw IOException("Cannot remove unused generation")
                    deleted++
                }
            }
            deleted
        }
    }

    // withContext may discard its result on cancellation when returning to the caller.
    // Release any acquired file lease in that case; closing a lease performs no disk IO.
    private suspend fun <T> io(block: suspend () -> T): T {
        var result: LocalDocument? = null
        try {
            return withContext(Dispatchers.IO) { block().also { if (it is LocalDocument) result = it } }
        } catch (cancelled: CancellationException) {
            result?.close()
            throw cancelled
        }
    }

    private fun acquireLocked(id: String): LocalDocument? {
        val directory = directory(id)
        val generation = currentGeneration(directory) ?: return null
        val folder = File(directory, generation)
        val pdf = File(folder, "document.pdf"); val index = File(folder, "search.json")
        val metadata = Properties().apply { File(folder, "metadata.properties").inputStream().use { load(it) } }
        val pdfHash = metadata.getProperty("pdfSha256") ?: throw IOException("Missing PDF checksum")
        val indexHash = metadata.getProperty("searchIndexSha256") ?: throw IOException("Missing index checksum")
        if (hash(pdf) != pdfHash || hash(index) != indexHash) throw IOException("Local document integrity failure")
        return lease(id, folder, metadata.getProperty("revision"), pdfHash, indexHash)
    }

    private fun lease(id: String, folder: File, revision: String?, pdfHash: String, indexHash: String): LocalDocument {
        coordinator.leases[folder.absolutePath] = coordinator.leases[folder.absolutePath].orZero() + 1
        return LocalDocument(id, revision, File(folder, "document.pdf"), File(folder, "search.json"), pdfHash, indexHash) {
            synchronized(coordinator) {
                val count = coordinator.leases[folder.absolutePath].orZero() - 1
                if (count <= 0) coordinator.leases.remove(folder.absolutePath) else coordinator.leases[folder.absolutePath] = count
            }
        }
    }

    private fun directory(id: String): File {
        require(Regex("[a-z][a-z0-9_]*").matches(id)) { "Invalid documentId" }
        val directory = File(root, id)
        require(directory.canonicalFile.parentFile == root.canonicalFile) { "Unsafe document path" }
        return directory
    }
    private val generationPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private fun currentGeneration(directory: File): String? {
        val pointer = File(directory, "current")
        if (!pointer.exists()) return null
        val generation = pointer.readText(Charsets.UTF_8)
        if (!generationPattern.matches(generation)) throw IOException("Invalid document pointer")
        return generation
    }
    private fun Int?.orZero() = this ?: 0
    private fun copy(open: () -> InputStream, target: File, checkCancellation: () -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        open().use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(8192)
                while (true) {
                    checkCancellation()
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count > 0) { output.write(buffer, 0, count); digest.update(buffer, 0, count) }
                }
                output.fd.sync()
            }
        }
        if (target.length() == 0L) throw IOException("Empty document component")
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    private fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) { val count = input.read(buffer); if (count < 0) break; if (count > 0) digest.update(buffer, 0, count) }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
