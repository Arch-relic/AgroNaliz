package ru.fo6osik.workjournal

import java.util.Locale

/** Presentation only: every numeric result comes from HarvestCalculator. */
object HarvestResultsFormatter {
    fun number(value: Double, decimals: Int = 2): String {
        // A positive fractional area must never be presented as a stored zero.
        if (value > 0 && value < 0.005) {
            return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')
        }
        return String.format(Locale.forLanguageTag("ru-RU"), "%.${decimals}f", value)
            .trimEnd('0').trimEnd(',').trimEnd('.')
    }

    fun range(value: HarvestCalculator.Range, units: String): String =
        if (value.min == value.max) "${number(value.min, if (units in setOf("м³", "т")) 3 else 2)} $units"
        else "—" // An unexpected interval is not an exact result; never invent a midpoint.

    fun density(value: Double?, mode: String?): String {
        val d = HarvestCalculator.positive(value) ?: return "Плотность не указана"
        val label = when(mode) { "MIN" -> "минимальная"; "AVERAGE" -> "средняя"; "MAX" -> "максимальная"; else -> "своё значение" }
        return "Насыпная плотность: ${number(d)} кг/м³ ($label)"
    }

    fun results(result: HarvestCalculator.Result, campaign: Boolean): String {
        val mass=result.massTonnes?.let { "≈ ${range(it,"т")}" } ?: "—"
        val area=if(result.knownAreaHa>0 && result.knownAreaHa.isFinite()) "${number(result.knownAreaHa)} га" else "—"
        val yield=result.yieldTonnesPerHa?.let { "≈ ${range(it,"т/га")}" } ?: "—"
        val incomplete=if(campaign && (!result.areasComplete || !result.massesComplete))
            "\nНеполные данные · урожайность по ${result.includedYieldDays}/${result.dayCount} дням" else ""
        return "Масса: $mass\nПлощадь: $area\nУрожайность: $yield$incomplete"
    }
}
