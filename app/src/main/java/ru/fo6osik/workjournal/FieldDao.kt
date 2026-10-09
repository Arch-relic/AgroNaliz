package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FieldDao {

    @Insert
    suspend fun insert(
        field: FieldEntity
    ): Long

    @Update
    suspend fun update(
        field: FieldEntity
    )

    @Query(
        """
        SELECT * FROM fields
        WHERE isActive = 1
        ORDER BY name ASC
        """
    )
    suspend fun getActiveFields():
        List<FieldEntity>

    @Query(
        """
        SELECT * FROM fields
        ORDER BY name ASC
        """
    )
    suspend fun getAllFields():
        List<FieldEntity>

    @Query(
        """
        SELECT * FROM fields
        WHERE id = :fieldId
        LIMIT 1
        """
    )
    suspend fun getById(
        fieldId: Int
    ): FieldEntity?

    @Query(
        """
        UPDATE fields
        SET isActive = 0
        WHERE id = :fieldId
        """
    )
    suspend fun archive(
        fieldId: Int
    )

    @Query(
        """
        SELECT COUNT(*)
        FROM season_works
        WHERE fieldId = :fieldId
        """
    )
    suspend fun countLinkedSeasonWorks(
        fieldId: Int
    ): Int

    @Query(
        """
        DELETE FROM fields
        WHERE id = :fieldId
        """
    )
    suspend fun deleteById(
        fieldId: Int
    )

    @Query(
        """
        SELECT COUNT(*)
        FROM farm_event_fields
        WHERE fieldId = :fieldId
        """
    )
    suspend fun countLinkedFarmEvents(
        fieldId: Int
    ): Int
}
