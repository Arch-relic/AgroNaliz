package ru.fo6osik.workjournal

import java.util.Locale

/** Presentation only: formats the supplied metrics, never recalculates harvest quantities. */
data class HarvestResultsCardModel(
    val overview: List<Pair<String, String>>,
    val details: List<Pair<String, String>>,
    val shortNotice: String,
    val warnings: List<String>,
    val progressText: String,
    val progressBar: Int?
) {
    companion object {
        fun from(result: HarvestCampaignMetrics.Result, fieldSnapshot: Double?, actual: Double?, inProgress: Boolean): HarvestResultsCardModel {
            fun number(value: Double) = if (value < 0 && value > -0.005) "-${HarvestResultsFormatter.number(-value)}"
                else HarvestResultsFormatter.number(value)
            fun area(value: Double?) = value?.takeIf { it.isFinite() }?.let { "${number(it)} га" } ?: "—"
            fun range(value: HarvestCalculator.Range?, units: String) = value?.let { HarvestResultsFormatter.range(it, units) } ?: "—"
            fun source(value: HarvestCampaignMetrics.AreaSource) = when (value) {
                HarvestCampaignMetrics.AreaSource.COMPLETED_DAYS -> "Сумма гектаров рабочих дней"
                HarvestCampaignMetrics.AreaSource.ACTUAL_PLANTED -> "Фактическая площадь посева"
                HarvestCampaignMetrics.AreaSource.FIELD_SNAPSHOT -> "Площадь поля при создании кампании"
                HarvestCampaignMetrics.AreaSource.UNKNOWN -> "Неизвестно"
            }
            val progress = result.progressPercent?.let { String.format(Locale.forLanguageTag("ru-RU"), "%.1f%%", it) } ?: "—"
            val notes = mutableListOf<String>()
            if (result.unknownAreaDays > 0) notes += "Прогресс предварительный: площадь указана не для всех рабочих дней"
            if (result.targetArea.hectares == null) notes += "Целевая площадь неизвестна: прогресс и остаток не рассчитаны."
            if (!result.harvestedHa.isFinite()) notes += "Сумма площадей не рассчитана: данные выходят за допустимый диапазон."
            if (result.completedDays + result.activeDays == 0) notes += "Рабочих дней пока нет: масса и урожайность неизвестны."
            else if (!result.massesComplete) notes += "Масса рассчитана не полностью: отсутствуют данные об объёме бункера или плотности отдельных дней."
            if (result.wholeAreaYield == null || result.yieldCentnersPerHa == null)
                notes += "Урожайность не рассчитана полностью: недостаточно данных о массе или площади."
            if (result.activeDays > 0) notes += "Есть активный рабочий день: его масса уже включена в общий итог, а площадь будет учтена после завершения."
            if (result.exceedsActual) notes += "Убранная площадь превышает целевую. Процент выше 100% сохранён; отрицательный остаток означает превышение цели."
            val partial = if (!result.progressComplete) " (известная часть, неполные данные)" else ""
            return HarvestResultsCardModel(
                listOf("Площадь" to area(result.selectedArea.hectares), "Бункеры" to result.bunkerCount.toString(),
                    "Масса урожая" to (range(result.mass, "т") + if (result.mass != null && !result.massesComplete) " (частично)" else ""),
                    "Урожайность" to "${range(result.wholeAreaYield, "т/га")}\n${range(result.yieldCentnersPerHa, "ц/га")}"),
                listOf("Общий расчётный объём бункеров" to range(result.volumeM3, "м³"),
                    "Источник расчётной площади" to source(result.selectedArea.source),
                    "Площадь поля при создании кампании" to area(HarvestCalculator.positive(fieldSnapshot)),
                    "Фактическая площадь посева" to area(HarvestCalculator.positive(actual)),
                    "Убрано по завершённым рабочим дням" to (area(result.harvestedHa) + partial),
                    "Целевая площадь" to "${area(result.targetArea.hectares)} (${source(result.targetArea.source)})",
                    "Процент выполнения уборки" to progress,
                    "Осталось убрать" to (area(result.remainingHa) + partial),
                    "Завершённых дней с неизвестной площадью" to result.unknownAreaDays.toString()) +
                    (result.harvestedAreaYield?.let { listOf("Урожайность по убранной площади" to range(it, "т/га")) } ?: emptyList()),
                listOfNotNull(if (inProgress || result.activeDays > 0) "Предварительно" else null,
                    if (!result.massesComplete || result.unknownAreaDays > 0 || result.selectedArea.hectares == null || result.targetArea.hectares == null || !result.harvestedHa.isFinite() || result.wholeAreaYield == null || result.yieldCentnersPerHa == null) "Неполные данные" else if (result.exceedsActual) "Превышение цели" else null).joinToString(" · "),
                notes, progress, result.progressPercent?.coerceIn(0.0, 100.0)?.toInt()
            )
        }
    }
}
