package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "fields"
)
data class FieldEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val areaHa: Double? = null,

    val location: String? = null,

    val note: String? = null,

    val isActive: Boolean = true
)