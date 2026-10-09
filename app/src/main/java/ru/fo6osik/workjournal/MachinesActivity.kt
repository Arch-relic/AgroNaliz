package ru.fo6osik.workjournal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MachinesActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_machines
        )

        findViewById<Button>(
            R.id.buttonTractors
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    VehiclesActivity::class.java
                )
            )
        }

        findViewById<Button>(
            R.id.buttonImplements
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AggregatesActivity::class.java
                )
            )
        }

        findViewById<Button>(
            R.id.buttonMaintenance
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    MaintenanceActivity::class.java
                )
            )
        }
    }
}
