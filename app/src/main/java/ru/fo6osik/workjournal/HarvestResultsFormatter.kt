package ru.fo6osik.workjournal

import java.util.Locale

/** Presentation only: every numeric result comes from HarvestCalculator. */
object HarvestResultsFormatter {
    fun number(value: Double): String {
        // A positive fractional area must never be presented as a stored zero.
        if (value > 0 && value < 0.005) {
            return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')
        }
        return String.format(Locale.forLanguageTag("ru-RU"), "%.2f", value)
            .trimEnd('0').trimEnd(',').trimEnd('.')
    }

    fun range(value: HarvestCalculator.Range, units: String): String =
        if (value.min == value.max) "${number(value.min)} $units"
        else "${number(value.min)}–${number(value.max)} $units"

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
