package ru.fo6osik.workjournal
import android.content.Intent
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
            onView(withHint("Влажность урожая, %")).perform(replaceText("18,5"));closeSoftKeyboard()
            onView(withText("Сохранить")).inRoot(isDialog()).perform(click())
            await { DatabaseProvider.getDatabase(context).harvestDao().getDayById(9001)!!.moisturePercent==18.5 }
            onView(withId(R.id.buttonEditHarvestMoisture)).perform(scrollTo(),click())
            onView(withHint("Влажность урожая, %")).perform(replaceText(""));closeSoftKeyboard()
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
}
