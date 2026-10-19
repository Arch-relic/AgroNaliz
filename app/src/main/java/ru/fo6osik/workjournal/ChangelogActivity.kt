package ru.fo6osik.workjournal

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Pure bundled UI: no database, network, files or preferences. */
class ChangelogActivity : AppCompatActivity() {
    private lateinit var expansion: ChangelogExpansion
    private val details = mutableMapOf<String, LinearLayout>()
    private val headers = mutableMapOf<String, Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "История изменений"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        expansion = ChangelogExpansion(BuildConfig.VERSION_NAME,
            savedInstanceState?.getStringArrayList("changelog.expanded"))
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        setContentView(ScrollView(this).apply {
            id = R.id.changelogScroll
            isFillViewport = true
            fitsSystemWindows = true
            addView(content)
        })
        content.addView(TextView(this).apply {
            text = "История изменений\nТекущая версия: ${BuildConfig.VERSION_NAME}"
            textSize = 22f
            setPadding(0, 0, 0, 16)
        })
        for (entry in AppChangelog.entries) {
            val header = Button(this).apply {
                tag = "changelog-header-${entry.id}"
                isAllCaps = false
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                minHeight = (48 * resources.displayMetrics.density).toInt()
            }
            val body = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                tag = "changelog-details-${entry.id}"
            }
            fun text(value: String, bold: Boolean = false) = TextView(this).apply {
                text = value
                textSize = 16f
                setPadding(8, 8, 8, 8)
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            entry.releaseDate?.let { body.addView(text(it)) }
            body.addView(text(entry.summary))
            for ((category, changes) in entry.visibleCategories) {
                body.addView(text(category, true))
                changes.forEach { body.addView(text("• $it")) }
            }
            headers[entry.id] = header
            details[entry.id] = body
            header.setOnClickListener { expansion.toggle(entry.id); render(entry) }
            content.addView(header, LinearLayout.LayoutParams(-1, -2))
            content.addView(body, LinearLayout.LayoutParams(-1, -2))
            render(entry)
        }
    }

    private fun render(entry: ChangelogEntry) {
        val open = entry.id in expansion.expanded
        details.getValue(entry.id).visibility = if (open) View.VISIBLE else View.GONE
        headers.getValue(entry.id).text = "${if (open) "▾" else "▸"} ${entry.title}" +
            (entry.status?.let { " — $it" } ?: "")
        headers.getValue(entry.id).contentDescription = "${entry.title}, " +
            (entry.status?.let { "$it, " } ?: "") + if (open) "свернуть" else "раскрыть"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("changelog.expanded", ArrayList(expansion.expanded))
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
