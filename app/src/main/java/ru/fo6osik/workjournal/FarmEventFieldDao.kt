package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FarmEventFieldDao {

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insert(
        link: FarmEventField
    ): Long

    @Query(
        """
        SELECT *
        FROM farm_event_fields
        WHERE eventUid = :eventUid
        ORDER BY id ASC
        """
    )
    suspend fun getByEventUid(
        eventUid: String
    ): List<FarmEventField>

    @Query(
        """
        SELECT *
        FROM farm_event_fields
        WHERE fieldId = :fieldId
        ORDER BY id ASC
        """
    )
    suspend fun getByFieldId(
        fieldId: Int
    ): List<FarmEventField>

    @Query(
        """
        DELETE FROM farm_event_fields
        WHERE eventUid = :eventUid
        """
    )
    suspend fun deleteByEventUid(
        eventUid: String
    )
}
