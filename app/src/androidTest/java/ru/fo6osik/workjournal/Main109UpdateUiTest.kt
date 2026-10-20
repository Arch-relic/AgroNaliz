package ru.fo6osik.workjournal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Loaded only after upgrading; old production APKs do not contain these UI classes. */
@RunWith(AndroidJUnit4::class)
class Main109UpdateUiTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private fun guard() {
        check(context.packageName in setOf("ru.fo6osik.workjournal","ru.fo6osik.workjournal.debugtest"))
        check(android.os.Build.HARDWARE in setOf("ranchu","goldfish"))
    }
    @Test fun verify109Screens() {
        guard()
        assertEquals("1.0.10",context.packageManager.getPackageInfo(context.packageName,0).versionName)
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var ready=false
            val until=System.nanoTime()+10_000_000_000L
            while(!ready && System.nanoTime()<until) {
                scenario.onActivity { activity ->
                    val button=activity.findViewById<android.widget.Button>(R.id.buttonContinueHarvest)
                    ready=button.isEnabled && button.visibility==android.view.View.VISIBLE
                }
                if(!ready) Thread.sleep(50)
            }
            assertTrue("Launcher must reopen migrated active campaign",ready)
        }
        androidx.test.core.app.ActivityScenario.launch(ChangelogActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val scroll=activity.findViewById<android.widget.ScrollView>(R.id.changelogScroll)
                assertEquals(android.view.View.VISIBLE,scroll.findViewWithTag<android.view.View>("changelog-details-1.0.10").visibility)
                assertTrue(scroll.findViewWithTag<android.widget.Button>("changelog-header-1.0.10").text.contains("В разработке"))
            }
        }
        val intent=android.content.Intent(context,HarvestProcessActivity::class.java)
            .putExtra(HarvestProcessActivity.EXTRA_PROCESS_ID,26102).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        androidx.test.core.app.ActivityScenario.launch<HarvestProcessActivity>(intent).use { scenario ->
            val until=System.nanoTime()+10_000_000_000L
            var ready=false
            while(!ready && System.nanoTime()<until) {
                scenario.onActivity { activity ->
                    val card=activity.findViewById<HarvestResultsCardView>(R.id.harvestResultsCard)
                    val calendar=activity.findViewById<HarvestDayCalendarView>(R.id.harvestDayCalendar)
                    ready=card.currentModel!=null && calendar.calendar.month=="2026-10"
                    if(ready) assertEquals("4",card.currentModel!!.overview[1].second)
                }
                if(!ready) Thread.sleep(50)
            }
            assertTrue("Migrated campaign results and calendar must render",ready)
        }
    }
}
