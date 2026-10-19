package ru.fo6osik.workjournal

import android.content.Intent
import android.view.View
import android.widget.TextView
import android.widget.Switch
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HarvestSlidersUiTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val dao get()=DatabaseProvider.getDatabase(context).harvestDao()
    private var process=0;private var day=0
    @Before fun seed() {
        check(context.packageName=="ru.fo6osik.workjournal.debugtest")
        check(android.os.Build.HARDWARE in setOf("ranchu","goldfish"))
        process=dao.insertProcess(HarvestProcess(processUid=UUID.randomUUID().toString(),startDate="2026-10-01",
            fieldId=null,fieldNameSnapshot="synthetic-sliders",crop="Пшеница",combineVehicleId=null,combineNameSnapshot="synthetic",bunkerVolumeM3=8.0)).toInt()
        day=dao.insertDay(HarvestDay(dayUid=UUID.randomUUID().toString(),processId=process,workDate="2026-10-19",
            startTime="08:00",endTime="18:00",status="COMPLETED",workIntervalMinutes=32,workIntervalCustom=true,harvestedAreaHa=2.0)).toInt()
        dao.insertBunkerRaw(HarvestBunker(bunkerUid=UUID.randomUUID().toString(),dayId=day,fillTime="10:00"))
    }
    @After fun cleanup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
            androidx.test.runner.lifecycle.Stage.values().flatMap { monitor.getActivitiesInStage(it) }.toSet()
                .filter { it.packageName==context.packageName && !it.isFinishing }.forEach { it.finish() }
        }
        DatabaseProvider.getDatabase(context).openHelper.writableDatabase.execSQL("DELETE FROM harvest_processes WHERE id=?",arrayOf(process))
    }
    private fun open()=ActivityScenario.launch<HarvestDayActivity>(Intent(context,HarvestDayActivity::class.java).putExtra(HarvestDayActivity.EXTRA_DAY_ID,day))
    private fun await(condition:()->Boolean) {
        val stop=System.nanoTime()+10_000_000_000L
        while(System.nanoTime()<stop) { if(condition())return;Thread.sleep(50) };fail("Sliders did not reach expected state")
    }
    private fun ready(s:ActivityScenario<HarvestDayActivity>) { await { var ok=false;s.onActivity { ok=it.findViewById<TextView>(R.id.textHarvestDayResults).text.isNotEmpty() };ok } }
    private fun dialog(density:Boolean=false) {
        onView(withContentDescription("Настройки рабочего дня")).perform(click())
        onView(withText(if(density)"Настройка насыпной плотности" else "Настройка событий")).perform(click())
    }
    private fun move(matcher:org.hamcrest.Matcher<View>,value:Int) {
        onView(matcher).inRoot(isDialog()).perform(scrollTo(),object:androidx.test.espresso.ViewAction {
            override fun getConstraints()=isAssignableFrom(HarvestValueSliderView::class.java)
            override fun getDescription()="Move slider through its user selection path"
            override fun perform(c:androidx.test.espresso.UiController,v:View) { (v as HarvestValueSliderView).choose(value);c.loopMainThreadUntilIdle() }
        })
    }
    private fun saveTime()=onView(withId(R.id.buttonSaveHarvestDaySettings)).inRoot(isDialog()).perform(scrollTo(),click())
    @Test fun stepSwitchDoesNotRoundAndCancelNeverWrites() {
        val before=dao.getDayById(day)
        open().use { s->ready(s);dialog()
            onView(withText("5 мин")).inRoot(isDialog()).perform(click())
            onView(withText("Интервал работы: 32 мин")).perform(scrollTo()).check(matches(isDisplayed()))
            move(withId(R.id.sliderHarvestWork),33)
            onView(withId(R.id.textReminderSettingsExample)).check(matches(withText(containsString("= 40 мин"))))
            assertEquals(before,dao.getDayById(day))
            onView(withId(R.id.buttonCancelHarvestDaySettings)).perform(scrollTo(),click())
            assertEquals(before,dao.getDayById(day))
        }
    }
    @Test fun saveStepReopenRecreateAndOtherDayIndependent() {
        val other=dao.insertDay(HarvestDay(dayUid=UUID.randomUUID().toString(),processId=process,workDate="2026-10-20",startTime="08:00")).toInt()
        val beforeOther=dao.getDayById(other)
        open().use { s->ready(s);dialog();onView(withText("5 мин")).perform(click());saveTime()
            await { dao.getDayById(day)!!.reminderStepMinutes==5 }
            assertEquals(32,dao.getDayById(day)!!.workIntervalMinutes)
            s.recreate();ready(s);dialog()
            onView(withText("5 мин")).check(matches(isChecked()))
            onView(withText("Интервал работы: 32 мин")).perform(scrollTo()).check(matches(isDisplayed()))
            move(withId(R.id.sliderHarvestWork),36);saveTime()
            await { dao.getDayById(day)!!.workIntervalMinutes==35 }
            assertEquals(beforeOther,dao.getDayById(other))
        }
    }
    @Test fun completedDaySwitchAndLegacyOutsideRangeRetained() {
        val old=dao.getDayById(day)!!
        dao.updateDayReminderSettings(day,600,old.unloadIntervalMinutes,15,15,30,true,false,false,false,false)
        open().use { s->ready(s);dialog()
            onView(withId(R.id.switchHarvestReminderEnabled)).check(matches(not(isEnabled())))
            onView(withText("Интервал работы: 600 мин")).check(matches(isDisplayed()))
            onView(withText("5 мин")).perform(click())
            saveTime();await { dao.getDayById(day)!!.reminderStepMinutes==5 }
            assertEquals(600,dao.getDayById(day)!!.workIntervalMinutes);assertTrue(dao.getDayById(day)!!.workIntervalCustom)
        }
    }
    @Test fun densityLegacyExactAndCancelNoMassChange() {
        dao.updateDayDensity(day,"CUSTOM",1200.125)
        val before=dao.getDayById(day)!!;val bunkers=dao.getBunkersForDay(day)
        open().use { s->ready(s);dialog(true)
            onView(withText(containsString("Насыпная плотность: 1200,125 кг/м³"))).check(matches(isDisplayed()))
            onView(withText("Сохранить")).inRoot(isDialog()).perform(click())
            assertEquals(before,dao.getDayById(day))
            dialog(true);move(withTagValue(equalTo("harvest-density-slider")),701)
            onView(withText("Отмена")).inRoot(isDialog()).perform(click())
            assertEquals(before,dao.getDayById(day));assertEquals(bunkers,dao.getBunkersForDay(day))
        }
    }
    @Test fun densityModesAndCustomUpdateMassOnly() {
        val bunkerBefore=dao.getBunkersForDay(day)
        open().use { s->ready(s)
            for((label,mode) in listOf("Средняя: 770 кг/м³" to "AVERAGE","Максимальная: 830 кг/м³" to "MAX","Минимальная: 720 кг/м³" to "MIN")) {
                dialog(true);onView(withText(label)).perform(click());onView(withText("Сохранить")).inRoot(isDialog()).perform(click())
                await { dao.getDayById(day)!!.densityMode==mode };Thread.sleep(100)
            }
            dialog(true);onView(withText("Своё значение")).perform(click())
            move(withTagValue(equalTo("harvest-density-slider")),756)
            onView(withText("Сохранить")).inRoot(isDialog()).perform(click())
            await { dao.getDayById(day)!!.bulkDensityKgM3==755.0 }
            assertEquals("CUSTOM",dao.getDayById(day)!!.densityMode)
            assertEquals(bunkerBefore,dao.getBunkersForDay(day))
            val r=HarvestCalculator.day(8.0,HarvestCalculator.DayInput(bunkerBefore.map { it.fillLevel },2.0,755.0))
            assertEquals(6.04,r.massTonnes!!.min,0.000001)
        }
    }
}
