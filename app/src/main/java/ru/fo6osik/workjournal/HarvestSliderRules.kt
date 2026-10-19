package ru.fo6osik.workjournal

import kotlin.math.abs

object HarvestSliderRules {
    val maxima = listOf(120, 15, 60, 60, 120)
    val defaults = listOf(30, 5, 15, 15, 30)
    val presets = listOf(setOf(30,40,50,60), setOf(5), setOf(10,15,20), setOf(10,15,20), setOf(30,40,60))
    fun example(v: List<Int>) = "Обычный цикл: ${v[0]} + ${v[1]} = ${v[0]+v[1]} мин\n" +
        "Обед: ${v[0]} + ${v[1]} + ${v[2]} = ${v[0]+v[1]+v[2]} мин\n" +
        "Ужин: ${v[0]} + ${v[1]} + ${v[3]} = ${v[0]+v[1]+v[3]} мин\n" +
        "Ожидание транспорта: ${v[0]} + ${v[1]} + ${v[4]} = ${v[0]+v[1]+v[4]} мин"
}

/** Opening and changing step never normalize old data. Only an explicit movement selects a new value. */
class HarvestSliderValue(val min: Int, val max: Int, var step: Int, initial: Double?) {
    var value: Double? = initial; private set
    fun move(raw: Int) {
        require(step == 1 || step == 5)
        val grid = (min..max).filter { it == min || it % step == 0 }
        value = grid.minByOrNull { abs(it.toLong() - raw.coerceIn(min, max)) }!!.toDouble()
    }
}
