package ru.fo6osik.workjournal

import org.junit.Assert.*
import org.junit.Test

class AppChangelogTest {
    @Test fun newestFirstWithStableUniqueKeys() {
        assertEquals(listOf("1.0.10", "1.0.9", "1.0.8", "1.0.7", "early"), AppChangelog.entries.map { it.id })
        assertEquals(AppChangelog.entries.size, AppChangelog.entries.map { it.id }.toSet().size)
    }
    @Test fun noInventedDatesOrDatabaseReleaseNumber() {
        assertTrue(AppChangelog.entries.all { it.releaseDate == null })
        assertTrue(AppChangelog.entries.none { it.version == "24" })
        assertNull(AppChangelog.entries.last().version)
    }
    @Test fun developmentAndCategories() {
        assertEquals("В разработке", AppChangelog.entries.first().status)
        assertTrue(AppChangelog.entries.drop(1).all { it.status == null })
        assertTrue(AppChangelog.entries.all { it.visibleCategories.values.all { changes -> changes.isNotEmpty() } })
        assertTrue(AppChangelog.entries.all { it.summary.isNotBlank() })
    }
    @Test fun expansionIndependentAndRestoredIncludingEmptySelection() {
        val state = ChangelogExpansion("1.0.8")
        assertEquals(setOf("1.0.8"), state.expanded)
        state.toggle("1.0.9"); assertEquals(setOf("1.0.8", "1.0.9"), state.expanded)
        state.toggle("1.0.8"); assertEquals(setOf("1.0.9"), state.expanded)
        assertEquals(state.expanded, ChangelogExpansion("1.0.8", state.expanded).expanded)
        assertTrue(ChangelogExpansion("1.0.8", emptyList()).expanded.isEmpty())
        assertTrue(ChangelogExpansion("unknown", listOf("deleted")).expanded.isEmpty())
    }
}
