package ru.fo6osik.workjournal

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class VehicleDetailsActivity : AppCompatActivity() {

    private var vehicleId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_vehicle_details)

        vehicleId = intent.getIntExtra("vehicle_id", -1)

        if (vehicleId == -1) {
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        loadVehicle()
    }

    private fun loadVehicle() {

        lifecycleScope.launch {

            val database =
                DatabaseProvider.getDatabase(applicationContext)

            val vehicle =
                database
                    .vehicleDao()
                    .getById(vehicleId)

            if (vehicle == null) {
                finish()
                return@launch
            }

            // -------------------------
            // Паспорт техники
            // -------------------------

            findViewById<TextView>(
                R.id.textVehicleTitle
            ).text =
                "${vehicle.brand} ${vehicle.model}"

            findViewById<TextView>(
                R.id.textVehicleType
            ).text =
                "Тип: ${vehicle.type}"

            findViewById<TextView>(
                R.id.textVehicleBrand
            ).text =
                "Марка: ${vehicle.brand}"

            findViewById<TextView>(
                R.id.textVehicleModel
            ).text =
                "Модель: ${vehicle.model}"
				
				val textVehicleEngine =
    findViewById<TextView>(
        R.id.textVehicleEngine
    )

val textVehicleEngineSpecs =
    findViewById<TextView>(
        R.id.textVehicleEngineSpecs
    )

if (vehicle.engineId != null) {

    val engine =
        database
            .carCatalogDao()
            .getEngineById(
                vehicle.engineId
            )

    if (engine != null) {

        textVehicleEngine.text =
            "Двигатель: ${engine.code}"

        val volume =
            engine.displacementCc
                ?.let { "$it см³" }
                ?: "Не указано"

        val power =
            engine.powerHp
                ?.let { "$it л.с." }
                ?: "Не указано"

        val cylinders =
            engine.cylinders
                ?.toString()
                ?: "Не указано"

        val valves =
            engine.valves
                ?.toString()
                ?: "Не указано"

        val fuel =
            if (engine.fuelType.isBlank())
                "Не указано"
            else
                engine.fuelType

        textVehicleEngineSpecs.text =
            "Производитель: ${engine.manufacturer}\n" +
            "Объём: $volume\n" +
            "Мощность: $power\n" +
            "Цилиндры: $cylinders\n" +
            "Клапаны: $valves\n" +
            "Топливо: $fuel"

        textVehicleEngineSpecs.visibility =
            View.VISIBLE

    } else {

        textVehicleEngine.text =
            "Двигатель: ${vehicle.engineName}"

        textVehicleEngineSpecs.visibility =
            View.GONE
    }

} else if (vehicle.engineName.isNotBlank()) {

    textVehicleEngine.text =
        "Двигатель: ${vehicle.engineName}"

    textVehicleEngineSpecs.visibility =
        View.GONE

} else {

    textVehicleEngine.text =
        "Двигатель: Не указан"

    textVehicleEngineSpecs.visibility =
        View.GONE
}

            findViewById<TextView>(
                R.id.textVehicleNumber
            ).text =
                "Госномер: ${
                    if (vehicle.number.isBlank())
                        "Не указан"
                    else
                        vehicle.number
                }"

            findViewById<TextView>(
                R.id.textVehicleYear
            ).text =
                "Год выпуска: ${
                    vehicle.year ?: "Не указан"
                }"

            findViewById<TextView>(
                R.id.textVehicleNote
            ).text =
                "Примечание: ${
                    if (vehicle.note.isBlank())
                        "Нет"
                    else
                        vehicle.note
                }"


            // -------------------------
            // Паспорт / ТТХ техники
            // -------------------------

            val detailsContainer =
                findViewById<TextView>(
                    R.id.textVehicleNote
                ).parent as? LinearLayout

            val textVehicleTitle =
                findViewById<TextView>(
                    R.id.textVehicleTitle
                )

            val textVehicleType =
                findViewById<TextView>(
                    R.id.textVehicleType
                )

            val noteView =
                findViewById<TextView>(
                    R.id.textVehicleNote
                )

            val textVehicleEngineForTtx =
                findViewById<TextView>(
                    R.id.textVehicleEngine
                )

            val textVehicleEngineSpecsForTtx =
                findViewById<TextView>(
                    R.id.textVehicleEngineSpecs
                )

            val isCombine =
                vehicle.type.equals(
                    "Комбайн",
                    ignoreCase =
                        true
                )

            val isNova340ForSpecs =
                isCombine &&
                    vehicle.brand.equals(
                        "Ростсельмаш",
                        ignoreCase =
                            true
                    ) &&
                    vehicle.model.equals(
                        "NOVA 340",
                        ignoreCase =
                            true
                    )

            // Убираем ранее созданные динамические заголовки/строки.
            listOf(
                "vehicle_passport_header",
                "vehicle_specs_header",
                "combine_specs_value",
                "combine_specs_button"
            ).forEach {
                tag ->

                detailsContainer
                    ?.findViewWithTag<View>(
                        tag
                    )
                    ?.let {
                        detailsContainer
                            .removeView(
                                it
                            )
                    }
            }

            /*
             * Двигатель больше не относится к паспортной части.
             * Физически переносим существующие TextView ниже
             * примечания в блок ТТХ.
             */
            (
                textVehicleEngineForTtx.parent
                    as? LinearLayout
                )
                ?.removeView(
                    textVehicleEngineForTtx
                )

            (
                textVehicleEngineSpecsForTtx.parent
                    as? LinearLayout
                )
                ?.removeView(
                    textVehicleEngineSpecsForTtx
                )

            // Заголовок паспортной части после названия техники.
            val passportHeader =
                TextView(
                    this@VehicleDetailsActivity
                ).apply {
                    tag =
                        "vehicle_passport_header"

                    text =
                        "Паспорт техники"

                    textSize =
                        22f

                    setTypeface(
                        typeface,
                        Typeface.BOLD
                    )

                    setPadding(
                        8,
                        8,
                        8,
                        12
                    )
                }

            val titleIndex =
                detailsContainer
                    ?.indexOfChild(
                        textVehicleTitle
                    )
                    ?: -1

            if (
                detailsContainer !=
                null &&
                titleIndex >=
                0
            ) {
                detailsContainer.addView(
                    passportHeader,
                    titleIndex + 1
                )
            }

            // Заголовок ТТХ после примечания.
            val specsHeader =
                TextView(
                    this@VehicleDetailsActivity
                ).apply {
                    tag =
                        "vehicle_specs_header"

                    text =
                        if (
                            isCombine
                        ) {
                            "ТТХ комбайна"
                        } else {
                            "ТТХ техники"
                        }

                    textSize =
                        22f

                    setTypeface(
                        typeface,
                        Typeface.BOLD
                    )

                    setPadding(
                        8,
                        28,
                        8,
                        10
                    )
                }

            val noteIndex =
                detailsContainer
                    ?.indexOfChild(
                        noteView
                    )
                    ?: -1

            if (
                detailsContainer !=
                null &&
                noteIndex >=
                0
            ) {
                var insertIndex =
                    noteIndex + 1

                detailsContainer.addView(
                    specsHeader,
                    insertIndex
                )

                insertIndex +=
                    1

                detailsContainer.addView(
                    textVehicleEngineForTtx,
                    insertIndex
                )

                insertIndex +=
                    1

                detailsContainer.addView(
                    textVehicleEngineSpecsForTtx,
                    insertIndex
                )

                insertIndex +=
                    1

                if (
                    isCombine
                ) {
                    var combineSpec =
                        database
                            .combineSpecDao()
                            .getByVehicleId(
                                vehicle.id
                            )

                    /*
                     * Для нашего Ростсельмаш NOVA 340
                     * фиксируем принятую рабочую ТТХ:
                     * зерновой бункер = 4,7 м³.
                     *
                     * Если значение уже было задано вручную,
                     * оно не перезаписывается.
                     */
                    if (
                        isNova340ForSpecs
                    ) {
                        val currentVolume =
                            combineSpec
                                ?.grainTankVolumeM3

                        /*
                         * Переход с ранее принятого 4,5 м³
                         * на новое рабочее значение 4,7 м³.
                         *
                         * Меняем только:
                         * - пустое значение;
                         * - прежнее значение 4,5 м³.
                         *
                         * Любое другое ручное значение
                         * не трогаем.
                         */
                        if (
                            currentVolume ==
                                null ||
                            kotlin.math.abs(
                                currentVolume -
                                    4.5
                            ) <
                            0.0001
                        ) {
                            combineSpec =
                                CombineSpec(
                                    vehicleId =
                                        vehicle.id,
                                    grainTankVolumeM3 =
                                        4.7
                                )

                            database
                                .combineSpecDao()
                                .upsert(
                                    combineSpec
                                )
                        }
                    }

                    val bunkerVolume =
                        combineSpec
                            ?.grainTankVolumeM3

                    /*
                     * Старые уборочные кампании создавались
                     * до появления этой ТТХ и могли хранить null.
                     * Заполняем ТОЛЬКО пустые значения.
                     * Если в конкретной кампании объём уже
                     * корректировали вручную — не трогаем его.
                     */
                    if (
                        bunkerVolume !=
                        null
                    ) {
                        val harvestDao =
                            database
                                .harvestDao()

                        harvestDao
                            .fillMissingBunkerVolumeForCombine(
                                vehicleId =
                                    vehicle.id,
                                bunkerVolumeM3 =
                                    bunkerVolume
                            )

                        if (
                            isNova340ForSpecs &&
                            kotlin.math.abs(
                                bunkerVolume -
                                    4.7
                            ) <
                            0.0001
                        ) {
                            harvestDao
                                .replaceBunkerVolumeForCombineIfEquals(
                                    vehicleId =
                                        vehicle.id,
                                    oldVolumeM3 =
                                        4.5,
                                    newVolumeM3 =
                                        4.7
                                )
                        }
                    }

                    val valueView =
                        TextView(
                            this@VehicleDetailsActivity
                        ).apply {
                            tag =
                                "combine_specs_value"

                            text =
                                if (
                                    bunkerVolume ==
                                    null
                                ) {
                                    "Объём зернового бункера: не указан"
                                } else {
                                    "Объём зернового бункера: " +
                                        formatVolume(
                                            bunkerVolume
                                        ) +
                                        " м³"
                                }

                            textSize =
                                18f

                            setPadding(
                                8,
                                8,
                                8,
                                8
                            )
                        }

                    val editButton =
                        Button(
                            this@VehicleDetailsActivity
                        ).apply {
                            tag =
                                "combine_specs_button"

                            text =
                                "Изменить ТТХ комбайна"

                            setOnClickListener {
                                showCombineSpecsDialog(
                                    vehicleId =
                                        vehicle.id,
                                    currentVolume =
                                        bunkerVolume
                                )
                            }
                        }

                    detailsContainer.addView(
                        valueView,
                        insertIndex
                    )

                    insertIndex +=
                        1

                    detailsContainer.addView(
                        editButton,
                        insertIndex
                    )
                }
            }
				
				// -------------------------
// Документация
// -------------------------

val documentationSection =
    findViewById<LinearLayout>(
        R.id.documentationSection
    )

val buttonVehicleManual =
    findViewById<Button>(
        R.id.buttonVehicleManual
    )

val buttonVehicleBulletins =
    findViewById<Button>(
        R.id.buttonVehicleBulletins
    )

val buttonEngineDiagnostics =
    findViewById<Button>(
        R.id.buttonEngineDiagnostics
    )


// -------------------------
// NOVA 340
// -------------------------

val isNova340 =
    vehicle.type.equals(
        "Комбайн",
        ignoreCase = true
    ) &&
    vehicle.brand.equals(
        "Ростсельмаш",
        ignoreCase = true
    ) &&
    vehicle.model.equals(
        "NOVA 340",
        ignoreCase = true
    )


// -------------------------
// LADA Vesta SW
// -------------------------

val isVestaSw =
    vehicle.type.equals(
        "Автомобиль",
        ignoreCase = true
    ) &&
    vehicle.brand.equals(
        "LADA",
        ignoreCase = true
    ) &&
    vehicle.model.equals(
        "Vesta SW",
        ignoreCase = true
    )


// -------------------------
// КИРОВЕЦ К-424
// -------------------------

val isKirovetsK424 =
    vehicle.type.equals(
        "Трактор",
        ignoreCase = true
    ) &&
    vehicle.brand.equals(
        "КИРОВЕЦ",
        ignoreCase = true
    ) &&
    vehicle.model.equals(
        "К-424",
        ignoreCase = true
    )


// -------------------------
// БЕЛАРУС 1021.3
// -------------------------

val isBelarus10213 =
    vehicle.type.equals(
        "Трактор",
        ignoreCase = true
    ) &&
    vehicle.brand.equals(
        "БЕЛАРУС",
        ignoreCase = true
    ) &&
    vehicle.model.equals(
        "1021.3",
        ignoreCase = true
    )


// Есть ли документация

val hasDocumentation =
    isNova340 ||
    isVestaSw ||
    isKirovetsK424 ||
    isBelarus10213


documentationSection.visibility =
    if (hasDocumentation) {
        View.VISIBLE
    } else {
        View.GONE
    }


// Руководство есть у всей техники
// из текущего парка

buttonVehicleManual.visibility =
    if (hasDocumentation) {
        View.VISIBLE
    } else {
        View.GONE
    }

buttonVehicleManual.isEnabled =
    hasDocumentation


// Бюллетени только NOVA 340

buttonVehicleBulletins.visibility =
    if (isNova340) {
        View.VISIBLE
    } else {
        View.GONE
    }

buttonVehicleBulletins.isEnabled =
    isNova340


// Диагностика ЯМЗ только NOVA 340

buttonEngineDiagnostics.visibility =
    if (isNova340) {
        View.VISIBLE
    } else {
        View.GONE
    }

buttonEngineDiagnostics.isEnabled =
    isNova340


// -------------------------
// Руководство по эксплуатации
// -------------------------

if (hasDocumentation) {

    buttonVehicleManual
        .setOnClickListener {

            val intent =
                Intent(
                    this@VehicleDetailsActivity,
                    PdfViewerActivity::class.java
                )

            when {

                isNova340 -> {

                    intent.putExtra(
                        "assetPath",
                        "manuals/nova_340_manual.pdf"
                    )

                    intent.putExtra(
                        "documentTitle",
                        "NOVA 340 — Руководство по эксплуатации"
                    )
                }


                isVestaSw -> {

                    intent.putExtra(
                        "assetPath",
                        "manuals/lada_vesta_sw_manual.pdf"
                    )

                    intent.putExtra(
                        "documentTitle",
                        "LADA Vesta — Руководство по эксплуатации"
                    )
                }


                isKirovetsK424 -> {

                    intent.putExtra(
                        "assetPath",
                        "manuals/kirovets_k424_manual.pdf"
                    )

                    intent.putExtra(
                        "documentTitle",
                        "КИРОВЕЦ К-424 — Инструкция по эксплуатации"
                    )
                }


                isBelarus10213 -> {

                    intent.putExtra(
                        "assetPath",
                        "manuals/belarus_1021_3_manual.pdf"
                    )

                    intent.putExtra(
                        "documentTitle",
                        "БЕЛАРУС 1021.3 — Руководство по эксплуатации"
                    )
                }
            }

            startActivity(
                intent
            )
        }
}


// -------------------------
// Бюллетени NOVA 340
// -------------------------

if (isNova340) {

    buttonVehicleBulletins
        .setOnClickListener {

            val intent =
                Intent(
                    this@VehicleDetailsActivity,
                    Nova340BulletinsActivity::class.java
                )

            startActivity(
                intent
            )
        }


    // -------------------------
    // Диагностика двигателя ЯМЗ
    // -------------------------

    buttonEngineDiagnostics
        .setOnClickListener {

            val intent =
                Intent(
                    this@VehicleDetailsActivity,
                    PdfViewerActivity::class.java
                )

            intent.putExtra(
                "assetPath",
                "manuals/engines/ymz_5340_536_diagnostics.pdf"
            )

            intent.putExtra(
                "documentTitle",
                "ЯМЗ-5340 / ЯМЗ-536 — Инструкция по диагностике"
            )

            startActivity(
                intent
            )
        }
}

            // -------------------------
            // Данные ТО
            // -------------------------

            val maintenanceDao =
                database.maintenanceDao()

            val records =
                maintenanceDao.getByVehicle(vehicleId)

            /*
             * В карточке техники отображаем только техническую историю:
             * - ТО из maintenance_records;
             * - неисправности и ремонты из farm_events.
             *
             * Рабочие FIELD_WORK-события здесь намеренно не показываются.
             */
            val technicalEvents =
                database
                    .objectLinkDao()
                    .getEventsForVehicle(
                        vehicleId
                    )
                    .mapNotNull { relation ->

                        database
                            .farmEventDao()
                            .getByUid(
                                relation.eventUid
                            )
                    }
                    .filter {
                        it.eventType == "MALFUNCTION" ||
                        it.eventType == "REPAIR"
                    }
                    .distinctBy {
                        it.eventUid
                    }
                    .sortedWith(
                        compareByDescending<FarmEvent> {
                            it.eventYear
                        }
                            .thenByDescending {
                                it.eventMonth ?: 0
                            }
                            .thenByDescending {
                                it.eventDay ?: 0
                            }
                            .thenByDescending {
                                it.createdAt
                            }
                    )

            val lastRecord =
                maintenanceDao.getLastByVehicle(vehicleId)

            val dateFormatter =
                SimpleDateFormat(
                    "dd.MM.yyyy",
                    Locale.getDefault()
                )

            val numberFormatter =
                NumberFormat.getIntegerInstance(
                    Locale.getDefault()
                )

            // Последние показания

            val lastMeterText =
                if (
                    lastRecord == null ||
                    lastRecord.meterValue == null
                ) {

                    "Последние показания: нет данных"

                } else {

                    val unit =
                        if (lastRecord.meterType == "km")
                            "км"
                        else
                            "м/ч"

                    "Последние показания: ${
                        numberFormatter.format(
                            lastRecord.meterValue
                        )
                    } $unit"
                }

            findViewById<TextView>(
                R.id.textVehicleLastMeter
            ).text =
                lastMeterText

            // Последнее ТО

            findViewById<TextView>(
                R.id.textVehicleLastMaintenance
            ).text =
                if (lastRecord == null) {

                    "Последнее ТО: нет данных"

                } else {

                    "Последнее ТО: ${
                        dateFormatter.format(
                            Date(lastRecord.date)
                        )
                    }"
                }

            // Количество ТО за текущий год

            val startOfYear =
                Calendar.getInstance().apply {

                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

            val startOfNextYear =
                Calendar.getInstance().apply {

                    timeInMillis =
                        startOfYear.timeInMillis

                    add(Calendar.YEAR, 1)
                }

            val maintenanceCount =
                maintenanceDao
                    .countByVehicleForPeriod(
                        vehicleId,
                        startOfYear.timeInMillis,
                        startOfNextYear.timeInMillis
                    )

            findViewById<TextView>(
                R.id.textVehicleMaintenanceCount
            ).text =
                "ТО за ${startOfYear.get(Calendar.YEAR)} год: $maintenanceCount"

            // -------------------------
            // История ТО
            // -------------------------

            val historyContainer =
                findViewById<LinearLayout>(
                    R.id.vehicleMaintenanceHistory
                )

            val emptyHistory =
                findViewById<TextView>(
                    R.id.textVehicleMaintenanceEmpty
                )

            historyContainer.removeAllViews()

            if (
                records.isEmpty() &&
                technicalEvents.isEmpty()
            ) {

                emptyHistory.visibility =
                    View.VISIBLE

            } else {

                emptyHistory.visibility =
                    View.GONE

                records.forEach { record ->

                    val meterText =
                        when {

                            record.meterValue == null ->
                                "Показания не указаны"

                            record.meterType == "km" ->
                                "${
                                    numberFormatter.format(
                                        record.meterValue
                                    )
                                } км"

                            else ->
                                "${
                                    numberFormatter.format(
                                        record.meterValue
                                    )
                                } м/ч"
                        }

                    val item =
                        TextView(
                            this@VehicleDetailsActivity
                        )

                    item.text =
                        "${
                            dateFormatter.format(
                                Date(record.date)
                            )
                        } — $meterText\n${record.work}"

                    item.textSize = 17f

                    item.setPadding(
                        8,
                        18,
                        8,
                        18
                    )

                    item.setOnClickListener {

                        val intent = Intent(
                            this@VehicleDetailsActivity,
                            MaintenanceDetailsActivity::class.java
                        )

                        intent.putExtra(
                            "maintenance_id",
                            record.id
                        )

                        startActivity(intent)
                    }

                    historyContainer.addView(item)
                }

                if (
                    technicalEvents.isNotEmpty()
                ) {

                    val header =
                        TextView(
                            this@VehicleDetailsActivity
                        ).apply {

                            text =
                                "Неисправности и ремонты"

                            textSize =
                                20f

                            setTypeface(
                                typeface,
                                Typeface.BOLD
                            )

                            setPadding(
                                8,
                                28,
                                8,
                                12
                            )
                        }

                    historyContainer.addView(
                        header
                    )

                    for (
                        event in technicalEvents
                    ) {

                        val item =
                            TextView(
                                this@VehicleDetailsActivity
                            )

                        item.text =
                            buildString {

                                append(
                                    technicalEventTypeName(
                                        event.eventType
                                    )
                                )

                                append("\n")

                                append(
                                    formatTechnicalEventDate(
                                        event
                                    )
                                )

                                append("\n")

                                append(
                                    event.title
                                )

                                event.description
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let {

                                        append("\n")
                                        append(it)
                                    }
                            }

                        item.textSize =
                            17f

                        item.setPadding(
                            8,
                            18,
                            8,
                            18
                        )

                        historyContainer.addView(
                            item
                        )
                    }
                }
            }

            // -------------------------
            // Редактирование паспорта
            // -------------------------

            val buttonEditVehicle =
                findViewById<Button>(
                    R.id.buttonEditVehicle
                )

            buttonEditVehicle.setOnClickListener {

                val intent = Intent(
                    this@VehicleDetailsActivity,
                    AddVehicleActivity::class.java
                )

                intent.putExtra(
                    "vehicle_id",
                    vehicle.id
                )

                startActivity(intent)
            }

            // -------------------------
            // Удаление техники
            // -------------------------

            val buttonDeleteVehicle =
                findViewById<Button>(
                    R.id.buttonDeleteVehicle
                )

            buttonDeleteVehicle.setOnClickListener {

                AlertDialog.Builder(
                    this@VehicleDetailsActivity
                )
                    .setTitle("Удаление техники")
                    .setMessage(
                        "Удалить ${vehicle.brand} ${vehicle.model}?"
                    )
                    .setPositiveButton(
                        "Удалить"
                    ) { _, _ ->

                        lifecycleScope.launch {

                            database
                                .vehicleDao()
                                .delete(vehicle)

                            finish()
                        }
                    }
                    .setNegativeButton(
                        "Отмена",
                        null
                    )
                    .show()
            }
        }
    }

    private fun showCombineSpecsDialog(
        vehicleId: Int,
        currentVolume: Double?
    ) {
        val input =
            EditText(
                this
            ).apply {
                hint =
                    "Объём бункера, м³"

                inputType =
                    InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL

                setText(
                    currentVolume
                        ?.toString()
                        ?: ""
                )

                setPadding(
                    32,
                    20,
                    32,
                    20
                )
            }

        val dialog =
            AlertDialog
                .Builder(
                    this
                )
                .setTitle(
                    "ТТХ комбайна"
                )
                .setMessage(
                    "Укажите паспортный объём зернового бункера."
                )
                .setView(
                    input
                )
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
                    val raw =
                        input.text
                            .toString()
                            .trim()
                            .replace(
                                ",",
                                "."
                            )

                    val volume =
                        raw
                            .toDoubleOrNull()

                    if (
                        volume == null ||
                        volume <=
                            0.0
                    ) {
                        input.error =
                            "Введите объём больше 0"
                        return@setOnClickListener
                    }

                    lifecycleScope.launch {
                        val database =
                            DatabaseProvider
                                .getDatabase(
                                    applicationContext
                                )

                        database
                            .combineSpecDao()
                            .upsert(
                                CombineSpec(
                                    vehicleId =
                                        vehicleId,
                                    grainTankVolumeM3 =
                                        volume
                                )
                            )

                        database
                            .harvestDao()
                            .fillMissingBunkerVolumeForCombine(
                                vehicleId =
                                    vehicleId,
                                bunkerVolumeM3 =
                                    volume
                            )

                        dialog.dismiss()
                        loadVehicle()
                    }
                }
        }

        dialog.show()
    }

    private fun formatVolume(
        value: Double
    ): String {
        return String.format(
            Locale("ru"),
            "%.2f",
            value
        )
            .trimEnd('0')
            .trimEnd(',')
    }

    private fun technicalEventTypeName(
        eventType: String
    ): String {

        return when (
            eventType
        ) {

            "MALFUNCTION" ->
                "Неисправность"

            "REPAIR" ->
                "Ремонт"

            else ->
                "Техническое событие"
        }
    }

    private fun formatTechnicalEventDate(
        event: FarmEvent
    ): String {

        val month =
            event.eventMonth

        val day =
            event.eventDay

        if (
            event.datePrecision == "DAY" &&
            month != null &&
            day != null
        ) {

            return "%02d.%02d.%04d".format(
                day,
                month,
                event.eventYear
            )
        }

        if (
            event.datePrecision == "MONTH" &&
            month != null
        ) {

            return "${
                monthName(
                    month
                )
            } ${event.eventYear}"
        }

        return event.eventYear.toString()
    }

    private fun monthName(
        month: Int
    ): String {

        return when (
            month
        ) {

            1 -> "Январь"
            2 -> "Февраль"
            3 -> "Март"
            4 -> "Апрель"
            5 -> "Май"
            6 -> "Июнь"
            7 -> "Июль"
            8 -> "Август"
            9 -> "Сентябрь"
            10 -> "Октябрь"
            11 -> "Ноябрь"
            12 -> "Декабрь"

            else -> ""
        }
    }

}