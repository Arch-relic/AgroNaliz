package ru.fo6osik.workjournal

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class HarvestProcessActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROCESS_ID =
            "harvest_process_id"
    }

    private lateinit var textTitle: TextView
    private lateinit var textStatus: TextView
    private lateinit var textStartDate: TextView
    private lateinit var textCombine: TextView
    private lateinit var textTotalBunkers: TextView
    private lateinit var textBunkerVolume: TextView
    private lateinit var textTotalVolume: TextView
    private lateinit var textAverageBunkerVolume: TextView
    private lateinit var textDaysEmpty: TextView
    private lateinit var dayList: LinearLayout
    private lateinit var buttonNewDay: Button
    private lateinit var buttonEditProcess: Button

    private var processId: Int = -1
    private var currentProcess: HarvestProcess? = null
    private var currentDays: List<HarvestDaySummary> =
        emptyList()

    private val crops =
        listOf(
            "Пшеница",
            "Ячмень",
            "Горох",
            "Подсолнечник",
            "Кукуруза",
            "Рапс",
            "Овёс",
            "Рожь",
            "Другое"
        )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        setContentView(
            R.layout.activity_harvest_process
        )

        processId =
            intent.getIntExtra(
                EXTRA_PROCESS_ID,
                -1
            )

        if (processId <= 0) {
            Toast.makeText(
                this,
                "Не удалось открыть уборочный процесс",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        textTitle =
            findViewById(
                R.id.textHarvestProcessTitle
            )

        textStatus =
            findViewById(
                R.id.textHarvestProcessStatus
            )

        textStartDate =
            findViewById(
                R.id.textHarvestProcessStartDate
            )

        textCombine =
            findViewById(
                R.id.textHarvestProcessCombine
            )

        textTotalBunkers =
            findViewById(
                R.id.textHarvestTotalBunkers
            )


        textBunkerVolume =
            findViewById(
                R.id.textHarvestProcessBunkerVolume
            )

        textTotalVolume =
            findViewById(
                R.id.textHarvestTotalVolume
            )

        textAverageBunkerVolume =
            findViewById(
                R.id.textHarvestAverageBunkerVolume
            )

        textDaysEmpty =
            findViewById(
                R.id.textHarvestDaysEmpty
            )

        dayList =
            findViewById(
                R.id.harvestDayList
            )

        buttonNewDay =
            findViewById(
                R.id.buttonNewHarvestDay
            )

        buttonEditProcess =
            findViewById(
                R.id.buttonEditHarvestProcess
            )

        buttonNewDay.setOnClickListener {
            showNewDayDialog()
        }

        buttonEditProcess.setOnClickListener {
            showEditProcessDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProcess()
    }

    private fun loadProcess() {
        Thread {
            try {
                val dao =
                    DatabaseProvider
                        .getDatabase(
                            applicationContext
                        )
                        .harvestDao()

                val process =
                    dao.getProcessById(
                        processId
                    )
                        ?: error(
                            "Уборочный процесс не найден"
                        )

                val days =
                    dao.getDaySummariesForProcess(
                        processId
                    )

                val bunkersByDay =
                    days.associate {
                        day ->
                        day.id to
                            dao.getBunkersForDay(
                                day.id
                            )
                    }

                runOnUiThread {
                    currentProcess =
                        process

                    currentDays =
                        days

                    renderProcess(
                        process,
                        days,
                        bunkersByDay
                    )
                }

            } catch (e: Throwable) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Ошибка загрузки уборочной карточки: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun renderProcess(
        process: HarvestProcess,
        days: List<HarvestDaySummary>,
        bunkersByDay: Map<Int, List<HarvestBunker>>
    ) {
        textTitle.text =
            "${process.fieldNameSnapshot} — ${process.crop}"

        textStatus.text =
            if (
                process.status ==
                "COMPLETED"
            ) {
                "Статус: Завершено"
            } else {
                "Статус: Не завершено"
            }

        textStartDate.text =
            "Начало: " +
                formatDateForUi(
                    process.startDate
                )

        textCombine.text =
            "Комбайн: ${process.combineNameSnapshot}"


        textBunkerVolume.text =
            if (
                process.bunkerVolumeM3 ==
                null
            ) {
                "Рабочий объём бункера: не задан"
            } else {
                "Рабочий объём бункера: " +
                    formatNumber(
                        process.bunkerVolumeM3
                    ) +
                    " м³"
            }

        val totalBunkers =
            days.sumOf {
                it.bunkerCount
            }

        textTotalBunkers.text =
            "Всего за все дни: " +
                bunkerCountText(
                    totalBunkers
                )


        val allBunkers =
            bunkersByDay
                .values
                .flatten()

        textTotalVolume.text =
            calculationText(
                label =
                    "Расчётный объём за все дни",
                bunkerVolumeM3 =
                    process.bunkerVolumeM3,
                bunkers =
                    allBunkers
            )

        textAverageBunkerVolume.text =
            averageText(
                bunkerVolumeM3 =
                    process.bunkerVolumeM3,
                bunkers =
                    allBunkers
            )

        renderDays(
            days,
            process.bunkerVolumeM3,
            bunkersByDay
        )

        val hasActiveDay =
            days.any {
                it.status ==
                    "ACTIVE"
            }

        buttonNewDay.visibility =
            if (
                process.status ==
                    "IN_PROGRESS" &&
                !hasActiveDay
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun renderDays(
        days: List<HarvestDaySummary>,
        bunkerVolumeM3: Double?,
        bunkersByDay: Map<Int, List<HarvestBunker>>
    ) {
        dayList.removeAllViews()

        if (days.isEmpty()) {
            textDaysEmpty.visibility =
                View.VISIBLE
            return
        }

        textDaysEmpty.visibility =
            View.GONE

        days.forEach {
                day ->

            val statusText =
                if (
                    day.status ==
                    "ACTIVE"
                ) {
                    "Текущий день"
                } else {
                    "Завершён"
                }

            val timeText =
                if (
                    day.endTime == null
                ) {
                    "${day.startTime}–—"
                } else {
                    "${day.startTime}–${day.endTime}"
                }

            val card =
                TextView(this).apply {
                    val dayBunkers =
                        bunkersByDay[
                            day.id
                        ] ?: emptyList()

                    val dayVolumeText =
                        shortCalculationText(
                            bunkerVolumeM3 =
                                bunkerVolumeM3,
                            bunkers =
                                dayBunkers
                        )

                    text =
                        "${formatDateForUi(day.workDate)}\n" +
                            "${bunkerCountText(day.bunkerCount)}" +
                            dayVolumeText +
                            "\n" +
                            "$timeText • $statusText"

                    textSize =
                        17f

                    setTextColor(
                        Color.BLACK
                    )

                    setPadding(
                        dp(16),
                        dp(14),
                        dp(16),
                        dp(14)
                    )

                    setBackgroundColor(
                        if (
                            day.status ==
                            "ACTIVE"
                        ) {
                            Color.rgb(
                                232,
                                245,
                                233
                            )
                        } else {
                            Color.rgb(
                                242,
                                242,
                                242
                            )
                        }
                    )

                    if (
                        day.status ==
                        "ACTIVE"
                    ) {
                        setTypeface(
                            typeface,
                            Typeface.BOLD
                        )
                    }

                    isClickable =
                        true

                    isFocusable =
                        true

                    setOnClickListener {
                        startActivity(
                            Intent(
                                this@HarvestProcessActivity,
                                HarvestDayActivity::class.java
                            ).putExtra(
                                HarvestDayActivity.EXTRA_DAY_ID,
                                day.id
                            )
                        )
                    }
                }

            val params =
                LinearLayout
                    .LayoutParams(
                        LinearLayout
                            .LayoutParams
                            .MATCH_PARENT,
                        LinearLayout
                            .LayoutParams
                            .WRAP_CONTENT
                    ).apply {
                        bottomMargin =
                            dp(10)
                    }

            dayList.addView(
                card,
                params
            )
        }
    }

    private fun showNewDayDialog() {
        val process =
            currentProcess
                ?: return

        if (
            process.status !=
            "IN_PROGRESS"
        ) {
            return
        }

        if (
            currentDays.any {
                it.status ==
                    "ACTIVE"
            }
        ) {
            Toast.makeText(
                this,
                "Сначала завершите текущий рабочий день",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogView =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.dialog_new_harvest_day,
                    null
                )

        val textDate =
            dialogView.findViewById<TextView>(
                R.id.textNewHarvestDayDate
            )

        val textStartTime =
            dialogView.findViewById<TextView>(
                R.id.textNewHarvestDayStartTime
            )

        val buttonConfirm =
            dialogView.findViewById<Button>(
                R.id.buttonConfirmNewHarvestDay
            )

        val buttonCancel =
            dialogView.findViewById<Button>(
                R.id.buttonCancelNewHarvestDay
            )

        val dateCalendar =
            Calendar.getInstance()

        val timeCalendar =
            Calendar.getInstance()

        fun updateDate() {
            textDate.text =
                SimpleDateFormat(
                    "dd.MM.yyyy",
                    Locale("ru")
                ).format(
                    dateCalendar.time
                )
        }

        fun updateTime() {
            textStartTime.text =
                formatCalendarTime(
                    timeCalendar
                )
        }

        updateDate()
        updateTime()

        textDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    dateCalendar.set(
                        Calendar.YEAR,
                        year
                    )

                    dateCalendar.set(
                        Calendar.MONTH,
                        month
                    )

                    dateCalendar.set(
                        Calendar.DAY_OF_MONTH,
                        day
                    )

                    updateDate()
                },
                dateCalendar.get(
                    Calendar.YEAR
                ),
                dateCalendar.get(
                    Calendar.MONTH
                ),
                dateCalendar.get(
                    Calendar.DAY_OF_MONTH
                )
            ).show()
        }

        textStartTime.setOnClickListener {
            android.app.TimePickerDialog(
                this,
                { _, hour, minute ->
                    timeCalendar.set(
                        Calendar.HOUR_OF_DAY,
                        hour
                    )

                    timeCalendar.set(
                        Calendar.MINUTE,
                        minute
                    )

                    updateTime()
                },
                timeCalendar.get(
                    Calendar.HOUR_OF_DAY
                ),
                timeCalendar.get(
                    Calendar.MINUTE
                ),
                true
            ).show()
        }

        val dialog =
            AlertDialog
                .Builder(this)
                .setView(
                    dialogView
                )
                .create()

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        buttonConfirm.setOnClickListener {
            val dateValue =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).format(
                    dateCalendar.time
                )

            if (
                dateValue <
                process.startDate
            ) {
                Toast.makeText(
                    this,
                    "Дата рабочего дня не может быть раньше начала уборки",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            if (
                currentDays.any {
                    it.workDate ==
                        dateValue
                }
            ) {
                Toast.makeText(
                    this,
                    "На эту дату рабочий день уже существует",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            buttonConfirm.isEnabled =
                false

            Thread {
                try {
                    val dayId =
                        DatabaseProvider
                            .getDatabase(
                                applicationContext
                            )
                            .harvestDao()
                            .insertDay(
                                HarvestDay(
                                    dayUid =
                                        "harvest-day-" +
                                            UUID
                                                .randomUUID()
                                                .toString(),
                                    processId =
                                        processId,
                                    workDate =
                                        dateValue,
                                    startTime =
                                        textStartTime
                                            .text
                                            .toString(),
                                    endTime =
                                        null,
                                    status =
                                        "ACTIVE"
                                )
                            )
                            .toInt()

                    runOnUiThread {
                        dialog.dismiss()

                        loadProcess()

                        startActivity(
                            Intent(
                                this,
                                HarvestDayActivity::class.java
                            ).putExtra(
                                HarvestDayActivity.EXTRA_DAY_ID,
                                dayId
                            )
                        )
                    }

                } catch (
                    e: Throwable
                ) {
                    runOnUiThread {
                        buttonConfirm.isEnabled =
                            true

                        Toast.makeText(
                            this,
                            "Ошибка создания рабочего дня: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.start()
        }

        dialog.show()
    }

    private fun showEditProcessDialog() {
        val process =
            currentProcess
                ?: return

        val dialogView =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.dialog_edit_harvest_process,
                    null
                )

        val textDate =
            dialogView.findViewById<TextView>(
                R.id.textEditHarvestStartDate
            )

        val spinnerField =
            dialogView.findViewById<Spinner>(
                R.id.spinnerEditHarvestField
            )

        val spinnerCrop =
            dialogView.findViewById<Spinner>(
                R.id.spinnerEditHarvestCrop
            )

        val spinnerCombine =
            dialogView.findViewById<Spinner>(
                R.id.spinnerEditHarvestCombine
            )


        val editBunkerVolume =
            dialogView.findViewById<EditText>(
                R.id.editHarvestBunkerVolume
            )

        editBunkerVolume.setText(
            process.bunkerVolumeM3
                ?.toString()
                ?: ""
        )

        val buttonSave =
            dialogView.findViewById<Button>(
                R.id.buttonConfirmEditHarvestProcess
            )

        val buttonCancel =
            dialogView.findViewById<Button>(
                R.id.buttonCancelEditHarvestProcess
            )

        val dateCalendar =
            parseDateToCalendar(
                process.startDate
            )

        fun updateDateText() {
            textDate.text =
                SimpleDateFormat(
                    "dd.MM.yyyy",
                    Locale("ru")
                ).format(
                    dateCalendar.time
                )
        }

        updateDateText()

        textDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    dateCalendar.set(
                        Calendar.YEAR,
                        year
                    )
                    dateCalendar.set(
                        Calendar.MONTH,
                        month
                    )
                    dateCalendar.set(
                        Calendar.DAY_OF_MONTH,
                        day
                    )
                    updateDateText()
                },
                dateCalendar.get(
                    Calendar.YEAR
                ),
                dateCalendar.get(
                    Calendar.MONTH
                ),
                dateCalendar.get(
                    Calendar.DAY_OF_MONTH
                )
            ).show()
        }

        val dialog =
            AlertDialog
                .Builder(this)
                .setView(
                    dialogView
                )
                .create()

        buttonSave.isEnabled =
            false

        Thread {
            try {
                val database =
                    DatabaseProvider
                        .getDatabase(
                            applicationContext
                        )
                        .openHelper
                        .readableDatabase

                val fieldIds =
                    mutableListOf<Int>()

                val fieldLabels =
                    mutableListOf<String>()

                database.query(
                    "SELECT id, name FROM fields " +
                        "WHERE isActive = 1 OR id = ${process.fieldId ?: -1} " +
                        "ORDER BY name COLLATE NOCASE"
                ).use { cursor ->
                    val idIndex =
                        cursor.getColumnIndexOrThrow(
                            "id"
                        )

                    val nameIndex =
                        cursor.getColumnIndexOrThrow(
                            "name"
                        )

                    while (
                        cursor.moveToNext()
                    ) {
                        fieldIds.add(
                            cursor.getInt(
                                idIndex
                            )
                        )

                        fieldLabels.add(
                            cursor.getString(
                                nameIndex
                            )
                        )
                    }
                }

                val combineIds =
                    mutableListOf<Int>()

                val combineLabels =
                    mutableListOf<String>()

                database.query(
                    "SELECT id, brand, model, number " +
                        "FROM vehicles " +
                        "WHERE type = 'Комбайн' " +
                        "OR id = ${process.combineVehicleId ?: -1} " +
                        "ORDER BY brand COLLATE NOCASE, model COLLATE NOCASE"
                ).use { cursor ->
                    val idIndex =
                        cursor.getColumnIndexOrThrow(
                            "id"
                        )

                    val brandIndex =
                        cursor.getColumnIndexOrThrow(
                            "brand"
                        )

                    val modelIndex =
                        cursor.getColumnIndexOrThrow(
                            "model"
                        )

                    val numberIndex =
                        cursor.getColumnIndexOrThrow(
                            "number"
                        )

                    while (
                        cursor.moveToNext()
                    ) {
                        val id =
                            cursor.getInt(
                                idIndex
                            )

                        val brand =
                            cursor.getString(
                                brandIndex
                            ).orEmpty()

                        val model =
                            cursor.getString(
                                modelIndex
                            ).orEmpty()

                        val number =
                            cursor.getString(
                                numberIndex
                            ).orEmpty()

                        val label =
                            buildString {
                                append(
                                    brand
                                )

                                if (
                                    brand.isNotBlank() &&
                                    model.isNotBlank()
                                ) {
                                    append(" ")
                                }

                                append(
                                    model
                                )

                                if (
                                    number.isNotBlank()
                                ) {
                                    append(" • ")
                                    append(
                                        number
                                    )
                                }
                            }

                        combineIds.add(
                            id
                        )

                        combineLabels.add(
                            label
                        )
                    }
                }

                val combineVolumeMap =
                    mutableMapOf<Int, Double?>()

                database.query(
                    "SELECT vehicleId, grainTankVolumeM3 " +
                        "FROM combine_specs"
                ).use {
                    cursor ->

                    val vehicleIndex =
                        cursor.getColumnIndexOrThrow(
                            "vehicleId"
                        )

                    val volumeIndex =
                        cursor.getColumnIndexOrThrow(
                            "grainTankVolumeM3"
                        )

                    while (
                        cursor.moveToNext()
                    ) {
                        combineVolumeMap[
                            cursor.getInt(
                                vehicleIndex
                            )
                        ] =
                            if (
                                cursor.isNull(
                                    volumeIndex
                                )
                            ) {
                                null
                            } else {
                                cursor.getDouble(
                                    volumeIndex
                                )
                            }
                    }
                }

                runOnUiThread {
                    spinnerField.adapter =
                        ArrayAdapter(
                            this,
                            android.R.layout
                                .simple_spinner_dropdown_item,
                            fieldLabels
                        )

                    spinnerCombine.adapter =
                        ArrayAdapter(
                            this,
                            android.R.layout
                                .simple_spinner_dropdown_item,
                            combineLabels
                        )

                    spinnerCrop.adapter =
                        ArrayAdapter(
                            this,
                            android.R.layout
                                .simple_spinner_dropdown_item,
                            crops
                        )

                    val fieldPosition =
                        fieldIds.indexOf(
                            process.fieldId
                        )

                    if (
                        fieldPosition >= 0
                    ) {
                        spinnerField.setSelection(
                            fieldPosition
                        )
                    }

                    val combinePosition =
                        combineIds.indexOf(
                            process.combineVehicleId
                        )

                    if (
                        combinePosition >= 0
                    ) {
                        spinnerCombine.setSelection(
                            combinePosition
                        )
                    }

                    val cropPosition =
                        crops.indexOf(
                            process.crop
                        )

                    spinnerCrop.setSelection(
                        if (
                            cropPosition >= 0
                        ) {
                            cropPosition
                        } else {
                            crops.lastIndex
                        }
                    )

                    buttonSave.isEnabled =
                        fieldIds.isNotEmpty() &&
                            combineIds.isNotEmpty()

                    buttonSave.setOnClickListener {
                        val newStartDate =
                            SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.US
                            ).format(
                                dateCalendar.time
                            )

                        val firstDay =
                            currentDays
                                .minByOrNull {
                                    it.workDate
                                }

                        if (
                            firstDay != null &&
                            newStartDate >
                                firstDay.workDate
                        ) {
                            Toast.makeText(
                                this,
                                "Дата начала не может быть позже первого рабочего дня",
                                Toast.LENGTH_LONG
                            ).show()

                            return@setOnClickListener
                        }

                        val fieldPos =
                            spinnerField
                                .selectedItemPosition

                        val combinePos =
                            spinnerCombine
                                .selectedItemPosition

                        if (
                            fieldPos !in
                            fieldIds.indices ||
                            combinePos !in
                            combineIds.indices
                        ) {
                            return@setOnClickListener
                        }

                        buttonSave.isEnabled =
                            false

                        Thread {
                            try {
                                DatabaseProvider
                                    .getDatabase(
                                        applicationContext
                                    )
                                    .harvestDao()
                                    .updateProcessDetails(
                                        processId =
                                            processId,
                                        startDate =
                                            newStartDate,
                                        fieldId =
                                            fieldIds[
                                                fieldPos
                                            ],
                                        fieldNameSnapshot =
                                            fieldLabels[
                                                fieldPos
                                            ],
                                        crop =
                                            crops[
                                                spinnerCrop
                                                    .selectedItemPosition
                                            ],
                                        combineVehicleId =
                                            combineIds[
                                                combinePos
                                            ],
                                        combineNameSnapshot =
                                            combineLabels[
                                                combinePos
                                            ],
                                        bunkerVolumeM3 =
                                            parseVolume(
                                                editBunkerVolume
                                                    .text
                                                    .toString()
                                            )
                                                ?: combineVolumeMap[
                                                    combineIds[
                                                        combinePos
                                                    ]
                                                ]
                                    )

                                runOnUiThread {
                                    dialog.dismiss()
                                    loadProcess()
                                }

                            } catch (
                                e: Throwable
                            ) {
                                runOnUiThread {
                                    buttonSave.isEnabled =
                                        true

                                    Toast.makeText(
                                        this,
                                        "Ошибка редактирования карточки: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }.start()
                    }
                }

            } catch (
                e: Throwable
            ) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Ошибка загрузки данных: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    dialog.dismiss()
                }
            }
        }.start()

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun coefficientRange(
        fillLevel: String
    ): Pair<Double, Double> {
        return when (
            fillLevel
        ) {
            "FULL" ->
                1.0 to 1.0

            "HALF" ->
                0.5 to 0.5

            "MORE_THAN_HALF" ->
                0.5 to 1.0

            "LESS_THAN_HALF" ->
                0.0 to 0.5

            else ->
                0.0 to 0.0
        }
    }

    private fun totalVolumeRange(
        bunkerVolumeM3: Double?,
        bunkers: List<HarvestBunker>
    ): Pair<Double, Double>? {
        if (
            bunkerVolumeM3 ==
            null
        ) {
            return null
        }

        var minEquivalent =
            0.0

        var maxEquivalent =
            0.0

        bunkers.forEach {
            bunker ->

            val range =
                coefficientRange(
                    bunker.fillLevel
                )

            minEquivalent +=
                range.first

            maxEquivalent +=
                range.second
        }

        return (
            minEquivalent *
                bunkerVolumeM3
            ) to (
            maxEquivalent *
                bunkerVolumeM3
            )
    }

    private fun calculationText(
        label: String,
        bunkerVolumeM3: Double?,
        bunkers: List<HarvestBunker>
    ): String {
        val range =
            totalVolumeRange(
                bunkerVolumeM3,
                bunkers
            )
                ?: return "$label: не настроен"

        return if (
            kotlin.math.abs(
                range.first -
                    range.second
            ) <
            0.0001
        ) {
            "$label: ${formatNumber(range.first)} м³"
        } else {
            "$label: ${formatNumber(range.first)}–" +
                "${formatNumber(range.second)} м³"
        }
    }

    private fun averageText(
        bunkerVolumeM3: Double?,
        bunkers: List<HarvestBunker>
    ): String {
        if (
            bunkers.isEmpty()
        ) {
            return "Средний объём бункера: —"
        }

        val range =
            totalVolumeRange(
                bunkerVolumeM3,
                bunkers
            )
                ?: return "Средний объём бункера: не настроен"

        val minAverage =
            range.first /
                bunkers.size

        val maxAverage =
            range.second /
                bunkers.size

        return if (
            kotlin.math.abs(
                minAverage -
                    maxAverage
            ) <
            0.0001
        ) {
            "Средний объём бункера: " +
                "${formatNumber(minAverage)} м³"
        } else {
            "Средний объём бункера: " +
                "${formatNumber(minAverage)}–" +
                "${formatNumber(maxAverage)} м³"
        }
    }

    private fun shortCalculationText(
        bunkerVolumeM3: Double?,
        bunkers: List<HarvestBunker>
    ): String {
        val range =
            totalVolumeRange(
                bunkerVolumeM3,
                bunkers
            )
                ?: return ""

        return if (
            kotlin.math.abs(
                range.first -
                    range.second
            ) <
            0.0001
        ) {
            " • ${formatNumber(range.first)} м³"
        } else {
            " • ${formatNumber(range.first)}–" +
                "${formatNumber(range.second)} м³"
        }
    }

    private fun parseVolume(
        value: String
    ): Double? {
        return value
            .trim()
            .replace(
                ",",
                "."
            )
            .toDoubleOrNull()
            ?.takeIf {
                it >
                    0.0
            }
    }

    private fun formatNumber(
        value: Double
    ): String {
        return String.format(
            Locale("ru"),
            "%.2f",
            value
        )
            .trimEnd('0')
            .trimEnd(',')
    }

    private fun bunkerCountText(
        count: Int
    ): String {
        val lastTwo =
            count % 100

        val last =
            count % 10

        val word =
            if (
                lastTwo in 11..14
            ) {
                "бункеров"
            } else {
                when (last) {
                    1 ->
                        "бункер"

                    2, 3, 4 ->
                        "бункера"

                    else ->
                        "бункеров"
                }
            }

        return "$count $word"
    }

    private fun formatCalendarTime(
        calendar: Calendar
    ): String {
        return String.format(
            Locale.US,
            "%02d:%02d",
            calendar.get(
                Calendar.HOUR_OF_DAY
            ),
            calendar.get(
                Calendar.MINUTE
            )
        )
    }

    private fun parseDateToCalendar(
        value: String
    ): Calendar {
        val calendar =
            Calendar.getInstance()

        try {
            val date =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).parse(
                    value
                )

            if (
                date != null
            ) {
                calendar.time =
                    date
            }

        } catch (
            _: Exception
        ) {
        }

        return calendar
    }

    private fun formatDateForUi(
        value: String
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
                    Locale("ru")
                )

            val date =
                source.parse(
                    value
                )

            if (
                date == null
            ) {
                value
            } else {
                target.format(
                    date
                )
            }

        } catch (
            _: Exception
        ) {
            value
        }
    }

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources
                    .displayMetrics
                    .density
            ).toInt()
    }
}
