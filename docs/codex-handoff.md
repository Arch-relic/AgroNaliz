# Передача контекста: индивидуальная плотность и урожайность

Дата: 2026-10-13. Статус: реализация готовится к проверке GitHub Actions.
Ветка feature/harvest-day-density; основа main c0ea3d01a97dd0eb4814b59307b44169916821bc.
Room v20→v21, без destructive migration. Посторонние PR, включая #25, не изменены.

Реализованы таблица MIN/AVERAGE/MAX/CUSTOM, независимые плотности дней, общий
расчёт диапазонов объёма/массы/урожайности, неполные итоги кампании, ввод/очистка
площади, отдельное меню событий и плотности, предупреждение при смене культуры.
Совместимость старых backup и сохранение новых полей при импорте.

Локальная команда source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test
не запустилась: Could not determine a usable wildcard IP for this machine.
Сборка и тесты будут проверены Actions; непроведённые проверки не объявляются успешными.
Тесты: 20 новых JVM, 16 инструментальных проверок с реальным обновлением v20 TEST APK.
PR #26: https://github.com/Arch-relic/AgroNaliz/pull/26 — открыт, Draft, не объединён. Проверенный кодовый SHA будет указан после CI.
Файлы и окончательные результаты будут дополнены после проверки.
Удалённых файлов нет. Механизм подписи APK не менялся; PR не объединять.
Ручная проверка на телефоне и реальном урожае не выполнялась.

Изменения DAO дополнительно проверяют конечность/положительность площади и отказ
от повторного завершения дня. Backup/импорт проверяют режим и плотность до записи.
Проверки v21 backup и импорта добавлены в инструментальный набор.

## Исправление замечаний PR #26

Предыдущий код 86bfa274e1e4276c11fcfd68818031573359e795 прошёл Actions #55:
https://github.com/Arch-relic/AgroNaliz/actions/runs/38361657619
Собраны основная/TEST APK, JVM, 13 catalog checks, изоляция, 16 инструментальных
проверок урожайности, включая реальный v20→v21 update и сохранение данных.

Новые исправления пока ожидают CI: HarvestChronology, транзакционные проверки
вставки/редактирования бункеров, завершения/редактирования дня, сортировка через
полночь, сохранение площади при повторном завершении, согласованные состояния
дня/кампании, действие продолжения. Запрещены второй активный день и завершение
кампании до закрытия активного дня. Старые записи автоматически не меняются.
Добавлены 12 JVM regression tests, 4 Room lifecycle tests и 2 campaign UI tests;
Area UI дополнен повторным открытием через реальный диалог. Всего планируется
23 harvest инструментальных выполнения. Успех новых тестов ещё не объявляется.

Ограничение схемы: HH:mm без даты бункера; время до начала относится к следующим
суткам, смена менее 24 ч. При изменении начала сторона полуночи существующих
бункеров сохраняется либо сохранение отклоняется. Смены 24 ч и более не представлены.

## Файлы текущего результата

- .github/workflows/android-pr-checks.yml
- app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt
- app/src/main/java/ru/fo6osik/workjournal/DataImportManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseBackupManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseProvider.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDao.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDay.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDayActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestProcessActivity.kt
- app/src/main/res/layout/activity_harvest_day.xml
- app/src/main/res/layout/activity_harvest_process.xml
- app/src/main/res/layout/dialog_add_harvest_bunker.xml
- app/src/main/res/layout/dialog_edit_harvest_day.xml
- app/src/main/res/layout/dialog_finish_harvest_day.xml
- docs/codex-handoff.md
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestAreaUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestBackupTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestCampaignStateUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestLifecycleTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestMigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestPersistenceTest.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestBackupCompatibility.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestCalculator.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestChronology.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDensityCatalog.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestResultsFormatter.kt
- app/src/main/res/layout/dialog_harvest_number.xml
- app/src/main/res/values/harvest_results.xml
- app/src/test/java/ru/fo6osik/workjournal/HarvestCalculatorTest.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestChronologyTest.kt
- docs/harvest-results.md
- docs/run-harvest-checks.py

Удалённых файлов нет. Локальный worktree содержит незакоммиченные зеркала;
GitHub-коммиты создаются отдельно через API с parent из ветки PR #26.
Новый regression CI запускается, отчёт будет дополнен проверенным SHA и результатами.

CI #56: сборки и JVM успешны; прежний тест независимости плотностей создавал
два активных дня одновременно и теперь корректно отклонён новым DAO guard.
Исправлена подготовка теста: предыдущий день закрывается перед следующим;
закрытая кампания явно открывается перед созданием нового дня. Защита не ослаблена.
