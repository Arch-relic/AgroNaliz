package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "season_work_aggregates",
    foreignKeys = [
        ForeignKey(
            entity = SeasonWork::class,
            parentColumns = ["id"],
            childColumns = ["seasonWorkId"],
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
                "seasonWorkId",
                "aggregateId"
            ],
            unique = true
        ),
        Index(value = ["aggregateId"])
    ]
)
data class SeasonWorkAggregate(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val seasonWorkId: Int,

    val aggregateId: Int,

    val role: String = "PRIMARY",

    val note: String? = null
)
