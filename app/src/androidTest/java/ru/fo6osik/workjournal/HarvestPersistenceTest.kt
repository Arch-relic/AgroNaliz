package ru.fo6osik.workjournal

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HarvestPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun database(): AppDatabase {
        check(context.packageName == "ru.fo6osik.workjournal.debugtest")
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }
    private fun withDatabase(block: (AppDatabase) -> Unit) {
        val db = database()
        try { block(db) } finally { db.close() }
    }
    private fun process(dao: HarvestDao): Int = dao.insertProcess(HarvestProcess(
        processUid = UUID.randomUUID().toString(), startDate = "2026-10-11", fieldId = null,
        fieldNameSnapshot = "synthetic", crop = "Пшеница", combineVehicleId = null,
        combineNameSnapshot = "synthetic", bunkerVolumeM3 = 8.0
    )).toInt()
    private fun day(dao: HarvestDao, process: Int): Int = dao.insertDay(HarvestDay(
        dayUid = UUID.randomUUID().toString(), processId = process, workDate = "2026-10-11",
        startTime = "08:00", workIntervalMinutes = 47, workIntervalCustom = true
    )).toInt()

    @Test fun completeWithoutAreaPreservesBunkersEventsAndTimer() {
        withDatabase { db ->
            val dao = db.harvestDao()
            val process = process(dao)
            val day = day(dao, process)
            dao.insertBunker(HarvestBunker(bunkerUid = "synthetic-bunker", dayId = day,
                fillTime = "09:00", fillLevel = "MORE_THAN_HALF", postEventType = "WAITING_TRANSPORT"))
            val before = dao.getDayById(day)!!
            dao.completeDay(day, "18:00", null)
            val after = dao.getDayById(day)!!
            assertEquals(before.copy(status = "COMPLETED", endTime = "18:00"), after)
            assertEquals("WAITING_TRANSPORT", dao.getBunkersForDay(day).single().postEventType)
            assertEquals("IN_PROGRESS", dao.getProcessById(process)!!.status)
        }
    }

    @Test fun completeWithFractionalAreaIsAtomicWithCampaignCompletion() {
        withDatabase { db ->
            val dao = db.harvestDao()
            val process = process(dao)
            val day = day(dao, process)
            db.runInTransaction {
                dao.completeDay(day, "18:00", HarvestCalculator.parseOptionalPositive("12,375"))
                dao.completeProcess(process, "2026-10-11")
            }
            assertEquals(12.375, dao.getDayById(day)!!.harvestedAreaHa!!, 0.0)
            assertEquals("COMPLETED", dao.getProcessById(process)!!.status)
            assertEquals(12.375, dao.getDaySummariesForProcess(process).single().harvestedAreaHa!!, 0.0)
        }
    }

    @Test fun areaCanBeAddedChangedAndClearedWithoutReopeningCompletedCampaign() {
        withDatabase { db ->
            val dao = db.harvestDao()
            val process = process(dao)
            val day = day(dao, process)
            dao.completeDay(day, "18:00")
            dao.completeProcess(process, "2026-10-11")
            val beforeDay = dao.getDayById(day)!!
            val beforeProcess = dao.getProcessById(process)!!
            for (area in listOf(3.125, 4.875, null)) {
                assertEquals(1, dao.updateHarvestedArea(day, area))
                assertEquals(beforeDay.copy(harvestedAreaHa = area), dao.getDayById(day))
                assertEquals(beforeProcess, dao.getProcessById(process))
            }
        }
    }

    @Test fun areaEditorCannotModifyActiveDayAndTransactionRollbackPreservesData() {
        withDatabase { db ->
            val dao = db.harvestDao()
            val process = process(dao)
            val day = day(dao, process)
            assertEquals(0, dao.updateHarvestedArea(day, 2.0))
            val before = dao.getDayById(day)
            try {
                db.runInTransaction {
                    dao.completeDay(day, "18:00", 5.0)
                    throw IllegalStateException("synthetic rollback")
                }
            } catch (_: IllegalStateException) { }
            assertEquals(before, dao.getDayById(day))
        }
    }

    @Test fun independentDensityPersistsAfterCompletionAndCropChange() {
        withDatabase { db ->
            val dao = db.harvestDao(); val process = process(dao); val first = day(dao, process)
            assertEquals("MIN", dao.getDayById(first)!!.densityMode)
            assertEquals(720.0, dao.getDayById(first)!!.bulkDensityKgM3!!, 0.0)
            dao.updateDayDensity(first, "CUSTOM", 725.5)
            val second = dao.insertDay(HarvestDay(dayUid = "second", processId = process, workDate = "2026-10-12", startTime = "08:00")).toInt()
            assertEquals(720.0, dao.getDayById(second)!!.bulkDensityKgM3!!, 0.0)
            dao.completeDay(first, "18:00"); dao.completeDay(second, "18:00"); dao.completeProcess(process, "2026-10-12")
            dao.updateDayDensity(first, "CUSTOM", 555.25)
            assertEquals(555.25, dao.getDayById(first)!!.bulkDensityKgM3!!, 0.0)
            assertEquals("CUSTOM", dao.getDayById(first)!!.densityMode)
            dao.updateProcessDetails(process, "2026-10-11", null, "synthetic", "Другое", null, "synthetic", 8.0)
            assertEquals(555.25, dao.getDayById(first)!!.bulkDensityKgM3!!, 0.0)
            assertEquals(720.0, dao.getDayById(second)!!.bulkDensityKgM3!!, 0.0)
            val third = dao.insertDay(HarvestDay(dayUid = "third", processId = process, workDate = "2026-10-13", startTime = "08:00")).toInt()
            assertNull(dao.getDayById(third)!!.bulkDensityKgM3)
            assertNull(dao.getDayById(third)!!.densityMode)
            assertEquals("COMPLETED", dao.getDayById(first)!!.status)
        }
    }
}
