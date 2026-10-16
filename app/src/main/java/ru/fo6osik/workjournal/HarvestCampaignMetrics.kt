package ru.fo6osik.workjournal

/** Area totals use completed days; mass includes every recorded day, including the active shift. */
object HarvestCampaignMetrics {
    data class Result(
        val harvestedHa: Double, val unknownAreaDays: Int, val completedDays: Int, val activeDays: Int,
        val remainingHa: Double?, val progressPercent: Double?, val exceedsActual: Boolean,
        val mass: HarvestCalculator.Range?, val massesComplete: Boolean,
        val wholeAreaYield: HarvestCalculator.Range?, val harvestedAreaYield: HarvestCalculator.Range?
    )

    fun calculate(capacity: Double?, actual: Double?, completed: List<HarvestCalculator.DayInput>,
                  allDays: List<HarvestCalculator.DayInput>): Result {
        val areaResult = HarvestCalculator.campaign(capacity, completed)
        val massResult = HarvestCalculator.campaign(capacity, allDays)
        val area = areaResult.knownAreaHa
        val unknown = completed.count { HarvestCalculator.positive(it.harvestedAreaHa) == null }
        val denominator = HarvestCalculator.positive(actual)
        val completeArea = unknown == 0 && area.isFinite()
        val mass = massResult.massTonnes
        fun yieldFor(value: Double?): HarvestCalculator.Range? {
            val divisor = HarvestCalculator.positive(value) ?: return null
            if (!massResult.massesComplete || mass == null) return null
            val min = mass.min / divisor
            val max = mass.max / divisor
            return if (min.isFinite() && max.isFinite()) HarvestCalculator.Range(min, max) else null
        }
        return Result(area, unknown, completed.size, allDays.size - completed.size,
            if (denominator != null && completeArea) denominator - area else null,
            if (denominator != null && completeArea) (area / denominator * 100).takeIf { it.isFinite() } else null,
            denominator != null && area > denominator, mass, massResult.massesComplete,
            yieldFor(denominator), if (completeArea) yieldFor(area) else null)
    }

    fun text(fieldAreaSnapshotHa: Double?, actualPlantedAreaHa: Double?, inProgress: Boolean, result: Result): String {
        fun area(value: Double?) = value?.let { "${HarvestResultsFormatter.number(it)} га" } ?: "Неизвестно"
        fun yield(value: HarvestCalculator.Range?) = value?.let { "≈ ${HarvestResultsFormatter.range(it, "т/га")}" } ?: "Не рассчитано"
        val incompleteArea = if (result.unknownAreaDays > 0) " (неполные данные: неизвестно у ${result.unknownAreaDays} из ${result.completedDays} дней)" else ""
        val massLabel = if (result.massesComplete) "Масса" else "Масса по известным данным (неполный расчёт)"
        return listOf(
            "Площадь поля на момент создания: ${area(fieldAreaSnapshotHa)}",
            "Фактическая площадь посева: ${area(actualPlantedAreaHa)}",
            "Убрано по завершённым рабочим дням: ${if (result.harvestedHa.isFinite()) area(result.harvestedHa) else "Не рассчитано"}$incompleteArea",
            "Осталось убрать: ${result.remainingHa?.let { area(it) } ?: "Не рассчитано"}",
            "Процент выполнения: ${result.progressPercent?.let { String.format(java.util.Locale.forLanguageTag("ru-RU"), "%.1f%%", it) } ?: "Не рассчитано"}",
            "$massLabel: ${result.mass?.let { "≈ ${HarvestResultsFormatter.range(it, "т")}" } ?: "Не рассчитано"}",
            "Урожайность по всей фактической площади${if (inProgress) " (предварительная)" else ""}: ${yield(result.wholeAreaYield)}",
            "Урожайность по убранной площади: ${yield(result.harvestedAreaYield)}"
        ).joinToString("\n") +
            (if (result.activeDays > 0) "\nЕсть активный рабочий день: его масса включена, площадь будет учтена после завершения. Показатели предварительные." else "") +
            (if (result.exceedsActual) "\nВнимание: убранная площадь превышает фактическую площадь посева." else "")
    }
}
