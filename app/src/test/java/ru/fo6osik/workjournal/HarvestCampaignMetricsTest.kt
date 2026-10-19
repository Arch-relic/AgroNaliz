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
        assertEquals(35.0, a.remainingHa!!, 0.0)
        assertEquals(60.0 / 95 * 100, a.progressPercent!!, 0.0)
        assertFalse(a.progressComplete)
        assertNull(a.harvestedAreaYield)
        assertTrue(HarvestCampaignMetrics.text(null, 95.0, false, a).contains("неполные данные"))
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
    private fun campaign(days: List<HarvestCalculator.DayInput>, actual: Double? = 95.0,
                         snapshot: Double? = 120.0, finished: Boolean = true,
                         completed: List<HarvestCalculator.DayInput> = days) =
        HarvestCampaignMetrics.calculate(150.0, actual, completed, days, finished, snapshot)

    @Test fun completedCampaignUsesAllDaysBeforeOtherSources() {
        val result = campaign(listOf(day(60.0), day(30.0)))
        assertEquals(HarvestCampaignMetrics.AreaSource.COMPLETED_DAYS, result.selectedArea.source)
        assertEquals(90.0, result.selectedArea.hectares!!, 0.0)
        assertEquals(300.0 / 90, result.wholeAreaYield!!.min, 0.0)
        assertEquals(90.0 / 95 * 100, result.progressPercent!!, 0.0)
        assertEquals(5.0, result.remainingHa!!, 0.0)
    }
    @Test fun missingOrInvalidDayAreaFallsBackWithoutUsingPartialSum() {
        for (invalid in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val result = campaign(listOf(day(), day(invalid)))
            assertEquals(HarvestCampaignMetrics.AreaSource.ACTUAL_PLANTED, result.selectedArea.source)
            assertEquals(95.0, result.selectedArea.hectares!!, 0.0)
            assertEquals(1, result.unknownAreaDays)
            assertFalse(result.progressComplete)
            assertEquals(300.0 / 95, result.wholeAreaYield!!.min, 0.0)
        }
    }
    @Test fun snapshotIsFallbackForMissingOrInvalidActualArea() {
        for (invalid in listOf(null, 0.0, -1.0, Double.NaN, Double.NEGATIVE_INFINITY)) {
            val result = campaign(listOf(day(null)), actual = invalid)
            assertEquals(HarvestCampaignMetrics.AreaSource.FIELD_SNAPSHOT, result.selectedArea.source)
            assertEquals(120.0, result.selectedArea.hectares!!, 0.0)
            assertEquals(1.25, result.wholeAreaYield!!.min, 0.0)
        }
    }
    @Test fun noValidSourceDoesNotInventZeroAreaOrYield() {
        for (invalid in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val result = campaign(listOf(day(null)), actual = invalid, snapshot = invalid)
            assertEquals(HarvestCampaignMetrics.AreaSource.UNKNOWN, result.selectedArea.source)
            assertNull(result.selectedArea.hectares)
            assertNull(result.wholeAreaYield); assertNull(result.yieldCentnersPerHa)
            assertNull(result.progressPercent); assertNull(result.remainingHa)
        }
    }
    @Test fun ongoingCampaignDoesNotUseDaySumEvenWhenAllDaysAreCompleted() {
        val days = listOf(day())
        val result = campaign(days, finished = false)
        assertEquals(HarvestCampaignMetrics.AreaSource.ACTUAL_PLANTED, result.selectedArea.source)
        assertEquals(95.0, result.selectedArea.hectares!!, 0.0)
        assertEquals(HarvestCampaignMetrics.AreaSource.FIELD_SNAPSHOT,
            campaign(days, actual = null, finished = false).selectedArea.source)
        assertEquals(HarvestCampaignMetrics.AreaSource.UNKNOWN,
            campaign(days, actual = null, snapshot = null, finished = false).selectedArea.source)
    }
    @Test fun selectionChecksAllDaysNotOnlyCompletedSubset() {
        val known = day()
        val result = campaign(listOf(known, day(null)), completed = listOf(known))
        assertEquals(HarvestCampaignMetrics.AreaSource.ACTUAL_PLANTED, result.selectedArea.source)
        assertEquals(60.0, result.harvestedHa, 0.0)
        assertEquals(1, result.activeDays)
    }
    @Test fun editingCompletedDayRecalculatesFinalAreaAndYield() {
        val before = campaign(listOf(day(60.0), day(30.0)))
        val after = campaign(listOf(day(60.0), day(40.0)))
        assertEquals(100.0, after.selectedArea.hectares!!, 0.0)
        assertEquals(3.0, after.wholeAreaYield!!.min, 0.0)
        assertEquals(before.mass, after.mass)
        assertNotEquals(before.wholeAreaYield, after.wholeAreaYield)
    }
    @Test fun individualDayDensityChangesOnlyItsMassContribution() {
        val before = campaign(listOf(day(30.0, 500.0), day(30.0, 1000.0)))
        val after = campaign(listOf(day(30.0, 800.0), day(30.0, 1000.0)))
        assertEquals(225.0, before.mass!!.min, 0.0)
        assertEquals(270.0, after.mass!!.min, 0.0)
        assertEquals(4.5, after.wholeAreaYield!!.min, 0.0)
        assertEquals(before.selectedArea, after.selectedArea)
    }
    @Test fun unknownMassIsNotZeroAndPartialMassDoesNotProduceCampaignYield() {
        val unknown = campaign(listOf(day(density = null)))
        assertNull(unknown.mass); assertFalse(unknown.massesComplete)
        assertNull(unknown.wholeAreaYield); assertNull(unknown.yieldCentnersPerHa)
        val partial = campaign(listOf(day(), day(density = null)))
        assertEquals(150.0, partial.mass!!.min, 0.0)
        assertFalse(partial.massesComplete); assertNull(partial.wholeAreaYield)
        assertNull(campaign(listOf(day(fill = "UNKNOWN"))).wholeAreaYield)
    }
    @Test fun preservesVolumeMassYieldRangesAndCentnerConversionWithoutRounding() {
        val result = campaign(listOf(day(7.0, 750.0, "MORE_THAN_HALF")))
        assertEquals(1, result.bunkerCount)
        assertEquals(HarvestCalculator.Range(75.0, 150.0), result.volumeM3)
        assertEquals(HarvestCalculator.Range(56.25, 112.5), result.mass)
        assertEquals(56.25 / 7, result.wholeAreaYield!!.min, 0.0)
        assertEquals(112.5 / 7, result.wholeAreaYield!!.max, 0.0)
        assertEquals(56.25 / 7 * 10, result.yieldCentnersPerHa!!.min, 0.0)
        assertEquals(112.5 / 7 * 10, result.yieldCentnersPerHa!!.max, 0.0)
    }
    @Test fun excessRemainsVisibleForSnapshotAndPartialProgress() {
        val result = campaign(listOf(day(), day(null)), actual = null, snapshot = 50.0)
        assertEquals(120.0, result.progressPercent!!, 0.0)
        assertEquals(-10.0, result.remainingHa!!, 0.0)
        assertTrue(result.exceedsActual); assertFalse(result.progressComplete)
        val text = HarvestCampaignMetrics.text(50.0, null, false, result)
        assertTrue(text.contains("Внимание")); assertTrue(text.contains("неполные данные"))
    }
    @Test fun emptyCampaignNeverSelectsZeroDayTotal() {
        val result = campaign(emptyList())
        assertEquals(HarvestCampaignMetrics.AreaSource.ACTUAL_PLANTED, result.selectedArea.source)
        assertNull(result.mass); assertNull(result.wholeAreaYield)
        assertEquals(0, result.bunkerCount); assertEquals(0, result.unknownAreaDays)
        assertEquals(0.0, result.progressPercent!!, 0.0)
        assertEquals(HarvestCampaignMetrics.AreaSource.UNKNOWN,
            campaign(emptyList(), actual = null, snapshot = null).selectedArea.source)
    }
    @Test fun overflowAreaSumFallsBackAndDoesNotProduceInfiniteProgress() {
        val result = campaign(listOf(day(Double.MAX_VALUE), day(Double.MAX_VALUE)))
        assertEquals(HarvestCampaignMetrics.AreaSource.ACTUAL_PLANTED, result.selectedArea.source)
        assertNull(result.progressPercent); assertNull(result.remainingHa)
        assertFalse(result.progressComplete)
    }
    @Test fun yieldAreaAndProgressTargetRemainIndependent() {
        val result = campaign(listOf(day(90.0)), actual = 100.0)
        assertEquals(90.0, result.selectedArea.hectares!!, 0.0)
        assertEquals(100.0, result.targetArea.hectares!!, 0.0)
        assertEquals(90.0, result.progressPercent!!, 0.0)
        assertEquals(10.0, result.remainingHa!!, 0.0)
        assertEquals(150.0 / 90, result.wholeAreaYield!!.min, 0.0)
    }
    @Test fun targetFallsBackToSnapshotButNeverCompletedDays() {
        val result = campaign(listOf(day(90.0)), actual = null, snapshot = 120.0)
        assertEquals(HarvestCampaignMetrics.AreaSource.FIELD_SNAPSHOT, result.targetArea.source)
        assertEquals(75.0, result.progressPercent!!, 0.0)
        assertEquals(30.0, result.remainingHa!!, 0.0)
        val unknown = campaign(listOf(day(90.0)), actual = null, snapshot = null)
        assertEquals(90.0, unknown.selectedArea.hectares!!, 0.0)
        assertNotNull(unknown.wholeAreaYield)
        assertNull(unknown.targetArea.hectares); assertNull(unknown.progressPercent); assertNull(unknown.remainingHa)
    }
    @Test fun invalidTargetsAreRejectedIndependentlyFromYieldArea() {
        for (invalid in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val result = campaign(listOf(day()), actual = invalid, snapshot = invalid)
            assertEquals(HarvestCampaignMetrics.AreaSource.UNKNOWN, result.targetArea.source)
            assertEquals(HarvestCampaignMetrics.AreaSource.COMPLETED_DAYS, result.selectedArea.source)
            assertNull(result.progressPercent); assertFalse(result.exceedsActual)
        }
    }
    @Test fun finishedCampaignExcessUsesTargetNotFinalDaySum() {
        val result = campaign(listOf(day(110.0)), actual = 100.0)
        assertEquals(110.0, result.progressPercent!!, 0.0000001)
        assertEquals(-10.0, result.remainingHa!!, 0.0); assertTrue(result.exceedsActual)
        assertEquals(150.0 / 110, result.wholeAreaYield!!.min, 0.0)
    }
    @Test fun changingTargetDoesNotChangeFinalYieldOrMass() {
        val before = campaign(listOf(day(90.0)), actual = 100.0)
        val after = campaign(listOf(day(90.0)), actual = 120.0)
        assertEquals(before.selectedArea, after.selectedArea); assertEquals(before.mass, after.mass)
        assertEquals(before.wholeAreaYield, after.wholeAreaYield)
        assertEquals(75.0, after.progressPercent!!, 0.0)
        assertEquals(30.0, after.remainingHa!!, 0.0)
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
