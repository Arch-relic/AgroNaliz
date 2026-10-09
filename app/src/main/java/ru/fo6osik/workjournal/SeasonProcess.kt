package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "season_processes",
    foreignKeys = [
        ForeignKey(
            entity = FieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(
            value = ["processUid"],
            unique = true
        ),
        Index(value = ["fieldId"]),
        Index(value = ["startDate"])
    ]
)
data class SeasonProcess(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val processUid: String,

    val title: String,

    val fieldId: Int? = null,

    // YYYY-MM-DD
    val startDate: String,

    // YYYY-MM-DD; null = процесс продолжается
    val endDate: String? = null,

    // IN_PROGRESS / COMPLETED
    val status: String = "IN_PROGRESS",

    val crop: String? = null,

    val goal: String? = null,

    val areaHa: Double? = null,

    val note: String? = null
)
