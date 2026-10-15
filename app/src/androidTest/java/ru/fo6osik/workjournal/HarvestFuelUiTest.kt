package ru.fo6osik.workjournal
import android.content.Intent
import android.view.View
import android.widget.TimePicker
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAction
import androidx.test.espresso.UiController
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matcher
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class HarvestFuelUiTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private fun open():ActivityScenario<HarvestDayActivity> {
        check(context.packageName=="ru.fo6osik.workjournal.debugtest")
        return ActivityScenario.launch(Intent(context,HarvestDayActivity::class.java).putExtra(HarvestDayActivity.EXTRA_DAY_ID,9001))
    }
    private fun await(block:()->Boolean){val stop=System.currentTimeMillis()+10000;while(!block()){if(System.currentTimeMillis()>stop)fail("UI state timeout");Thread.sleep(100)}}
    @Test fun moistureCompletedEditAndClear(){
        open().use {
            onView(withId(R.id.buttonEditHarvestMoisture)).perform(scrollTo(),click())
            onView(withHint("Влажность урожая, %")).inRoot(isDialog()).perform(replaceText("18,5"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard())
            onView(withText("Сохранить")).inRoot(isDialog()).perform(click())
            await { DatabaseProvider.getDatabase(context).harvestDao().getDayById(9001)!!.moisturePercent==18.5 }
            onView(withId(R.id.buttonEditHarvestMoisture)).perform(scrollTo(),click())
            onView(withHint("Влажность урожая, %")).inRoot(isDialog()).perform(replaceText(""),androidx.test.espresso.action.ViewActions.closeSoftKeyboard())
            onView(withText("Сохранить")).inRoot(isDialog()).perform(click())
            await { DatabaseProvider.getDatabase(context).harvestDao().getDayById(9001)!!.moisturePercent==null }
            DatabaseProvider.getDatabase(context).harvestDao().updateMoisture(9001,14.25)
        }
    }
    @Test fun fuelIconOpensIndependentDialog(){
        open().use {
            onView(withId(R.id.buttonHarvestFuel)).perform(scrollTo(),click())
            onView(withText("Топливо за день")).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText("Добавить заправку")).inRoot(isDialog()).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("Закрыть")).inRoot(isDialog()).perform(click())
            assertEquals("COMPLETED",DatabaseProvider.getDatabase(context).harvestDao().getDayById(9001)!!.status)
        }
    }

    private fun refillView(matcher:Matcher<View>)=onView(matcher).inRoot(allOf(isDialog(),withDecorView(
        hasDescendant(allOf(withContentDescription("До заправки, %"),withEffectiveVisibility(Visibility.VISIBLE))))))
    private fun openRefill() {
        onView(withId(R.id.buttonHarvestFuel)).perform(scrollTo(),click())
        // Wait for the existing worker to finish loading before opening the editor.
        await { DatabaseProvider.getDatabase(context).harvestFuelDao().getDay(9001)!=null }
        onView(withText("Добавить заправку")).inRoot(isDialog()).perform(scrollTo(),click())
        refillView(withContentDescription("До заправки, %")).perform(replaceText("41"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard())
        refillView(withContentDescription("После заправки, %")).perform(replaceText("81"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard())
    }
    private fun chooseTime(hour:Int,minute:Int,expectedHour:Int?=null,expectedMinute:Int?=null) {
        refillView(withContentDescription("Время заправки")).perform(scrollTo(),click())
        onView(isAssignableFrom(TimePicker::class.java)).inRoot(isDialog()).perform(object:ViewAction {
            override fun getDescription()="Select a 24-hour refueling time"
            override fun getConstraints():Matcher<View> = isAssignableFrom(TimePicker::class.java)
            override fun perform(ui:UiController,view:View) {
                val picker=view as TimePicker
                assertTrue(picker.is24HourView)
                expectedHour?.let { assertEquals(it,picker.hour) }
                expectedMinute?.let { assertEquals(it,picker.minute) }
                picker.hour=hour;picker.minute=minute;ui.loopMainThreadUntilIdle()
            }
        })
        onView(withId(android.R.id.button1)).inRoot(allOf(isDialog(),withDecorView(
            hasDescendant(isAssignableFrom(TimePicker::class.java))))).perform(click())
    }
    private fun temporaryRefill()=DatabaseProvider.getDatabase(context).harvestFuelDao().getRefills(9001)
        .singleOrNull { it.beforePercent==41.0 && it.afterPercent==81.0 }
    private fun saveRefill() {
        refillView(withText("Сохранить")).perform(click())
        await { temporaryRefill()!=null }
    }
    private fun editTemporary() {
        val edit=onView(allOf(withText("Изменить"),hasSibling(withText(org.hamcrest.Matchers.containsString("41% → 81%")))))
            .inRoot(allOf(isDialog(),withDecorView(hasDescendant(withText("Топливо за день")))))
        await {
            try { edit.check(matches(withEffectiveVisibility(Visibility.VISIBLE)));true }
            catch(_:NoMatchingViewException) { false }
        }
        edit.perform(scrollTo(),click())
    }
    @Test fun pickerSavesLeadingZerosAndEditsExistingTime() {
        try { open().use {
            openRefill();chooseTime(0,5);saveRefill()
            val original=temporaryRefill()!!;assertEquals("00:05",original.time)
            editTemporary()
            refillView(withText("Время: 00:05")).check(matches(isDisplayed()))
            chooseTime(23,59,0,5);refillView(withText("Сохранить")).perform(click())
            await { temporaryRefill()?.time=="23:59" }
            assertEquals(original.refuelUid,temporaryRefill()!!.refuelUid)
            editTemporary();chooseTime(1,2,23,59)
            refillView(withText("Отмена")).perform(click())
            assertEquals("23:59",temporaryRefill()!!.time)
        } } finally { temporaryRefill()?.let { DatabaseProvider.getDatabase(context).harvestFuelDao().deleteRefill(it.id) } }
    }
    @Test fun betweenShiftsTimeRemainsOptionalAndCanBeCleared() {
        try { open().use {
            openRefill()
            refillView(withText("Между сменами, перед этим рабочим днём")).perform(scrollTo(),click())
            saveRefill();assertNull(temporaryRefill()!!.time);assertEquals("BEFORE_DAY",temporaryRefill()!!.scope)
            editTemporary();chooseTime(9,7);refillView(withText("Сохранить")).perform(click())
            await { temporaryRefill()?.time=="09:07" }
            editTemporary();refillView(withText("Очистить время")).perform(scrollTo(),click())
            refillView(withText("Сохранить")).perform(click());await { temporaryRefill()?.time==null }
        } } finally { temporaryRefill()?.let { DatabaseProvider.getDatabase(context).harvestFuelDao().deleteRefill(it.id) } }
    }
    @Test fun pickerCancelPreservesPreviouslySavedTime() {
        val dao=DatabaseProvider.getDatabase(context).harvestFuelDao()
        try {
            dao.saveRefill(9001,null,41.0,81.0,"12:30","DAY")
            open().use {
                onView(withId(R.id.buttonHarvestFuel)).perform(scrollTo(),click());editTemporary()
                refillView(withText("Время: 12:30")).check(matches(isDisplayed()))
                refillView(withContentDescription("Время заправки")).perform(scrollTo(),click())
                onView(withId(android.R.id.button2)).inRoot(allOf(isDialog(),withDecorView(
                    hasDescendant(isAssignableFrom(TimePicker::class.java))))).perform(click())
                refillView(withText("Сохранить")).perform(click())
                assertEquals("12:30",temporaryRefill()!!.time)
            }
        } finally { temporaryRefill()?.let { dao.deleteRefill(it.id) } }
    }
}
