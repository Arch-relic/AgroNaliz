package ru.fo6osik.workjournal

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HarvestLifecycleTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun fixture(db: AppDatabase, start: String = "10:47"): Pair<Int,Int> {
        check(context.packageName == "ru.fo6osik.workjournal.debugtest")
        val dao = db.harvestDao()
        val process = dao.insertProcess(HarvestProcess(processUid = UUID.randomUUID().toString(), startDate = "2026-10-10",
            fieldId = null, fieldNameSnapshot = "synthetic", crop = "Пшеница", combineVehicleId = null,
            combineNameSnapshot = "synthetic", bunkerVolumeM3 = 8.0)).toInt()
        val day = dao.insertDay(HarvestDay(dayUid = UUID.randomUUID().toString(), processId = process,
            workDate = "2026-10-10", startTime = start)).toInt()
        return process to day
    }
    private fun rejected(block: () -> Unit) { try { block(); fail("Expected rejection") } catch (_: IllegalArgumentException) {} }
    private fun memory(block: (AppDatabase) -> Unit) {
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try { block(db) } finally { db.close() }
    }
    @Test fun reportedConflictCannotSaveCompletionOrEditedTimes() = memory { db ->
        val (_,day)=fixture(db);val dao=db.harvestDao()
        dao.insertBunker(HarvestBunker(bunkerUid="one",dayId=day,fillTime="10:47"))
        dao.insertBunker(HarvestBunker(bunkerUid="two",dayId=day,fillTime="11:47"))
        val before=dao.getDayById(day)
        rejected { dao.completeDay(day,"10:48",2.0) }
        rejected { dao.updateDayTimes(day,"12:00",null) }
        rejected { dao.updateDayDetails(day,"2026-10-10","10:47","10:48","COMPLETED") }
        assertEquals(before,dao.getDayById(day));assertEquals(2,dao.getBunkersForDay(day).size)
    }
    @Test fun overnightBunkerOrderAndEndValidation() = memory { db ->
        val (_,day)=fixture(db,"23:00");val dao=db.harvestDao()
        val first=dao.insertBunker(HarvestBunker(bunkerUid="night1",dayId=day,fillTime="23:30")).toInt()
        val second=dao.insertBunker(HarvestBunker(bunkerUid="night2",dayId=day,fillTime="00:30")).toInt()
        assertEquals(listOf(first,second),dao.getBunkersForDay(day).map { it.id })
        rejected { dao.completeDay(day,"00:20") }
        dao.completeDay(day,"01:00")
        val before=dao.getBunkerById(second)
        rejected { dao.updateBunker(second,"01:30","HALF") }
        rejected { dao.insertBunker(HarvestBunker(bunkerUid="late",dayId=day,fillTime="00:40")) }
        assertEquals(before,dao.getBunkerById(second))
        dao.updateBunker(second,"00:45","HALF")
        assertEquals("00:45",dao.getBunkerById(second)!!.fillTime)
    }
    @Test fun noSecondActiveDayOrPrematureCampaignCompletion() = memory { db ->
        val (process,day)=fixture(db);val dao=db.harvestDao()
        rejected { dao.insertDay(HarvestDay(dayUid="duplicate",processId=process,workDate="2026-10-11",startTime="08:00")) }
        rejected { dao.completeProcess(process,"2026-10-10") }
        assertEquals(1,dao.getDaysForProcess(process).size)
        dao.completeDay(day,"18:00");dao.completeProcess(process,"2026-10-10")
        rejected { dao.insertDay(HarvestDay(dayUid="closed",processId=process,workDate="2026-10-11",startTime="08:00")) }
    }
    @Test fun reopeningSurvivesDatabaseReopenAndRetainsAllDayData() {
        check(context.packageName == "ru.fo6osik.workjournal.debugtest")
        val name="synthetic-lifecycle-${UUID.randomUUID()}.db"
        fun open()=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
        var db=open()
        try {
            val (process,day)=fixture(db);var dao=db.harvestDao()
            dao.updateDayDensity(day,"CUSTOM",555.125)
            dao.insertBunker(HarvestBunker(bunkerUid="kept",dayId=day,fillTime="11:47",postEventType="MEAL"))
            dao.completeDay(day,"18:00",3.125);dao.completeProcess(process,"2026-10-10")
            val before=dao.getDayById(day)!!
            dao.updateDayDetails(day,before.workDate,before.startTime,null,"ACTIVE")
            assertEquals(before.copy(endTime=null,status="ACTIVE"),dao.getDayById(day))
            assertEquals("IN_PROGRESS",dao.getProcessById(process)!!.status)
            db.close();db=open();dao=db.harvestDao()
            assertEquals("ACTIVE",dao.getDayById(day)!!.status)
            assertEquals("IN_PROGRESS",dao.getProcessById(process)!!.status)
            assertEquals("MEAL",dao.getBunkersForDay(day).single().postEventType)
            dao.completeDay(day,"19:00");dao.completeProcess(process,"2026-10-10")
            assertEquals(3.125,dao.getDayById(day)!!.harvestedAreaHa!!,0.0)
            assertEquals(555.125,dao.getDayById(day)!!.bulkDensityKgM3!!,0.0)
            dao.reopenProcess(process)
            dao.insertDay(HarvestDay(dayUid="next",processId=process,workDate="2026-10-11",startTime="08:00"))
            rejected { dao.updateDayDetails(day,before.workDate,before.startTime,null,"ACTIVE") }
        } finally { db.close();context.deleteDatabase(name) }
    }
    @Test fun reCompletingEarlierDayKeepsLatestCampaignDateAndLaterDay() = memory { db ->
        val(process,first)=fixture(db);val dao=db.harvestDao()
        dao.completeDay(first,"18:00",2.0)
        val second=dao.insertDay(HarvestDay(dayUid="later",processId=process,workDate="2026-10-11",startTime="08:00")).toInt()
        dao.completeDay(second,"18:00",3.0);dao.completeProcess(process,"2026-10-11")
        val later=dao.getDayById(second)
        val firstDay=dao.getDayById(first)!!
        dao.updateDayDetails(first,firstDay.workDate,firstDay.startTime,null,"ACTIVE")
        dao.completeDay(first,"19:00");dao.completeProcess(process,"2026-10-10")
        assertEquals("2026-10-11",dao.getProcessById(process)!!.endDate)
        assertEquals(later,dao.getDayById(second))
        assertEquals(2.0,dao.getDayById(first)!!.harvestedAreaHa!!,0.0)
    }

}
