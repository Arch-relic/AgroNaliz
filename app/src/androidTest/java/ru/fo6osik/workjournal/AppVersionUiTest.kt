package ru.fo6osik.workjournal

import android.util.TypedValue
import android.view.Gravity
import kotlin.math.roundToInt
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Invoked for both installed variants, and again after the guarded TEST update. */
@RunWith(AndroidJUnit4::class)
class AppVersionUiTest {
    @Test fun showsInstalledVersion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val installed = context.packageManager.getPackageInfo(context.packageName, 0)
        assertEquals("1.0.8", installed.versionName)
        assertEquals(installed.versionName, BuildConfig.VERSION_NAME)
        assertEquals(23, DatabaseProvider.getDatabase(context).openHelper.readableDatabase.version)
        val test = context.packageName == "ru.fo6osik.workjournal.debugtest"
        assertTrue(test || context.packageName == "ru.fo6osik.workjournal")
        assertEquals(test, BuildConfig.BUILD_TYPE == "debugTest")
        val expected = "Версия ${installed.versionName}" + if (test) " · TEST" else ""
        ActivityScenario.launch(OptionsActivity::class.java).use { scenario ->
            onView(withId(R.id.textAppVersion)).perform(scrollTo())
                .check(matches(withText(expected))).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                val version = activity.findViewById<TextView>(R.id.textAppVersion)
                assertEquals(Gravity.CENTER, version.gravity)
                // XML dimensions use getDimensionPixelSize: account for SP conversion and pixel rounding.
                val expectedSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 12f, activity.resources.displayMetrics
                ).roundToInt().toFloat()
                assertEquals(expectedSize, version.textSize, 0.1f)
            }
            // Developer footer keeps the catalog; one backup entry opens the full backup screen.
            onView(withId(R.id.buttonDeveloperCatalog)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.buttonDataBackup)).perform(scrollTo(), click())
            for (id in listOf(R.id.buttonCreateBackup, R.id.buttonRestoreBackup, R.id.buttonImportData))
                onView(withId(id)).perform(scrollTo()).check(matches(isDisplayed())).check(matches(isEnabled()))
            pressBack()
        }
    }
}
