package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "aggregates",
    foreignKeys = [
        ForeignKey(
            entity = Aggregate::class,
            parentColumns = ["id"],
            childColumns = ["parentAggregateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(
            value = ["aggregateUid"],
            unique = true
        ),
        Index(value = ["parentAggregateId"]),
        Index(value = ["category"])
    ]
)
data class Aggregate(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val aggregateUid: String,

    val name: String,

    val category: String,

    val parentAggregateId: Int? = null,

    val workingWidthM: Double? = null,

    val note: String = "",

    val isActive: Boolean = true
)
