package ru.fo6osik.workjournal.documents

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fo6osik.workjournal.documents.sources.AssetsDocumentSource

/** Opt-in composition only; no existing Activity is modified or automatically initialized. */
object DocumentRepositoryFactory {
    suspend fun create(context: Context): DocumentRepository = withContext(Dispatchers.IO) {
        val application = context.applicationContext
        DefaultDocumentRepository(DocumentCatalog.load(application.assets), LocalDocumentStore(application), listOf(AssetsDocumentSource(application.assets)))
    }
}
