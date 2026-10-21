package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

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
    val bunkerCount: Int,
    val harvestedAreaHa: Double?,
    val bulkDensityKgM3: Double?,
    val densityMode: String?
)

@Dao
interface HarvestDao {

    @Query("SELECT * FROM harvest_processes WHERE status = 'IN_PROGRESS' ORDER BY startDate DESC, id DESC")
    fun getActiveProcesses(): List<HarvestProcess>

    @Query("SELECT areaHa FROM fields WHERE id = :fieldId")
    fun getFieldArea(fieldId: Int): Double?

    @Transaction
    fun createProcess(process: HarvestProcess): Long {
        val area = process.fieldId?.let { getFieldArea(it) }
        return insertProcess(calculationProcess(process).copy(fieldAreaSnapshotHa = area, actualPlantedAreaHa = area))
    }

    @Query("SELECT grainTankVolumeM3 FROM combine_specs WHERE vehicleId = :vehicleId LIMIT 1")
    fun getCombineCapacity(vehicleId: Int): Double?

    /** Use the linked combine's real TTX; retain the historical snapshot as fallback when its
     * card is unavailable. Neither source is rewritten and no model-specific constant is used. */
    fun calculationProcess(process: HarvestProcess): HarvestProcess {
        val capacity = process.combineVehicleId?.let { HarvestCalculator.positive(getCombineCapacity(it)) }
            ?: HarvestCalculator.positive(process.bunkerVolumeM3)
        return process.copy(bunkerVolumeM3 = capacity)
    }

    @Query("UPDATE harvest_processes SET actualPlantedAreaHa = :area WHERE id = :id")
    fun updateActualAreaRaw(id: Int, area: Double?): Int

    fun updateActualArea(id: Int, area: Double?): Int {
        require(area == null || HarvestCalculator.positive(area) != null)
        return updateActualAreaRaw(id, area)
    }

    @Insert
    fun insertProcess(
        process: HarvestProcess
    ): Long

    @Insert
    fun insertDayRaw(day: HarvestDay): Long

    @Transaction
    fun insertDay(day: HarvestDay): Long {
        val process = requireNotNull(getProcessById(day.processId))
        require(HarvestFuelCalculator.valid(day.moisturePercent))
        HarvestChronology.minutes(day.startTime)
        require(day.workDate >= process.startDate) { "Дата дня раньше начала кампании" }
        require(day.status in setOf("ACTIVE", "COMPLETED"))
        if (day.status == "ACTIVE") {
            require(process.status == "IN_PROGRESS") { "Кампания завершена; сначала откройте её" }
            require(getDaysForProcess(day.processId).none { it.status == "ACTIVE" }) { "В кампании уже есть активный день" }
            require(day.endTime == null)
        } else {
            HarvestChronology.validateEnd(day.startTime, requireNotNull(day.endTime), emptyList())
        }
        val (mode, density) = HarvestDensityCatalog.initial(process.crop)
        return insertDayRaw(day.copy(densityMode = mode, bulkDensityKgM3 = density))
    }

    @Insert
    fun insertBunkerRaw(bunker: HarvestBunker): Long

    @Transaction
    fun insertBunker(bunker: HarvestBunker): Long {
        val day = requireNotNull(getDayById(bunker.dayId))
        require(day.status == "ACTIVE" && getProcessById(day.processId)?.status == "IN_PROGRESS") { "Рабочий день завершён" }
        HarvestChronology.validateBunker(day.startTime, day.endTime, bunker.fillTime)
        return insertBunkerRaw(bunker)
    }

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
            hd.harvestedAreaHa AS harvestedAreaHa,
            hd.bulkDensityKgM3 AS bulkDensityKgM3,
            hd.densityMode AS densityMode,
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
        SELECT hb.* FROM harvest_bunkers hb JOIN harvest_days hd ON hd.id = hb.dayId
        WHERE hb.dayId = :dayId
        ORDER BY ((CAST(substr(hb.fillTime,1,2) AS INTEGER)*60 + CAST(substr(hb.fillTime,4,2) AS INTEGER)
            - CAST(substr(hd.startTime,1,2) AS INTEGER)*60 - CAST(substr(hd.startTime,4,2) AS INTEGER) + 1440) % 1440), hb.id
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
    fun updateBunkerRaw(bunkerId: Int, fillTime: String, fillLevel: String)

    @Query("SELECT * FROM harvest_bunkers WHERE id = :bunkerId LIMIT 1")
    fun getBunkerById(bunkerId: Int): HarvestBunker?

    @Transaction
    fun updateBunker(bunkerId: Int, fillTime: String, fillLevel: String) {
        val bunker = requireNotNull(getBunkerById(bunkerId))
        val day = requireNotNull(getDayById(bunker.dayId))
        HarvestChronology.validateBunker(day.startTime, day.endTime, fillTime)
        updateBunkerRaw(bunkerId, fillTime, fillLevel)
    }

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
    fun updateDayTimesRaw(dayId: Int, startTime: String, endTime: String?)

    @Transaction
    fun updateDayTimes(dayId: Int, startTime: String, endTime: String?) {
        val day = requireNotNull(getDayById(dayId))
        updateDayDetails(dayId, day.workDate, startTime, endTime, day.status)
    }

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
    fun updateDayDetailsRaw(dayId: Int, workDate: String, startTime: String, endTime: String?, status: String)

    @Transaction
    fun updateDayDetails(dayId: Int, workDate: String, startTime: String, endTime: String?, status: String) {
        val day = requireNotNull(getDayById(dayId))
        val process = requireNotNull(getProcessById(day.processId))
        require(workDate >= process.startDate) { "Дата дня раньше начала кампании" }
        val otherDays = getDaysForProcess(day.processId).filter { it.id != dayId }
        require(otherDays.none { it.workDate == workDate }) { "На эту дату рабочий день уже существует" }
        val times = getBunkersForDay(dayId).map { it.fillTime }
        HarvestChronology.validateStartChange(day.startTime, startTime, times)
        require(status in setOf("ACTIVE", "COMPLETED"))
        if (status == "ACTIVE") {
            require(endTime == null)
            require(otherDays.none { it.status == "ACTIVE" }) { "В кампании уже есть активный день" }
        } else HarvestChronology.validateEnd(startTime, requireNotNull(endTime), times)
        updateDayDetailsRaw(dayId, workDate, startTime, endTime, status)
        if (status == "ACTIVE") reopenProcess(day.processId) else refreshCompletedProcessEndDate(day.processId)
    }

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

    @Query("UPDATE harvest_days SET reminderStepMinutes=:step WHERE id=:id")
    fun updateReminderStepRaw(id: Int, step: Int): Int

    @Transaction
    fun saveSliderSettings(id: Int, values: List<Int>, step: Int): HarvestDay {
        require(step == 1 || step == 5)
        require(values.size == 5)
        val old = requireNotNull(getDayById(id))
        val before = listOf(old.workIntervalMinutes, old.unloadIntervalMinutes, old.lunchIntervalMinutes,
            old.dinnerIntervalMinutes, old.waitingTransportIntervalMinutes)
        val flags = listOf(old.workIntervalCustom, old.unloadIntervalCustom, old.lunchIntervalCustom,
            old.dinnerIntervalCustom, old.waitingTransportIntervalCustom)
        val custom = values.indices.map { n ->
            require(values[n] == before[n] || values[n] in 1..HarvestSliderRules.maxima[n])
            if (values[n] == before[n]) flags[n] else values[n] !in HarvestSliderRules.presets[n]
        }
        updateDayReminderSettings(id, values[0], values[1], values[2], values[3], values[4],
            custom[0], custom[1], custom[2], custom[3], custom[4], true)
        check(updateReminderStepRaw(id, step) == 1)
        return requireNotNull(getDayById(id))
    }

    @Query(
        """
        UPDATE harvest_days
        SET endTime = :endTime,
            status = 'COMPLETED',
            harvestedAreaHa = :harvestedAreaHa
        WHERE id = :dayId AND status = 'ACTIVE'
        """
    )
    fun completeDayRaw(dayId: Int, endTime: String, harvestedAreaHa: Double?): Int

    @Transaction
    fun completeDay(dayId: Int, endTime: String): Int =
        completeDay(dayId, endTime, requireNotNull(getDayById(dayId)).harvestedAreaHa)

    @Transaction
    fun completeDay(dayId: Int, endTime: String, harvestedAreaHa: Double?): Int {
        require(harvestedAreaHa == null || HarvestCalculator.positive(harvestedAreaHa) != null)
        val day = requireNotNull(getDayById(dayId))
        HarvestChronology.validateEnd(day.startTime, endTime, getBunkersForDay(dayId).map { it.fillTime })
        return completeDayRaw(dayId, endTime, harvestedAreaHa)
    }

    @Query("UPDATE harvest_days SET moisturePercent=:value WHERE id=:id")
    fun updateMoistureRaw(id:Int,value:Double?):Int
    fun updateMoisture(id:Int,value:Double?):Int {
        require(HarvestFuelCalculator.valid(value)) { "Влажность: от 0 до 100%" }
        return updateMoistureRaw(id,value)
    }
    @Transaction fun completeDay(dayId:Int,endTime:String,harvestedAreaHa:Double?,moisture:Double?):Int {
        require(HarvestFuelCalculator.valid(moisture)) { "Влажность: от 0 до 100%" }
        val count=completeDay(dayId,endTime,harvestedAreaHa)
        if(count==1) updateMoistureRaw(dayId,moisture)
        return count
    }

    @Query("UPDATE harvest_days SET harvestedAreaHa = :harvestedAreaHa WHERE id = :dayId AND status = 'COMPLETED'")
    fun updateHarvestedAreaRaw(dayId: Int, harvestedAreaHa: Double?): Int

    fun updateHarvestedArea(dayId: Int, harvestedAreaHa: Double?): Int {
        require(harvestedAreaHa == null || HarvestCalculator.positive(harvestedAreaHa) != null)
        return updateHarvestedAreaRaw(dayId, harvestedAreaHa)
    }

    @Query("UPDATE harvest_days SET bulkDensityKgM3 = :densityKgM3, densityMode = :mode WHERE id = :dayId")
    fun updateDayDensityRaw(dayId: Int, mode: String?, densityKgM3: Double?): Int

    @Transaction
    fun updateDayDensity(dayId: Int, mode: String, custom: Double? = null): Int {
        val day = requireNotNull(getDayById(dayId))
        val process = requireNotNull(getProcessById(day.processId))
        require(mode in setOf("MIN", "AVERAGE", "MAX", "CUSTOM"))
        val density = HarvestDensityCatalog.selected(process.crop, mode, custom)
        require(mode != "CUSTOM" || density != null)
        return updateDayDensityRaw(dayId, if (density == null) null else mode, density)
    }

    @Query(
        """
        UPDATE harvest_processes
        SET endDate = :endDate,
            status = 'COMPLETED'
        WHERE id = :processId
        """
    )
    fun completeProcessRaw(processId: Int, endDate: String)

    @Transaction
    fun completeProcess(processId: Int, endDate: String) {
        val days = getDaysForProcess(processId)
        require(days.none { it.status == "ACTIVE" }) { "Сначала завершите активный рабочий день" }
        // Reopening an earlier day must not shorten the existing campaign history.
        completeProcessRaw(processId, maxOf(endDate, days.maxOfOrNull { it.workDate } ?: endDate))
    }


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
