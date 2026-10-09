package ru.fo6osik.workjournal

import android.content.Intent
import android.os.Bundle
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

        findViewById<Button>(
            R.id.buttonDataBackup
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    DataBackupActivity::class.java
                )
            )
        }
    }
}