package ru.fo6osik.workjournal

import org.junit.Assert.*
import org.junit.Test

class HarvestCalculatorTest {
    private fun input(area: Double? = 2.0, density: Double? = 720.0, vararg levels: String) =
        HarvestCalculator.DayInput(levels.toList().ifEmpty { listOf("FULL") }, area, density)
    @Test fun catalogMatchesAllSpecifiedValues() {
        val expected = listOf(listOf(720.0,770.0,830.0),listOf(580.0,640.0,700.0),listOf(750.0,780.0,820.0),listOf(320.0,400.0,440.0),listOf(680.0,730.0,780.0),listOf(620.0,660.0,700.0),listOf(400.0,480.0,550.0),listOf(680.0,710.0,750.0))
        assertEquals(expected, HarvestDensityCatalog.values.values.map { listOf(it.min,it.average,it.max) })
        assertEquals(8, HarvestDensityCatalog.values.size)
    }
    @Test fun initialDensityIsMin() { assertEquals("MIN" to 720.0,HarvestDensityCatalog.initial("Пшеница")) }
    @Test fun otherAndUnsupportedAreUnknown() { for(c in listOf("Другое","unknown")) assertEquals(null to null,HarvestDensityCatalog.initial(c)) }
    @Test fun modesSelectReference() { assertEquals(770.0,HarvestDensityCatalog.selected("Пшеница","AVERAGE")!!,0.0);assertEquals(830.0,HarvestDensityCatalog.selected("Пшеница","MAX")!!,0.0) }
    @Test fun customDensityIsExact() { assertEquals(555.125,HarvestDensityCatalog.selected("Другое","CUSTOM",555.125)!!,0.0) }
    @Test fun invalidDensityRejected() { for(v in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY)) assertNull(HarvestDensityCatalog.selected("Пшеница","CUSTOM",v)) }
    @Test fun decimalInputAndUnknown() { assertEquals(12.375,HarvestCalculator.parseOptionalPositive("12,375")!!,0.0);assertEquals(12.375,HarvestCalculator.parseOptionalPositive("12.375")!!,0.0);assertNull(HarvestCalculator.parseOptionalPositive(" ")) }
    @Test fun invalidInputRejected() { for(s in listOf("0","-1","NaN","Infinity","1e309","1,2.3")) { try { HarvestCalculator.parseOptionalPositive(s);fail(s) } catch(_:IllegalArgumentException) {} } }
    @Test fun fullVolume() { assertEquals(HarvestCalculator.Range(8.0,8.0),HarvestCalculator.volume(8.0,listOf("FULL"))) }
    @Test fun halfVolume() { assertEquals(HarvestCalculator.Range(4.0,4.0),HarvestCalculator.volume(8.0,listOf("HALF"))) }
    @Test fun moreThanHalfVolume() { assertEquals(HarvestCalculator.Range(4.0,8.0),HarvestCalculator.volume(8.0,listOf("MORE_THAN_HALF"))) }
    @Test fun lessThanHalfVolume() { assertEquals(HarvestCalculator.Range(0.0,4.0),HarvestCalculator.volume(8.0,listOf("LESS_THAN_HALF"))) }
    @Test fun unknownLevelDoesNotBecomeZero() { assertNull(HarvestCalculator.volume(8.0,listOf("unknown"))) }
    @Test fun massAndYieldRange() {
        val r=HarvestCalculator.day(8.0,input(2.0,750.0,"MORE_THAN_HALF"))
        assertEquals(HarvestCalculator.Range(3.0,6.0),r.massTonnes)
        assertEquals(HarvestCalculator.Range(1.5,3.0),r.yieldTonnesPerHa)
        assertEquals(HarvestCalculator.Range(15.0,30.0),r.yieldCentnersPerHa)
    }
    @Test fun missingAreaStillCalculatesMass() { val r=HarvestCalculator.day(8.0,input(null));assertNotNull(r.massTonnes);assertNull(r.yieldTonnesPerHa) }
    @Test fun zeroAreaNoDivision() { assertNull(HarvestCalculator.day(8.0,input(0.0)).yieldTonnesPerHa) }
    @Test fun missingDensityIsUnknownMass() { val r=HarvestCalculator.day(8.0,input(density=null));assertNull(r.massTonnes);assertNull(r.yieldTonnesPerHa) }
    @Test fun campaignUsesIndividualDensities() { val r=HarvestCalculator.campaign(8.0,listOf(input(density=500.0),input(density=1000.0)));assertEquals(12.0,r.massTonnes!!.min,0.0);assertEquals(3.0,r.yieldTonnesPerHa!!.min,0.0);assertTrue(r.massesComplete) }
    @Test fun partialCampaignUsesMatchedDaysOnly() {
        val r=HarvestCalculator.campaign(8.0,listOf(input(2.0,500.0),input(null,1000.0),input(3.0,null)))
        assertEquals(12.0,r.massTonnes!!.min,0.0);assertEquals(5.0,r.knownAreaHa,0.0)
        assertEquals(2.0,r.yieldTonnesPerHa!!.min,0.0);assertEquals(1,r.includedYieldDays)
        assertFalse(r.areasComplete);assertFalse(r.massesComplete)
        assertTrue(HarvestResultsFormatter.results(r,true).contains("Неполные данные"))
    }
    @Test fun emptyCampaignAndOverflowAreSafe() { assertNull(HarvestCalculator.campaign(8.0,emptyList()).yieldTonnesPerHa);assertNull(HarvestCalculator.day(Double.MAX_VALUE,input(density=Double.MAX_VALUE)).massTonnes) }
}
