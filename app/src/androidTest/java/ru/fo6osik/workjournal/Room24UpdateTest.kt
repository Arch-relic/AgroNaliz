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
    @Test fun seed23()=runBlocking(Dispatchers.IO) {
        guard();val room=DatabaseProvider.getDatabase(context);val db=room.openHelper.writableDatabase;assertEquals(23,db.version)
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
        context.getSharedPreferences("harvest_reminder_prefs",0).edit().putBoolean("enabled_26201",false).putInt("interval_26201",32).putInt("unload_26201",9).putLong("next_at_26201",2000000000000L).putBoolean("waiting_26201",true).commit()
        File(context.filesDir,"synthetic-room24-retained").writeText("keep")
        File(context.filesDir,"synthetic-room24-documents/manual.txt").apply { parentFile!!.mkdirs();writeText("synthetic document retained") }
        File(context.getExternalFilesDir(null),"synthetic-room24-document.txt").writeText("synthetic external document retained")
        val exported=DatabaseBackupManager.exportBackup(context,uri("before23"))
        assertTrue(exported.exceptionOrNull()?.stackTraceToString(),exported.isSuccess)
    }
    @Test fun verify24AndSeparateRestore()=runBlocking(Dispatchers.IO) {
        guard();val room=DatabaseProvider.getDatabase(context);assertEquals(24,room.openHelper.readableDatabase.version)
        val prefs=context.getSharedPreferences("synthetic-room24",0)
        assertEquals(prefs.getString("cert",null),cert());assertEquals(32,prefs.getInt("interval",0))
        val reminders=context.getSharedPreferences("harvest_reminder_prefs",0)
        assertFalse(reminders.getBoolean("enabled_26201",true));assertEquals(32,reminders.getInt("interval_26201",0))
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
        for(id in 26201..26202) { val d=room.harvestDao().getDayById(id)!!;assertEquals(1,d.reminderStepMinutes);assertEquals(32,d.workIntervalMinutes);assertEquals(555.125,d.bulkDensityKgM3!!,0.0) }
        room.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        val target=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            assertTrue(DatabaseBackupManager.importBackup(context,uri("before23"),target).isSuccess)
            assertTrue(DatabaseBackupManager.exportBackup(context,uri("restored23"),target).isSuccess)
            compare(read("before23"),read("restored23"))
            assertEquals(1,target.harvestDao().getDayById(26201)!!.reminderStepMinutes)
            assertTrue(DatabaseBackupManager.importBackup(context,uri("after24"),target).isSuccess)
            assertTrue(DatabaseBackupManager.exportBackup(context,uri("roundtrip24"),target).isSuccess)
            compare(read("after24"),read("roundtrip24"))
        } finally { target.close() }
    }
    @Test fun verify109Screens() {
        guard()
        assertEquals("1.0.9",context.packageManager.getPackageInfo(context.packageName,0).versionName)
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var ready=false
            val until=System.nanoTime()+10_000_000_000L
            while(!ready && System.nanoTime()<until) {
                scenario.onActivity { activity ->
                    val button=activity.findViewById<android.widget.Button>(R.id.buttonContinueHarvest)
                    ready=button.isEnabled && button.visibility==android.view.View.VISIBLE
                }
                if(!ready) Thread.sleep(50)
            }
            assertTrue("Launcher must reopen migrated active campaign",ready)
        }
        androidx.test.core.app.ActivityScenario.launch(ChangelogActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val scroll=activity.findViewById<android.widget.ScrollView>(R.id.changelogScroll)
                assertEquals(android.view.View.VISIBLE,scroll.findViewWithTag<android.view.View>("changelog-details-1.0.9").visibility)
                assertTrue(scroll.findViewWithTag<android.widget.Button>("changelog-header-1.0.9").text.contains("В разработке"))
            }
        }
        val intent=android.content.Intent(context,HarvestProcessActivity::class.java)
            .putExtra(HarvestProcessActivity.EXTRA_PROCESS_ID,26102).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        androidx.test.core.app.ActivityScenario.launch<HarvestProcessActivity>(intent).use { scenario ->
            val until=System.nanoTime()+10_000_000_000L
            var ready=false
            while(!ready && System.nanoTime()<until) {
                scenario.onActivity { activity ->
                    val card=activity.findViewById<HarvestResultsCardView>(R.id.harvestResultsCard)
                    val calendar=activity.findViewById<HarvestDayCalendarView>(R.id.harvestDayCalendar)
                    ready=card.currentModel!=null && calendar.calendar.month=="2026-10"
                    if(ready) assertEquals("4",card.currentModel!!.overview[1].second)
                }
                if(!ready) Thread.sleep(50)
            }
            assertTrue("Migrated campaign results and calendar must render",ready)
        }
    }
}
