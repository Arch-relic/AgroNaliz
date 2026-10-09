package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "harvest_bunkers",
    foreignKeys = [
        ForeignKey(
            entity = HarvestDay::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bunkerUid"], unique = true),
        Index(value = ["dayId"]),
        Index(value = ["fillTime"])
    ]
)
data class HarvestBunker(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val bunkerUid: String,

    val dayId: Int,

    // HH:mm
    val fillTime: String,

    // FULL / MORE_THAN_HALF / HALF / LESS_THAN_HALF
    val fillLevel: String = "FULL",

    // Состояние промежутка после этого бункера:
    // CONTINUE / MEAL / WAITING_TRANSPORT.
    // null = событие ещё не указано (важно для старых записей).
    val postEventType: String? = null,

    val createdAt: Long = System.currentTimeMillis()
)
