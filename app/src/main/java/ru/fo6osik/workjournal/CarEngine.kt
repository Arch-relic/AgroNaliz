package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "car_engines",
    indices = [
        Index(value = ["code"])
    ]
)
data class CarEngine(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Например: Renault, ВАЗ
    val manufacturer: String,

    // Например: H4M, ВАЗ-21129
    val code: String,

    // Рабочий объём в см³
    val displacementCc: Int? = null,

    // Мощность в л.с.
    val powerHp: Int? = null,

    val cylinders: Int? = null,

    val valves: Int? = null,

    // Бензин, дизель и т.д.
    val fuelType: String = "",

    val userCreated: Boolean = false
)