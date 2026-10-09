package ru.fo6osik.workjournal

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.AdapterView
import android.widget.Filter
import androidx.core.widget.doAfterTextChanged

class AddVehicleActivity : AppCompatActivity() {

    private var editingVehicleId: Int = -1

    private lateinit var editBrand: AutoCompleteTextView
    private lateinit var editModel: AutoCompleteTextView
    private lateinit var editEngine: AutoCompleteTextView

    private lateinit var textEngineSpecs: TextView

    private var carBrands: List<CarBrand> = emptyList()
    private var tractorBrands: List<TractorBrand> = emptyList()
    private var combineBrands: List<CombineBrand> = emptyList()
    private var engines: List<CarEngine> = emptyList()

    private var currentVehicleType: String = "Трактор"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_vehicle)

        val spinnerVehicleType =
            findViewById<Spinner>(R.id.spinnerVehicleType)

        editBrand =
            findViewById(R.id.editBrand)

        editModel =
            findViewById(R.id.editModel)

        editEngine =
            findViewById(R.id.editEngine)

        textEngineSpecs =
            findViewById(R.id.textEngineSpecs)

        val editNumber =
            findViewById<EditText>(R.id.editNumber)

        val editYear =
            findViewById<EditText>(R.id.editYear)

        val editNote =
            findViewById<EditText>(R.id.editNote)

        val buttonSaveVehicle =
            findViewById<Button>(R.id.buttonSaveVehicle)

        val textVehicleFormTitle =
            findViewById<TextView>(R.id.textVehicleFormTitle)

        val vehicleTypes = arrayOf(
    "Трактор",
    "Комбайн",
    "Автомобиль"
)

        val typeAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                vehicleTypes
            )

        typeAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerVehicleType.adapter =
            typeAdapter
			spinnerVehicleType.onItemSelectedListener =
    object : AdapterView.OnItemSelectedListener {

        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {

            val newVehicleType =
    vehicleTypes[position]

val typeChanged =
    newVehicleType != currentVehicleType

currentVehicleType =
    newVehicleType

if (typeChanged) {

    editBrand.setText("", false)
    editModel.setText("", false)
    editEngine.setText("", false)

    hideEngineSpecs()
}

lifecycleScope.launch {
    loadBrandsForCurrentType()
  }
}

        override fun onNothingSelected(
            parent: AdapterView<*>?
        ) {
        }
    }

        editingVehicleId =
            intent.getIntExtra(
                "vehicle_id",
                -1
            )

        lifecycleScope.launch {

            val database =
                DatabaseProvider
                    .getDatabase(applicationContext)

            CarCatalogSeeder
                .seedBaseCatalog(database)
				
				TractorCatalogSeeder
    .seedBaseCatalog(database)
	
	           CombineCatalogSeeder
    .seedBaseCatalog(database)

            loadBrandsForCurrentType()

            if (editingVehicleId != -1) {

                textVehicleFormTitle.text =
                    "Редактирование техники"

                buttonSaveVehicle.text =
                    "Сохранить изменения"

                val vehicle =
                    database
                        .vehicleDao()
                        .getById(editingVehicleId)

                if (vehicle != null) {

                    val typePosition =
    vehicleTypes.indexOf(
        vehicle.type
    )

currentVehicleType =
    vehicle.type

if (typePosition >= 0) {
    spinnerVehicleType
        .setSelection(typePosition)
}

loadBrandsForCurrentType()

editBrand.setText(
    vehicle.brand,
    false
)

when (currentVehicleType) {

    "Автомобиль" -> {

        val catalogBrand =
            database
                .carCatalogDao()
                .getBrandByName(
                    vehicle.brand
                )

        if (catalogBrand != null) {
            loadModels(
                catalogBrand.id
            )
        }
    }

    "Комбайн" -> {

        val catalogBrand =
            database
                .combineCatalogDao()
                .getBrandByName(
                    vehicle.brand
                )

        if (catalogBrand != null) {
            loadModels(
                catalogBrand.id
            )
        }
    }

    else -> {

        val catalogBrand =
            database
                .tractorCatalogDao()
                .getBrandByName(
                    vehicle.brand
                )

        if (catalogBrand != null) {
            loadModels(
                catalogBrand.id
            )
        }
    }
}

                    editModel.setText(
                        vehicle.model,
                        false
                    )

                    if (vehicle.engineId != null) {

                        val engine =
                            database
                                .carCatalogDao()
                                .getEngineById(
                                    vehicle.engineId
                                )

                        if (engine != null) {

                            editEngine.setText(
                                engine.code,
                                false
                            )

                            showEngineSpecs(
                                engine
                            )
                        }

                    } else if (
                        vehicle.engineName.isNotBlank()
                    ) {

                        editEngine.setText(
                            vehicle.engineName,
                            false
                        )
                    }

                    editNumber.setText(
                        vehicle.number
                    )

                    if (vehicle.year != null) {

                        editYear.setText(
                            vehicle.year.toString()
                        )
                    }

                    editNote.setText(
                        vehicle.note
                    )
                }
            }
        }

        // -------------------------
        // Марка
        // -------------------------

        editBrand.setOnItemClickListener {
        _, _, position, _ ->

    val selectedName =
        editBrand.adapter
            .getItem(position)
            .toString()

    editModel.setText("", false)
    editEngine.setText("", false)

    hideEngineSpecs()

    lifecycleScope.launch {

        val database =
            DatabaseProvider
                .getDatabase(applicationContext)

        when (currentVehicleType) {

    "Автомобиль" -> {

        val selectedBrand =
            database
                .carCatalogDao()
                .getBrandByName(
                    selectedName
                )

        if (selectedBrand != null) {
            loadModels(
                selectedBrand.id
            )
        }
    }

    "Комбайн" -> {

        val selectedBrand =
            database
                .combineCatalogDao()
                .getBrandByName(
                    selectedName
                )

        if (selectedBrand != null) {
            loadModels(
                selectedBrand.id
            )
        }
    }

    else -> {

        val selectedBrand =
            database
                .tractorCatalogDao()
                .getBrandByName(
                    selectedName
                )

        if (selectedBrand != null) {
            loadModels(
                selectedBrand.id
            )
        }
    }
}
    }
}

    editBrand.doAfterTextChanged {

    editModel.setText("", false)
    editEngine.setText("", false)

    engines = emptyList()

    hideEngineSpecs()
}

        // -------------------------
        // Модель
        // -------------------------

        editModel.setOnClickListener {

    lifecycleScope.launch {

        val database =
            DatabaseProvider
                .getDatabase(applicationContext)

        val brandName =
            editBrand.text
                .toString()
                .trim()

        when (currentVehicleType) {

    "Автомобиль" -> {

        val brand =
            database
                .carCatalogDao()
                .getBrandByName(
                    brandName
                )

        if (brand != null) {

            loadModels(
                brand.id
            )

            editModel.showDropDown()
        }
    }

    "Комбайн" -> {

        val brand =
            database
                .combineCatalogDao()
                .getBrandByName(
                    brandName
                )

        if (brand != null) {

            loadModels(
                brand.id
            )

            editModel.showDropDown()
        }
    }

    else -> {

        val brand =
            database
                .tractorCatalogDao()
                .getBrandByName(
                    brandName
                )

        if (brand != null) {

            loadModels(
                brand.id
            )

            editModel.showDropDown()
        }
    }
}
    }
}

        editModel.setOnItemClickListener {
        _, _, _, _ ->

    editEngine.setText("", false)
    hideEngineSpecs()

    lifecycleScope.launch {

        loadEnginesForCurrentModel(
            showDropdown = true
        )
    }
}

editModel.doAfterTextChanged {

    editEngine.setText("", false)

    engines = emptyList()

    hideEngineSpecs()
}

        // -------------------------
        // Двигатель
        // -------------------------

editEngine.setOnClickListener {

    lifecycleScope.launch {

        loadEnginesForCurrentModel(
            showDropdown = true
        )
    }
}

editEngine.setOnFocusChangeListener { _, hasFocus ->

    if (hasFocus) {

        lifecycleScope.launch {

            loadEnginesForCurrentModel(
                showDropdown = true
            )
        }
    }
}

editEngine.doAfterTextChanged {

    hideEngineSpecs()
	
}

editEngine.setOnItemClickListener {
        _, _, position, _ ->

    if (position >= 0 && position < engines.size) {

        val selectedEngine =
            engines[position]

        showEngineSpecs(
            selectedEngine
        )
    }
}

        // -------------------------
        // Сохранение
        // -------------------------

        buttonSaveVehicle.setOnClickListener {

            val type =
                spinnerVehicleType
                    .selectedItem
                    .toString()

            val enteredBrand =
                editBrand.text
                    .toString()
                    .trim()

            val enteredModel =
                editModel.text
                    .toString()
                    .trim()

            val enteredEngine =
                editEngine.text
                    .toString()
                    .trim()

            val number =
                editNumber.text
                    .toString()
                    .trim()

            val year =
                editYear.text
                    .toString()
                    .trim()
                    .toIntOrNull()

            val note =
                editNote.text
                    .toString()
                    .trim()

            if (
                enteredBrand.isEmpty() ||
                enteredModel.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Укажите марку и модель",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            lifecycleScope.launch {

                val database =
                    DatabaseProvider
                        .getDatabase(applicationContext)

                var finalBrand =
    enteredBrand

var finalModel =
    enteredModel

var catalogEngine: CarEngine? =
    null

when (type) {

    "Автомобиль" -> {

        val catalogDao =
            database.carCatalogDao()

        val catalogBrand =
            catalogDao.getBrandByName(
                enteredBrand
            )

        if (catalogBrand != null) {

            finalBrand =
                catalogBrand.name

            val catalogModel =
                catalogDao.getModelByName(
                    catalogBrand.id,
                    enteredModel
                )

            if (catalogModel != null) {

                finalModel =
                    catalogModel.name

                if (enteredEngine.isNotBlank()) {

                    catalogEngine =
                        catalogDao
                            .getEnginesByModel(
                                catalogModel.id
                            )
                            .firstOrNull {

                                it.code.equals(
                                    enteredEngine,
                                    ignoreCase = true
                                )
                            }
                }
            }
        }
    }


    "Комбайн" -> {

        val catalogDao =
            database.combineCatalogDao()

        val catalogBrand =
            catalogDao.getBrandByName(
                enteredBrand
            )

        if (catalogBrand != null) {

            finalBrand =
                catalogBrand.name

            val catalogModel =
                catalogDao.getModelByName(
                    catalogBrand.id,
                    enteredModel
                )

            if (catalogModel != null) {

                finalModel =
                    catalogModel.name

                if (enteredEngine.isNotBlank()) {

                    catalogEngine =
                        catalogDao
                            .getEnginesByModel(
                                catalogModel.id
                            )
                            .firstOrNull {

                                it.code.equals(
                                    enteredEngine,
                                    ignoreCase = true
                                )
                            }
                }
            }
        }
    }


    else -> {

        val catalogDao =
            database.tractorCatalogDao()

        val catalogBrand =
            catalogDao.getBrandByName(
                enteredBrand
            )

        if (catalogBrand != null) {

            finalBrand =
                catalogBrand.name

            val catalogModel =
                catalogDao.getModelByName(
                    catalogBrand.id,
                    enteredModel
                )

            if (catalogModel != null) {

                finalModel =
                    catalogModel.name

                if (enteredEngine.isNotBlank()) {

                    catalogEngine =
                        catalogDao
                            .getEnginesByModel(
                                catalogModel.id
                            )
                            .firstOrNull {

                                it.code.equals(
                                    enteredEngine,
                                    ignoreCase = true
                                )
                            }
                }
            }
        }
    }
}


val finalEngineId =
    catalogEngine?.id

val finalEngineName =
    catalogEngine?.code
        ?: enteredEngine

                val vehicleDao =
                    database.vehicleDao()

                if (editingVehicleId == -1) {

                    val vehicle =
                        Vehicle(
                            type = type,
                            brand = finalBrand,
                            model = finalModel,
                            number = number,
                            year = year,
                            note = note,
                            engineId = finalEngineId,
                            engineName = finalEngineName
                        )

                    vehicleDao.insert(vehicle)

                    Toast.makeText(
                        this@AddVehicleActivity,
                        "Техника сохранена",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    val vehicle =
                        Vehicle(
                            id = editingVehicleId,
                            type = type,
                            brand = finalBrand,
                            model = finalModel,
                            number = number,
                            year = year,
                            note = note,
                            engineId = finalEngineId,
                            engineName = finalEngineName
                        )

                    vehicleDao.update(vehicle)

                    Toast.makeText(
                        this@AddVehicleActivity,
                        "Изменения сохранены",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                finish()
            }
        }
    }
	
	private suspend fun loadBrandsForCurrentType() {

    val database =
        DatabaseProvider
            .getDatabase(applicationContext)

    val names: List<String>

when (currentVehicleType) {

    "Автомобиль" -> {

        carBrands =
            database
                .carCatalogDao()
                .getAllBrands()

        names =
            carBrands.map {
                it.name
            }
    }


    "Комбайн" -> {

        combineBrands =
            database
                .combineCatalogDao()
                .getAllBrands()

        names =
            combineBrands.map {
                it.name
            }
    }


    else -> {

        tractorBrands =
            database
                .tractorCatalogDao()
                .getAllBrands()

        names =
            tractorBrands.map {
                it.name
            }
    }
}

    editBrand.setAdapter(
        createNormalizedAdapter(names)
    )
}

    // -------------------------
    // Модели
    // -------------------------

    private suspend fun loadModels(
    brandId: Int
) {

    val database =
        DatabaseProvider
            .getDatabase(applicationContext)

    val names: List<String>

    when (currentVehicleType) {

    "Автомобиль" -> {

        names =
            database
                .carCatalogDao()
                .getModelsByBrand(
                    brandId
                )
                .map {
                    it.name
                }
    }


    "Комбайн" -> {

        names =
            database
                .combineCatalogDao()
                .getModelsByBrand(
                    brandId
                )
                .map {
                    it.name
                }
    }


    else -> {

        names =
            database
                .tractorCatalogDao()
                .getModelsByBrand(
                    brandId
                )
                .map {
                    it.name
                }
    }
}

    editModel.setAdapter(
    createNormalizedAdapter(names)
)
}

private fun normalizeSearch(
    value: String
): String {

    return value
        .lowercase()
        .replace("-", "")
        .replace("–", "")
        .replace("—", "")
        .replace(" ", "")
}


private fun createNormalizedAdapter(
    items: List<String>
): ArrayAdapter<String> {

    return object : ArrayAdapter<String>(
        this@AddVehicleActivity,
        android.R.layout.simple_dropdown_item_1line,
        items.toMutableList()
    ) {

        private val allItems =
            items.toList()

        override fun getFilter(): Filter {

            return object : Filter() {

                override fun performFiltering(
                    constraint: CharSequence?
                ): FilterResults {

                    val query =
                        normalizeSearch(
                            constraint
                                ?.toString()
                                .orEmpty()
                        )

                    val filtered =
                        if (query.isEmpty()) {

                            allItems

                        } else {

                            allItems.filter {

                                normalizeSearch(it)
                                    .contains(query)
                            }
                        }

                    return FilterResults().apply {

                        values = filtered
                        count = filtered.size
                    }
                }

                override fun publishResults(
                    constraint: CharSequence?,
                    results: FilterResults?
                ) {

                    clear()

                    val filtered =
                        results?.values as? List<*>
                            ?: emptyList<Any>()

                    filtered.forEach {

                        if (it is String) {
                            add(it)
                        }
                    }

                    notifyDataSetChanged()
                }
            }
        }
    }
}

    // -------------------------
    // Двигатели
    // -------------------------

    private suspend fun loadEnginesForCurrentModel(
    showDropdown: Boolean
) {

    val database =
        DatabaseProvider
            .getDatabase(applicationContext)

    val brandName =
        editBrand.text
            .toString()
            .trim()

    val modelName =
        editModel.text
            .toString()
            .trim()

    when (currentVehicleType) {

    "Автомобиль" -> {

        val dao =
            database.carCatalogDao()

        val brand =
            dao.getBrandByName(
                brandName
            )

        if (brand == null) {

            engines =
                emptyList()

            return
        }

        val model =
            dao.getModelByName(
                brand.id,
                modelName
            )

        if (model == null) {

            engines =
                emptyList()

            return
        }

        engines =
            dao.getEnginesByModel(
                model.id
            )
    }


    "Комбайн" -> {

        val dao =
            database.combineCatalogDao()

        val brand =
            dao.getBrandByName(
                brandName
            )

        if (brand == null) {

            engines =
                emptyList()

            return
        }

        val model =
            dao.getModelByName(
                brand.id,
                modelName
            )

        if (model == null) {

            engines =
                emptyList()

            return
        }

        engines =
            dao.getEnginesByModel(
                model.id
            )
    }


    else -> {

        val dao =
            database.tractorCatalogDao()

        val brand =
            dao.getBrandByName(
                brandName
            )

        if (brand == null) {

            engines =
                emptyList()

            return
        }

        val model =
            dao.getModelByName(
                brand.id,
                modelName
            )

        if (model == null) {

            engines =
                emptyList()

            return
        }

        engines =
            dao.getEnginesByModel(
                model.id
            )
    }
}

    val names =
        engines.map {
            it.code
        }

    editEngine.setAdapter(
        ArrayAdapter(
            this@AddVehicleActivity,
            android.R.layout
                .simple_dropdown_item_1line,
            names
        )
    )

    if (
        showDropdown &&
        engines.isNotEmpty()
    ) {

        editEngine.showDropDown()
    }
}

    // -------------------------
    // ТТХ двигателя
    // -------------------------

    private fun showEngineSpecs(
        engine: CarEngine
    ) {

        val volume =
            if (engine.displacementCc != null)
                "${engine.displacementCc} см³"
            else
                "не указано"

        val power =
            if (engine.powerHp != null)
                "${engine.powerHp} л.с."
            else
                "не указано"

        val cylinders =
            engine.cylinders?.toString()
                ?: "не указано"

        val valves =
            engine.valves?.toString()
                ?: "не указано"

        val fuel =
            if (engine.fuelType.isBlank())
                "не указано"
            else
                engine.fuelType

        textEngineSpecs.text =
            "Характеристики двигателя\n" +
            "Производитель: ${engine.manufacturer}\n" +
            "Объём: $volume\n" +
            "Мощность: $power\n" +
            "Цилиндры: $cylinders\n" +
            "Клапаны: $valves\n" +
            "Топливо: $fuel"

        textEngineSpecs.visibility =
            View.VISIBLE
    }

    private fun hideEngineSpecs() {

        textEngineSpecs.visibility =
            View.GONE
    }
}