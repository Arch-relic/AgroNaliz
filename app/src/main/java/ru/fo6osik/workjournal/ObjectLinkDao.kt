package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ObjectLinkDao {

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertEventAggregate(
        relation: FarmEventAggregate
    ): Long

    @Query(
        """
        SELECT *
        FROM farm_event_aggregates
        WHERE eventUid = :eventUid
        ORDER BY id ASC
        """
    )
    suspend fun getAggregatesForEvent(
        eventUid: String
    ): List<FarmEventAggregate>

    @Query(
        """
        SELECT *
        FROM farm_event_aggregates
        WHERE aggregateId = :aggregateId
        ORDER BY id DESC
        """
    )
    suspend fun getEventsForAggregate(
        aggregateId: Int
    ): List<FarmEventAggregate>

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertEventVehicle(
        relation: FarmEventVehicle
    ): Long

    @Query(
        """
        SELECT *
        FROM farm_event_vehicles
        WHERE eventUid = :eventUid
        ORDER BY id ASC
        """
    )
    suspend fun getVehiclesForEvent(
        eventUid: String
    ): List<FarmEventVehicle>

    @Query(
        """
        SELECT *
        FROM farm_event_vehicles
        WHERE vehicleId = :vehicleId
        ORDER BY id DESC
        """
    )
    suspend fun getEventsForVehicle(
        vehicleId: Int
    ): List<FarmEventVehicle>

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertEventProcess(
        relation: FarmEventProcess
    ): Long

    @Query(
        """
        SELECT *
        FROM farm_event_processes
        WHERE processId = :processId
        ORDER BY id ASC
        """
    )
    suspend fun getEventsForProcess(
        processId: Int
    ): List<FarmEventProcess>

    @Query(
        """
        SELECT *
        FROM farm_event_processes
        WHERE eventUid = :eventUid
        ORDER BY id ASC
        """
    )
    suspend fun getProcessesForEvent(
        eventUid: String
    ): List<FarmEventProcess>

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertSeasonWorkAggregate(
        relation: SeasonWorkAggregate
    ): Long

    @Query(
        """
        SELECT *
        FROM season_work_aggregates
        WHERE seasonWorkId = :seasonWorkId
        ORDER BY id ASC
        """
    )
    suspend fun getAggregatesForSeasonWork(
        seasonWorkId: Int
    ): List<SeasonWorkAggregate>

    @Query(
        """
        SELECT *
        FROM season_work_aggregates
        WHERE aggregateId = :aggregateId
        ORDER BY id DESC
        """
    )
    suspend fun getSeasonWorksForAggregate(
        aggregateId: Int
    ): List<SeasonWorkAggregate>
}
