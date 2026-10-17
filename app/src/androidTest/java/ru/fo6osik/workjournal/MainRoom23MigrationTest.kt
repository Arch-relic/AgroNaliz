package ru.fo6osik.workjournal

import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/** Only the fresh MAIN package on a disposable emulator, never a user's database. */
@RunWith(AndroidJUnit4::class)
class MainRoom23MigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get() = context.getSharedPreferences("synthetic-main-room23", 0)
    private fun guard() {
        check(context.packageName == "ru.fo6osik.workjournal")
        check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish"))
    }
    @Suppress("DEPRECATION") private fun installed() = context.packageManager.getPackageInfo(context.packageName, 64)
    @Suppress("DEPRECATION") private fun certificate() = MessageDigest.getInstance("SHA-256")
        .digest(installed().signatures!!.single().toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun uri(name: String) = Uri.fromFile(File(context.filesDir, "synthetic-main-$name.json"))
    private fun read(name: String) = context.contentResolver.openInputStream(uri(name))!!.use {
        JSONObject(it.bufferedReader().readText()).getJSONObject("tables")
    }
    private fun compare(before: JSONObject, after: JSONObject) {
        assertEquals(before.keys().asSequence().toSet(), after.keys().asSequence().toSet())
        for (table in before.keys()) {
            val old = before.getJSONArray(table); val fresh = after.getJSONArray(table)
            assertEquals(table, old.length(), fresh.length())
            for (i in 0 until old.length()) for (column in old.getJSONObject(i).keys())
                assertEquals("$table/$i/$column", old.getJSONObject(i).get(column).toString(),
                    fresh.getJSONObject(i).get(column).toString())
        }
    }
    @Test fun seedMain107Room22() = runBlocking(Dispatchers.IO) {
        guard(); assertEquals("1.0.7", installed().versionName); assertEquals(3000003, installed().versionCode)
        val db = DatabaseProvider.getDatabase(context).openHelper.writableDatabase
        assertEquals(22, db.version)
        for (table in listOf("fields", "vehicles", "harvest_processes", "farm_events"))
            db.query("SELECT count(*) FROM $table").use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
        db.beginTransaction()
        try {
            db.execSQL("INSERT INTO fields(id,name,areaHa,isActive) VALUES(12001,'synthetic',120,1)")
            db.execSQL("INSERT INTO vehicles(id,type,brand,model,number,year,note,engineId,engineName) VALUES(12001,'combine','synthetic','room22','SYNTHETIC',2020,'keep',NULL,'')")
            db.execSQL("INSERT INTO farm_events(id,eventUid,eventType,targetModule,title,description,eventYear,eventMonth,eventDay,datePrecision,payloadJson,targetRecordUid,createdAt) VALUES(12001,'main-room22-event','FIELD_WORK','FIELDS','synthetic','keep',2026,10,17,'DAY',NULL,NULL,100)")
            for (p in 1..2) {
                db.execSQL("""INSERT INTO harvest_processes(id,processUid,startDate,endDate,status,fieldId,fieldNameSnapshot,crop,combineVehicleId,combineNameSnapshot,bunkerVolumeM3,createdAt)
                    VALUES(?,?,'2026-10-17',?,?,12001,'synthetic','Другое',12001,'synthetic',8,100)""",
                    arrayOf<Any?>(12100+p,"main-process-$p",if(p==1)null else "2026-10-17",if(p==1)"IN_PROGRESS" else "COMPLETED"))
                db.execSQL("""INSERT INTO harvest_days(id,dayUid,processId,workDate,startTime,endTime,status,harvestedAreaHa,bulkDensityKgM3,densityMode,moisturePercent,workIntervalMinutes,workIntervalCustom,createdAt)
                    VALUES(?,?,?,'2026-10-17','08:00',?,?,60,750,'CUSTOM',14.5,47,1,101)""",
                    arrayOf<Any?>(12200+p,"main-day-$p",12100+p,if(p==1)null else "18:00",if(p==1)"ACTIVE" else "COMPLETED"))
                for ((i, level) in listOf("FULL","HALF","MORE_THAN_HALF","LESS_THAN_HALF").withIndex())
                    db.execSQL("INSERT INTO harvest_bunkers(id,bunkerUid,dayId,fillTime,fillLevel,postEventType,createdAt) VALUES(?,?,?,? ,?,'WAITING_TRANSPORT',102)",
                        arrayOf<Any?>(12300+p*10+i,"main-bunker-$p-$i",12200+p,"${9+i}:00".padStart(5,'0'),level))
                db.execSQL("INSERT INTO harvest_fuel_days(dayId,dayUid,vehicleId,vehicleNameSnapshot,startPercent,endPercent) VALUES(?,?,12001,'synthetic',80,55)", arrayOf<Any?>(12200+p,"main-day-$p"))
                for ((i, scope) in listOf("DAY", "BEFORE_DAY").withIndex())
                    db.execSQL("""INSERT INTO harvest_refuelings(id,refuelUid,dayId,dayUid,vehicleId,vehicleNameSnapshot,scope,beforePercent,afterPercent,time,createdAt)
                        VALUES(?,?,?,?,12001,'synthetic',?,30,90,'12:30',103)""", arrayOf<Any?>(12400+p*10+i,"main-refill-$p-$i",12200+p,"main-day-$p",scope))
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        File(context.filesDir,"synthetic-main-retained").writeText("MAIN22 synthetic file")
        assertTrue(prefs.edit().putString("certificate",certificate()).putInt("interval",47).putBoolean("enabled",false).commit())
        assertTrue(context.getSharedPreferences("harvest_reminder_prefs",0).edit().putBoolean("enabled_12201",false).putInt("interval_12201",47).commit())
        assertTrue(DatabaseBackupManager.exportBackup(context,uri("before22")).isSuccess)
    }
    @Test fun verifyMain108Room23AndSeparateRestore() = runBlocking(Dispatchers.IO) {
        guard(); assertEquals("1.0.8", installed().versionName); assertEquals(3000004, installed().versionCode)
        assertEquals(prefs.getString("certificate", null), certificate())
        assertEquals(47,prefs.getInt("interval",0)); assertFalse(prefs.getBoolean("enabled",true))
        val timers=context.getSharedPreferences("harvest_reminder_prefs",0)
        assertEquals(47,timers.getInt("interval_12201",0));assertFalse(timers.getBoolean("enabled_12201",true))
        assertEquals("MAIN22 synthetic file",File(context.filesDir,"synthetic-main-retained").readText())
        val room=DatabaseProvider.getDatabase(context); assertEquals(23,room.openHelper.readableDatabase.version)
        assertTrue(DatabaseBackupManager.exportBackup(context,uri("after23")).isSuccess)
        compare(read("before22"), read("after23"))
        for (id in 12101..12102) {
            val process=room.harvestDao().getProcessById(id)!!
            assertNull(process.fieldAreaSnapshotHa);assertNull(process.actualPlantedAreaHa)
        }
        assertEquals(12201,HarvestQuickAccess.resolve(room,12101)!!.dayId)
        assertNull(HarvestQuickAccess.resolve(room,12102))
        room.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        val day=room.harvestDao().getDayById(12202)!!
        val mass=HarvestCalculator.day(8.0,HarvestCalculator.DayInput(
            room.harvestDao().getBunkersForDay(day.id).map { it.fillLevel },day.harvestedAreaHa,day.bulkDensityKgM3))
        assertEquals(12.0,mass.massTonnes!!.min,0.0);assertEquals(18.0,mass.massTonnes!!.max,0.0)
        val fuel=room.harvestFuelDao().getDay(day.id)!!
        val used=HarvestFuelCalculator.calculate(fuel.startPercent,fuel.endPercent,
            room.harvestFuelDao().getRefills(day.id).map { HarvestFuelCalculator.Refill(it.beforePercent,it.afterPercent,it.scope=="BEFORE_DAY") },day.harvestedAreaHa)
        assertEquals(85.0,used.consumed!!,0.0)
        val target=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            assertTrue(DatabaseBackupManager.importBackup(context,uri("after23"),target).isSuccess)
            assertTrue(DatabaseBackupManager.exportBackup(context,uri("restored23"),target).isSuccess)
            compare(read("after23"),read("restored23"))
            target.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        } finally { target.close() }
    }
}
