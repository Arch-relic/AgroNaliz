package ru.fo6osik.workjournal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Ordered by run-harvest-checks.py, with a real TEST process force-stop between methods. */
@RunWith(AndroidJUnit4::class)
class HarvestRestartTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val dao get()=DatabaseProvider.getDatabase(context).harvestDao()
    private val prefs get()=context.getSharedPreferences("synthetic-harvest-restart",0)

    @Test fun seedReopenedDay() {
        check(context.packageName=="ru.fo6osik.workjournal.debugtest")
        val process=dao.insertProcess(HarvestProcess(processUid=UUID.randomUUID().toString(),startDate="2026-10-10",
            fieldId=null,fieldNameSnapshot="synthetic restart",crop="Пшеница",combineVehicleId=null,
            combineNameSnapshot="synthetic",bunkerVolumeM3=8.0)).toInt()
        val day=dao.insertDay(HarvestDay(dayUid=UUID.randomUUID().toString(),processId=process,
            workDate="2026-10-10",startTime="10:47",workIntervalMinutes=47,workIntervalCustom=true)).toInt()
        dao.insertBunker(HarvestBunker(bunkerUid=UUID.randomUUID().toString(),dayId=day,
            fillTime="11:47",fillLevel="MORE_THAN_HALF",postEventType="WAITING_TRANSPORT"))
        dao.updateDayDensity(day,"CUSTOM",555.125)
        dao.completeDay(day,"18:00",3.125);dao.completeProcess(process,"2026-10-10")
        dao.updateDayDetails(day,"2026-10-10","10:47",null,"ACTIVE")
        assertTrue(prefs.edit().putInt("process",process).putInt("day",day).commit())
    }

    @Test fun verifyAfterProcessRestart() {
        check(context.packageName=="ru.fo6osik.workjournal.debugtest")
        val process=prefs.getInt("process",-1);val id=prefs.getInt("day",-1)
        assertTrue(process>0 && id>0)
        val day=dao.getDayById(id)!!
        assertEquals("ACTIVE",day.status);assertNull(day.endTime)
        assertEquals("IN_PROGRESS",dao.getProcessById(process)!!.status)
        assertNull(dao.getProcessById(process)!!.endDate)
        assertEquals(3.125,day.harvestedAreaHa!!,0.0)
        assertEquals(555.125,day.bulkDensityKgM3!!,0.0);assertEquals("CUSTOM",day.densityMode)
        assertEquals(47,day.workIntervalMinutes);assertTrue(day.workIntervalCustom)
        assertEquals("11:47",dao.getBunkersForDay(id).single().fillTime)
        assertEquals("WAITING_TRANSPORT",dao.getBunkersForDay(id).single().postEventType)
        dao.completeDay(id,"19:00");dao.completeProcess(process,"2026-10-10")
        assertEquals("COMPLETED",dao.getProcessById(process)!!.status)
        assertEquals("COMPLETED",dao.getDayById(id)!!.status)
        assertEquals(3.125,dao.getDayById(id)!!.harvestedAreaHa!!,0.0)
    }
}
