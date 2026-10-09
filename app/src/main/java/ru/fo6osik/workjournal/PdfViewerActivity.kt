package ru.fo6osik.workjournal

import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.infomaniak.lib.pdfview.PDFView
import com.infomaniak.lib.pdfview.util.FitPolicy
import org.json.JSONObject
import java.util.Locale
import android.graphics.Typeface
import android.graphics.Paint
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.LinearLayout

class PdfViewerActivity : AppCompatActivity() {

    private data class SearchWord(
        val start: Int,
        val end: Int,
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float
    )

    private data class SearchHighlight(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float
    )

    private data class SearchPage(
        val page: Int,
        val text: String,
        val leftOffsetRatio: Float = 0f,
        val words: List<SearchWord> = emptyList()
    )

    private data class SearchResult(
        val page: Int,
        val snippet: String,
        val leftOffsetRatio: Float = 0f,
        val highlights: List<SearchHighlight> = emptyList()
    )

    private var searchPagesCache: List<SearchPage>? = null
	
	private var activeSearchResults: List<SearchResult> =
    emptyList()

private var activeSearchIndex: Int =
    -1
	
	private var activeSearchQuery: String =
    ""

private var maxPdfBasePageWidth: Float =
    0f

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_pdf_viewer
        )


        val pdfView =
            findViewById<PDFView>(
                R.id.pdfView
            )

        val textPdfTitle =
            findViewById<TextView>(
                R.id.textPdfTitle
            )

        val textPdfPage =
            findViewById<TextView>(
                R.id.textPdfPage
            )

        val buttonPdfSearch =
            findViewById<ImageButton>(
                R.id.buttonPdfSearch
            )

val searchNavigationBar =
    findViewById<LinearLayout>(
        R.id.searchNavigationBar
    )

val buttonSearchPrevious =
    findViewById<ImageButton>(
        R.id.buttonSearchPrevious
    )

val textSearchPosition =
    findViewById<TextView>(
        R.id.textSearchPosition
    )

val buttonSearchNext =
    findViewById<ImageButton>(
        R.id.buttonSearchNext
    )

val buttonSearchClose =
    findViewById<ImageButton>(
        R.id.buttonSearchClose
    )


buttonSearchPrevious.setOnClickListener {

    if (activeSearchResults.isNotEmpty()) {

        activeSearchIndex--

        if (activeSearchIndex < 0) {
            activeSearchIndex =
                activeSearchResults.lastIndex
        }

        val result =
            activeSearchResults[
                activeSearchIndex
            ]

        pdfView.jumpTo(
            result.page - 1,
            true
        )

        textSearchPosition.text =
    "$activeSearchQuery • ${activeSearchIndex + 1} из ${activeSearchResults.size}"

        pdfView.invalidate()
  }
}

buttonSearchNext.setOnClickListener {

    if (activeSearchResults.isNotEmpty()) {

        activeSearchIndex++

        if (
            activeSearchIndex >
            activeSearchResults.lastIndex
        ) {
            activeSearchIndex = 0
        }

        val result =
            activeSearchResults[
                activeSearchIndex
            ]

        pdfView.jumpTo(
            result.page - 1,
            true
        )

        textSearchPosition.text =
    "$activeSearchQuery • ${activeSearchIndex + 1} из ${activeSearchResults.size}"

        pdfView.invalidate()
    }
}

textSearchPosition.setOnClickListener {

    if (
        activeSearchResults.isNotEmpty() &&
        activeSearchQuery.isNotBlank()
    ) {

        showSearchResults(
            pdfView = pdfView,
            results = activeSearchResults,
            query = activeSearchQuery
        )
    }
}

buttonSearchClose.setOnClickListener {

    activeSearchResults =
        emptyList()

    activeSearchIndex =
        -1
		
		activeSearchQuery =
    ""

    searchNavigationBar.visibility =
        View.GONE

    pdfView.invalidate()

		
}

        /*
         * ПЕРЕХОД ПО НОМЕРУ СТРАНИЦЫ
         */
        textPdfPage.setOnClickListener {

            val totalPages =
                pdfView.pageCount

            if (totalPages <= 0) {

                Toast.makeText(
                    this,
                    "Документ ещё загружается",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            val input =
                EditText(this).apply {

                    inputType =
                        InputType.TYPE_CLASS_NUMBER

                    setText(
                        (pdfView.currentPage + 1)
                            .toString()
                    )

                    selectAll()
                }


            val dialog =
                AlertDialog.Builder(this)
                    .setTitle(
                        "Перейти на страницу"
                    )
                    .setMessage(
                        "Введите номер страницы от 1 до $totalPages"
                    )
                    .setView(
                        input
                    )
                    .setNegativeButton(
                        "Отмена",
                        null
                    )
                    .setPositiveButton(
                        "Перейти",
                        null
                    )
                    .create()


            dialog.setOnShowListener {

                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                ).setOnClickListener {

                    val pageNumber =
                        input.text
                            .toString()
                            .toIntOrNull()


                    if (
                        pageNumber == null ||
                        pageNumber !in 1..totalPages
                    ) {

                        input.error =
                            "Введите число от 1 до $totalPages"

                        return@setOnClickListener
                    }


                    pdfView.jumpTo(
                        pageNumber - 1,
                        true
                    )

                    dialog.dismiss()
                }


                input.requestFocus()

                dialog.window
                    ?.setSoftInputMode(
                        WindowManager
                            .LayoutParams
                            .SOFT_INPUT_STATE_ALWAYS_VISIBLE
                    )
            }


            dialog.show()
        }


        val assetPath =
            intent.getStringExtra(
                "assetPath"
            )

        val documentTitle =
            intent.getStringExtra(
                "documentTitle"
            ) ?: "Документ"


        textPdfTitle.text =
            documentTitle


        if (assetPath.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Путь к документу не указан",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }


        /*
         * ПОИСК ПО ДОКУМЕНТУ
         */
        buttonPdfSearch.setOnClickListener {

            showSearchDialog(
                pdfView = pdfView,
                assetPath = assetPath
            )
        }


        /*
         * ЗАГРУЗКА PDF
         */
        pdfView
            .fromAsset(
                assetPath
            )
            .enableSwipe(
                true
            )
            .swipeHorizontal(
                false
            )
            .enableDoubletap(
                true
            )
            .defaultPage(
                0
            )
            .enableAnnotationRendering(
                true
            )
            .enableAntialiasing(
                true
            )
            .pageFitPolicy(
                FitPolicy.WIDTH
            )
            .autoSpacing(
                true
            )
            .onDrawAll {
                canvas,
                pageWidth,
                pageHeight,
                displayedPage
            ->

                if (
                    activeSearchIndex in
                    activeSearchResults.indices
                ) {

                    val activeResult =
                        activeSearchResults[
                            activeSearchIndex
                        ]

                    if (
                        displayedPage ==
                        activeResult.page - 1
                    ) {

                        val paint =
                            Paint().apply {

                                color =
                                    0x66FFEB3B

                                style =
                                    Paint.Style.FILL

                                isAntiAlias =
                                    true
                            }

                        val currentBasePageWidth =
                            pdfView
                                .getPageSize(
                                    displayedPage
                                )
                                .width

                        val maxScaledPageWidth =
                            pdfView.toCurrentScale(
                                maxPdfBasePageWidth
                            )

                        val pageLeftOffset =
                            (
                                maxScaledPageWidth -
                                    pageWidth
                            )
                                .coerceAtLeast(
                                    0f
                                ) / 2f


                        for (
                            highlight in
                            activeResult.highlights
                        ) {

                            canvas.drawRect(
                                pageLeftOffset +
                                    highlight.x0 *
                                        pageWidth,
                                highlight.y0 *
                                    pageHeight,
                                pageLeftOffset +
                                    highlight.x1 *
                                        pageWidth,
                                highlight.y1 *
                                    pageHeight,
                                paint
                            )
                        }
                    }
                }
            }
            .onLoad { pageCount ->

                maxPdfBasePageWidth =
                    (
                        0 until pageCount
                    )
                        .map {
                            pdfView
                                .getPageSize(
                                    it
                                )
                                .width
                        }
                        .maxOrNull()
                        ?: 0f

                textPdfPage.text =
                    "Страница 1 из $pageCount"
            }
            .onPageChange { page, pageCount ->

                textPdfPage.text =
                    "Страница ${page + 1} из $pageCount"
            }
            .onError { error ->

                Toast.makeText(
                    this,
                    "Ошибка открытия PDF: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            .load()
    }


    /*
     * ОКНО ВВОДА ПОИСКОВОГО ЗАПРОСА
     */
    private fun showSearchDialog(
        pdfView: PDFView,
        assetPath: String
    ) {

        val input =
            EditText(this).apply {

                hint =
                    "Например: тормозная жидкость"

                inputType =
                    InputType.TYPE_CLASS_TEXT
            }


        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Поиск по документу"
                )
                .setView(
                    input
                )
                .setNegativeButton(
                    "Отмена",
                    null
                )
                .setPositiveButton(
                    "Найти",
                    null
                )
                .create()


        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val query =
                    input.text
                        .toString()
                        .trim()


                if (query.isBlank()) {

                    input.error =
                        "Введите текст для поиска"

                    return@setOnClickListener
                }


                dialog.dismiss()


                searchDocument(
                    pdfView = pdfView,
                    assetPath = assetPath,
                    query = query
                )
            }


            input.requestFocus()

            dialog.window
                ?.setSoftInputMode(
                    WindowManager
                        .LayoutParams
                        .SOFT_INPUT_STATE_ALWAYS_VISIBLE
                )
        }


        dialog.show()
    }
	
	/*
 * НОРМАЛИЗАЦИЯ ТЕКСТА ДЛЯ ПОИСКА
 */
private fun normalizeSearchText(
    text: String
): String {

    return text
        .lowercase(
            Locale.ROOT
        )
        .replace(
            'ё',
            'е'
        )
        .replace(
            'й',
            'и'
        )
}

    /*
     * КООРДИНАТЫ ПОДСВЕТКИ НА СТРАНИЦЕ PDF
     */
    private fun createHighlights(
        page: SearchPage,
        matchStart: Int,
        matchEnd: Int
    ): List<SearchHighlight> {

        if (page.words.isEmpty()) {
            return emptyList()
        }

        return page.words
            .filter { word ->

                word.end >
                    matchStart &&
                word.start <
                    matchEnd
            }
            .map { word ->

                SearchHighlight(
                    x0 = word.x0,
                    y0 = word.y0,
                    x1 = word.x1,
                    y1 = word.y1
                )
            }
    }


    /*
     * ВЫПОЛНЕНИЕ ПОИСКА
     */
    private fun searchDocument(
        pdfView: PDFView,
        assetPath: String,
        query: String
    ) {

        Toast.makeText(
            this,
            "Поиск...",
            Toast.LENGTH_SHORT
        ).show()


        Thread {

            try {

                val pages =
                    searchPagesCache
                        ?: loadSearchPages(
                            assetPath
                        ).also {

                            searchPagesCache =
                                it
                        }


                val normalizedQuery =
    normalizeSearchText(
        query.trim()
    )


                val results =
                    mutableListOf<SearchResult>()


                for (page in pages) {

                    val cleanText =
                        page.text
                            .replace(
                                Regex("\\s+"),
                                " "
                            )
                            .trim()


                    val normalizedText =
    normalizeSearchText(
        cleanText
    )


                    val matchPosition =
    normalizedText.indexOf(
        normalizedQuery
    )


                    if (matchPosition >= 0) {

                        val start =
    (
        matchPosition - 35
    ).coerceAtLeast(
        0
    )


val end =
    (
        matchPosition +
            normalizedQuery.length +
            75
    ).coerceAtMost(
        cleanText.length
    )


val snippet =
    buildString {

        if (start > 0) {
            append("…")
        }

        append(
            cleanText.substring(
                start,
                end
            )
        )

        if (
            end <
            cleanText.length
        ) {
            append("…")
        }
	}


                        val highlights =
                            createHighlights(
                                page = page,
                                matchStart =
                                    matchPosition,
                                matchEnd =
                                    matchPosition +
                                        normalizedQuery.length
                            )


                        results.add(
                            SearchResult(
                                page =
                                    page.page,
                                snippet =
                                    snippet,
                                leftOffsetRatio =
                                    page.leftOffsetRatio,
                                highlights =
                                    highlights
                            )
                        )
                    }
                }


                runOnUiThread {

                    if (results.isEmpty()) {

                        Toast.makeText(
                            this,
                            "Ничего не найдено",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {

                        showSearchResults(
    pdfView =
        pdfView,
    results =
        results,
    query =
        query
)
                    }
                }

            } catch (error: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Ошибка поиска: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }


    /*
     * ЗАГРУЗКА ПОИСКОВОГО ИНДЕКСА
     */
    private fun loadSearchPages(
        assetPath: String
    ): List<SearchPage> {

        val extensionPosition =
            assetPath.lastIndexOf(
                '.'
            )


        val searchIndexPath =

            if (
                extensionPosition >= 0
            ) {

                assetPath.substring(
                    0,
                    extensionPosition
                ) +
                    "_search.json"

            } else {

                assetPath +
                    "_search.json"
            }


        val jsonText =
            assets
                .open(
                    searchIndexPath
                )
                .bufferedReader()
                .use {
                    it.readText()
                }


        val root =
            JSONObject(
                jsonText
            )


        val jsonPages =
            root.getJSONArray(
                "pages"
            )


        val pages =
            mutableListOf<SearchPage>()


        for (
            index in
            0 until jsonPages.length()
        ) {

            val pageObject =
                jsonPages.getJSONObject(
                    index
                )


            val words =
                mutableListOf<SearchWord>()

            if (
                pageObject.has(
                    "words"
                )
            ) {

                val jsonWords =
                    pageObject.getJSONArray(
                        "words"
                    )

                for (
                    wordIndex in
                    0 until jsonWords.length()
                ) {

                    val wordObject =
                        jsonWords.getJSONObject(
                            wordIndex
                        )

                    words.add(
                        SearchWord(
                            start =
                                wordObject.getInt(
                                    "start"
                                ),
                            end =
                                wordObject.getInt(
                                    "end"
                                ),
                            x0 =
                                wordObject
                                    .getDouble(
                                        "x0"
                                    )
                                    .toFloat(),
                            y0 =
                                wordObject
                                    .getDouble(
                                        "y0"
                                    )
                                    .toFloat(),
                            x1 =
                                wordObject
                                    .getDouble(
                                        "x1"
                                    )
                                    .toFloat(),
                            y1 =
                                wordObject
                                    .getDouble(
                                        "y1"
                                    )
                                    .toFloat()
                        )
                    )
                }
            }


            pages.add(
                SearchPage(
                    page =
                        pageObject.getInt(
                            "page"
                        ),
                    text =
                        pageObject.getString(
                            "text"
                        ),
                    leftOffsetRatio =
                        pageObject
                            .optDouble(
                                "leftOffsetRatio",
                                0.0
                            )
                            .toFloat(),
                    words =
                        words
                )
            )
        }


        return pages
    }


    /*
     * СПИСОК РЕЗУЛЬТАТОВ ПОИСКА
     */
    private fun showSearchResults(
    pdfView: PDFView,
    results: List<SearchResult>,
    query: String
) {

    val maximumDisplayedResults = 100

    val visibleResults =
        results.take(
            maximumDisplayedResults
        )

    val items =
        visibleResults.map { result ->

            createSearchResultText(
                page = result.page,
                snippet = result.snippet,
                query = query
            )
        }


    val listView =
        ListView(this)


    val adapter =
        object : ArrayAdapter<CharSequence>(
            this,
            android.R.layout.simple_list_item_1,
            items
        ) {

            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {

                val view =
                    super.getView(
                        position,
                        convertView,
                        parent
                    )

                val textView =
                    view.findViewById<TextView>(
                        android.R.id.text1
                    )

                textView.textSize = 15f

                textView.maxLines = 3

                textView.ellipsize =
                    TextUtils.TruncateAt.END

                textView.setPadding(
                    32,
                    18,
                    32,
                    18
                )
				
				if (
    activeSearchResults.isNotEmpty() &&
    activeSearchQuery == query &&
    position == activeSearchIndex
) {

    view.setBackgroundColor(
    0xFFFFE4EC.toInt()
    )

} else {

    view.setBackgroundColor(
        0x00000000
    )
}

                return view
            }
        }


    listView.adapter = adapter


    val title =

        if (
            results.size >
            maximumDisplayedResults
        ) {

            "Найдено ${results.size} — показаны первые $maximumDisplayedResults"

        } else {

            "Найдено: ${results.size}"
        }


    val dialog =
        AlertDialog.Builder(this)
            .setTitle(
                title
            )
            .setView(
                listView
            )
            .setNegativeButton(
                "Закрыть",
                null
            )
            .create()


    listView.setOnItemClickListener {
        _,
        _,
        position,
        _
    ->

    val result =
        visibleResults[
            position
        ]


    activeSearchResults =
        results

    activeSearchIndex =
        position
		
		activeSearchQuery =
    query


    val searchNavigationBar =
        findViewById<LinearLayout>(
            R.id.searchNavigationBar
        )

    val textSearchPosition =
        findViewById<TextView>(
            R.id.textSearchPosition
        )


    textSearchPosition.text =
    "$activeSearchQuery • ${activeSearchIndex + 1} из ${activeSearchResults.size}"

    searchNavigationBar.visibility =
        View.VISIBLE


    dialog.dismiss()

pdfView.postDelayed(
    {
        pdfView.jumpTo(
            result.page - 1,
            true
        )

        pdfView.invalidate()
    },
    150
)
}


    dialog.show()

if (
    activeSearchResults.isNotEmpty() &&
    activeSearchQuery == query &&
    activeSearchIndex in visibleResults.indices
) {

    listView.post {

        val targetPosition =
            (
                activeSearchIndex - 2
            ).coerceAtLeast(
                0
            )

        listView.setSelection(
            targetPosition
        )
      }
 }
  }
  private fun createSearchResultText(
    page: Int,
    snippet: String,
    query: String
): CharSequence {

    val pageTitle =
        "Страница $page\n"

    val result =
        SpannableStringBuilder()

    val titleStart =
        result.length

    result.append(
        pageTitle
    )

    result.setSpan(
        StyleSpan(
            Typeface.BOLD
        ),
        titleStart,
        result.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    val snippetStart =
        result.length

    result.append(
        snippet
    )

    val normalizedSnippet =
    normalizeSearchText(
        snippet
    )

val normalizedQuery =
    normalizeSearchText(
        query.trim()
    )

    var searchPosition = 0

    while (
    normalizedQuery.isNotEmpty()
) {

        val match =
    normalizedSnippet.indexOf(
        normalizedQuery,
        searchPosition
    )

        if (match < 0) {
            break
        }

        val start =
            snippetStart +
                match

        val end =
    start +
        normalizedQuery.length

        result.setSpan(
            BackgroundColorSpan(
                0x66FFEB3B
            ),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        result.setSpan(
            StyleSpan(
                Typeface.BOLD
            ),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        searchPosition =
    match +
        normalizedQuery.length
    }

    return result
 }
}