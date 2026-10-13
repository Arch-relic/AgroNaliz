package ru.fo6osik.workjournal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Used only by the disposable-emulator fixture sequence in run-test-signing-checks.py. */
@RunWith(AndroidJUnit4::class)
class TestSigningUpdateTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get()=context.getSharedPreferences("synthetic-signing-update",0)
    private val room get()=DatabaseProvider.getDatabase(context)
    private fun guard() { check(context.packageName=="ru.fo6osik.workjournal.debugtest") }
    @Suppress("DEPRECATION")
    private fun version()=context.packageManager.getPackageInfo(context.packageName,0).versionCode
    @Suppress("DEPRECATION")
    private fun certificate():String {
        val bytes=context.packageManager.getPackageInfo(context.packageName,android.content.pm.PackageManager.GET_SIGNATURES).signatures!!.single().toByteArray()
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    private fun backup()=android.net.Uri.parse(prefs.getString("backup",null)!!)
    private fun originalTables():JSONObject = context.contentResolver.openInputStream(backup())!!.use {
        JSONObject(it.bufferedReader().readText()).getJSONObject("tables")
    }
    private fun verifyData() {
        guard()
        val day=room.harvestDao().getDayById(prefs.getInt("day",-1))!!
        assertEquals(47,day.workIntervalMinutes);assertTrue(day.workIntervalCustom)
        assertEquals("MEAL",room.harvestDao().getBunkersForDay(day.id).single().postEventType)
        assertEquals("MORE_THAN_HALF",room.harvestDao().getBunkersForDay(day.id).single().fillLevel)
        assertEquals("synthetic preserved file",File(context.filesDir,"signing-update-marker").readText())
        assertEquals("synthetic preserved preference",prefs.getString("marker",null))
        assertEquals(prefs.getString("certificate",null),certificate())
        assertTrue(version()>prefs.getInt("firstVersion",-1))
        val db=room.openHelper.readableDatabase
        db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        val tables=originalTables()
        for(table in tables.keys()) {
            val rows=tables.getJSONArray(table)
            db.query("SELECT COUNT(*) FROM `$table`").use { assertTrue(it.moveToFirst());assertEquals(table,rows.length(),it.getInt(0)) }
            for(index in 0 until rows.length()) {
                val row=rows.getJSONObject(index)
                val id=row.getLong("id")
                db.query("SELECT * FROM `$table` WHERE id = ?",arrayOf(id)).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    for(column in row.keys()) {
                        val pos=cursor.getColumnIndexOrThrow(column)
                        if(row.isNull(column)) assertTrue(cursor.isNull(pos))
                        else when(cursor.getType(pos)) {
                            android.database.Cursor.FIELD_TYPE_INTEGER -> assertEquals("$table/$column",row.getLong(column),cursor.getLong(pos))
                            android.database.Cursor.FIELD_TYPE_FLOAT -> assertEquals("$table/$column",row.getDouble(column),cursor.getDouble(pos),0.0)
                            else -> assertEquals("$table/$column",row.get(column).toString(),cursor.getString(pos))
                        }
                    }
                }
            }
        }
    }
    @Test fun seedDataAndExport() = runBlocking(Dispatchers.IO) {
        guard()
        val dao=room.harvestDao()
        val process=dao.insertProcess(HarvestProcess(processUid=UUID.randomUUID().toString(),startDate="2026-10-10",
            fieldId=null,fieldNameSnapshot="synthetic signing",crop="Пшеница",combineVehicleId=null,
            combineNameSnapshot="synthetic",bunkerVolumeM3=8.0)).toInt()
        val day=dao.insertDay(HarvestDay(dayUid=UUID.randomUUID().toString(),processId=process,
            workDate="2026-10-10",startTime="08:00",workIntervalMinutes=47,workIntervalCustom=true)).toInt()
        dao.insertBunker(HarvestBunker(bunkerUid=UUID.randomUUID().toString(),dayId=day,fillTime="09:00",
            fillLevel="MORE_THAN_HALF",postEventType="MEAL"))
        val db=room.openHelper.writableDatabase
        db.query("PRAGMA table_info(harvest_days)").use { cursor ->
            val columns=mutableSetOf<String>();while(cursor.moveToNext())columns.add(cursor.getString(1))
            if("harvestedAreaHa" in columns)db.execSQL("UPDATE harvest_days SET harvestedAreaHa=3.125,bulkDensityKgM3=555.125,densityMode='CUSTOM' WHERE id=?",arrayOf(day))
        }
        File(context.filesDir,"signing-update-marker").writeText("synthetic preserved file")
        val uri=PrivateTestBackups.createDestination(context)
        assertTrue(DatabaseBackupManager.exportBackup(context,uri).isSuccess)
        assertTrue(prefs.edit().putInt("day",day).putInt("firstVersion",version()).putString("certificate",certificate())
            .putString("marker","synthetic preserved preference").putString("backup",uri.toString()).commit())
    }
    @Test fun verifyAfterSameKeyUpdate() = runBlocking(Dispatchers.IO) { verifyData() }
    @Test fun verifyAfterRejectedDifferentKeyUpdate() = runBlocking(Dispatchers.IO) { verifyData() }
    @Test fun verifySyntheticBackupRestore() = runBlocking(Dispatchers.IO) {
        guard()
        val id=prefs.getInt("day",-1)
        // Only the explicitly seeded synthetic TEST row; never touches main/user data.
        room.openHelper.writableDatabase.execSQL("DELETE FROM harvest_days WHERE id=?",arrayOf(id))
        assertNull(room.harvestDao().getDayById(id))
        assertTrue(DatabaseBackupManager.importBackup(context,backup()).isSuccess)
        verifyData()
    }
}
