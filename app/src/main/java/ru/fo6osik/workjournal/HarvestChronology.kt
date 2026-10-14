package ru.fo6osik.workjournal

/** HH:mm belongs to the start date; a clock time before start belongs to the next date.
 * The existing schema represents one work shift of less than 24 hours, not multiple days.
 */
object HarvestChronology {
    fun minutes(time: String): Int {
        require(Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(time)) { "Некорректное время: $time" }
        return time.substring(0, 2).toInt() * 60 + time.substring(3).toInt()
    }
    fun offset(start: String, time: String): Int = (minutes(time) - minutes(start) + 1440) % 1440

    fun validateEnd(start: String, end: String, bunkers: List<String>) {
        val endOffset = offset(start, end)
        val last = bunkers.maxByOrNull { offset(start, it) }
        require(last == null || endOffset >= offset(start, last)) {
            "Окончание $end раньше последнего бункера $last (начало дня $start)"
        }
    }

    fun validateBunker(start: String, end: String?, time: String) {
        val point = offset(start, time)
        require(end == null || point <= offset(start, end)) {
            "Бункер $time позже окончания дня $end (начало $start)"
        }
    }

    /** Preserve the calendar side of midnight of existing bunker times when changing start. */
    fun validateStartChange(oldStart: String, newStart: String, bunkers: List<String>) {
        val old = minutes(oldStart); val proposed = minutes(newStart)
        for (time in bunkers.sortedBy { offset(oldStart, it) }) {
            val absolute = old + offset(oldStart, time)
            require(proposed <= absolute) { "Начало $newStart позже зарегистрированного бункера $time" }
            require(absolute - proposed < 1440 && proposed + offset(newStart, time) == absolute) {
                "Начало $newStart меняет дату бункера $time через полночь; исправьте время явно"
            }
        }
    }
}
