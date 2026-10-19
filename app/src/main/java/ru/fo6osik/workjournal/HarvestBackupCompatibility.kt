package ru.fo6osik.workjournal

/** Only documented additive changes are accepted when restoring older full backups. */
object HarvestBackupCompatibility {
    fun accepts(current: Int, backup: Int): Boolean = current == backup ||
        (current in 20..24 && backup in 18 until current)

    fun allowedMissingColumns(current: Int, backup: Int, table: String): Set<String> {
        if (!accepts(current, backup) || current == backup) return emptySet()
        val columns = mutableSetOf<String>()
        if (table == "harvest_processes" && current >= 23 && backup < 23)
            columns.addAll(setOf("fieldAreaSnapshotHa", "actualPlantedAreaHa"))
        if (table == "harvest_days") {
            if (current >= 24 && backup < 24) columns.add("reminderStepMinutes")
            if (backup < 20) columns.addAll(setOf(
                "workIntervalMinutes", "unloadIntervalMinutes", "lunchIntervalMinutes",
                "dinnerIntervalMinutes", "waitingTransportIntervalMinutes", "workIntervalCustom",
                "unloadIntervalCustom", "lunchIntervalCustom", "dinnerIntervalCustom",
                "waitingTransportIntervalCustom", "reminderSettingsInitialized"
            ))
            if (current >= 21 && backup < 21) columns.addAll(setOf("harvestedAreaHa", "bulkDensityKgM3", "densityMode"))
        }
        if (table == "harvest_days" && current >= 22 && backup < 22) columns.add("moisturePercent")
        if (table == "harvest_bunkers" && backup == 18) columns.add("postEventType")
        return columns
    }
}
