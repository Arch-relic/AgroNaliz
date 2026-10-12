package ru.fo6osik.workjournal

/** Pure calculation; lower/upper bounds retain the existing bunker-fill estimates. */
object HarvestCalculator {
    data class Range(val min: Double, val max: Double) {
        init { require(min.isFinite() && max.isFinite() && min >= 0 && max >= min) }
        fun asPair() = min to max
    }

    data class DayInput(val fillLevels: List<String>, val harvestedAreaHa: Double?)
    data class Result(
        val bunkerCount: Int,
        val volumeM3: Range?,
        val massTonnes: Range?,
        val knownAreaHa: Double,
        val areasComplete: Boolean,
        val yieldTonnesPerHa: Range?,
        val yieldCentnersPerHa: Range?
    )

    fun positive(value: Double?): Double? = value?.takeIf { it.isFinite() && it > 0 }

    /** Empty means unknown. Reject signs, exponents, mixed separators and non-finite values. */
    fun parseOptionalPositive(text: String): Double? {
        val input = text.trim()
        if (input.isEmpty()) return null
        require(Regex("[0-9]+([.,][0-9]+)?").matches(input)) { "Введите положительное число" }
        return requireNotNull(positive(input.replace(',', '.').toDoubleOrNull())) {
            "Введите положительное число"
        }
    }

    fun volume(bunkerVolumeM3: Double?, fillLevels: List<String>): Range? {
        val capacity = positive(bunkerVolumeM3) ?: return null
        var min = 0.0
        var max = 0.0
        for (level in fillLevels) {
            val coefficient = when (level) {
                "FULL" -> 1.0 to 1.0
                "HALF" -> 0.5 to 0.5
                "MORE_THAN_HALF" -> 0.5 to 1.0
                "LESS_THAN_HALF" -> 0.0 to 0.5
                else -> return null // Corrupt/unknown levels must not silently contribute zero.
            }
            min += coefficient.first * capacity
            max += coefficient.second * capacity
        }
        return finiteRange(min, max)
    }

    fun averageVolume(bunkerVolumeM3: Double?, fillLevels: List<String>): Range? {
        if (fillLevels.isEmpty()) return null
        return volume(bunkerVolumeM3, fillLevels)?.let {
            finiteRange(it.min / fillLevels.size, it.max / fillLevels.size)
        }
    }

    fun day(bunkerVolumeM3: Double?, densityKgM3: Double?, input: DayInput): Result =
        campaign(bunkerVolumeM3, densityKgM3, listOf(input))

    fun campaign(bunkerVolumeM3: Double?, densityKgM3: Double?, days: List<DayInput>): Result {
        val levels = days.flatMap { it.fillLevels }
        val volume = volume(bunkerVolumeM3, levels)
        val density = positive(densityKgM3)
        val mass = if (volume != null && density != null) {
            finiteRange(volume.min * (density / 1000), volume.max * (density / 1000))
        } else null
        val knownAreas = days.mapNotNull { positive(it.harvestedAreaHa) }
        val area = knownAreas.sum()
        val complete = days.isNotEmpty() && knownAreas.size == days.size && positive(area) != null
        val tonnesYield = if (complete && mass != null) {
            finiteRange(mass.min / area, mass.max / area)
        } else null
        val centnersYield = tonnesYield?.let { finiteRange(it.min * 10, it.max * 10) }
        return Result(levels.size, volume, mass, area, complete, tonnesYield, centnersYield)
    }

    private fun finiteRange(min: Double, max: Double): Range? =
        if (min.isFinite() && max.isFinite() && min >= 0 && max >= min) Range(min, max) else null
}
