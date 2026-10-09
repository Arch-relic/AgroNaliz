package ru.fo6osik.workjournal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MaintenanceDetailsActivity : AppCompatActivity() {

    private var maintenanceId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_maintenance_details
        )

        maintenanceId =
            intent.getIntExtra(
                "maintenance_id",
                -1
            )

        if (maintenanceId == -1) {
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        loadMaintenance()
    }

    private fun loadMaintenance() {

        lifecycleScope.launch {

            val database =
                DatabaseProvider
                    .getDatabase(applicationContext)

            val record =
                database
                    .maintenanceDao()
                    .getById(maintenanceId)

            if (record == null) {
                finish()
                return@launch
            }

            val vehicle =
                database
                    .vehicleDao()
                    .getById(record.vehicleId)

            val vehicleName =
                if (vehicle != null)
                    "${vehicle.brand} ${vehicle.model}"
                else
                    "Неизвестная техника"

            val formatter =
                SimpleDateFormat(
                    "dd.MM.yyyy",
                    Locale.getDefault()
                )

            val meterText =
                when {

                    record.meterValue == null ->
                        "Не указано"

                    record.meterType == "km" ->
                        "${record.meterValue} км"

                    else ->
                        "${record.meterValue} м/ч"
                }

            findViewById<TextView>(
                R.id.textMaintenanceVehicle
            ).text =
                vehicleName

            findViewById<TextView>(
                R.id.textMaintenanceDate
            ).text =
                "Дата: ${
                    formatMaintenanceDate(
                        record,
                        formatter
                    )
                }"

            findViewById<TextView>(
                R.id.textMaintenanceMeter
            ).text =
                "Показания: $meterText"

            findViewById<TextView>(
                R.id.textMaintenanceWork
            ).text =
                record.work

            findViewById<TextView>(
                R.id.textMaintenanceNote
            ).text =
                if (record.note.isBlank())
                    "Нет"
                else
                    record.note

            val buttonEdit =
                findViewById<Button>(
                    R.id.buttonEditMaintenance
                )

            val buttonDelete =
                findViewById<Button>(
                    R.id.buttonDeleteMaintenance
                )

            buttonEdit.setOnClickListener {

                val intent = Intent(
                    this@MaintenanceDetailsActivity,
                    MaintenanceActivity::class.java
                )

                intent.putExtra(
                    "maintenance_id",
                    record.id
                )

                startActivity(intent)
            }

            buttonDelete.setOnClickListener {

                AlertDialog.Builder(
                    this@MaintenanceDetailsActivity
                )
                    .setTitle(
                        "Удаление записи ТО"
                    )
                    .setMessage(
                        "Удалить эту запись ТО для $vehicleName?"
                    )
                    .setPositiveButton(
                        "Удалить"
                    ) { _, _ ->

                        lifecycleScope.launch {

                            database
                                .maintenanceDao()
                                .delete(record)

                            finish()
                        }
                    }
                    .setNegativeButton(
                        "Отмена",
                        null
                    )
                    .show()
            }
        }
    }

    private fun formatMaintenanceDate(
        record: MaintenanceRecord,
        dayFormatter: SimpleDateFormat
    ): String {

        if (
            record.datePrecision == "MONTH"
        ) {

            val calendar =
                Calendar.getInstance().apply {

                    timeInMillis =
                        record.date
                }

            val month =
                when (
                    calendar.get(
                        Calendar.MONTH
                    )
                ) {

                    Calendar.JANUARY -> "Январь"
                    Calendar.FEBRUARY -> "Февраль"
                    Calendar.MARCH -> "Март"
                    Calendar.APRIL -> "Апрель"
                    Calendar.MAY -> "Май"
                    Calendar.JUNE -> "Июнь"
                    Calendar.JULY -> "Июль"
                    Calendar.AUGUST -> "Август"
                    Calendar.SEPTEMBER -> "Сентябрь"
                    Calendar.OCTOBER -> "Октябрь"
                    Calendar.NOVEMBER -> "Ноябрь"
                    Calendar.DECEMBER -> "Декабрь"

                    else -> ""
                }

            return "$month ${
                calendar.get(
                    Calendar.YEAR
                )
            }"
        }

        return dayFormatter.format(
            Date(
                record.date
            )
        )
    }
}
