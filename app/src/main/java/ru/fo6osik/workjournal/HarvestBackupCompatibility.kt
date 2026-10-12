package ru.fo6osik.workjournal

/** Only documented additive changes are accepted when restoring older full backups. */
object HarvestBackupCompatibility {
    fun accepts(current: Int, backup: Int): Boolean = current == backup ||
        (current in 20..21 && backup in 18 until current)

    fun allowedMissingColumns(current: Int, backup: Int, table: String): Set<String> {
        if (!accepts(current, backup) || current == backup) return emptySet()
        val columns = mutableSetOf<String>()
        if (table == "harvest_days") {
            if (backup < 20) columns.addAll(setOf(
                "workIntervalMinutes", "unloadIntervalMinutes", "lunchIntervalMinutes",
                "dinnerIntervalMinutes", "waitingTransportIntervalMinutes", "workIntervalCustom",
                "unloadIntervalCustom", "lunchIntervalCustom", "dinnerIntervalCustom",
                "waitingTransportIntervalCustom", "reminderSettingsInitialized"
            ))
            if (current == 21 && backup < 21) columns.add("harvestedAreaHa")
        }
        if (table == "harvest_processes" && current == 21 && backup < 21) columns.add("bulkDensityKgM3")
        if (table == "harvest_bunkers" && backup == 18) columns.add("postEventType")
        return columns
    }
}
