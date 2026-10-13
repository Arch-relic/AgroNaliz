package ru.fo6osik.workjournal

/** Indicative kg/m³ supplied by the product specification; not laboratory measurements. */
object HarvestDensityCatalog {
    data class Values(val min: Double, val average: Double, val max: Double)
    val values = mapOf(
        "Пшеница" to Values(720.0, 770.0, 830.0),
        "Ячмень" to Values(580.0, 640.0, 700.0),
        "Горох" to Values(750.0, 780.0, 820.0),
        "Подсолнечник" to Values(320.0, 400.0, 440.0),
        "Кукуруза" to Values(680.0, 730.0, 780.0),
        "Рапс" to Values(620.0, 660.0, 700.0),
        "Овёс" to Values(400.0, 480.0, 550.0),
        "Рожь" to Values(680.0, 710.0, 750.0)
    )
    fun selected(crop: String, mode: String, custom: Double? = null): Double? {
        if (mode == "CUSTOM") return HarvestCalculator.positive(custom)
        val v = values[crop] ?: return null
        return when (mode) { "MIN" -> v.min; "AVERAGE" -> v.average; "MAX" -> v.max; else -> null }
    }
    fun initial(crop: String): Pair<String?, Double?> = selected(crop, "MIN").let {
        if (it == null) null to null else "MIN" to it
    }
}
