package ru.fo6osik.workjournal

/** Bundled editorial history. Dates are release dates, never inferred from commits. */
data class ChangelogEntry(
    val id: String,
    val title: String,
    val version: String? = null,
    val releaseDate: String? = null,
    val status: String? = null,
    val summary: String,
    val categories: Map<String, List<String>>
) {
    val visibleCategories get() = categories.filterValues { it.isNotEmpty() }
}

object AppChangelog {
    // Newest first. Add entries here without changing the screen or database.
    val entries = listOf(
        ChangelogEntry("1.0.11", "1.0.11", version = "1.0.11", status = "В разработке",
            summary = "Точный расчёт уборки с учётом заполненности бункеров.",
            categories = linkedMapOf(
                "Улучшения" to listOf("Заполненность бункеров 25%, 50%, 75% и 100%; единый расчёт объёма, массы и урожайности без диапазонов.",
                    "Объём берётся из карточки комбайна, а при недоступности карточки — из сохранённого значения кампании."),
                "Исправления" to listOf("Старые записи и восстановленные резервные копии рассчитываются по новым коэффициентам без перезаписи событий и дней.",
                    "Малые объёмы и массы показываются с точностью до трёх знаков."))),
        ChangelogEntry("1.0.10", "1.0.10", version = "1.0.10",
            summary = "Улучшения интерфейса и сведения об установленной сборке.",
            categories = linkedMapOf(
                "Исправления" to listOf("Числа в результатах уборки не разрываются при переносе; карточка подстраивается под ширину экрана."),
                "Улучшения" to listOf("Заголовок «АгроНализ»; версия, код, схема и полный отпечаток сертификата в «Опциях»."),
                "Технические изменения" to listOf("Подготовлена проверка обновления с сохранением данных без изменения схемы базы."))),
        ChangelogEntry("1.0.9", "1.0.9", version = "1.0.9",
            summary = "Новые расчёты и удобное управление уборочной кампанией.",
            categories = linkedMapOf(
                "Новое" to listOf("Календарь рабочих дней с выбором даты и краткой сводкой.",
                    "Компактная карточка результатов с подробной статистикой.",
                    "Офлайн-история изменений приложения."),
                "Улучшения" to listOf("Выбор площади для урожайности: полные данные завершённой кампании, фактическая площадь посева или снимок поля.",
                    "Целевая площадь прогресса учитывается отдельно от площади для урожайности.",
                    "Ползунки времени с шагом 1/5 минут и плотности с шагом 5 кг/м³; настройки сохраняются отдельно для каждого дня."),
                "Технические изменения" to listOf("Добавлено сохранение шага времени с переносом старых данных и совместимостью резервных копий."))),
        ChangelogEntry("1.0.8", "1.0.8", version = "1.0.8",
            summary = "Завершение рабочего дня, быстрый доступ к уборке и учёт площадей.",
            categories = linkedMapOf(
                "Новое" to listOf("Единое завершение дня: время, гектары, влажность и конечное топливо.",
                    "Кнопка «ПРОДОЛЖИТЬ УБОРКУ» для активных кампаний.",
                    "Снимок площади поля и независимая фактическая площадь посева."),
                "Улучшения" to listOf("Общие резервные копии сохраняют данные уборки, включая топливо и площади.",
                    "Блок «Для разработчика» перенесён в «Опции»; данные и резервные копии открываются одной кнопкой."),
                "Исправления" to listOf("Конечное топливо сохраняется без потери начального остатка; завершение дня выполняется целиком или откатывается при ошибке."))),
        ChangelogEntry("1.0.7", "1.0.7", version = "1.0.7",
            summary = "Учёт топлива и заправок в уборке.",
            categories = linkedMapOf(
                "Новое" to listOf("Учёт топлива и заправок."),
                "Исправления" to listOf("Исправление выбора времени (TimePicker)."))),
        ChangelogEntry("early", "Ранние этапы разработки",
            summary = "Основные разделы приложения. Историческая запись неполная.",
            categories = linkedMapOf("Новое" to listOf("Главное меню и список техники.",
                "История технического обслуживания и ремонтов, техническая документация и PDF-просмотрщик.",
                "Список полей, рабочие записи и журнал событий.", "Раздел уборки урожая.")))
    )
}

/** UI state only: never persisted in Room or preferences. */
class ChangelogExpansion(currentVersion: String, restored: Collection<String>? = null) {
    val expanded = (restored ?: AppChangelog.entries.filter { it.version == currentVersion }.map { it.id })
        .filter { id -> AppChangelog.entries.any { it.id == id } }.toMutableSet()
    fun toggle(id: String) { if (!expanded.remove(id)) expanded.add(id) }
}
