package ru.fo6osik.workjournal

import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HarvestCampaignUiTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dao get() = DatabaseProvider.getDatabase(context).harvestDao()
    private fun fixture(crop: String, secondArea: Double?): Int {
        check(context.packageName == "ru.fo6osik.workjournal.debugtest")
        val process = dao.insertProcess(HarvestProcess(processUid = UUID.randomUUID().toString(),
            startDate = "2026-10-10", endDate = "2026-10-11", status = "COMPLETED",
            fieldId = null, fieldNameSnapshot = "synthetic campaign", crop = crop,
            combineVehicleId = null, combineNameSnapshot = "synthetic", bunkerVolumeM3 = 8.0)).toInt()
        val first = dao.insertDay(HarvestDay(dayUid = UUID.randomUUID().toString(), processId = process,
            workDate = "2026-10-10", startTime = "08:00", endTime = "18:00", status = "COMPLETED",
            harvestedAreaHa = 2.0)).toInt()
        val second = dao.insertDay(HarvestDay(dayUid = UUID.randomUUID().toString(), processId = process,
            workDate = "2026-10-11", startTime = "08:00", endTime = "18:00", status = "COMPLETED",
            harvestedAreaHa = secondArea)).toInt()
        for ((day, level) in listOf(first to "FULL", second to "MORE_THAN_HALF", second to "HALF")) {
            dao.insertBunker(HarvestBunker(bunkerUid = UUID.randomUUID().toString(), dayId = day,
                fillTime = "09:00", fillLevel = level))
        }
        return process
    }
    private fun awaitText(scenario: ActivityScenario<HarvestProcessActivity>, contains: String): String {
        val deadline = System.currentTimeMillis() + 10000
        while (true) {
            var text = ""
            scenario.onActivity { text = it.findViewById<TextView>(R.id.textHarvestCampaignResults).text.toString() }
            if (text.contains(contains)) return text
            if (System.currentTimeMillis() > deadline) { fail("Campaign UI did not update: $text") }
            Thread.sleep(50)
        }
    }
    private fun open(process: Int) = ActivityScenario.launch<HarvestProcessActivity>(
        Intent(context, HarvestProcessActivity::class.java).putExtra(HarvestProcessActivity.EXTRA_PROCESS_ID, process))

    @Test fun completeAreasShowAggregateMassAndBothYieldRanges() {
        val process = fixture("Пшеница", 3.0)
        open(process).use { scenario ->
            val text = awaitText(scenario, "Урожайность: ≈")
            assertTrue(text.contains("Приблизительная масса: ≈ 12–15 т"))
            assertTrue(text.contains("площадей: 5 га"))
            assertTrue(text.contains("24–30 ц/га (2,4–3 т/га)"))
            assertFalse(text.contains("площади неполные"))
        }
    }

    @Test fun otherCropRequiresManualDensityAndIncompleteAreaKeepsYieldUnknown() {
        val process = fixture("Другое", null)
        open(process).use { scenario ->
            var text = awaitText(scenario, "Сведения о площади неполные")
            assertTrue(text.contains("Приблизительная масса: Не рассчитана"))
            onView(withId(R.id.buttonEditHarvestDensity)).perform(scrollTo(), click())
            onView(withId(R.id.editHarvestNumber)).perform(replaceText("550,5"))
            closeSoftKeyboard()
            onView(withText("Сохранить")).inRoot(isDialog()).perform(click())
            text = awaitText(scenario, "Приблизительная масса: ≈")
            assertTrue(text.contains("8,81–11,01 т"))
            assertTrue(text.contains("площадей: 2 га"))
            assertTrue(text.contains("Урожайность: Не рассчитана"))
            assertEquals(550.5, dao.getProcessById(process)!!.bulkDensityKgM3!!, 0.0)
            onView(withId(R.id.buttonEditHarvestDensity)).perform(scrollTo(), click())
            onView(withText("По справочнику")).inRoot(isDialog()).perform(click())
            awaitText(scenario, "Приблизительная масса: Не рассчитана")
            assertNull(dao.getProcessById(process)!!.bulkDensityKgM3)
            assertEquals("COMPLETED", dao.getProcessById(process)!!.status)
        }
    }
}
