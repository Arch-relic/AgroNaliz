package ru.fo6osik.workjournal

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AggregateDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_AGGREGATE_ID =
            "aggregate_id"
    }

    private lateinit var textAggregateName: TextView
    private lateinit var textAggregateCategory: TextView
    private lateinit var textAggregateWidth: TextView
    private lateinit var textAggregateNote: TextView
    private lateinit var childrenContainer: LinearLayout
    private lateinit var textChildrenHeader: TextView
    private lateinit var historyContainer: LinearLayout
    private lateinit var textEmptyHistory: TextView

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
            R.layout.activity_aggregate_details
        )

        textAggregateName =
            findViewById(
                R.id.textAggregateName
            )

        textAggregateCategory =
            findViewById(
                R.id.textAggregateCategory
            )

        textAggregateWidth =
            findViewById(
                R.id.textAggregateWidth
            )

        textAggregateNote =
            findViewById(
                R.id.textAggregateNote
            )

        childrenContainer =
            findViewById(
                R.id.childrenContainer
            )

        textChildrenHeader =
            findViewById(
                R.id.textChildrenHeader
            )

        historyContainer =
            findViewById(
                R.id.historyContainer
            )

        textEmptyHistory =
            findViewById(
                R.id.textEmptyHistory
            )

        val aggregateId =
            intent.getIntExtra(
                EXTRA_AGGREGATE_ID,
                -1
            )

        if (
            aggregateId <= 0
        ) {
            finish()
            return
        }

        loadAggregate(
            aggregateId
        )
    }

    private fun loadAggregate(
        aggregateId: Int
    ) {

        lifecycleScope.launch {

            val aggregate =
                database
                    .aggregateDao()
                    .getById(
                        aggregateId
                    )

            if (
                aggregate == null
            ) {
                finish()
                return@launch
            }

            showAggregate(
                aggregate
            )

            showChildren(
                aggregate
            )

            showHistory(
                aggregate
            )
        }
    }

    private fun showAggregate(
        aggregate: Aggregate
    ) {

        textAggregateName.text =
            aggregate.name

        textAggregateCategory.text =
            "Тип: ${aggregate.category}"

        if (
            aggregate.workingWidthM == null
        ) {

            textAggregateWidth.visibility =
                View.GONE

        } else {

            textAggregateWidth.visibility =
                View.VISIBLE

            textAggregateWidth.text =
                "Рабочая ширина: ${
                    formatNumber(
                        aggregate.workingWidthM
                    )
                } м"
        }

        if (
            aggregate.note.isBlank()
        ) {

            textAggregateNote.visibility =
                View.GONE

        } else {

            textAggregateNote.visibility =
                View.VISIBLE

            textAggregateNote.text =
                "Примечание: ${aggregate.note}"
        }
    }

    private suspend fun showChildren(
        aggregate: Aggregate
    ) {

        val children =
            database
                .aggregateDao()
                .getChildren(
                    aggregate.id
                )

        childrenContainer.removeAllViews()

        textChildrenHeader.visibility =
            if (
                children.isEmpty()
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }

        childrenContainer.visibility =
            if (
                children.isEmpty()
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }

        for (
            child in children
        ) {

            childrenContainer.addView(
                createSimpleCard(
                    title =
                        child.name,
                    body =
                        child.category
                )
            )
        }
    }

    private suspend fun showHistory(
        aggregate: Aggregate
    ) {

        historyContainer.removeAllViews()

        /*
         * Карточка агрегата показывает только техническую историю.
         *
         * Рабочие события (FIELD_WORK) остаются в Журнале событий
         * и по-прежнему содержат ссылку на агрегат, но в карточку
         * агрегата не попадают.
         */
        val technicalTypes =
            setOf(
                "MALFUNCTION",
                "REPAIR",
                "MAINTENANCE"
            )

        val events =
            database
                .objectLinkDao()
                .getEventsForAggregate(
                    aggregate.id
                )
                .mapNotNull { relation ->

                    database
                        .farmEventDao()
                        .getByUid(
                            relation.eventUid
                        )
                }
                .filter {
                    it.eventType in technicalTypes
                }
                .distinctBy {
                    it.eventUid
                }
                .sortedWith(
                    compareByDescending<FarmEvent> {
                        it.eventYear
                    }
                        .thenByDescending {
                            it.eventMonth ?: 0
                        }
                        .thenByDescending {
                            it.eventDay ?: 0
                        }
                        .thenByDescending {
                            it.createdAt
                        }
                )

        textEmptyHistory.visibility =
            if (
                events.isEmpty()
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        for (
            event in events
        ) {

            addHistoryCard(
                event
            )
        }
    }

    private fun addHistoryCard(
        event: FarmEvent
    ) {

        val density =
            resources.displayMetrics.density

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    (14 * density).toInt(),
                    (12 * density).toInt(),
                    (14 * density).toInt(),
                    (12 * density).toInt()
                )

                background =
                    GradientDrawable().apply {

                        cornerRadius =
                            16 * density

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
                    eventTypeName(
                        event.eventType
                    )

                textSize =
                    13f

                setTextColor(
                    Color.LTGRAY
                )
            }
        )

        card.addView(
            TextView(this).apply {

                text =
                    event.title

                textSize =
                    18f

                setTypeface(
                    typeface,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.WHITE
                )
            }
        )

        card.addView(
            createInfoText(
                "Дата: ${
                    formatEventDate(
                        event
                    )
                }"
            )
        )

        event.description
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                card.addView(
                    createInfoText(it)
                )
            }

        historyContainer.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                bottomMargin =
                    (10 * density).toInt()
            }
        )
    }

    private fun createSimpleCard(
        title: String,
        body: String
    ): LinearLayout {

        val density =
            resources.displayMetrics.density

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                (12 * density).toInt(),
                (10 * density).toInt(),
                (12 * density).toInt(),
                (10 * density).toInt()
            )

            background =
                GradientDrawable().apply {

                    cornerRadius =
                        14 * density

                    setColor(
                        Color.rgb(
                            45,
                            45,
                            45
                        )
                    )
                }

            addView(
                TextView(
                    this@AggregateDetailsActivity
                ).apply {

                    text =
                        title

                    textSize =
                        17f

                    setTypeface(
                        typeface,
                        Typeface.BOLD
                    )

                    setTextColor(
                        Color.WHITE
                    )
                }
            )

            addView(
                createInfoText(
                    body
                )
            )
        }
    }

    private fun createInfoText(
        value: String
    ): TextView {

        val density =
            resources.displayMetrics.density

        return TextView(this).apply {

            text =
                value

            textSize =
                15f

            setTextColor(
                Color.LTGRAY
            )

            setPadding(
                0,
                (4 * density).toInt(),
                0,
                0
            )
        }
    }

    private fun eventTypeName(
        eventType: String
    ): String {

        return when (
            eventType
        ) {

            "MALFUNCTION" ->
                "Неисправность"

            "REPAIR" ->
                "Ремонт"

            "MAINTENANCE" ->
                "Техническое обслуживание"

            else ->
                "Событие"
        }
    }

    private fun formatEventDate(
        event: FarmEvent
    ): String {

        val month =
            event.eventMonth

        val day =
            event.eventDay

        if (
            event.datePrecision == "DAY" &&
            month != null &&
            day != null
        ) {

            return "%02d.%02d.%04d".format(
                day,
                month,
                event.eventYear
            )
        }

        if (
            event.datePrecision == "MONTH" &&
            month != null
        ) {

            return "${
                monthName(
                    month
                )
            } ${event.eventYear}"
        }

        return event.eventYear.toString()
    }

    private fun monthName(
        month: Int
    ): String {

        return when (
            month
        ) {

            1 -> "Январь"
            2 -> "Февраль"
            3 -> "Март"
            4 -> "Апрель"
            5 -> "Май"
            6 -> "Июнь"
            7 -> "Июль"
            8 -> "Август"
            9 -> "Сентябрь"
            10 -> "Октябрь"
            11 -> "Ноябрь"
            12 -> "Декабрь"

            else -> ""
        }
    }

    private fun formatNumber(
        value: Double
    ): String {

        return if (
            value % 1.0 == 0.0
        ) {
            value.toInt().toString()
        } else {
            value
                .toString()
                .replace(
                    ".",
                    ","
                )
        }
    }
}
