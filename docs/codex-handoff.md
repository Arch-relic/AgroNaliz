# Уборка: влажность, компактная статистика и топливо

Дата: 2026-10-15. Статус: реализация сохранена локально; Android-сборка, миграционные instrumentation-тесты и публикация не подтверждены.

## Реализация
Room 21 → 22: nullable HarvestDay.moisturePercent, таблицы harvest_fuel_days и harvest_refuelings. Миграция добавляет столбец/таблицы; существующие записи не удаляет. Влажность не влияет на формулы массы/урожайности и ручную плотность. Топливо отдельно от таймера: уровни, несколько заправок, редактирование закрытых дней, постоянные UID дня и снимок техники. BEFORE_DAY не включается повторно в расход текущего дня. Резервное копирование учитывает новые поля и старые форматы. Основная подпись и TEST signer сохранены; только guards версии Room адаптированы к22.

## Проверки
- Повторно: python3 -m unittest discover -s docs -p 'test_*py' -q — 64 теста, 62 успешных, 2 пропуска SDK, ошибок нет.
- Чистые Kotlin-классы HarvestCalculator, HarvestDensityCatalog, HarvestResultsFormatter, HarvestFuelCalculator, HarvestChronology скомпилированы напрямую cached Kotlin 2.0.21; JUnitCore для HarvestCalculatorTest, HarvestFuelCalculatorTest, HarvestChronologyTest — 45 тестов успешно, включая 13 новых топливных. Это не Gradle/Android-сборка и не проверка Room.
- git diff --check — успешно.
- source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test --stacktrace — сбой до компиляции: Could not determine a usable wildcard IP for this machine. Причина в stacktrace: java.net.SocketException: Operation not permitted (Socket creation failed), NetworkInterface.getNetworkInterfaces, Gradle FileLockContentionHandler. Код приложения ради обхода ограничений не менялся.
- Git CLI ls-remote и curl GitHub: соединение с proxy:8080 недоступно. GitHub App чтение доступно; создание ветки зависает, в одном повторе получен tool call rejected because the catalog changed. Публикация пока не подтверждена.

## Ветка и передача
Удалённый исходный main: 68cef94fcf1db9939154d9ad7289ac424ca7089f. Рабочая копия /workspace/AgroNaliz-fuel сохраняется. Для переносимого patch восстановлен локальный baseline файлов main; этот служебный baseline не следует отправлять в GitHub. Переносить task patch поверх указанного main либо cherry-pick только task-коммита из bundle. SHA фактически проверенного результата будет указан следующим отчётным коммитом. Удалений нет.

## Изменённые и созданные файлы
- .github/workflows/android-pr-checks.yml
- .github/workflows/main-apk-publish.yml
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestAreaUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFuelTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFuelUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestMigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/MainUpdateTest.kt
- app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt
- app/src/main/java/ru/fo6osik/workjournal/DataImportManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseBackupManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseProvider.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestBackupCompatibility.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDao.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDay.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDayActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestFuel.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestFuelBackup.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestFuelCalculator.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestFuelDialog.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestProcessActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestResultsFormatter.kt
- app/src/main/res/drawable/ic_harvest_fuel.xml
- app/src/main/res/layout/activity_harvest_day.xml
- app/src/main/res/layout/dialog_finish_harvest_day.xml
- app/src/test/java/ru/fo6osik/workjournal/HarvestFuelCalculatorTest.kt
- docs/codex-handoff.md
- docs/export-installed-test-backup.py
- docs/harvest-moisture-fuel.md
- docs/run-fuel-checks.py
- docs/test_main_apk_release.py
- docs/test_verify_main_update.py
- docs/verify-main-apk-update.py

## Ограничения и следующий шаг
Android APK не собрана. Room 21 → 22 и сохранность записей проверяются добавленными HarvestFuelTest/HarvestFuelUiTest и docs/run-fuel-checks.py, но ещё НЕ запускались на эмуляторе. Нужен runner с работающими сокетами и Android SDK/AVD. После публикации запускать Android PR checks и Main APK safe update checks; основной release не запускать. Проверить закрытые дни, межсменную заправку, миграцию и импорт старых/новых backup на синтетических данных.

Draft PR пока не создан: GitHub mutation не завершена. CI не запускался для изменений. TEST artifact отсутствует. Стабильная подписанная TEST APK доступна через существующий main-only test-apk-publish.yml только после отдельного решения владельца о merge; текущая задача merge не разрешает. Не менять signer ради preview и не удалять установленное TEST-приложение при несовпадении сертификата. Ручные сценарии: docs/harvest-moisture-fuel.md. Данные реального телефона не затрагивались; основная APK не публиковалась.
