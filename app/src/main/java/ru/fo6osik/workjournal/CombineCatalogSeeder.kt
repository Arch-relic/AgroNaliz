package ru.fo6osik.workjournal

object CombineCatalogSeeder {

    suspend fun seedBaseCatalog(
        database: AppDatabase
    ) {

        val combineDao =
            database.combineCatalogDao()

        val engineDao =
            database.carCatalogDao()


        // =========================
        // РОСТСЕЛЬМАШ
        // =========================

        val rostselmash =
            ensureBrand(
                combineDao,
                "Ростсельмаш"
            )


        // -------------------------
        // NOVA 340
        // -------------------------

        val nova340 =
            ensureModel(
                combineDao,
                rostselmash.id,
                "NOVA 340"
            )


        val yamz53425 =
            ensureEngine(
                engineDao,
                manufacturer = "ЯМЗ",
                code = "53425",
                displacementCc = 4430,
                powerHp = 180,
                cylinders = 4,
                valves = null,
                fuelType = "Дизель"
            )


        combineDao.insertModelEngine(
            CombineModelEngine(
                modelId = nova340.id,
                engineId = yamz53425.id
            )
        )
    }


    private suspend fun ensureBrand(
        dao: CombineCatalogDao,
        name: String
    ): CombineBrand {

        val existing =
            dao.getBrandByName(
                name
            )

        if (existing != null) {
            return existing
        }

        dao.insertBrand(
            CombineBrand(
                name = name,
                userCreated = false
            )
        )

        return dao.getBrandByName(
            name
        )!!
    }


    private suspend fun ensureModel(
        dao: CombineCatalogDao,
        brandId: Int,
        name: String
    ): CombineModel {

        val existing =
            dao.getModelByName(
                brandId,
                name
            )

        if (existing != null) {
            return existing
        }

        dao.insertModel(
            CombineModel(
                brandId = brandId,
                name = name,
                userCreated = false
            )
        )

        return dao.getModelByName(
            brandId,
            name
        )!!
    }


    private suspend fun ensureEngine(
        dao: CarCatalogDao,
        manufacturer: String,
        code: String,
        displacementCc: Int?,
        powerHp: Int,
        cylinders: Int?,
        valves: Int?,
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

        return dao.getEngine(
            manufacturer,
            code,
            powerHp
        )!!
    }
}