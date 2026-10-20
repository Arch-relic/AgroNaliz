package ru.fo6osik.workjournal

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import android.net.Uri
import java.io.File
import java.security.MessageDigest

/** The driver only installs disposable debug MAIN/TEST fixtures, never production certificates. */
@RunWith(AndroidJUnit4::class)
class Room24UpdateTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private fun guard() {
        check(context.packageName in setOf("ru.fo6osik.workjournal","ru.fo6osik.workjournal.debugtest"))
        check(android.os.Build.HARDWARE in setOf("ranchu","goldfish"))
    }
    private fun uri(name:String): Uri {
        if (context.packageName != "ru.fo6osik.workjournal.debugtest")
            return Uri.fromFile(File(context.filesDir,"synthetic-room24-$name.json"))
        val prefs=context.getSharedPreferences("synthetic-room24-uris",0)
        prefs.getString(name,null)?.let { return Uri.parse(it) }
        return PrivateTestBackups.createDestination(context).also {
            check(prefs.edit().putString(name,it.toString()).commit())
        }
    }
    private fun read(name:String)=JSONObject(context.contentResolver.openInputStream(uri(name))!!.bufferedReader().use { it.readText() })
    @Suppress("DEPRECATION") private fun cert()=MessageDigest.getInstance("SHA-256").digest(context.packageManager.getPackageInfo(context.packageName,64).signatures!!.single().toByteArray()).joinToString("") { "%02X".format(it) }
    private fun schemaVersion(db:androidx.sqlite.db.SupportSQLiteDatabase):Int = db.query("PRAGMA schema_version").use { it.moveToFirst();it.getInt(0) }
    private fun schemaSql(db:androidx.sqlite.db.SupportSQLiteDatabase):String = db.query("SELECT type,name,tbl_name,sql FROM sqlite_master ORDER BY type,name").use { c ->
        val rows=org.json.JSONArray()
        while(c.moveToNext()) rows.put(org.json.JSONArray((0 until c.columnCount).map { c.getString(it) }))
        rows.toString()
    }
    @Test fun seed23()=seed(23)
    @Test fun seed24()=seed(24)
    private fun seed(originalRoom:Int)=runBlocking(Dispatchers.IO) {
        guard();val room=DatabaseProvider.getDatabase(context);val db=room.openHelper.writableDatabase;assertEquals(originalRoom,db.version)
        val dao=room.harvestDao()
        assertTrue(dao.getAllProcessSummaries().isEmpty())
        db.execSQL("INSERT INTO fields(id,name,areaHa,isActive) VALUES(26001,'synthetic',120,1)")
        db.execSQL("INSERT INTO vehicles(id,type,brand,model,number,year,note,engineId,engineName) VALUES(26001,'combine','synthetic','room23','SYNTHETIC',2020,'keep',NULL,'')")
        db.execSQL("INSERT INTO farm_events(id,eventUid,eventType,targetModule,title,description,eventYear,eventMonth,eventDay,datePrecision,payloadJson,targetRecordUid,createdAt) VALUES(26001,'synthetic-room24-event','FIELD_WORK','FIELDS','synthetic','keep',2026,10,19,'DAY',NULL,NULL,100)")
        db.execSQL("INSERT INTO maintenance_records(id,vehicleId,date,meterValue,meterType,work,note,eventUid,datePrecision) VALUES(26001,26001,100,123,'mh','synthetic service','keep','synthetic-room24-event','DAY')")
        db.execSQL("INSERT INTO work_types(id,name,isActive) VALUES(26001,'synthetic work',1)")
        db.execSQL("INSERT INTO season_processes(id,processUid,title,fieldId,startDate,endDate,status,crop,goal,areaHa,note) VALUES(26001,'synthetic-season-process','synthetic',26001,'2026-10-19',NULL,'IN_PROGRESS','Пшеница','keep',95,'keep')")
        db.execSQL("INSERT INTO season_works(id,workDate,endDate,status,workTypeId,fieldId,processId,areaHa,note,eventUid) VALUES(26001,'2026-10-19',NULL,'COMPLETED',26001,26001,26001,60,'keep','synthetic-room24-event')")
        db.execSQL("INSERT INTO season_work_days(id,dayUid,seasonWorkId,workDate,endDate,areaHa,cumulativeAreaHa,stage,note) VALUES(26001,'synthetic-season-day',26001,'2026-10-19',NULL,60,60,'WORK','keep')")
        db.execSQL("INSERT INTO season_work_vehicles(id,seasonWorkId,vehicleId,vehicleNameSnapshot) VALUES(26001,26001,26001,'synthetic')")
        db.execSQL("INSERT INTO farm_event_fields(id,eventUid,fieldId) VALUES(26001,'synthetic-room24-event',26001)")
        db.execSQL("INSERT INTO farm_event_vehicles(id,eventUid,vehicleId,relationType,note) VALUES(26001,'synthetic-room24-event',26001,'RELATED','keep')")
        for(p in 1..2) {
            db.execSQL("""INSERT INTO harvest_processes(id,processUid,startDate,endDate,status,fieldId,fieldNameSnapshot,crop,combineVehicleId,combineNameSnapshot,bunkerVolumeM3,createdAt,fieldAreaSnapshotHa,actualPlantedAreaHa)
                VALUES(?,?,'2026-10-19',?,?,26001,'synthetic','Пшеница',26001,'synthetic',8,100,120,95)""",arrayOf<Any?>(26100+p,"synthetic-room24-p$p",if(p==1)null else "2026-10-19",if(p==1)"IN_PROGRESS" else "COMPLETED"))
            db.execSQL("""INSERT INTO harvest_days(id,dayUid,processId,workDate,startTime,endTime,status,harvestedAreaHa,bulkDensityKgM3,densityMode,moisturePercent,workIntervalMinutes,unloadIntervalMinutes,lunchIntervalMinutes,dinnerIntervalMinutes,waitingTransportIntervalMinutes,workIntervalCustom,unloadIntervalCustom,reminderSettingsInitialized,createdAt)
                VALUES(?,?,?,'2026-10-19','08:00',?,?,60,555.125,'CUSTOM',14.5,32,9,22,23,41,1,1,1,101)""",arrayOf<Any?>(26200+p,"synthetic-room24-d$p",26100+p,if(p==1)null else "18:00",if(p==1)"ACTIVE" else "COMPLETED"))
            for((n,level) in listOf("FULL","HALF","MORE_THAN_HALF","LESS_THAN_HALF").withIndex()) db.execSQL("INSERT INTO harvest_bunkers(id,bunkerUid,dayId,fillTime,fillLevel,postEventType,createdAt) VALUES(?,?,?,? ,?,'WAITING_TRANSPORT',102)",arrayOf<Any?>(26300+p*10+n,"synthetic-room24-b$p-$n",26200+p,"${9+n}:00".padStart(5,'0'),level))
            db.execSQL("INSERT INTO harvest_fuel_days(dayId,dayUid,vehicleId,vehicleNameSnapshot,startPercent,endPercent) VALUES(?,?,26001,'synthetic',80,55)",arrayOf<Any?>(26200+p,"synthetic-room24-d$p"))
            for((n,scope) in listOf("DAY","BEFORE_DAY").withIndex()) db.execSQL("INSERT INTO harvest_refuelings(id,refuelUid,dayId,dayUid,vehicleId,vehicleNameSnapshot,scope,beforePercent,afterPercent,time,createdAt) VALUES(?,?,?,?,26001,'synthetic',?,30,90,'12:30',103)",arrayOf<Any?>(26400+p*10+n,"synthetic-room24-r$p-$n",26200+p,"synthetic-room24-d$p",scope))
        }
        context.getSharedPreferences("synthetic-room24",0).edit().putString("cert",cert()).putInt("interval",32).commit()
        context.getSharedPreferences("harvest_reminder_prefs",0).edit().putBoolean("enabled_26201",true).putBoolean("enabled_26202",false).putInt("interval_26201",32).putInt("unload_26201",9).putLong("next_at_26201",2000000000000L).putBoolean("waiting_26201",true).commit()
        File(context.filesDir,"synthetic-room24-retained").writeText("keep")
        File(context.filesDir,"synthetic-room24-documents/manual.txt").apply { parentFile!!.mkdirs();writeText("synthetic document retained") }
        File(context.getExternalFilesDir(null),"synthetic-room24-document.txt").writeText("synthetic external document retained")
        if(originalRoom==24) {
            db.execSQL("UPDATE harvest_days SET reminderStepMinutes=5 WHERE id=26201")
            context.getSharedPreferences("synthetic-room24",0).edit().putInt("schemaVersion",schemaVersion(db)).putString("schemaSql",schemaSql(db)).commit()
        }
        val exported=DatabaseBackupManager.exportBackup(context,uri("before23"))
        assertTrue(exported.exceptionOrNull()?.stackTraceToString(),exported.isSuccess)
    }
    @Test fun verify24AndSeparateRestore()=verify(23)
    @Test fun verify24SameAndSeparateRestore()=verify(24)
    private fun verify(originalRoom:Int)=runBlocking(Dispatchers.IO) {
        guard();val room=DatabaseProvider.getDatabase(context);assertEquals(24,room.openHelper.readableDatabase.version)
        val prefs=context.getSharedPreferences("synthetic-room24",0)
        assertEquals(prefs.getString("cert",null),cert());assertEquals(32,prefs.getInt("interval",0))
        val reminders=context.getSharedPreferences("harvest_reminder_prefs",0)
        assertTrue(reminders.getBoolean("enabled_26201",false));assertFalse(reminders.getBoolean("enabled_26202",true));assertEquals(32,reminders.getInt("interval_26201",0))
        assertEquals(9,reminders.getInt("unload_26201",0));assertEquals(2000000000000L,reminders.getLong("next_at_26201",0L));assertTrue(reminders.getBoolean("waiting_26201",false))
        assertEquals("keep",File(context.filesDir,"synthetic-room24-retained").readText())
        assertEquals("synthetic document retained",File(context.filesDir,"synthetic-room24-documents/manual.txt").readText())
        assertEquals("synthetic external document retained",File(context.getExternalFilesDir(null),"synthetic-room24-document.txt").readText())
        assertTrue(DatabaseBackupManager.exportBackup(context,uri("after24")).isSuccess)
        fun compare(old:JSONObject,fresh:JSONObject) {
            val before=old.getJSONObject("tables");val after=fresh.getJSONObject("tables")
            assertEquals(before.keys().asSequence().toSet(),after.keys().asSequence().toSet())
            for(t in before.keys()) { val rows=before.getJSONArray(t);val other=after.getJSONArray(t);assertEquals(t,rows.length(),other.length())
                for(i in 0 until rows.length()) for(k in rows.getJSONObject(i).keys()) assertEquals("$t/$i/$k",rows.getJSONObject(i).get(k).toString(),other.getJSONObject(i).get(k).toString()) }
        }
        compare(read("before23"),read("after24"))
        for(id in 26201..26202) { val d=room.harvestDao().getDayById(id)!!;assertEquals(if(originalRoom==24 && id==26201)5 else 1,d.reminderStepMinutes);assertEquals(32,d.workIntervalMinutes);assertEquals(555.125,d.bulkDensityKgM3!!,0.0) }
        room.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        if(originalRoom==24) {
            assertEquals(prefs.getInt("schemaVersion",-1),schemaVersion(room.openHelper.readableDatabase))
            assertEquals(prefs.getString("schemaSql",null),schemaSql(room.openHelper.readableDatabase))
        }
        val target=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            assertTrue(DatabaseBackupManager.importBackup(context,uri("before23"),target).isSuccess)
            assertTrue(DatabaseBackupManager.exportBackup(context,uri("restored23"),target).isSuccess)
            compare(read("before23"),read("restored23"))
            assertEquals(if(originalRoom==24)5 else 1,target.harvestDao().getDayById(26201)!!.reminderStepMinutes)
            assertTrue(DatabaseBackupManager.importBackup(context,uri("after24"),target).isSuccess)
            assertTrue(DatabaseBackupManager.exportBackup(context,uri("roundtrip24"),target).isSuccess)
            compare(read("after24"),read("roundtrip24"))
        } finally { target.close() }
    }
}
