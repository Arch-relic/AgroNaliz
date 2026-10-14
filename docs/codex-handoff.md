# Основная APK — безопасное обновление, выпуск ожидает владельца

Дата: 2026-10-14. Статус: Draft PR #29, реализация проверена двумя успешными CI.
Выпуск НЕ завершён. Настоящая актуальная APK оригинальным ключом ещё не
создана/проверена. Требуется отдельный merge владельцем, затем manual release.
Codex main не меняет, PR не объединяет и данные телефона не трогает.

## Контекст

Ветка: feature/main-original-safe-update.
Main: b59df2ed7be777fd69a4848111d93f86b3e3c3db (включает PR #26/#28).
Проверенный SHA: 2b9eba3850aa95b5bc44694147c0a61774619011.
Этот отчёт сохраняется отдельным следующим коммитом только документации;
собственный SHA отчёта намеренно не вписывается в содержащий его файл.
GitHub commits опубликованы Git API; локальное изолированное зеркало остаётся
незакоммиченным. Предыдущие рабочие каталоги не изменялись.

Владелец подтвердил установленную основную versionCode53, Room20 и pin:
3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C.
Историческое настоящее подписание в PR #25 уже выдало этот публичный pin,
но иной ожидаемый pin тогда заблокировал выпуск. PR #25 не менялся, head
b7b73397470e0ae3e9b45db3bfdcefd7ea5abd2f проверен неизменным.
Исторический сертификат не доказывает подпись новой APK до свежего release.

## Реализация и файлы

Изменены:
- app/build.gradle.kts
- docs/codex-handoff.md

Созданы:
- .github/workflows/main-apk-publish.yml
- .github/workflows/main-apk-checks.yml
- app/src/androidTest/java/ru/fo6osik/workjournal/MainUpdateTest.kt
- docs/test_main_apk_release.py
- docs/test_verify_main_update.py
- docs/run-main-update-checks.py
- docs/verify-main-apk-update.py
- docs/main-original-update.md

Удалений нет. Схема Room21, migrations (включая20→21), runtime функциональность
и TEST версия/подпись/workflows не меняются. Новое mainVersionCode свойство
влияет только на debug main outputs; без свойства main остаётся1, TEST
самостоятельно сохраняет default/testVersionCode политику.

Publish original main APK — owner/main-only workflow_dispatch/attempt1.
Build без Secrets; sign без checkout/Gradle, с временным keystore0700/0600,
прежними четырьмя ANDROID_* Secrets, фиксированным public pin, strict
package/version/Room gates. APK публикуется после SDK проверки. Key удаляется
finally/always, unsigned transport удаляется или истекает1day. История runs и
concurrency предотвращают обратный порядок версии. Room target должен быть21,
установленный Room20 либо21; неизвестные версии блокируются до нового аудита.

## Фактические проверки

Main APK safe update checks #3 — success:
https://github.com/Arch-relic/AgroNaliz/actions/runs/38603496625

- `./gradlew -PmainVersionCode=3000001 :app:assembleDebug :app:assembleDebugTest :app:assembleDebugAndroidTest :app:testDebugUnitTest --no-daemon --stacktrace --console=plain` — success;99 JVM pass,0 errors/skipped по XML.
- `python3 -m unittest discover -s docs -p 'test_main_apk_release.py' -v` —18 pass,0 skip: реальный exact inline signer на существующем disposable Gradle debug fixture, неверный pin, cleanup, gates и неизменная TEST versionCode1 при main override.
- `python3 -m unittest discover -s docs -p 'test_verify_main_update.py' -v` —8 pass.
- `./gradlew -PmainVersionCode=3000002 :app:assembleDebug --no-daemon --stacktrace --console=plain` —success.
- `.main-v20/gradlew -p .main-v20 :app:assembleDebug --no-daemon --stacktrace --console=plain` —success, pinned C0 baseline Room20 с единственной metadata-поправкой versionCode53, только ephemeral fixture.
- `python3 docs/run-main-update-checks.py` —3 instrumentation methods pass на свежем API35 emulator: основной пакет53/Room20→3000001/Room21→3000002/Room21, без uninstall/clear/downgrade. Проверены все прежние значения пользовательских таблиц синтетической БД, настройки, таймеры, файл, сертификат, новые площадь/плотность и force-stop/restart. Guards в script и test class запрещают физический телефон/существующую main установку.

Android PR checks #72 — success:
https://github.com/Arch-relic/AgroNaliz/actions/runs/38603496901

- Стандартные main/TEST и обе instrumentation APK собраны.
- JVM99 debug +99 debugTest,0 errors/skipped по XML; tasks реально выполнены, не UP-TO-DATE.
- Catalog13 и TEST signer/backup25 Python tests pass, SDK test не пропущен.
- APK ID/provider/data isolation,39 instrumentation исполнений (26 уборки), TEST Room20→21, chronology/reopening/backup/restart и TEST same-key update/different-key reject успешны.

Локально:18 signing/release tests (17 pass,1 SDK skip до APK),8 read-only tests
pass, прежние TEST tests25 (24 pass,1 SDK skip); YAML/py_compile/diff pass.
`source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test`
не стартует: Could not determine a usable wildcard IP for this machine.
Успешная сборка выполнена GitHub Actions, это не локальный pass/UP-TO-DATE.

Первый main CI SDK test не смог прочитать default debug key по фиксированному
пути. Исправлено определение Store через Gradle signingReport (stdout
перехватывается); повторный CI успешен. Production key не использовался.
После этого отчёта автоматический CI запускается отдельно, без изменения
кода; ручная повторная Android сборка документации по AGENTS.md не обязательна.

## Ограничения и обязательный следующий шаг

Реальные ANDROID_* Secrets доступны только trusted release job после owner
merge. Новый постоянный ключ не создавался. Подпись готовой новой production
APK и actual data пользователя НЕ подтверждены fixture/историческими тестами.
GitHub CI публикует диагностику и обычную debug APK; последняя НЕ предназначена
для обновления основной AndroidIDE версии. Original artifact пока отсутствует.

Владелец после проверки объединяет PR #29 (не #25), затем Actions → Publish
original main APK → Run workflow → main → minimum_installed_version_code53,
minimum_room_version20. Если телефон уже обновлён, вводить новые фактические
значения. Скачивать только app-main-original-main-<SHA>-v<code>, распаковать
app-main-original.apk и verification.json. Workflow ещё раз проверяет Pin,
package, strictly higher version и Room до публикации.

Далее read-only verify-main-apk-update.py на компьютере владельца сравнивает
установленную base.apk с новой APK и report. Room ввод подтверждён владельцем
из его backup; script не читает живую БД. Только после PASS открывать APK как
обновление. Не удалять приложение/данные и не использовать TEST/debug APK.
Сохранять backup приватно вне Git; JSON содержит Room, не все prefs/files.
Физический телефон недоступен CI; нет безусловной гарантии для произвольной
нестандартной Room20 схемы или неподтверждённых данных. При несовпадении
остановить установку; destructive recovery запрещён.
Полные инструкции: docs/main-original-update.md.

## Pull Request

https://github.com/Arch-relic/AgroNaliz/pull/29 — открыт, Draft, не объединён.
Слияние и запуск настоящего release — точное действие владельца, после него
проверить новый original APK artifact и actual installed certificate.
