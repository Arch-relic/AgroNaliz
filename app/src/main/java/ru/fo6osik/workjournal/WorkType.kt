package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_types",
    indices = [
        Index(
            value = ["name"],
            unique = true
        )
    ]
)
data class WorkType(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val isActive: Boolean = true
)
