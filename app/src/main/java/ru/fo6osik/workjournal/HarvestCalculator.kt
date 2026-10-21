package ru.fo6osik.workjournal

/** Pure fixed-percentage calculation. Range is retained as a compatible result container; bounds are equal.
 * Stored level codes are never rewritten: old records and restored backups use the same new scale. */
object HarvestCalculator {
    data class Range(val min: Double, val max: Double) {
        init { require(min.isFinite() && max.isFinite() && min >= 0 && max >= min) }
        fun asPair() = min to max
    }

    data class DayInput(val fillLevels: List<String>, val harvestedAreaHa: Double?, val bulkDensityKgM3: Double?)
    data class Result(
        val bunkerCount: Int,
        val volumeM3: Range?,
        val massTonnes: Range?,
        val knownAreaHa: Double,
        val areasComplete: Boolean,
        val massesComplete: Boolean,
        val includedYieldDays: Int,
        val dayCount: Int,
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
        // Sum quarter-units before multiplying; no rounding or persisted conversion per bunker.
        var quarters = 0L
        for (level in fillLevels) {
            quarters += when (level) {
                "FULL" -> 4
                "HALF" -> 2
                "MORE_THAN_HALF" -> 3
                "LESS_THAN_HALF" -> 1
                else -> return null // Unknown levels must not silently contribute zero.
            }
        }
        val total = capacity * (quarters / 4.0)
        return finiteRange(total, total)
    }

    fun averageVolume(bunkerVolumeM3: Double?, fillLevels: List<String>): Range? {
        if (fillLevels.isEmpty()) return null
        return volume(bunkerVolumeM3, fillLevels)?.let {
            finiteRange(it.min / fillLevels.size, it.max / fillLevels.size)
        }
    }

    fun day(bunkerVolumeM3: Double?, input: DayInput): Result = campaign(bunkerVolumeM3, listOf(input))

    fun campaign(bunkerVolumeM3: Double?, days: List<DayInput>): Result {
        val volume = volume(bunkerVolumeM3, days.flatMap { it.fillLevels })
        val masses = days.map { day ->
            val v = volume(bunkerVolumeM3, day.fillLevels)
            val d = positive(day.bulkDensityKgM3)
            if (v != null && d != null) finiteRange(v.min * (d / 1000), v.max * (d / 1000)) else null
        }
        val knownMasses = masses.filterNotNull()
        val mass = if (knownMasses.isEmpty()) null else finiteRange(knownMasses.sumOf { it.min }, knownMasses.sumOf { it.max })
        val knownAreas = days.mapNotNull { positive(it.harvestedAreaHa) }
        val area = knownAreas.sum()
        val included = days.indices.filter { positive(days[it].harvestedAreaHa) != null && masses[it] != null }
        val yieldArea = included.sumOf { days[it].harvestedAreaHa!! }
        val tonnes = if (included.isNotEmpty() && positive(yieldArea) != null) finiteRange(
            included.sumOf { masses[it]!!.min } / yieldArea,
            included.sumOf { masses[it]!!.max } / yieldArea
        ) else null
        return Result(days.sumOf { it.fillLevels.size }, volume, mass, area,
            days.isNotEmpty() && knownAreas.size == days.size && positive(area) != null,
            days.isNotEmpty() && knownMasses.size == days.size && mass != null,
            included.size, days.size, tonnes, tonnes?.let { finiteRange(it.min * 10, it.max * 10) })
    }

    private fun finiteRange(min: Double, max: Double): Range? =
        if (min.isFinite() && max.isFinite() && min >= 0 && max >= min) Range(min, max) else null
}
