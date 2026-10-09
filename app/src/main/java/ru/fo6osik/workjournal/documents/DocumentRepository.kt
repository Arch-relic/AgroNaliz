package ru.fo6osik.workjournal.documents

import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable
import java.io.File

/** The viewer must keep this lease until PDFView releases its files. */
class LocalDocument internal constructor(
    val documentId: String,
    val revision: String?,
    val pdfFile: File,
    val searchIndexFile: File,
    val pdfSha256: String,
    val searchIndexSha256: String,
    private val release: () -> Unit
) : Closeable {
    @Synchronized override fun close() { if (!closed) { closed = true; release() } }
    private var closed = false
}

sealed class DocumentState {
    object Unchecked : DocumentState()
    object Missing : DocumentState()
    object Loading : DocumentState()
    data class ReadyOffline(val revision: String?) : DocumentState()
    data class Error(val cause: Exception, val previousCopyAvailable: Boolean) : DocumentState()
    data class UpdateAvailable(val currentRevision: String?, val proposedRevision: String?) : DocumentState()
}

enum class UpdateStatus { CURRENT, AVAILABLE, UNKNOWN }

interface DocumentRepository {
    fun observeState(documentId: String): StateFlow<DocumentState>
    suspend fun open(documentId: String): LocalDocument
    suspend fun download(documentId: String): LocalDocument
    suspend fun checkForUpdate(documentId: String): UpdateStatus
}

class DocumentBusyException(id: String) : IllegalStateException("Document is already loading: $id")

class UnverifiedDocumentException : IllegalStateException("Both expected PDF and index SHA-256 values are required before activation")

class UnsupportedDocumentSourceException(id: String) : java.io.IOException("No configured document source for $id")
