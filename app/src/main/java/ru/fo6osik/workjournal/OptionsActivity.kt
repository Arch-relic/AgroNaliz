package ru.fo6osik.workjournal

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OptionsActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_options
        )

        findViewById<TextView>(R.id.textAppVersion).text = getString(
            if (BuildConfig.BUILD_TYPE == "debugTest") R.string.app_version_test else R.string.app_version,
            BuildConfig.VERSION_NAME
        )
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