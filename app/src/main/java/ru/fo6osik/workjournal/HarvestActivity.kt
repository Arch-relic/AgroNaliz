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

class HarvestActivity : AppCompatActivity() {

    private lateinit var buttonStartHarvestProcess: Button
    private lateinit var textHarvestEmpty: TextView
    private lateinit var harvestProcessList: LinearLayout

    private val fieldIds = mutableListOf<Int>()
    private val fieldLabels = mutableListOf<String>()

    private val combineIds = mutableListOf<Int>()
    private val combineLabels = mutableListOf<String>()

    private val crops = listOf(
        "Выберите культуру",
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_harvest)

        buttonStartHarvestProcess =
            findViewById(R.id.buttonStartHarvestProcess)

        textHarvestEmpty =
            findViewById(R.id.textHarvestEmpty)

        harvestProcessList =
            findViewById(R.id.harvestProcessList)

        loadFieldsAndCombines()
        loadHarvestProcesses()

        buttonStartHarvestProcess.setOnClickListener {
            showStartHarvestDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadHarvestProcesses()
    }

    private fun loadFieldsAndCombines() {
        Thread {
            try {
                val database = DatabaseProvider
                    .getDatabase(applicationContext)
                    .openHelper
                    .readableDatabase

                val loadedFieldIds = mutableListOf<Int>()
                val loadedFieldLabels = mutableListOf<String>()

                database.query(
                    "SELECT id, name FROM fields " +
                        "WHERE isActive = 1 " +
                        "ORDER BY name COLLATE NOCASE"
                ).use { cursor ->
                    val idIndex =
                        cursor.getColumnIndexOrThrow("id")
                    val nameIndex =
                        cursor.getColumnIndexOrThrow("name")

                    while (cursor.moveToNext()) {
                        loadedFieldIds.add(
                            cursor.getInt(idIndex)
                        )
                        loadedFieldLabels.add(
                            cursor.getString(nameIndex)
                        )
                    }
                }

                val loadedCombineIds =
                    mutableListOf<Int>()
                val loadedCombineLabels =
                    mutableListOf<String>()

                database.query(
                    "SELECT id, brand, model, number " +
                        "FROM vehicles " +
                        "WHERE type = 'Комбайн' " +
                        "ORDER BY brand COLLATE NOCASE, " +
                        "model COLLATE NOCASE"
                ).use { cursor ->
                    val idIndex =
                        cursor.getColumnIndexOrThrow("id")
                    val brandIndex =
                        cursor.getColumnIndexOrThrow("brand")
                    val modelIndex =
                        cursor.getColumnIndexOrThrow("model")
                    val numberIndex =
                        cursor.getColumnIndexOrThrow("number")

                    while (cursor.moveToNext()) {
                        val id =
                            cursor.getInt(idIndex)
                        val brand =
                            cursor.getString(brandIndex)
                                .orEmpty()
                        val model =
                            cursor.getString(modelIndex)
                                .orEmpty()
                        val number =
                            cursor.getString(numberIndex)
                                .orEmpty()

                        val label = buildString {
                            append(brand)

                            if (
                                brand.isNotBlank() &&
                                model.isNotBlank()
                            ) {
                                append(" ")
                            }

                            append(model)

                            if (number.isNotBlank()) {
                                append(" • ")
                                append(number)
                            }
                        }

                        loadedCombineIds.add(id)
                        loadedCombineLabels.add(label)
                    }
                }

                runOnUiThread {
                    fieldIds.clear()
                    fieldIds.addAll(loadedFieldIds)

                    fieldLabels.clear()
                    fieldLabels.addAll(loadedFieldLabels)

                    combineIds.clear()
                    combineIds.addAll(
                        loadedCombineIds
                    )

                    combineLabels.clear()
                    combineLabels.addAll(
                        loadedCombineLabels
                    )
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Ошибка загрузки полей или комбайнов: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun loadHarvestProcesses() {
        Thread {
            try {
                val processes =
                    DatabaseProvider
                        .getDatabase(applicationContext)
                        .harvestDao()
                        .getAllProcessSummaries()

                runOnUiThread {
                    renderHarvestProcesses(
                        processes
                    )
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Ошибка загрузки уборочных работ: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun renderHarvestProcesses(
        processes: List<HarvestProcessSummary>
    ) {
        harvestProcessList.removeAllViews()

        if (processes.isEmpty()) {
            textHarvestEmpty.visibility =
                View.VISIBLE
            return
        }

        textHarvestEmpty.visibility =
            View.GONE

        processes.forEach { process ->

            val statusText =
                if (
                    process.status ==
                    "COMPLETED"
                ) {
                    "Завершено"
                } else {
                    "Не завершено"
                }

            val textView =
                TextView(this).apply {

                    val dateText =
                        formatDateForUi(
                            process.startDate
                        )

                    text =
                        "${process.fieldName} — ${process.crop}\n" +
                            "Начало: $dateText\n" +
                            "Комбайн: ${process.combineName}\n" +
                            "Рабочих дней: ${process.dayCount} • " +
                            "Бункеров: ${process.bunkerCount}\n" +
                            "Статус: $statusText"

                    textSize = 17f
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
                        Color.rgb(
                            238,
                            238,
                            238
                        )
                    )

                    if (
                        process.status ==
                        "IN_PROGRESS"
                    ) {
                        setTypeface(
                            typeface,
                            Typeface.BOLD
                        )
                    }

                    isClickable = true
                    isFocusable = true

                    setOnClickListener {
                        startActivity(
                            Intent(
                                this@HarvestActivity,
                                HarvestProcessActivity::class.java
                            ).putExtra(
                                HarvestProcessActivity.EXTRA_PROCESS_ID,
                                process.id
                            )
                        )
                    }
                }

            val params =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin =
                        dp(12)
                }

            harvestProcessList.addView(
                textView,
                params
            )
        }
    }

    private fun showStartHarvestDialog() {
        val dialogView =
            LayoutInflater.from(this).inflate(
                R.layout.dialog_start_harvest,
                null
            )

        val textStartDate =
            dialogView.findViewById<TextView>(
                R.id.textDialogHarvestStartDate
            )

        val spinnerField =
            dialogView.findViewById<Spinner>(
                R.id.spinnerDialogHarvestField
            )

        val spinnerCrop =
            dialogView.findViewById<Spinner>(
                R.id.spinnerDialogHarvestCrop
            )

        val spinnerCombine =
            dialogView.findViewById<Spinner>(
                R.id.spinnerDialogHarvestCombine
            )

        val buttonCancel =
            dialogView.findViewById<Button>(
                R.id.buttonDialogHarvestCancel
            )

        val buttonStart =
            dialogView.findViewById<Button>(
                R.id.buttonDialogHarvestStart
            )

        val selectedDate =
            Calendar.getInstance()

        fun updateDateText() {
            val formatter =
                SimpleDateFormat(
                    "dd.MM.yyyy",
                    Locale("ru")
                )

            textStartDate.text =
                formatter.format(
                    selectedDate.time
                )
        }

        updateDateText()

        textStartDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate.set(
                        Calendar.YEAR,
                        year
                    )
                    selectedDate.set(
                        Calendar.MONTH,
                        month
                    )
                    selectedDate.set(
                        Calendar.DAY_OF_MONTH,
                        day
                    )

                    updateDateText()
                },
                selectedDate.get(
                    Calendar.YEAR
                ),
                selectedDate.get(
                    Calendar.MONTH
                ),
                selectedDate.get(
                    Calendar.DAY_OF_MONTH
                )
            ).show()
        }

        spinnerField.adapter =
            ArrayAdapter(
                this,
                android.R.layout
                    .simple_spinner_dropdown_item,
                if (fieldLabels.isEmpty()) {
                    listOf(
                        "Нет доступных полей"
                    )
                } else {
                    fieldLabels
                }
            )

        spinnerCrop.adapter =
            ArrayAdapter(
                this,
                android.R.layout
                    .simple_spinner_dropdown_item,
                crops
            )

        spinnerCombine.adapter =
            ArrayAdapter(
                this,
                android.R.layout
                    .simple_spinner_dropdown_item,
                if (combineLabels.isEmpty()) {
                    listOf(
                        "В парке нет комбайнов"
                    )
                } else {
                    combineLabels
                }
            )

        buttonStart.isEnabled =
            fieldIds.isNotEmpty() &&
                combineIds.isNotEmpty()

        val dialog =
            AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        buttonStart.setOnClickListener {

            if (fieldIds.isEmpty()) {
                Toast.makeText(
                    this,
                    "Нет доступных полей",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (combineIds.isEmpty()) {
                Toast.makeText(
                    this,
                    "В парке техники нет комбайнов",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (
                spinnerCrop.selectedItemPosition <=
                0
            ) {
                Toast.makeText(
                    this,
                    "Выберите культуру",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val fieldPosition =
                spinnerField.selectedItemPosition

            val combinePosition =
                spinnerCombine.selectedItemPosition

            if (
                fieldPosition !in
                fieldIds.indices ||
                combinePosition !in
                combineIds.indices
            ) {
                return@setOnClickListener
            }

            val selectedFieldId =
                fieldIds[
                    fieldPosition
                ]

            val selectedFieldName =
                fieldLabels[
                    fieldPosition
                ]

            val selectedCombineId =
                combineIds[
                    combinePosition
                ]

            val selectedCombineName =
                combineLabels[
                    combinePosition
                ]

            val selectedCrop =
                crops[
                    spinnerCrop
                        .selectedItemPosition
                ]

            val dateFormatter =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                )

            val selectedDateValue =
                dateFormatter.format(
                    selectedDate.time
                )

            val timeFormatter =
                SimpleDateFormat(
                    "HH:mm",
                    Locale.US
                )

            val startTime =
                timeFormatter.format(
                    Calendar
                        .getInstance()
                        .time
                )

            buttonStart.isEnabled =
                false

            createHarvestProcess(
                selectedDateValue =
                    selectedDateValue,
                selectedFieldId =
                    selectedFieldId,
                selectedFieldName =
                    selectedFieldName,
                selectedCrop =
                    selectedCrop,
                selectedCombineId =
                    selectedCombineId,
                selectedCombineName =
                    selectedCombineName,
                startTime =
                    startTime,
                onSuccess = {
                    dialog.dismiss()
                    loadHarvestProcesses()

                    Toast.makeText(
                        this,
                        "Уборочный процесс создан",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onError = { error ->
                    buttonStart.isEnabled =
                        true

                    Toast.makeText(
                        this,
                        "Ошибка создания процесса: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        dialog.show()
    }

    private fun createHarvestProcess(
        selectedDateValue: String,
        selectedFieldId: Int,
        selectedFieldName: String,
        selectedCrop: String,
        selectedCombineId: Int,
        selectedCombineName: String,
        startTime: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        Thread {
            try {
                val database =
                    DatabaseProvider
                        .getDatabase(
                            applicationContext
                        )

                var bunkerVolumeM3: Double? =
                    null

                database
                    .openHelper
                    .readableDatabase
                    .query(
                        "SELECT grainTankVolumeM3 " +
                            "FROM combine_specs " +
                            "WHERE vehicleId = ? " +
                            "LIMIT 1",
                        arrayOf(
                            selectedCombineId
                        )
                    )
                    .use {
                        cursor ->

                        if (
                            cursor.moveToFirst() &&
                            !cursor.isNull(0)
                        ) {
                            bunkerVolumeM3 =
                                cursor.getDouble(0)
                        }
                    }

                val process =
                    HarvestProcess(
                        processUid =
                            "harvest-" +
                                UUID
                                    .randomUUID()
                                    .toString(),
                        startDate =
                            selectedDateValue,
                        fieldId =
                            selectedFieldId,
                        fieldNameSnapshot =
                            selectedFieldName,
                        crop =
                            selectedCrop,
                        combineVehicleId =
                            selectedCombineId,
                        combineNameSnapshot =
                            selectedCombineName,
                        bunkerVolumeM3 =
                            bunkerVolumeM3
                    )

                var newProcessId = 0

                database.runInTransaction {

                    newProcessId =
                        database
                            .harvestDao()
                            .insertProcess(
                                process
                            )
                            .toInt()

                    database
                        .harvestDao()
                        .insertDay(
                            HarvestDay(
                                dayUid =
                                    "harvest-day-" +
                                        UUID
                                            .randomUUID()
                                            .toString(),
                                processId =
                                    newProcessId,
                                workDate =
                                    selectedDateValue,
                                startTime =
                                    startTime,
                                endTime =
                                    null,
                                status =
                                    "ACTIVE"
                            )
                        )
                }

                runOnUiThread {
                    onSuccess()
                }

            } catch (e: Throwable) {
                runOnUiThread {
                    onError(e)
                }
            }
        }.start()
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
                source.parse(value)

            if (date == null) {
                value
            } else {
                target.format(date)
            }

        } catch (_: Exception) {
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
