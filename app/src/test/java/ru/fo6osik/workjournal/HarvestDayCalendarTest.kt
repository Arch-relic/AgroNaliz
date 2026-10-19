package ru.fo6osik.workjournal

import org.junit.Assert.*
import org.junit.Test

class HarvestDayCalendarTest {
    private fun day(date: String = "2026-10-19", area: Double? = 10.0, id: Int = 1,
                    status: String = "COMPLETED", bunkers: Int = 2) =
        HarvestDaySummary(id, 1, date, "08:00", if (status == "ACTIVE") null else "18:00",
            status, bunkers, area, null, null)
    private fun model(vararg days: HarvestDaySummary) = HarvestDayCalendar().apply { update(days.toList()) }

    @Test fun singleMonthStartsWithoutSelection() {
        val m = model(day())
        assertEquals(listOf("2026-10"), m.months); assertEquals("2026-10", m.month)
        assertNull(m.selectedDate); assertNull(m.selected)
    }
    @Test fun latestMonthIsInitialEvenForUnsortedRecords() {
        val m = model(day("2026-01-01"), day("2026-10-19"), day("2026-03-01"))
        assertEquals("2026-10", m.month)
        assertEquals(3, m.months.size)
    }
    @Test fun decemberToJanuaryAcrossYears() {
        val m = model(day("2025-12-31"), day("2026-01-01"))
        m.move(-1); assertEquals("2025-12", m.month)
        m.move(1); assertEquals("2026-01", m.month)
        m.move(1); assertEquals("2026-01", m.month)
    }
    @Test fun skipsMonthsWithoutRecords() {
        val m = model(day("2025-12-01"), day("2026-10-19"))
        m.move(-1); assertEquals("2025-12", m.month)
        m.move(1); assertEquals("2026-10", m.month)
    }
    @Test fun mondayFirstGridHasRealOffsetsAndLeapDays() {
        val m = model(day("2026-10-19"))
        assertEquals(listOf(null, null, null, "2026-10-01"), m.cells().take(4))
        assertEquals(35, m.cells().size)
        m.update(listOf(day("2024-02-29")))
        assertTrue(m.cells().contains("2024-02-29")); assertFalse(m.cells().contains("2024-02-30"))
        assertEquals(0, m.cells().size % 7)
    }
    @Test fun validAreaIsGreenIncludingActiveDay() {
        assertEquals(HarvestDayCalendar.Mark.GREEN, model(day(status = "ACTIVE")).summary("2026-10-19")!!.mark)
    }
    @Test fun unknownOrInvalidAreaIsAmberAndNotZero() {
        for (value in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val s = model(day(area = value)).summary("2026-10-19")!!
            assertEquals(HarvestDayCalendar.Mark.AMBER, s.mark); assertNull(s.areaHa)
        }
    }
    @Test fun emptyDateCannotBeSelected() {
        val m = model(day()); m.toggle("2026-10-18")
        assertNull(m.summary("2026-10-18")); assertNull(m.selectedDate)
    }
    @Test fun repeatTapClearsAndOtherDateSwitchesSelection() {
        val m = model(day(), day("2026-10-20"))
        m.toggle("2026-10-19"); assertEquals("2026-10-19", m.selectedDate)
        m.toggle("2026-10-20"); assertEquals("2026-10-20", m.selectedDate)
        m.toggle("2026-10-20"); assertNull(m.selected)
    }
    @Test fun changingMonthClearsSelection() {
        val m = model(day(), day("2026-01-01"))
        m.toggle("2026-10-19"); m.move(-1); assertNull(m.selected)
        m.toggle("2026-10-19"); assertNull(m.selected)
    }
    @Test fun singleSummaryPreservesIdentityTimeStatusAndCounts() {
        val d = day(); val s = model(d).summary(d.workDate)!!
        assertEquals(listOf(d), s.records); assertEquals(2, s.bunkerCount)
        assertEquals(10.0, s.areaHa!!, 0.0); assertEquals("08:00", s.start)
        assertEquals("18:00", s.end); assertEquals("Завершён", s.status)
    }
    @Test fun groupedDateOnlyUsesCompleteAreaAndRetainsIndividualRecords() {
        val a = day(); val b = day(id = 2, area = 4.5, status = "ACTIVE", bunkers = 3).copy(startTime = "09:00")
        val m = model(b, a); val s = m.summary(a.workDate)!!
        assertEquals(listOf(a, b), s.records); assertEquals(5, s.bunkerCount)
        assertEquals(14.5, s.areaHa!!, 0.0); assertNull(s.end)
        assertEquals("Есть активные записи", s.status)
        m.update(listOf(a, b.copy(harvestedAreaHa = null)))
        assertNull(m.summary(a.workDate)!!.areaHa)
        assertEquals(HarvestDayCalendar.Mark.AMBER, m.summary(a.workDate)!!.mark)
    }
    @Test fun refreshAndRestoreKeepOnlyValidMonthAndSelection() {
        val d = day(); val m = model(d); m.toggle(d.workDate)
        m.update(listOf(d.copy(harvestedAreaHa = 20.0)))
        assertEquals(20.0, m.selected!!.areaHa!!, 0.0)
        val restored = HarvestDayCalendar().apply { update(listOf(d), m.month, m.selectedDate) }
        assertEquals(d.workDate, restored.selectedDate)
        m.update(listOf(day("2026-01-01")))
        assertEquals("2026-01", m.month); assertNull(m.selected)
    }
    @Test fun emptyAndMalformedDatesAreSafe() {
        val m = model(); assertTrue(m.months.isEmpty()); assertTrue(m.cells().isEmpty()); assertNull(m.month)
        m.move(-1); m.toggle("2026-10-19"); assertNull(m.selected)
        m.update(listOf(day("2026-02-30"), day("bad")))
        assertTrue(m.months.isEmpty())
    }
}
