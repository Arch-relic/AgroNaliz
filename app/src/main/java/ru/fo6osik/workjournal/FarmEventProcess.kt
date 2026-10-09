package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "farm_event_processes",
    foreignKeys = [
        ForeignKey(
            entity = FarmEvent::class,
            parentColumns = ["eventUid"],
            childColumns = ["eventUid"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SeasonProcess::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = [
                "eventUid",
                "processId"
            ],
            unique = true
        ),
        Index(value = ["processId"])
    ]
)
data class FarmEventProcess(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val eventUid: String,

    val processId: Int
)
