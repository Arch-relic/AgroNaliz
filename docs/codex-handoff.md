# Передача контекста: АгроНализ 1.0.8, уборка и полный JSON-бэкап

Дата: 16 октября 2026. Статус: Draft PR #33 создан; сборка Android в CI успешна.
Основной CI упал на HarvestQuickAccessUiTest; реализация не полностью проверена.
Фактические результаты всех проверок приведены ниже.
Задача: единое завершение дня с топливом, быстрый доступ, три площади и Room23,
полное/объединяющее резервное копирование уборки.

Ветка: feature/harvest-1-0-8. Опорный main:
4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee, подтверждён GitHub-коннектором.
Исходники сохранены локальным коммитом c68bf0925f34b6d54e256a4a1886926c43cb33e7.
После коммита повторены 72 Python-проверки и 28 Kotlin/JUnit-тестов.
Опубликованный идентичный результат: b001325351b478b7d2b9e8e0a68126ee0f7144a5.
Git tree обоих коммитов совпадает: 4299fe27aee3cab76aef4b24e555415b2e575bef.
Разные SHA связаны с публикацией через GitHub API (прямой git fetch не смог
соединиться с сетевым proxy), а не с различием файлов. Эти проверки не являются
Android-сборкой. Следующий коммит уточняет только этот отчёт и статус публикации;
исходники/тесты остаются идентичны проверенному b001325.
Ветка: https://github.com/Arch-relic/AgroNaliz/tree/feature/harvest-1-0-8 .
На момент завершения локальных незакоммиченных изменений нет.

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
запуск прерван пользователем; его нельзя считать успешным. Это описание локального
этапа; последующие результаты GitHub Actions приведены ниже.

Draft PR #33: https://github.com/Arch-relic/AgroNaliz/pull/33 — OPEN, Draft, не объединён.
Пользователь явно разрешил установку синтетической MAIN APK только в одноразовом
изолированном CI-эмуляторе GitHub Actions с тестовыми данными и создание этого PR.
Ограничения на пользовательские устройства, рабочую базу, подпись и выпуск сохранены.
Проверки безопасности не отключаются. Защищённые workflows публикации не изменены.

CI запущен автоматически для 9dd5faa23aed5a761226c75dabe34cd658d4887e:
- Android PR checks #94: https://github.com/Arch-relic/AgroNaliz/actions/runs/38905755717 .
- Main APK safe update checks #21: https://github.com/Arch-relic/AgroNaliz/actions/runs/38905755688 .
Окончательные результаты для 9dd5faa:
- Main APK safe update checks #21: SUCCESS. Сборка и JVM-тесты, 18+8 Python,
  три инструментальные проверки обновления синтетической MAIN Room20→23 прошли.
- Android PR checks #94: FAILURE на HarvestQuickAccessUiTest (2 теста, 2 failures).
  Первый не дождался HarvestDayActivity (строки 70/81), второй упал на проверке
  пустого списка активных кампаний (строка 20), возможно вследствие первой ошибки.
  Первопричина перехода не установлена. Сборка обеих APK/тестов, обе JVM-задачи,
  13+6+27 Python и 60 инструментальных выполнений до этого прошли, включая два
  полных прогона HarvestAreaUiTest (по9), старые backup и топливо.
- Новые Harvest108Test, HarvestFullBackup108Test, целевая Room22→23 с топливом
  и run-test-signing-checks.py не запускались из-за остановки на быстром доступе.
  Нельзя объявлять новый полный бэкап/merge и обновление TEST полностью проверенными.
Подробные ошибки, границы и результаты — в harvest-1.0.8-review.md.
Этот следующий коммит содержит только отчёт/ссылку PR; код тот же, что проверял CI.
Синтетическая MAIN устанавливалась только в разрешённом одноразовом эмуляторе.
После создания PR код не исправлялся, проверки не отключались и не перезапускались
ради получения случайного успешного результата.

Работа остановлена для независимого review, как потребовал пользователь.
Следующему этапу нужна диагностика первого падения быстрого доступа и отдельное
продолжение с исправлениями/полным CI; не снимать проверки, не считать второй
каскадный сбой независимой подтверждённой ошибкой функциональности.
После отчёта о CI остановиться для независимого review. Не merge, не выпускать APK,
не устанавливать на пользовательские устройства и не восстанавливать рабочую базу.

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
