package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tractor_models",
    foreignKeys = [
        ForeignKey(
            entity = TractorBrand::class,
            parentColumns = ["id"],
            childColumns = ["brandId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["brandId"]),
        Index(
            value = ["brandId", "name"],
            unique = true
        )
    ]
)
data class TractorModel(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val brandId: Int,

    val name: String,
	
	val baseModelId: Int? = null,

    val userCreated: Boolean = false
)