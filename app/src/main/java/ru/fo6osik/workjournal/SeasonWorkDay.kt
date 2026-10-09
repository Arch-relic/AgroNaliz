package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "season_work_days",
    foreignKeys = [
        ForeignKey(
            entity = SeasonWork::class,
            parentColumns = ["id"],
            childColumns = ["seasonWorkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["dayUid"],
            unique = true
        ),
        Index(value = ["seasonWorkId"]),
        Index(value = ["workDate"])
    ]
)
data class SeasonWorkDay(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Стабильный UID этапа для импорта/экспорта.
    val dayUid: String,

    val seasonWorkId: Int,

    // Начало конкретного этапа/дня.
    val workDate: String,

    // Если исходная запись задана периодом, например 1–2 мая.
    val endDate: String? = null,

    // Фактически выполнено на этом этапе.
    val areaHa: Double? = null,

    // Накопительный итог после этого этапа.
    val cumulativeAreaHa: Double? = null,

    // START / PROGRESS / END / WORK.
    val stage: String = "WORK",

    val note: String? = null
)
