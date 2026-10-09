package ru.fo6osik.workjournal

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "season_works",
    foreignKeys = [
        ForeignKey(
            entity = WorkType::class,
            parentColumns = ["id"],
            childColumns = ["workTypeId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = FieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SeasonProcess::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["workTypeId"]),
        Index(value = ["fieldId"]),
        Index(value = ["processId"]),
        Index(value = ["workDate"]),
        Index(
            value = ["eventUid"],
            unique = true
        )
    ]
)
data class SeasonWork(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val workDate: String,

    val endDate: String? = null,

    @ColumnInfo(defaultValue = "'COMPLETED'")
    val status: String = "COMPLETED",

    val workTypeId: Int,

    val fieldId: Int? = null,

    // null = самостоятельная операция
    val processId: Int? = null,

    val areaHa: Double? = null,

    val note: String? = null,

    val eventUid: String? = null
)
