package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CombineSpecDao {

    @Query(
        """
        SELECT *
        FROM combine_specs
        WHERE vehicleId = :vehicleId
        LIMIT 1
        """
    )
    suspend fun getByVehicleId(
        vehicleId: Int
    ): CombineSpec?

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsert(
        spec: CombineSpec
    )
}
