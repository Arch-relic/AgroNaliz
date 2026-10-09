package ru.fo6osik.workjournal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.content.Intent

class MainActivity : AppCompatActivity() {
    override fun 
	onCreate(savedInstanceState: Bundle?)
	 {
        super.onCreate(savedInstanceState)
		
        setContentView(
		   R.layout.activity_main
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

