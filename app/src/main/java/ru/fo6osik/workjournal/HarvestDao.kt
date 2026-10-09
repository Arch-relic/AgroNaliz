package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

data class HarvestProcessSummary(
    val id: Int,
    val processUid: String,
    val startDate: String,
    val endDate: String?,
    val status: String,
    val crop: String,
    val fieldName: String,
    val combineName: String,
    val bunkerVolumeM3: Double?,
    val dayCount: Int,
    val bunkerCount: Int
)

data class HarvestDaySummary(
    val id: Int,
    val processId: Int,
    val workDate: String,
    val startTime: String,
    val endTime: String?,
    val status: String,
    val bunkerCount: Int
)

@Dao
interface HarvestDao {

    @Insert
    fun insertProcess(
        process: HarvestProcess
    ): Long

    @Insert
    fun insertDay(
        day: HarvestDay
    ): Long

    @Insert
    fun insertBunker(
        bunker: HarvestBunker
    ): Long

    @Query(
        """
        SELECT
            hp.id AS id,
            hp.processUid AS processUid,
            hp.startDate AS startDate,
            hp.endDate AS endDate,
            hp.status AS status,
            hp.crop AS crop,

            COALESCE(
                f.name,
                hp.fieldNameSnapshot
            ) AS fieldName,

            CASE
                WHEN v.id IS NULL
                    THEN hp.combineNameSnapshot
                ELSE
                    TRIM(
                        v.brand || ' ' || v.model ||
                        CASE
                            WHEN TRIM(v.number) <> ''
                                THEN ' • ' || v.number
                            ELSE ''
                        END
                    )
            END AS combineName,

            hp.bunkerVolumeM3 AS bunkerVolumeM3,

            (
                SELECT COUNT(*)
                FROM harvest_days hd
                WHERE hd.processId = hp.id
            ) AS dayCount,

            (
                SELECT COUNT(*)
                FROM harvest_bunkers hb
                INNER JOIN harvest_days hd2
                    ON hd2.id = hb.dayId
                WHERE hd2.processId = hp.id
            ) AS bunkerCount

        FROM harvest_processes hp

        LEFT JOIN fields f
            ON f.id = hp.fieldId

        LEFT JOIN vehicles v
            ON v.id = hp.combineVehicleId

        ORDER BY
            CASE
                WHEN hp.status = 'IN_PROGRESS' THEN 0
                ELSE 1
            END,
            hp.startDate DESC,
            hp.id DESC
        """
    )
    fun getAllProcessSummaries():
        List<HarvestProcessSummary>

    @Query(
        """
        SELECT *
        FROM harvest_processes
        WHERE id = :processId
        LIMIT 1
        """
    )
    fun getProcessById(
        processId: Int
    ): HarvestProcess?

    @Query(
        """
        SELECT *
        FROM harvest_days
        WHERE id = :dayId
        LIMIT 1
        """
    )
    fun getDayById(
        dayId: Int
    ): HarvestDay?

    @Query(
        """
        SELECT *
        FROM harvest_days
        WHERE processId = :processId
        ORDER BY workDate ASC, id ASC
        """
    )
    fun getDaysForProcess(
        processId: Int
    ): List<HarvestDay>

    @Query(
        """
        SELECT
            hd.id AS id,
            hd.processId AS processId,
            hd.workDate AS workDate,
            hd.startTime AS startTime,
            hd.endTime AS endTime,
            hd.status AS status,
            (
                SELECT COUNT(*)
                FROM harvest_bunkers hb
                WHERE hb.dayId = hd.id
            ) AS bunkerCount
        FROM harvest_days hd
        WHERE hd.processId = :processId
        ORDER BY hd.workDate ASC, hd.id ASC
        """
    )
    fun getDaySummariesForProcess(
        processId: Int
    ): List<HarvestDaySummary>

    @Query(
        """
        SELECT *
        FROM harvest_bunkers
        WHERE dayId = :dayId
        ORDER BY fillTime ASC, id ASC
        """
    )
    fun getBunkersForDay(
        dayId: Int
    ): List<HarvestBunker>

    @Query(
        """
        UPDATE harvest_bunkers
        SET fillTime = :fillTime,
            fillLevel = :fillLevel
        WHERE id = :bunkerId
        """
    )
    fun updateBunker(
        bunkerId: Int,
        fillTime: String,
        fillLevel: String
    )

    @Query(
        """
        UPDATE harvest_bunkers
        SET postEventType = :postEventType
        WHERE id = :bunkerId
        """
    )
    fun updateBunkerPostEventType(
        bunkerId: Int,
        postEventType: String?
    )

    @Query(
        """
        DELETE FROM harvest_bunkers
        WHERE id = :bunkerId
        """
    )
    fun deleteBunker(
        bunkerId: Int
    )

    @Query(
        """
        UPDATE harvest_days
        SET startTime = :startTime,
            endTime = :endTime
        WHERE id = :dayId
        """
    )
    fun updateDayTimes(
        dayId: Int,
        startTime: String,
        endTime: String?
    )

    @Query(
        """
        UPDATE harvest_days
        SET workDate = :workDate,
            startTime = :startTime,
            endTime = :endTime,
            status = :status
        WHERE id = :dayId
        """
    )
    fun updateDayDetails(
        dayId: Int,
        workDate: String,
        startTime: String,
        endTime: String?,
        status: String
    )

    @Query(
        """
        UPDATE harvest_days
        SET workIntervalMinutes = :workIntervalMinutes,
            unloadIntervalMinutes = :unloadIntervalMinutes,
            lunchIntervalMinutes = :lunchIntervalMinutes,
            dinnerIntervalMinutes = :dinnerIntervalMinutes,
            waitingTransportIntervalMinutes = :waitingTransportIntervalMinutes,
            workIntervalCustom = :workIntervalCustom,
            unloadIntervalCustom = :unloadIntervalCustom,
            lunchIntervalCustom = :lunchIntervalCustom,
            dinnerIntervalCustom = :dinnerIntervalCustom,
            waitingTransportIntervalCustom = :waitingTransportIntervalCustom,
            reminderSettingsInitialized = :reminderSettingsInitialized
        WHERE id = :dayId
        """
    )
    fun updateDayReminderSettings(
        dayId: Int,
        workIntervalMinutes: Int,
        unloadIntervalMinutes: Int,
        lunchIntervalMinutes: Int,
        dinnerIntervalMinutes: Int,
        waitingTransportIntervalMinutes: Int,
        workIntervalCustom: Boolean,
        unloadIntervalCustom: Boolean,
        lunchIntervalCustom: Boolean,
        dinnerIntervalCustom: Boolean,
        waitingTransportIntervalCustom: Boolean,
        reminderSettingsInitialized: Boolean = true
    )

    @Query(
        """
        UPDATE harvest_days
        SET endTime = :endTime,
            status = 'COMPLETED'
        WHERE id = :dayId
        """
    )
    fun completeDay(
        dayId: Int,
        endTime: String
    )

    @Query(
        """
        UPDATE harvest_processes
        SET endDate = :endDate,
            status = 'COMPLETED'
        WHERE id = :processId
        """
    )
    fun completeProcess(
        processId: Int,
        endDate: String
    )


    @Query(
        """
        UPDATE harvest_processes
        SET status = 'IN_PROGRESS',
            endDate = NULL
        WHERE id = :processId
        """
    )
    fun reopenProcess(
        processId: Int
    )

    @Query(
        """
        UPDATE harvest_processes
        SET startDate = :startDate,
            fieldId = :fieldId,
            fieldNameSnapshot = :fieldNameSnapshot,
            crop = :crop,
            combineVehicleId = :combineVehicleId,
            combineNameSnapshot = :combineNameSnapshot,
            bunkerVolumeM3 = :bunkerVolumeM3
        WHERE id = :processId
        """
    )
    fun updateProcessDetails(
        processId: Int,
        startDate: String,
        fieldId: Int?,
        fieldNameSnapshot: String,
        crop: String,
        combineVehicleId: Int?,
        combineNameSnapshot: String,
        bunkerVolumeM3: Double?
    )

    @Query(
        """
        UPDATE harvest_processes
        SET bunkerVolumeM3 = :bunkerVolumeM3
        WHERE combineVehicleId = :vehicleId
          AND bunkerVolumeM3 IS NULL
        """
    )
    suspend fun fillMissingBunkerVolumeForCombine(
        vehicleId: Int,
        bunkerVolumeM3: Double
    )

    @Query(
        """
        UPDATE harvest_processes
        SET bunkerVolumeM3 = :newVolumeM3
        WHERE combineVehicleId = :vehicleId
          AND ABS(bunkerVolumeM3 - :oldVolumeM3) < 0.0001
        """
    )
    suspend fun replaceBunkerVolumeForCombineIfEquals(
        vehicleId: Int,
        oldVolumeM3: Double,
        newVolumeM3: Double
    )

    @Query(
        """
        UPDATE harvest_processes
        SET endDate = (
            SELECT MAX(workDate)
            FROM harvest_days
            WHERE processId = :processId
        )
        WHERE id = :processId
          AND status = 'COMPLETED'
        """
    )
    fun refreshCompletedProcessEndDate(
        processId: Int
    )
}
