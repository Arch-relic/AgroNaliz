# АгроНализ1.0.9 — этап4: ползунки и Room24

Согласованное расширение Room23→24 после preflight, ветка feature/harvest-1-0-9-sliders.
База main861024e7852c16adbd733eca84e4ef11f53a32be, tree3f4c461290ce221ceef52c2b1d43c703b6abfe27,
с PR35/36/37. Локальный f059c36 имеет идентичное дерево. Чистое начало, публикация
GitHub API с parent актуального main (локальный git proxy8080 недоступен).
Изменения ещё не опубликованы/PR ещё не создан; CI требуется, не объявлен успешным.

## Реализация

Room24: reminderStepMinutes INTEGER NOT NULL DEFAULT1 в harvest_days, явная
MIGRATION_23_24 зарегистрирована после всех прежних. Никакого destructive fallback.
Новое поле добавлено последним параметром entity, старые колонки/конструкторы
сохранены. Для каждого дня собственный шаг, новый/старый мигрированный день=1.
DAO.saveSliderSettings сохраняет5интервалов и шаг одной транзакцией. Существующий
updateDayReminderSettings не изменён. Старые Custom флаги при неизменённых
значениях сохраняются; при новом значении отражают прежний смысл preset/custom,
UI от них не зависит. Исторические значения вне новых диапазонов сохраняются
без clamping при открытии/неподвижном сохранении.

5стандартных SeekBar, общий шаг1/5, сетка1,5,10...max. Переключение шага не
изменяет значение32; округляется только пользовательское движение. Текущие
значения/пример сумм обновляются сразу, Room только на Save, Cancel без записи.
Диалог ScrollView, labels multiline, крупные touch targets. Напоминания toggle,
ограничения завершённого дня и старый notification save-path сохранены. Сам
HarvestReminderManager/таймеры/прошлые события не изменены.
Плотность320..900/шаг5 независим от времени. Каталог MIN/AVERAGE/MAX берётся
точно; CUSTOM SeekBar. Старая дробная/вне диапазона плотность остаётся точной,
без прикосновения Save не нормализует её. Новая плотность только по явному
выбору, через старый DAO, бункеры не изменяются. Для Другого доступен CUSTOM.

Backup JSON formatVersion не менялся: новый столбец экспортируется общим SQL
экспортом. Full restore24 читает18..23 и24; merge сохраняет прежние16,18..22 и
добавляет23/24. Старые копии получают step1, повторный импорт не перезаписывает
пользовательский шаг. Валидация шага1/5 до изменения/очистки базы; неверные
структуры отклоняются, transaction rollback сохраняется. Room24-копии нельзя
восстанавливать старой Room23 — это штатный запрет downgrade, не обещать обратное.

## Защита MAIN

main-apk-publish.yml байт-в-байт неизменён, SHA256
27a24a645fd97a6b7ec6d99c8a18e466196823f0023d57eba444c368599c7d91.
Новый Python negative test требует отказа publisher на реальной Room24.
Исторический sandbox одобренного Room23 патча использует frozen version-test
fixture и schema23 только в TemporaryDirectory: прежние manifest hashes/guards
не меняются, production допуск не расширен. Live version tests требуют24.

Отдельный readonly room24-update-checks.yml: pinned MAIN/TEST Room23 APK из861024e,
current synthetic Room24 APK, original applicationId, строго возрастающие
CI-only versionCode flags и общий disposable debug certificate (production pin
явно запрещён). Fresh emulator guards, сравнение всех старых столбцов/таблиц,
foreign keys/active states/fuel/events/files/preferences/certificate, отдельный
restore и restart. Ни Secrets, ни APK upload, ни release. Исходные versionName/
versionCode/build/signatures не менялись. Старые CI обновляют22/21/20→current24;
исторические имена тестов/шагов сохранены, проверка текущей схемы стала24.
Production MAIN остаётся заблокирована до отдельного решения по политике.

## Файлы

- .github/workflows/room24-update-checks.yml
- app/src/androidTest/java/ru/fo6osik/workjournal/AppVersionUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestAreaUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFuelTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFullBackup108Test.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestMigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestRoom23MigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestSlidersUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/MainRoom23MigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/MainUpdateTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/Room24UpdateTest.kt
- app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt
- app/src/main/java/ru/fo6osik/workjournal/DataImportManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseBackupManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseProvider.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestBackupCompatibility.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDao.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDay.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDayActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestSliderRules.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestValueSliderView.kt
- app/src/main/res/layout/dialog_harvest_day_settings.xml
- app/src/test/java/ru/fo6osik/workjournal/HarvestCampaignMetricsTest.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestSliderRulesTest.kt
- docs/codex-handoff.md
- docs/fixtures/room23-app-version.txt
- docs/run-harvest-108-checks.py
- docs/run-room24-update-checks.py
- docs/test_app_version.py
- docs/test_main_room23_policy_patch.py

Удалённых файлов нет. Calendar/results card/утверждённые расчёты не изменены.
Широкий diff обусловлен заменой пресетов и поддержкой миграции/backup/тестов,
не рефакторингом других функций.

## Проверки и следующий этап

Локально python3 -m unittest discover -s docs -p 'test_*.py':83/81pass/2SDKskip,
вложенные24 legacy tests не суммировать (23pass/1SDKskip). Diff whitespace clean.
agrogradle :app:assembleDebug :app:test не стартовал: FileLockContentionHandler /
Could not determine a usable wildcard IP for this machine. Конфигурация проекта
ради локальной среды не менялась. Новые JVM/UI/Room23→24/старые backup cases
должны фактически пройти CI; результаты/точный проверенный SHA будут записаны
следующим отчётным коммитом. Старые проверки сохранены, ожидаемая версия23
обновлена только в current verification, не в baseline seeds.
Ручной смартфон не проверяется/не затрагивается; все fixture data синтетические.
После полного CI требуется review, не merge/APK publication/настоящий MAIN sign.
