package ru.fo6osik.workjournal

import android.content.Intent
import android.view.View
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HarvestCampaignStateUiTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val dao get()=DatabaseProvider.getDatabase(context).harvestDao()
    private fun fixture(): Pair<Int,Int> {
        check(context.packageName=="ru.fo6osik.workjournal.debugtest")
        val process=dao.insertProcess(HarvestProcess(processUid=UUID.randomUUID().toString(),startDate="2026-10-10",
            fieldId=null,fieldNameSnapshot="synthetic",crop="Пшеница",combineVehicleId=null,combineNameSnapshot="synthetic",bunkerVolumeM3=8.0)).toInt()
        val day=dao.insertDay(HarvestDay(dayUid=UUID.randomUUID().toString(),processId=process,workDate="2026-10-10",startTime="08:00")).toInt()
        context.getSharedPreferences("harvest_reminder_prefs",0).edit().putBoolean("enabled_$day",false).commit()
        dao.completeDay(day,"18:00",2.0);dao.completeProcess(process,"2026-10-10")
        return process to day
    }
    private fun open(process:Int)=ActivityScenario.launch<HarvestProcessActivity>(Intent(context,HarvestProcessActivity::class.java)
        .putExtra(HarvestProcessActivity.EXTRA_PROCESS_ID,process))
    private fun await(scenario:ActivityScenario<HarvestProcessActivity>,text:String) {
        val end=System.currentTimeMillis()+10000
        while(true) {
            var ready=false
            scenario.onActivity { val b=it.findViewById<Button>(R.id.buttonNewHarvestDay);ready=b.visibility==View.VISIBLE && b.text.toString()==text && it.findViewById<HarvestResultsCardView>(R.id.harvestResultsCard).currentModel != null }
            if(ready)return
            if(System.currentTimeMillis()>end)fail("Campaign continuation action did not update")
            Thread.sleep(50)
        }
    }
    @Test fun reopenedDayHasContinueActionAfterActivityRecreation() {
        val(process,day)=fixture();val before=dao.getDayById(day)!!
        dao.updateDayDetails(day,before.workDate,before.startTime,null,"ACTIVE")
        open(process).use { scenario ->
            await(scenario,"Продолжить рабочий день");scenario.recreate();await(scenario,"Продолжить рабочий день")
            onView(withId(R.id.buttonNewHarvestDay)).perform(scrollTo(),click())
            onView(withId(R.id.buttonFinishHarvestDay)).check(matches(withText("Рабочий день окончен")))
            androidx.test.espresso.Espresso.pressBack()
        }
        assertEquals(1,dao.getDaysForProcess(process).size)
    }
    @Test fun reopenedCampaignWithoutActiveDayHasNewDayAction() {
        val(process,_)=fixture();dao.reopenProcess(process)
        open(process).use { scenario ->
            await(scenario,"Новый день");scenario.recreate();await(scenario,"Новый день")
            onView(withId(R.id.buttonNewHarvestDay)).perform(scrollTo(),click())
            onView(withId(R.id.buttonCancelNewHarvestDay)).perform(click())
        }
    }
}
