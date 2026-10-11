package ru.fo6osik.workjournal

/** Indicative bulk densities, kg/m³, not laboratory measurements. See docs/harvest-results.md. */
object HarvestDensityCatalog {
    private val densities = mapOf(
        "Пшеница" to 750.0,
        "Ячмень" to 600.0,
        "Горох" to 750.0,
        "Подсолнечник" to 400.0,
        "Кукуруза" to 720.0,
        "Рапс" to 650.0,
        "Овёс" to 450.0,
        "Рожь" to 700.0
    )

    fun reference(crop: String): Double? = densities[crop]

    /** A supplied but invalid override must not be replaced by an assumed reference value. */
    fun effective(crop: String, overrideKgM3: Double?): Double? =
        if (overrideKgM3 == null) reference(crop) else HarvestCalculator.positive(overrideKgM3)
}
