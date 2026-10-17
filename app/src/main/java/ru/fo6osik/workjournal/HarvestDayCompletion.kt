package ru.fo6osik.workjournal

/** A single transaction covers the day, its end fuel reading and optional campaign completion. */
object HarvestDayCompletion {
    fun save(database: AppDatabase, dayId: Int, endTime: String, area: Double?,
             moisture: Double?, endFuel: Double?, finishProcess: Boolean) {
        database.runInTransaction {
            val dao = database.harvestDao()
            val day = requireNotNull(dao.getDayById(dayId)) { "Рабочий день удалён" }
            check(dao.getProcessById(day.processId)?.status == "IN_PROGRESS") { "Кампания уже завершена" }
            check(dao.completeDay(dayId, endTime, area, moisture) == 1) { "День уже завершён" }
            database.harvestFuelDao().saveEndLevel(dayId, endFuel)
            if (finishProcess) dao.completeProcess(day.processId, day.workDate)
        }
    }
}
