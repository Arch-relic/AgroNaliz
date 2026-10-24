package ru.fo6osik.workjournal

import android.text.SpannableString
import android.text.Spanned

/** Keep each measured quantity together without changing its text or copyable value. */
object HarvestValueText {
    fun unbroken(value: String) = SpannableString(value).apply {
        Regex("-?\\d+(?:[.,]\\d+)?(?:[ \\u00a0]+(?:кг/м³|т/га|ц/га|м³|га|т|ц))?").findAll(value).forEach {
            setSpan(UnbrokenHarvestValue(), it.range.first, it.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
