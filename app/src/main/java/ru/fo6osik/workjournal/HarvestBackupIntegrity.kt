package ru.fo6osik.workjournal

import org.json.JSONObject

/** Check source identities before any restore/import writes, including rows a merge would skip. */
internal object HarvestBackupIntegrity {
    fun validate(tables: JSONObject) {
        fun indexed(table: String, id: String, uid: String): Map<Long, JSONObject> {
            val rows = tables.optJSONArray(table) ?: return emptyMap()
            val result = mutableMapOf<Long, JSONObject>()
            val uids = mutableSetOf<String>()
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val value = row.getLong(id)
                val identity = row.getString(uid)
                require(value > 0 && identity.isNotBlank() && result.put(value, row) == null && uids.add(identity)) {
                    "Повторяющийся или пустой идентификатор в $table"
                }
            }
            return result
        }
        val processes = indexed("harvest_processes", "id", "processUid")
        val days = indexed("harvest_days", "id", "dayUid")
        val bunkers = indexed("harvest_bunkers", "id", "bunkerUid")
        val fuel = indexed("harvest_fuel_days", "dayId", "dayUid")
        val refills = indexed("harvest_refuelings", "id", "refuelUid")
        for (row in processes.values) require(row.getString("status") in setOf("IN_PROGRESS", "COMPLETED"))
        val dates = mutableSetOf<Pair<Long, String>>()
        val active = mutableSetOf<Long>()
        for (row in days.values) {
            val processId = row.getLong("processId")
            val process = requireNotNull(processes[processId]) { "Нет кампании для рабочего дня" }
            require(dates.add(processId to row.getString("workDate"))) { "Повтор даты рабочего дня" }
            require(row.getString("status") in setOf("ACTIVE", "COMPLETED"))
            HarvestChronology.minutes(row.getString("startTime"))
            if (row.getString("status") == "ACTIVE") {
                require(row.isNull("endTime") && process.getString("status") == "IN_PROGRESS" && active.add(processId)) {
                    "Несогласованные активные дни кампании"
                }
            } else HarvestChronology.validateEnd(row.getString("startTime"), row.getString("endTime"), emptyList())
        }
        for (row in bunkers.values) {
            val day = requireNotNull(days[row.getLong("dayId")]) { "Нет рабочего дня для бункера" }
            require(row.getString("fillLevel") in setOf("FULL", "HALF", "MORE_THAN_HALF", "LESS_THAN_HALF"))
            HarvestChronology.validateBunker(day.getString("startTime"),
                if (day.isNull("endTime")) null else day.getString("endTime"), row.getString("fillTime"))
        }
        for (row in fuel.values + refills.values) {
            val day = requireNotNull(days[row.getLong("dayId")]) { "Нет рабочего дня для топлива" }
            require(row.getString("dayUid") == day.getString("dayUid")) { "Неверный UID рабочего дня в топливе" }
        }
    }
}
