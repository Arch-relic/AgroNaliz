package ru.fo6osik.workjournal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HarvestFuelTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val db get()=DatabaseProvider.getDatabase(context)
    private fun guard(){check(context.packageName=="ru.fo6osik.workjournal.debugtest");check(android.os.Build.HARDWARE in setOf("ranchu","goldfish"))}
    @Test fun seed21()=runBlocking(Dispatchers.IO) {
        guard();val sql=db.openHelper.writableDatabase;assertEquals(21,sql.version)
        sql.execSQL("INSERT INTO vehicles(id,type,brand,model,number,year,note,engineId,engineName) VALUES(7001,'combine','synthetic','fuel','FUEL-TEST',2020,'keep',NULL,'')")
        sql.execSQL("""INSERT INTO harvest_processes(id,processUid,startDate,endDate,status,fieldId,fieldNameSnapshot,crop,combineVehicleId,combineNameSnapshot,bunkerVolumeM3,createdAt) VALUES(8001,'fuel-migration','2026-10-10','2026-10-10','COMPLETED',NULL,'synthetic','Пшеница',7001,'synthetic combine',8,1)""")
        sql.execSQL("""INSERT INTO harvest_days(id,dayUid,processId,workDate,startTime,endTime,status,harvestedAreaHa,bulkDensityKgM3,densityMode,createdAt) VALUES(9001,'fuel-day',8001,'2026-10-10','08:00','18:00','COMPLETED',3.125,555.125,'CUSTOM',2)""")
        sql.execSQL("INSERT INTO harvest_bunkers(id,bunkerUid,dayId,fillTime,fillLevel,postEventType,createdAt) VALUES(9002,'fuel-bunker',9001,'09:00','HALF','WAITING_TRANSPORT',3)")
        val uri=PrivateTestBackups.createDestination(context)
        assertTrue(DatabaseBackupManager.exportBackup(context,uri).isSuccess)
        assertTrue(context.getSharedPreferences("fuel-tests",0).edit().putString("legacy",uri.toString()).commit())
    }
    @Test fun verify22()=runBlocking(Dispatchers.IO) {
        guard();assertEquals(22,db.openHelper.readableDatabase.version)
        val day=db.harvestDao().getDayById(9001)!!
        assertNull(day.moisturePercent);assertEquals(555.125,day.bulkDensityKgM3!!,0.0)
        assertEquals(3.125,day.harvestedAreaHa!!,0.0);assertEquals("CUSTOM",day.densityMode)
        assertEquals("WAITING_TRANSPORT",db.harvestDao().getBunkersForDay(9001).single().postEventType)
        assertNull(db.harvestFuelDao().getDay(9001));assertTrue(db.harvestFuelDao().getRefills(9001).isEmpty())
    }
    private fun populate() {
        db.harvestDao().updateMoisture(9001,14.25)
        val fuel=db.harvestFuelDao();fuel.saveLevels(9001,80.0,55.0)
        if(fuel.getRefills(9001).isEmpty()){
            fuel.saveRefill(9001,null,30.0,90.0,"12:30","DAY")
            fuel.saveRefill(9001,null,20.0,80.0,null,"BEFORE_DAY")
        }
    }
    @Test fun completedEditsAndReopenPreserve()=runBlocking(Dispatchers.IO) {
        guard();populate();val dao=db.harvestDao();val fuel=db.harvestFuelDao()
        assertEquals(14.25,dao.getDayById(9001)!!.moisturePercent!!,0.0)
        dao.updateMoisture(9001,0.0);assertEquals(0.0,dao.getDayById(9001)!!.moisturePercent!!,0.0)
        dao.updateMoisture(9001,null);assertNull(dao.getDayById(9001)!!.moisturePercent)
        dao.updateMoisture(9001,14.25)
        val row=fuel.getRefills(9001).first { it.scope=="DAY" }
        fuel.saveRefill(9001,row.id,30.0,95.0,"00:30","DAY")
        assertEquals(row.refuelUid,fuel.getRefill(row.id)!!.refuelUid)
        fuel.saveRefill(9001,row.id,30.0,90.0,"12:30","DAY")
        val extra=fuel.saveRefill(9001,null,50.0,60.0,null,"DAY").toInt();fuel.deleteRefill(extra)
        dao.updateDayDetails(9001,"2026-10-10","08:00",null,"ACTIVE")
        assertEquals(2,fuel.getRefills(9001).size)
        assertEquals(80.0,fuel.getDay(9001)!!.startPercent!!,0.0)
        dao.completeDay(9001,"19:00");dao.completeProcess(8001,"2026-10-10")
        assertEquals(555.125,dao.getDayById(9001)!!.bulkDensityKgM3!!,0.0)
        val used=HarvestFuelCalculator.calculate(80.0,55.0,fuel.getRefills(9001).map { HarvestFuelCalculator.Refill(it.beforePercent,it.afterPercent,it.scope=="BEFORE_DAY") },3.125)
        assertEquals(85.0,used.consumed!!,0.0);assertEquals(27.2,used.perHa!!,0.000001)
    }
    @Test fun betweenDaysIsIndependent()=runBlocking(Dispatchers.IO) {
        guard();populate();val dao=db.harvestDao();val fuel=db.harvestFuelDao()
        val next=dao.insertDay(HarvestDay(dayUid=UUID.randomUUID().toString(),processId=8001,workDate="2026-10-11",startTime="08:00",endTime="18:00",status="COMPLETED")).toInt()
        fuel.saveLevels(next,90.0,50.0)
        assertTrue(fuel.getRefills(next).isEmpty()) // No inferred automatic refuelling.
        fuel.saveRefill(next,null,55.0,90.0,null,"BEFORE_DAY")
        assertEquals(55.0,fuel.previous(next,7001)!!.endPercent!!,0.0)
        val row=fuel.getRefills(next).single()
        assertEquals(7001,row.vehicleId!!);assertEquals(dao.getDayById(next)!!.dayUid,row.dayUid)
        val result=HarvestFuelCalculator.calculate(90.0,50.0,listOf(HarvestFuelCalculator.Refill(row.beforePercent,row.afterPercent,true)),null)
        assertEquals(40.0,result.consumed!!,0.0);assertNull(result.perHa)
        assertEquals(80.0,fuel.getDay(9001)!!.startPercent!!,0.0)
    }
    @Test fun newBackupAndMergeRoundTrip()=runBlocking(Dispatchers.IO) {
        guard();populate();val uri=PrivateTestBackups.createDestination(context)
        assertTrue(DatabaseBackupManager.exportBackup(context,uri).isSuccess)
        db.harvestDao().updateMoisture(9001,90.0)
        assertTrue(DatabaseBackupManager.importBackup(context,uri).isSuccess)
        assertEquals(14.25,db.harvestDao().getDayById(9001)!!.moisturePercent!!,0.0)
        assertEquals(2,db.harvestFuelDao().getRefills(9001).size)
        val count=db.harvestFuelDao().getRefills(9001).size
        assertTrue(DataImportManager.importData(context,uri).isSuccess)
        assertEquals(count,db.harvestFuelDao().getRefills(9001).size)
    }
    @Test fun legacyBackupAndImport()=runBlocking(Dispatchers.IO) {
        guard();val prefs=context.getSharedPreferences("fuel-tests",0)
        val uri=android.net.Uri.parse(prefs.getString("legacy",null)!!)
        populate();assertTrue(DataImportManager.importData(context,uri).isSuccess)
        assertEquals(2,db.harvestFuelDao().getRefills(9001).size)
        assertTrue(DatabaseBackupManager.importBackup(context,uri).isSuccess)
        assertNull(db.harvestDao().getDayById(9001)!!.moisturePercent)
        assertTrue(db.harvestFuelDao().getRefills(9001).isEmpty())
        populate()
    }
    @Test fun invalidValuesRollback()=runBlocking(Dispatchers.IO) {
        guard();populate()
        for(value in listOf(-1.0,101.0,Double.NaN)) {
            try{db.harvestDao().updateMoisture(9001,value);fail()}catch(_:IllegalArgumentException){}
            try{db.harvestFuelDao().saveLevels(9001,value,10.0);fail()}catch(_:IllegalArgumentException){}
        }
        assertEquals(80.0,db.harvestFuelDao().getDay(9001)!!.startPercent!!,0.0)
    }
    @Test fun restartPreserved()=runBlocking(Dispatchers.IO) {
        guard();assertEquals(14.25,db.harvestDao().getDayById(9001)!!.moisturePercent!!,0.0)
        assertEquals(2,db.harvestFuelDao().getRefills(9001).size)
        assertEquals(80.0,db.harvestFuelDao().getDay(9001)!!.startPercent!!,0.0)
    }
}
