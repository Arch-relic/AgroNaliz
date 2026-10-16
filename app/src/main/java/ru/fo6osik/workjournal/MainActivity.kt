package ru.fo6osik.workjournal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.content.Intent

class MainActivity : AppCompatActivity() {
    private var quickAccessGeneration = 0

    override fun onResume() {
        super.onResume()
        refreshHarvest(false)
    }

    override fun onPause() {
        quickAccessGeneration++
        super.onPause()
    }

    private fun refreshHarvest(open: Boolean) {
        val generation = ++quickAccessGeneration
        val button = findViewById<Button>(R.id.buttonContinueHarvest)
        button.isEnabled = false
        Thread {
            try {
                val campaigns = DatabaseProvider.getDatabase(applicationContext).harvestDao().getActiveProcesses()
                runOnUiThread {
                    if (generation != quickAccessGeneration || isFinishing || isDestroyed) return@runOnUiThread
                    button.visibility = if (campaigns.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                    button.isEnabled = true
                    if (open && campaigns.size == 1) openHarvest(campaigns.single().id)
                    else if (open && campaigns.size > 1) androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Выберите уборочную кампанию")
                        .setItems(campaigns.map { "${it.fieldNameSnapshot} — ${it.crop} · ${it.startDate} · ${it.combineNameSnapshot} · №${it.id}" }.toTypedArray()) { _, index ->
                            openHarvest(campaigns[index].id)
                        }.setNegativeButton("Отмена", null).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (generation != quickAccessGeneration) return@runOnUiThread
                    button.isEnabled = true
                    android.widget.Toast.makeText(this, "Ошибка загрузки уборки: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun openHarvest(processId: Int) {
        val generation = quickAccessGeneration
        findViewById<Button>(R.id.buttonContinueHarvest).isEnabled = false
        Thread {
            try {
                val target = HarvestQuickAccess.resolve(DatabaseProvider.getDatabase(applicationContext), processId)
                runOnUiThread {
                    if (generation != quickAccessGeneration || isFinishing || isDestroyed) return@runOnUiThread
                    findViewById<Button>(R.id.buttonContinueHarvest).isEnabled = true
                    if (target == null) {
                        android.widget.Toast.makeText(this, "Кампания завершена или удалена", android.widget.Toast.LENGTH_SHORT).show()
                        refreshHarvest(false)
                    } else if (target.dayId != null) startActivity(Intent(this, HarvestDayActivity::class.java)
                        .putExtra(HarvestDayActivity.EXTRA_DAY_ID, target.dayId))
                    else startActivity(Intent(this, HarvestProcessActivity::class.java)
                        .putExtra(HarvestProcessActivity.EXTRA_PROCESS_ID, target.processId))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (generation != quickAccessGeneration || isFinishing || isDestroyed) return@runOnUiThread
                    findViewById<Button>(R.id.buttonContinueHarvest).isEnabled = true
                    android.widget.Toast.makeText(this, e.message, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    override fun 
	onCreate(savedInstanceState: Bundle?)
	 {
        super.onCreate(savedInstanceState)
		
        setContentView(
		   R.layout.activity_main
		   )

        findViewById<Button>(R.id.buttonContinueHarvest).setOnClickListener { refreshHarvest(true) }

        findViewById<TextView>(R.id.textAppVersion).text = getString(
            if (BuildConfig.BUILD_TYPE == "debugTest") R.string.app_version_test else R.string.app_version,
            BuildConfig.VERSION_NAME
        )
		   
		val buttonMachines = 
		    findViewById<Button>(
			    R.id.buttonMachines
			)
			
    buttonMachines.setOnClickListener {
	
    val intent = 
	    Intent(
		   this,
		   
MachinesActivity::class.java
            )
			
    startActivity(intent)
}

    val buttonDeveloperCatalog =
    findViewById<Button>(
        R.id.buttonDeveloperCatalog
    )
	
	val buttonFields =
    findViewById<Button>(
        R.id.buttonFields
    )

buttonFields.setOnClickListener {

    val intent =
        Intent(
            this,
            FieldsActivity::class.java
        )

    startActivity(intent)
}

buttonDeveloperCatalog.setOnClickListener {

    val intent =
        Intent(
            this,
            DeveloperCatalogActivity::class.java
        )

    startActivity(intent)
}

val buttonWorkNotes =
    findViewById<Button>(
        R.id.buttonWorkNotes
    )

buttonWorkNotes.setOnClickListener {

    val intent =
        Intent(
            this,
            WorkRecordsActivity::class.java
        )

    startActivity(intent)
}

val buttonOptions =
    findViewById<Button>(
        R.id.buttonOptions
    )

buttonOptions.setOnClickListener {

    val intent =
        Intent(
            this,
            OptionsActivity::class.java
        )

    startActivity(intent)
}

findViewById<Button>(
    R.id.buttonBackupRestore
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

