package ru.fo6osik.workjournal
import org.junit.Assert.*
import org.junit.Test

class HarvestSliderRulesTest {
    @Test fun defaultsAndAllFiveRanges() {
        assertEquals(listOf(30,5,15,15,30),HarvestSliderRules.defaults)
        assertEquals(listOf(120,15,60,60,120),HarvestSliderRules.maxima)
        for (i in 0..4) {
            val s=HarvestSliderValue(1,HarvestSliderRules.maxima[i],1,HarvestSliderRules.defaults[i].toDouble())
            for (n in 1..s.max) { s.move(n); assertEquals(n.toDouble(),s.value!!,0.0) }
            s.move(-1);assertEquals(1.0,s.value!!,0.0)
            s.move(999);assertEquals(s.max.toDouble(),s.value!!,0.0)
        }
    }
    @Test fun fiveGridIncludesOneAndOnlyMovesRound() {
        val s=HarvestSliderValue(1,120,1,32.0)
        s.step=5;assertEquals(32.0,s.value!!,0.0)
        s.move(32);assertEquals(30.0,s.value!!,0.0)
        s.move(1);assertEquals(1.0,s.value!!,0.0)
        for(n in 1..120) { s.move(n);assertTrue(s.value==1.0 || s.value!!%5==0.0) }
    }
    @Test fun legacyValuesAndFractionalDensityAreNeverClampedOnOpening() {
        for(v in listOf(32.0,600.0,555.125,1200.0,1.0)) {
            val s=HarvestSliderValue(320,900,5,v)
            assertEquals(v,s.value!!,0.0)
        }
        assertNull(HarvestSliderValue(320,900,5,null).value)
    }
    @Test fun densityGridHasIndependentFiveStep() {
        val time=HarvestSliderValue(1,120,1,32.0)
        val density=HarvestSliderValue(320,900,5,555.125)
        time.step=5;assertEquals(5,density.step);assertEquals(555.125,density.value!!,0.0)
        for(n in 320..900) { density.move(n);assertTrue(density.value!! in 320.0..900.0);assertEquals(0.0,density.value!!%5,0.0) }
        density.move(999);assertEquals(900.0,density.value!!,0.0)
    }
    @Test fun examplesRetainTimerFormulaAndCatalogModes() {
        val example=HarvestSliderRules.example(listOf(32,5,15,15,30))
        assertTrue(example.contains("= 37 мин"));assertTrue(example.contains("= 52 мин"));assertTrue(example.contains("= 67 мин"))
        for((crop,v) in HarvestDensityCatalog.values) {
            assertEquals(v.min,HarvestDensityCatalog.selected(crop,"MIN")!!,0.0)
            assertEquals(v.average,HarvestDensityCatalog.selected(crop,"AVERAGE")!!,0.0)
            assertEquals(v.max,HarvestDensityCatalog.selected(crop,"MAX")!!,0.0)
        }
        assertEquals(1200.125,HarvestDensityCatalog.selected("Другое","CUSTOM",1200.125)!!,0.0)
    }
    @Test fun compatibilityExtendsOnlyDocumentedAdditiveColumn() {
        for(v in 18..24) assertTrue(HarvestBackupCompatibility.accepts(24,v))
        assertFalse(HarvestBackupCompatibility.accepts(24,17));assertFalse(HarvestBackupCompatibility.accepts(24,25))
        assertEquals(setOf("reminderStepMinutes"),HarvestBackupCompatibility.allowedMissingColumns(24,23,"harvest_days"))
        assertTrue(HarvestBackupCompatibility.allowedMissingColumns(24,24,"harvest_days").isEmpty())
    }
}
