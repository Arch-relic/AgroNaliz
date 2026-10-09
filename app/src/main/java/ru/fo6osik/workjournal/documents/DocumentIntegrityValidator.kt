package ru.fo6osik.workjournal.documents

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import org.json.JSONObject
import java.io.File
import java.io.IOException

fun interface DocumentIntegrityValidator {
    fun validate(pdf: File, index: File)
}

/** Parses the actual PDF, not only its signature, and checks the matching index page range. */
class AndroidDocumentIntegrityValidator : DocumentIntegrityValidator {
    override fun validate(pdf: File, index: File) {
        val pages = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (renderer.pageCount <= 0) throw IOException("Empty PDF")
                renderer.openPage(0).use { /* Ensure the document is readable. */ }
                renderer.pageCount
            }
        }
        val json = JSONObject(index.readText(Charsets.UTF_8))
        require(json.getInt("version") == 4 && json.getString("coordinateSystem") == "normalized_page") { "Unsupported search index" }
        val entries = json.getJSONArray("pages")
        require(entries.length() > 0) { "Empty search index" }
        val seen = mutableSetOf<Int>()
        for (i in 0 until entries.length()) {
            val page = entries.getJSONObject(i)
            val number = page.getInt("page")
            require(number in 1..pages && seen.add(number)) { "Invalid index page: $number" }
            val text = page.getString("text")
            val words = page.optJSONArray("words") ?: continue
            for (j in 0 until words.length()) {
                val word = words.getJSONObject(j)
                require(word.getInt("start") >= 0 && word.getInt("end") > word.getInt("start") && word.getInt("end") <= text.length) { "Invalid word offsets" }
                val x0 = word.getDouble("x0"); val x1 = word.getDouble("x1")
                val y0 = word.getDouble("y0"); val y1 = word.getDouble("y1")
                require(x0.isFinite() && x1.isFinite() && y0.isFinite() && y1.isFinite() && x0 >= 0 && x1 <= 1 && x1 >= x0 && y0 >= 0 && y1 <= 1 && y1 >= y0) { "Invalid word coordinates" }
            }
        }
    }
}
