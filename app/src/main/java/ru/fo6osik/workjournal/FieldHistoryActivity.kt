package ru.fo6osik.workjournal

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class FieldHistoryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FIELD_ID =
            "extra_field_id"

        const val EXTRA_YEAR =
            "extra_year"
    }

    private lateinit var textHistoryTitle: TextView
    private lateinit var historyContainer: LinearLayout
    private lateinit var textEmptyHistory: TextView

    private var fieldId: Int = 0
    private var selectedYear: Int? = null

    private val database by lazy {
        DatabaseProvider.getDatabase(
            applicationContext
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_field_history
        )

        fieldId =
            intent.getIntExtra(
                EXTRA_FIELD_ID,
                0
            )

        selectedYear =
            if (
                intent.hasExtra(
                    EXTRA_YEAR
                )
            ) {
                intent.getIntExtra(
                    EXTRA_YEAR,
                    0
                )
            } else {
                null
            }

        if (fieldId <= 0) {
            finish()
            return
        }

        textHistoryTitle =
            findViewById(
                R.id.textHistoryTitle
            )

        historyContainer =
            findViewById(
                R.id.historyContainer
            )

        textEmptyHistory =
            findViewById(
                R.id.textEmptyHistory
            )
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {

        lifecycleScope.launch {

            val field =
                database
                    .fieldDao()
                    .getById(
                        fieldId
                    )

            if (field == null) {
                finish()
                return@launch
            }

            textHistoryTitle.text =
                if (
                    selectedYear == null
                ) {
                    "${field.name} — все года"
                } else {
                    "${field.name} — $selectedYear"
                }

            val works =
                if (
                    selectedYear == null
                ) {
                    database
                        .seasonWorkDao()
                        .getWorksByField(
                            fieldId
                        )
                } else {
                    database
                        .seasonWorkDao()
                        .getWorksByFieldAndYear(
                            fieldId,
                            selectedYear.toString()
                        )
                }

            historyContainer.removeAllViews()

            textEmptyHistory.visibility =
                if (works.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            var previousYear: Int? = null

            for (work in works) {

                val currentYear =
                    work.workDate
                        .take(4)
                        .toIntOrNull()

                if (
                    selectedYear == null &&
                    currentYear != null &&
                    currentYear != previousYear
                ) {
                    addYearHeader(
                        currentYear
                    )

                    previousYear =
                        currentYear
                }

                val workType =
                    database
                        .workTypeDao()
                        .getById(
                            work.workTypeId
                        )

                val relations =
                    database
                        .seasonWorkDao()
                        .getVehiclesForWork(
                            work.id
                        )

                val vehicleNames =
                    relations.map { relation ->

                        relation.vehicleId
                            ?.let { vehicleId ->

                                database
                                    .vehicleDao()
                                    .getById(
                                        vehicleId
                                    )
                                    ?.let {
                                        buildVehicleName(
                                            it
                                        )
                                    }
                            }
                            ?: relation
                                .vehicleNameSnapshot
                    }

                addWorkCard(
                    work = work,
                    workTypeName =
                        workType?.name
                            ?: "Работа",
                    vehicleNames =
                        vehicleNames
                )
            }
        }
    }

    private fun addYearHeader(
        year: Int
    ) {

        val density =
            resources.displayMetrics.density

        historyContainer.addView(
            TextView(this).apply {
                text =
                    year.toString()

                textSize =
                    22f

                setPadding(
                    0,
                    (12 * density).toInt(),
                    0,
                    (8 * density).toInt()
                )
            }
        )
    }

    private fun addWorkCard(
        work: SeasonWork,
        workTypeName: String,
        vehicleNames: List<String>
    ) {

        val density =
            resources.displayMetrics.density

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    (16 * density).toInt(),
                    (14 * density).toInt(),
                    (16 * density).toInt(),
                    (14 * density).toInt()
                )

                background =
                    GradientDrawable().apply {

                        cornerRadius =
                            18 * density

                        setColor(
                            Color.rgb(
                                35,
                                35,
                                35
                            )
                        )

                        setStroke(
                            (1 * density).toInt(),
                            Color.rgb(
                                80,
                                80,
                                80
                            )
                        )
                    }
            }

        card.addView(
            TextView(this).apply {
                text =
                    workTypeName

                textSize =
                    20f

                setTextColor(
                    Color.WHITE
                )
            }
        )

        card.addView(
            createInfoText(
                "Дата: ${formatDate(work.workDate)}"
            )
        )

        work.areaHa?.let {
            card.addView(
                createInfoText(
                    "Площадь: ${formatArea(it)} га"
                )
            )
        }

        if (
            vehicleNames.isNotEmpty()
        ) {
            card.addView(
                createInfoText(
                    "Техника: " +
                        vehicleNames.joinToString(
                            ", "
                        )
                )
            )
        }

        if (
            !work.note.isNullOrBlank()
        ) {
            card.addView(
                createInfoText(
                    "Примечание: ${work.note}"
                )
            )
        }

        historyContainer.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin =
                    (12 * density).toInt()
            }
        )
    }

    private fun createInfoText(
        value: String
    ): TextView {

        val density =
            resources.displayMetrics.density

        return TextView(this).apply {
            text = value

            textSize = 15f

            setTextColor(
                Color.LTGRAY
            )

            setPadding(
                0,
                (5 * density).toInt(),
                0,
                0
            )
        }
    }

    private fun buildVehicleName(
        vehicle: Vehicle
    ): String {

        return listOf(
            vehicle.brand,
            vehicle.model
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(
                " "
            )
    }

    private fun formatDate(
        databaseDate: String
    ): String {

        return try {

            val source =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                )

            val target =
                SimpleDateFormat(
                    "dd.MM.yyyy",
                    Locale.getDefault()
                )

            val date =
                source.parse(
                    databaseDate
                )

            if (date != null) {
                target.format(
                    date
                )
            } else {
                databaseDate
            }

        } catch (_: Exception) {
            databaseDate
        }
    }

    private fun formatArea(
        area: Double
    ): String {

        return if (
            area % 1.0 == 0.0
        ) {
            area.toInt().toString()
        } else {
            area
                .toString()
                .replace(
                    '.',
                    ','
                )
        }
    }
}
