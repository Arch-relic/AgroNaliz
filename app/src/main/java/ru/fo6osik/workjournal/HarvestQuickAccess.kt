package ru.fo6osik.workjournal

object HarvestQuickAccess {
    data class Target(val processId: Int, val dayId: Int?)

    /** Re-read both records together on every tap, including after selection in the dialog. */
    fun resolve(database: AppDatabase, processId: Int): Target? {
        var target: Target? = null
        database.runInTransaction {
            val dao = database.harvestDao()
            val process = dao.getProcessById(processId)
            if (process?.status == "IN_PROGRESS") {
                val active = dao.getDaysForProcess(processId).filter { it.status == "ACTIVE" }
                check(active.size <= 1) { "В кампании несколько активных дней. Откройте карточку уборки." }
                target = Target(processId, active.singleOrNull()?.id)
            }
        }
        return target
    }
}
