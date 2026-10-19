package ru.fo6osik.workjournal

/** Area totals use completed days; mass includes every recorded day, including the active shift. */
object HarvestCampaignMetrics {
    enum class AreaSource { COMPLETED_DAYS, ACTUAL_PLANTED, FIELD_SNAPSHOT, UNKNOWN }
    data class SelectedArea(val hectares: Double?, val source: AreaSource)

    /** A final day total requires a nonempty, complete list, never just its known subset. */
    fun selectArea(campaignCompleted: Boolean, actual: Double?, fieldSnapshot: Double?,
                   allDays: List<HarvestCalculator.DayInput>): SelectedArea {
        val areas = allDays.mapNotNull { HarvestCalculator.positive(it.harvestedAreaHa) }
        if (campaignCompleted && allDays.isNotEmpty() && areas.size == allDays.size) {
            HarvestCalculator.positive(areas.sum())?.let { return SelectedArea(it, AreaSource.COMPLETED_DAYS) }
        }
        HarvestCalculator.positive(actual)?.let { return SelectedArea(it, AreaSource.ACTUAL_PLANTED) }
        HarvestCalculator.positive(fieldSnapshot)?.let { return SelectedArea(it, AreaSource.FIELD_SNAPSHOT) }
        return SelectedArea(null, AreaSource.UNKNOWN)
    }

    /** Progress has a stable planting target; a completed-day total is never a target. */
    fun selectTargetArea(actual: Double?, fieldSnapshot: Double?): SelectedArea =
        selectArea(false, actual, fieldSnapshot, emptyList())

    data class Result(
        val harvestedHa: Double, val unknownAreaDays: Int, val completedDays: Int, val activeDays: Int,
        val remainingHa: Double?, val progressPercent: Double?, val exceedsActual: Boolean,
        val mass: HarvestCalculator.Range?, val massesComplete: Boolean,
        val wholeAreaYield: HarvestCalculator.Range?, val harvestedAreaYield: HarvestCalculator.Range?,
        val selectedArea: SelectedArea, val progressComplete: Boolean,
        val yieldCentnersPerHa: HarvestCalculator.Range?,
        val bunkerCount: Int, val volumeM3: HarvestCalculator.Range?,
        val targetArea: SelectedArea = SelectedArea(null, AreaSource.UNKNOWN)
    )

    fun calculate(capacity: Double?, actual: Double?, completed: List<HarvestCalculator.DayInput>,
                  allDays: List<HarvestCalculator.DayInput>, campaignCompleted: Boolean = false,
                  fieldAreaSnapshotHa: Double? = null): Result {
        val areaResult = HarvestCalculator.campaign(capacity, completed)
        val massResult = HarvestCalculator.campaign(capacity, allDays)
        val area = areaResult.knownAreaHa
        val unknown = completed.count { HarvestCalculator.positive(it.harvestedAreaHa) == null }
        val selected = selectArea(campaignCompleted, actual, fieldAreaSnapshotHa, allDays)
        val denominator = selected.hectares
        val target = selectTargetArea(actual, fieldAreaSnapshotHa)
        val targetHa = target.hectares
        val completeArea = unknown == 0 && area.isFinite()
        val mass = massResult.massTonnes
        fun yieldFor(value: Double?): HarvestCalculator.Range? {
            val divisor = HarvestCalculator.positive(value) ?: return null
            if (!massResult.massesComplete || mass == null) return null
            val min = mass.min / divisor
            val max = mass.max / divisor
            return if (min.isFinite() && max.isFinite()) HarvestCalculator.Range(min, max) else null
        }
        val campaignYield = yieldFor(denominator)
        val centners = campaignYield?.let {
            val min = it.min * 10
            val max = it.max * 10
            if (min.isFinite() && max.isFinite()) HarvestCalculator.Range(min, max) else null
        }
        return Result(area, unknown, completed.size, allDays.size - completed.size,
            if (targetHa != null && area.isFinite()) targetHa - area else null,
            if (targetHa != null && area.isFinite()) (area / targetHa * 100).takeIf { it.isFinite() } else null,
            targetHa != null && area > targetHa, mass, massResult.massesComplete,
            campaignYield, if (completeArea) yieldFor(area) else null,
            selected, completeArea, centners, massResult.bunkerCount, massResult.volumeM3, target)
    }

    fun text(fieldAreaSnapshotHa: Double?, actualPlantedAreaHa: Double?, inProgress: Boolean, result: Result): String {
        fun area(value: Double?) = value?.let { "${HarvestResultsFormatter.number(it)} га" } ?: "Неизвестно"
        fun yield(value: HarvestCalculator.Range?) = value?.let { "≈ ${HarvestResultsFormatter.range(it, "т/га")}" } ?: "Не рассчитано"
        val incompleteArea = if (result.unknownAreaDays > 0) " (неполные данные: неизвестно у ${result.unknownAreaDays} из ${result.completedDays} дней)" else ""
        val massLabel = if (result.massesComplete) "Масса" else "Масса по известным данным (неполный расчёт)"
        val sourceLabel = when (result.selectedArea.source) {
            AreaSource.COMPLETED_DAYS -> "сумма гектаров всех рабочих дней"
            AreaSource.ACTUAL_PLANTED -> "фактическая площадь посева"
            AreaSource.FIELD_SNAPSHOT -> "снимок площади поля"
            AreaSource.UNKNOWN -> "площадь неизвестна"
        }
        val progressNote = if (!result.progressComplete) " (неполные данные)" else ""
        return listOf(
            "Площадь поля на момент создания: ${area(fieldAreaSnapshotHa)}",
            "Фактическая площадь посева: ${area(actualPlantedAreaHa)}",
            "Убрано по завершённым рабочим дням: ${if (result.harvestedHa.isFinite()) area(result.harvestedHa) else "Не рассчитано"}$incompleteArea",
            "Осталось убрать: ${result.remainingHa?.let { area(it) } ?: "Не рассчитано"}$progressNote",
            "Процент выполнения: ${result.progressPercent?.let { String.format(java.util.Locale.forLanguageTag("ru-RU"), "%.1f%%", it) } ?: "Не рассчитано"}$progressNote",
            "$massLabel: ${result.mass?.let { "≈ ${HarvestResultsFormatter.range(it, "т")}" } ?: "Не рассчитано"}",
            "Урожайность кампании ($sourceLabel)${if (inProgress) " (предварительная)" else ""}: ${yield(result.wholeAreaYield)}",
            "Урожайность по убранной площади: ${yield(result.harvestedAreaYield)}"
        ).joinToString("\n") +
            (if (result.activeDays > 0) "\nЕсть активный рабочий день: его масса включена, площадь будет учтена после завершения. Показатели предварительные." else "") +
            (if (result.exceedsActual) "\nВнимание: убранная площадь превышает целевую площадь кампании." else "")
    }
}
