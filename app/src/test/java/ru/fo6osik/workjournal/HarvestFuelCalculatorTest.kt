package ru.fo6osik.workjournal
import org.junit.Assert.*
import org.junit.Test
class HarvestFuelCalculatorTest {
    private fun r(a:Double?,b:Double?,vararg ref:HarvestFuelCalculator.Refill)=HarvestFuelCalculator.calculate(a,b,ref.toList(),2.0)
    @Test fun noRefills(){assertEquals(25.0,r(80.0,55.0).consumed!!,0.0)}
    @Test fun example(){assertEquals(85.0,r(80.0,55.0,HarvestFuelCalculator.Refill(30.0,90.0)).consumed!!,0.0)}
    @Test fun multiple(){assertEquals(150.0,r(80.0,30.0,HarvestFuelCalculator.Refill(20.0,80.0),HarvestFuelCalculator.Refill(40.0,80.0)).consumed!!,0.0)}
    @Test fun betweenDaysNotCountedTwice(){assertEquals(25.0,r(80.0,55.0,HarvestFuelCalculator.Refill(20.0,80.0,true)).consumed!!,0.0)}
    @Test fun perHectare(){assertEquals(12.5,r(80.0,55.0).perHa!!,0.0)}
    @Test fun unknown(){assertNull(r(null,55.0).consumed);assertNull(r(80.0,null).consumed)}
    @Test fun zeroIsKnown(){assertEquals(0.0,r(0.0,0.0).consumed!!,0.0)}
    @Test fun negativeWarns(){val v=r(10.0,80.0);assertNull(v.consumed);assertNotNull(v.warning)}
    @Test fun invalidRefill(){assertNotNull(r(80.0,10.0,HarvestFuelCalculator.Refill(90.0,20.0)).warning)}
    @Test fun noArea(){for(a in listOf(null,0.0,Double.NaN))assertNull(HarvestFuelCalculator.calculate(80.0,55.0,emptyList(),a).perHa)}
    @Test fun commaAndOptional(){assertEquals(12.5,HarvestFuelCalculator.parse("12,5")!!,0.0);assertNull(HarvestFuelCalculator.parse(""))}
    @Test fun invalidNumbers(){for(s in listOf("-1","101","NaN","Infinity","abc"))try{HarvestFuelCalculator.parse(s);fail(s)}catch(_:IllegalArgumentException){}}
    @Test fun compactUnknown(){val v=HarvestCalculator.day(null,HarvestCalculator.DayInput(emptyList(),null,null));val s=HarvestResultsFormatter.results(v,false);assertTrue(s.contains("Масса: —"));assertTrue(s.contains("Площадь: —"));assertFalse(s.contains("ц/га"))}
}
