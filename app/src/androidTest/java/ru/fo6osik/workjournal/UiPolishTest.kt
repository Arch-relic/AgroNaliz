package ru.fo6osik.workjournal

import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/** Only the dedicated fresh CI emulator invokes this class, for MAIN and TEST/API23 and35. */
@RunWith(AndroidJUnit4::class)
class UiPolishTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun guard() {
        check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish"))
        check(context.packageName in setOf("ru.fo6osik.workjournal", "ru.fo6osik.workjournal.debugtest"))
    }
    @Suppress("DEPRECATION")
    @Test fun installedDiagnosticsBrandingCopyAndNoDatabaseChanges() {
        guard()
        fun databases() = context.databaseList().associateWith { context.getDatabasePath(it).readBytes().toList() }
        val before = databases()
        val info = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
        val expectedPins = info.signatures!!.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02X".format(it) }
        }.distinct().sorted().joinToString("\n")
        val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        ActivityScenario.launch(OptionsActivity::class.java).use { scenario ->
            fun verify() = scenario.onActivity { activity ->
                assertEquals(context.getString(R.string.app_version_code, code.toString()), activity.findViewById<TextView>(R.id.textAppVersionCode).text.toString())
                assertEquals(context.getString(R.string.app_room_schema, BuildConfig.ROOM_SCHEMA_VERSION), activity.findViewById<TextView>(R.id.textRoomSchemaVersion).text.toString())
                assertEquals(expectedPins, activity.findViewById<TextView>(R.id.textSigningCertificate).text.toString())
                assertTrue(activity.findViewById<TextView>(R.id.textAppVersion).text.contains(info.versionName!!))
                assertTrue(activity.findViewById<TextView>(R.id.textSigningCertificate).isTextSelectable)
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                activity.findViewById<View>(R.id.buttonCopySigningCertificate).performClick()
                assertEquals(expectedPins, clipboard.primaryClip!!.getItemAt(0).text.toString())
                val label = context.packageManager.getApplicationLabel(context.applicationInfo).toString()
                assertEquals(if (context.packageName.endsWith(".debugtest")) "АгроНализ TEST" else "АгроНализ", label)
                val main = LayoutInflater.from(activity).inflate(R.layout.activity_main, null) as android.view.ViewGroup
                assertTrue(allTexts(main).any { it.text.toString() == label })
            }
            onView(withId(R.id.buttonCopySigningCertificate)).perform(scrollTo(), click())
            verify(); scenario.recreate(); verify()
        }
        assertEquals(before, databases())
    }
    @Test fun inaccessiblePackageMetadataReturnsUnknown() {
        guard()
        val inaccessible = object : ContextWrapper(context) {
            override fun getPackageName(): String = throw SecurityException("Synthetic metadata failure")
        }
        val result = InstalledAppDiagnostics.read(inaccessible)
        assertNull(result.versionName); assertNull(result.versionCode); assertTrue(result.certificateSha256.isEmpty())
    }
    private fun allTexts(view: View): List<TextView> = when(view) {
        is TextView -> listOf(view)
        is android.view.ViewGroup -> (0 until view.childCount).flatMap { allTexts(view.getChildAt(it)) }
        else -> emptyList()
    }
    @Test fun numericRangesRemainWholeAtDifferentWidthsAndFontScales() {
        guard()
        instrumentation.runOnMainSync {
            for (font in listOf(1f, 1.6f)) for (widthDp in listOf(280, 360, 600)) {
                val configuration = Configuration(context.resources.configuration).apply { fontScale = font }
                val scaled = context.createConfigurationContext(configuration)
                val card = HarvestResultsCardView(scaled)
                for (yield in listOf("22,28–22,43 ц/га\n2,23–2,24 т/га", "12345678,12–12345679,34 ц/га\n—")) {
                    val model = HarvestResultsCardModel(listOf("Площадь" to "125 га", "Бункеры" to "84",
                        "Масса урожая" to "278,4–280,5 т", "Урожайность" to yield),
                        listOf("Осталось" to "-12345678,12 га"), "", emptyList(), "—", null)
                    card.update(model); card.setExpansion(true)
                    val width = (widthDp * scaled.resources.displayMetrics.density).toInt()
                    card.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                    card.layout(0, 0, width, card.measuredHeight)
                    assertEquals(yield, card.findViewWithTag<TextView>("harvest-result-3").text.toString())
                    for (value in allTexts(card)) {
                        val layout = value.layout ?: continue
                        Regex("-?\\d+(?:[.,]\\d+)?|[тц]/га|м³").findAll(value.text).forEach { token ->
                            assertEquals("Split ${token.value} width=$widthDp font=$font",
                                layout.getLineForOffset(token.range.first), layout.getLineForOffset(token.range.last))
                        }
                        for (line in 0 until layout.lineCount) {
                            assertEquals(0, layout.getEllipsisCount(line))
                            assertTrue("Clipped width=$widthDp font=$font text=${value.text}",
                                layout.getLineWidth(line) <= value.width - value.compoundPaddingLeft - value.compoundPaddingRight + 1)
                        }
                        assertTrue(value.height >= layout.height + value.compoundPaddingTop + value.compoundPaddingBottom)
                    }
                }
            }
        }
    }
}
