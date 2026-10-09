package ru.fo6osik.workjournal

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class SeasonWorksActivity : AppCompatActivity() {

    private data class DisplayItem(
        val sortDate: String,
        val process: SeasonProcess? = null,
        val work: SeasonWork? = null
    )

    private lateinit var worksContainer: LinearLayout
    private lateinit var textEmptyWorks: TextView
    private lateinit var worksScroll: ScrollView

    private var savedScrollY: Int = 0

    private val expandedProcessIds =
        mutableSetOf<Int>()

    private val expandedWorkIds =
        mutableSetOf<Int>()

    private val database by lazy {
        DatabaseProvider.getDatabase(
            applicationContext
        )
    }

    private val seasonProcessDao by lazy {
        database.seasonProcessDao()
    }

    private val seasonWorkDao by lazy {
        database.seasonWorkDao()
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_season_works
        )

        worksContainer =
            findViewById(
                R.id.worksContainer
            )

        textEmptyWorks =
            findViewById(
                R.id.textEmptyWorks
            )

        worksScroll =
            worksContainer.parent as ScrollView

        savedScrollY =
            savedInstanceState
                ?.getInt(
                    "season_works_scroll_y",
                    0
                )
                ?: 0

        savedInstanceState
            ?.getIntegerArrayList(
                "season_works_expanded_processes"
            )
            ?.let {
                expandedProcessIds.addAll(it)
            }

        savedInstanceState
            ?.getIntegerArrayList(
                "season_works_expanded_works"
            )
            ?.let {
                expandedWorkIds.addAll(it)
            }

        findViewById<Button>(
            R.id.buttonAddSeasonWork
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AddSeasonWorkActivity::class.java
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadWorks()
    }

    override fun onPause() {
        savedScrollY =
            worksScroll.scrollY

        super.onPause()
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        outState.putInt(
            "season_works_scroll_y",
            worksScroll.scrollY
        )

        outState.putIntegerArrayList(
            "season_works_expanded_processes",
            ArrayList(
                expandedProcessIds
            )
        )

        outState.putIntegerArrayList(
            "season_works_expanded_works",
            ArrayList(
                expandedWorkIds
            )
        )

        super.onSaveInstanceState(
            outState
        )
    }

    private fun loadWorks() {

        lifecycleScope.launch {

            val processes =
                seasonProcessDao.getAll()

            val standaloneWorks =
                seasonWorkDao
                    .getAllWorks()
                    .filter {
                        it.processId == null
                    }

            val items =
                mutableListOf<DisplayItem>()
                    .apply {

                        processes.forEach {

                            add(
                                DisplayItem(
                                    sortDate =
                                        it.startDate,
                                    process =
                                        it
                                )
                            )
                        }

                        standaloneWorks.forEach {

                            add(
                                DisplayItem(
                                    sortDate =
                                        it.workDate,
                                    work =
                                        it
                                )
                            )
                        }
                    }
                    .sortedByDescending {
                        it.sortDate
                    }

            worksContainer.removeAllViews()

            textEmptyWorks.visibility =
                if (
                    items.isEmpty()
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            for (
                item in items
            ) {

                item.process
                    ?.let {
                        addProcessCard(it)
                    }

                item.work
                    ?.let {
                        addStandaloneWorkCard(it)
                    }
            }

            worksScroll.post {

                worksScroll.scrollTo(
                    0,
                    savedScrollY
                )
            }
        }
    }

    private suspend fun addProcessCard(
        process: SeasonProcess
    ) {

        val density =
            resources.displayMetrics.density

        val operations =
            seasonWorkDao
                .getWorksByProcessId(
                    process.id
                )

        val fieldName =
            process.fieldId
                ?.let {

                    database
                        .fieldDao()
                        .getById(it)
                        ?.name
                }

        val card =
            createDarkCard()

        val processHeader =
            createHeaderRow(
                title =
                    process.title,
                onDelete = {
                    showDeleteProcessDialog(
                        process
                    )
                }
            )

        card.addView(
            processHeader
        )

        card.addView(
            createInfoText(
                formatProcessPeriod(
                    process
                )
            )
        )

        card.addView(
            createInfoText(
                "Статус: ${
                    statusName(
                        process.status
                    )
                }"
            )
        )

        fieldName
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                card.addView(
                    createInfoText(
                        "Поле: $it"
                    )
                )
            }

        process.crop
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                card.addView(
                    createInfoText(
                        "Культура: $it"
                    )
                )
            }

        process.goal
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                card.addView(
                    createInfoText(
                        "Цель: $it"
                    )
                )
            }

        process.areaHa
            ?.let {

                card.addView(
                    createInfoText(
                        "Площадь: ${
                            formatArea(it)
                        } га"
                    )
                )
            }

        process.note
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                card.addView(
                    createInfoText(
                        "Примечание: $it"
                    )
                )
            }

        if (
            operations.isNotEmpty()
        ) {

            card.addView(
                createInfoText(
                    "Операций: ${operations.size}. " +
                        "Нажмите на процесс, чтобы раскрыть."
                )
            )
        }

        val operationsContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                visibility =
                    if (
                        process.id in
                        expandedProcessIds
                    ) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                setPadding(
                    0,
                    (10 * density).toInt(),
                    0,
                    0
                )
            }

        for (
            work in operations
        ) {

            operationsContainer.addView(
                buildOperationCard(
                    work =
                        work,
                    nested =
                        true
                )
            )
        }

        addLinkedProcessEvents(
            process =
                process,
            container =
                operationsContainer
        )

        card.addView(
            operationsContainer
        )

        if (
            operations.isNotEmpty()
        ) {

            /*
             * Родительский процесс раскрывается только по нажатию
             * на его собственный заголовок.
             *
             * Поэтому клики по дочерним операциям больше никогда
             * не могут свернуть весь родительский процесс.
             */
            processHeader.isClickable =
                true

            processHeader.isFocusable =
                true

            processHeader.setOnClickListener {

                if (
                    operationsContainer.visibility ==
                    View.VISIBLE
                ) {

                    operationsContainer.visibility =
                        View.GONE

                    expandedProcessIds.remove(
                        process.id
                    )

                } else {

                    operationsContainer.visibility =
                        View.VISIBLE

                    expandedProcessIds.add(
                        process.id
                    )
                }
            }
        }

        worksContainer.addView(
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

    private suspend fun addStandaloneWorkCard(
        work: SeasonWork
    ) {

        val density =
            resources.displayMetrics.density

        worksContainer.addView(
            buildOperationCard(
                work =
                    work,
                nested =
                    false
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                bottomMargin =
                    (12 * density).toInt()
            }
        )
    }

    private suspend fun buildOperationCard(
        work: SeasonWork,
        nested: Boolean
    ): LinearLayout {

        val density =
            resources.displayMetrics.density

        val workTypeName =
            database
                .workTypeDao()
                .getById(
                    work.workTypeId
                )
                ?.name
                ?: "Работа"

        val fieldNames =
            resolveFieldNames(
                work
            )

        val vehicleNames =
            seasonWorkDao
                .getVehiclesForWork(
                    work.id
                )
                .map { relation ->

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

        val aggregateRelations =
            database
                .objectLinkDao()
                .getAggregatesForSeasonWork(
                    work.id
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

        val days =
            seasonWorkDao
                .getDaysForWork(
                    work.id
                )

        val card =
            createDarkCard(
                nested =
                    nested
            )

        val operationHeader =
            createHeaderRow(
                title =
                    workTypeName,
                onDelete = {
                    showDeleteWorkDialog(
                        work = work,
                        workTypeName =
                            workTypeName
                    )
                },
                nested =
                    nested
            )

        card.addView(
            operationHeader
        )

        card.addView(
            createInfoText(
                formatWorkPeriod(
                    work
                )
            )
        )

        card.addView(
            createInfoText(
                "Статус: ${
                    statusName(
                        work.status
                    )
                }"
            )
        )

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

        work.areaHa
            ?.let {

                card.addView(
                    createInfoText(
                        "Площадь: ${
                            formatArea(it)
                        } га"
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
            aggregates.isNotEmpty()
        ) {

            val label =
                if (
                    aggregates.size == 1
                ) {
                    "Агрегат"
                } else {
                    "Агрегаты"
                }

            card.addView(
                createInfoText(
                    "$label:"
                )
            )

            for (
                aggregate in aggregates
            ) {

                card.addView(
                    createObjectLinkText(
                        "↗ ${aggregate.name}"
                    ) {

                        openAggregateDetails(
                            aggregate.id
                        )
                    }
                )
            }
        }

        work.note
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                card.addView(
                    createInfoText(
                        "Примечание: $it"
                    )
                )
            }

        if (
            days.size > 1
        ) {

            card.addView(
                createInfoText(
                    "Этапов: ${days.size}. " +
                        "Нажмите на название операции, чтобы раскрыть."
                )
            )

            val daysContainer =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    visibility =
                        if (
                            work.id in
                            expandedWorkIds
                        ) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    setPadding(
                        0,
                        (8 * density).toInt(),
                        0,
                        0
                    )
                }

            daysContainer.addView(
                TextView(this).apply {

                    text =
                        "Ход работы"

                    textSize =
                        16f

                    setTypeface(
                        typeface,
                        Typeface.BOLD
                    )

                    setTextColor(
                        Color.WHITE
                    )
                }
            )

            for (
                day in days
            ) {

                daysContainer.addView(
                    createInfoText(
                        formatWorkDay(
                            day
                        )
                    )
                )
            }

            card.addView(
                daysContainer
            )

            /*
             * Дочерняя операция управляет только собственной
             * хронологией. Родительский процесс здесь вообще
             * не участвует в обработке нажатия.
             */
            operationHeader.isClickable =
                true

            operationHeader.isFocusable =
                true

            operationHeader.setOnClickListener {

                if (
                    daysContainer.visibility ==
                    View.VISIBLE
                ) {

                    daysContainer.visibility =
                        View.GONE

                    expandedWorkIds.remove(
                        work.id
                    )

                } else {

                    daysContainer.visibility =
                        View.VISIBLE

                    expandedWorkIds.add(
                        work.id
                    )
                }
            }
        }

        return card
    }

    private suspend fun resolveFieldNames(
        work: SeasonWork
    ): List<String> {

        val eventUid =
            work.eventUid
                ?.takeIf {
                    it.isNotBlank()
                }

        val linked =
            if (
                eventUid != null
            ) {

                database
                    .farmEventFieldDao()
                    .getByEventUid(
                        eventUid
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

            } else {
                emptyList()
            }

        if (
            linked.isNotEmpty()
        ) {
            return linked
        }

        return work.fieldId
            ?.let {

                database
                    .fieldDao()
                    .getById(it)
                    ?.name
            }
            ?.let {
                listOf(it)
            }
            ?: emptyList()
    }

    private fun createDarkCard(
        nested: Boolean = false
    ): LinearLayout {

        val density =
            resources.displayMetrics.density

        return LinearLayout(this).apply {

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
                        if (
                            nested
                        ) {
                            Color.rgb(
                                48,
                                48,
                                48
                            )
                        } else {
                            Color.rgb(
                                35,
                                35,
                                35
                            )
                        }
                    )

                    setStroke(
                        (1 * density).toInt(),
                        Color.rgb(
                            85,
                            85,
                            85
                        )
                    )
                }

            if (
                nested
            ) {

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        topMargin =
                            (8 * density).toInt()
                    }
            }
        }
    }

    private fun createHeaderRow(
        title: String,
        onDelete: () -> Unit,
        nested: Boolean = false
    ): LinearLayout {

        val density =
            resources.displayMetrics.density

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER_VERTICAL

            addView(
                TextView(
                    this@SeasonWorksActivity
                ).apply {

                    text =
                        title

                    textSize =
                        if (
                            nested
                        ) {
                            18f
                        } else {
                            20f
                        }

                    setTypeface(
                        typeface,
                        Typeface.BOLD
                    )

                    setTextColor(
                        Color.WHITE
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            addView(
                TextView(
                    this@SeasonWorksActivity
                ).apply {

                    text =
                        "×"

                    textSize =
                        24f

                    gravity =
                        Gravity.CENTER

                    setTextColor(
                        Color.LTGRAY
                    )

                    background =
                        GradientDrawable().apply {

                            cornerRadius =
                                8 * density

                            setColor(
                                Color.rgb(
                                    55,
                                    55,
                                    55
                                )
                            )

                            setStroke(
                                (1 * density).toInt(),
                                Color.rgb(
                                    110,
                                    110,
                                    110
                                )
                            )
                        }

                    isClickable =
                        true

                    isFocusable =
                        true

                    setOnClickListener {
                        onDelete()
                    }
                },
                LinearLayout.LayoutParams(
                    (42 * density).toInt(),
                    (42 * density).toInt()
                ).apply {

                    marginStart =
                        (8 * density).toInt()
                }
            )
        }
    }

    private fun showDeleteProcessDialog(
        process: SeasonProcess
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Удалить рабочий процесс?"
            )
            .setMessage(
                "Будет удалён процесс «${process.title}», " +
                    "все входящие операции, их дни/этапы, " +
                    "связи с техникой и соответствующие " +
                    "события общего журнала. Действие необратимо."
            )
            .setNegativeButton(
                "Отмена",
                null
            )
            .setPositiveButton(
                "Удалить"
            ) { _, _ ->

                lifecycleScope.launch {

                    try {

                        database.withTransaction {

                            val works =
                                seasonWorkDao
                                    .getWorksByProcessId(
                                        process.id
                                    )

                            for (
                                work in works
                            ) {

                                deleteWorkData(
                                    work
                                )
                            }

                            seasonProcessDao
                                .deleteById(
                                    process.id
                                )
                        }

                        Toast.makeText(
                            this@SeasonWorksActivity,
                            "Рабочий процесс удалён",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadWorks()

                    } catch (
                        error: Exception
                    ) {

                        Toast.makeText(
                            this@SeasonWorksActivity,
                            "Не удалось удалить процесс: " +
                                error.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun showDeleteWorkDialog(
        work: SeasonWork,
        workTypeName: String
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Удалить операцию?"
            )
            .setMessage(
                "Будет удалена операция «$workTypeName», " +
                    "все её дни/этапы, связь с техникой и " +
                    "соответствующее событие общего журнала. " +
                    "Остальные операции процесса сохранятся."
            )
            .setNegativeButton(
                "Отмена",
                null
            )
            .setPositiveButton(
                "Удалить"
            ) { _, _ ->

                lifecycleScope.launch {

                    try {

                        val processId =
                            work.processId

                        database.withTransaction {

                            deleteWorkData(
                                work
                            )

                            if (
                                processId != null &&
                                seasonWorkDao
                                    .countWorksByProcessId(
                                        processId
                                    ) == 0
                            ) {

                                seasonProcessDao
                                    .deleteById(
                                        processId
                                    )
                            }
                        }

                        Toast.makeText(
                            this@SeasonWorksActivity,
                            "Операция удалена",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadWorks()

                    } catch (
                        error: Exception
                    ) {

                        Toast.makeText(
                            this@SeasonWorksActivity,
                            "Не удалось удалить операцию: " +
                                error.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    private suspend fun deleteWorkData(
        work: SeasonWork
    ) {

        seasonWorkDao.clearVehicles(
            work.id
        )

        /*
         * season_work_days удаляются каскадно
         * вместе с season_works.
         */
        seasonWorkDao.deleteWork(
            work.id
        )

        work.eventUid
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let { eventUid ->

                /*
                 * farm_event_fields удаляются каскадно.
                 */
                database
                    .farmEventDao()
                    .deleteByUid(
                        eventUid
                    )
            }
    }

    private suspend fun addLinkedProcessEvents(
        process: SeasonProcess,
        container: LinearLayout
    ) {

        val relations =
            database
                .objectLinkDao()
                .getEventsForProcess(
                    process.id
                )

        val events =
            relations
                .mapNotNull { relation ->

                    database
                        .farmEventDao()
                        .getByUid(
                            relation.eventUid
                        )
                }
                .distinctBy {
                    it.eventUid
                }
                .sortedWith(
                    compareBy<FarmEvent>(
                        { it.eventYear },
                        { it.eventMonth ?: 0 },
                        { it.eventDay ?: 0 },
                        { it.createdAt }
                    )
                )

        if (
            events.isEmpty()
        ) {
            return
        }

        val density =
            resources.displayMetrics.density

        container.addView(
            TextView(this).apply {

                text =
                    "Связанные события"

                textSize =
                    17f

                setTypeface(
                    typeface,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    0,
                    (16 * density).toInt(),
                    0,
                    (6 * density).toInt()
                )
            }
        )

        for (
            event in events
        ) {

            container.addView(
                buildLinkedEventCard(
                    event
                )
            )
        }
    }

    private suspend fun buildLinkedEventCard(
        event: FarmEvent
    ): LinearLayout {

        val density =
            resources.displayMetrics.density

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

        val card =
            createDarkCard(
                nested =
                    true
            )

        card.addView(
            TextView(this).apply {

                text =
                    eventTypeNameForProcess(
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
                    17f

                setTypeface(
                    typeface,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    0,
                    (3 * density).toInt(),
                    0,
                    0
                )
            }
        )

        card.addView(
            createInfoText(
                "Дата: ${
                    formatFarmEventDate(
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
                    createInfoText(
                        it
                    )
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
         * Если событие относится ровно к одному объекту,
         * кликабельна вся карточка неисправности/события.
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

        return card
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

    private fun eventTypeNameForProcess(
        eventType: String
    ): String {

        return when (
            eventType
        ) {

            "MALFUNCTION" ->
                "⚠ Неисправность"

            "REPAIR" ->
                "Ремонт"

            "MAINTENANCE" ->
                "Техническое обслуживание"

            "INSPECTION" ->
                "Осмотр"

            "WEATHER" ->
                "Погодные условия"

            else ->
                "Связанное событие"
        }
    }

    private fun formatFarmEventDate(
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

            return "%02d.%04d".format(
                month,
                event.eventYear
            )
        }

        return event.eventYear.toString()
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

    private fun formatProcessPeriod(
        process: SeasonProcess
    ): String {

        return if (
            process.endDate.isNullOrBlank()
        ) {

            "Начало: ${
                formatDate(
                    process.startDate
                )
            }"

        } else {

            "Период: ${
                formatDate(
                    process.startDate
                )
            } — ${
                formatDate(
                    process.endDate
                )
            }"
        }
    }

    private fun formatWorkPeriod(
        work: SeasonWork
    ): String {

        return if (
            !work.endDate.isNullOrBlank() &&
            work.endDate != work.workDate
        ) {

            "Период: ${
                formatDate(
                    work.workDate
                )
            } — ${
                formatDate(
                    work.endDate
                )
            }"

        } else {

            "Дата: ${
                formatDate(
                    work.workDate
                )
            }"
        }
    }

    private fun statusName(
        status: String
    ): String {

        return when (
            status
        ) {

            "IN_PROGRESS" ->
                "В работе"

            "COMPLETED" ->
                "Завершено"

            else ->
                status
        }
    }

    private fun formatWorkDay(
        day: SeasonWorkDay
    ): String {

        val period =
            if (
                !day.endDate.isNullOrBlank() &&
                day.endDate != day.workDate
            ) {

                "${
                    formatDate(
                        day.workDate
                    )
                } — ${
                    formatDate(
                        day.endDate
                    )
                }"

            } else {

                formatDate(
                    day.workDate
                )
            }

        val stage =
            when (
                day.stage
            ) {

                "START" ->
                    "Начало"

                "PROGRESS" ->
                    "Продолжение"

                "END" ->
                    "Окончание"

                else ->
                    "Работа"
            }

        return buildString {

            append(period)
            append(" — ")
            append(stage)

            day.areaHa
                ?.let {

                    append("\nЗа этап: ")
                    append(
                        formatArea(it)
                    )
                    append(" га")
                }

            day.cumulativeAreaHa
                ?.let {

                    append("\nСуммарно: ")
                    append(
                        formatArea(it)
                    )
                    append(" га")
                }

            day.note
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {

                    append("\n")
                    append(it)
                }
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
        value: String?
    ): String {

        if (
            value.isNullOrBlank()
        ) {
            return "—"
        }

        return try {

            val source =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val target =
                SimpleDateFormat(
                    "dd.MM.yyyy",
                    Locale.getDefault()
                )

            target.format(
                source.parse(value)
                    ?: return value
            )

        } catch (
            error: Exception
        ) {
            value
        }
    }

    private fun formatArea(
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
                    ".",
                    ","
                )
        }
    }
}
