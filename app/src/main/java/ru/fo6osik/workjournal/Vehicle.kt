package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val type: String,

    val brand: String,

    val model: String,

    val number: String,

    val year: Int?,

    val note: String,
	
	val engineId: Int? = null,

val engineName: String = ""
)