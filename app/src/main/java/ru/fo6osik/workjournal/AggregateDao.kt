package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AggregateDao {

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insert(
        aggregate: Aggregate
    ): Long

    @Update
    suspend fun update(
        aggregate: Aggregate
    )

    @Query(
        """
        SELECT *
        FROM aggregates
        WHERE id = :aggregateId
        LIMIT 1
        """
    )
    suspend fun getById(
        aggregateId: Int
    ): Aggregate?

    @Query(
        """
        SELECT *
        FROM aggregates
        WHERE aggregateUid = :aggregateUid
        LIMIT 1
        """
    )
    suspend fun getByUid(
        aggregateUid: String
    ): Aggregate?

    @Query(
        """
        SELECT *
        FROM aggregates
        WHERE parentAggregateId IS NULL
        ORDER BY name COLLATE NOCASE ASC, id ASC
        """
    )
    suspend fun getRootAggregates():
        List<Aggregate>

    @Query(
        """
        SELECT *
        FROM aggregates
        WHERE parentAggregateId = :parentAggregateId
        ORDER BY name COLLATE NOCASE ASC, id ASC
        """
    )
    suspend fun getChildren(
        parentAggregateId: Int
    ): List<Aggregate>

    @Query(
        """
        SELECT *
        FROM aggregates
        ORDER BY name COLLATE NOCASE ASC, id ASC
        """
    )
    suspend fun getAll():
        List<Aggregate>
}
