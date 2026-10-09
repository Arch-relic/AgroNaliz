package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FarmEventDao {

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insert(
        event: FarmEvent
    ): Long

    @Query(
        """
        SELECT *
        FROM farm_events
        ORDER BY
            eventYear DESC,
            eventMonth DESC,
            eventDay DESC,
            id DESC
        """
    )
    suspend fun getAll(): List<FarmEvent>

    @Query(
        """
        SELECT *
        FROM farm_events
        WHERE targetModule = :targetModule
        ORDER BY
            eventYear DESC,
            eventMonth DESC,
            eventDay DESC,
            id DESC
        """
    )
    suspend fun getByModule(
        targetModule: String
    ): List<FarmEvent>

    @Query(
        """
        SELECT *
        FROM farm_events
        WHERE eventUid = :eventUid
        LIMIT 1
        """
    )
    suspend fun getByUid(
        eventUid: String
    ): FarmEvent?

    @Query(
        """
        DELETE FROM farm_events
        WHERE eventUid = :eventUid
        """
    )
    suspend fun deleteByUid(
        eventUid: String
    )
}
