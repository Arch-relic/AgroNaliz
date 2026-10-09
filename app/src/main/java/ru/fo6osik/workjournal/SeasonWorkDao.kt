package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import java.util.UUID

@Dao
interface SeasonWorkDao {

    @Insert
    suspend fun insertWorkRaw(
        work: SeasonWork
    ): Long

    @Transaction
    suspend fun insertWork(
        work: SeasonWork
    ): Long {

        val newId =
            insertWorkRaw(
                work
            )

        insertDay(
            SeasonWorkDay(
                dayUid =
                    UUID.randomUUID()
                        .toString(),
                seasonWorkId =
                    newId.toInt(),
                workDate =
                    work.workDate,
                endDate =
                    work.endDate,
                areaHa =
                    work.areaHa,
                cumulativeAreaHa =
                    work.areaHa,
                stage =
                    if (
                        work.status == "IN_PROGRESS"
                    ) {
                        "START"
                    } else {
                        "WORK"
                    },
                note =
                    work.note
            )
        )

        return newId
    }

    @Update
    suspend fun updateWork(
        work: SeasonWork
    )

    @Insert
    suspend fun insertDay(
        day: SeasonWorkDay
    ): Long

    @Query(
        """
        SELECT *
        FROM season_work_days
        WHERE seasonWorkId = :seasonWorkId
        ORDER BY workDate ASC, id ASC
        """
    )
    suspend fun getDaysForWork(
        seasonWorkId: Int
    ): List<SeasonWorkDay>

    @Insert
    suspend fun insertVehicleRelation(
        relation: SeasonWorkVehicle
    ): Long

    @Query(
        """
        DELETE FROM season_work_vehicles
        WHERE seasonWorkId = :seasonWorkId
        """
    )
    suspend fun clearVehicles(
        seasonWorkId: Int
    )

    @Query(
        """
        SELECT *
        FROM season_works
        ORDER BY workDate DESC, id DESC
        """
    )
    suspend fun getAllWorks():
        List<SeasonWork>

    @Query(
        """
        SELECT *
        FROM season_works
        WHERE processId = :processId
        ORDER BY workDate ASC, id ASC
        """
    )
    suspend fun getWorksByProcessId(
        processId: Int
    ): List<SeasonWork>

    @Query(
        """
        SELECT COUNT(*)
        FROM season_works
        WHERE processId = :processId
        """
    )
    suspend fun countWorksByProcessId(
        processId: Int
    ): Int

    @Query(
        """
        SELECT *
        FROM season_works
        WHERE eventUid = :eventUid
        LIMIT 1
        """
    )
    suspend fun getByEventUid(
        eventUid: String
    ): SeasonWork?

    @Query(
        """
        SELECT *
        FROM season_works
        WHERE id = :seasonWorkId
        LIMIT 1
        """
    )
    suspend fun getById(
        seasonWorkId: Int
    ): SeasonWork?

    @Query(
        """
        SELECT *
        FROM season_works
        WHERE fieldId = :fieldId
        ORDER BY workDate DESC, id DESC
        """
    )
    suspend fun getWorksByField(
        fieldId: Int
    ): List<SeasonWork>

    @Query(
        """
        SELECT *
        FROM season_works
        WHERE fieldId = :fieldId
          AND workDate LIKE :yearPrefix || '%'
        ORDER BY workDate DESC, id DESC
        """
    )
    suspend fun getWorksByFieldAndYear(
        fieldId: Int,
        yearPrefix: String
    ): List<SeasonWork>

    @Query(
        """
        SELECT DISTINCT CAST(
            substr(workDate, 1, 4)
            AS INTEGER
        )
        FROM season_works
        WHERE fieldId = :fieldId
        ORDER BY 1 DESC
        """
    )
    suspend fun getYearsByField(
        fieldId: Int
    ): List<Int>

    @Query(
        """
        SELECT *
        FROM season_work_vehicles
        WHERE seasonWorkId = :seasonWorkId
        ORDER BY id ASC
        """
    )
    suspend fun getVehiclesForWork(
        seasonWorkId: Int
    ): List<SeasonWorkVehicle>

    @Query(
        """
        DELETE FROM season_works
        WHERE id = :seasonWorkId
        """
    )
    suspend fun deleteWork(
        seasonWorkId: Int
    )
}
