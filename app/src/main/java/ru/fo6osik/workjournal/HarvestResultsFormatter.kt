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
        val mass = result.massTonnes?.let { "≈ ${range(it, "т")}" }
            ?: "Не рассчитана: проверьте объём бункера, заполнение и плотность культуры"
        val area = if (!campaign && !result.areasComplete) "Не указана"
            else if (!result.knownAreaHa.isFinite()) "Не рассчитана: слишком большое значение"
            else "${number(result.knownAreaHa)} га"
        val tonnes = result.yieldTonnesPerHa
        val centners = result.yieldCentnersPerHa
        val yield = if (tonnes != null && centners != null)
            "≈ ${range(centners, "ц/га")} (${range(tonnes, "т/га")})"
        else "Не рассчитана"
        val incomplete = if (campaign && (!result.areasComplete || !result.massesComplete))
            "\nНеполные данные: масса — сумма только известных масс; урожайность — только по дням с известными массой и площадью (${result.includedYieldDays} из ${result.dayCount})." else ""
        val areaLabel = if (campaign) "Сумма известных убранных площадей" else "Фактически убранная площадь"
        return "Приблизительная масса: $mass\n$areaLabel: $area\nУрожайность: $yield$incomplete" +
            "\nОценка зависит от влажности, засорённости и состояния зерна."
    }
}
