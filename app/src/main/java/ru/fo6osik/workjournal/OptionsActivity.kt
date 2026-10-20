package ru.fo6osik.workjournal

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OptionsActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_options
        )

        val installed = InstalledAppDiagnostics.read(this)
        val unknown = getString(R.string.app_diagnostic_unknown)
        findViewById<TextView>(R.id.textAppVersion).text = getString(
            if (BuildConfig.BUILD_TYPE == "debugTest") R.string.app_version_test else R.string.app_version,
            installed.versionName ?: BuildConfig.VERSION_NAME
        )
        findViewById<TextView>(R.id.textAppVersionCode).text = getString(R.string.app_version_code, installed.versionCode?.toString() ?: unknown)
        findViewById<TextView>(R.id.textRoomSchemaVersion).text = getString(R.string.app_room_schema, BuildConfig.ROOM_SCHEMA_VERSION)
        findViewById<TextView>(R.id.textSigningCertificate).text = installed.certificateSha256.joinToString("\n").ifEmpty { unknown }
        findViewById<Button>(R.id.buttonCopySigningCertificate).apply {
            isEnabled = installed.certificateSha256.isNotEmpty()
            setOnClickListener {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText(getString(R.string.app_signing_certificate), installed.certificateSha256.joinToString("\n")))
                Toast.makeText(this@OptionsActivity, R.string.app_certificate_copied, Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.buttonChangelog).setOnClickListener {
            startActivity(Intent(this, ChangelogActivity::class.java))
        }
        findViewById<Button>(R.id.buttonDeveloperCatalog).setOnClickListener {
            startActivity(Intent(this, DeveloperCatalogActivity::class.java))
        }

        findViewById<Button>(
            R.id.buttonDataBackup
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    BackupRestoreActivity::class.java
                )
            )
        }
    }
}
