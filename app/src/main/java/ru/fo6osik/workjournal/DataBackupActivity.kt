package ru.fo6osik.workjournal

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DataBackupActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_data_backup
        )

        findViewById<Button>(
            R.id.buttonImportData
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Импорт подключим следующим шагом",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<Button>(
            R.id.buttonExportData
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Экспорт подключим следующим шагом",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}