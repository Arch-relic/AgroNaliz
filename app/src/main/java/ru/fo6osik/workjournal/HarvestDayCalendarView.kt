package ru.fo6osik.workjournal

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Locale

class HarvestDayCalendarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    val calendar = HarvestDayCalendar()
    var onOpenDay: ((Int, String) -> Unit)? = null
    init { orientation = VERTICAL }

    fun update(days: List<HarvestDaySummary>, month: String? = calendar.month, date: String? = calendar.selectedDate) {
        calendar.update(days, month, date)
        render()
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun label(value: String) = TextView(context).apply {
        text = value; textSize = 16f; setTextColor(Color.BLACK); gravity = Gravity.CENTER
        setPadding(dp(2), dp(8), dp(2), dp(8))
    }
    private fun render() {
        removeAllViews()
        visibility = if (calendar.month == null) View.GONE else View.VISIBLE
        val month = calendar.month ?: return
        val header = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        val previous = Button(context).apply {
            tag = "harvest-calendar-previous"; text = "‹"; contentDescription = "Предыдущий месяц с записями"
            visibility = if (calendar.months.size > 1) View.VISIBLE else View.GONE
            isEnabled = calendar.months.indexOf(month) > 0
            setOnClickListener { calendar.move(-1); render() }
        }
        val next = Button(context).apply {
            tag = "harvest-calendar-next"; text = "›"; contentDescription = "Следующий месяц с записями"
            visibility = previous.visibility; isEnabled = calendar.months.indexOf(month) < calendar.months.lastIndex
            setOnClickListener { calendar.move(1); render() }
        }
        val parsed = SimpleDateFormat("yyyy-MM", Locale.ROOT).parse(month)!!
        val title = label(SimpleDateFormat("LLLL yyyy", Locale.forLanguageTag("ru-RU")).format(parsed)).apply {
            tag = "harvest-calendar-month"; setTypeface(typeface, Typeface.BOLD)
        }
        header.addView(previous, LayoutParams(dp(48), LayoutParams.WRAP_CONTENT))
        header.addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        header.addView(next, LayoutParams(dp(48), LayoutParams.WRAP_CONTENT)); addView(header)
        val week = LinearLayout(context)
        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach {
            week.addView(label(it), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
        addView(week)
        calendar.cells().chunked(7).forEach { dates ->
            val row = LinearLayout(context)
            dates.forEach { date ->
                val summary = date?.let(calendar::summary)
                val selected = date != null && date == calendar.selectedDate
                val cell = label(date?.takeLast(2)?.toInt()?.toString() ?: "").apply {
                    tag = date?.let { "harvest-date-$it" }; minHeight = dp(48)
                    isEnabled = summary != null; isClickable = summary != null; isFocusable = summary != null
                    isSelected = selected
                    if (date == null) importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    val mark = when (summary?.mark) {
                        HarvestDayCalendar.Mark.GREEN -> "площадь указана"
                        HarvestDayCalendar.Mark.AMBER -> "неполные данные площади"
                        null -> "нет рабочих записей"
                    }
                    contentDescription = date?.let { "$it, $mark${if (selected) ", выбрано" else ""}" }
                    setTextColor(if (summary == null) Color.DKGRAY else Color.BLACK)
                    background = GradientDrawable().apply {
                        setColor(when (summary?.mark) {
                            HarvestDayCalendar.Mark.GREEN -> Color.rgb(200, 230, 201)
                            HarvestDayCalendar.Mark.AMBER -> Color.rgb(255, 224, 178)
                            null -> Color.TRANSPARENT
                        })
                        cornerRadius = dp(4).toFloat()
                        if (selected) setStroke(dp(3), Color.rgb(21, 101, 192))
                    }
                    if (summary != null) setOnClickListener { calendar.toggle(date!!); render() }
                }
                row.addView(cell, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            }
            addView(row)
        }
        calendar.selected?.let { summary ->
            val card = label(summaryText(summary)).apply {
                tag = "harvest-date-summary"; gravity = Gravity.START; setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundColor(Color.rgb(242, 242, 242))
                if (summary.records.size == 1) {
                    isClickable = true; isFocusable = true
                    setOnClickListener { onOpenDay?.invoke(summary.records.single().id, summary.date) }
                }
            }
            addView(card)
            if (summary.records.size > 1) summary.records.forEach { record ->
                val text = "№${record.id} · ${record.startTime}–${record.endTime ?: "—"} · ${if (record.status == "ACTIVE") "Текущий день" else "Завершён"}"
                addView(label(text).apply {
                    tag = "harvest-record-${record.id}"; isClickable = true; isFocusable = true
                    setOnClickListener { onOpenDay?.invoke(record.id, summary.date) }
                })
            }
        }
    }
    private fun summaryText(summary: HarvestDayCalendar.Summary): String {
        val area = summary.areaHa?.let { "${HarvestResultsFormatter.number(it)} га" } ?: "— (неполные данные)"
        return "${summary.date.takeLast(2)}.${summary.date.substring(5, 7)}.${summary.date.take(4)}\n" +
            "Бункеров: ${summary.bunkerCount}\nУбрано: $area\n${summary.status}\n" +
            "${summary.start ?: "—"}–${summary.end ?: "—"}"
    }
}
