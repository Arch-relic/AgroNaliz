package ru.fo6osik.workjournal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class VehiclesActivity : AppCompatActivity() {

    private lateinit var vehiclesContainer: LinearLayout
    private lateinit var textEmptyVehicles: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicles)

        vehiclesContainer =
            findViewById(R.id.vehiclesContainer)

        textEmptyVehicles =
            findViewById(R.id.textEmptyVehicles)

        val buttonAddVehicle =
            findViewById<Button>(R.id.buttonAddVehicle)

        buttonAddVehicle.setOnClickListener {
            val intent = Intent(this, AddVehicleActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadVehicles()
    }

    private fun loadVehicles() {

        lifecycleScope.launch {

            val vehicles = DatabaseProvider
                .getDatabase(applicationContext)
                .vehicleDao()
                .getAll()

            vehiclesContainer.removeAllViews()

            if (vehicles.isEmpty()) {

                textEmptyVehicles.visibility = View.VISIBLE

            } else {

                textEmptyVehicles.visibility = View.GONE

                vehicles.forEach { vehicle ->

                    val vehicleName = TextView(this@VehiclesActivity)

                    vehicleName.text =
                        "${vehicle.brand} ${vehicle.model}"

                    vehicleName.textSize = 20f

                    vehicleName.setPadding(
                        16,
                        28,
                        16,
                        28
                    )

                    vehicleName.setOnClickListener {

                        val intent = Intent(
                            this@VehiclesActivity,
                            VehicleDetailsActivity::class.java
                        )

                        intent.putExtra(
                            "vehicle_id",
                            vehicle.id
                        )

                        startActivity(intent)
                    }

                    vehiclesContainer.addView(vehicleName)
                }
            }
        }
    }
}