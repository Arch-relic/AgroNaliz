# Передача результатов Codex — «АгроНализ»

## Дата, цель и статус

11 октября 2026. Отдельная безопасная TEST-сборка с applicationId
ru.fo6osik.workjournal.debugtest и названием «АгроНализ TEST».
Реализация готова к ревью. Штатная сборка обеих APK, JVM-тесты и проверка
изоляции на API 35 эмуляторе прошли; APK скачаны и проверены. Открыт Draft PR №22.
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
Проверенный Android-код/head PR: 502b66e943489fec17f243e703bd4fefce3f5755.
Фактически собранный merge SHA: 132b499663b5409faa14a052c5fba412f97bcbd1.
GitHub Actions run 38134641076: SUCCESS.
https://github.com/Arch-relic/AgroNaliz/actions/runs/38134641076
После него коммит 309f1243d5e635bc79a855b920bc9d18306b885a изменил только runner проверки
и его инструкцию: запрет физических устройств/существующих пакетов и отказ
от adb install -r. Эти два отказа проверены локально с поддельным adb до
любых мутаций. Android-код/APK-конфигурация не менялись.
Этот отчёт сохранён следующим коммитом документации. Указанные SHA обозначают
реально проверенные результаты, а не хеш самого содержащего отчёт коммита.
Изменения закоммичены локально и опубликованы в отдельной GitHub-ветке.

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

## Подтверждённые результаты GitHub Actions

1. ./gradlew :app:assembleDebug :app:assembleDebugTest :app:testDebugUnitTest :app:testDebugTestUnitTest :app:assembleDebugAndroidTest --no-daemon --stacktrace --console=plain:
   BUILD SUCCESSFUL in 3m 25s. Unit tasks выполнены новым запуском, не UP-TO-DATE.
   XML скачанного diagnostics: 67 tests для каждого варианта (5 suites),
   всего 134 JVM-теста, 0 failures/errors/skipped. Четыре новые проверки backup
   guard выполняются в обоих вариантах. Вспомогательные no-source/preBuild
   задачи могли быть UP-TO-DATE; результаты unit tasks не взяты из старого запуска.
2. ./gradlew -PtestBuildType=debugTest :app:assembleDebugTestAndroidTest --no-daemon --stacktrace --console=plain:
   BUILD SUCCESSFUL in 25s. AGP создаёт instrumented APK только для выбранного
   testBuildType; второй вызов использует уже построенный TEST (часть задач
   UP-TO-DATE), но test APK собрана успешно. Обычный testBuildType остаётся debug.
3. python3 -m unittest discover -s docs -p 'test_validate_document_catalog.py' -v:
   новый CI запуск, 13 тестов, OK.
4. python3 docs/verify-build-variants.py: успешно, IDs извлечены из настоящих APK,
   label TEST подтверждён. Merged manifest: sharedUserId отсутствует, providers
   exported=false. Main authorities: ru.fo6osik.workjournal.androidx-startup.
   TEST: ru.fo6osik.workjournal.debugtest.androidx-startup и
   ru.fo6osik.workjournal.debugtest.testbackups. Пересечений нет.
   FileProvider основной APK отсутствует; TEST files-path только test-backups.
   allowBackup=false и dataExtractionRules TEST подтверждены.
5. python3 docs/run-build-isolation-checks.py, API 35 x86_64/google_apis/KVM:
   совместная установка обеих APK и test APK успешна. 5 запусков инструментальных
   тестов, каждый OK (1 test):
   - main seed: синтетическая запись через настоящий VehicleDao/Room, prefs,
     files/cache/noBackupFilesDir/documents/app-scoped external-files;
   - TEST seed: другая запись в той же Room-схеме, отдельные пустые каталоги,
     разные UID и dataDir, собственный путь БД;
   - TEST provider: private URI read/write, отказ открытия файла вне разрешённого
     backup-каталога, экспорт реальной TEST базы содержит только TEST-сентинел,
     внешний SAF экспорт отвергнут, exported=false, чтение main sandbox запрещено;
   - main verify: основная запись Room, prefs и все файловые сентинелы сохранены;
   - TEST удалён; повторный main verify: все основные сентинелы снова сохранены.
   Итог: ISOLATION SUCCESS. Исходные данные владельца не использовались.
6. ZIP обеих APK и diagnostics скачаны через GitHub artifact API. ZIP CRC и
   внутренняя структура APK проверены zipfile.testzip; AndroidManifest.xml найден.
   aapt2 dump badging повторно проверил package/label/launchable activity.
   apksigner verify обеих APK: exit 0 (стандартные предупреждения META-INF вне
   JAR-защиты не меняли результат). Main label «АгроУчёт — КФХ Губановы» сохранён.
   Команда основной Release-сборки в CI не выполнялась; её build type и ID
   сохраняются исходной конфигурацией. Java 17, Gradle 8.13, Kotlin 2.2.20,
   AGP 8.12.0, compileSdk 36 и minSdk 23 сохранены.
7. Проверка защиты runner поддельным adb: физическое устройство и эмулятор с
   установленным main отвергнуты до install/uninstall/instrument.

APK публикуются только после успешных проверок изоляции.

## APK и получение

TEST artifact: app-TEST-feature-safe-test-apk-132b499663b5, ID 11700861805.
https://github.com/Arch-relic/AgroNaliz/actions/runs/38134641076/artifacts/11700861805
ZIP 26303055 байт; app-debugTest.apk 38097874 байт; срок хранения до 25.10.2026.
applicationId: ru.fo6osik.workjournal.debugtest, label: «АгроНализ TEST».
SHA-256 APK: f66784148fb284b35c6af762b1d75d72fc4b29fdf40ae08033ba024536fea8b1.
SHA-256 ZIP совпал с GitHub digest:
ce4fa5f75b997e3a21a9cd1b9d4896c3da937ea24499890e4cc1d5923a21d601.

Main artifact: app-debug-feature-safe-test-apk-132b499663b5, ID 11700971543.
applicationId: ru.fo6osik.workjournal. APK 38096490 байт.
SHA-256 APK: 64441249fcf5fd569eea75ca9aff865374fc2a7a8771f27dbf442f187c26648e.
ZIP SHA-256 совпал с GitHub digest:
f4c18eb95270c9c0476abc2442e0bc6c74cc82f0768229dce569f55b25110451.
Diagnostics ID 11700806915 содержит JUnit XML обоих вариантов.

## Исправленные ошибки при проверке

- Run 38132846058: отсутствовала задача assembleDebugTestAndroidTest.
  Исправлено выбором testBuildType через Gradle property и отдельным вызовом.
- Run 38132964978: старый test AndroidX manifest не задавал exported для
  InstrumentationActivityInvoker на target 35. Обновлены исключительно
  androidTestImplementation junit 1.1.2 → 1.2.1 и espresso 3.3.0 → 3.6.1.
  Production-зависимости не менялись.
- Run 38134250896: неверное сравнение canonical database path с алиасом dataDir.
  Канонизируются обе стороны. Ошибка была в тесте, не в путях приложения.
Все три исправления подтверждены успешным run 38134641076.

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

[PR №22](https://github.com/Arch-relic/AgroNaliz/pull/22): Draft, открыт,
base main, head feature/safe-test-apk. Не объединён; main не изменён.
Отчёт и результаты добавлены после создания PR в эту же ветку.
Следующее действие владельца: ревью и ручная установка TEST по docs/test-apk.md.
Слияние требует отдельного разрешения владельца.
