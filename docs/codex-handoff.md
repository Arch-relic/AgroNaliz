# Безопасное обновление основной APK — подготовка выпуска

Дата2026-10-14. Статус: реализация подготовлена, CI/Draft PR следующим шагом.
Выпуск не завершён: для owner/main-only workflow требуется отдельный merge
владельцем; Codex не объединяет. Настоящая подписанная актуальная APK ещё
не создана/проверена, безопасная установка на личный телефон не выполнена.

Main b59df2ed7be777fd69a4848111d93f86b3e3c3db, ветка
feature/main-original-safe-update. Владельцем подтверждены versionCode53,
Room20 и pin3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C.
Историческое реальное подписание PR #25 выдало этот pin; тогда иной expected
pin остановил публикацию. PR #25 не изменён. Текущий main включает PR #26/#28,
Room21, уборку, хронологию и reopening. Их код не меняется.

## Файлы

Изменены: app/build.gradle.kts, docs/codex-handoff.md.
Созданы:
- .github/workflows/main-apk-publish.yml
- .github/workflows/main-apk-checks.yml
- app/src/androidTest/java/ru/fo6osik/workjournal/MainUpdateTest.kt
- docs/test_main_apk_release.py
- docs/test_verify_main_update.py
- docs/run-main-update-checks.py
- docs/verify-main-apk-update.py
- docs/main-original-update.md
Удалений нет. Схема, миграции, подпись/версия TEST и android-pr-checks не меняются.
Main-only Gradle property, отдельный trusted signer с прежними ANDROID_* Secrets,
временным keystore и strict pin/ID/version/Room checks. Публикация только после
проверки. mainVersionCode override не затрагивает debugTest.

## Проверки

Локально: 18 signing/release tests (17 pass,1 SDK skip до APK),8 read-only tests
pass, YAML/py_compile/diff checks pass. Cloud Gradle команда
`source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test`
не стартует: Could not determine a usable wildcard IP for this machine.
CI ещё не выполнен; результаты/проверенный SHA и номер PR будут добавлены.
Модульные тесты не получают production secrets. Новые update tests используют
существующий временный Gradle debug key runner, не новый production key.
На момент отчёта изменения локальные; GitHub commits/PR публикуются далее.

## Ограничения и передача владельцу

Нужен owner merge нового PR, затем Publish original main APK/main/53/20.
Не объединять #25 и не менять TEST key. Совместимость подписи готовой новой
APK пока не подтверждена: её подтвердят apksigner gate в trusted release job
и read-only сверка установленной APK на телефоне. Не считать выпуск завершённым
по историческому pin или fixture тестам. Room compatibility проверяется для
20→21/21→21 на синтетических данных, не всей реальной базы телефона.
Инструкции скачивания/readonly pre-install/обновления: docs/main-original-update.md.

## PR

Draft PR #29: https://github.com/Arch-relic/AgroNaliz/pull/29, открыт, не объединён.
Объединение только владельцем. CI выполняется, итог будет обновлён.
