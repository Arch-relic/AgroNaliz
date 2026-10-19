package ru.fo6osik.workjournal

import org.junit.Assert.*
import org.junit.Test

class HarvestResultsCardModelTest {
    private fun day(area: Double? = 90.0, density: Double? = 1000.0, fill: String = "FULL") =
        HarvestCalculator.DayInput(listOf(fill), area, density)
    private fun metrics(days: List<HarvestCalculator.DayInput> = listOf(day()), capacity: Double? = 150.0,
                        actual: Double? = 100.0, snapshot: Double? = 120.0, finished: Boolean = true,
                        completed: List<HarvestCalculator.DayInput> = days) =
        HarvestCampaignMetrics.calculate(capacity, actual, completed, days, finished, snapshot)
    private fun card(r: HarvestCampaignMetrics.Result, inProgress: Boolean = false) =
        HarvestResultsCardModel.from(r, 120.0, 100.0, inProgress)

    @Test fun overviewFormatsReadyMetricsAndHasExactlyFourCells() {
        val r = metrics(); val c = card(r)
        assertEquals(listOf("Площадь", "Бункеры", "Масса урожая", "Урожайность"), c.overview.map { it.first })
        assertEquals("90 га", c.overview[0].second); assertEquals("1", c.overview[1].second)
        assertEquals("150 т", c.overview[2].second)
        assertEquals("1,67 т/га\n16,67 ц/га", c.overview[3].second)
        assertEquals("90,0%", c.progressText); assertEquals(90, c.progressBar)
        assertEquals("10 га", c.details.toMap()["Осталось убрать"])
        assertTrue(c.shortNotice.isEmpty())
    }
    @Test fun allThreeRangesRetainBoundsAndUnits() {
        val c = card(metrics(listOf(day(fill = "MORE_THAN_HALF"))))
        assertEquals("75–150 т", c.overview[2].second)
        assertEquals("75–150 м³", c.details.toMap()["Общий расчётный объём бункеров"])
        assertTrue(c.overview[3].second.contains("–")); assertTrue(c.overview[3].second.contains("ц/га"))
    }
    @Test fun emptyCampaignDoesNotInventMassOrYield() {
        val c = card(metrics(emptyList()))
        assertEquals("0", c.overview[1].second)
        assertEquals("—", c.overview[2].second); assertEquals("—\n—", c.overview[3].second)
        assertTrue(c.warnings.any { it.contains("Рабочих дней пока нет") })
    }
    @Test fun unknownAreasDoNotBecomeZerosOrProgressTargets() {
        val r = metrics(listOf(day(null)), actual = null, snapshot = null)
        val c = HarvestResultsCardModel.from(r, null, null, false)
        assertEquals("—", c.overview[0].second); assertEquals("—\n—", c.overview[3].second)
        assertNull(c.progressBar); assertEquals("—", c.progressText)
        assertEquals("Неизвестно", c.details.toMap()["Источник расчётной площади"])
        assertEquals("—", c.details.toMap()["Фактическая площадь посева"])
        assertTrue(c.warnings.any { it.contains("Целевая площадь неизвестна") })
    }
    @Test fun partialMassIsClearlyMarkedAndNeverProducesExactYield() {
        val c = card(metrics(listOf(day(), day(density = null))))
        assertTrue(c.overview[2].second.contains("150 т")); assertTrue(c.overview[2].second.contains("частично"))
        assertEquals("—\n—", c.overview[3].second)
        assertTrue(c.shortNotice.contains("Неполные данные"))
        assertTrue(c.warnings.any { it.contains("Масса рассчитана не полностью") })
    }
    @Test fun missingCapacityAndDensityShowDashes() {
        for (r in listOf(metrics(capacity = null), metrics(listOf(day(density = null))))) {
            val c = card(r)
            assertEquals("—", c.overview[2].second); assertEquals("—\n—", c.overview[3].second)
        }
        assertEquals("—", card(metrics(capacity = null)).details.toMap()["Общий расчётный объём бункеров"])
    }
    @Test fun partialCompletedAreaHasExactWarningAndKnownPartLabel() {
        val c = card(metrics(listOf(day(60.0), day(null))))
        assertTrue(c.warnings.contains("Прогресс предварительный: площадь указана не для всех рабочих дней"))
        assertTrue(c.details.toMap()["Убрано по завершённым рабочим дням"]!!.contains("известная часть"))
        assertEquals("1", c.details.toMap()["Завершённых дней с неизвестной площадью"])
        assertEquals("60,0%", c.progressText)
    }
    @Test fun activeMassAndAreaTreatmentIsExplained() {
        val a = day(20.0)
        val c = card(metrics(listOf(day(), a), finished = false, completed = listOf(day())), true)
        assertEquals("300 т", c.overview[2].second)
        assertTrue(c.shortNotice.contains("Предварительно"))
        assertTrue(c.warnings.any { it.contains("масса уже включена") && it.contains("после завершения") })
    }
    @Test fun excessOnlyClampsVisualBarAndRetainsNegativeRemainder() {
        val c = card(metrics(listOf(day(110.0))))
        assertEquals("110,0%", c.progressText); assertEquals(100, c.progressBar)
        assertEquals("-10 га", c.details.toMap()["Осталось убрать"])
        assertTrue(c.warnings.any { it.contains("превышает целевую") })
        assertFalse(c.shortNotice.contains("Неполные данные"))
    }
    @Test fun sourceAndSeparateHarvestedYieldAreIncludedWhenAvailable() {
        val c = card(metrics())
        assertEquals("Сумма гектаров рабочих дней", c.details.toMap()["Источник расчётной площади"])
        assertEquals("1,67 т/га", c.details.toMap()["Урожайность по убранной площади"])
        val snapshot = card(metrics(listOf(day(null)), actual = null))
        assertEquals("Площадь поля при создании кампании", snapshot.details.toMap()["Источник расчётной площади"])
    }
}
