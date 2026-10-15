package ru.fo6osik.workjournal

/** Percent of tank capacity; independent of grain mass/density calculations. */
object HarvestFuelCalculator {
    data class Refill(val before: Double, val after: Double, val betweenDays: Boolean = false)
    data class Result(val consumed: Double?, val perHa: Double?, val warning: String?)
    fun valid(value: Double?) = value == null || (value.isFinite() && value in 0.0..100.0)
    fun parse(text: String): Double? {
        if (text.isBlank()) return null
        val number=text.trim().replace(',','.').toDoubleOrNull()
        require(number != null && valid(number)) { "Введите число от 0 до 100" }
        return number
    }
    fun calculate(start: Double?, end: Double?, refills: List<Refill>, area: Double?): Result {
        if (!valid(start) || !valid(end) || refills.any { !valid(it.before) || !valid(it.after) || it.after < it.before })
            return Result(null,null,"Проверьте показания: 0–100%, после заправки не ниже, чем до неё")
        if (start == null || end == null) return Result(null,null,null)
        val used=start+refills.filterNot { it.betweenDays }.sumOf { it.after-it.before }-end
        if (!used.isFinite() || used < -0.0000001) return Result(null,null,"Расхождение показаний: расход отрицательный. Проверьте уровни и заправки за день")
        val consumption=used.coerceAtLeast(0.0)
        val per=HarvestCalculator.positive(area)?.let { (consumption/it).takeIf(Double::isFinite) }
        return Result(consumption,per,null)
    }
}
