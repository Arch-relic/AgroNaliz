package ru.fo6osik.workjournal

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.Intent

class FieldsActivity : AppCompatActivity() {

    private lateinit var fieldsContainer: LinearLayout
    private lateinit var textEmptyFields: TextView

    private val database by lazy {
        DatabaseProvider.getDatabase(
            applicationContext
        )
    }

    private val fieldDao by lazy {
        database.fieldDao()
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_fields
        )

        fieldsContainer =
            findViewById(
                R.id.fieldsContainer
            )

        textEmptyFields =
            findViewById(
                R.id.textEmptyFields
            )

        findViewById<Button>(
            R.id.buttonAddField
        ).setOnClickListener {
            showFieldDialog()
        }

        loadFields()
    }

    override fun onResume() {
        super.onResume()
        loadFields()
    }

    private fun loadFields() {

        lifecycleScope.launch {

            val fields =
                fieldDao.getActiveFields()

            fieldsContainer.removeAllViews()

            textEmptyFields.visibility =
                if (fields.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            fields.forEach { field ->
                addFieldCard(field)
            }
        }
    }

    private fun addFieldCard(
        field: FieldEntity
    ) {

        val density =
            resources.displayMetrics.density

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    (16 * density).toInt(),
                    (14 * density).toInt(),
                    (16 * density).toInt(),
                    (14 * density).toInt()
                )

                val backgroundDrawable =
                    GradientDrawable().apply {

                        cornerRadius =
                            18 * density

                        setColor(
                            Color.rgb(
                                35,
                                35,
                                35
                            )
                        )

                        setStroke(
                            (1 * density).toInt(),
                            Color.rgb(
                                80,
                                80,
                                80
                            )
                        )
                    }

                background =
                    backgroundDrawable

                isClickable = true
                isFocusable = true
            }

        val title =
            TextView(this).apply {

                text = field.name

                textSize = 20f

                setTextColor(
                    Color.WHITE
                )
            }

        card.addView(title)

        field.areaHa?.let { area ->

            val areaText =
                TextView(this).apply {

                    text =
                        "Площадь: " +
                        formatArea(area) +
                        " га"

                    textSize = 16f

                    setTextColor(
                        Color.LTGRAY
                    )

                    setPadding(
                        0,
                        (6 * density).toInt(),
                        0,
                        0
                    )
                }

            card.addView(areaText)
        }

        if (
            !field.location.isNullOrBlank()
        ) {

            val locationText =
                TextView(this).apply {

                    text =
                        "Местоположение: " +
                        field.location

                    textSize = 15f

                    setTextColor(
                        Color.LTGRAY
                    )

                    setPadding(
                        0,
                        (4 * density).toInt(),
                        0,
                        0
                    )
                }

            card.addView(
                locationText
            )
        }

        val deleteButton =
            Button(this).apply {

                text =
                    "Удалить поле"

                setOnClickListener {
                    showDeleteFieldDialog(
                        field
                    )
                }
            }

        card.addView(
            deleteButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    (10 * density).toInt()
            }
        )

        card.setOnClickListener {

    val intent =
        Intent(
            this,
            FieldDetailsActivity::class.java
        )

    intent.putExtra(
        FieldDetailsActivity.EXTRA_FIELD_ID,
        field.id
    )

    startActivity(intent)
}

        card.setOnLongClickListener {
            showArchiveDialog(field)
            true
        }

        fieldsContainer.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                bottomMargin =
                    (12 * density).toInt()
            }
        )
    }

    private fun showFieldDialog(
        existingField: FieldEntity? = null
    ) {

        val density =
            resources.displayMetrics.density

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

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

                setText(
                    existingField?.name
                        .orEmpty()
                )
            }

        val editArea =
            EditText(this).apply {

                hint = "Площадь, га"

                inputType =
                    InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL

                setText(
                    existingField
                        ?.areaHa
                        ?.let {
                            formatArea(it)
                        }
                        .orEmpty()
                )
            }

        val editLocation =
            EditText(this).apply {

                hint =
                    "Местоположение / описание"

                setText(
                    existingField
                        ?.location
                        .orEmpty()
                )
            }

        val editNote =
            EditText(this).apply {

                hint = "Примечание"

                minLines = 2

                gravity =
                    Gravity.TOP

                setText(
                    existingField
                        ?.note
                        .orEmpty()
                )
            }

        container.addView(editName)
        container.addView(editArea)
        container.addView(editLocation)
        container.addView(editNote)

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    if (
                        existingField == null
                    ) {
                        "Новое поле"
                    } else {
                        "Редактирование поля"
                    }
                )
                .setView(container)
                .setNegativeButton(
                    "Отмена",
                    null
                )
                .setPositiveButton(
                    "Сохранить",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog
                .getButton(
                    AlertDialog.BUTTON_POSITIVE
                )
                .setOnClickListener {

                    val name =
                        editName
                            .text
                            .toString()
                            .trim()

                    if (name.isBlank()) {

                        editName.error =
                            "Введите название поля"

                        return@setOnClickListener
                    }

                    val areaText =
                        editArea
                            .text
                            .toString()
                            .trim()
                            .replace(
                                ',',
                                '.'
                            )

                    val area =
                        if (
                            areaText.isBlank()
                        ) {
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
                        editLocation
                            .text
                            .toString()
                            .trim()
                            .ifBlank {
                                null
                            }

                    val note =
                        editNote
                            .text
                            .toString()
                            .trim()
                            .ifBlank {
                                null
                            }

                    lifecycleScope.launch {

                        if (
                            existingField == null
                        ) {

                            fieldDao.insert(
                                FieldEntity(
                                    name = name,
                                    areaHa = area,
                                    location = location,
                                    note = note
                                )
                            )

                        } else {

                            fieldDao.update(
                                existingField.copy(
                                    name = name,
                                    areaHa = area,
                                    location = location,
                                    note = note
                                )
                            )
                        }

                        dialog.dismiss()

                        loadFields()
                    }
                }
        }

        dialog.show()
    }

    private fun showDeleteFieldDialog(
        field: FieldEntity
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Удалить поле?"
            )
            .setMessage(
                "Поле «${field.name}» будет удалено из базы. " +
                    "Удаление возможно только если с ним не осталось " +
                    "связанных сезонных записей."
            )
            .setNegativeButton(
                "Отмена",
                null
            )
            .setPositiveButton(
                "Удалить"
            ) { _, _ ->

                lifecycleScope.launch {

                    val linkedWorks =
                        fieldDao.countLinkedSeasonWorks(
                            field.id
                        )

                    val linkedEvents =
                        fieldDao.countLinkedFarmEvents(
                            field.id
                        )

                    if (
                        linkedWorks > 0 ||
                        linkedEvents > 0
                    ) {

                        AlertDialog.Builder(
                            this@FieldsActivity
                        )
                            .setTitle(
                                "Поле используется"
                            )
                            .setMessage(
                                "Нельзя удалить поле «${field.name}». " +
                                    "Связанных сезонных записей: " +
                                    linkedWorks +
                                    ", событий общего журнала: " +
                                    linkedEvents +
                                    ". Сначала удалите связанные записи."
                            )
                            .setPositiveButton(
                                "ОК",
                                null
                            )
                            .show()

                        return@launch
                    }

                    try {

                        fieldDao.deleteById(
                            field.id
                        )

                        Toast.makeText(
                            this@FieldsActivity,
                            "Поле удалено",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadFields()

                    } catch (
                        error: Exception
                    ) {

                        AlertDialog.Builder(
                            this@FieldsActivity
                        )
                            .setTitle(
                                "Не удалось удалить поле"
                            )
                            .setMessage(
                                error.message
                                    ?: "Поле связано с другими данными."
                            )
                            .setPositiveButton(
                                "ОК",
                                null
                            )
                            .show()
                    }
                }
            }
            .show()
    }

    private fun showArchiveDialog(
        field: FieldEntity
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Убрать поле из списка?"
            )
            .setMessage(
                "Поле «${field.name}» будет перенесено в архив. " +
                "Его ID и связь с будущими рабочими записями сохранятся."
            )
            .setNegativeButton(
                "Отмена",
                null
            )
            .setPositiveButton(
                "В архив"
            ) { _, _ ->

                lifecycleScope.launch {

                    fieldDao.archive(
                        field.id
                    )

                    Toast.makeText(
                        this@FieldsActivity,
                        "Поле перенесено в архив",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadFields()
                }
            }
            .show()
    }

    private fun formatArea(
        area: Double
    ): String {

        return if (
            area % 1.0 == 0.0
        ) {
            area.toInt().toString()
        } else {
            area
                .toString()
                .replace(
                    '.',
                    ','
                )
        }
    }
}
