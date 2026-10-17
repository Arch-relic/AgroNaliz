package ru.fo6osik.workjournal

import android.app.Activity
import android.graphics.Rect
import android.view.ViewGroup
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MainScreenLayoutUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val db get() = DatabaseProvider.getDatabase(context)
    private val owned = mutableListOf<Int>()

    @Before fun isolatedEmulator() {
        check(context.packageName == "ru.fo6osik.workjournal.debugtest")
        check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish"))
        check(db.harvestDao().getActiveProcesses().isEmpty())
    }

    @After fun cleanup() {
        for (id in owned) db.openHelper.writableDatabase.execSQL("DELETE FROM harvest_processes WHERE id=?", arrayOf(id))
    }

    private fun campaign(): Int = db.harvestDao().createProcess(HarvestProcess(
        processUid = UUID.randomUUID().toString(), startDate = "2026-10-17", fieldId = null,
        fieldNameSnapshot = "synthetic layout", crop = "Другое", combineVehicleId = null,
        combineNameSnapshot = "synthetic"
    )).toInt().also { owned.add(it) }

    private fun awaitMain(scenario: ActivityScenario<MainActivity>, visible: Boolean) {
        val end = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < end) {
            var ready = false
            scenario.onActivity {
                val button = it.findViewById<Button>(R.id.buttonContinueHarvest)
                ready = button.isEnabled && (button.visibility == View.VISIBLE) == visible &&
                    it.findViewById<Button>(R.id.buttonOptions).height > 0 && (!visible || button.height > 0)
            }
            if (ready) return
            Thread.sleep(50)
        }
        fail("Main layout did not load")
    }

    private fun awaitScreen(type: Class<out Activity>) {
        val end = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < end) {
            var ready = false
            instrumentation.runOnMainSync {
                ready = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).any { type.isInstance(it) }
            }
            if (ready) return
            Thread.sleep(50)
        }
        fail("Expected screen ${type.simpleName}")
    }

    private fun tap(id: Int) = onView(withId(id)).perform(scrollTo(), click())

    @Test fun navigationAndDeveloperBlock() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitMain(scenario, false)
            for (id in listOf(R.id.textAppVersion, R.id.buttonDeveloperCatalog))
                onView(withId(id)).check(doesNotExist())
            tap(R.id.buttonOptions)
            awaitScreen(OptionsActivity::class.java)
            for (id in listOf(R.id.textDeveloperHeading, R.id.textAppVersion, R.id.buttonDeveloperCatalog))
                onView(withId(id)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("Резервное копирование")).check(doesNotExist())
            tap(R.id.buttonDataBackup)
            awaitScreen(BackupRestoreActivity::class.java)
            for (id in listOf(R.id.buttonCreateBackup, R.id.buttonRestoreBackup, R.id.buttonImportData))
                onView(withId(id)).perform(scrollTo()).check(matches(isDisplayed())).check(matches(isEnabled()))
            pressBack(); awaitScreen(OptionsActivity::class.java)
            tap(R.id.buttonDeveloperCatalog)
            awaitScreen(DeveloperCatalogActivity::class.java)
            pressBack(); awaitScreen(OptionsActivity::class.java)
            pressBack(); awaitMain(scenario, false)
        }
    }

    @Test fun continueBottomHeightAndNoReservedHiddenBlock() {
        val id = campaign()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitMain(scenario, true)
            scenario.onActivity { activity ->
                val body = activity.findViewById<LinearLayout>(R.id.mainContent)
                val button = activity.findViewById<Button>(R.id.buttonContinueHarvest)
                val standard = activity.findViewById<Button>(R.id.buttonOptions)
                val work = activity.findViewById<Button>(R.id.buttonWorkNotes)
                val dp = activity.resources.displayMetrics.density
                assertSame(button, body.getChildAt(body.childCount - 1))
                assertEquals(standard.width, button.width)
                assertTrue(button.height >= standard.height * 1.8)
                assertTrue(button.height <= standard.height * 2.2)
                assertEquals(Gravity.CENTER, button.gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK or
                    (button.gravity and Gravity.VERTICAL_GRAVITY_MASK))
                assertTrue("Keep existing Options gap", standard.top - work.bottom >= 56 * dp)
                assertEquals("Pin to content bottom with padding", body.height - body.paddingBottom, button.bottom)
                assertTrue(body.paddingBottom >= 24 * dp - 1)
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            db.harvestDao().completeProcess(id, "2026-10-17")
            scenario.moveToState(Lifecycle.State.RESUMED)
            awaitMain(scenario, false)
            scenario.onActivity {
                val button = it.findViewById<Button>(R.id.buttonContinueHarvest)
                assertEquals(View.GONE, button.visibility)
                // There is no fixed-height wrapper retained when this direct child is GONE.
                assertEquals(LinearLayout.LayoutParams.WRAP_CONTENT, button.layoutParams.height)
                assertTrue(button.parent is LinearLayout)
            }
        }
    }

    private fun assertReadable(id: Int) {
        try {
            onView(withId(id)).perform(scrollTo()).check(matches(isDisplayed()))
        } catch (failure: Throwable) {
            instrumentation.runOnMainSync {
                val activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                fun dump(view: View, depth: Int) {
                    val rect = Rect()
                    val visible = view.getGlobalVisibleRect(rect)
                    println("LAYOUT ${" ".repeat(depth)}${view.javaClass.simpleName} id=${view.id} " +
                        "bounds=${view.left},${view.top},${view.right},${view.bottom} " +
                        "scroll=${view.scrollX},${view.scrollY} padding=${view.paddingTop},${view.paddingBottom} " +
                        "global=$visible:$rect")
                    if (view is ViewGroup) for (i in 0 until view.childCount) dump(view.getChildAt(i), depth + 1)
                }
                activities.forEach { dump(it.window.decorView, 0) }
            }
            throw failure
        }
        instrumentation.runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
            val text = activity.findViewById<TextView>(id)
            assertNotNull(text.layout)
            assertTrue("Text clipped at $id", text.layout.height + text.compoundPaddingTop + text.compoundPaddingBottom <= text.height)
            for (line in 0 until text.layout.lineCount) assertEquals(0, text.layout.getEllipsisCount(line))
        }
    }

    /** Runner sets a compact viewport and font_scale=1.6, then restores both in finally. */
    @Test fun compactLargeFont() {
        campaign()
        assertTrue(context.resources.configuration.fontScale >= 1.5f)
        assertTrue(context.resources.configuration.screenHeightDp < 500)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitMain(scenario, true)
            for (id in listOf(R.id.buttonMachines, R.id.buttonFields, R.id.buttonWorkNotes,
                R.id.buttonOptions, R.id.buttonContinueHarvest)) assertReadable(id)
            scenario.onActivity {
                val scroll = it.findViewById<ScrollView>(R.id.mainScroll)
                assertTrue(scroll.getChildAt(0).height > scroll.height)
            }
            tap(R.id.buttonOptions); awaitScreen(OptionsActivity::class.java)
            for (id in listOf(R.id.buttonDataBackup, R.id.textDeveloperHeading, R.id.textAppVersion,
                R.id.buttonDeveloperCatalog)) assertReadable(id)
            pressBack(); awaitMain(scenario, true)
        }
    }
}
