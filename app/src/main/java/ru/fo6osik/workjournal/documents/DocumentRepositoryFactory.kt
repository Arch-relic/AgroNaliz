package ru.fo6osik.workjournal.documents

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fo6osik.workjournal.documents.sources.AssetsDocumentSource

/** Opt-in composition only; default creation remains assets-only. */
object DocumentRepositoryFactory {
    /** Explicit opt-in; the default assets composition remains unchanged. */
    suspend fun createWithGoogleDrive(context: Context): DocumentRepository =
        ru.fo6osik.workjournal.documents.sources.GoogleDriveServices.get(context).repository()

    suspend fun create(context: Context): DocumentRepository = withContext(Dispatchers.IO) {
        val application = context.applicationContext
        DefaultDocumentRepository(DocumentCatalog.load(application.assets), LocalDocumentStore(application), listOf(AssetsDocumentSource(application.assets)))
    }
}
