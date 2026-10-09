package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "farm_event_fields",
    foreignKeys = [
        ForeignKey(
            entity = FarmEvent::class,
            parentColumns = ["eventUid"],
            childColumns = ["eventUid"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(
            value = ["eventUid", "fieldId"],
            unique = true
        ),
        Index(
            value = ["fieldId"]
        )
    ]
)
data class FarmEventField(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventUid: String,
    val fieldId: Int
)
