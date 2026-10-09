package ru.fo6osik.workjournal

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class EventJournalActivity : AppCompatActivity() {

    private lateinit var eventsContainer: LinearLayout
    private lateinit var textEmptyEvents: TextView
    private lateinit var journalScroll: ScrollView

    private var savedScrollY: Int = 0

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
            R.layout.activity_event_journal
        )

        eventsContainer =
            findViewById(
                R.id.eventsContainer
            )

        textEmptyEvents =
            findViewById(
                R.id.textEmptyEvents
            )

        journalScroll =
            eventsContainer.parent as ScrollView

        savedScrollY =
            savedInstanceState
                ?.getInt(
                    "event_journal_scroll_y",
                    0
                )
                ?: 0
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    override fun onPause() {
        savedScrollY =
            journalScroll.scrollY

        super.onPause()
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        outState.putInt(
            "event_journal_scroll_y",
            journalScroll.scrollY
        )

        super.onSaveInstanceState(
            outState
        )
    }

    private fun loadEvents() {

        lifecycleScope.launch {

            val events =
                database
                    .farmEventDao()
                    .getAll()
                    .sortedWith(
                        compareBy<FarmEvent>(
                            { it.eventYear },
                            { it.eventMonth ?: 0 },
                            { it.eventDay ?: 0 },
                            { it.createdAt }
                        )
                    )

            eventsContainer.removeAllViews()

            textEmptyEvents.visibility =
                if (events.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            var previousYear: Int? = null
            var previousMonth: Int? = null

            for (event in events) {

                if (
                    previousYear != event.eventYear ||
                    previousMonth != event.eventMonth
                ) {

                    addMonthHeader(
                        event.eventYear,
                        event.eventMonth
                    )

                    previousYear =
                        event.eventYear

                    previousMonth =
                        event.eventMonth
                }

                addEvent(
                    event
                )
            }

            journalScroll.post {

                journalScroll.scrollTo(
                    0,
                    savedScrollY
                )
            }
        }
    }

    private fun addMonthHeader(
        year: Int,
        month: Int?
    ) {

        val density =
            resources.displayMetrics.density

        val header =
            TextView(this).apply {

                text =
                    if (month == null) {
                        year.toString()
                    } else {
                        "${monthName(month)} $year"
                    }

                textSize =
                    20f

                setTypeface(
                    typeface,
                    Typeface.BOLD
                )

                setPadding(
                    0,
                    (12 * density).toInt(),
                    0,
                    (10 * density).toInt()
                )
            }

        eventsContainer.addView(
            header
        )
    }

    private suspend fun addEvent(
        event: FarmEvent
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
                    20f

                setTypeface(
                    typeface,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    0,
                    (4 * density).toInt(),
                    0,
                    0
                )
            }
        )

        val linkedSeasonWork =
            if (
                event.eventType == "FIELD_WORK"
            ) {

                database
                    .seasonWorkDao()
                    .getByEventUid(
                        event.eventUid
                    )

            } else {
                null
            }

        val dateText =
            if (
                linkedSeasonWork != null &&
                !linkedSeasonWork.endDate.isNullOrBlank() &&
                linkedSeasonWork.endDate !=
                    linkedSeasonWork.workDate
            ) {

                "Период: ${
                    formatDatabaseDate(
                        linkedSeasonWork.workDate
                    )
                } — ${
                    formatDatabaseDate(
                        linkedSeasonWork.endDate
                    )
                }"

            } else {

                "Дата: ${
                    formatEventDate(
                        event
                    )
                }"
            }

        card.addView(
            createInfoText(
                dateText
            )
        )

        when (event.eventType) {

            "FIELD_WORK" ->
                addFieldWorkDetails(
                    card,
                    event
                )

            "PURCHASE" ->
                addPurchaseDetails(
                    card,
                    event
                )

            "MATERIAL_STOCK" ->
                addMaterialStockDetails(
                    card,
                    event
                )
        }

        if (
            !event.description.isNullOrBlank()
        ) {

            card.addView(
                createInfoText(
                    "Примечание: ${event.description}"
                )
            )
        }

        addObjectLinks(
            card =
                card,
            event =
                event
        )

        eventsContainer.addView(
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

    private suspend fun addFieldWorkDetails(
        card: LinearLayout,
        event: FarmEvent
    ) {

        val linkedFieldNames =
            database
                .farmEventFieldDao()
                .getByEventUid(
                    event.eventUid
                )
                .mapNotNull { relation ->

                    database
                        .fieldDao()
                        .getById(
                            relation.fieldId
                        )
                        ?.name
                }
                .distinct()

        val work =
            database
                .seasonWorkDao()
                .getByEventUid(
                    event.eventUid
                )

        val fallbackFieldName =
            work
                ?.fieldId
                ?.let {

                    database
                        .fieldDao()
                        .getById(it)
                        ?.name
                }

        val fieldNames =
            if (
                linkedFieldNames.isNotEmpty()
            ) {
                linkedFieldNames
            } else {
                fallbackFieldName
                    ?.let {
                        listOf(it)
                    }
                    ?: emptyList()
            }

        if (
            fieldNames.isNotEmpty()
        ) {

            val label =
                if (
                    fieldNames.size == 1
                ) {
                    "Поле"
                } else {
                    "Поля"
                }

            card.addView(
                createInfoText(
                    "$label: " +
                        fieldNames.joinToString(
                            ", "
                        )
                )
            )
        }

        if (
            work == null
        ) {
            return
        }

        card.addView(
            createInfoText(
                "Статус: ${
                    if (
                        work.status ==
                        "IN_PROGRESS"
                    ) {
                        "В работе"
                    } else {
                        "Завершено"
                    }
                }"
            )
        )

        work.areaHa?.let {

            card.addView(
                createInfoText(
                    "Площадь: ${formatNumber(it)} га"
                )
            )
        }

        val relations =
            database
                .seasonWorkDao()
                .getVehiclesForWork(
                    work.id
                )

        val vehicleNames =
            relations.mapNotNull { relation ->

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
                        .takeIf {
                            it.isNotBlank()
                        }
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
    }

    private fun addMaterialStockDetails(
        card: LinearLayout,
        event: FarmEvent
    ) {

        val payload =
            event.payloadJson
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return

        try {

            val json =
                JSONObject(
                    payload
                )

            val items =
                json.optJSONArray(
                    "items"
                )
                ?: return

            if (
                items.length() == 0
            ) {
                return
            }

            card.addView(
                createInfoText(
                    "Перечень СЗР:"
                )
            )

            for (
                index in
                0 until items.length()
            ) {

                val item =
                    items.optJSONObject(index)
                        ?: continue

                val name =
                    item.optString(
                        "name"
                    )
                        .trim()

                if (
                    name.isBlank()
                ) {
                    continue
                }

                val quantityText =
                    if (
                        item.has("quantity") &&
                        !item.isNull("quantity")
                    ) {

                        formatNumber(
                            item.optDouble(
                                "quantity"
                            )
                        )

                    } else {
                        null
                    }

                val unit =
                    item.optString(
                        "unit"
                    )
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }

                val packageWeight =
                    if (
                        item.has(
                            "packageWeightKg"
                        ) &&
                        !item.isNull(
                            "packageWeightKg"
                        )
                    ) {

                        item.optDouble(
                            "packageWeightKg"
                        )
                            .takeIf {
                                it > 0.0
                            }

                    } else {
                        null
                    }

                val line =
                    buildString {

                        append("• ")
                        append(name)

                        if (
                            quantityText != null
                        ) {

                            append(" — ")
                            append(quantityText)

                            if (
                                unit != null
                            ) {

                                append(" ")
                                append(unit)
                            }
                        }

                        if (
                            packageWeight != null
                        ) {

                            append(" × ")
                            append(
                                formatNumber(
                                    packageWeight
                                )
                            )
                            append(" кг")
                        }
                    }

                card.addView(
                    createInfoText(
                        line
                    )
                )
            }

        } catch (_: Exception) {

            /*
             * Неизвестный формат старой записи
             * не должен ломать журнал.
             */
        }
    }

    private fun addPurchaseDetails(
        card: LinearLayout,
        event: FarmEvent
    ) {

        val payload =
            event.payloadJson
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return

        try {

            val json =
                JSONObject(
                    payload
                )

            val materialName =
                json.optString(
                    "materialName"
                )

            if (
                materialName.isNotBlank()
            ) {

                card.addView(
                    createInfoText(
                        "Материал: $materialName"
                    )
                )
            }

            if (
                json.has(
                    "packageCount"
                )
            ) {

                val packageCount =
                    json.optInt(
                        "packageCount"
                    )

                if (
                    packageCount > 0
                ) {

                    card.addView(
                        createInfoText(
                            "Количество: $packageCount биг-бэг."
                        )
                    )
                }
            }

            if (
                json.has(
                    "packageWeightKg"
                )
            ) {

                val packageWeightKg =
                    json.optDouble(
                        "packageWeightKg"
                    )

                if (
                    packageWeightKg > 0.0
                ) {

                    card.addView(
                        createInfoText(
                            "Вес одного: ${formatNumber(packageWeightKg)} кг"
                        )
                    )
                }
            }

            if (
                json.has(
                    "totalWeightKg"
                )
            ) {

                val totalWeightKg =
                    json.optDouble(
                        "totalWeightKg"
                    )

                if (
                    totalWeightKg > 0.0
                ) {

                    card.addView(
                        createInfoText(
                            "Всего: ${formatWeight(totalWeightKg)}"
                        )
                    )
                }
            }

            if (
                json.has(
                    "applicationRateKgHa"
                )
            ) {

                val applicationRate =
                    json.optDouble(
                        "applicationRateKgHa"
                    )

                if (
                    applicationRate > 0.0
                ) {

                    card.addView(
                        createInfoText(
                            "Норма внесения: ${formatNumber(applicationRate)} кг/га"
                        )
                    )
                }
            }

        } catch (_: Exception) {

            /*
             * Если в старом событии payloadJson
             * имеет неизвестный формат,
             * журнал всё равно продолжит работать.
             */
        }
    }

    private suspend fun addObjectLinks(
        card: LinearLayout,
        event: FarmEvent
    ) {

        val aggregateRelations =
            database
                .objectLinkDao()
                .getAggregatesForEvent(
                    event.eventUid
                )

        val vehicleRelations =
            database
                .objectLinkDao()
                .getVehiclesForEvent(
                    event.eventUid
                )

        val aggregates =
            aggregateRelations
                .mapNotNull { relation ->

                    database
                        .aggregateDao()
                        .getById(
                            relation.aggregateId
                        )
                }

        val vehicles =
            vehicleRelations
                .mapNotNull { relation ->

                    database
                        .vehicleDao()
                        .getById(
                            relation.vehicleId
                        )
                }

        for (
            aggregate in aggregates
        ) {

            card.addView(
                createObjectLinkText(
                    "↗ Агрегат: ${aggregate.name}"
                ) {

                    openAggregateDetails(
                        aggregate.id
                    )
                }
            )
        }

        for (
            vehicle in vehicles
        ) {

            card.addView(
                createObjectLinkText(
                    "↗ Техника: ${
                        buildVehicleName(
                            vehicle
                        )
                    }"
                ) {

                    openVehicleDetails(
                        vehicle.id
                    )
                }
            )
        }

        /*
         * Для события, привязанного ровно к одному объекту,
         * вся карточка тоже ведёт в карточку этого объекта.
         */
        val totalTargets =
            aggregates.size +
            vehicles.size

        if (
            totalTargets == 1
        ) {

            card.isClickable =
                true

            card.isFocusable =
                true

            card.setOnClickListener {

                if (
                    aggregates.size == 1
                ) {

                    openAggregateDetails(
                        aggregates.first().id
                    )

                } else if (
                    vehicles.size == 1
                ) {

                    openVehicleDetails(
                        vehicles.first().id
                    )
                }
            }
        }
    }

    private fun createObjectLinkText(
        value: String,
        onClick: () -> Unit
    ): TextView {

        val density =
            resources.displayMetrics.density

        return TextView(this).apply {

            text =
                value

            textSize =
                15f

            setTypeface(
                typeface,
                Typeface.BOLD
            )

            setTextColor(
                Color.WHITE
            )

            setPadding(
                0,
                (7 * density).toInt(),
                0,
                (2 * density).toInt()
            )

            isClickable =
                true

            isFocusable =
                true

            setOnClickListener {
                onClick()
            }
        }
    }

    private fun openAggregateDetails(
        aggregateId: Int
    ) {

        startActivity(
            Intent(
                this,
                AggregateDetailsActivity::class.java
            ).apply {

                putExtra(
                    AggregateDetailsActivity.EXTRA_AGGREGATE_ID,
                    aggregateId
                )
            }
        )
    }

    private fun openVehicleDetails(
        vehicleId: Int
    ) {

        startActivity(
            Intent(
                this,
                VehicleDetailsActivity::class.java
            ).apply {

                putExtra(
                    "vehicle_id",
                    vehicleId
                )
            }
        )
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
                (5 * density).toInt(),
                0,
                0
            )
        }
    }

    private fun eventTypeName(
        eventType: String
    ): String {

        return when (eventType) {

            "FIELD_WORK" ->
                "Сезонное событие"

            "PURCHASE" ->
                "Закупка"

            "MAINTENANCE" ->
                "Техническое обслуживание"

            "REPAIR" ->
                "Ремонт"

            "MATERIAL_STOCK" ->
                "Учёт СЗР"

            "DOCUMENTATION" ->
                "Документация"

            "LAND_RECORD" ->
                "Учёт земли"

            "CROP_ROTATION" ->
                "Севооборот"

            "RECORDS" ->
                "Учёт"

            "MALFUNCTION" ->
                "Неисправность"

            "INSPECTION" ->
                "Осмотр"

            "WEATHER" ->
                "Погодные условия"

            else ->
                "Событие"
        }
    }

    private fun formatEventDate(
        event: FarmEvent
    ): String {

        return when (
            event.datePrecision
        ) {

            "DAY" -> {

                val day =
                    event.eventDay ?: 1

                val month =
                    event.eventMonth ?: 1

                "%02d.%02d.%04d".format(
                    day,
                    month,
                    event.eventYear
                )
            }

            "MONTH" -> {

                val month =
                    event.eventMonth

                if (
                    month == null
                ) {

                    event.eventYear.toString()

                } else {

                    "${monthName(month)} ${event.eventYear}"
                }
            }

            else ->
                event.eventYear.toString()
        }
    }


    private fun formatDatabaseDate(
        value: String?
    ): String {

        if (
            value.isNullOrBlank()
        ) {
            return "—"
        }

        val parts =
            value.split("-")

        if (
            parts.size != 3
        ) {
            return value
        }

        return "${
            parts[2]
        }.${parts[1]}.${parts[0]}"
    }

    private fun monthName(
        month: Int
    ): String {

        return when (month) {

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

            else ->
                "Без месяца"
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

    private fun formatWeight(
        kilograms: Double
    ): String {

        return if (
            kilograms >= 1000.0
        ) {

            "${formatNumber(kilograms / 1000.0)} т"

        } else {

            "${formatNumber(kilograms)} кг"
        }
    }

    private fun formatNumber(
        value: Double
    ): String {

        return if (
            value % 1.0 == 0.0
        ) {

            value
                .toInt()
                .toString()

        } else {

            value
                .toString()
                .replace(
                    '.',
                    ','
                )
        }
    }
}
