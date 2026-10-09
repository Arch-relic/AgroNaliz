package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkTypeDao {

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insert(
        workType: WorkType
    ): Long

    @Update
    suspend fun update(
        workType: WorkType
    )

    @Query(
        """
        SELECT * FROM work_types
        WHERE isActive = 1
        ORDER BY name ASC
        """
    )
    suspend fun getActiveWorkTypes():
        List<WorkType>

    @Query(
        """
        SELECT * FROM work_types
        WHERE id = :workTypeId
        LIMIT 1
        """
    )
    suspend fun getById(
        workTypeId: Int
    ): WorkType?

    @Query(
        """
        SELECT * FROM work_types
        WHERE name = :name COLLATE NOCASE
        LIMIT 1
        """
    )
    suspend fun getByName(
        name: String
    ): WorkType?
}
