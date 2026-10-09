package ru.fo6osik.workjournal

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MaintenanceActivity : AppCompatActivity() {

    private var selectedDate: Long = System.currentTimeMillis()

    private var vehicles: List<Vehicle> = emptyList()

    private var editingRecordId: Int = -1
    private var editingRecord: MaintenanceRecord? = null
    private var formInitialized = false

    private lateinit var spinnerVehicle: Spinner
    private lateinit var spinnerMeterType: Spinner
    private lateinit var buttonDate: Button
    private lateinit var buttonSave: Button

    private lateinit var editMeterValue: EditText
    private lateinit var editWork: EditText
    private lateinit var editNote: EditText

    private lateinit var maintenanceHistoryContainer: LinearLayout
    private lateinit var textEmptyMaintenance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maintenance)

        editingRecordId =
            intent.getIntExtra("maintenance_id", -1)

        spinnerVehicle =
            findViewById(R.id.spinnerMaintenanceVehicle)

        spinnerMeterType =
            findViewById(R.id.spinnerMeterType)

        buttonDate =
            findViewById(R.id.buttonMaintenanceDate)

        buttonSave =
            findViewById(R.id.buttonSaveMaintenance)

        editMeterValue =
            findViewById(R.id.editMeterValue)

        editWork =
            findViewById(R.id.editMaintenanceWork)

        editNote =
            findViewById(R.id.editMaintenanceNote)

        maintenanceHistoryContainer =
            findViewById(R.id.maintenanceHistoryContainer)

        textEmptyMaintenance =
            findViewById(R.id.textEmptyMaintenance)

        val meterTypes = arrayOf(
            "Пробег, км",
            "Моточасы"
        )

        spinnerMeterType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            meterTypes
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        if (editingRecordId != -1) {
            buttonSave.text = "Сохранить изменения"
        }

        updateDateButton()

        buttonDate.setOnClickListener {
            showDatePicker()
        }

        buttonSave.setOnClickListener {
            saveMaintenance()
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {

            if (vehicles.isEmpty()) {
                loadVehicles()
            }

            if (
                editingRecordId != -1 &&
                !formInitialized
            ) {
                loadRecordForEditing()
            }

            loadHistory()
        }
    }

    private suspend fun loadVehicles() {

        vehicles = DatabaseProvider
            .getDatabase(applicationContext)
            .vehicleDao()
            .getAll()

        val vehicleNames =
            vehicles.map {
                "${it.brand} ${it.model}"
            }

        spinnerVehicle.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            vehicleNames
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }
    }

    private suspend fun loadRecordForEditing() {

        val record = DatabaseProvider
            .getDatabase(applicationContext)
            .maintenanceDao()
            .getById(editingRecordId)

        if (record == null) {
            finish()
            return
        }

        editingRecord = record

        selectedDate = record.date
        updateDateButton()

        val vehiclePosition =
            vehicles.indexOfFirst {
                it.id == record.vehicleId
            }

        if (vehiclePosition >= 0) {
            spinnerVehicle.setSelection(
                vehiclePosition
            )
        }

        if (record.meterType == "km") {
            spinnerMeterType.setSelection(0)
        } else {
            spinnerMeterType.setSelection(1)
        }

        if (record.meterValue != null) {
            editMeterValue.setText(
                record.meterValue.toString()
            )
        }

        editWork.setText(record.work)
        editNote.setText(record.note)

        formInitialized = true
    }

    private fun saveMaintenance() {

        if (vehicles.isEmpty()) {

            Toast.makeText(
                this,
                "Сначала добавьте технику",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val selectedVehicle =
            vehicles[
                spinnerVehicle.selectedItemPosition
            ]

        val meterValue =
            editMeterValue.text
                .toString()
                .trim()
                .toIntOrNull()

        val meterType =
            if (
                spinnerMeterType.selectedItemPosition == 0
            )
                "km"
            else
                "mh"

        val work =
            editWork.text
                .toString()
                .trim()

        val note =
            editNote.text
                .toString()
                .trim()

        if (work.isEmpty()) {

            Toast.makeText(
                this,
                "Укажите выполненные работы",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            val dao = DatabaseProvider
                .getDatabase(applicationContext)
                .maintenanceDao()

            if (editingRecordId == -1) {

                val record =
                    MaintenanceRecord(
                        vehicleId = selectedVehicle.id,
                        date = selectedDate,
                        meterValue = meterValue,
                        meterType = meterType,
                        work = work,
                        note = note
                    )

                dao.insert(record)

                Toast.makeText(
                    this@MaintenanceActivity,
                    "ТО сохранено",
                    Toast.LENGTH_SHORT
                ).show()

                editMeterValue.text.clear()
                editWork.text.clear()
                editNote.text.clear()

                loadHistory()

            } else {

                val oldRecord = editingRecord

                if (oldRecord == null) {

                    Toast.makeText(
                        this@MaintenanceActivity,
                        "Не удалось загрузить запись",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val updatedRecord =
                    oldRecord.copy(
                        vehicleId = selectedVehicle.id,
                        date = selectedDate,
                        meterValue = meterValue,
                        meterType = meterType,
                        work = work,
                        note = note
                    )

                dao.update(updatedRecord)

                Toast.makeText(
                    this@MaintenanceActivity,
                    "Изменения сохранены",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }
    }

    private suspend fun loadHistory() {

        val records = DatabaseProvider
            .getDatabase(applicationContext)
            .maintenanceDao()
            .getAll()

        maintenanceHistoryContainer.removeAllViews()

        if (records.isEmpty()) {

            textEmptyMaintenance.visibility =
                View.VISIBLE

            return
        }

        textEmptyMaintenance.visibility =
            View.GONE

        val vehicleMap =
            vehicles.associateBy { it.id }

        val formatter =
            SimpleDateFormat(
                "dd.MM.yyyy",
                Locale.getDefault()
            )

        records.forEach { record ->

            val vehicle =
                vehicleMap[record.vehicleId]

            val vehicleName =
                if (vehicle != null)
                    "${vehicle.brand} ${vehicle.model}"
                else
                    "Неизвестная техника"

            val meterText =
                when {

                    record.meterValue == null ->
                        "Показания не указаны"

                    record.meterType == "km" ->
                        "${record.meterValue} км"

                    else ->
                        "${record.meterValue} м/ч"
                }

            val item =
                TextView(this@MaintenanceActivity)

            item.text =
                "$vehicleName\n" +
                "${formatter.format(Date(record.date))}\n" +
                "$meterText\n" +
                record.work

            item.textSize = 18f

            item.setPadding(
                16,
                24,
                16,
                24
            )

            item.setOnClickListener {

                val intent = Intent(
                    this@MaintenanceActivity,
                    MaintenanceDetailsActivity::class.java
                )

                intent.putExtra(
                    "maintenance_id",
                    record.id
                )

                startActivity(intent)
            }

            maintenanceHistoryContainer.addView(item)
        }
    }

    private fun showDatePicker() {

        val calendar =
            Calendar.getInstance()

        calendar.timeInMillis =
            selectedDate

        DatePickerDialog(
            this,
            { _, year, month, day ->

                val selectedCalendar =
                    Calendar.getInstance()

                selectedCalendar.set(
                    year,
                    month,
                    day,
                    12,
                    0,
                    0
                )

                selectedCalendar.set(
                    Calendar.MILLISECOND,
                    0
                )

                selectedDate =
                    selectedCalendar.timeInMillis

                updateDateButton()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateButton() {

        val formatter =
            SimpleDateFormat(
                "dd.MM.yyyy",
                Locale.getDefault()
            )

        buttonDate.text =
            "Дата ТО: ${
                formatter.format(
                    Date(selectedDate)
                )
            }"
    }
}