# Точный audit MAIN1.0.7 Room22 → MAIN1.0.8 Room23

17 октября2026. Draft PR33, feature/harvest-1-0-8. Миграционный барьер закрыт тестом;
production signing policy не менялась, подписанного выпуска нет. Остановиться для решения.

## Что было после проверенного SHA

До этой задачи head888a9a6d4fab86c45c3f580bd699da5ec5051f02 отличался от
8702bf6cfcd7a783236bd2bb325795b3cee154d7 только тремя отчётами:
docs/codex-handoff.md, docs/main-1.0.8-release-audit.md, docs/main-1.0.8-verification.json.
Продуктовый код, tests/workflows были одинаковыми. Main остаётся
4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee; PR33 остаётся Draft, не merged.

Проверенный новый код CI:9fe8fa2f922c8e62dc124307663993387a56b2cf,
локальный эквивалент6e09bcf, tree4aa516e17219b47ec98ab41c7a9709a01402ef75.
Разные SHA объясняются API-публикацией. Последующий отчётный коммит меняет только
документы/JSON, не код теста/runner/workflow; не выдавать его SHA за проверенный.

## Новый отдельный CI

[MAIN Room22 to23 migration audit #1 SUCCESS](https://github.com/Arch-relic/AgroNaliz/actions/runs/39035077568).
[Diagnostics и metadata JSON, без APK](https://github.com/Arch-relic/AgroNaliz/actions/runs/39035077568/artifacts/12101538033).
Три инструментальных вызова успешно прошли:
1. seedMain107Room22 — реальный исходный baseline4d9ecfe7, MAIN1.0.7,
   applicationId ru.fo6osik.workjournal, versionCode3000003, фактический Room22.
2. install -r текущей MAIN1.0.8/code3000004, фактический Room23; verify.
3. force-stop и повторный verify после нового запуска процесса.

APK — синтетические debug-сборки на одном runner, с общим одноразовым CI-сертификатом.
Проверка отвергает исходный production digest в CI fixture. Это не подпись настоящим
MAIN ключом; production Secrets отсутствуют. Идентификатор, versionName/code и
сертификат обоих input APK проверены aapt2/apksigner до эмулятора.
Runner требует ro.kernel.qemu=1 и отказывает при существующих MAIN/TEST packages;
сам тест требует MAIN package и ranchu/goldfish. Никаких uninstall/clear/-d flags.
Эмулятор новый, реальные устройства и пользовательская база не используются.

## Что сохранено и проверено

Seed содержит поле120га, связанную технику, активную и завершённую кампании,
активный и завершённый рабочие дни, гектары60, влажность14.5, плотность750/custom,
индивидуальный таймер47, бункеры FULL/HALF/MORE_THAN_HALF/LESS_THAN_HALF,
WAITING_TRANSPORT после бункеров, farm_event, start80/end55 и заправки DAY/BEFORE_DAY.

Экспорт JSON22 фиксирует все старые колонки всех таблиц, включая пустые таблицы.
После update и restart количество строк/таблиц и каждое старое значение сравниваются;
проверяются prefs, реальный reminder prefs, отдельный файл, общий сертификат и version.
Room валидирует схему23; fieldAreaSnapshotHa/actualPlantedAreaHa у старых кампаний NULL.
foreign_key_check пуст; QuickAccess сохраняет активный день и отвергает завершённую
кампанию. Диапазон массы12–18т и расход85 процентных пунктов с исключением BEFORE_DAY
подтверждены. Никаких новых изменений production-моделей/миграции/backup не потребовалось.

JSON23 полностью восстанавливается штатным DatabaseBackupManager в отдельную
in-memory Room-базу, затем повторно экспортируется; все её таблицы/колонки совпадают
с исходным JSON23, foreign_key_check пуст. Это выполняется до и после restart.
Исходная обновлённая база при restore не очищается — target передаётся отдельно.

## Остальные проверки

MAIN safety#32 SUCCESS: https://github.com/Arch-relic/AgroNaliz/actions/runs/39035077518
Старый сценарий20→23→23 сохранён,26 Python и3 инструментальных вызова успешны.
Новый job и MAIN safety заново выполняли testDebugUnitTest; не UP-TO-DATE,
число JVM-тестов стандартный Gradle-лог не печатает. New job собрал debug,
instrumentation и unsigned release, baseline debug; обе сборки успешны.

Android PR checks#105 SUCCESS: https://github.com/Arch-relic/AgroNaliz/actions/runs/39035077524
Обе APK/instrumentation APK и обе JVM-задачи заново;46 Python и84 инструментальных
выполнения в45 вызовах. Harvest1085/5, FullBackup1084/4, TEST22→23 seed/verify/restart,
QuickAccess/UI/Options и безопасное обновление TEST успешны.
Итого три CI:72 успешных Python и90 инструментальных выполнений, ошибок нет.
Старые проверки не сокращались, release policy tests по-прежнему блокируют Room23.

Локально python3 -m unittest discover -s docs -p 'test_*py' -q:72 всего,
70 успешно,2 без APK пропущены. py_compile обоих новых Python runner/inspector и
JSON/diff validation успешны. Проектная команда source /workspace/agronaliz-env.sh;
agrogradle :app:assembleDebug :app:test до компиляции остановилась на
Could not determine a usable wildcard IP for this machine. Локальный Android
успех не заявляется; реальные Android-сборки/инструментальные проверки — CI.

## Неподписанная APK для анализа

Существующий :app:assembleRelease (без изменения Gradle/signing) создал
app-release-unsigned.apk. Inspector проверил MAIN package, versionName1.0.8,
отсутствие подписи apksigner и вычислил SHA256. Метаданные лежат в
main-room23-analysis.json в diagnostics; сама APK не загружалась в артефакты.

Её versionCode1 — штатный default аналитического release build, не будущий
установочный versionCode и не кандидат обновления. Её нельзя предлагать для
установки. Реальные миграционные input — отдельные CI debug APK3000003/3000004.
Аналитический release отличается build type от нынешнего production-процесса,
который переподписывает debug APK оригинальным ключом. Проверка не выдаёт unsigned
analysis за готовую оригинально подписанную MAIN. По завершении disposable runner
его локальные APK исчезают, в diagnostics только hash/metadata/test reports.

## Сертификаты и policy

Исторический MAIN и стабильный TEST имеют один SHA256
3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C,
но разные applicationId. По явному пункту6 задания это зафиксировано и больше
не считается блокером. MAIN сохраняет прежний сертификат/ANDROID_* Secrets;
смена исключительно ради разделения не требуется, TEST_* signer не используется.
Свежее состояние production Secrets здесь не проверялось, они не доступны CI job.

main-apk-publish.yml не менялся: owner/main/attempt1, room==22 gate, версия/порядок,
pin и cleanup остаются. Python-тесты по-прежнему требуют отказа текущего Room23
production preflight. MAIN/TEST publication workflow не менялись.

[Точное предложение политики и рисков — НЕ ПРИМЕНЕНО](main-room23-release-policy-proposal.md).
Для будущего22→23 предлагается отдельная narrow branch проверки схемы, положительный
успешный audit точного source SHA из собственного owner/main dispatch и прежний
оригинальный signer. Не просто заменить22 на23 и не открывать Secrets PR-веткам.

## Оставшиеся условия и безопасный план

1. Владелец отдельно рассматривает и разрешает точное изменение production policy
   из предложения. Сейчас такого разрешения нет и policy не изменена.
2. Реализовать разрешённый guard + негативные tests, повторить CI. Не менять pin/id.
3. Только после отдельного решения о merge (сейчас запрещён) получить main SHA,
   выполнить новый readonly audit dispatch на нём. Не ссылаться на чужой/старый SHA.
4. В прежнем owner-only MAIN signer проверить history/floor>3000003 либо актуального
   установленного code, нужную пару22→23, matching audit и оригинальный pin.
5. После sign заново проверить package/code/version/Room/certificate/apk hash,
   сохранить verification.json. Публиковать только временный Actions artifact,
   без GitHub Release. Установку на телефон здесь не выполнять.

Production APK/её SHA256 не существуют в этом этапе. Реальное состояние базы и
установленной APK на телефоне не считывалось; перед будущей установкой владелец
сверяет публичные metadata и сохраняет доступный backup. OEM/устройство не проверены.
Новый тест закрывает конкретный Room22-сценарий, не обещает отсутствие любых дефектов
во всех возможных данных. Исходный ключ должен повторно проверяться защищённым signer.

Изменённые файлы от начала этой задачи, удалений нет:
- .github/workflows/main-room23-checks.yml (новый, readonly, без Secrets)
- app/src/androidTest/java/ru/fo6osik/workjournal/MainRoom23MigrationTest.kt (новый)
- docs/run-main-room23-checks.py (новый)
- docs/verify-main-room23-inputs.py (новый)
- docs/main-room23-release-policy-proposal.md (новый, не применяется)
- docs/main-room23-ci-review.md (этот новый отчёт)
- docs/main-1.0.8-release-audit.md (пометка нового результата над историей)
- docs/main-1.0.8-verification.json (новое evidence, productionAuthorized=false)
- docs/codex-handoff.md

Нет изменений src/main бизнес-кода, схемы/миграции, подписей/id и защищённых
publication/check workflows. PR33 остаётся Draft, без merge/релиза/реальных данных.
