package ru.fo6osik.workjournal

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/** UI state only. Does not alter dates, statuses, areas or the one-day-per-date Room constraint. */
class HarvestDayCalendar {
    enum class Mark { GREEN, AMBER }
    data class Summary(val date: String, val records: List<HarvestDaySummary>) {
        val bunkerCount = records.sumOf { it.bunkerCount }
        val areaHa: Double? = records.map { HarvestCalculator.positive(it.harvestedAreaHa) }.let { areas ->
            if (areas.any { it == null }) null else HarvestCalculator.positive(areas.filterNotNull().sum())
        }
        val mark = if (records.all { HarvestCalculator.positive(it.harvestedAreaHa) != null }) Mark.GREEN else Mark.AMBER
        val status = when {
            records.all { it.status == "COMPLETED" } -> "Завершён"
            records.all { it.status == "ACTIVE" } -> "Текущий день"
            else -> "Есть активные записи"
        }
        val start = records.map { it.startTime }.filter { it.isNotBlank() }.minOrNull()
        val end = if (records.all { !it.endTime.isNullOrBlank() }) records.mapNotNull { it.endTime }.maxOrNull() else null
    }
    private var dates: Map<String, Summary> = emptyMap()
    var months: List<String> = emptyList(); private set
    var month: String? = null; private set
    var selectedDate: String? = null; private set
    val selected: Summary? get() = selectedDate?.let(dates::get)

    fun update(days: List<HarvestDaySummary>, preferredMonth: String? = month, preferredDate: String? = selectedDate) {
        dates = days.filter { validDate(it.workDate) }.groupBy { it.workDate }
            .mapValues { (date, records) -> Summary(date, records.sortedWith(compareBy({ it.startTime }, { it.id }))) }
        months = dates.keys.map { it.take(7) }.distinct().sorted()
        month = preferredMonth?.takeIf { it in months } ?: months.lastOrNull()
        selectedDate = preferredDate?.takeIf { it in dates && it.take(7) == month }
    }
    fun summary(date: String): Summary? = dates[date]
    fun toggle(date: String) {
        if (date !in dates || date.take(7) != month) return
        selectedDate = if (selectedDate == date) null else date
    }
    fun move(offset: Int) {
        val target = months.getOrNull(months.indexOf(month) + offset) ?: return
        if (target != month) { month = target; selectedDate = null }
    }
    /** Null cells pad full Monday-first weeks; real dates with no records remain disabled in the view. */
    fun cells(): List<String?> {
        val value = month ?: return emptyList()
        val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
            clear(); set(value.take(4).toInt(), value.takeLast(2).toInt() - 1, 1)
        }
        val offset = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val count = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val size = ((offset + count + 6) / 7) * 7
        return (0 until size).map { index ->
            val day = index - offset + 1
            if (day in 1..count) "$value-${day.toString().padStart(2, '0')}" else null
        }
    }
    companion object {
        private fun validDate(value: String): Boolean {
            if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value) || value.startsWith("0000")) return false
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
                isLenient = false; timeZone = TimeZone.getTimeZone("UTC")
            }
            return try { format.parse(value)?.let { format.format(it) == value } == true } catch (_: Exception) { false }
        }
    }
}
