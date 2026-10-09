package ru.fo6osik.workjournal.documents.sources

import android.content.res.AssetManager
import ru.fo6osik.workjournal.documents.DocumentDescriptor
import ru.fo6osik.workjournal.documents.DocumentSource as Location
import java.io.InputStream

class AssetsDocumentSource(private val openAsset: (String) -> InputStream) : DocumentSource {
    constructor(assets: AssetManager) : this({ path -> assets.open(path) })
    override fun supports(location: Location) = location is Location.Assets
    override suspend fun metadata(document: DocumentDescriptor, location: Location) =
        SourceMetadata(document.revision, document.pdfSha256, document.searchIndexSha256)
    override fun openPdf(location: Location) = openAsset((location as Location.Assets).pdfPath)
    override fun openSearchIndex(location: Location) = openAsset((location as Location.Assets).searchIndexPath)
}
