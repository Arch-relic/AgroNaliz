package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "farm_event_vehicles",
    foreignKeys = [
        ForeignKey(
            entity = FarmEvent::class,
            parentColumns = ["eventUid"],
            childColumns = ["eventUid"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(
            value = [
                "eventUid",
                "vehicleId"
            ],
            unique = true
        ),
        Index(value = ["vehicleId"])
    ]
)
data class FarmEventVehicle(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val eventUid: String,

    val vehicleId: Int,

    val relationType: String = "RELATED",

    val note: String? = null
)
