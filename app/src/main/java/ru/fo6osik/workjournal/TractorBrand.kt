package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tractor_brands",
    indices = [
        Index(
            value = ["name"],
            unique = true
        )
    ]
)
data class TractorBrand(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val userCreated: Boolean = false
)