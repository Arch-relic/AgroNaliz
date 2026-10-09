package ru.fo6osik.workjournal

object TractorCatalogSeeder {

    suspend fun seedBaseCatalog(
        database: AppDatabase
    ) {

        val tractorDao =
            database.tractorCatalogDao()

        val engineDao =
            database.carCatalogDao()


        // =========================
        // КИРОВЕЦ
        // =========================

        val kirovets =
            ensureBrand(
                tractorDao,
                "КИРОВЕЦ"
            )


        // -------------------------
        // К-424
        // -------------------------

        val k424 =
            ensureModel(
                tractorDao,
                kirovets.id,
                "К-424"
            )

        val ymz53625 =
            ensureEngine(
                engineDao,
                manufacturer = "ЯМЗ",
                code = "53625",
                displacementCc = 6650,
                powerHp = 240,
                cylinders = 6,
                valves = null,
                fuelType = "Дизель"
            )

        tractorDao.insertModelEngine(
            TractorModelEngine(
                modelId = k424.id,
                engineId = ymz53625.id
            )
        )


        // -------------------------
// К-525 и К-525 Пр
// -------------------------

val k525 =
    ensureModel(
        tractorDao,
        kirovets.id,
        "К-525"
    )

val k525Pr =
    ensureModel(
        tractorDao,
        kirovets.id,
        "К-525 Пр"
    )
	
	tractorDao.updateBaseModel(
    modelId = k525Pr.id,
    baseModelId = k525.id
)

val ymz53645 =
    ensureEngine(
        engineDao,
        manufacturer = "ЯМЗ",
        code = "53645",
        displacementCc = 6650,
        powerHp = 250,
        cylinders = 6,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = k525.id,
        engineId = ymz53645.id
    )
)

val ymz53645Pr =
    ensureEngine(
        engineDao,
        manufacturer = "ЯМЗ",
        code = "53645-31",
        displacementCc = 6650,
        powerHp = 250,
        cylinders = 6,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = k525Pr.id,
        engineId = ymz53645Pr.id
    )
)


        // -------------------------
        // К-530Т
        // -------------------------

        val k530t =
            ensureModel(
                tractorDao,
                kirovets.id,
                "К-530Т"
            )

        val ymz53715 =
            ensureEngine(
                engineDao,
                manufacturer = "ЯМЗ",
                code = "53715-10",
                displacementCc = 7700,
                powerHp = 300,
                cylinders = 6,
                valves = null,
                fuelType = "Дизель"
            )

        tractorDao.insertModelEngine(
            TractorModelEngine(
                modelId = k530t.id,
                engineId = ymz53715.id
            )
        )
	
	// =========================
// КИРОВЕЦ К-7М
// =========================


// -------------------------
// К-730М
// -------------------------

val k730m =
    ensureModel(
        tractorDao,
        kirovets.id,
        "К-730М"
    )

val tmz84811011 =
    ensureEngine(
        engineDao,
        manufacturer = "ТМЗ",
        code = "8481.10-11",
        displacementCc = 17240,
        powerHp = 300,
        cylinders = 8,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = k730m.id,
        engineId = tmz84811011.id
    )
)


// -------------------------
// К-735М
// -------------------------

val k735m =
    ensureModel(
        tractorDao,
        kirovets.id,
        "К-735М"
    )

val tmz848110 =
    ensureEngine(
        engineDao,
        manufacturer = "ТМЗ",
        code = "8481.10",
        displacementCc = 17240,
        powerHp = 350,
        cylinders = 8,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = k735m.id,
        engineId = tmz848110.id
    )
)


// -------------------------
// К-739М
// -------------------------

val k739m =
    ensureModel(
        tractorDao,
        kirovets.id,
        "К-739М"
    )

val tmz84811002 =
    ensureEngine(
        engineDao,
        manufacturer = "ТМЗ",
        code = "8481.10-02",
        displacementCc = 17240,
        powerHp = 390,
        cylinders = 8,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = k739m.id,
        engineId = tmz84811002.id
    )
)


// -------------------------
// К-742М
// -------------------------

val k742m =
    ensureModel(
        tractorDao,
        kirovets.id,
        "К-742М"
    )

val tmz84811004 =
    ensureEngine(
        engineDao,
        manufacturer = "ТМЗ",
        code = "8481.10-04",
        displacementCc = 17240,
        powerHp = 420,
        cylinders = 8,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = k742m.id,
        engineId = tmz84811004.id
    )
)


// -------------------------
// К-743М
// -------------------------

val k743m =
    ensureModel(
        tractorDao,
        kirovets.id,
        "К-743М"
    )

val weichai430 =
    ensureEngine(
        engineDao,
        manufacturer = "Weichai",
        code = "WP12G430E300",
        displacementCc = 11600,
        powerHp = 430,
        cylinders = 6,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = k743m.id,
        engineId = weichai430.id
    )
)


// -------------------------
// К-746М
// -------------------------

val k746m =
    ensureModel(
        tractorDao,
        kirovets.id,
        "К-746М"
    )

val weichai460 =
    ensureEngine(
        engineDao,
        manufacturer = "Weichai",
        code = "WP12G460E300",
        displacementCc = 11600,
        powerHp = 460,
        cylinders = 6,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = k746m.id,
        engineId = weichai460.id
    )
)

// =========================
// КИРОВЕЦ К-7МК
// Двигатели КАМАЗ
// =========================


// -------------------------
// К-740МК
// -------------------------

val k740mk =
    ensureModel(
        tractorDao,
        kirovets.id,
        "К-740МК"
    )

val kamaz96080400 =
    ensureEngine(
        engineDao,
        manufacturer = "КАМАЗ",
        code = "960.80-400",
        displacementCc = 12980,
        powerHp = 400,
        cylinders = 6,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = k740mk.id,
        engineId = kamaz96080400.id
    )
)


// -------------------------
// К-743МК
// -------------------------

val k743mk =
    ensureModel(
        tractorDao,
        kirovets.id,
        "К-743МК"
    )

val kamaz96081430 =
    ensureEngine(
        engineDao,
        manufacturer = "КАМАЗ",
        code = "960.81-430",
        displacementCc = 12980,
        powerHp = 430,
        cylinders = 6,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = k743mk.id,
        engineId = kamaz96081430.id
    )
)


// -------------------------
// К-746МК
// -------------------------

val k746mk =
    ensureModel(
        tractorDao,
        kirovets.id,
        "К-746МК"
    )

val kamaz96082460 =
    ensureEngine(
        engineDao,
        manufacturer = "КАМАЗ",
        code = "960.82-460",
        displacementCc = 12980,
        powerHp = 460,
        cylinders = 6,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = k746mk.id,
        engineId = kamaz96082460.id
    )
)

// =========================
// БЕЛАРУС / МТЗ
// =========================

val belarus =
    ensureBrand(
        tractorDao,
        "БЕЛАРУС"
    )


// -------------------------
// БЕЛАРУС 82.1
// -------------------------

val belarus821 =
    ensureModel(
        tractorDao,
        belarus.id,
        "82.1"
    )

val mmzD243 =
    ensureEngine(
        engineDao,
        manufacturer = "ММЗ",
        code = "Д-243",
        displacementCc = 4750,
        powerHp = 81,
        cylinders = 4,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = belarus821.id,
        engineId = mmzD243.id
    )
)


// -------------------------
// БЕЛАРУС 952.3
// -------------------------

val belarus9523 =
    ensureModel(
        tractorDao,
        belarus.id,
        "952.3"
    )

val mmzD2455S2 =
    ensureEngine(
        engineDao,
        manufacturer = "ММЗ",
        code = "Д-245.5S2",
        displacementCc = 4750,
        powerHp = 95,
        cylinders = 4,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = belarus9523.id,
        engineId = mmzD2455S2.id
    )
)

// -------------------------
// БЕЛАРУС 1021.3
// -------------------------

val belarus10213 =
    ensureModel(
        tractorDao,
        belarus.id,
        "1021.3"
    )

val mmzD245S2_110 =
    ensureEngine(
        engineDao,
        manufacturer = "ММЗ",
        code = "Д-245S2",
        displacementCc = 4750,
        powerHp = 110,
        cylinders = 4,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = belarus10213.id,
        engineId = mmzD245S2_110.id
    )
)


// -------------------------
// БЕЛАРУС 1021.5
// -------------------------

val belarus10215 =
    ensureModel(
        tractorDao,
        belarus.id,
        "1021.5"
    )

val mmzD245S3B =
    ensureEngine(
        engineDao,
        manufacturer = "ММЗ",
        code = "Д-245S3B",
        displacementCc = 4750,
        powerHp = 110,
        cylinders = 4,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = belarus10215.id,
        engineId = mmzD245S3B.id
    )
)


// -------------------------
// БЕЛАРУС 1221.3
// -------------------------

val belarus12213 =
    ensureModel(
        tractorDao,
        belarus.id,
        "1221.3"
    )

val mmzD2602S2 =
    ensureEngine(
        engineDao,
        manufacturer = "ММЗ",
        code = "Д-260.2S2",
        displacementCc = 7120,
        powerHp = 136,
        cylinders = 6,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = belarus12213.id,
        engineId = mmzD2602S2.id
    )
)


// -------------------------
// БЕЛАРУС 2022.3
// -------------------------

val belarus20223 =
    ensureModel(
        tractorDao,
        belarus.id,
        "2022.3"
    )

val mmzD2604S2 =
    ensureEngine(
        engineDao,
        manufacturer = "ММЗ",
        code = "Д-260.4S2",
        displacementCc = 7120,
        powerHp = 212,
        cylinders = 6,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = belarus20223.id,
        engineId = mmzD2604S2.id
    )
)

// =========================
// БЕЛАРУС
// Семейство 80 / 82 / 892 / 952
// =========================


// -------------------------
// БЕЛАРУС 80.1
// -------------------------

val belarus801 =
    ensureModel(
        tractorDao,
        belarus.id,
        "80.1"
    )

// Д-243 уже используется на БЕЛАРУС 82.1,
// поэтому используем существующий объект mmzD243

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = belarus801.id,
        engineId = mmzD243.id
    )
)


// -------------------------
// Д-245.5 — 88 л.с.
// -------------------------

val mmzD2455_88 =
    ensureEngine(
        engineDao,
        manufacturer = "ММЗ",
        code = "Д-245.5",
        displacementCc = 4750,
        powerHp = 88,
        cylinders = 4,
        valves = null,
        fuelType = "Дизель"
    )


// -------------------------
// БЕЛАРУС 892
// -------------------------

val belarus892 =
    ensureModel(
        tractorDao,
        belarus.id,
        "892"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = belarus892.id,
        engineId = mmzD2455_88.id
    )
)


// -------------------------
// БЕЛАРУС 952
// -------------------------

val belarus952 =
    ensureModel(
        tractorDao,
        belarus.id,
        "952"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = belarus952.id,
        engineId = mmzD2455_88.id
    )
)


// -------------------------
// БЕЛАРУС 952.2
// -------------------------

val belarus9522 =
    ensureModel(
        tractorDao,
        belarus.id,
        "952.2"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = belarus9522.id,
        engineId = mmzD2455_88.id
    )
)


// -------------------------
// БЕЛАРУС 892.2
// -------------------------

val belarus8922 =
    ensureModel(
        tractorDao,
        belarus.id,
        "892.2"
    )

val mmzD2455_90 =
    ensureEngine(
        engineDao,
        manufacturer = "ММЗ",
        code = "Д-245.5",
        displacementCc = 4750,
        powerHp = 90,
        cylinders = 4,
        valves = null,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = belarus8922.id,
        engineId = mmzD2455_90.id
    )
)

// =========================
// РОСТСЕЛЬМАШ
// =========================

val rostselmash =
    ensureBrand(
        tractorDao,
        "РОСТСЕЛЬМАШ"
    )


// -------------------------
// 2375
// -------------------------

val rostselmash2375 =
    ensureModel(
        tractorDao,
        rostselmash.id,
        "2375"
    )

val cumminsQsm11 =
    ensureEngine(
        engineDao,
        manufacturer = "Cummins",
        code = "QSM11",
        displacementCc = 10800,
        powerHp = 375,
        cylinders = 6,
        valves = 24,
        fuelType = "Дизель"
    )

tractorDao.insertModelEngine(
    TractorModelEngine(
        modelId = rostselmash2375.id,
        engineId = cumminsQsm11.id
    )
)

}

    private suspend fun ensureBrand(
        dao: TractorCatalogDao,
        name: String
    ): TractorBrand {

        val existing =
            dao.getBrandByName(name)

        if (existing != null) {
            return existing
        }

        dao.insertBrand(
            TractorBrand(
                name = name,
                userCreated = false
            )
        )

        return dao.getBrandByName(name)!!
    }


    private suspend fun ensureModel(
        dao: TractorCatalogDao,
        brandId: Int,
        name: String
    ): TractorModel {

        val existing =
            dao.getModelByName(
                brandId,
                name
            )

        if (existing != null) {
            return existing
        }

        dao.insertModel(
            TractorModel(
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