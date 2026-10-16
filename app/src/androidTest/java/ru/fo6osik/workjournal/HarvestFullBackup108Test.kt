package ru.fo6osik.workjournal

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HarvestFullBackup108Test {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase
    private val tables = listOf("fields", "vehicles", "harvest_processes", "harvest_days", "harvest_bunkers", "harvest_fuel_days", "harvest_refuelings")
    @Before fun setup() {
        check(context.packageName == "ru.fo6osik.workjournal.debugtest")
        source = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        target = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val sql = source.openHelper.writableDatabase
        sql.execSQL("INSERT INTO fields(id,name,areaHa,location,note,isActive) VALUES(1,'synthetic',120,'synthetic','keep',1)")
        sql.execSQL("INSERT INTO vehicles(id,type,brand,model,number,year,note,engineId,engineName) VALUES(1,'combine','synthetic','backup','SYNTHETIC',2020,'keep',NULL,'')")
        for (p in 1..2) {
            val process = source.harvestDao().createProcess(HarvestProcess(processUid="backup-process-$p",startDate="2026-10-15",
                fieldId=1,fieldNameSnapshot="synthetic",crop="Другое",combineVehicleId=1,combineNameSnapshot="synthetic",bunkerVolumeM3=8.0)).toInt()
            source.harvestDao().updateActualArea(process,95.0)
            for (n in 1..2) {
                val day=source.harvestDao().insertDay(HarvestDay(dayUid="backup-day-$p-$n",processId=process,
                    workDate="2026-10-${14+n}",startTime="08:00",workIntervalMinutes=47,workIntervalCustom=true,
                    unloadIntervalMinutes=9,lunchIntervalMinutes=22,dinnerIntervalMinutes=23,waitingTransportIntervalMinutes=41,
                    unloadIntervalCustom=true,lunchIntervalCustom=true,dinnerIntervalCustom=true,waitingTransportIntervalCustom=true)).toInt()
                source.harvestDao().updateDayDensity(day,"CUSTOM",750.0)
                for ((index,level) in listOf("FULL","HALF","MORE_THAN_HALF","LESS_THAN_HALF").withIndex()) {
                    val bunker=source.harvestDao().insertBunker(HarvestBunker(bunkerUid="backup-bunker-$p-$n-$index",dayId=day,
                        fillTime="${9+index}:00".padStart(5,'0'),fillLevel=level)).toInt()
                    source.harvestDao().updateBunkerPostEventType(bunker,"WAITING_TRANSPORT")
                }
                source.harvestFuelDao().saveLevels(day,80.0,null)
                source.harvestFuelDao().saveRefill(day,null,30.0,90.0,"12:30","DAY")
                source.harvestFuelDao().saveRefill(day,null,20.0,80.0,null,"BEFORE_DAY")
                if (p==2 || n==1) HarvestDayCompletion.save(source,day,"18:00",30.0,14.5,55.0,p==2 && n==2)
                else source.harvestDao().updateMoisture(day,13.0)
            }
        }
    }
    @After fun close() { source.close();target.close() }
    private fun rows(db: AppDatabase, table: String): List<String> =
        db.openHelper.readableDatabase.query("SELECT * FROM $table").use { cursor ->
            buildList { while(cursor.moveToNext()) add((0 until cursor.columnCount).joinToString("|") { i ->
                cursor.getColumnName(i)+"="+if(cursor.isNull(i)) "NULL" else cursor.getString(i)
            }) }.sorted()
        }
    private suspend fun backup(): android.net.Uri {
        val uri=PrivateTestBackups.createDestination(context)
        val result=DatabaseBackupManager.exportBackup(context,uri,source)
        assertTrue(result.exceptionOrNull()?.stackTraceToString(),result.isSuccess)
        return uri
    }
    private fun read(uri: android.net.Uri)=context.contentResolver.openInputStream(uri)!!.use { JSONObject(it.bufferedReader().readText()) }
    private fun write(uri: android.net.Uri,json: JSONObject) {
        context.contentResolver.openOutputStream(uri,"wt")!!.bufferedWriter().use { it.write(json.toString()) }
    }
    private fun verifyCalculations(db: AppDatabase) {
        for(p in db.harvestDao().getAllProcessSummaries()) {
            val days=db.harvestDao().getDaysForProcess(p.id)
            val result=HarvestCalculator.campaign(p.bunkerVolumeM3,days.map { day ->
                HarvestCalculator.DayInput(db.harvestDao().getBunkersForDay(day.id).map { it.fillLevel },day.harvestedAreaHa,day.bulkDensityKgM3) })
            assertEquals(24.0,result.massTonnes!!.min,0.0)
            assertEquals(36.0,result.massTonnes!!.max,0.0)
            for(day in days.filter { it.status=="COMPLETED" }) {
                val fuel=db.harvestFuelDao().getDay(day.id)!!
                val used=HarvestFuelCalculator.calculate(fuel.startPercent,fuel.endPercent,
                    db.harvestFuelDao().getRefills(day.id).map { HarvestFuelCalculator.Refill(it.beforePercent,it.afterPercent,it.scope=="BEFORE_DAY") },day.harvestedAreaHa)
                assertEquals(85.0,used.consumed!!,0.0)
            }
        }
    }
    @Test fun fullJsonRestoresAllColumnsAndRelationshipsIntoSeparateDatabase()=runBlocking(Dispatchers.IO) {
        val uri=backup();val json=read(uri)
        assertEquals(23,json.getInt("databaseVersion"))
        for(table in tables) assertTrue(json.getJSONObject("tables").getJSONArray(table).length()>0)
        val result=DatabaseBackupManager.importBackup(context,uri,target)
        assertTrue(result.exceptionOrNull()?.stackTraceToString(),result.isSuccess)
        for(table in tables) assertEquals(table,rows(source,table),rows(target,table))
        assertEquals(1,target.harvestDao().getActiveProcesses().size)
        assertEquals(1,target.harvestDao().getDaysForProcess(1).count { it.status=="ACTIVE" })
        target.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        verifyCalculations(target)
    }
    @Test fun mergeRemapsIdsPreservesLocalEditsAndRepeatedImportIsIdempotent()=runBlocking(Dispatchers.IO) {
        val uri=backup();val sql=target.openHelper.writableDatabase
        sql.execSQL("INSERT INTO fields(id,name,areaHa,isActive) VALUES(1,'unrelated',999,1)")
        sql.execSQL("INSERT INTO vehicles(id,type,brand,model,number,year,note,engineId,engineName) VALUES(1,'combine','other','other','OTHER',2020,'keep',NULL,'')")
        val result=DataImportManager.importData(context,uri,target)
        assertTrue(result.exceptionOrNull()?.stackTraceToString(),result.isSuccess)
        val process=target.harvestDao().getProcessById(1)!!
        assertNotEquals(1,process.fieldId);assertNotEquals(1,process.combineVehicleId)
        assertEquals(120.0,process.fieldAreaSnapshotHa!!,0.0)
        target.harvestDao().updateActualArea(1,90.0)
        val before=tables.associateWith { rows(target,it) }
        assertTrue(DataImportManager.importData(context,uri,target).isSuccess)
        for(table in tables) assertEquals(table,before[table],rows(target,table))
        verifyCalculations(target)
    }
    @Test fun room22BackupLeavesHistoricalAreasNullForRestoreAndMerge()=runBlocking(Dispatchers.IO) {
        val uri=backup();val json=read(uri);json.put("databaseVersion",22)
        val processes=json.getJSONObject("tables").getJSONArray("harvest_processes")
        for(i in 0 until processes.length()) { processes.getJSONObject(i).remove("fieldAreaSnapshotHa"); processes.getJSONObject(i).remove("actualPlantedAreaHa") }
        write(uri,json)
        assertTrue(DatabaseBackupManager.importBackup(context,uri,target).isSuccess)
        for(p in target.harvestDao().getAllProcessSummaries()) {
            assertNull(target.harvestDao().getProcessById(p.id)!!.fieldAreaSnapshotHa)
            assertNull(target.harvestDao().getProcessById(p.id)!!.actualPlantedAreaHa)
        }
        val third=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            assertTrue(DataImportManager.importData(context,uri,third).isSuccess)
            assertNull(third.harvestDao().getProcessById(1)!!.fieldAreaSnapshotHa)
            verifyCalculations(third)
        } finally { third.close() }
    }
    @Test fun corruptAndRelationallyInvalidBackupsLeaveExistingDatabaseUnchanged()=runBlocking(Dispatchers.IO) {
        val uri=backup();assertTrue(DatabaseBackupManager.importBackup(context,uri,target).isSuccess)
        val before=tables.associateWith { rows(target,it) }
        val json=read(uri)
        json.getJSONObject("tables").getJSONArray("harvest_bunkers").getJSONObject(0).put("dayId",999999)
        write(uri,json)
        assertTrue(DatabaseBackupManager.importBackup(context,uri,target).isFailure)
        for(table in tables) assertEquals(before[table],rows(target,table))
        assertTrue(DataImportManager.importData(context,uri,target).isFailure)
        for(table in tables) assertEquals(before[table],rows(target,table))
        context.contentResolver.openOutputStream(uri,"wt")!!.bufferedWriter().use { it.write("{broken") }
        assertTrue(DatabaseBackupManager.importBackup(context,uri,target).isFailure)
        assertTrue(DataImportManager.importData(context,uri,target).isFailure)
        for(table in tables) assertEquals(before[table],rows(target,table))
    }
}
