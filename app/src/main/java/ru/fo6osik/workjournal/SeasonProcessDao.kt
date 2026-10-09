package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SeasonProcessDao {

    @Insert
    suspend fun insert(
        process: SeasonProcess
    ): Long

    @Update
    suspend fun update(
        process: SeasonProcess
    )

    @Query(
        """
        SELECT *
        FROM season_processes
        ORDER BY startDate DESC, id DESC
        """
    )
    suspend fun getAll():
        List<SeasonProcess>

    @Query(
        """
        SELECT *
        FROM season_processes
        WHERE id = :processId
        LIMIT 1
        """
    )
    suspend fun getById(
        processId: Int
    ): SeasonProcess?

    @Query(
        """
        SELECT *
        FROM season_processes
        WHERE processUid = :processUid
        LIMIT 1
        """
    )
    suspend fun getByUid(
        processUid: String
    ): SeasonProcess?

    @Query(
        """
        DELETE FROM season_processes
        WHERE id = :processId
        """
    )
    suspend fun deleteById(
        processId: Int
    )
}
