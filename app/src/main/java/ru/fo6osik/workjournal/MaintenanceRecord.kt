package ru.fo6osik.workjournal

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["vehicleId"]),
        Index(
            value = ["eventUid"],
            unique = true
        )
    ]
)
data class MaintenanceRecord(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // К какой технике относится ТО
    val vehicleId: Int,

    // Внутренняя дата для сортировки и выборок.
    // Для MONTH используется первое число месяца,
    // но интерфейс обязан показывать только месяц и год.
    val date: Long,

    // Пробег или моточасы на момент ТО
    val meterValue: Int?,

    // Тип показателя: "km" или "mh"
    val meterType: String,

    // Что было выполнено
    val work: String,

    // Дополнительное примечание
    val note: String = "",

    // Связь с главным событием журнала.
    // У старых записей может оставаться null.
    val eventUid: String? = null,

    // DAY / MONTH.
    @ColumnInfo(defaultValue = "'DAY'")
    val datePrecision: String = "DAY"
)
