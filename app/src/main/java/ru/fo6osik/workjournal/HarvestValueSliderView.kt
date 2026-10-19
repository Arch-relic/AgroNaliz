package ru.fo6osik.workjournal

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class HarvestValueSliderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val label = TextView(context).apply { textSize = 18f; setSingleLine(false) }
    val seekBar = SeekBar(context)
    lateinit var state: HarvestSliderValue; private set
    private var title = ""
    private var units = ""
    var onValueChanged: (() -> Unit)? = null
    init { orientation = VERTICAL; addView(label); addView(seekBar, LayoutParams(LayoutParams.MATCH_PARENT, (64 * resources.displayMetrics.density).toInt())) }
    fun bind(title: String, units: String, state: HarvestSliderValue) {
        this.title = title; this.units = units; this.state = state
        seekBar.max = state.max - state.min
        refresh()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) { if (fromUser) choose(progress + state.min) }
            override fun onStartTrackingTouch(bar: SeekBar) {}
            override fun onStopTrackingTouch(bar: SeekBar) {
                // A deliberate interaction at an endpoint must also select it when the
                // old value is unknown/outside the track and Android reports no progress change.
                if (state.value == null || state.value!! !in state.min.toDouble()..state.max.toDouble())
                    choose(bar.progress + state.min)
            }
        })
    }
    fun choose(raw: Int) { state.move(raw); refresh(); onValueChanged?.invoke() }
    private fun refresh() {
        val value = state.value
        label.text = "$title: ${value?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString().replace('.', ',') } ?: "—"} $units"
        seekBar.progress = ((value ?: state.min.toDouble()).coerceIn(state.min.toDouble(),state.max.toDouble()) - state.min).toInt()
        seekBar.contentDescription = label.text
    }
}
