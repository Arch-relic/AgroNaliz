package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface VehicleDao {

    @Insert
    suspend fun insert(vehicle: Vehicle)

    @Query("SELECT * FROM vehicles ORDER BY id DESC")
    suspend fun getAll(): List<Vehicle>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Vehicle?

    @Delete
    suspend fun delete(vehicle: Vehicle)
	
	@Update
suspend fun update(vehicle: Vehicle)
}