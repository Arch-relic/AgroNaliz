package ru.fo6osik.workjournal

import org.junit.Assert.*
import org.junit.Test

class HarvestCalculatorTest {
    private fun day(levels: List<String>, area: Double? = null, density: Double? = 750.0) =
        HarvestCalculator.day(8.0, density, HarvestCalculator.DayInput(levels, area))
    private fun range(min: Double, max: Double, actual: HarvestCalculator.Range?) {
        assertNotNull(actual)
        assertEquals(min, actual!!.min, 0.000001)
        assertEquals(max, actual.max, 0.000001)
    }

    @Test fun emptyAreaMeansUnknown() { assertNull(HarvestCalculator.parseOptionalPositive("  ")) }
    @Test fun commaAndPointFractionalArea() {
        assertEquals(12.375, HarvestCalculator.parseOptionalPositive("12,375")!!, 0.0)
        assertEquals(12.375, HarvestCalculator.parseOptionalPositive(" 12.375 ")!!, 0.0)
    }
    @Test fun invalidInputIsRejected() {
        for (input in listOf("0", "0,0", "-1", "NaN", "Infinity", "1e3", "1,2.3", "1 2", ".", "1..2")) {
            try { HarvestCalculator.parseOptionalPositive(input); fail(input) }
            catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun fullBunkerMass() { range(6.0, 6.0, day(listOf("FULL")).massTonnes) }
    @Test fun halfBunkerMass() { range(3.0, 3.0, day(listOf("HALF")).massTonnes) }
    @Test fun moreThanHalfMass() { range(3.0, 6.0, day(listOf("MORE_THAN_HALF")).massTonnes) }
    @Test fun lessThanHalfMassRetainsZeroLowerBound() {
        range(0.0, 3.0, day(listOf("LESS_THAN_HALF")).massTonnes)
    }
    @Test fun allFourFillLevelsRetainVolumeRange() {
        val result = day(listOf("FULL", "MORE_THAN_HALF", "HALF", "LESS_THAN_HALF"), 2.0)
        assertEquals(4, result.bunkerCount)
        range(16.0, 24.0, result.volumeM3)
        range(12.0, 18.0, result.massTonnes)
        range(6.0, 9.0, result.yieldTonnesPerHa)
        range(60.0, 90.0, result.yieldCentnersPerHa)
    }
    @Test fun massWithoutAreaButNoYield() {
        val result = day(listOf("FULL"))
        range(6.0, 6.0, result.massTonnes)
        assertNull(result.yieldTonnesPerHa)
        assertFalse(result.areasComplete)
    }
    @Test fun fractionalAreaYield() {
        val result = day(listOf("FULL"), 2.5)
        range(2.4, 2.4, result.yieldTonnesPerHa)
        range(24.0, 24.0, result.yieldCentnersPerHa)
    }
    @Test fun addingAndChangingAreaRecalculates() {
        assertNull(day(listOf("FULL")).yieldTonnesPerHa)
        range(3.0, 3.0, day(listOf("FULL"), 2.0).yieldTonnesPerHa)
        range(1.5, 1.5, day(listOf("FULL"), 4.0).yieldTonnesPerHa)
    }
    @Test fun zeroNegativeAndNonFiniteAreasNeverDivide() {
        for (area in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(day(listOf("FULL"), area).yieldTonnesPerHa)
            assertNull(day(listOf("FULL"), area).yieldCentnersPerHa)
        }
    }
    @Test fun missingOrInvalidDensityNeverProducesMass() {
        for (density in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val result = day(listOf("FULL"), 2.0, density)
            assertNotNull(result.volumeM3)
            assertNull(result.massTonnes)
            assertNull(result.yieldTonnesPerHa)
        }
    }
    @Test fun missingOrInvalidCapacityDoesNotInventVolume() {
        for (capacity in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(HarvestCalculator.volume(capacity, listOf("FULL")))
        }
    }
    @Test fun unknownFillLevelDoesNotSilentlyUnderestimate() {
        assertNull(day(listOf("FULL", "CORRUPT"), 2.0).massTonnes)
    }
    @Test fun campaignAggregatesMassAndAreaThenDivides() {
        val result = HarvestCalculator.campaign(8.0, 750.0, listOf(
            HarvestCalculator.DayInput(listOf("FULL"), 2.0),
            HarvestCalculator.DayInput(listOf("MORE_THAN_HALF", "HALF"), 3.0)
        ))
        assertEquals(3, result.bunkerCount)
        assertEquals(5.0, result.knownAreaHa, 0.0)
        assertTrue(result.areasComplete)
        range(16.0, 20.0, result.volumeM3)
        range(12.0, 15.0, result.massTonnes)
        range(2.4, 3.0, result.yieldTonnesPerHa)
        range(24.0, 30.0, result.yieldCentnersPerHa)
    }
    @Test fun incompleteAreaDoesNotUseAllMassWithPartialArea() {
        val result = HarvestCalculator.campaign(8.0, 750.0, listOf(
            HarvestCalculator.DayInput(listOf("FULL"), 2.0),
            HarvestCalculator.DayInput(listOf("FULL"), null)
        ))
        range(12.0, 12.0, result.massTonnes)
        assertEquals(2.0, result.knownAreaHa, 0.0)
        assertFalse(result.areasComplete)
        assertNull(result.yieldCentnersPerHa)
        assertTrue(HarvestResultsFormatter.results(result, true).contains("Сведения о площади неполные"))
    }
    @Test fun emptyDayHasZeroMassWithKnownCapacityAndDensity() {
        range(0.0, 0.0, day(emptyList(), 1.0).massTonnes)
        range(0.0, 0.0, day(emptyList(), 1.0).yieldTonnesPerHa)
        assertNull(HarvestCalculator.averageVolume(8.0, emptyList()))
    }
    @Test fun emptyCampaignHasNoYield() {
        val result = HarvestCalculator.campaign(8.0, 750.0, emptyList())
        assertFalse(result.areasComplete)
        assertNull(result.yieldTonnesPerHa)
    }
    @Test fun arithmeticOverflowDoesNotDisplayInfinity() {
        assertNull(HarvestCalculator.volume(Double.MAX_VALUE, listOf("FULL", "FULL")))
        val result = HarvestCalculator.campaign(8.0, 750.0, listOf(
            HarvestCalculator.DayInput(listOf("FULL"), Double.MAX_VALUE),
            HarvestCalculator.DayInput(listOf("FULL"), Double.MAX_VALUE)
        ))
        assertNull(result.yieldTonnesPerHa)
    }
    @Test fun densityCatalogCoversEightCropsAndOtherNeedsManualInput() {
        for (crop in listOf("Пшеница", "Ячмень", "Горох", "Подсолнечник", "Кукуруза", "Рапс", "Овёс", "Рожь")) {
            assertTrue(HarvestDensityCatalog.reference(crop)!! > 0)
        }
        assertNull(HarvestDensityCatalog.effective("Другое", null))
        assertEquals(520.0, HarvestDensityCatalog.effective("Другое", 520.0)!!, 0.0)
        assertEquals(700.0, HarvestDensityCatalog.effective("Пшеница", 700.0)!!, 0.0)
        assertNull(HarvestDensityCatalog.effective("Пшеница", -1.0))
    }
    @Test fun averageVolumeRetainsRange() {
        range(4.0, 6.0, HarvestCalculator.averageVolume(8.0, listOf("FULL", "LESS_THAN_HALF")))
    }
    @Test fun unknownAreaAndMassHaveReadableMessages() {
        val text = HarvestResultsFormatter.results(day(listOf("FULL"), density = null), false)
        assertTrue(text.contains("Фактически убранная площадь: Не указана"))
        assertTrue(text.contains("Урожайность: Не рассчитана"))
        assertTrue(text.contains("плотность культуры"))
    }
}
