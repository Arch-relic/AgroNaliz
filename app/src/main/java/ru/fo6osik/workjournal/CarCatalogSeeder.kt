package ru.fo6osik.workjournal

object CarCatalogSeeder {

    suspend fun seedBaseCatalog(
        database: AppDatabase
    ) {

        val dao = database.carCatalogDao()

        seedLada(dao)
		seedRenault(dao)
    }

    private suspend fun seedLada(
        dao: CarCatalogDao
    ) {

        // -------------------------
        // Марка LADA
        // -------------------------

        val lada =
            getOrCreateBrand(
                dao = dao,
                name = "LADA"
            )

        // -------------------------
        // Модели семейства Vesta
        // -------------------------

        val vesta =
            getOrCreateModel(
                dao,
                lada.id,
                "Vesta"
            )

        val vestaCross =
            getOrCreateModel(
                dao,
                lada.id,
                "Vesta Cross"
            )

        val vestaSw =
            getOrCreateModel(
                dao,
                lada.id,
                "Vesta SW"
            )

        val vestaSwCross =
            getOrCreateModel(
                dao,
                lada.id,
                "Vesta SW Cross"
            )

        // -------------------------
        // Двигатели
        // -------------------------

        val engine21129 =
            getOrCreateEngine(
                dao = dao,
                manufacturer = "АВТОВАЗ",
                code = "21129",
                displacementCc = 1596,
                powerHp = 106,
                cylinders = 4,
                valves = 16,
                fuelType = "Бензин"
            )

        val engine21179 =
            getOrCreateEngine(
                dao = dao,
                manufacturer = "АВТОВАЗ",
                code = "21179",
                displacementCc = 1774,
                powerHp = 122,
                cylinders = 4,
                valves = 16,
                fuelType = "Бензин"
            )

        val engineH4M =
            getOrCreateEngine(
                dao = dao,
                manufacturer = "Renault-Nissan",
                code = "H4M",
                displacementCc = 1598,
                powerHp = 113,
                cylinders = 4,
                valves = 16,
                fuelType = "Бензин"
            )

        // -------------------------
        // Связи модель ↔ двигатель
        // -------------------------

        val models = listOf(
            vesta,
            vestaCross,
            vestaSw,
            vestaSwCross
        )

        val engines = listOf(
            engine21129,
            engine21179,
            engineH4M
        )

        models.forEach { model ->

            engines.forEach { engine ->

                dao.insertModelEngine(
                    CarModelEngine(
                        modelId = model.id,
                        engineId = engine.id
                    )
                )
            }
        }
    }
	
	private suspend fun seedRenault(
    dao: CarCatalogDao
) {

    // -------------------------
    // Марка Renault
    // -------------------------

    val renault =
        getOrCreateBrand(
            dao = dao,
            name = "Renault"
        )


    // -------------------------
    // Модели
    // -------------------------

    val logan2 =
        getOrCreateModel(
            dao,
            renault.id,
            "Logan II"
        )

    val loganStepway =
        getOrCreateModel(
            dao,
            renault.id,
            "Logan Stepway"
        )

    val sandero2 =
        getOrCreateModel(
            dao,
            renault.id,
            "Sandero II"
        )

    val sanderoStepway =
        getOrCreateModel(
            dao,
            renault.id,
            "Sandero Stepway"
        )


    // -------------------------
    // H4M
    // Используется существующая
    // запись двигателя
    // -------------------------

    val engineH4M =
        getOrCreateEngine(
            dao = dao,
            manufacturer = "Renault-Nissan",
            code = "H4M",
            displacementCc = 1598,
            powerHp = 113,
            cylinders = 4,
            valves = 16,
            fuelType = "Бензин"
        )


    // -------------------------
    // Связи модель ↔ двигатель
    // -------------------------

    val models = listOf(
        logan2,
        loganStepway,
        sandero2,
        sanderoStepway
    )

    models.forEach { model ->

        dao.insertModelEngine(
            CarModelEngine(
                modelId = model.id,
                engineId = engineH4M.id
            )
        )
    }
}

    private suspend fun getOrCreateBrand(
        dao: CarCatalogDao,
        name: String
    ): CarBrand {

        val existing =
            dao.getBrandByName(name)

        if (existing != null) {
            return existing
        }

        val id =
            dao.insertBrand(
                CarBrand(
                    name = name,
                    userCreated = false
                )
            )

        if (id > 0) {

            return CarBrand(
                id = id.toInt(),
                name = name,
                userCreated = false
            )
        }

        return dao.getBrandByName(name)
            ?: error(
                "Не удалось создать марку $name"
            )
    }

    private suspend fun getOrCreateModel(
        dao: CarCatalogDao,
        brandId: Int,
        name: String
    ): CarModel {

        val existing =
            dao.getModelByName(
                brandId,
                name
            )

        if (existing != null) {
            return existing
        }

        val id =
            dao.insertModel(
                CarModel(
                    brandId = brandId,
                    name = name,
                    userCreated = false
                )
            )

        if (id > 0) {

            return CarModel(
                id = id.toInt(),
                brandId = brandId,
                name = name,
                userCreated = false
            )
        }

        return dao.getModelByName(
            brandId,
            name
        ) ?: error(
            "Не удалось создать модель $name"
        )
    }

    private suspend fun getOrCreateEngine(
        dao: CarCatalogDao,
        manufacturer: String,
        code: String,
        displacementCc: Int,
        powerHp: Int,
        cylinders: Int,
        valves: Int,
        fuelType: String
    ): CarEngine {

        val existing =
            dao.getEngine(
                manufacturer,
                code,
                powerHp
            )

        if (existing != null) {
            return existing
        }

        val id =
            dao.insertEngine(
                CarEngine(
                    manufacturer = manufacturer,
                    code = code,
                    displacementCc = displacementCc,
                    powerHp = powerHp,
                    cylinders = cylinders,
                    valves = valves,
                    fuelType = fuelType,
                    userCreated = false
                )
            )

        return CarEngine(
            id = id.toInt(),
            manufacturer = manufacturer,
            code = code,
            displacementCc = displacementCc,
            powerHp = powerHp,
            cylinders = cylinders,
            valves = valves,
            fuelType = fuelType,
            userCreated = false
        )
    }
}