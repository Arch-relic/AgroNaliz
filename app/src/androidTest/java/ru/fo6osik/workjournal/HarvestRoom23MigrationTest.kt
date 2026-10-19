package ru.fo6osik.workjournal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Seed with the real Room22 TEST APK, then verify after install -r of Room23. */
@RunWith(AndroidJUnit4::class)
class HarvestRoom23MigrationTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private fun guard() {
        check(context.packageName=="ru.fo6osik.workjournal.debugtest")
        check(android.os.Build.HARDWARE in setOf("ranchu","goldfish"))
    }
    @Test fun seed22()=runBlocking(Dispatchers.IO) {
        guard();val db=DatabaseProvider.getDatabase(context).openHelper.writableDatabase
        assertEquals(22,db.version)
        db.execSQL("INSERT INTO fields(id,name,areaHa,isActive) VALUES(12001,'synthetic',120,1)")
        db.execSQL("INSERT INTO vehicles(id,type,brand,model,number,year,note,engineId,engineName) VALUES(12001,'combine','synthetic','room22','SYNTHETIC',2020,'keep',NULL,'')")
        for(p in 1..2) {
            db.execSQL("""INSERT INTO harvest_processes(id,processUid,startDate,endDate,status,fieldId,fieldNameSnapshot,crop,combineVehicleId,combineNameSnapshot,bunkerVolumeM3,createdAt)
                VALUES(?,?,'2026-10-16',?,?,12001,'synthetic','Другое',12001,'synthetic',8,100)""",
                arrayOf(12100+p,"room22-$p",if(p==1)null else "2026-10-16",if(p==1)"IN_PROGRESS" else "COMPLETED"))
            db.execSQL("""INSERT INTO harvest_days(id,dayUid,processId,workDate,startTime,endTime,status,harvestedAreaHa,bulkDensityKgM3,densityMode,moisturePercent,workIntervalMinutes,workIntervalCustom,createdAt)
                VALUES(?,?,?,'2026-10-16','08:00',?,?,60,750,'CUSTOM',14.5,47,1,101)""",
                arrayOf(12200+p,"room22-day-$p",12100+p,if(p==1)null else "18:00",if(p==1)"ACTIVE" else "COMPLETED"))
            db.execSQL("INSERT INTO harvest_bunkers(id,bunkerUid,dayId,fillTime,fillLevel,postEventType,createdAt) VALUES(?,?,?,'09:00','MORE_THAN_HALF','WAITING_TRANSPORT',102)",
                arrayOf(12300+p,"room22-bunker-$p",12200+p))
            db.execSQL("INSERT INTO harvest_fuel_days(dayId,dayUid,vehicleId,vehicleNameSnapshot,startPercent,endPercent) VALUES(?,?,12001,'synthetic',80,55)",arrayOf(12200+p,"room22-day-$p"))
            db.execSQL("""INSERT INTO harvest_refuelings(id,refuelUid,dayId,dayUid,vehicleId,vehicleNameSnapshot,scope,beforePercent,afterPercent,time,createdAt)
                VALUES(?,?,?,?,12001,'synthetic','DAY',30,90,'12:30',103)""",arrayOf(12400+p,"room22-refill-$p",12200+p,"room22-day-$p"))
        }
        val uri=PrivateTestBackups.createDestination(context)
        assertTrue(DatabaseBackupManager.exportBackup(context,uri).isSuccess)
        assertTrue(context.getSharedPreferences("synthetic-room23-migration",0).edit().putString("backup",uri.toString()).commit())
    }
    @Test fun verify23AllValuesAndRestart()=runBlocking(Dispatchers.IO) {
        guard();val room=DatabaseProvider.getDatabase(context);val db=room.openHelper.readableDatabase
        assertEquals(24,db.version)
        val oldUri=android.net.Uri.parse(context.getSharedPreferences("synthetic-room23-migration",0).getString("backup",null)!!)
        val uri=PrivateTestBackups.createDestination(context)
        assertTrue(DatabaseBackupManager.exportBackup(context,uri).isSuccess)
        fun read(u:android.net.Uri)=context.contentResolver.openInputStream(u)!!.use { JSONObject(it.bufferedReader().readText()).getJSONObject("tables") }
        val old=read(oldUri);val current=read(uri)
        for(table in old.keys()) {
            val before=old.getJSONArray(table);val after=current.getJSONArray(table)
            assertEquals(table,before.length(),after.length())
            for(i in 0 until before.length()) for(column in before.getJSONObject(i).keys())
                assertEquals("$table/$i/$column",before.getJSONObject(i).get(column).toString(),after.getJSONObject(i).get(column).toString())
        }
        for(p in 12101..12102) {
            val process=room.harvestDao().getProcessById(p)!!
            assertNull(process.fieldAreaSnapshotHa);assertNull(process.actualPlantedAreaHa)
        }
        assertEquals(12201,HarvestQuickAccess.resolve(room,12101)!!.dayId)
        assertNull(HarvestQuickAccess.resolve(room,12102))
        db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
    }
}
