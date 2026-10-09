package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "farm_event_aggregates",
    foreignKeys = [
        ForeignKey(
            entity = FarmEvent::class,
            parentColumns = ["eventUid"],
            childColumns = ["eventUid"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Aggregate::class,
            parentColumns = ["id"],
            childColumns = ["aggregateId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(
            value = [
                "eventUid",
                "aggregateId"
            ],
            unique = true
        ),
        Index(value = ["aggregateId"])
    ]
)
data class FarmEventAggregate(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val eventUid: String,

    val aggregateId: Int,

    val relationType: String = "RELATED",

    val note: String? = null
)
