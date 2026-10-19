# АгроНализ1.0.9 — этап4: ползунки и Room24

Согласованное расширение Room23→24 после preflight, ветка feature/harvest-1-0-9-sliders.
База main861024e7852c16adbd733eca84e4ef11f53a32be, tree3f4c461290ce221ceef52c2b1d43c703b6abfe27,
с PR35/36/37. Локальный f059c36 имеет идентичное дерево. Чистое начало, публикация
GitHub API с parent актуального main (локальный git proxy8080 недоступен).
Draft PR: https://github.com/Arch-relic/AgroNaliz/pull/38 (не объединён).
Дата отчёта: 19.10.2026. Проверяемый код:27253e342a60dc09686653dbffa2041c91c2924f,
tree08b0e6c129ecf31f0db0c69111ebe50cf2aeede6 (локальный96f6e10 — то же дерево).
Следующий отчётный коммит изменяет только этот документ, не код/проверки.

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
- docs/export-installed-test-backup.py
- docs/test_test_backup_export.py
- docs/fixtures/room23-app-version.txt
- docs/run-harvest-108-checks.py
- docs/run-room24-update-checks.py
- docs/test_app_version.py
- docs/test_main_room23_policy_patch.py

Удалённых файлов нет. Calendar/results card/утверждённые расчёты не изменены.
Широкий diff обусловлен заменой пресетов и поддержкой миграции/backup/тестов,
не рефакторингом других функций.

## Проверки и следующий этап

Локально python3 -m unittest discover -s docs -p 'test_*.py':85/83pass/2SDKskip,
вложенные24 legacy tests не суммировать (23pass/1SDKskip). Diff whitespace clean.
agrogradle :app:assembleDebug :app:test не стартовал: FileLockContentionHandler /
Could not determine a usable wildcard IP for this machine. Конфигурация проекта
ради локальной среды не менялась. Android/JVM фактически выполняются в CI.

На предыдущем коде11ff454: JVM168/168 в MAIN и TEST, успешно прошли MAIN safe
update, MAIN22→24, новый MAIN/TEST23→24 audit и policy checks. Последний Android
прогон39218340966 прошёл99 инструментальных тестов (45 вызовов), включая5 новых
UI-тестов, повтор с увеличенным шрифтом и7 полных backup-тестов, затем остановился:
read-only export-installed-test-backup.py ещё разрешал только Room20..23.
Исправление27253e3 добавляет24 с обязательным integer step1/5 и прежним запретом
downgrade; новые2 Python-теста покрывают допустимые/недопустимые значения.
Скрипт остаётся только чтением по adb run-as, без записи на устройства.

Ранее исправлены две подтверждённые причины падений тестового окружения:
- audit39216905207: TEST backup destination file:// запрещён существующей
  BackupStoragePolicy. Исправлена только fixture: PrivateTestBackups destination.
  Защита приложения не менялась; последующие MAIN/TEST audit прошли.
- Android39216412413: Espresso click5мин при font1.6/720x960 не видел90%контрола.
  Тест прокручивает ScrollView к контролу, ограничения click/проверки сохранены.
  Последующий39218340966 прошёл этот сценарий и весь HarvestSlidersUiTest.

Полный повтор CI для27253e3 (PR merge2e97a2d3f6c34b60316f3822b7f0ff49ce1a74a0
имеет то же дерево08b0e6c...):
- Android PR checks #127: https://github.com/Arch-relic/AgroNaliz/actions/runs/39220430531 — success;104инструментальных/50вызовов,49Python.
- MAIN safe update #50: https://github.com/Arch-relic/AgroNaliz/actions/runs/39220430578 — success;3инструментальных,26Python.
- MAIN Room22→current #20: https://github.com/Arch-relic/AgroNaliz/actions/runs/39220430499 — success;3инструментальных.
- Room24 synthetic MAIN TEST audit #6: https://github.com/Arch-relic/AgroNaliz/actions/runs/39220430524 — success;6инструментальных.
- Detached policy checks #10: https://github.com/Arch-relic/AgroNaliz/actions/runs/39220430611 — success.

XML нового audit: testDebugUnitTest168/168 и testDebugTestUnitTest168/168,
0errors/0failures/0skips. Не считать повторные прогоны уникальными сценариями.
Артефакт12171338532 содержит verification.json и диагностику,0APK. Metadata:
sourceSha2e97a2d...; MAIN23/code3000004→24/code3000005, TEST23/code1000201→
24/code1000202; dataRetained/separateRestore/restart=true для обоих.
Коды только CI flags, не изменение версий исходников. Ephemeral certificate
338D8AFB5D610C3B60B1868BD804ADCDBA582D81A78D0146CA03696514C853EE,
productionReleaseAuthorized=false. Это не оригинальный MAIN certificate и не
подтверждение допуска production подписи. Исторические названия шагов MAIN22→23
сохранены, фактические assertions требуют currentRoom24.

Всего116 инструментальных выполнений:104+3+3+6, все успешны.
Android: HarvestSlidersUiTest5/5 плюс1 повтор при font1.6/720x960;
HarvestFullBackup108Test7/7; TEST SIGNING SUCCESS подтверждает совместимый
synthetic update, отказ при другом ключе без потери данных, read-only JSON export
и restore. Android49Python+MAIN26Python=75, policy job отдельно10; вложенные
sandbox tests не считать уникальными дополнительно. Android diagnostics12171813987,
без APK; installable artifacts/publish workflows не запускались.

Ручной смартфон не проверяется/не затрагивается; все fixture data синтетические.
Новый риск потери данных не обнаружен. MAIN production Room24 не разрешена:
потребуется отдельное согласование политики, которая здесь не изменена.
После CI требуется review, не merge/APK publication/настоящее MAIN signing.
