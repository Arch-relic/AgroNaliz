package ru.fo6osik.workjournal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WorkRecordsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_records)

        findViewById<Button>(R.id.buttonEventJournal).setOnClickListener {
            startActivity(Intent(this, EventJournalActivity::class.java))
        }

        findViewById<Button>(R.id.buttonSeasonWorks).setOnClickListener {
            startActivity(Intent(this, SeasonWorksActivity::class.java))
        }

        findViewById<Button>(R.id.buttonHarvest).setOnClickListener {
            startActivity(Intent(this, HarvestActivity::class.java))
        }
    }
}
