package ru.fo6osik.workjournal

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

/** Synthetic records only, run by the existing fresh TEST emulator harness. */
@RunWith(AndroidJUnit4::class)
class HarvestCalendarUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val dao get() = DatabaseProvider.getDatabase(context).harvestDao()
    private var processId = -1
    @Before fun seed() {
        check(context.packageName == "ru.fo6osik.workjournal.debugtest")
        check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish"))
        processId = dao.createProcess(HarvestProcess(processUid = UUID.randomUUID().toString(), startDate = "2025-01-01",
            fieldId = null, fieldNameSnapshot = "synthetic-calendar", crop = "Другое", combineVehicleId = null,
            combineNameSnapshot = "synthetic-calendar")).toInt()
    }
    @After fun cleanup() {
        instrumentation.runOnMainSync {
            val monitor = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
            androidx.test.runner.lifecycle.Stage.values().flatMap { monitor.getActivitiesInStage(it) }.toSet()
                .filter { it.packageName == context.packageName && !it.isFinishing }.forEach { it.finish() }
        }
        if (processId > 0) DatabaseProvider.getDatabase(context).openHelper.writableDatabase
            .execSQL("DELETE FROM harvest_processes WHERE id=?", arrayOf(processId))
    }
    private fun day(date: String, area: Double? = 10.0): Int = dao.insertDay(HarvestDay(
        dayUid = UUID.randomUUID().toString(), processId = processId, workDate = date,
        startTime = "08:00", endTime = "18:00", status = "COMPLETED", harvestedAreaHa = area)).toInt()
    private fun launch() = ActivityScenario.launch<HarvestProcessActivity>(Intent(context, HarvestProcessActivity::class.java)
        .putExtra(HarvestProcessActivity.EXTRA_PROCESS_ID, processId))
    private fun await(s: ActivityScenario<HarvestProcessActivity>, condition: (HarvestDayCalendarView) -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) {
            var ready = false
            s.onActivity { ready = condition(it.findViewById(R.id.harvestDayCalendar)) }
            if (ready) return
            Thread.sleep(50)
        }
        fail("Calendar did not reach expected state")
    }
    @Test fun selectionColorsSummaryRecreationAndRefresh() {
        val id = day("2026-10-19"); day("2026-10-20", null)
        launch().use { s ->
            await(s) { it.calendar.month == "2026-10" }
            s.onActivity { a ->
                val v = a.findViewById<HarvestDayCalendarView>(R.id.harvestDayCalendar)
                assertNull(v.calendar.selectedDate)
                assertEquals(View.GONE, v.findViewWithTag<TextView>("harvest-calendar-previous")?.visibility ?: View.GONE)
                val green = v.findViewWithTag<TextView>("harvest-date-2026-10-19")
                assertTrue(green.isEnabled); assertTrue(green.contentDescription.contains("площадь указана"))
                assertFalse(v.findViewWithTag<TextView>("harvest-date-2026-10-18").isEnabled)
                green.performClick()
                val selected = v.findViewWithTag<TextView>("harvest-date-2026-10-19")
                assertTrue(selected.isSelected); assertTrue(selected.background is GradientDrawable)
                assertTrue(v.findViewWithTag<TextView>("harvest-date-summary").text.contains("10 га"))
                selected.performClick(); assertNull(v.findViewWithTag<View>("harvest-date-summary"))
                v.findViewWithTag<TextView>("harvest-date-2026-10-20").performClick()
                assertTrue(v.findViewWithTag<TextView>("harvest-date-summary").text.contains("неполные данные"))
                v.findViewWithTag<TextView>("harvest-date-2026-10-19").performClick()
            }
            s.recreate(); await(s) { it.calendar.selectedDate == "2026-10-19" }
            s.moveToState(Lifecycle.State.CREATED)
            assertEquals(1, dao.updateHarvestedArea(id, 25.0))
            s.moveToState(Lifecycle.State.RESUMED)
            await(s) { it.findViewWithTag<TextView>("harvest-date-summary")?.text?.contains("25 га") == true }
        }
    }
    @Test fun monthArrowsSkipEmptyMonthsAndClearSelection() {
        day("2025-12-31"); day("2026-02-01")
        launch().use { s ->
            await(s) { it.calendar.month == "2026-02" }
            s.onActivity { a ->
                val v = a.findViewById<HarvestDayCalendarView>(R.id.harvestDayCalendar)
                v.findViewWithTag<TextView>("harvest-date-2026-02-01").performClick()
                v.findViewWithTag<TextView>("harvest-calendar-previous").performClick()
                assertEquals("2025-12", v.calendar.month); assertNull(v.calendar.selectedDate)
                assertNull(v.findViewWithTag<View>("harvest-date-summary"))
                v.findViewWithTag<TextView>("harvest-calendar-next").performClick(); assertEquals("2026-02", v.calendar.month)
            }
        }
    }
    @Test fun summaryOpensExactDayAndDeletedDayCannotOpen() {
        val id = day("2026-10-19")
        launch().use { s ->
            await(s) { it.calendar.month != null }
            s.onActivity { a ->
                val v = a.findViewById<HarvestDayCalendarView>(R.id.harvestDayCalendar)
                v.findViewWithTag<TextView>("harvest-date-2026-10-19").performClick()
                v.findViewWithTag<TextView>("harvest-date-summary").performClick()
            }
            val stop = System.nanoTime() + 10_000_000_000L
            var opened = false
            while (!opened && System.nanoTime() < stop) {
                instrumentation.runOnMainSync {
                    val days = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filterIsInstance<HarvestDayActivity>()
                    opened = days.any { it.intent.getIntExtra(HarvestDayActivity.EXTRA_DAY_ID, -1) == id }
                }
                if (!opened) Thread.sleep(50)
            }
            assertTrue("Exact day should open", opened)
            androidx.test.espresso.Espresso.pressBack()
            await(s) { it.calendar.selectedDate == "2026-10-19" }
            DatabaseProvider.getDatabase(context).openHelper.writableDatabase.execSQL("DELETE FROM harvest_days WHERE id=?", arrayOf(id))
            s.onActivity { a -> a.findViewById<HarvestDayCalendarView>(R.id.harvestDayCalendar)
                .findViewWithTag<TextView>("harvest-date-summary").performClick() }
            await(s) { it.calendar.month == null }
        }
    }
    @Test fun groupedViewShowsIndividualNavigationWithoutChangingRoomConstraint() {
        instrumentation.runOnMainSync {
            val v = HarvestDayCalendarView(context)
            val a = HarvestDaySummary(101, processId, "2026-10-19", "08:00", "12:00", "COMPLETED", 2, 10.0, null, null)
            val b = a.copy(id = 102, startTime = "13:00", endTime = null, status = "ACTIVE", harvestedAreaHa = null, bunkerCount = 3)
            var opened = -1
            v.onOpenDay = { id, _ -> opened = id }
            v.update(listOf(a, b)); v.findViewWithTag<TextView>("harvest-date-2026-10-19").performClick()
            val summary = v.findViewWithTag<TextView>("harvest-date-summary")
            assertTrue(summary.text.contains("Бункеров: 5")); assertTrue(summary.text.contains("неполные данные"))
            assertFalse(summary.isClickable)
            v.findViewWithTag<TextView>("harvest-record-102").performClick(); assertEquals(102, opened)
        }
    }
    @Test fun emptyCampaignHidesCalendarAndShowsEmptyMessage() {
        launch().use { s ->
            val stop = System.nanoTime() + 10_000_000_000L
            var ready = false
            while (!ready && System.nanoTime() < stop) {
                s.onActivity { ready = it.findViewById<TextView>(R.id.textHarvestProcessTitle).text.isNotEmpty() }
                if (!ready) Thread.sleep(50)
            }
            assertTrue(ready)
            s.onActivity {
                assertEquals(View.GONE, it.findViewById<View>(R.id.harvestDayCalendar).visibility)
                assertEquals(View.VISIBLE, it.findViewById<View>(R.id.textHarvestDaysEmpty).visibility)
            }
        }
    }
}
