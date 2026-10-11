package ru.fo6osik.workjournal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

/** seedV20 runs against the pinned real v20 TEST APK; verifyV21 after an in-place update.
 * Seed code deliberately uses the stable DatabaseProvider API and SQL, no new model ABI.
 */
@RunWith(AndroidJUnit4::class)
class HarvestMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun checkTest() { check(context.packageName == "ru.fo6osik.workjournal.debugtest") }

    @Test fun seedV20() = runBlocking(Dispatchers.IO) {
        checkTest()
        val room = DatabaseProvider.getDatabase(context)
        val db = room.openHelper.writableDatabase
        assertEquals(20, db.version)
        db.execSQL("INSERT INTO vehicles (id,type,brand,model,number,year,note,engineId,engineName) VALUES (4101,'combine','synthetic','migration','MIGRATION',2020,'keep',NULL,'')")
        db.execSQL("""INSERT INTO harvest_processes
            (id,processUid,startDate,endDate,status,fieldId,fieldNameSnapshot,crop,combineVehicleId,combineNameSnapshot,bunkerVolumeM3,createdAt)
            VALUES (4201,'migration-process','2026-10-10','2026-10-10','COMPLETED',NULL,'synthetic','Пшеница',4101,'synthetic',8.0,101)""")
        db.execSQL("""INSERT INTO harvest_days
            (id,dayUid,processId,workDate,startTime,endTime,status,workIntervalMinutes,unloadIntervalMinutes,lunchIntervalMinutes,
             dinnerIntervalMinutes,waitingTransportIntervalMinutes,workIntervalCustom,unloadIntervalCustom,lunchIntervalCustom,
             dinnerIntervalCustom,waitingTransportIntervalCustom,reminderSettingsInitialized,createdAt)
            VALUES (4301,'migration-day',4201,'2026-10-10','08:00','18:00','COMPLETED',47,9,22,23,41,1,1,1,1,1,1,102)""")
        db.execSQL("INSERT INTO harvest_bunkers (id,bunkerUid,dayId,fillTime,fillLevel,postEventType,createdAt) VALUES (4401,'migration-bunker',4301,'09:00','MORE_THAN_HALF','WAITING_TRANSPORT',103)")
        assertTrue(context.getSharedPreferences("harvest_reminder_prefs", 0).edit()
            .putBoolean("enabled_4301", false).putInt("interval_4301", 47).commit())
        val uri = PrivateTestBackups.createDestination(context)
        assertTrue(DatabaseBackupManager.exportBackup(context, uri).isSuccess)
        assertTrue(context.getSharedPreferences("harvest-migration", 0).edit()
            .putString("legacyBackup", uri.toString()).commit())
        room.close()
    }

    @Test fun verifyV21AndRestoreLegacyBackup() = runBlocking(Dispatchers.IO) {
        checkTest()
        val room = DatabaseProvider.getDatabase(context)
        val db = room.openHelper.writableDatabase // Room validates the entire migrated schema here.
        assertEquals(21, db.version)
        fun verify() {
            db.query("SELECT * FROM harvest_days WHERE id=4301").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("harvestedAreaHa")))
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
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("bulkDensityKgM3")))
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
            db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            db.query("SELECT number FROM vehicles WHERE id=4101").use { assertTrue(it.moveToFirst()); assertEquals("MIGRATION", it.getString(0)) }
        }
        verify()
        val prefs = context.getSharedPreferences("harvest_reminder_prefs", 0)
        assertFalse(prefs.getBoolean("enabled_4301", true))
        assertEquals(47, prefs.getInt("interval_4301", 0))
        val uri = android.net.Uri.parse(context.getSharedPreferences("harvest-migration", 0).getString("legacyBackup", null)!!)
        val legacy = context.contentResolver.openInputStream(uri)!!.use {
            JSONObject(it.bufferedReader().readText())
        }
        assertEquals(20, legacy.getInt("databaseVersion"))
        val migratedUri = PrivateTestBackups.createDestination(context)
        assertTrue(DatabaseBackupManager.exportBackup(context, migratedUri).isSuccess)
        val migrated = context.contentResolver.openInputStream(migratedUri)!!.use {
            JSONObject(it.bufferedReader().readText())
        }
        val legacyTables = legacy.getJSONObject("tables")
        val newTables = migrated.getJSONObject("tables")
        assertEquals(legacyTables.keys().asSequence().toSet(), newTables.keys().asSequence().toSet())
        // Every stored value in every table survives; not just the harvest fixture.
        for (table in legacyTables.keys()) {
            val oldRows = legacyTables.getJSONArray(table)
            val newRows = newTables.getJSONArray(table)
            assertEquals(table, oldRows.length(), newRows.length())
            for (index in 0 until oldRows.length()) {
                val oldRow = oldRows.getJSONObject(index)
                val newRow = newRows.getJSONObject(index)
                for (column in oldRow.keys()) {
                    assertEquals("$table/$index/$column", oldRow.get(column).toString(), newRow.get(column).toString())
                }
            }
        }
        assertTrue(DatabaseBackupManager.importBackup(context, uri).isSuccess)
        verify()
        room.close()
    }
}
