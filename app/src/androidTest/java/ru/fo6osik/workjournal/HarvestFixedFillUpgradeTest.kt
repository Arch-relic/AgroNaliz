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

/** Driver requires a fresh emulator and disposable MAIN/TEST debug fixtures. No phone data. */
@RunWith(AndroidJUnit4::class)
class HarvestFixedFillUpgradeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun guard() {
        check(context.packageName in setOf("ru.fo6osik.workjournal", "ru.fo6osik.workjournal.debugtest"))
        check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish"))
    }
    private fun uri(name: String): Uri {
        if (context.packageName == "ru.fo6osik.workjournal") return Uri.fromFile(File(context.filesDir,"issue48-$name.json"))
        val prefs = context.getSharedPreferences("issue48-backups",0)
        return prefs.getString(name,null)?.let(Uri::parse) ?: PrivateTestBackups.createDestination(context).also {
            check(prefs.edit().putString(name,it.toString()).commit())
        }
    }
    private fun read(name: String) = context.contentResolver.openInputStream(uri(name))!!.bufferedReader().use { JSONObject(it.readText()) }
    private suspend fun export(name: String, db: AppDatabase) {
        val result = DatabaseBackupManager.exportBackup(context,uri(name),db)
        assertTrue(result.exceptionOrNull()?.stackTraceToString(),result.isSuccess)
    }
    private fun schema(db: AppDatabase) = db.openHelper.readableDatabase.query("SELECT type,name,tbl_name,sql FROM sqlite_master ORDER BY type,name").use { c ->
        buildList { while(c.moveToNext()) add((0 until c.columnCount).joinToString("|") { if(c.isNull(it)) "NULL" else c.getString(it) }) }.joinToString("\n")
    }
    private fun assertTables(expected: JSONObject, actual: JSONObject) {
        val a=expected.getJSONObject("tables");val b=actual.getJSONObject("tables")
        assertEquals(a.keys().asSequence().toSet(),b.keys().asSequence().toSet())
        for(table in a.keys()) assertEquals(table,a.getJSONArray(table).toString(),b.getJSONArray(table).toString())
    }
    @Test fun seedBaseline110() = runBlocking(Dispatchers.IO) {
        guard();assertEquals("1.0.10",context.packageManager.getPackageInfo(context.packageName,0).versionName)
        val room=DatabaseProvider.getDatabase(context);val dao=room.harvestDao();val sql=room.openHelper.writableDatabase
        assertEquals(24,sql.version);assertTrue(dao.getAllProcessSummaries().isEmpty())
        sql.execSQL("INSERT INTO fields(id,name,areaHa,isActive) VALUES(48001,'synthetic issue48',10,1)")
        sql.execSQL("INSERT INTO vehicles(id,type,brand,model,number,year,note,engineId,engineName) VALUES(48001,'combine','synthetic','issue48','SYNTHETIC',2020,'keep',NULL,'')")
        sql.execSQL("INSERT INTO combine_specs(vehicleId,grainTankVolumeM3) VALUES(48001,4.7)")
        sql.execSQL("INSERT INTO farm_events(id,eventUid,eventType,targetModule,title,description,eventYear,eventMonth,eventDay,datePrecision,payloadJson,targetRecordUid,createdAt) VALUES(48001,'issue48-event','FIELD_WORK','FIELDS','synthetic','keep',2026,10,20,'DAY',NULL,NULL,100)")
        dao.insertProcess(HarvestProcess(id=48001,processUid="issue48-process",startDate="2026-10-20",endDate="2026-10-20",status="COMPLETED",
            fieldId=48001,fieldNameSnapshot="synthetic",crop="Пшеница",combineVehicleId=48001,combineNameSnapshot="synthetic",
            bunkerVolumeM3=4.7,fieldAreaSnapshotHa=10.0,actualPlantedAreaHa=10.0,createdAt=100))
        dao.insertDayRaw(HarvestDay(id=48001,dayUid="issue48-day",processId=48001,workDate="2026-10-20",startTime="08:00",endTime="18:00",
            status="COMPLETED",harvestedAreaHa=10.0,bulkDensityKgM3=720.0,densityMode="MIN",moisturePercent=14.5,
            workIntervalMinutes=47,unloadIntervalMinutes=9,reminderStepMinutes=5,createdAt=101))
        for(i in 0..10) dao.insertBunkerRaw(HarvestBunker(id=48001+i,bunkerUid="issue48-bunker-$i",dayId=48001,
            fillTime="08:${10+i}",fillLevel=if(i==10)"LESS_THAN_HALF" else "FULL",postEventType="WAITING_TRANSPORT",createdAt=102+i.toLong()))
        // An old campaign without a snapshot must resolve the actual card value without a write.
        dao.insertProcess(HarvestProcess(id=48002,processUid="issue48-missing-capacity",startDate="2026-10-20",fieldId=48001,
            fieldNameSnapshot="synthetic",crop="Пшеница",combineVehicleId=48001,combineNameSnapshot="synthetic",createdAt=104))
        context.getSharedPreferences("issue48-retained",0).edit().putInt("interval",47).commit()
        File(context.filesDir,"issue48-document.txt").writeText("synthetic retained document")
        File(context.filesDir,"issue48-schema.txt").writeText(schema(room))
        val old=HarvestCalculator.volume(4.7,dao.getBunkersForDay(48001).map { it.fillLevel })!!
        assertEquals(47.0,old.min,1e-10);assertEquals(49.35,old.max,1e-10)
        export("baseline",room)
    }
    private fun verifyMath(room: AppDatabase) {
        val dao=room.harvestDao();val process=dao.getProcessById(48001)!!;val day=dao.getDayById(48001)!!
        val input=HarvestCalculator.DayInput(dao.getBunkersForDay(day.id).map { it.fillLevel },day.harvestedAreaHa,day.bulkDensityKgM3)
        val result=HarvestCalculator.day(process.bunkerVolumeM3,input)
        val volume=requireNotNull(result.volumeM3);val mass=requireNotNull(result.massTonnes)
        assertEquals(48.175,volume.min,1e-10);assertEquals(volume.min,volume.max,0.0)
        assertEquals(34.686,mass.min,1e-10);assertEquals(mass.min,mass.max,0.0)
        assertEquals(3.4686,result.yieldTonnesPerHa!!.min,1e-10)
        val metrics=HarvestCampaignMetrics.calculate(process.bunkerVolumeM3,process.actualPlantedAreaHa,listOf(input),listOf(input),true,process.fieldAreaSnapshotHa)
        assertEquals(result.massTonnes,metrics.mass)
        assertFalse(HarvestResultsCardModel.from(metrics,10.0,10.0,false).overview.any { it.second.contains('–') })
        val missing=dao.getProcessById(48002)!!
        assertNull(missing.bunkerVolumeM3);assertEquals(4.7,dao.calculationProcess(missing).bunkerVolumeM3!!,0.0)
        assertEquals(missing,dao.getProcessById(48002))
    }
    @Test fun verifyUpgradeAndLegacyBackup() = runBlocking(Dispatchers.IO) {
        guard();assertEquals("1.0.11",context.packageManager.getPackageInfo(context.packageName,0).versionName)
        val room=DatabaseProvider.getDatabase(context);assertEquals(24,room.openHelper.readableDatabase.version)
        assertEquals(File(context.filesDir,"issue48-schema.txt").readText(),schema(room))
        repeat(3) { verifyMath(room) }
        assertEquals(47,context.getSharedPreferences("issue48-retained",0).getInt("interval",0))
        assertEquals("synthetic retained document",File(context.filesDir,"issue48-document.txt").readText())
        export("candidate",room);assertTables(read("baseline"),read("candidate"))
        val restored=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            val result=DatabaseBackupManager.importBackup(context,uri("baseline"),restored)
            assertTrue(result.exceptionOrNull()?.stackTraceToString(),result.isSuccess)
            verifyMath(restored);export("roundtrip",restored);assertTables(read("baseline"),read("roundtrip"))
        } finally { restored.close() }
    }
    @Test fun editOldBunkerAndRestoreWithoutChangingOtherFields() = runBlocking(Dispatchers.IO) {
        guard();val room=DatabaseProvider.getDatabase(context);val dao=room.harvestDao()
        val before=dao.getBunkerById(48011)!!;val day=dao.getDayById(48001);val process=dao.getProcessById(48001)
        try {
            dao.updateBunker(before.id,before.fillTime,"MORE_THAN_HALF")
            assertEquals(before.copy(fillLevel="MORE_THAN_HALF"),dao.getBunkerById(before.id))
            val v=HarvestCalculator.volume(4.7,dao.getBunkersForDay(48001).map { it.fillLevel })!!
            assertEquals(50.525,v.min,1e-10);assertEquals(v.min,v.max,0.0)
            assertEquals(day,dao.getDayById(48001));assertEquals(process,dao.getProcessById(48001))
        } finally { dao.updateBunker(before.id,before.fillTime,before.fillLevel) }
        assertEquals(before,dao.getBunkerById(before.id));verifyMath(room)
        export("after-edit",room);assertTables(read("baseline"),read("after-edit"))
    }
    @Test fun quantitiesStayTogetherOnSmallScreens() {
        guard();val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val value=HarvestValueText.unbroken("1,175 м³ · 34,686 т · 3,47 т/га · 34,69 ц/га")
            for(token in listOf("1,175 м³","34,686 т","3,47 т/га","34,69 ц/га")) {
                val begin=value.indexOf(token)
                val spans=value.getSpans(begin,begin+token.length,android.text.style.ReplacementSpan::class.java)
                assertEquals(1,spans.size);assertEquals(begin,value.getSpanStart(spans.single()))
                assertEquals(begin+token.length,value.getSpanEnd(spans.single()))
            }
        }
    }
}
