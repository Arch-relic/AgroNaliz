package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "combine_specs",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CombineSpec(
    @androidx.room.PrimaryKey
    val vehicleId: Int,

    // Паспортный объём зернового бункера, м³
    val grainTankVolumeM3: Double? = null
)
