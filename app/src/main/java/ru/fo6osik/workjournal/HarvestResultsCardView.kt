package ru.fo6osik.workjournal

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.text.style.ReplacementSpan
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class HarvestResultsCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    var expanded: Boolean = false; private set
    var currentModel: HarvestResultsCardModel? = null; private set
    init {
        orientation = VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = GradientDrawable().apply {
            setColor(Color.rgb(245, 245, 245)); cornerRadius = dp(12).toFloat(); setStroke(dp(1), Color.LTGRAY)
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    // Keep the original glyphs and formatting, but make each number/unit indivisible.
    private fun readable(value: String) = HarvestValueText.unbroken(value)
    fun setExpansion(value: Boolean) { expanded = value; render() }
    fun update(model: HarvestResultsCardModel) { currentModel = model; render() }
    private fun text(value: String, size: Float = 17f) = TextView(context).apply {
        text = readable(value); textSize = size; setTextColor(Color.BLACK)
        setPadding(dp(4), dp(4), dp(4), dp(4))
        // Wrap text and grow vertically, including on small screens with a large system font.
        setSingleLine(false)
        breakStrategy = android.text.Layout.BREAK_STRATEGY_SIMPLE
        hyphenationFrequency = android.text.Layout.HYPHENATION_FREQUENCY_NONE
    }
    // Keep exactly two rows of two cells; do not change the grid while measuring it.
    // The fixed 25/50/75/100% values are shorter than the former ranges.
    private fun render() {
        val model = currentModel ?: return
        removeAllViews()
        addView(text("Результаты уборки", 22f).apply { setTypeface(typeface, Typeface.BOLD) })
        addView(Button(context).apply {
            tag = "harvest-results-toggle"; isAllCaps = false
            text = if (expanded) "Свернуть ▴" else "Подробнее ▾"
            contentDescription = if (expanded) "Свернуть результаты уборки" else "Подробнее о результатах уборки"
            setOnClickListener { setExpansion(!expanded) }
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        if (model.shortNotice.isNotEmpty()) addView(text(model.shortNotice, 15f).apply { tag = "harvest-results-notice" })
        val grid = LinearLayout(context).apply { orientation = VERTICAL; tag = "harvest-results-grid" }
        model.overview.chunked(2).forEachIndexed { rowIndex, items ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            items.forEachIndexed { column, (label, value) ->
                val cell = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(4), dp(4), dp(4), dp(4)) }
                cell.addView(text(label, 16f))
                cell.addView(text(value, 18f).apply { tag = "harvest-result-${rowIndex * 2 + column}"; setTypeface(typeface, Typeface.BOLD) })
                row.addView(cell, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            }
            grid.addView(row)
        }
        addView(grid)
        val details = LinearLayout(context).apply {
            orientation = VERTICAL; tag = "harvest-results-details"
            visibility = if (expanded) View.VISIBLE else View.GONE
        }
        model.details.forEach { (label, value) -> details.addView(text("$label: $value")) }
        details.addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            tag = "harvest-results-progress"; max = 100; isIndeterminate = false
            progress = model.progressBar ?: 0
            visibility = if (model.progressBar == null) View.GONE else View.VISIBLE
            contentDescription = "Прогресс уборки: ${model.progressText}"
        }, LayoutParams(LayoutParams.MATCH_PARENT, dp(12)))
        model.warnings.forEach { details.addView(text(it, 16f).apply { setTextColor(Color.rgb(125, 60, 0)) }) }
        addView(details)
    }
}

internal class UnbrokenHarvestValue : ReplacementSpan() {
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int =
        kotlin.math.ceil(paint.measureText(text, start, end).toDouble()).toInt()
    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        canvas.drawText(text, start, end, x, y.toFloat(), paint)
    }
}
