package ru.fo6osik.workjournal

import org.junit.Assert.*
import org.junit.Test

class HarvestFixedFillTest {
    private val levels = listOf("LESS_THAN_HALF", "HALF", "MORE_THAN_HALF", "FULL")
    private fun exact(expected: Double, value: HarvestCalculator.Range?) {
        assertNotNull(value); assertEquals(expected, value!!.min, 1e-12); assertEquals(value.min, value.max, 0.0)
    }
    @Test fun fourCoefficientsForFiveAndNovaCapacity() {
        for ((i, level) in levels.withIndex()) {
            exact(5.0 * (i+1) / 4, HarvestCalculator.volume(5.0, listOf(level)))
            exact(4.7 * (i+1) / 4, HarvestCalculator.volume(4.7, listOf(level)))
        }
    }
    @Test fun tenFullAndLastQuarterAreSummedBeforeRounding() {
        val fills = List(10) { "FULL" } + "LESS_THAN_HALF"
        val result = HarvestCalculator.day(4.7, HarvestCalculator.DayInput(fills, 10.0, 720.0))
        exact(48.175, result.volumeM3); exact(34.686, result.massTonnes)
        exact(3.4686, result.yieldTonnesPerHa); exact(34.686, result.yieldCentnersPerHa)
        assertEquals("1,175 м³", HarvestResultsFormatter.range(HarvestCalculator.volume(4.7,listOf("LESS_THAN_HALF"))!!,"м³"))
        assertEquals("34,686 т", HarvestResultsFormatter.range(result.massTonnes!!,"т"))
        assertFalse(HarvestResultsFormatter.results(result, false).contains('–'))
    }
    @Test fun everyDensityModeUsesOneStoredValue() {
        for ((mode, custom, expected) in listOf(Triple("MIN",null,720.0), Triple("AVERAGE",null,770.0),
                Triple("MAX",null,830.0), Triple("CUSTOM",555.125,555.125))) {
            val density = HarvestDensityCatalog.selected("Пшеница",mode,custom)
            assertEquals(expected,density!!,0.0)
            exact(4.7*expected/1000,HarvestCalculator.day(4.7,HarvestCalculator.DayInput(listOf("FULL"),1.0,density)).massTonnes)
        }
    }
    @Test fun historicalCodesAreReadRepeatedlyWithoutTransformingInputs() {
        val original = levels.toList()
        val input = HarvestCalculator.DayInput(original, 2.0, 720.0)
        val first = HarvestCalculator.day(4.7,input)
        repeat(10) { assertEquals(first,HarvestCalculator.day(4.7,input)) }
        assertEquals(levels,original); exact(11.75,first.volumeM3)
    }
    @Test fun multipleDaysUseIndividualDensitiesAndExistingAreaRules() {
        val a = HarvestCalculator.DayInput(listOf("FULL"),2.0,720.0)
        val b = HarvestCalculator.DayInput(listOf("LESS_THAN_HALF"),3.0,800.0)
        val r = HarvestCampaignMetrics.calculate(4.7,null,listOf(a,b),listOf(a,b),true)
        exact(5.875,r.volumeM3); exact(4.324,r.mass); exact(4.324/5,r.wholeAreaYield)
        assertEquals(5.0,r.selectedArea.hectares!!,0.0)
    }
    @Test fun unknownInputsNeverInventCapacityDensityOrArea() {
        val input = HarvestCalculator.DayInput(listOf("FULL"),null,720.0)
        assertNull(HarvestCalculator.day(null,input).massTonnes)
        assertNull(HarvestCalculator.day(4.7,input).yieldTonnesPerHa)
        assertNull(HarvestCalculator.day(4.7,input.copy(bulkDensityKgM3=null)).massTonnes)
        assertNull(HarvestCalculator.volume(4.7,listOf("UNKNOWN")))
    }
}
