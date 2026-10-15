package ru.fo6osik.workjournal

import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HarvestAreaUiTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dao get() = DatabaseProvider.getDatabase(context).harvestDao()
    private fun fixture(completed: Boolean = false): Int {
        check(context.packageName == "ru.fo6osik.workjournal.debugtest")
        val process = dao.insertProcess(HarvestProcess(processUid = UUID.randomUUID().toString(),
            startDate = "2026-10-11", fieldId = null, fieldNameSnapshot = "synthetic UI",
            crop = "Пшеница", combineVehicleId = null, combineNameSnapshot = "synthetic", bunkerVolumeM3 = 8.0)).toInt()
        val day = dao.insertDay(HarvestDay(dayUid = UUID.randomUUID().toString(), processId = process,
            workDate = "2026-10-11", startTime = "08:00", workIntervalMinutes = 47, workIntervalCustom = true)).toInt()
        dao.insertBunker(HarvestBunker(bunkerUid = UUID.randomUUID().toString(), dayId = day,
            fillTime = "09:00", fillLevel = "FULL", postEventType = "WAITING_TRANSPORT"))
        context.getSharedPreferences("harvest_reminder_prefs", 0).edit().putBoolean("enabled_$day", false).commit()
        if (completed) { dao.completeDay(day, "18:00"); dao.completeProcess(process, "2026-10-11") }
        return day
    }
    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10000
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) fail("Harvest UI did not update")
            Thread.sleep(50)
        }
    }
    private fun open(day: Int): ActivityScenario<HarvestDayActivity> {
        val scenario = ActivityScenario.launch<HarvestDayActivity>(Intent(context, HarvestDayActivity::class.java)
            .putExtra(HarvestDayActivity.EXTRA_DAY_ID, day))
        await {
            var ready = false
            scenario.onActivity { ready = it.findViewById<TextView>(R.id.textHarvestDayResults).text.isNotEmpty() }
            ready
        }
        return scenario
    }
    private fun finishDialog() {
        onView(withId(R.id.buttonFinishHarvestDay)).perform(scrollTo(), click())
        onView(withId(R.id.textFinishHarvestDayTime)).perform(click())
        onView(isAssignableFrom(android.widget.TimePicker::class.java)).perform(object : androidx.test.espresso.ViewAction {
            override fun getDescription() = "Select deterministic end time after fixture bunker"
            override fun getConstraints(): org.hamcrest.Matcher<android.view.View> = isAssignableFrom(android.widget.TimePicker::class.java)
            override fun perform(controller: androidx.test.espresso.UiController, view: android.view.View) {
                (view as android.widget.TimePicker).currentHour = 18
                view.currentMinute = 0
                controller.loopMainThreadUntilIdle()
            }
        })
        onView(withId(android.R.id.button1)).perform(click())
    }

    @Test fun finishWithoutAreaKeepsUnknownAndCampaignQuestion() {
        val day = fixture()
        open(day).use {
            finishDialog()
            onView(withId(R.id.buttonConfirmFinishHarvestDay)).perform(scrollTo(), click())
            onView(withText("Уборочная кампания окончена?")).check(matches(isDisplayed()))
            onView(withText("Нет")).inRoot(isDialog()).perform(click())
            await { dao.getDayById(day)!!.status == "COMPLETED" }
            assertNull(dao.getDayById(day)!!.harvestedAreaHa)
            assertEquals("IN_PROGRESS", dao.getProcessById(dao.getDayById(day)!!.processId)!!.status)
        }
    }
    @Test fun finishWithCommaFractionSavesAreaAndCampaign() {
        val day = fixture()
        open(day).use {
            finishDialog()
            onView(withId(R.id.editFinishHarvestDayArea)).perform(replaceText("12,375"))
            closeSoftKeyboard()
            onView(withId(R.id.buttonConfirmFinishHarvestDay)).perform(scrollTo(), click())
            onView(withText("Да")).inRoot(isDialog()).perform(click())
            await { dao.getDayById(day)!!.status == "COMPLETED" }
            assertEquals(12.375, dao.getDayById(day)!!.harvestedAreaHa!!, 0.0)
            assertEquals("COMPLETED", dao.getProcessById(dao.getDayById(day)!!.processId)!!.status)
        }
    }
    @Test fun bothCancellationStepsWriteNothing() {
        val day = fixture()
        val before = dao.getDayById(day)
        open(day).use {
            finishDialog()
            onView(withId(R.id.editFinishHarvestDayArea)).perform(replaceText("2.5"))
            closeSoftKeyboard()
            onView(withId(R.id.buttonCancelFinishHarvestDay)).perform(scrollTo(), click())
            assertEquals(before, dao.getDayById(day))
            finishDialog()
            onView(withId(R.id.editFinishHarvestDayArea)).perform(replaceText("3,5"))
            closeSoftKeyboard()
            onView(withId(R.id.buttonConfirmFinishHarvestDay)).perform(scrollTo(), click())
            onView(withText("Отмена")).inRoot(isDialog()).perform(click())
            assertEquals(before, dao.getDayById(day))
        }
    }
    @Test fun zeroAreaKeepsFinishDialogOpenAndDayActive() {
        val day = fixture()
        open(day).use {
            finishDialog()
            onView(withId(R.id.editFinishHarvestDayArea)).perform(replaceText("0"))
            closeSoftKeyboard()
            onView(withId(R.id.buttonConfirmFinishHarvestDay)).perform(scrollTo(), click())
            onView(withId(R.id.editFinishHarvestDayArea)).check(matches(hasErrorText(context.getString(R.string.harvest_positive_number))))
            assertEquals("ACTIVE", dao.getDayById(day)!!.status)
            onView(withId(R.id.buttonCancelFinishHarvestDay)).perform(scrollTo(), click())
        }
    }
    @Test fun addAndEditAreaAfterCampaignCompletionUpdatesYieldAndPreservesDay() {
        val day = fixture(completed = true)
        val before = dao.getDayById(day)!!
        open(day).use { scenario ->
            onView(withId(R.id.buttonEditHarvestArea)).check(matches(withText("Добавить площадь")))
            for (input in listOf("2,5", "3.125")) {
                onView(withId(R.id.buttonEditHarvestArea)).perform(scrollTo(), click())
                onView(withId(R.id.editHarvestNumber)).perform(replaceText(input))
                closeSoftKeyboard()
                onView(withText("Сохранить")).inRoot(isDialog()).perform(click())
                val expected = HarvestCalculator.parseOptionalPositive(input)
                await { dao.getDayById(day)!!.harvestedAreaHa == expected }
                await {
                    var ready = false
                    scenario.onActivity {
                        val text = it.findViewById<TextView>(R.id.textHarvestDayResults).text
                        ready = text.contains("Урожайность: ≈") && text.contains(
                            "Площадь: ${HarvestResultsFormatter.number(expected!!)} га")
                    }
                    ready
                }
                onView(withId(R.id.buttonEditHarvestArea)).check(matches(withText("Изменить площадь")))
                assertEquals(before.copy(harvestedAreaHa = expected), dao.getDayById(day))
                assertEquals("COMPLETED", dao.getProcessById(before.processId)!!.status)
                assertEquals("WAITING_TRANSPORT", dao.getBunkersForDay(day).single().postEventType)
            }
        }
    }
    @Test fun completedDayCustomDensitySurvivesDialogReopen() {
        val day = fixture(completed = true)
        open(day).use {
            fun densityDialog() {
                onView(withContentDescription("Настройки рабочего дня")).perform(click())
                onView(withText("Настройка насыпной плотности")).perform(click())
            }
            densityDialog()
            onView(withText("Своё значение")).perform(click())
            onView(withHint("Своё значение, кг/м³")).perform(replaceText("555,125"))
            closeSoftKeyboard()
            onView(withText("Сохранить")).inRoot(isDialog()).perform(click())
            await { dao.getDayById(day)!!.bulkDensityKgM3 == 555.125 }
            await { dao.getDayById(day)!!.bulkDensityKgM3 == 555.125 }
            densityDialog()
            onView(withHint("Своё значение, кг/м³")).check(matches(withText("555.125")))
            onView(withText("Своё значение")).check(matches(isChecked()))
            onView(withText("Отмена")).inRoot(isDialog()).perform(click())
            assertEquals("COMPLETED", dao.getDayById(day)!!.status)
        }
    }

    @Test fun editReopensCampaignAndKeepsHarvestResults() {
        val day = fixture(completed = true)
        dao.updateHarvestedArea(day, 3.125)
        dao.updateDayDensity(day, "CUSTOM", 555.125)
        val before = dao.getDayById(day)!!
        open(day).use { scenario ->
            onView(withId(R.id.buttonEditHarvestDay)).perform(scrollTo(), click())
            onView(withId(R.id.spinnerEditHarvestDayStatus)).inRoot(isDialog()).perform(object : androidx.test.espresso.ViewAction {
                override fun getDescription() = "Select active day in the real edit dialog"
                override fun getConstraints(): org.hamcrest.Matcher<android.view.View> = isAssignableFrom(android.widget.Spinner::class.java)
                override fun perform(controller: androidx.test.espresso.UiController, view: android.view.View) {
                    (view as android.widget.Spinner).setSelection(0)
                    controller.loopMainThreadUntilIdle()
                }
            })
            onView(withId(R.id.buttonConfirmEditHarvestDay)).perform(scrollTo(), click())
            await { dao.getDayById(day)!!.status == "ACTIVE" }
            assertEquals(before.copy(status="ACTIVE",endTime=null),dao.getDayById(day))
            assertEquals("IN_PROGRESS",dao.getProcessById(before.processId)!!.status)
            scenario.recreate()
            await { var ready=false;scenario.onActivity { ready=it.findViewById<android.widget.Button>(R.id.buttonFinishHarvestDay).visibility==android.view.View.VISIBLE && it.findViewById<TextView>(R.id.textHarvestDayResults).text.isNotEmpty() };ready }
            finishDialog()
            onView(withId(R.id.editFinishHarvestDayArea)).check(matches(withText("3.125")))
            onView(withId(R.id.buttonCancelFinishHarvestDay)).perform(scrollTo(), click())
        }
    }

}
