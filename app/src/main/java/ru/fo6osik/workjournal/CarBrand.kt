package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "car_brands",
    indices = [
        Index(
            value = ["name"],
            unique = true
        )
    ]
)
data class CarBrand(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    // false = базовый каталог
    // true = добавлено пользователем
    val userCreated: Boolean = false
)