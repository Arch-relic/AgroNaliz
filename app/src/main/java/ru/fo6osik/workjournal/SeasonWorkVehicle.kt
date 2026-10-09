package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "season_work_vehicles",
    foreignKeys = [
        ForeignKey(
            entity = SeasonWork::class,
            parentColumns = ["id"],
            childColumns = ["seasonWorkId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["seasonWorkId"]),
        Index(value = ["vehicleId"])
    ]
)
data class SeasonWorkVehicle(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val seasonWorkId: Int,

    // null возможен только если техника была удалена из парка.
    val vehicleId: Int?,

    // Страховочная копия названия на момент записи.
    // Пока техника существует, интерфейс должен показывать
    // её актуальное название по vehicleId.
    val vehicleNameSnapshot: String
)
