# Передача результатов Codex — «АгроНализ»

## Дата, цель и статус

11 октября 2026. Отдельная безопасная TEST-сборка с applicationId
ru.fo6osik.workjournal.debugtest и названием «АгроНализ TEST».
Реализация подготовлена; штатная CI-сборка и эмуляторная проверка ожидаются.
Основной applicationId ru.fo6osik.workjournal сохранён.

## Решения и изоляция

Добавлен build type debugTest, наследующий debug с отдельным applicationIdSuffix,
без изменения namespace и обычных debug/release. Room 20, миграции и бизнес-логика
сохранены. Android разделяет UID, базу, prefs, files/cache/noBackup и app-scoped
external-files. Проверены все записи через File/contentResolver, манифест,
reminder prefs и LocalDocumentStore. Общих фиксированных внешних путей нет.
В main отсутствовал FileProvider; TEST получает приватный provider с authority
${applicationId}.testbackups и доступом только к filesDir/test-backups.
TEST экспорт не выбирает общий SAF-файл и дополнительно отвергает чужие URI в
DatabaseBackupManager до открытия БД. Основной SAF-экспорт сохранён.
Автоматическое резервирование и device transfer TEST исключены.
Импорт/restore читает выбранный файл и пишет только в текущую базу; автоматического
переноса данных основной версии нет. TEST-копии приватные, получение через ADB
описано в docs/test-apk.md. Варианты могут быть установлены одновременно.

## Файлы

Изменены:
- app/build.gradle.kts
- app/src/main/java/ru/fo6osik/workjournal/BackupRestoreActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseBackupManager.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/ExampleInstrumentedTest.kt
- .github/workflows/android-pr-checks.yml
- docs/codex-handoff.md

Созданы:
- app/src/debug/AndroidManifest.xml
- app/src/debugTest/AndroidManifest.xml
- app/src/debugTest/res/values/strings.xml
- app/src/debugTest/res/xml/test_backup_paths.xml
- app/src/debugTest/res/xml/test_data_extraction_rules.xml
- app/src/main/java/ru/fo6osik/workjournal/BackupStoragePolicy.kt
- app/src/main/java/ru/fo6osik/workjournal/PrivateTestBackups.kt
- app/src/test/java/ru/fo6osik/workjournal/BackupStoragePolicyTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/BuildVariantIsolationTest.kt
- docs/run-build-isolation-checks.py
- docs/verify-build-variants.py
- docs/test-apk.md

Удалений нет. PDF не возвращались, каталоги/индексы и облачные интеграции не
изменялись. Секреты/APK/ключи не добавлены в Git. Debug queries между двумя
пакетами нужны только для проверки совместной установки; release не затрагиваются.

## Ветка и SHA

Ветка feature/safe-test-apk создана GitHub API от актуального main
e97063cece9b66062ad1cfd4190f7e1cedb3fbf0. main не изменяется.
Предыдущая ветка сохранена; отдельный локальный worktree восстановлен из
сохранённого snapshot и точных файлов актуального main через API.
GitHub-ветка имеет настоящий parent main, а не локальную историю зеркала.
SHA проверенного результата будет записан после CI, не как хеш этого отчёта.

## Проверки

- git diff --check: успешно.
- YAML через PyYAML BaseLoader: успешно.
- python3 docs/run-document-jvm-tests.py: новый fallback запуск, 63 JUnit-теста,
  0 ошибок, 0 failures; Kotlin 2.0.21/JVM 1.8. Новая backup-политика этим скриптом
  не проверяется; штатные unit tests обоих вариантов ожидаются.
- python3 -m unittest discover -s docs -p 'test_validate_document_catalog.py' -v:
  новый запуск, 13 тестов, все прошли.
- source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:assembleDebugTest :app:test:
  FAILED до компиляции: Could not determine a usable wildcard IP for this machine.
  Локальная штатная сборка не объявляется успешной.

CI сохранит существующие проверки, соберёт Debug/TEST и instrumented APK,
запустит JVM-тесты обоих вариантов, проверит фактические APK ID и manifest,
установит оба варианта на одноразовый API 35 эмулятор. Синтетические записи в
Room/prefs/files/cache/documents/external-files проверяются до/после TEST и его
удаления. Проверяются UID, отказ чтения main, ограничение provider и чужого URI.
APK публикуются только после успешных проверок изоляции.

## Риски и ручные проверки

Реальная установка на телефон/OEM не проверена. Debug-подпись между CI runners
может отличаться; конфликт обновления TEST не требует удаления основной версии.
TEST backup хранится приватно; его удаление удалит только TEST-копии.
Автоматическое main-поведение обновится лишь после слияния владельцем.
PDF-байты и совместимость содержания PDF/индексов не проверяются в этом этапе.
Перед установкой использовать только artifact app-TEST-..., не app-debug-....
Не выбирать для импорта реальные рабочие данные; использовать синтетические.
См. docs/test-apk.md для проверки двух иконок/настроек/записей/удаления TEST.

## Pull Request

Отдельный Draft PR в main будет создан после публикации реализации.
Номер, URL и подтверждённые результаты будут записаны следующим коммитом.
Автоматическое слияние запрещено; main не изменяется.
