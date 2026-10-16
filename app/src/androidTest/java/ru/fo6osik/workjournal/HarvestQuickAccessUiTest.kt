package ru.fo6osik.workjournal

import android.widget.Button
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Run on a fresh disposable TEST installation before migration fixtures. */
@RunWith(AndroidJUnit4::class)
class HarvestQuickAccessUiTest {
    @Test fun refreshesOnResumeAndAfterFinalCampaignCompletion() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName=="ru.fo6osik.workjournal.debugtest")
        check(android.os.Build.HARDWARE in setOf("ranchu","goldfish"))
        val db=DatabaseProvider.getDatabase(context)
        check(db.harvestDao().getActiveProcesses().isEmpty())
        fun await(scenario:ActivityScenario<MainActivity>,visible:Boolean) {
            val until=System.nanoTime()+5_000_000_000L
            while(System.nanoTime()<until) {
                var ready=false
                scenario.onActivity { val b=it.findViewById<Button>(R.id.buttonContinueHarvest)
                    ready=b.isEnabled && (b.visibility==View.VISIBLE)==visible }
                if(ready)return
                Thread.sleep(50)
            }
            fail("Quick access did not refresh")
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await(scenario,false)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            val p=db.harvestDao().createProcess(HarvestProcess(processUid="synthetic-quick-ui",startDate="2026-10-16",
                fieldId=null,fieldNameSnapshot="synthetic",crop="Другое",combineVehicleId=null,combineNameSnapshot="synthetic")).toInt()
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);await(scenario,true)
            scenario.recreate();await(scenario,true)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            val d=db.harvestDao().insertDay(HarvestDay(dayUid="synthetic-quick-ui-day",processId=p,workDate="2026-10-16",startTime="08:00")).toInt()
            HarvestDayCompletion.save(db,d,"18:00",null,null,null,false)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);await(scenario,true)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            db.harvestDao().completeProcess(p,"2026-10-16")
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);await(scenario,false)
        }
    }
    @Test fun opensActiveDayOrCardAndRequiresChoiceForMultipleCampaigns() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName=="ru.fo6osik.workjournal.debugtest")
        check(android.os.Build.HARDWARE in setOf("ranchu","goldfish"))
        val db=DatabaseProvider.getDatabase(context);val dao=db.harvestDao()
        check(dao.getActiveProcesses().isEmpty())
        fun campaign(name:String)=dao.createProcess(HarvestProcess(processUid=java.util.UUID.randomUUID().toString(),
            startDate="2026-10-16",fieldId=null,fieldNameSnapshot=name,crop="Другое",combineVehicleId=null,combineNameSnapshot="synthetic")).toInt()
        fun waitFor(type:Class<*>,key:String?=null,id:Int=0) {
            val stop=System.nanoTime()+10_000_000_000L
            while(System.nanoTime()<stop) {
                var ready=false
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    ready=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).any {
                            type.isInstance(it) && (key==null || it.intent.getIntExtra(key,-1)==id) &&
                                (it !is MainActivity || it.findViewById<Button>(R.id.buttonContinueHarvest).isEnabled)
                        }
                }
                if(ready)return
                Thread.sleep(50)
            }
            fail("Expected resumed activity: ${type.simpleName}")
        }
        fun clickContinue() {
            waitFor(MainActivity::class.java)
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.buttonContinueHarvest))
                .perform(androidx.test.espresso.action.ViewActions.scrollTo(),androidx.test.espresso.action.ViewActions.click())
        }
        val a=campaign("synthetic-choice-A")
        val d=dao.insertDay(HarvestDay(dayUid=java.util.UUID.randomUUID().toString(),processId=a,
            workDate="2026-10-16",startTime="08:00")).toInt()
        ActivityScenario.launch(MainActivity::class.java).use {
            clickContinue();waitFor(HarvestDayActivity::class.java,HarvestDayActivity.EXTRA_DAY_ID,d)
            androidx.test.espresso.Espresso.pressBack()
            HarvestDayCompletion.save(db,d,"18:00",null,null,null,false)
            clickContinue();waitFor(HarvestProcessActivity::class.java,HarvestProcessActivity.EXTRA_PROCESS_ID,a)
            androidx.test.espresso.Espresso.pressBack()
            val b=campaign("synthetic-choice-B")
            clickContinue()
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Выберите уборочную кампанию"))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(
                "synthetic-choice-B — Другое · 2026-10-16 · synthetic · №$b"))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(androidx.test.espresso.action.ViewActions.click())
            waitFor(HarvestProcessActivity::class.java,HarvestProcessActivity.EXTRA_PROCESS_ID,b)
            androidx.test.espresso.Espresso.pressBack()
            dao.completeProcess(a,"2026-10-16");dao.completeProcess(b,"2026-10-16")
        }
    }

}
