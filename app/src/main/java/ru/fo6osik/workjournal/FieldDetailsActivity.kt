package ru.fo6osik.workjournal

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class FieldDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FIELD_ID = "extra_field_id"
    }

    private lateinit var textFieldName: TextView
    private lateinit var textFieldArea: TextView
    private lateinit var textFieldLocation: TextView
    private lateinit var textFieldNote: TextView
    private lateinit var textFieldHistoryEmpty: TextView
    private lateinit var yearsContainer: LinearLayout
    private lateinit var buttonAllHistory: Button

    private var fieldId: Int = 0
    private var currentField: FieldEntity? = null

    private val database by lazy {
        DatabaseProvider.getDatabase(applicationContext)
    }

    private val fieldDao by lazy {
        database.fieldDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_field_details)

        fieldId = intent.getIntExtra(EXTRA_FIELD_ID, 0)

        if (fieldId <= 0) {
            finish()
            return
        }

        textFieldName = findViewById(R.id.textFieldName)
        textFieldArea = findViewById(R.id.textFieldArea)
        textFieldLocation = findViewById(R.id.textFieldLocation)
        textFieldNote = findViewById(R.id.textFieldNote)
        textFieldHistoryEmpty = findViewById(R.id.textFieldHistoryEmpty)
        yearsContainer = findViewById(R.id.yearsContainer)
        buttonAllHistory = findViewById(R.id.buttonAllHistory)

        findViewById<Button>(
            R.id.buttonEditField
        ).setOnClickListener {
            currentField?.let {
                showEditDialog(it)
            }
        }

        buttonAllHistory.setOnClickListener {
            val intent =
                Intent(
                    this,
                    FieldHistoryActivity::class.java
                )

            intent.putExtra(
                FieldHistoryActivity.EXTRA_FIELD_ID,
                fieldId
            )

            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadField()
    }

    private fun loadField() {
        lifecycleScope.launch {

            val field =
                fieldDao.getById(fieldId)

            if (field == null) {
                finish()
                return@launch
            }

            currentField = field

            textFieldName.text = field.name

            textFieldArea.text =
                field.areaHa?.let {
                    "Площадь: ${formatArea(it)} га"
                } ?: "Площадь: не указана"

            textFieldLocation.text =
                if (field.location.isNullOrBlank()) {
                    "Местоположение: не указано"
                } else {
                    "Местоположение: ${field.location}"
                }

            textFieldNote.text =
                if (field.note.isNullOrBlank()) {
                    "Примечание: нет"
                } else {
                    "Примечание: ${field.note}"
                }

            loadYears()
        }
    }

    private suspend fun loadYears() {

        val years =
            database
                .seasonWorkDao()
                .getYearsByField(fieldId)

        yearsContainer.removeAllViews()

        val hasYears =
            years.isNotEmpty()

        textFieldHistoryEmpty.visibility =
            if (hasYears) {
                View.GONE
            } else {
                View.VISIBLE
            }

        buttonAllHistory.isEnabled =
            hasYears

        if (!hasYears) {
            return
        }

        val density =
            resources.displayMetrics.density

        years.forEach { year ->

            val button =
                Button(this).apply {

                    text = year.toString()

                    setOnClickListener {

                        val intent =
                            Intent(
                                this@FieldDetailsActivity,
                                FieldHistoryActivity::class.java
                            )

                        intent.putExtra(
                            FieldHistoryActivity.EXTRA_FIELD_ID,
                            fieldId
                        )

                        intent.putExtra(
                            FieldHistoryActivity.EXTRA_YEAR,
                            year
                        )

                        startActivity(intent)
                    }
                }

            yearsContainer.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin =
                        (8 * density).toInt()
                }
            )
        }
    }

    private fun showEditDialog(
        field: FieldEntity
    ) {

        val density =
            resources.displayMetrics.density

        val container =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    (20 * density).toInt(),
                    0,
                    (20 * density).toInt(),
                    0
                )
            }

        val editName =
            EditText(this).apply {
                hint = "Название поля"
                setText(field.name)
            }

        val editArea =
            EditText(this).apply {
                hint = "Площадь, га"
                inputType =
                    InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(
                    field.areaHa
                        ?.let { formatArea(it) }
                        .orEmpty()
                )
            }

        val editLocation =
            EditText(this).apply {
                hint = "Местоположение / описание"
                setText(field.location.orEmpty())
            }

        val editNote =
            EditText(this).apply {
                hint = "Примечание"
                minLines = 2
                gravity = Gravity.TOP
                setText(field.note.orEmpty())
            }

        container.addView(editName)
        container.addView(editArea)
        container.addView(editLocation)
        container.addView(editNote)

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Редактирование поля")
                .setView(container)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null)
                .create()

        dialog.setOnShowListener {

            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val name =
                        editName.text
                            .toString()
                            .trim()

                    if (name.isBlank()) {
                        editName.error =
                            "Введите название поля"
                        return@setOnClickListener
                    }

                    val areaText =
                        editArea.text
                            .toString()
                            .trim()
                            .replace(',', '.')

                    val area =
                        if (areaText.isBlank()) {
                            null
                        } else {
                            areaText.toDoubleOrNull()
                        }

                    if (
                        areaText.isNotBlank() &&
                        area == null
                    ) {
                        editArea.error =
                            "Проверьте площадь"
                        return@setOnClickListener
                    }

                    if (
                        area != null &&
                        area <= 0.0
                    ) {
                        editArea.error =
                            "Площадь должна быть больше нуля"
                        return@setOnClickListener
                    }

                    val location =
                        editLocation.text
                            .toString()
                            .trim()
                            .ifBlank { null }

                    val note =
                        editNote.text
                            .toString()
                            .trim()
                            .ifBlank { null }

                    lifecycleScope.launch {
                        fieldDao.update(
                            field.copy(
                                name = name,
                                areaHa = area,
                                location = location,
                                note = note
                            )
                        )

                        dialog.dismiss()
                        loadField()
                    }
                }
        }

        dialog.show()
    }

    private fun formatArea(
        area: Double
    ): String {

        return if (
            area % 1.0 == 0.0
        ) {
            area.toInt().toString()
        } else {
            area.toString().replace('.', ',')
        }
    }
}
