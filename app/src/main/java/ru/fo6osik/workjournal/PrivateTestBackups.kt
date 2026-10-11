package ru.fo6osik.workjournal

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.UUID

internal object PrivateTestBackups {
    fun createDestination(context: Context): Uri {
        check(BackupStoragePolicy.isTest(context.packageName))
        val directory = File(context.filesDir, "test-backups")
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Cannot create private TEST backup directory")
        }
        val file = File(directory, "Agronaliz_TEST_backup_${UUID.randomUUID()}.json")
        return FileProvider.getUriForFile(context, "${context.packageName}.testbackups", file)
    }
}
