package ru.fo6osik.workjournal

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.room.withTransaction
import java.util.UUID

class AddSeasonWorkActivity : AppCompatActivity() {

    private lateinit var textWorkDate: TextView
    private lateinit var spinnerWorkType: Spinner
    private lateinit var spinnerField: Spinner
    private lateinit var buttonSelectVehicles: Button
    private lateinit var editWorkArea: EditText
    private lateinit var editWorkNote: EditText

    private val calendar =
        Calendar.getInstance()

    private var workTypes:
        List<WorkType> =
        emptyList()

    private var fields:
        List<FieldEntity> =
        emptyList()

    private var vehicles:
        List<Vehicle> =
        emptyList()

    private val selectedVehicleIds =
        mutableSetOf<Int>()

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
            R.layout.activity_add_season_work
        )

        textWorkDate =
            findViewById(
                R.id.textWorkDate
            )

        spinnerWorkType =
            findViewById(
                R.id.spinnerWorkType
            )

        spinnerField =
            findViewById(
                R.id.spinnerField
            )

        buttonSelectVehicles =
            findViewById(
                R.id.buttonSelectVehicles
            )

        editWorkArea =
            findViewById(
                R.id.editWorkArea
            )

        editWorkNote =
            findViewById(
                R.id.editWorkNote
            )

        editWorkArea.inputType =
            InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_FLAG_DECIMAL

        textWorkDate.setOnClickListener {
            showDatePicker()
        }

        buttonSelectVehicles.setOnClickListener {
            showVehiclePicker()
        }

        findViewById<Button>(
            R.id.buttonSaveSeasonWork
        ).setOnClickListener {
            saveWork()
        }

        updateDateText()
        loadReferenceData()
    }

    private fun loadReferenceData() {

        lifecycleScope.launch {

            workTypes =
                database
                    .workTypeDao()
                    .getActiveWorkTypes()

            fields =
                database
                    .fieldDao()
                    .getActiveFields()

            vehicles =
                database
                    .vehicleDao()
                    .getAll()
                    .sortedWith(
                        compareBy(
                            { it.brand },
                            { it.model }
                        )
                    )

            val workTypeNames =
                workTypes.map {
                    it.name
                }

            spinnerWorkType.adapter =
                ArrayAdapter(
                    this@AddSeasonWorkActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    workTypeNames
                )

            val fieldNames =
                mutableListOf(
                    "Без поля"
                )

            fieldNames.addAll(
                fields.map {
                    "${it.name} (${formatArea(it.areaHa)} га)"
                }
            )

            spinnerField.adapter =
                ArrayAdapter(
                    this@AddSeasonWorkActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    fieldNames
                )

            updateVehicleButton()
        }
    }

    private fun showDatePicker() {

        DatePickerDialog(
            this,
            { _, year, month, day ->

                calendar.set(
                    Calendar.YEAR,
                    year
                )

                calendar.set(
                    Calendar.MONTH,
                    month
                )

                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    day
                )

                updateDateText()
            },
            calendar.get(
                Calendar.YEAR
            ),
            calendar.get(
                Calendar.MONTH
            ),
            calendar.get(
                Calendar.DAY_OF_MONTH
            )
        ).show()
    }

    private fun updateDateText() {

        val formatter =
            SimpleDateFormat(
                "dd.MM.yyyy",
                Locale.getDefault()
            )

        textWorkDate.text =
            formatter.format(
                calendar.time
            )
    }

    private fun showVehiclePicker() {

        if (vehicles.isEmpty()) {

            Toast.makeText(
                this,
                "В списке техники пока нет машин",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val labels =
            vehicles
                .map {
                    buildVehicleName(it)
                }
                .toTypedArray()

        val checked =
            BooleanArray(
                vehicles.size
            ) { index ->

                selectedVehicleIds.contains(
                    vehicles[index].id
                )
            }

        AlertDialog.Builder(this)
            .setTitle(
                "Выберите технику"
            )
            .setMultiChoiceItems(
                labels,
                checked
            ) { _, which, isChecked ->

                val vehicleId =
                    vehicles[which].id

                if (isChecked) {
                    selectedVehicleIds.add(
                        vehicleId
                    )
                } else {
                    selectedVehicleIds.remove(
                        vehicleId
                    )
                }
            }
            .setNegativeButton(
                "Отмена",
                null
            )
            .setPositiveButton(
                "Готово"
            ) { _, _ ->
                updateVehicleButton()
            }
            .show()
    }

    private fun updateVehicleButton() {

        val selectedCount =
            selectedVehicleIds.size

        buttonSelectVehicles.text =
            when (selectedCount) {

                0 ->
                    "Техника: не выбрана"

                1 ->
                    "Техника: выбрана 1 единица"

                else ->
                    "Техника: выбрано $selectedCount"
            }
    }

    private fun saveWork() {

        val selectedWorkType =
            workTypes.getOrNull(
                spinnerWorkType.selectedItemPosition
            )

        if (selectedWorkType == null) {

            Toast.makeText(
                this,
                "Выберите вид работы",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val selectedField =
            if (
                spinnerField.selectedItemPosition <= 0
            ) {
                null
            } else {
                fields.getOrNull(
                    spinnerField.selectedItemPosition - 1
                )
            }

        val areaText =
            editWorkArea
                .text
                .toString()
                .trim()
                .replace(
                    ',',
                    '.'
                )

        val area =
            if (
                areaText.isBlank()
            ) {
                null
            } else {
                areaText.toDoubleOrNull()
            }

        if (
            areaText.isNotBlank() &&
            area == null
        ) {

            editWorkArea.error =
                "Проверьте площадь"

            return
        }

        if (
            area != null &&
            area <= 0.0
        ) {

            editWorkArea.error =
                "Площадь должна быть больше нуля"

            return
        }

        val note =
            editWorkNote
                .text
                .toString()
                .trim()
                .ifBlank {
                    null
                }

        val databaseDateFormatter =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            )

        val workDate =
            databaseDateFormatter.format(
                calendar.time
            )

        lifecycleScope.launch {

    val eventUid =
        UUID.randomUUID()
            .toString()

    database.withTransaction {

        /*
         * Сначала создаём главное событие.
         */
        database
            .farmEventDao()
            .insert(
                FarmEvent(
                    eventUid =
                        eventUid,

                    eventType =
                        "FIELD_WORK",

                    targetModule =
                        "SEASON_WORKS",

                    title =
                        selectedWorkType.name,

                    description =
                        note,

                    eventYear =
                        calendar.get(
                            Calendar.YEAR
                        ),

                    eventMonth =
                        calendar.get(
                            Calendar.MONTH
                        ) + 1,

                    eventDay =
                        calendar.get(
                            Calendar.DAY_OF_MONTH
                        ),

                    datePrecision =
                        "DAY",

                    payloadJson =
                        null,

                    targetRecordUid =
                        null
                )
            )

        /*
         * Затем профильные данные
         * сезонной работы.
         */
        val seasonWorkId =
            database
                .seasonWorkDao()
                .insertWork(
                    SeasonWork(
                        eventUid =
                            eventUid,

                        workDate =
                            workDate,

                        workTypeId =
                            selectedWorkType.id,

                        fieldId =
                            selectedField?.id,

                        areaHa =
                            area,

                        note =
                            note
                    )
                )
                .toInt()

        /*
         * И привязываем технику.
         */
        selectedVehicleIds
            .forEach { vehicleId ->

                val vehicle =
                    vehicles.firstOrNull {
                        it.id == vehicleId
                    }

                if (vehicle != null) {

                    database
                        .seasonWorkDao()
                        .insertVehicleRelation(
                            SeasonWorkVehicle(
                                seasonWorkId =
                                    seasonWorkId,

                                vehicleId =
                                    vehicle.id,

                                vehicleNameSnapshot =
                                    buildVehicleName(
                                        vehicle
                                    )
                            )
                        )
                }
            }
    }

    Toast.makeText(
        this@AddSeasonWorkActivity,
        "Запись сохранена",
        Toast.LENGTH_SHORT
    ).show()

    finish()
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

    private fun formatArea(
        area: Double?
    ): String {

        if (area == null) {
            return "—"
        }

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
