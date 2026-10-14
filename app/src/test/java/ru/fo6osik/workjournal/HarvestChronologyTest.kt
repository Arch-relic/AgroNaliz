package ru.fo6osik.workjournal

import org.junit.Assert.*
import org.junit.Test

class HarvestChronologyTest {
    private fun rejects(conflict: String, block: () -> Unit) {
        try { block(); fail("Expected chronology rejection") } catch(e:IllegalArgumentException) { assertTrue(e.message!!.contains(conflict)) }
    }
    @Test fun reportedEndBeforeLastBunkerIsRejected() { rejects("11:47") { HarvestChronology.validateEnd("10:47","10:48",listOf("10:47","11:47")) } }
    @Test fun endEqualToLastBunkerIsAccepted() { HarvestChronology.validateEnd("10:47","11:47",listOf("11:47")) }
    @Test fun overnightEndAndBunkersAreAccepted() { HarvestChronology.validateEnd("23:00","01:00",listOf("23:30","00:30")) }
    @Test fun overnightEndBeforeLastBunkerIsRejected() { rejects("00:30") { HarvestChronology.validateEnd("23:00","00:20",listOf("23:30","00:30")) } }
    @Test fun lateSameEveningEndBeforeNextMorningBunkerIsRejected() { rejects("00:30") { HarvestChronology.validateEnd("23:00","23:50",listOf("00:30")) } }
    @Test fun bunkerCannotExceedCompletedDayEnd() { rejects("11:47") { HarvestChronology.validateBunker("10:47","11:00","11:47") } }
    @Test fun bunkerCannotExceedOvernightEnd() { rejects("01:30") { HarvestChronology.validateBunker("23:00","01:00","01:30") } }
    @Test fun startCannotMovePastFirstBunker() { rejects("11:47") { HarvestChronology.validateStartChange("10:47","12:00",listOf("11:47")) } }
    @Test fun startChangeCannotSilentlyMoveBunkerAcrossMidnight() { rejects("00:30") { HarvestChronology.validateStartChange("23:00","00:10",listOf("00:30")) } }
    @Test fun validStartChangePreservesOvernightBunkers() { HarvestChronology.validateStartChange("23:00","22:00",listOf("23:30","00:30")) }
    @Test fun invalidClockTimeRejected() { for(t in listOf("24:00","10:60","bad","1:02")) rejects(t) { HarvestChronology.minutes(t) } }
    @Test fun midnightOffsetsAreChronological() { assertEquals(90,HarvestChronology.offset("23:00","00:30"));assertEquals(30,HarvestChronology.offset("23:00","23:30")) }
}
