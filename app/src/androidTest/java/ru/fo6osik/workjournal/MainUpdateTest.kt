package ru.fo6osik.workjournal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import org.json.JSONArray

/** Synthetic main-package in-place update sequence; disposable emulator only. */
@RunWith(AndroidJUnit4::class)
class MainUpdateTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get()=context.getSharedPreferences("synthetic-main-update",0)
    private fun guard() {
        check(context.packageName=="ru.fo6osik.workjournal")
        check(android.os.Build.HARDWARE in setOf("ranchu","goldfish")) { "Disposable emulator required" }
    }
    @Suppress("DEPRECATION") private fun version()=context.packageManager.getPackageInfo(context.packageName,0).versionCode
    @Suppress("DEPRECATION") private fun certificate():String {
        val bytes=context.packageManager.getPackageInfo(context.packageName,android.content.pm.PackageManager.GET_SIGNATURES).signatures!!.single().toByteArray()
        return java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    private fun snapshot(db:SupportSQLiteDatabase):JSONObject {
        val root=JSONObject()
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT IN ('android_metadata','sqlite_sequence','room_master_table') ORDER BY name").use { tables ->
            while(tables.moveToNext()) {
                val name=tables.getString(0);val rows=JSONArray()
                db.query("SELECT * FROM `$name` ORDER BY rowid").use { cursor ->
                    while(cursor.moveToNext()) {
                        val row=JSONObject()
                        for(i in 0 until cursor.columnCount) {
                            val value:Any=when(cursor.getType(i)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                                android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                                android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                                android.database.Cursor.FIELD_TYPE_BLOB -> android.util.Base64.encodeToString(cursor.getBlob(i),android.util.Base64.NO_WRAP)
                                else -> cursor.getString(i)
                            }
                            row.put(cursor.getColumnName(i),value)
                        }
                        rows.put(row)
                    }
                }
                root.put(name,rows)
            }
        }
        return JSONObject(root.toString())
    }
    private fun verifyRetained(db:SupportSQLiteDatabase,file:String) {
        val old=JSONObject(java.io.File(context.filesDir,file).readText());val current=snapshot(db)
        assertEquals(old.keys().asSequence().toSet()+setOf("harvest_fuel_days","harvest_refuelings"),current.keys().asSequence().toSet())
        for(table in old.keys()) {
            val before=old.getJSONArray(table);val after=current.getJSONArray(table)
            assertEquals(table,before.length(),after.length())
            for(i in 0 until before.length())for(column in before.getJSONObject(i).keys()) {
                assertEquals("$table/$i/$column",before.getJSONObject(i).get(column).toString(),after.getJSONObject(i).get(column).toString())
            }
        }
        assertEquals("synthetic retained file",java.io.File(context.filesDir,"main-update-file").readText())
        assertEquals(prefs.getString("certificate",null),certificate())
        assertTrue(version()>prefs.getInt("version",-1))
        val timers=context.getSharedPreferences("harvest_reminder_prefs",0)
        assertFalse(timers.getBoolean("enabled_4301",true));assertEquals(47,timers.getInt("interval_4301",0))
    }
    @Test fun seedV20() = runBlocking(Dispatchers.IO) {
        guard()
        val room = DatabaseProvider.getDatabase(context)
        val db = room.openHelper.writableDatabase
        assertEquals(20, db.version)
        db.execSQL("INSERT INTO fields (id,name,areaHa,location,note,isActive) VALUES (4001,'synthetic field',99.5,'synthetic','keep',1)")
        db.execSQL("INSERT INTO farm_events (id,eventUid,eventType,targetModule,title,description,eventYear,eventMonth,eventDay,datePrecision,payloadJson,targetRecordUid,createdAt) VALUES (4002,'migration-event','FIELD_WORK','FIELDS','synthetic','keep',2026,10,10,'DAY',NULL,NULL,100)")
        db.execSQL("INSERT INTO vehicles (id,type,brand,model,number,year,note,engineId,engineName) VALUES (4101,'combine','synthetic','migration','MIGRATION',2020,'keep',NULL,'')")
        db.execSQL("""INSERT INTO harvest_processes
            (id,processUid,startDate,endDate,status,fieldId,fieldNameSnapshot,crop,combineVehicleId,combineNameSnapshot,bunkerVolumeM3,createdAt)
            VALUES (4201,'migration-process','2026-10-10','2026-10-10','COMPLETED',4001,'synthetic','Пшеница',4101,'synthetic',8.0,101)""")
        db.execSQL("""INSERT INTO harvest_days
            (id,dayUid,processId,workDate,startTime,endTime,status,workIntervalMinutes,unloadIntervalMinutes,lunchIntervalMinutes,
             dinnerIntervalMinutes,waitingTransportIntervalMinutes,workIntervalCustom,unloadIntervalCustom,lunchIntervalCustom,
             dinnerIntervalCustom,waitingTransportIntervalCustom,reminderSettingsInitialized,createdAt)
            VALUES (4301,'migration-day',4201,'2026-10-10','08:00','18:00','COMPLETED',47,9,22,23,41,1,1,1,1,1,1,102)""")
        db.execSQL("INSERT INTO harvest_bunkers (id,bunkerUid,dayId,fillTime,fillLevel,postEventType,createdAt) VALUES (4401,'migration-bunker',4301,'09:00','MORE_THAN_HALF','WAITING_TRANSPORT',103)")
        db.execSQL("INSERT INTO harvest_processes SELECT 4202,'unknown-process',startDate,endDate,status,fieldId,fieldNameSnapshot,'Другое',combineVehicleId,combineNameSnapshot,bunkerVolumeM3,createdAt FROM harvest_processes WHERE id=4201")
        db.execSQL("INSERT INTO harvest_days SELECT 4302,'unknown-day',4202,workDate,startTime,endTime,status,workIntervalMinutes,unloadIntervalMinutes,lunchIntervalMinutes,dinnerIntervalMinutes,waitingTransportIntervalMinutes,workIntervalCustom,unloadIntervalCustom,lunchIntervalCustom,dinnerIntervalCustom,waitingTransportIntervalCustom,reminderSettingsInitialized,createdAt FROM harvest_days WHERE id=4301")
        assertTrue(context.getSharedPreferences("harvest_reminder_prefs", 0).edit()
            .putBoolean("enabled_4301", false).putInt("interval_4301", 47).commit())
        java.io.File(context.filesDir,"main-update-file").writeText("synthetic retained file")
        java.io.File(context.filesDir,"main-v20-snapshot.json").writeText(snapshot(db).toString())
        assertTrue(prefs.edit().putInt("version",version()).putString("certificate",certificate()).commit())
        room.close()
    }
    @Test fun verifyV21Update() = runBlocking(Dispatchers.IO) {
        guard()
        val room = DatabaseProvider.getDatabase(context)
        val db = room.openHelper.writableDatabase // Room validates the entire migrated schema here.
        assertEquals(22, db.version)
        fun verify() {
            db.query("SELECT * FROM harvest_days WHERE id=4301").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("harvestedAreaHa")))
                assertEquals(720.0, cursor.getDouble(cursor.getColumnIndexOrThrow("bulkDensityKgM3")), 0.0)
                assertEquals("MIN", cursor.getString(cursor.getColumnIndexOrThrow("densityMode")))
                assertEquals("COMPLETED", cursor.getString(cursor.getColumnIndexOrThrow("status")))
                assertEquals("18:00", cursor.getString(cursor.getColumnIndexOrThrow("endTime")))
                for ((column, value) in mapOf("workIntervalMinutes" to 47, "unloadIntervalMinutes" to 9,
                    "lunchIntervalMinutes" to 22, "dinnerIntervalMinutes" to 23, "waitingTransportIntervalMinutes" to 41,
                    "workIntervalCustom" to 1, "unloadIntervalCustom" to 1, "lunchIntervalCustom" to 1,
                    "dinnerIntervalCustom" to 1, "waitingTransportIntervalCustom" to 1, "reminderSettingsInitialized" to 1)) {
                    assertEquals(column, value, cursor.getInt(cursor.getColumnIndexOrThrow(column)))
                }
                assertEquals(102, cursor.getInt(cursor.getColumnIndexOrThrow("createdAt")))
            }
            db.query("SELECT * FROM harvest_processes WHERE id=4201").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(8.0, cursor.getDouble(cursor.getColumnIndexOrThrow("bunkerVolumeM3")), 0.0)
                assertEquals(4101, cursor.getInt(cursor.getColumnIndexOrThrow("combineVehicleId")))
                assertEquals("COMPLETED", cursor.getString(cursor.getColumnIndexOrThrow("status")))
            }
            db.query("SELECT fillLevel,postEventType,createdAt FROM harvest_bunkers WHERE id=4401").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("MORE_THAN_HALF", cursor.getString(0))
                assertEquals("WAITING_TRANSPORT", cursor.getString(1))
                assertEquals(103, cursor.getInt(2))
            }
            db.query("SELECT bulkDensityKgM3,densityMode,harvestedAreaHa FROM harvest_days WHERE id=4302").use {
                assertTrue(it.moveToFirst()); assertTrue(it.isNull(0)); assertTrue(it.isNull(1)); assertTrue(it.isNull(2))
            }
            db.query("SELECT areaHa FROM fields WHERE id=4001").use { assertTrue(it.moveToFirst()); assertEquals(99.5,it.getDouble(0),0.0) }
            db.query("SELECT description FROM farm_events WHERE id=4002").use { assertTrue(it.moveToFirst()); assertEquals("keep",it.getString(0)) }
            db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            db.query("SELECT number FROM vehicles WHERE id=4101").use { assertTrue(it.moveToFirst()); assertEquals("MIGRATION", it.getString(0)) }
        }
        verify()
        val reminderPrefs = context.getSharedPreferences("harvest_reminder_prefs", 0)
        assertFalse(reminderPrefs.getBoolean("enabled_4301", true))
        assertEquals(47, reminderPrefs.getInt("interval_4301", 0))
        verifyRetained(db,"main-v20-snapshot.json")
        assertTrue(version()>53)
        db.execSQL("UPDATE harvest_days SET harvestedAreaHa=3.125,bulkDensityKgM3=555.125,densityMode='CUSTOM' WHERE id=4301")
        java.io.File(context.filesDir,"main-v21-snapshot.json").writeText(snapshot(db).toString())
        assertTrue(prefs.edit().putInt("version",version()).commit())
        room.close()
    }
    @Test fun verifySecondUpdateAndRestart() = runBlocking(Dispatchers.IO) {
        guard()
        val room=DatabaseProvider.getDatabase(context)
        val db=room.openHelper.readableDatabase
        assertEquals(22,db.version)
        verifyRetained(db,"main-v21-snapshot.json")
        db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        room.close()
    }
}
