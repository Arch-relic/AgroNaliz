# Этап: отделённый патч MAIN Room23 для согласования, НЕ ПРИМЕНЁН

17 октября2026. feature/harvest-1-0-8, Draft PR33, без merge/main push/релиза.
До задачи head dfc3baa6a0569fad6fcbc11fe04ca3ce2ad90a11 отличается от проверенного
9fe8fa2f922c8e62dc124307663993387a56b2cf только5 отчётами (полный список в новом report).
main4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee. Production-код/миграции/signers неизменны.

[Патч .patch](patches/main-room23-release-policy.patch), [manifest](patches/main-room23-release-policy.json),
[полный отчёт, условия и порядок backup/merge/audit/sign/update](main-room23-policy-patch-review.md).
Четыре будущих target: .github/workflows/main-apk-publish.yml,
docs/test_main_apk_release.py, docs/test_app_version.py, docs/main-original-update.md.
Их live SHA сейчас beforeSha256; git apply выполняется только в TemporaryDirectory.
В рабочей инфраструктуре patch не применяется. Room23 production остаётся закрыт.

Добавляются только предложение/документы/tests и отдельный readonly CI, без Secrets/APK.
10 методов exact-patch tests проверяют narrow22→23, audit metadata/job/steps/sourceSHA,
owner/main/dispatch/attempt, API failures, wrong version/floor/sequence/cert и TEST id,
post-sign package mismatch. Preserved: main owner1st dispatch, ANDROID_* pin/identity,
секрет-изоляция, code3000000+run_number, cleanup/transport, legacyRoom22 branch.
Во временной копии проходят прежние MAIN18 tests (17pass/1skip) иversion6.
Локально общий unittest discover:82/80pass/2skip; 10 новых включены в82.
Вложенный24tests не прибавлять к82. Реальные APK/ключи/SDK/сеть для patch tests
не используются; подпись не запускалась. git apply --check/manifest/diff checks успешны.

[Readonly detached-policy CI SUCCESS](https://github.com/Arch-relic/AgroNaliz/actions/runs/39037182692):
Python3.12,10/10 outer patch tests + старые MAIN/version suites (1SDKskip).
Проверенный patch/test SHA56e65f80a83e3caba1badf9988e31f94964c0b3f,
локальныйaf0afc2, tree2a822777fc276ffac077a4f63a90bc2decd82325.
API объясняет разные SHA. Следующий коммит меняет только два отчёта, не patch/tests;
не называть его SHA проверенным Android/patch-кодом. YAML предложенного workflow
дополнительно разобран PyYAML BaseLoader: manual-only и audit output propagation верны.
Текущая Android source остаётся проверенным9fe8fa2 (90instrumental).
Отдельный запуск Android для неизменённого product кода не нужен; известное локальное
wildcard IP ограничение Gradle сохраняется. Непроверено: реальный production signer
после разрешения patch, доступность Secrets сейчас, реальная база/устройство/OEM.

Шесть изменённых/новых файлов, удалений нет:
- docs/patches/main-room23-release-policy.patch
- docs/patches/main-room23-release-policy.json
- docs/test_main_room23_policy_patch.py
- .github/workflows/main-policy-proposal-checks.yml
- docs/main-room23-policy-patch-review.md
- docs/codex-handoff.md

Для будущего выпуска нужно ОТДЕЛЬНОЕ разрешение применить патч и отдельное merge.
После merge новый main SHA M, owner/manual audit M, затем main publisher с floor
реальной MAIN (baseline3000003), installedRoom22 и auditRunID. Merge нынешнегоPR33
с не применённым patch-файлом сам по себе Room23 не разрешит. MAIN/TEST certificate
исторически одинаковый, package различает их; смена cert не требуется. TEST всегда
отвергается точным MAIN id, даже с тем же сертификатом. Сначала штатный MAIN JSON22
backup вне песочницы, без restore рабочей базы; это не полный backup preferences/files.
Обновление на смартфоне только владельцем после отдельного решения, без uninstall/clear.
Никаких публикаций, установок или действий с реальными данными в этом этапе.
После отчёта остановиться и ждать разрешения.

---

История предыдущего этапа:

# Этап: точный MAIN22→23 CI успешно выполнен, политика не применяется

17 октября2026, feature/harvest-1-0-8, [Draft PR33](https://github.com/Arch-relic/AgroNaliz/pull/33).
Работа завершена. Остановиться для отдельного решения о release policy, не merge/релиз.
HEAD до задачи888a9a6d4fab86c45c3f580bd699da5ec5051f02 отличался от проверенного
8702bf6cfcd7a783236bd2bb325795b3cee154d7 только тремя отчётами:
docs/codex-handoff.md, docs/main-1.0.8-release-audit.md, docs/main-1.0.8-verification.json.
Продуктовый код/tests/workflows совпадали. main4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee
не изменялся. Проверенный новый SHA9fe8fa2f922c8e62dc124307663993387a56b2cf,
локальный6e09bcf, tree4aa516e17219b47ec98ab41c7a9709a01402ef75. API объясняет разницу.
Последующий коммит только документы/JSON, не выдавать его SHA за проверенный Android-код.

[Полный новый отчёт](main-room23-ci-review.md).
[Предложение release policy — НЕ ПРИМЕНЕНО](main-room23-release-policy-proposal.md).
[Машиночитаемый статус](main-1.0.8-verification.json): migrationVerified=true,
productionReleaseAuthorized=false, signedApkPrepared=false.

Новый [MAIN Room22 to23 audit#1 SUCCESS](https://github.com/Arch-relic/AgroNaliz/actions/runs/39035077568):
3 инструментальных вызова. Pinned baseline4d9ecfe7 MAIN1.0.7/code3000003/Room22 →
MAIN1.0.8/code3000004/Room23 через install-r, verify, force-stop/restart/verify.
Пакет MAIN, общий одноразовый CI debug certificate (не production), readonly workflow
без Secrets; runner отказывает существующим MAIN/TEST и требует disposable emulator.

Сравниваются все старые колонки всех таблиц/количество строк, поля/техника,
активная/завершённая уборка и дни, бункеры/события, гектары/влажность/плотность,
start/end fuel и DAY/BEFORE_DAY refill, prefs/реальные reminder prefs/file/certificate,
NULL обеих новых площадей, foreign_key_check, масса12–18т и расход85.
Export23 восстановлен в отдельную in-memory Room-базу, экспорт target совпал со всем
source JSON; повторено после restart. Исходную базу при restore не очищают.

Также успешно: [MAIN safety#32](https://github.com/Arch-relic/AgroNaliz/actions/runs/39035077518)
(26Python/3instrumental, старый20→23→23),
[Android#105](https://github.com/Arch-relic/AgroNaliz/actions/runs/39035077524)
(46Python/84instrumental/45вызовов, все прежние уборка/backup/TEST migration/UI/update).
Итого72 Python и90 инструментальных выполнений, ошибок нет. JVM-задачи выполнялись
заново, не UP-TO-DATE, стандартный лог не печатает число JVM-тестов.

Существующий unsigned :app:assembleRelease успешен без изменения Gradle/signing.
aapt2/apksigner подтвердили MAIN1.0.8, code1 (аналитический default), отсутствие
подписи; SHA256 в main-room23-analysis.json. APK не публиковались/не распространялись.
[Diagnostics artifact, только metadata/test reports](https://github.com/Arch-relic/AgroNaliz/actions/runs/39035077568/artifacts/12101538033).
Это не установочный кандидат: CI миграция использует отдельные debug fixtures3000003/4,
production signer по-прежнему закрыт и настоящая MAIN APK не подготовлена.

Локально python3 -m unittest discover -s docs -p 'test_*py' -q:72/70pass/2skip без APK;
py_compile новых scripts, JSON и diff --check успешны.
source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test
не стартует до компиляции: usable wildcard IP. Android проверен в CI, не локально.

Изменены/созданы9 файлов, удалений нет:
- .github/workflows/main-room23-checks.yml (новый readonly CI, не signer)
- app/src/androidTest/java/ru/fo6osik/workjournal/MainRoom23MigrationTest.kt (новый)
- docs/run-main-room23-checks.py (новый)
- docs/verify-main-room23-inputs.py (новый)
- docs/main-room23-release-policy-proposal.md (новый, только предложение)
- docs/main-room23-ci-review.md (новый отчёт)
- docs/main-1.0.8-release-audit.md (пометка обновлённого evidence)
- docs/main-1.0.8-verification.json (migration verified, signing false)
- docs/codex-handoff.md
После документирующего коммита незакоммиченной работы нет. Production src/main,
модели/миграции, MAIN/TEST signer и старые protected workflows не менялись.

Сертификат исторических MAIN/TEST одинаковый3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C,
пакеты разные. По прямому уточнению пользователя это не блокер: MAIN сохраняет
оригинальный pin/ANDROID_* signer; менять сертификат ради разделения не нужно.
Свежее наличие production Secrets здесь не проверяли, они не используются в audit.

Остаётся отдельное решение о policy: сейчас только main/owner/attempt1 и room==22.
Предложение сохраняет guards/cleanup/монотонный code, добавляет narrow23+installed22
и позитивный audit того же SHA из своего owner/main dispatch. Оно НЕ применяется
этой задачей. Будущий путь после отдельного разрешения: policy+негативные tests,
CI, отдельно разрешённый merge, audit точного main SHA, прежний защищённый signer.
Никакого PR signing, обхода защиты или автоматического merge.

Реальная база/телефон/OEM и оригинальная production-подпись новой APK не проверялись;
CI доказывает конкретный синтетический22→23 сценарий, не отсутствие любых дефектов.
Нет MAIN/TEST установок на пользовательские устройства, очистки/восстановления
реальных данных, публичного Release, подписанного артефакта или изменения подписи/id.

---

История предыдущего аудита:

# Этап: финальный аудит MAIN1.0.8 — выпуск BLOCKED

17 октября2026. feature/harvest-1-0-8, Draft PR33 открыт, не merged.
Проверенный head8702bf6cfcd7a783236bd2bb325795b3cee154d7 / app ec39c15f708120ddba4397f06264d95120bae2a7;
main4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee. Следующий коммит только отчётный,
не называть его SHA проверенным Android-кодом. Product/tests/workflows не менялись.

[Полный аудит и блокирующие причины](main-1.0.8-release-audit.md).
[verification BLOCKED](main-1.0.8-verification.json): signedApkPrepared=false,
versionCode/apkSha256/artifactUrl=null. MAIN1.0.8 не подготовлена/не опубликована.

Последний MAIN1.0.7 run38923727875#3 успешен: ru.fo6osik.workjournal,
code3000003/Room22, оригинальный сертификат
3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C.
Логи подтверждают работу оригинальных ANDROID_* Secrets тогда, не их свежее
наличие. TEST_* не используются. Исторический MAIN сертификат совпадает с TEST:
не менять его, не обещать разные сертификаты при сохранении прежней подписи.
Публичные metadata установленной сейчас MAIN неизвестны, данные телефона не читались.

Блокеры: защищённый MAIN workflow толькоmain/ref owner/attempt1, собирает main1.0.7;
Room23 preflight намеренно закрыт (room!=22). Точный MAIN22→23 сценарий не выполнялся:
MAIN#30 проверяет фактически20→23→23 с синтетическим ключом, хотя имена/лог говорят21.
TEST22→23 отдельно прошёл, это не равно требуемой MAIN22 проверке. Merge запрещён,
безопасного pre-merge MAIN artifact в существующем workflow нет. Защита не обходилась.

CI текущего8702bf6: Android#103 run39030341329 SUCCESS (46Python+84instrumental),
MAIN#30 run39030341188 SUCCESS (26Python+3instrumental); обе JVM-задачи выполнены
заново, число JVM-тестов стандартный лог не печатает. FullBackup4/4, Harvest1085/5,
QuickAccess2/2, TEST22→23 seed/verify/restart3успеха, UI/Options и TEST update прошли.
Новые локальные python3 -m unittest discover -s docs -p 'test_*py' -q:72/70pass/2skip;
docs/run-harvest-jvm-checks.py --gradle-lib <Gradle8.13/lib>:28/28реальных Kotlin/JUnit
успешны. Это не Android-сборка. Новая локальная Android-сборка не запускалась:
известный wildcard IP ограничитель сохраняется. diff --check выполнен.

Изменены только docs/main-1.0.8-release-audit.md (новый),
docs/main-1.0.8-verification.json (новый), docs/codex-handoff.md. Удалений нет.
После отчётного коммита незакоммиченной работы нет. Отчёт содержит сопоставление
48 файлов PR, функций, миграции/backup, проверок и рисков без обещания полной верификации.

Далее остановиться по условию пользователя о неподтверждённом безопасном обновлении.
Нужны точный disposable MAIN22→23 прогон, отдельный безопасный pre-merge MAIN signing
путь, актуальные публичные metadata MAIN и уточнение требования разных сертификатов.
Не открывать signing всем PR/не убирать room gate без аудита, не merge/релиз/установка.
Реальные данные не затрагивались, секреты не раскрывались, production signer не запускался.

---

История предыдущего этапа:

# Этап: единый пункт резервного копирования в Options

17 октября 2026. Завершено, остановиться для дальнейших указаний.
Ветка `feature/harvest-1-0-8`, [Draft PR33](https://github.com/Arch-relic/AgroNaliz/pull/33)
открыт, не merged. Head до задачи `791fa7e418e11e88c7c1f43ba6c41eb0ce0eedc8`;
опорный main `4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee`.
Проверенный GitHub SHA **78b8b754f3c8aa01c81f97b51cfe298f1e2aed57**;
локальный `5cd1d5d`, общий tree `beee4afd3e72bdaff2e1980cf17b2726066d1823`.
API-публикация объясняет разные SHA. Следующий коммит меняет только этот отчёт;
не называть SHA самого документирующего коммита проверенным Android-результатом.

## Результат и границы

`buttonDataBackup` («Данные и резервные копии») теперь открывает существующую
BackupRestoreActivity вместо DataBackupActivity. Нижняя `buttonBackupRestore`
и её обработчик удалены. Developer footer сохраняет заголовок, динамическую
версию с MAIN/TEST-разделением и «Справочник техники».
DataBackupActivity, её layout и manifest сохранены. `rg` по app/src после изменения
находит DataBackupActivity только в определении класса и manifest; пользовательских
переходов к ней нет. Настоящий экран создания/восстановления/импорта, его layout и
реализация backup не менялись. Предыдущий UI-отчёт ниже отражает прошлый этап:
описание двух backup-входов в нём заменено текущим единым пунктом меню.

Изоляция TEST backup, пути/правила безопасности, Room23, миграции, расчёты,
подписи, applicationId, версии и защищённые workflows не менялись. Нет действий
с пользовательской базой, очистки/восстановления/установки на смартфон.

Изменены ровно пять файлов относительно начала задачи (пути от корня):
- app/src/main/java/ru/fo6osik/workjournal/OptionsActivity.kt
- app/src/main/res/layout/activity_options.xml
- app/src/androidTest/java/ru/fo6osik/workjournal/AppVersionUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/MainScreenLayoutUiTest.kt
- docs/codex-handoff.md
Удалённых файлов нет. Незакоммиченных изменений после отчётного коммита нет.

## Проверки

**[Android PR checks #102 — SUCCESS](https://github.com/Arch-relic/AgroNaliz/actions/runs/39029306687)**
для указанного проверенного SHA:
- debug/TEST и обе instrumentation APK собраны. `testDebugUnitTest` и
  `testDebugTestUnitTest` выполнены заново, не UP-TO-DATE. Число JVM-тестов
  стандартный лог не печатает; результаты находятся в diagnostics.
- 46 Python:13 catalog + 6 version + 27 TEST safety/signing tools, все успешны.
- 84 успешных инструментальных выполнения в 45 вызовах, ошибок нет.
- AppVersionUiTest:3 успешных вызова (MAIN, TEST, TEST после update). Сохранены
  проверки версии/TEST-маркера/Room/12sp; добавлены переход по верхней backup-кнопке
  и видимость/доступность Create/Restore/Import. Сами операции не нажимаются.
- MainScreenLayoutUiTest:3/3, отсутствие дубликата в Options, переход именно в
  BackupRestoreActivity, рабочий каталог, прежняя геометрия/GONE уборки,
  compact720×960/font160% без обрезанного текста.
- HarvestQuickAccessUiTest2/2, Harvest108Test5/5, HarvestFullBackup108Test4/4;
  Room22→23 seed/verify/restart3 вызова успешны. Прежние full JSON roundtrip/merge,
  повторный импорт, legacy backup и rollback сохранились и прошли.
- Безопасное TEST update прошло: тот же синтетический ключ/больший code сохраняет
  Room/preferences/files, другой ключ отвергнут без потери данных; read-only JSON
  export и синтетическое восстановление успешны, MAIN не изменён.

**[Main APK safe update checks #29 — SUCCESS](https://github.com/Arch-relic/AgroNaliz/actions/runs/39029306676)**:
сборка/JVM выполнены заново,26 Python и3 инструментальные проверки успешны.
Синтетическая MAIN — только в ранее разрешённом одноразовом CI-эмуляторе.

Итого два CI:72 успешных Python и87 инструментальных выполнений, ошибок нет.
Тесты и workflow не отключались/не ослаблялись. Команды сборки в CI:
`:app:assembleDebug :app:assembleDebugTest :app:testDebugUnitTest
:app:testDebugTestUnitTest :app:assembleDebugAndroidTest`, затем
`-PtestBuildType=debugTest :app:assembleDebugTestAndroidTest`; существующие runners
изоляции/уборки/топлива/1.0.8/signing выполнены полностью.

Локально `python3 -m unittest discover -s docs -p 'test_*py' -q`:72 всего,
70 успешно,2 пропущены без APK. `git diff --check` успешен.
`source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test`
остановился до компиляции: `Could not determine a usable wildcard IP for this machine`.
Не считать локальную Android-сборку успешной; реальная сборка/проверка — Actions.

## Риски, ручная проверка и продолжение

Риска изменения данных от этой корректировки не выявлено: изменены только меню и
навигация, целевой backup-экран сохранён. Реальная пользовательская база, физический
телефон и OEM-поведение не проверялись. DataBackupActivity пока остаётся неиспользуемым
кодом по явному ТЗ; удалять отдельно только по следующему заданию.

Для ручной проверки после подготовки обновлённой TEST: Options → «Данные и резервные
копии» должно открывать три действия (создание, восстановление, импорт); внизу только
заголовок/версия/каталог. Возврат назад ведёт в Options. Не выполнять восстановление
рабочей базы ради проверки навигации. Не удалять и не очищать приложение.
Прежняя APK2000008 не содержит эту последнюю корректировку. Новая устанавливаемая
APK этой задачей не публиковалась, подпись/id не менялись.

Незавершённых задач кода нет. После отчёта остановиться; не merge, не релиз,
не устанавливать APK на пользовательские устройства.

---

История предыдущего этапа:

# Этап: окончательная компоновка главного экрана 1.0.8

17 октября 2026. Задача завершена. Только существующая `feature/harvest-1-0-8`.
[PR #33](https://github.com/Arch-relic/AgroNaliz/pull/33) открыт, Draft, не merged.
Main: `4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee`; head до задачи:
`5c1c74b9299f13537369da6180747140063f7ab5`.

Проверенный GitHub SHA `51570722d4b909ae3fa203eccb2678f89e1c6392`;
локальный `20811d4`, одинаковый tree `3013b840f499c7cdecc3bae76ae516bd19d51ad7`.
Публикация через API объясняет разницу SHA. Последующий коммит меняет только два
отчёта, не исходники/тесты; не считать SHA самого документирующего коммита проверенным.

Developer-блок целиком внизу Options с прежними переходами и MAIN/TEST-маркером.
Main: прежний gap 56dp + 16dp перед Options; уборка min96dp, прежняя ширина,
центрирование, гибкое место сверху, 24dp снизу, GONE без фиксированной обёртки.
Оба экрана scroll/fillViewport; fitsSystemWindows на внешнем FrameLayout.
Бизнес-логика/Room23/миграции/backup/подписи/id/workflows не менялись.

Изменены/добавлены (пути от корня, удалений нет):
- app/src/main/java/ru/fo6osik/workjournal/MainActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/OptionsActivity.kt
- app/src/main/res/layout/activity_main.xml
- app/src/main/res/layout/activity_options.xml
- app/src/androidTest/java/ru/fo6osik/workjournal/AppVersionUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/MainScreenLayoutUiTest.kt (новый)
- docs/test_app_version.py
- docs/run-harvest-108-checks.py
- docs/main-screen-1.0.8-review.md (новый)
- docs/codex-handoff.md

Проверки проверенного SHA:
- [Android #100 SUCCESS](https://github.com/Arch-relic/AgroNaliz/actions/runs/39016802399):
  обе APK и instrumentation APK, обе JVM-задачи заново, 46 Python без пропусков,
  84 успешных инструментальных выполнения / 45 вызовов.
  Новые UI 3/3 (навигация, геометрия/GONE, compact720×960/font1.6), QuickAccess2/2,
  Harvest108 5/5, FullBackup4/4, Room22→23 seed/verify/restart 3 вызова — всё успешно.
  Безопасное обновление TEST и отказ другого ключа без потери данных прошли.
- [Main #27 SUCCESS](https://github.com/Arch-relic/AgroNaliz/actions/runs/39016802395):
  сборка/JVM, 26 Python и 3 инструментальных выполнения успешны.
- Итого CI: 72 Python и 87 инструментальных выполнений, ошибок нет.
  Число JVM-тестов не печатается в стандартном логе, отчёты в diagnostics;
  задачи выполнялись заново, не UP-TO-DATE.
- Локально `python3 -m unittest discover -s docs -p 'test_*py' -q`:72/70pass/2skip
  без APK. XML parse и diff --check успешны. Проектная команда
  `source /workspace/agronaliz-env.sh`, `agrogradle :app:assembleDebug :app:test`
  не стартует: FileLockContentionHandler / usable wildcard IP. Android реально
  собран и проверен в Actions, локальный результат не считать успешной сборкой.

Первый #98 и диагностический #99 падали в compact scrollTo(WorkNotes); остальные
две новые UI-проверки прошли. Координаты println не попали в Actions console.
В ScrollView расчёт scroll-to не вычитает padding; системные inset вынесены за
ScrollView, после чего тот же строгий тест прошёл. Утверждения не ослаблены.

[Полный отчёт, ограничения и ручная проверка](main-screen-1.0.8-review.md).
Физический телефон/OEM/экстремальный font scale не проверены. Старый опубликованный
TEST APK не содержит эту новую UI-корректировку; новая устанавливаемая APK не
публиковалась. Данные не затронуты, риска потери данных от UI-изменения не выявлено.
MAIN устанавливалась только синтетическая в ранее разрешённом одноразовом CI.

Далее: остановиться и ожидать независимый review. Не merge, не релиз и не установка
на пользовательское устройство. Незавершённых задач кода нет; ручная проверка
на смартфоне — после отдельной согласованной подготовки TEST APK, без очистки данных.

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
