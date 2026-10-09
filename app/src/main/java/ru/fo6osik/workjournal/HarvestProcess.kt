package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "harvest_processes",
    foreignKeys = [
        ForeignKey(
            entity = FieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["combineVehicleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["processUid"], unique = true),
        Index(value = ["fieldId"]),
        Index(value = ["combineVehicleId"]),
        Index(value = ["startDate"]),
        Index(value = ["status"])
    ]
)
data class HarvestProcess(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val processUid: String,

    // YYYY-MM-DD
    val startDate: String,

    // YYYY-MM-DD; null = уборочная работа ещё продолжается
    val endDate: String? = null,

    // IN_PROGRESS / COMPLETED
    val status: String = "IN_PROGRESS",

    // Связь с существующим полем.
    val fieldId: Int?,

    // Резервное название на случай удаления поля.
    val fieldNameSnapshot: String,

    val crop: String,

    // Выбранный комбайн из парка техники.
    val combineVehicleId: Int?,

    // Резервное имя техники на момент начала уборки.
    val combineNameSnapshot: String,

    // Рабочий объём бункера конкретного процесса.
    // Пока может быть null; позже будет заполняться из ТТХ комбайна.
    val bunkerVolumeM3: Double? = null,

    val createdAt: Long = System.currentTimeMillis()
)
