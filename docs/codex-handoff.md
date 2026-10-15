# Уборка: влажность, компактные результаты и топливо

Дата: 2026-10-15.

## PR #30: исправление выбора окна Espresso (2026-10-15)
Исследован Android PR checks https://github.com/Arch-relic/AgroNaliz/actions/runs/38700776280, job116308148384. APK/JVM/catalog/provider checks прошли. В 07:53:51 UTC есть ISOLATION SUCCESS: раздельные UID/Room/preferences/files/cache/document store/external/FileProvider и неизменность main подтверждены. В 07:55:12 UTC упал HarvestAreaUiTest.zeroAreaKeepsFinishDialogOpenAndDayActive, строка114, при вводе нуля. RootViewWithoutFocusException показывает выбранный BASE_APPLICATION (Activity) с has-window-focus=false вместо модального диалога. Это дефект синхронизации/выбора окна UI-теста, не упавшее утверждение изоляции. Диагностика не доказывает отсутствие любых возможных дефектов приложения, но конкретный сбой не является нарушением разделения данных. В отличие от прежнего повтора без исправления, теперь устранён ошибочный root matcher.

Изменены только:
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestAreaUiTest.kt — dialogView выбирает isDialog + decor с нужным контролом; ввод/кнопки/валидация и закрытие клавиатуры выполняются в нужном окне; после TimePicker проверяется18:00. Все7 @Test и проверки БД/ошибки/отмены/площади/статусов сохранены.
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFuelUiTest.kt — ввод/очистка влажности и закрытие клавиатуры привязаны к диалогу; оба @Test сохранены.
- docs/run-harvest-checks.py — HarvestAreaUiTest7 запускается дважды; ОБА прохода обязательны, первая ошибка прерывает job, исключения не подавляются. Всего33 harvest instrumentation executions вместо26, полный Android набор ожидает57 вместо50. Обновлена подпись реальной схемы20→22.
- docs/codex-handoff.md — этот отчёт. Удалений нет. Android production-код, Room, APK IDs, подписи и workflows не менялись.

Ветка remote feature/harvest-moisture-fuel, опорный main68cef94fcf1db9939154d9ad7289ac424ca7089f, исходный headb34c6f47b0284d5ac75d197b4d153897dea58f46. Текущая правка ещё не проверена CI; SHA/новые результаты будут зафиксированы после запуска. git diff --check и Python syntax проверены. source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test снова остановился до компиляции из-за запрета сокетов/usable wildcard IP; локальный успех Android не заявляется. Полные проверки выполняет GitHub runner. PR30 остаётся Draft, merge/основной release запрещены. Ни одна проверка изоляции/подписи/данных не отключена, таймауты не увеличены.

Результаты предыдущих этапов ниже являются историческими, не результатом текущей правки.


## Итог повторной проверки последнего кода и guard
Проверенный head 87a491f723a2b98185e9f65937d2a5aadfef60c1:
- Android PR checks https://github.com/Arch-relic/AgroNaliz/actions/runs/38697088623 — SUCCESS после одного повтора только упавшего задания. В первой попытке Espresso RootViewWithoutFocusException в HarvestAreaUiTest.finishWithoutAreaKeepsUnknownAndCampaignQuestion; тесты/код ради повтора не изменялись, проверки не ослаблялись. На повторе все50 instrumentation test executions пройдены, включая fuel10 и Room21→22. Возможная нестабильность фокуса автоматизированного UI остаётся известным ограничением, не скрывать первый сбой.
- Main APK safe update checks https://github.com/Arch-relic/AgroNaliz/actions/runs/38697088525 — SUCCESS без повтора.
- Upload debug APK — SKIPPED по guard для PR; актуальный run публикует только диагностику, не основную APK. TEST artifact не добавлялся.
Следующий коммит обновляет ТОЛЬКО этот Markdown-отчёт, без изменения Android-кода, тестов или workflows; по AGENTS.md отдельная Android-сборка для него не обязательна. Автоматические новые CI runs могут стартовать, но приведённые результаты относятся к указанному проверенному head, а не к выдуманному SHA самого отчёта.

Исторические подробности предыдущего полного прогона сохранены ниже.

Статус: реализация опубликована в Draft PR #30; обе проверки кода 409427e13539fc435fd34bd179ad4cee5496258f успешно завершены. Merge и защищённый выпуск основной APK не выполнялись.

## Реализация и архитектура
- HarvestDay.moisturePercent: nullable, 0..100%, отдельное значение каждого дня. Ввод при завершении, последующее изменение/очистка, отображение в краткой статистике. Формулы массы/урожайности и ручная плотность сохранены.
- Компактные количество/объём/масса/площадь/урожайность; неизвестное — «—», известный ноль сохраняется, неполные кампании обозначаются.
- HarvestFuelDay/HarvestRefueling: независимый журнал, постоянные dayUid/refuelUid и FK дня/техники со снимком названия. Уровни/несколько заправок/редактирование/подтверждённое удаление, включая завершённые дни. Не события таймера.
- Расход = начало + сумма приростов DAY − конец; расход/га только при положительной площади. BEFORE_DAY закреплён перед следующим днём и не считается второй раз; показания/заправки автоматически не создаются. При неизвестных данных нет выдуманного числа; отрицательный расход объясняется предупреждением. Это приблизительные проценты бака, не литры.
- Room 21 → 22: ALTER ADD nullable moisturePercent и две новые таблицы/индексы/FK, без пересоздания старых таблиц и destructive migration. Полная копия22 и merge import учитывают новые данные; старые18–21 принимаются с документированными отсутствующими полями. Полное восстановление имеет прежнюю явно выбранную пользователем семантику замены базы, не автоматическую очистку при обновлении.
- Исправлены найденные CI ошибки reserved SQL alias current (заменён на anchor_day) и неоднозначного closeSoftKeyboard. Тесты усиливают сохранение влажности при завершении и фактическое merge-восстановление удалённых ТОЛЬКО синтетических fuel fixture rows.

## Git и проверенные SHA
- Опорный remote main: 68cef94fcf1db9939154d9ad7289ac424ca7089f.
- Remote ветка: feature/harvest-moisture-fuel; source/head проверенного кода: 409427e13539fc435fd34bd179ad4cee5496258f. GitHub PR runner проверял refs/pull/30/merge 13e7a52bb51cac93c52a3f9d8688db8581f15db0; main является предком head, дерево кода совпадает.
- После этих проверок следующим коммитом сохраняются этот отчёт, документация и guard публикации основной debug APK из PR. Android-код/тесты/схема после проверенного SHA не менялись. Хеш отчётного коммита здесь не выдумывается; смотреть head PR. Новые автоматические runs этого коммита следует отличать от подтверждённых ниже.
- Локальная ветка feature/harvest-moisture-fuel-local в /workspace/AgroNaliz-fuel имеет те же файлы, но отдельную историю зеркала. Служебный локальный baseline de56c511c8db9628fd11ce6be6ad98c4b833ffe2 реконструирует дерево main; НЕ отправлять эту историю в main/remote. Remote коммиты созданы GitHub API строго поверх настоящего main. Незакоммиченные изменения после сохранения отчёта не планируются.

## Проверки и результаты
1. GitHub Android PR checks: https://github.com/Arch-relic/AgroNaliz/actions/runs/38695502165 — SUCCESS.
   - assembleDebug, assembleDebugTest, обе instrumentation APK, testDebugUnitTest и testDebugTestUnitTest — SUCCESS, новые запуски, не только UP-TO-DATE. В исходниках 112 JVM @Test на вариант; отдельный XML-счётчик здесь не извлечён.
   - catalog13 и TEST signer/backup helper25 — 38 Python tests успешно без пропусков. Main helper18 + readonly8 ниже дают64 Python tests между двумя workflows.
   - 50 успешных instrumentation test executions (32 вызова instrument, включая повторные проверки main): изоляция UID/Room/preferences/files/cache/external/FileProvider, старые harvest26, новые fuel/moisture10, backup old/new merge/restore, перезапуск, одинаковая TEST подпись принимает обновление, другая отвергается без потери synthetic data. Включены реальные обновления APK со схем20 и21 до22.
   - FUEL SUCCESS в логе подтверждает Room21→22, сохранение площади/плотности/бункеров, дневные/межсменные заправки, редактирование закрытого дня, backup22 и legacy21, restart.
2. GitHub Main APK safe update checks: https://github.com/Arch-relic/AgroNaliz/actions/runs/38695502155 — SUCCESS.
   - Сборки/JVM, exact signer18 + readonly8 Python tests, три instrumentation execution: установленная synthetic version53 Room20 → повышенная версия Room22 → следующая Room22, без uninstall/clear/downgrade, сохранены все прежние значения таблиц/настройки/таймеры/файлы и fixture certificate. Рабочие Secrets не использовались.
   - В legacy именах сценариев/строках SUCCESS осталось «v21», однако HarvestMigrationTest и MainUpdateTest реально assertEquals(22, db.version). Не принимать старую подпись лога за фактическую версию схемы.
3. Локально java org.jetbrains.kotlin.cli.jvm.K2JVMCompiler с cached Kotlin2.0.21: HarvestCalculator/HarvestDensityCatalog/HarvestResultsFormatter/HarvestFuelCalculator/HarvestChronology; org.junit.runner.JUnitCore для HarvestCalculatorTest/HarvestFuelCalculatorTest/HarvestChronologyTest — OK45, включая13 новых fuel tests. Это дополнительная проверка pure Kotlin, не замена Android CI.
4. python3 -m unittest discover -s docs -p 'test_*py' -q —64, 62 успешно/2 пропуска (собранные APK отсутствуют локально); source /workspace/agronaliz-env.sh не устраняет отсутствие APK.
5. SQL smoke: восемь фактических SQL-команд MIGRATION_21_22 выполнены SQLite на синтетической старой схеме; старые строки не изменены, moisture NULL, новые fuel rows/FK корректны. Полную Room-валидацию подтверждает CI выше.
6. XML/YAML/py_compile и git diff --check — успешно. Для последующего guard отдельно проверены YAML и условие Upload debug APK: github.event_name != 'pull_request'. Сборки/тесты не отключены.

## Среда, публикация и ограничения
Требуемая локальная команда source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test --stacktrace остановилась ДО компиляции: Could not determine a usable wildcard IP for this machine. Корень: java.net.SocketException: Operation not permitted (Socket creation failed) в NetworkInterface.getNetworkInterfaces → Gradle FileLockContentionHandler. Обход ограничений платформы не применялся, исходники ради этого не менялись; сборку выполнил GitHub runner.
Git CLI/curl не соединяются с proxy:8080. GitHub App чтение/запись в итоге работают через короткие асинхронные вызовы; создана ветка, три коммита кода и Draft PR. Ранее зависавшие вызовы и catalog changed не считаются успешной публикацией сами по себе.
До добавления guard прежний Android workflow автоматически сохранил основную debug APK, artifact app-debug-feature-harvest-moisture-fuel-13e7a52bb51c. Это НЕ TEST, не APK с рабочей оригинальной подписью, и не предлагается для установки на телефон. Guard отключает такое сохранение из дальнейших PR runs, оставляя обычные non-PR сборки. Защищённый основной release никогда не запускался. Диагностика: artifact android-pr6-diagnostics, main-update-check-diagnostics.
Ни главный packageId, ни ключи/подпись основной версии, ни стабильный TEST signer не менялись. В main-apk-publish/readonly guards изменена только совместимость схемы до22; TEST versionCode/signing workflow не изменены.
Реальный телефон и его данные не проверялись. Миграционные/удаляемые fixtures только одноразовый QEMU с guards от существующих установок. Сохранность произвольной повреждённой пользовательской базы этими тестами не гарантируется.

## TEST APK и следующий этап
Стабильной подписанной TEST APK из feature-ветки нет: существующий защищённый Publish stable TEST APK допускает только owner/main. Не ослаблять этот guard и не подменять стабильный ключ временным.
После отдельного решения владельца о merge: Actions → Publish stable TEST APK → Run workflow → main, указать реальные установленные TEST versionCode/Room; цель22. Скачать app-TEST-stable-main-* → app-TEST-stable.apk, проверить package ru.fo6osik.workjournal.debugtest и совпадение сертификата с установленной TEST, повышенный versionCode. До этого шага текущая задача даёт Draft PR/успешные проверки, не установочный TEST artifact. Не удалять старую TEST при несовпадении ключа.
Ручные сценарии описаны в docs/harvest-moisture-fuel.md. Основную APK не выпускать и PR не объединять без отдельного разрешения. Архив/patch/bundle сохраняются в /workspace как запасная передача; предпочтителен task patch поверх указанного remote main, не push служебной локальной истории.

## Файлы
Удалений нет. Полные пути созданных/изменённых файлов:
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

## Pull Request
https://github.com/Arch-relic/AgroNaliz/pull/30 — #30, OPEN, DRAFT, НЕ MERGED. Отчёт читать из feature-ветки до разрешённого владельцем merge.
