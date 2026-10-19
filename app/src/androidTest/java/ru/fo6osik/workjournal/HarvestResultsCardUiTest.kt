package ru.fo6osik.workjournal

import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HarvestResultsCardUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val db get() = DatabaseProvider.getDatabase(context)
    private val dao get() = db.harvestDao()
    private var processId = -1
    private var dayId = -1
    private var bunkerId = -1
    @Before fun seed() {
        check(context.packageName == "ru.fo6osik.workjournal.debugtest")
        check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish"))
        processId = dao.insertProcess(HarvestProcess(processUid = UUID.randomUUID().toString(), startDate = "2026-10-01",
            fieldId = null, fieldNameSnapshot = "synthetic-results", crop = "Другое", combineVehicleId = null,
            combineNameSnapshot = "synthetic-results", bunkerVolumeM3 = 150.0,
            fieldAreaSnapshotHa = 120.0, actualPlantedAreaHa = 100.0)).toInt()
        dayId = dao.insertDay(HarvestDay(dayUid = UUID.randomUUID().toString(), processId = processId,
            workDate = "2026-10-19", startTime = "08:00", endTime = "18:00", status = "COMPLETED", harvestedAreaHa = 90.0)).toInt()
        dao.updateDayDensityRaw(dayId, "CUSTOM", 1000.0)
        bunkerId = dao.insertBunkerRaw(HarvestBunker(bunkerUid = UUID.randomUUID().toString(), dayId = dayId, fillTime = "10:00")).toInt()
        dao.completeProcess(processId, "2026-10-19")
    }
    @After fun cleanup() {
        instrumentation.runOnMainSync {
            val monitor = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
            androidx.test.runner.lifecycle.Stage.values().flatMap { monitor.getActivitiesInStage(it) }.toSet()
                .filter { it.packageName == context.packageName && !it.isFinishing }.forEach { it.finish() }
        }
        if (processId > 0) db.openHelper.writableDatabase.execSQL("DELETE FROM harvest_processes WHERE id=?", arrayOf(processId))
    }
    private fun launch() = ActivityScenario.launch<HarvestProcessActivity>(Intent(context, HarvestProcessActivity::class.java)
        .putExtra(HarvestProcessActivity.EXTRA_PROCESS_ID, processId))
    private fun await(s: ActivityScenario<HarvestProcessActivity>, condition: (HarvestResultsCardView) -> Boolean = { it.currentModel != null }) {
        val stop = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < stop) {
            var ready = false
            s.onActivity { ready = condition(it.findViewById(R.id.harvestResultsCard)) }
            if (ready) return
            Thread.sleep(50)
        }
        fail("Results card did not update")
    }
    @Test fun twoByTwoGridInitiallyCollapsedAndToggle() {
        launch().use { s ->
            await(s)
            s.onActivity { a ->
                val v = a.findViewById<HarvestResultsCardView>(R.id.harvestResultsCard)
                val grid = v.findViewWithTag<LinearLayout>("harvest-results-grid")
                assertEquals(2, grid.childCount)
                for (i in 0..1) assertEquals(2, (grid.getChildAt(i) as LinearLayout).childCount)
                assertEquals("90 га", v.findViewWithTag<TextView>("harvest-result-0").text.toString())
                assertTrue(v.findViewWithTag<TextView>("harvest-result-3").text.contains("ц/га"))
                assertFalse(v.expanded); assertEquals(View.GONE, v.findViewWithTag<View>("harvest-results-details").visibility)
                v.findViewWithTag<View>("harvest-results-toggle").performClick()
                assertTrue(v.expanded); assertEquals(View.VISIBLE, v.findViewWithTag<View>("harvest-results-details").visibility)
                assertEquals(90, v.findViewWithTag<ProgressBar>("harvest-results-progress").progress)
                v.findViewWithTag<View>("harvest-results-toggle").performClick(); assertFalse(v.expanded)
            }
        }
    }
    @Test fun roomUpdatesAndRecreationRetainExpansion() {
        launch().use { s ->
            await(s)
            s.onActivity { it.findViewById<HarvestResultsCardView>(R.id.harvestResultsCard).setExpansion(true) }
            s.recreate(); await(s) { it.currentModel != null && it.expanded }
            s.moveToState(Lifecycle.State.CREATED); dao.updateActualArea(processId, 120.0)
            s.moveToState(Lifecycle.State.RESUMED)
            await(s) { it.expanded && it.currentModel?.progressText == "75,0%" }
            s.moveToState(Lifecycle.State.CREATED)
            dao.updateHarvestedArea(dayId, 60.0)
            db.openHelper.writableDatabase.execSQL("UPDATE harvest_bunkers SET fillLevel='HALF' WHERE id=?", arrayOf(bunkerId))
            s.moveToState(Lifecycle.State.RESUMED)
            await(s) { it.expanded && it.currentModel?.overview?.get(2)?.second == "75 т" }
            s.onActivity { assertEquals("60 га", it.findViewById<HarvestResultsCardView>(R.id.harvestResultsCard).currentModel!!.overview[0].second) }
        }
    }
    @Test fun rangesIncompleteWarningsAndExcessAreDisplayed() {
        instrumentation.runOnMainSync {
            val v = HarvestResultsCardView(context)
            fun update(area: Double?, density: Double?, fill: String = "MORE_THAN_HALF") {
                val days = listOf(HarvestCalculator.DayInput(listOf(fill), area, density))
                val r = HarvestCampaignMetrics.calculate(150.0, 100.0, days, days, true)
                v.update(HarvestResultsCardModel.from(r, null, 100.0, false)); v.setExpansion(true)
            }
            update(110.0, 1000.0)
            assertTrue(v.findViewWithTag<TextView>("harvest-result-2").text.contains("75–150 т"))
            assertEquals("110,0%", v.currentModel!!.progressText)
            assertEquals(100, v.findViewWithTag<ProgressBar>("harvest-results-progress").progress)
            assertTrue(v.currentModel!!.details.any { it.first == "Осталось убрать" && it.second == "-10 га" })
            update(null, null)
            assertEquals("—", v.findViewWithTag<TextView>("harvest-result-2").text.toString())
            assertTrue(v.currentModel!!.warnings.any { it.contains("площадь указана не для всех") })
            assertTrue(v.findViewWithTag<TextView>("harvest-results-notice").text.contains("Неполные данные"))
        }
    }
    @Test fun cardPrecedesCalendarAndToggleKeepsCalendarSelection() {
        launch().use { s ->
            await(s)
            s.onActivity { a ->
                val v = a.findViewById<HarvestResultsCardView>(R.id.harvestResultsCard)
                val calendar = a.findViewById<HarvestDayCalendarView>(R.id.harvestDayCalendar)
                assertTrue(v.top < calendar.top)
                calendar.findViewWithTag<TextView>("harvest-date-2026-10-19").performClick()
                v.setExpansion(true); v.setExpansion(false)
                assertEquals("2026-10-19", calendar.calendar.selectedDate)
                assertNotNull(calendar.findViewWithTag<View>("harvest-date-summary"))
            }
        }
    }
    /** Also rerun with720x960/font1.6 by the guarded CI driver. */
    @Test fun compactLargeFont() {
        launch().use { s ->
            await(s) { it.currentModel != null && it.findViewWithTag<View>("harvest-results-grid").width > 0 }
            s.onActivity { a ->
                val v = a.findViewById<HarvestResultsCardView>(R.id.harvestResultsCard)
                val grid = v.findViewWithTag<LinearLayout>("harvest-results-grid")
                for (row in 0..1) {
                    val r = grid.getChildAt(row) as LinearLayout
                    assertTrue(r.getChildAt(0).right <= r.getChildAt(1).left)
                    for (col in 0..1) {
                        val cell = r.getChildAt(col) as LinearLayout
                        val value = cell.getChildAt(1) as TextView
                        assertTrue(value.width > 0); assertNotNull(value.layout)
                        for (line in 0 until value.layout.lineCount) assertEquals(0, value.layout.getEllipsisCount(line))
                        assertTrue(value.height >= value.layout.height)
                    }
                }
            }
        }
    }
}
