package ru.fo6osik.workjournal

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class DeveloperCatalogActivity : AppCompatActivity() {

    private lateinit var catalogContainer: LinearLayout
	private lateinit var textCatalogStats: TextView
	private lateinit var editCatalogSearch: EditText
	private var searchJob: Job? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_developer_catalog
        )

        catalogContainer =
            findViewById(
                R.id.catalogContainer
            )
		
		textCatalogStats =
    findViewById(
        R.id.textCatalogStats
    )
	
	editCatalogSearch =
    findViewById(
        R.id.editCatalogSearch
    )

editCatalogSearch.doAfterTextChanged { text ->

    searchJob?.cancel()

    searchJob =
        lifecycleScope.launch {

            delay(250)

            loadCatalog(
                text
                    ?.toString()
                    .orEmpty()
            )
        }
}

searchJob =
    lifecycleScope.launch {
        loadCatalog()
    }
	}

    private suspend fun loadCatalog(
    searchQuery: String = ""
) {

        catalogContainer.removeAllViews()

        val database =
            DatabaseProvider
                .getDatabase(applicationContext)
				
				var tractorModelCount = 0
val tractorEngineIds =
    mutableSetOf<Int>()
	
	var combineModelCount = 0

val combineEngineIds =
    mutableSetOf<Int>()

var carModelCount = 0

val carEngineIds =
    mutableSetOf<Int>()


        // =========================
        // ТРАКТОРЫ
        // =========================

        addCategoryTitle(
            "ТРАКТОРЫ"
        )

        val tractorDao =
            database.tractorCatalogDao()

        val tractorBrands =
            tractorDao
                .getAllBrands()
                .sortedBy {
                    it.name
                }

        for (brand in tractorBrands) {

    val models =
        tractorDao
            .getModelsByBrand(
                brand.id
            )
            .sortedBy {
                it.name
            }

    val matchingModels =
        mutableListOf<
            Pair<
                TractorModel,
                List<CarEngine>
            >
        >()

    val normalizedQuery =
        normalizeSearch(
            searchQuery
        )

    val brandMatches =
        normalizedQuery.isEmpty() ||
        normalizeSearch(
            brand.name
        ).contains(
            normalizedQuery
        )

    for (model in models) {

        tractorModelCount++

        val engines =
            tractorDao
                .getEnginesByModel(
                    model.id
                )

        engines.forEach { engine ->

            tractorEngineIds.add(
                engine.id
            )
        }

        val modelMatches =
            normalizeSearch(
                model.name
            ).contains(
                normalizedQuery
            )

        val engineMatches =
            engines.any { engine ->

                normalizeSearch(
                    buildEngineText(
                        engine
                    )
                ).contains(
                    normalizedQuery
                )
            }

        if (
            normalizedQuery.isEmpty() ||
            brandMatches ||
            modelMatches ||
            engineMatches
        ) {

            matchingModels.add(
                model to engines
            )
        }
    }

    if (matchingModels.isNotEmpty()) {

        addBrandTitle(
            brand.name
        )

        for (
            (model, engines)
            in matchingModels
        ) {

            addModelTitle(
                model.name
            )
			
			model.baseModelId?.let { baseModelId ->

    val baseModel =
        tractorDao.getModelById(
            baseModelId
        )

    if (baseModel != null) {

        addRelationLine(
            "Базовая модель: ${baseModel.name}"
        )
    }
}

            if (engines.isEmpty()) {

                addEngineLine(
                    "Двигатель не указан"
                )

            } else {

                for (engine in engines) {

                    addEngineLine(
                        buildEngineText(
                            engine
                        )
                    )
                }
            }
        }
    }
}


        addSeparator()
		
		// =========================
// КОМБАЙНЫ
// =========================

addCategoryTitle(
    "КОМБАЙНЫ"
)

val combineDao =
    database.combineCatalogDao()

val combineBrands =
    combineDao
        .getAllBrands()
        .sortedBy {
            it.name
        }

for (brand in combineBrands) {

    val models =
        combineDao
            .getModelsByBrand(
                brand.id
            )
            .sortedBy {
                it.name
            }

    val matchingModels =
        mutableListOf<
            Pair<
                CombineModel,
                List<CarEngine>
            >
        >()

    val normalizedQuery =
        normalizeSearch(
            searchQuery
        )

    val brandMatches =
        normalizedQuery.isEmpty() ||
        normalizeSearch(
            brand.name
        ).contains(
            normalizedQuery
        )

    for (model in models) {

        combineModelCount++

        val engines =
            combineDao
                .getEnginesByModel(
                    model.id
                )

        engines.forEach { engine ->

            combineEngineIds.add(
                engine.id
            )
        }

        val modelMatches =
            normalizeSearch(
                model.name
            ).contains(
                normalizedQuery
            )

        val engineMatches =
            engines.any { engine ->

                normalizeSearch(
                    buildEngineText(
                        engine
                    )
                ).contains(
                    normalizedQuery
                )
            }

        if (
            normalizedQuery.isEmpty() ||
            brandMatches ||
            modelMatches ||
            engineMatches
        ) {

            matchingModels.add(
                model to engines
            )
        }
    }

    if (matchingModels.isNotEmpty()) {

        addBrandTitle(
            brand.name
        )

        for (
            (model, engines)
            in matchingModels
        ) {

            addModelTitle(
                model.name
            )

            if (engines.isEmpty()) {

                addEngineLine(
                    "Двигатель не указан"
                )

            } else {

                for (engine in engines) {

                    addEngineLine(
                        buildEngineText(
                            engine
                        )
                    )
                }
            }
        }
    }
}

addSeparator()


        // =========================
        // АВТОМОБИЛИ
        // =========================

        addCategoryTitle(
            "АВТОМОБИЛИ"
        )

        val carDao =
            database.carCatalogDao()

        val carBrands =
            carDao
                .getAllBrands()
                .sortedBy {
                    it.name
                }

        for (brand in carBrands) {

    val models =
        carDao
            .getModelsByBrand(
                brand.id
            )
            .sortedBy {
                it.name
            }

    val matchingModels =
        mutableListOf<
            Pair<
                CarModel,
                List<CarEngine>
            >
        >()

    val normalizedQuery =
        normalizeSearch(
            searchQuery
        )

    val brandMatches =
        normalizedQuery.isEmpty() ||
        normalizeSearch(
            brand.name
        ).contains(
            normalizedQuery
        )

    for (model in models) {

        carModelCount++

        val engines =
            carDao
                .getEnginesByModel(
                    model.id
                )

        engines.forEach { engine ->

            carEngineIds.add(
                engine.id
            )
        }

        val modelMatches =
            normalizeSearch(
                model.name
            ).contains(
                normalizedQuery
            )

        val engineMatches =
            engines.any { engine ->

                normalizeSearch(
                    buildEngineText(
                        engine
                    )
                ).contains(
                    normalizedQuery
                )
            }

        if (
            normalizedQuery.isEmpty() ||
            brandMatches ||
            modelMatches ||
            engineMatches
        ) {

            matchingModels.add(
                model to engines
            )
        }
    }

    if (matchingModels.isNotEmpty()) {

        addBrandTitle(
            brand.name
        )

        for (
            (model, engines)
            in matchingModels
        ) {

            addModelTitle(
                model.name
            )

            if (engines.isEmpty()) {

                addEngineLine(
                    "Двигатель не указан"
                )

            } else {

                for (engine in engines) {

                    addEngineLine(
                        buildEngineText(
                            engine
                        )
                    )
                }
            }
        }
    }
}
		
		textCatalogStats.text =
    buildString {

        append("Тракторы: ")

        append(
            plural(
                tractorBrands.size,
                "марка",
                "марки",
                "марок"
            )
        )

        append(" • ")

        append(
            plural(
                tractorModelCount,
                "модель",
                "модели",
                "моделей"
            )
        )

        append(" • ")

        append(
            plural(
                tractorEngineIds.size,
                "двигатель",
                "двигателя",
                "двигателей"
            )
        )


        append("\n")

        append("Комбайны: ")

        append(
            plural(
                combineBrands.size,
                "марка",
                "марки",
                "марок"
            )
        )

        append(" • ")

        append(
            plural(
                combineModelCount,
                "модель",
                "модели",
                "моделей"
            )
        )

        append(" • ")

        append(
            plural(
                combineEngineIds.size,
                "двигатель",
                "двигателя",
                "двигателей"
            )
        )


        append("\n")

        append("Автомобили: ")

        append(
            plural(
                carBrands.size,
                "марка",
                "марки",
                "марок"
            )
        )

        append(" • ")

        append(
            plural(
                carModelCount,
                "модель",
                "модели",
                "моделей"
            )
        )

        append(" • ")

        append(
            plural(
                carEngineIds.size,
                "двигатель",
                "двигателя",
                "двигателей"
            )
        )
      }
    }
	
	private fun normalizeSearch(
    value: String
): String {

    return value
        .lowercase()
        .replace("-", "")
        .replace("–", "")
        .replace("—", "")
        .replace(".", "")
        .replace(" ", "")
}


    private fun buildEngineText(
        engine: CarEngine
    ): String {

        return buildString {

            append(
                engine.manufacturer
            )

            append(" ")

            append(
                engine.code
            )

            engine.powerHp?.let {

                append(
                    " — $it л.с."
                )
            }
        }
    }


    private fun addCategoryTitle(
        text: String
    ) {

        val view =
            TextView(this)

        view.text =
            text

        view.textSize =
            22f

        view.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        view.setPadding(
            0,
            24,
            0,
            16
        )

        catalogContainer.addView(
            view
        )
    }


    private fun addBrandTitle(
        text: String
    ) {

        val view =
            TextView(this)

        view.text =
            text

        view.textSize =
            19f

        view.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        view.setPadding(
            0,
            18,
            0,
            8
        )

        catalogContainer.addView(
            view
        )
    }


    private fun addModelTitle(
        text: String
    ) {

        val view =
            TextView(this)

        view.text =
            "• $text"

        view.textSize =
            17f

        view.setPadding(
            24,
            8,
            0,
            2
        )

        catalogContainer.addView(
            view
        )
    }
	
	private fun addRelationLine(
    text: String
) {

    val view =
        TextView(this)

    view.text =
        "↳ $text"

    view.textSize =
        14f

    view.setTypeface(
        null,
        android.graphics.Typeface.ITALIC
    )

    view.setPadding(
        52,
        2,
        0,
        4
    )

    catalogContainer.addView(
        view
    )
}


    private fun addEngineLine(
        text: String
    ) {

        val view =
            TextView(this)

        view.text =
            "↳ $text"

        view.textSize =
            15f

        view.setPadding(
            52,
            2,
            0,
            8
        )

        catalogContainer.addView(
            view
        )
    }


    private fun addSeparator() {

        val separator =
            View(this)

        separator.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {

                topMargin =
                    28

                bottomMargin =
                    12
            }

        separator.setBackgroundColor(
            0xFF808080.toInt()
        )

        catalogContainer.addView(
            separator
        )
    }
	private fun plural(
    count: Int,
    one: String,
    few: String,
    many: String
): String {

    val mod100 =
        count % 100

    val mod10 =
        count % 10

    val word =
        when {

            mod100 in 11..14 ->
                many

            mod10 == 1 ->
                one

            mod10 in 2..4 ->
                few

            else ->
                many
        }

    return "$count $word"
   }
}