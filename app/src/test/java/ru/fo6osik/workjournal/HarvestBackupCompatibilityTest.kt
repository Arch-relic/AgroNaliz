package ru.fo6osik.workjournal

import org.junit.Assert.*
import org.junit.Test

class HarvestBackupCompatibilityTest {
    @Test fun v21AcceptsOnlySupportedLegacyFullBackups() {
        for (version in 18..21) assertTrue(HarvestBackupCompatibility.accepts(21, version))
        for (version in listOf(16, 17, 22)) assertFalse(HarvestBackupCompatibility.accepts(21, version))
    }
    @Test fun v20OnlyOmitsNewNullableHarvestColumns() {
        assertEquals(setOf("harvestedAreaHa"), HarvestBackupCompatibility.allowedMissingColumns(21, 20, "harvest_days"))
        assertEquals(setOf("bulkDensityKgM3"), HarvestBackupCompatibility.allowedMissingColumns(21, 20, "harvest_processes"))
        assertTrue(HarvestBackupCompatibility.allowedMissingColumns(21, 20, "vehicles").isEmpty())
    }
    @Test fun v18AndV19RetainTimerAndPostEventCompatibility() {
        assertEquals(12, HarvestBackupCompatibility.allowedMissingColumns(21, 18, "harvest_days").size)
        assertEquals(12, HarvestBackupCompatibility.allowedMissingColumns(21, 19, "harvest_days").size)
        assertEquals(setOf("postEventType"), HarvestBackupCompatibility.allowedMissingColumns(21, 18, "harvest_bunkers"))
        assertTrue(HarvestBackupCompatibility.allowedMissingColumns(21, 19, "harvest_bunkers").isEmpty())
    }
    @Test fun currentBackupMustIncludeEveryColumn() {
        assertTrue(HarvestBackupCompatibility.allowedMissingColumns(21, 21, "harvest_days").isEmpty())
        assertTrue(HarvestBackupCompatibility.allowedMissingColumns(21, 21, "harvest_processes").isEmpty())
    }
}
