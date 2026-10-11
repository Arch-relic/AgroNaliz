package ru.fo6osik.workjournal

import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Runs only on the disposable CI emulator; never copies production user data. */
@RunWith(AndroidJUnit4::class)
class BuildVariantIsolationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val mainId = "ru.fo6osik.workjournal"
    private val marker get() = if (context.packageName == mainId) "MAIN-synthetic" else "TEST-synthetic"
    private fun files() = listOf(
        File(context.filesDir, "isolation-probe"),
        File(context.cacheDir, "isolation-probe"),
        File(context.noBackupFilesDir, "documents/isolation-probe"),
        File(context.getExternalFilesDir(null)!!, "isolation-probe")
    )

    @Test fun seedAndCheckSandbox() = runBlocking(Dispatchers.IO) {
        assertEquals(BuildConfig.APPLICATION_ID, context.packageName)
        val other = if (context.packageName == mainId) BackupStoragePolicy.TEST_APPLICATION_ID else mainId
        val otherInfo = context.packageManager.getApplicationInfo(other, 0)
        assertNotEquals(context.applicationInfo.uid, otherInfo.uid)
        assertNotEquals(context.applicationInfo.dataDir, otherInfo.dataDir)
        files().forEach { file ->
            assertFalse(file.exists())
            file.parentFile!!.mkdirs()
            file.writeText(marker)
            assertEquals(marker, file.readText())
        }
        val prefs = context.getSharedPreferences("isolation-probe", 0)
        assertFalse(prefs.contains("marker"))
        assertTrue(prefs.edit().putString("marker", marker).commit())
        val db = DatabaseProvider.getDatabase(context)
        assertTrue(db.vehicleDao().getAll().isEmpty())
        db.vehicleDao().insert(Vehicle(type="probe", brand=marker, model="synthetic", number=marker,
            year=null, note="Synthetic isolation test"))
        assertEquals(marker, db.vehicleDao().getAll().single().number)
        assertTrue(context.getDatabasePath("agrouchet_database").canonicalPath.startsWith(context.applicationInfo.dataDir))
        db.close()
    }

    @Test fun testBackupProviderAndExternalWriteGuard() = runBlocking(Dispatchers.IO) {
        if (!BackupStoragePolicy.isTest(context.packageName)) return@runBlocking
        assertEquals("АгроНализ TEST", context.getString(R.string.app_name))
        val uri = PrivateTestBackups.createDestination(context)
        assertEquals("${context.packageName}.testbackups", uri.authority)
        context.contentResolver.openOutputStream(uri)!!.use { it.write("synthetic backup".toByteArray()) }
        context.contentResolver.openInputStream(uri)!!.use { assertEquals("synthetic backup", it.bufferedReader().readText()) }
        try {
            FileProvider.getUriForFile(context, uri.authority!!, File(context.filesDir, "outside.json"))
            fail("Provider exposed files outside test-backups")
        } catch (_: IllegalArgumentException) { }
        val unsafe = Uri.parse("content://com.android.externalstorage.documents/document/synthetic")
        assertTrue(DatabaseBackupManager.exportBackup(context, unsafe).isFailure)
        assertTrue(DatabaseBackupManager.exportBackup(context, uri).isSuccess)
        context.contentResolver.openInputStream(uri)!!.use {
            val json = it.bufferedReader().readText()
            assertTrue(json.contains("TEST-synthetic"))
            assertFalse(json.contains("MAIN-synthetic"))
        }
        try {
            BackupStoragePolicy.requireSafeWrite(context.packageName, unsafe.scheme, unsafe.authority)
            fail("Shared backup accepted")
        } catch (_: IllegalArgumentException) { }
        assertFalse(context.packageManager.getProviderInfo(
            android.content.ComponentName(context, FileProvider::class.java), 0).exported)
        val main = context.packageManager.getApplicationInfo(mainId, 0)
        try {
            File(main.dataDir, "files/isolation-probe").readText()
            fail("TEST read the main sandbox")
        } catch (_: java.io.IOException) { }
    }

    @Test fun verifyMainAfterTest() = runBlocking(Dispatchers.IO) {
        assertEquals(mainId, context.packageName)
        files().forEach { assertEquals("MAIN-synthetic", it.readText()) }
        assertEquals("MAIN-synthetic", context.getSharedPreferences("isolation-probe", 0).getString("marker", null))
        val db = DatabaseProvider.getDatabase(context)
        assertEquals("MAIN-synthetic", db.vehicleDao().getAll().single().number)
        db.close()
        assertFalse(File(context.filesDir, "test-backups").exists())
    }
}
