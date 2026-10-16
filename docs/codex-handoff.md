# Передача контекста: АгроНализ 1.0.8, уборка и полный JSON-бэкап

Дата: 16 октября 2026. Статус: код и тесты подготовлены, Android ещё не проверен.
Задача: единое завершение дня с топливом, быстрый доступ, три площади и Room23,
полное/объединяющее резервное копирование уборки.

Ветка: feature/harvest-1-0-8. Опорный main:
4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee, подтверждён GitHub-коннектором.
На момент записи исходники незакоммичены; SHA проверенного коммита пока нет.
Проверены локальные файлы, а не Android APK. Публикация ветки и SHA будут уточнены.

Полный технический отчёт, результаты и ручная инструкция:
[harvest-1.0.8-review.md](harvest-1.0.8-review.md).

Существенные решения: общий формат JSON сохранён; снимок площади только при новом
создании, исторические площади NULL; конечное топливо меняется отдельно от начального;
все завершения в одной транзакции; повторный импорт сохраняет местные строки по UID,
коллизия родителя отклоняется. Масса диапазоном, неполнота явно указана.
Room22→23 добавляет два nullable REAL без перерасчёта/очистки.

Проверки: 72 Python-теста (70 успешно, 2 пропущены без APK), 28 чистых Kotlin/JUnit
тестов успешно через локальный компилятор Gradle 8.13. Команды/ограничения — в отчёте.
Проектный agrogradle :app:assembleDebug :app:test остановился до компиляции:
Could not determine a usable wildcard IP for this machine. Дополнительный сетевой
запуск прерван пользователем; его нельзя считать успешным. Android-тесты не запускались.

Draft PR пока отсутствует: ожидается ответ о запрете установки MAIN даже в одноразовом
CI-эмуляторе. Создание PR автоматически запускает существующие проверки установки
MAIN; пользователь запретил её установку без уточнения среды. Вопрос отправлен,
ответ не получен. Не создавать PR до разрешения этого противоречия и не менять
защищённые workflows публикации. Отдельный feature push эти CI не запускает.
Не выпускать APK, не ставить на устройство, не merge, не восстанавливать рабочую базу.

Нужно продолжить: уточнить CI-ограничение; создать Draft PR после ответа, выполнить
Android CI, исправить реальные ошибки, записать номера запусков/точные результаты
и PR URL сюда. Если CI MAIN запрещён, согласовать конкретное изменение проверок;
не обходить пользовательский запрет. Ручная проверка владельцем остаётся невыполненной.

Изменённые/созданные файлы (пути относительно репозитория):
- .github/workflows/android-pr-checks.yml
- app/build.gradle.kts
- app/src/androidTest/java/ru/fo6osik/workjournal/AppVersionUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/Harvest108Test.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestAreaUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFuelTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFullBackup108Test.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestMigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestQuickAccessUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestRoom23MigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/MainUpdateTest.kt
- app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt
- app/src/main/java/ru/fo6osik/workjournal/DataImportManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseBackupManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseProvider.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestBackupCompatibility.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestBackupIntegrity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestCampaignMetrics.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDao.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDayActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDayCompletion.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestFuel.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestFuelBackup.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestProcess.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestProcessActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestQuickAccess.kt
- app/src/main/java/ru/fo6osik/workjournal/MainActivity.kt
- app/src/main/res/layout/activity_backup_restore.xml
- app/src/main/res/layout/activity_harvest_process.xml
- app/src/main/res/layout/activity_main.xml
- app/src/main/res/layout/dialog_finish_harvest_day.xml
- app/src/test/java/ru/fo6osik/workjournal/HarvestCampaignMetricsTest.kt
- docs/codex-handoff.md
- docs/export-installed-test-backup.py
- docs/harvest-1.0.8-review.md
- docs/run-harvest-108-checks.py
- docs/run-harvest-checks.py
- docs/run-harvest-jvm-checks.py
- docs/test_app_version.py
- docs/test_main_apk_release.py
- docs/test_test_backup_export.py
- docs/verify-build-variants.py
- docs/version-history.md

Удалённых файлов нет. Ключи, applicationId и защищённая публикация не изменены.
