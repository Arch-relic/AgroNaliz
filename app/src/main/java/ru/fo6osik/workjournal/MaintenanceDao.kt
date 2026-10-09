package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MaintenanceDao {

    @Insert
    suspend fun insert(record: MaintenanceRecord)

    @Update
    suspend fun update(record: MaintenanceRecord)

    @Delete
    suspend fun delete(record: MaintenanceRecord)

    // Все записи ТО
    @Query(
        "SELECT * FROM maintenance_records " +
        "ORDER BY date DESC, id DESC"
    )
    suspend fun getAll(): List<MaintenanceRecord>

    // История ТО определённой техники
    @Query(
        "SELECT * FROM maintenance_records " +
        "WHERE vehicleId = :vehicleId " +
        "ORDER BY date DESC, id DESC"
    )
    suspend fun getByVehicle(
        vehicleId: Int
    ): List<MaintenanceRecord>

    // Последнее ТО конкретной техники
    @Query(
        "SELECT * FROM maintenance_records " +
        "WHERE vehicleId = :vehicleId " +
        "ORDER BY date DESC, id DESC " +
        "LIMIT 1"
    )
    suspend fun getLastByVehicle(
        vehicleId: Int
    ): MaintenanceRecord?

    // Количество ТО за выбранный период
    @Query(
        "SELECT COUNT(*) FROM maintenance_records " +
        "WHERE vehicleId = :vehicleId " +
        "AND date >= :startDate " +
        "AND date < :endDate"
    )
    suspend fun countByVehicleForPeriod(
        vehicleId: Int,
        startDate: Long,
        endDate: Long
    ): Int
	
	@Query("SELECT * FROM maintenance_records WHERE id = :id LIMIT 1")
suspend fun getById(id: Int): MaintenanceRecord?

    @Query(
        """
        SELECT *
        FROM maintenance_records
        WHERE eventUid = :eventUid
        LIMIT 1
        """
    )
    suspend fun getByEventUid(
        eventUid: String
    ): MaintenanceRecord?
}
