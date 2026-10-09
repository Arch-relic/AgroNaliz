package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "tractor_model_engines",
    primaryKeys = [
        "modelId",
        "engineId"
    ],
    foreignKeys = [
        ForeignKey(
            entity = TractorModel::class,
            parentColumns = ["id"],
            childColumns = ["modelId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CarEngine::class,
            parentColumns = ["id"],
            childColumns = ["engineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["modelId"]),
        Index(value = ["engineId"])
    ]
)
data class TractorModelEngine(
    val modelId: Int,
    val engineId: Int
)