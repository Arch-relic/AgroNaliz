# Этап завершён: стабильный TEST1.0.8 опубликован как Actions artifact

16 октября2026. Отдельная ветка test-artifact/1.0.8-pr33 от PR33 head
5c1c74b9299f13537369da6180747140063f7ab5; main4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee
и PR33 не изменены/не merged. Draft PR34 направлен в feature/harvest-1-0-8:
https://github.com/Arch-relic/AgroNaliz/pull/34 . PR33 тоже остаётся Draft.

Publish stable TEST APK #7 SUCCESS:
https://github.com/Arch-relic/AgroNaliz/actions/runs/38933732351
Скачать ZIP:
https://github.com/Arch-relic/AgroNaliz/actions/runs/38933732351/artifacts/12059653830
Имя app-TEST-stable-pr33-1.0.8-f881f41f61565867e67e084e195e101346f422d5-v2000007,
содержит app-TEST-stable.apk и verification.json, хранится до30 октября2026.

Проверенный исходный SHA f881f41f61565867e67e084e195e101346f422d5.
Локальный идентичный ca7119b, tree c444d77aafd33a0674635504fef3f75dc95edbc4.
Следующий коммит меняет только отчёты; APK остаётся из указанного проверенного SHA,
сам SHA документирующего коммита не называется проверенным. Незакоммиченных изменений
на момент завершения нет. Android-код тот же, что прошёл CI97 в PR33.

Использован существующий TEST workflow, TEST Secrets и inline signer, build
без секретов/sign без checkout и Gradle, прежний pinned сертификат. Для этой
явно разрешённой владельцем публикации добавлен только owner-only push по точной
ветке и изменению TEST workflow, run_attempt1. PR/fork/остальные refs не подписывают.
Главный ручной main dispatch сохранён; MAIN workflows не изменены. История версий
того же workflow и concurrency сохранены; отчётный push не публикует ещё одну APK.

Проверены фактические package ru.fo6osik.workjournal.debugtest, versionName1.0.8,
code2000007 (> последнего2000006), Room23 и SHA сертификата
3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C — совпадает с
последним успешным стабильным TEST38923846794. Signer verify и gate прошли до upload.
Пользователь подтвердил, что телефонные certificate/code/Room НЕИЗВЕСТНЫ.
Поэтому совместимость с телефоном НЕ ПОДТВЕРЖДЕНА; verification.json содержит
NOT_CONFIRMED. Не обещать обновление с сохранением данных по одному GitHub pin.

Изменённые/новые файлы этого этапа:
- .github/workflows/test-apk-publish.yml
- docs/test_test_apk_signing.py
- docs/test_test_preview_artifact.py (новый)
- docs/test-apk-1.0.8-artifact.md (новый)
- docs/test-apk.md
- docs/test-apk-signing.md
- docs/codex-handoff.md
Удалений нет. Production-код/MAIN подписи/пользовательские данные не менялись.

Результаты: :app:assembleDebugTest и :app:testDebugTestUnitTest SUCCESS,
JVM-тесты выполнены заново (не UP-TO-DATE). Python Actions78:77 успешно,1 пропущен
(SDK-тест MAIN APK, которая не строится этим TEST job). Локально78:76 успешно,
2 без APK пропущены; отдельная проверка JSON roundtrip синтетического report успешна.
Diff --check успешно. Sign job SUCCESS, временный ключ/транспорт удалены, финальный
artifact остался. Tool download подтвердил доступность ZIP; локальный повторный
SDK-анализ скачанного ZIP не делался. SHA APK находится в verification.json;
ZIP digest отдельно указан в полном отчёте, их не путать.

[Полный отчёт и инструкция получения сертификата/code/Room на смартфоне](test-apk-1.0.8-artifact.md).
В App Manager: сведения установленной TEST для code, подписи для полного SHA256.
Room точно из доступного JSON databaseVersion; если частный JSON недоступен на
смартфоне, оставить неизвестной, не обходить защиту. Приватный ключ не отправлять.
Не удалять/очищать TEST, не устанавливать APK автоматически. Для старой установки
с другим сертификатом нужен соответствующий исходный ключ, новый его не заменит.

Остановиться для review/метаданных телефона. Не merge, не MAIN Release, не установка
на пользовательские устройства, не восстановление рабочей базы. Условия выполнены:
APK опубликована как Actions artifact без изменения main и без пользовательских данных.

---

История предыдущего этапа:

# Передача контекста: PR #33, быстрый доступ исправлен, полный CI успешен

16 октября 2026. Ветка feature/harvest-1-0-8. Опорный main:
4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee.
Draft PR https://github.com/Arch-relic/AgroNaliz/pull/33 — OPEN, Draft, не merged.

Цель продолжения: установить причину двух HarvestQuickAccessUiTest, исправить её
минимально и довести полный CI, особенно новые тесты backup/миграции/обновления TEST.
Статус: исправлено, оба полных CI прошли с первой попытки; остановиться для review.

Проверенный удалённый коммит: 49d16a71776d07cd24ec7f73b857dd07be9ee420.
Локальный идентичный snapshot f346566, tree c19a0a796b64fbf2edd401a39c8a6b0e437e4e76.
Последующий коммит меняет только отчёты, исходники/тесты остаются проверенными.
Не пытаться записывать SHA самого этого документирующего коммита как проверенный.
Локальные и GitHub SHA различаются из-за публикации через API; tree совпадает.
Все результаты ниже относятся к 49d16a7, не к повторному CI документационного коммита.

Диагноз: в логах #94/#95 mCurrentFocus/mFocusedApp у GrantPermissionsActivity.
После uninstall/install TEST разрешение уведомлений терялось, а активный день
штатно запрашивал его. Runner теперь выдаёт разрешения как прежний harvest runner,
Before проверяет их наличие. Второй тест получал активную фикстуру первого после
ошибки — After теперь удаляет только собственные созданные ID и напоминания.
MainActivity, quickAccessGeneration и оба экрана не менялись: прежние условия
RESUMED/ID, навигации, выбора кампании и видимости прошли без ослаблений/роста тайм-аута.

Изменены в этом продолжении (удалений нет):
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestQuickAccessUiTest.kt
- docs/run-harvest-108-checks.py
- docs/harvest-1.0.8-review.md
- docs/codex-handoff.md
Полный список файлов исходной реализации 1.0.8 сохранён в историческом разделе ниже.

Фактические результаты для 49d16a7:
- Android PR checks #96 SUCCESS: https://github.com/Arch-relic/AgroNaliz/actions/runs/38910940062
  Обе APK и обе instrumentation APK, testDebugUnitTest и testDebugTestUnitTest
  (новое выполнение, не UP-TO-DATE), 13+6+27 Python и 81 инструментальное выполнение
  в42 invocation прошли. Новые: QuickAccess2/2, Harvest108 5/5, FullBackup108 4/4,
  Room23Migration seed22/verify23/verify23 после restart — все три прошли.
  Сохранены оба полных UI-прогона площади по9 и пять fuel UI. Safe TEST update:
  тот же тестовый ключ сохраняет данные; другой ключ отвергнут без потери данных;
  частный read-only JSON и синтетическое полное восстановление прошли.
- Main APK safe update checks #23 SUCCESS: https://github.com/Arch-relic/AgroNaliz/actions/runs/38910939812
  Сборка/JVM, 18+8 Python и три инструментальных обновления synthetic MAIN прошли.
- Итого CI: 72 Python успешно, 84 инструментальных выполнения успешно, failures0.
  Количество JVM стандартный Gradle-лог не печатает; XML/reports в diagnostics.
- Локально после исправления: python3 -m unittest discover -s docs -p 'test_*py' -q:
  72 теста, 70 успешно, 2 пропущены без APK. diff --check успешно.
- source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test опять
  не стартовал до компиляции: FileLockContentionHandler / usable wildcard IP.
  Это локальный сбой среды, не результат последующей успешной сборки GitHub Actions.

Полный диагноз с временными отметками/ссылками, границы CI, старые ошибки,
изменения миграции и ручная инструкция TEST: [harvest-1.0.8-review.md](harvest-1.0.8-review.md).

Непроверено: физическое пользовательское устройство, реальные пользовательские
копии и разные размеры/масштаб экранов. Миграции/backup/подписи проверялись только
на синтетических данных изолированного эмулятора. Production-код, applicationId,
ключи, подписи и workflows не менялись в этом продолжении; destructive migration нет.
Не merge, не выпускать релиз, не устанавливать на телефон, не изменять рабочую базу.
Пользователь разрешил MAIN только в одноразовом изолированном GitHub Actions эмуляторе.
Далее: независимый review владельцем, любые выпуск/merge/телефон требуют отдельного
указания. Автоматическую защиту выпуска MAIN Room23 не менять без отдельного аудита.

---

Предыдущий этап (исторический отчёт):

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
