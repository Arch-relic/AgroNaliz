package ru.fo6osik.workjournal

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.View
import android.widget.ScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.`is`
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ChangelogUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    @Before fun emulatorOnly() {
        check(context.packageName == "ru.fo6osik.workjournal.debugtest")
        check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish"))
    }
    private fun header(id: String) = onView(withTagValue(`is`("changelog-header-$id")))
    private fun visibility(scenario: ActivityScenario<ChangelogActivity>, id: String, expected: Int) {
        scenario.onActivity { activity ->
            assertEquals(expected, activity.findViewById<ScrollView>(R.id.changelogScroll)
                .findViewWithTag<View>("changelog-details-$id").visibility)
        }
    }
    @Test fun navigationIndependentExpansionAndRecreation() {
        ActivityScenario.launch(OptionsActivity::class.java).use {
            onView(withId(R.id.buttonChangelog)).perform(scrollTo(), click())
            header("1.0.9").check(matches(withText(org.hamcrest.Matchers.containsString("В разработке"))))
            pressBack()
        }
        ActivityScenario.launch(ChangelogActivity::class.java).use { scenario ->
            visibility(scenario,"1.0.9",View.GONE)
            visibility(scenario,"1.0.8",View.VISIBLE)
            header("1.0.9").perform(scrollTo(), click())
            visibility(scenario,"1.0.8",View.VISIBLE)
            header("1.0.7").perform(scrollTo(), click())
            scenario.recreate()
            visibility(scenario,"1.0.9",View.VISIBLE)
            visibility(scenario,"1.0.7",View.VISIBLE)
            header("1.0.9").perform(scrollTo(), click())
            visibility(scenario,"1.0.9",View.GONE)
            visibility(scenario,"1.0.7",View.VISIBLE)
            header("1.0.8").perform(scrollTo(), click())
            header("1.0.7").perform(scrollTo(), click())
            scenario.recreate()
            visibility(scenario,"1.0.8",View.GONE)
        }
    }
    private fun databaseSnapshot(): Map<String,List<List<String?>>> {
        val db = DatabaseProvider.getDatabase(context).openHelper.readableDatabase
        val names = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name").use {
            while (it.moveToNext()) names.add(it.getString(0))
        }
        return names.associateWith { table ->
            val rows = mutableListOf<List<String?>>()
            db.query("SELECT * FROM \"${table.replace("\"", "\"\"")}\" ORDER BY rowid").use {
                while (it.moveToNext()) rows.add((0 until it.columnCount).map { n -> if (it.isNull(n)) null else it.getString(n) })
            }
            rows
        }
    }
    @Test fun offlineAndNoDatabaseChanges() {
        // The installed app has no INTERNET permission: even an available network cannot be used.
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission("android.permission.INTERNET"))
        val before = databaseSnapshot()
        ActivityScenario.launch(ChangelogActivity::class.java).use { scenario ->
            for (id in listOf("1.0.9","1.0.7","early")) header(id).perform(scrollTo(),click())
            scenario.recreate()
            visibility(scenario,"early",View.VISIBLE)
        }
        assertEquals(before, databaseSnapshot())
    }
    @Test fun compactLargeFontScreenshot() {
        ActivityScenario.launch(ChangelogActivity::class.java).use { scenario ->
            header("1.0.9").perform(scrollTo(),click())
            header("early").perform(scrollTo(),click())
            onView(withText("• Раздел уборки урожая.")).perform(scrollTo()).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                val scroll = activity.findViewById<ScrollView>(R.id.changelogScroll)
                assertTrue(scroll.height > 0)
                assertTrue(scroll.getChildAt(0).height > scroll.height)
                scroll.scrollTo(0,0)
            }
            instrumentation.waitForIdleSync()
            val file = File(context.getExternalFilesDir(null), "changelog-screenshots/history.png")
            file.parentFile!!.mkdirs()
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
            bitmap.recycle()
        }
    }
}
