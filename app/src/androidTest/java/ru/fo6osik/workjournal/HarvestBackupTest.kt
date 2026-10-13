package ru.fo6osik.workjournal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HarvestBackupTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val room get() = DatabaseProvider.getDatabase(context)
    private fun fixture(): Pair<Int, Int> {
        check(context.packageName == "ru.fo6osik.workjournal.debugtest")
        val process = room.harvestDao().insertProcess(HarvestProcess(
            processUid = UUID.randomUUID().toString(), startDate = "2026-10-11", fieldId = null,
            fieldNameSnapshot = "synthetic backup", crop = "Другое", combineVehicleId = null,
            combineNameSnapshot = "synthetic", bunkerVolumeM3 = 8.0
        )).toInt()
        val day = room.harvestDao().insertDay(HarvestDay(dayUid = UUID.randomUUID().toString(), processId = process,
            workDate = "2026-10-11", startTime = "08:00", endTime = "18:00", status = "COMPLETED",
            harvestedAreaHa = 2.375)).toInt()
        room.harvestDao().updateDayDensity(day, "CUSTOM", 555.5)
        return process to day
    }
    @Test fun v21FullBackupRoundTripPreservesNewFields() = runBlocking(Dispatchers.IO) {
        val (process, day) = fixture()
        val uri = PrivateTestBackups.createDestination(context)
        assertTrue(DatabaseBackupManager.exportBackup(context, uri).isSuccess)
        room.harvestDao().updateDayDensity(day, "CUSTOM", 600.0)
        room.harvestDao().updateHarvestedArea(day, 5.0)
        assertTrue(DatabaseBackupManager.importBackup(context, uri).isSuccess)
        assertEquals(555.5, room.harvestDao().getDayById(day)!!.bulkDensityKgM3!!, 0.0)
        assertEquals(2.375, room.harvestDao().getDayById(day)!!.harvestedAreaHa!!, 0.0)
    }
    @Test fun v21AdditiveImportPreservesNewFieldsAndRemapsRelationships() = runBlocking(Dispatchers.IO) {
        val (process, day) = fixture()
        val processUid = room.harvestDao().getProcessById(process)!!.processUid
        val dayUid = room.harvestDao().getDayById(day)!!.dayUid
        val uri = PrivateTestBackups.createDestination(context)
        assertTrue(DatabaseBackupManager.exportBackup(context, uri).isSuccess)
        room.openHelper.writableDatabase.execSQL("DELETE FROM harvest_processes WHERE id = ?", arrayOf(process))
        val imported = DataImportManager.importData(context, uri)
        assertTrue(imported.exceptionOrNull()?.message, imported.isSuccess)
        val db = room.openHelper.writableDatabase
        db.query("SELECT id FROM harvest_processes WHERE processUid = ?", arrayOf(processUid)).use { cursor ->
            assertTrue(cursor.moveToFirst())
            val newProcess = cursor.getInt(0)
            db.query("SELECT processId,harvestedAreaHa,bulkDensityKgM3,densityMode FROM harvest_days WHERE dayUid = ?", arrayOf(dayUid)).use { dayCursor ->
                assertTrue(dayCursor.moveToFirst())
                assertEquals(newProcess, dayCursor.getInt(0))
                assertEquals(2.375, dayCursor.getDouble(1), 0.0)
                assertEquals(555.5, dayCursor.getDouble(2), 0.0)
                assertEquals("CUSTOM", dayCursor.getString(3))
            }
        }
    }
    @Test fun invalidAreaBackupFailsBeforeReplacingExistingData() = runBlocking(Dispatchers.IO) {
        val (_, day) = fixture()
        val before = room.harvestDao().getDayById(day)
        val uri = PrivateTestBackups.createDestination(context)
        assertTrue(DatabaseBackupManager.exportBackup(context, uri).isSuccess)
        val json = context.contentResolver.openInputStream(uri)!!.use { JSONObject(it.bufferedReader().readText()) }
        val rows = json.getJSONObject("tables").getJSONArray("harvest_days")
        for (index in 0 until rows.length()) {
            if (rows.getJSONObject(index).getInt("id") == day) rows.getJSONObject(index).put("harvestedAreaHa", -1.0)
        }
        context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(json.toString()) }
        assertTrue(DatabaseBackupManager.importBackup(context, uri).isFailure)
        assertEquals(before, room.harvestDao().getDayById(day))
    }
}
