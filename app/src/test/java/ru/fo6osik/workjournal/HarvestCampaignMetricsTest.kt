package ru.fo6osik.workjournal

import org.junit.Assert.*
import org.junit.Test

class HarvestCampaignMetricsTest {
    private fun day(area: Double? = 60.0, density: Double? = 1000.0, fill: String = "FULL") =
        HarvestCalculator.DayInput(listOf(fill), area, density)
    private fun metrics(actual: Double?, days: List<HarvestCalculator.DayInput>) =
        HarvestCampaignMetrics.calculate(150.0, actual, days, days)

    @Test fun controlExampleAndIndependentAreaEdit() {
        val a = metrics(95.0, listOf(day()))
        assertEquals(60.0, a.harvestedHa, 0.0)
        assertEquals(35.0, a.remainingHa!!, 0.0)
        assertEquals(63.1578947, a.progressPercent!!, 0.000001)
        assertEquals(150.0 / 95, a.wholeAreaYield!!.min, 0.000001)
        assertEquals(2.5, a.harvestedAreaYield!!.max, 0.0)
        val b = metrics(90.0, listOf(day()))
        assertEquals(30.0, b.remainingHa!!, 0.0)
        assertEquals(a.mass, b.mass)
        assertEquals(a.harvestedHa, b.harvestedHa, 0.0)
        assertEquals(a.harvestedAreaYield, b.harvestedAreaYield)
    }
    @Test fun retainsMassRangeAndRecalculatesEditedDay() {
        val a = metrics(95.0, listOf(day(fill = "MORE_THAN_HALF")))
        assertEquals(75.0 / 95, a.wholeAreaYield!!.min, 0.0)
        assertEquals(150.0 / 95, a.wholeAreaYield!!.max, 0.0)
        val b = metrics(95.0, listOf(day(30.0)))
        assertEquals(30.0, b.harvestedHa, 0.0)
        assertEquals(5.0, b.harvestedAreaYield!!.min, 0.0)
    }
    @Test fun unknownAreaIsNotZeroOrExactTotal() {
        val a = metrics(95.0, listOf(day(), day(null)))
        assertEquals(1, a.unknownAreaDays)
        assertEquals(60.0, a.harvestedHa, 0.0)
        assertNull(a.remainingHa); assertNull(a.progressPercent); assertNull(a.harvestedAreaYield)
    }
    @Test fun incompleteMassNeverBecomesExactYield() {
        val a = metrics(95.0, listOf(day(), day(10.0, null)))
        assertFalse(a.massesComplete); assertNotNull(a.mass)
        assertNull(a.wholeAreaYield); assertNull(a.harvestedAreaYield)
    }
    @Test fun unknownZeroAreaAndNoDays() {
        for (value in listOf(null, 0.0)) {
            val a = metrics(value, listOf(day()))
            assertNull(a.wholeAreaYield); assertNull(a.progressPercent); assertNull(a.remainingHa)
        }
        val empty = metrics(95.0, emptyList())
        assertNull(empty.wholeAreaYield); assertNull(empty.harvestedAreaYield)
        assertEquals(0.0, empty.progressPercent!!, 0.0)
    }
    @Test fun excessIsWarningAndIsNotClamped() {
        val a = metrics(50.0, listOf(day()))
        assertTrue(a.exceedsActual)
        assertEquals(-10.0, a.remainingHa!!, 0.0)
        assertEquals(120.0, a.progressPercent!!, 0.0)
    }
    @Test fun activeDayDoesNotEnterCompletedArea() {
        val a = HarvestCampaignMetrics.calculate(150.0, 95.0, listOf(day()), listOf(day(), day(20.0)))
        assertEquals(60.0, a.harvestedHa, 0.0)
        assertEquals(300.0, a.mass!!.min, 0.0)
    }
    @Test fun onlyDocumentedVersionsAndColumnsAllowed() {
        for (version in 18..23) assertTrue(HarvestBackupCompatibility.accepts(23, version))
        assertFalse(HarvestBackupCompatibility.accepts(23, 17))
        assertFalse(HarvestBackupCompatibility.accepts(23, 24))
        assertEquals(setOf("fieldAreaSnapshotHa", "actualPlantedAreaHa"),
            HarvestBackupCompatibility.allowedMissingColumns(23, 22, "harvest_processes"))
        assertTrue(HarvestBackupCompatibility.allowedMissingColumns(23, 23, "harvest_processes").isEmpty())
    }
}
