package ru.fo6osik.workjournal

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

/** Every mutation is confined to a separate in-memory database. */
@RunWith(AndroidJUnit4::class)
class Harvest108Test {
    private lateinit var db: AppDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
    }
    @After fun close() { db.close() }
    private fun process(): Int = db.harvestDao().createProcess(HarvestProcess(
        processUid=UUID.randomUUID().toString(), startDate="2026-10-16", fieldId=null,
        fieldNameSnapshot="synthetic", crop="Другое", combineVehicleId=null, combineNameSnapshot="synthetic", bunkerVolumeM3=8.0)).toInt()
    private fun day(p: Int): Int = db.harvestDao().insertDay(HarvestDay(dayUid=UUID.randomUUID().toString(), processId=p,
        workDate="2026-10-16", startTime="08:00")).toInt()

    @Test fun completesAllValuesAndPreservesStartAndRefillConsumption() {
        val p=process(); val d=day(p); val fuel=db.harvestFuelDao()
        fuel.saveLevels(d,80.0,null)
        fuel.saveRefill(d,null,30.0,90.0,"12:00","DAY")
        fuel.saveRefill(d,null,10.0,80.0,null,"BEFORE_DAY")
        HarvestDayCompletion.save(db,d,"18:00",60.0,14.5,55.0,true)
        val saved=db.harvestDao().getDayById(d)!!
        assertEquals("COMPLETED",saved.status);assertEquals("18:00",saved.endTime)
        assertEquals(60.0,saved.harvestedAreaHa!!,0.0);assertEquals(14.5,saved.moisturePercent!!,0.0)
        assertEquals(80.0,fuel.getDay(d)!!.startPercent!!,0.0)
        val used=HarvestFuelCalculator.calculate(80.0,fuel.getDay(d)!!.endPercent,
            fuel.getRefills(d).map { HarvestFuelCalculator.Refill(it.beforePercent,it.afterPercent,it.scope=="BEFORE_DAY") },60.0)
        assertEquals(85.0,used.consumed!!,0.0)
        assertTrue(db.harvestDao().getActiveProcesses().isEmpty())
        db.harvestDao().updateDayDetails(d,saved.workDate,saved.startTime,null,"ACTIVE")
        assertEquals(55.0,fuel.getDay(d)!!.endPercent!!,0.0)
        assertEquals(60.0,db.harvestDao().getDayById(d)!!.harvestedAreaHa!!,0.0)
        assertEquals(d,HarvestQuickAccess.resolve(db,p)!!.dayId)
    }
    @Test fun emptyEndFuelStaysUnknownAndCampaignRemainsVisible() {
        val p=process();val d=day(p)
        db.harvestFuelDao().saveLevels(d,70.0,40.0)
        HarvestDayCompletion.save(db,d,"18:00",null,null,null,false)
        assertNull(db.harvestFuelDao().getDay(d)!!.endPercent)
        assertEquals(70.0,db.harvestFuelDao().getDay(d)!!.startPercent!!,0.0)
        assertNull(HarvestQuickAccess.resolve(db,p)!!.dayId)
        assertEquals(p,db.harvestDao().getActiveProcesses().single().id)
    }
    @Test fun fuelFailureRollsBackDayAndCampaign() {
        val p=process();val d=day(p);db.harvestFuelDao().saveLevels(d,70.0,40.0)
        val before=db.harvestDao().getDayById(d)
        try { HarvestDayCompletion.save(db,d,"18:00",60.0,14.5,101.0,true);fail() }
        catch (_: IllegalArgumentException) {}
        assertEquals(before,db.harvestDao().getDayById(d))
        assertEquals("IN_PROGRESS",db.harvestDao().getProcessById(p)!!.status)
        assertEquals(40.0,db.harvestFuelDao().getDay(d)!!.endPercent!!,0.0)
        // Force an actual SQLite failure after completing the day, not only input validation.
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER synthetic_fuel_failure BEFORE UPDATE OF endPercent ON harvest_fuel_days BEGIN SELECT RAISE(ABORT, 'synthetic'); END")
        try { HarvestDayCompletion.save(db,d,"18:00",60.0,14.5,50.0,true);fail() }
        catch (_: android.database.sqlite.SQLiteException) {}
        assertEquals(before,db.harvestDao().getDayById(d))
        assertEquals(40.0,db.harvestFuelDao().getDay(d)!!.endPercent!!,0.0)
    }
    @Test fun quickAccessZeroOneMultipleAndStaleSelection() {
        assertTrue(db.harvestDao().getActiveProcesses().isEmpty())
        val p=process();assertNull(HarvestQuickAccess.resolve(db,p)!!.dayId)
        val d=day(p);assertEquals(d,HarvestQuickAccess.resolve(db,p)!!.dayId)
        val second=process();assertEquals(2,db.harvestDao().getActiveProcesses().size)
        db.harvestDao().completeProcess(second,"2026-10-16")
        assertNull(HarvestQuickAccess.resolve(db,second))
        HarvestDayCompletion.save(db,d,"18:00",null,null,null,true)
        assertTrue(db.harvestDao().getActiveProcesses().isEmpty())
        assertNull(HarvestQuickAccess.resolve(db,p))
    }
    @Test fun snapshotAndActualAreaAreIndependentFromFieldAndDays() {
        val sql=db.openHelper.writableDatabase
        sql.execSQL("INSERT INTO fields(id,name,areaHa,isActive) VALUES(1,'synthetic',120,1)")
        val p=db.harvestDao().createProcess(HarvestProcess(processUid="snapshot",startDate="2026-10-16",fieldId=1,
            fieldNameSnapshot="synthetic",crop="Другое",combineVehicleId=null,combineNameSnapshot="synthetic")).toInt()
        val d=day(p)
        sql.execSQL("UPDATE fields SET areaHa=130 WHERE id=1")
        db.harvestDao().updateActualArea(p,95.0)
        HarvestDayCompletion.save(db,d,"18:00",60.0,null,null,true)
        db.harvestDao().updateActualArea(p,90.0)
        assertEquals(120.0,db.harvestDao().getProcessById(p)!!.fieldAreaSnapshotHa!!,0.0)
        assertEquals(90.0,db.harvestDao().getProcessById(p)!!.actualPlantedAreaHa!!,0.0)
        assertEquals(130.0,db.harvestDao().getFieldArea(1)!!,0.0)
        assertEquals(60.0,db.harvestDao().getDayById(d)!!.harvestedAreaHa!!,0.0)
    }
}
