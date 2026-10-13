# Передача контекста: Issue #27, стабильная TEST подпись

Дата: 2026-10-13. Статус: реализация подготовлена, локальные проверки пройдены;
Draft PR и CI публикуются следующим шагом. Основной APK signer не изменён.

## Изменения

Созданы:
- .github/workflows/test-apk-publish.yml
- app/src/androidTest/java/ru/fo6osik/workjournal/TestSigningUpdateTest.kt
- docs/export-installed-test-backup.py
- docs/run-test-signing-checks.py
- docs/test_test_apk_signing.py
- docs/test_test_backup_export.py
- docs/test-apk-signing.md

Изменены:
- .github/workflows/android-pr-checks.yml
- .gitignore
- app/build.gradle.kts
- docs/codex-handoff.md

Удалений нет. Нет изменений Room, данных, APK основной подписи и PR #25/#26.
Раздельный signer использует TEST_* Secrets и TEST_CERT_SHA256 Variable,
ограничен владельцем/main/первой попыткой. Проверяет сертификат и ID до upload.
TEST versionCode увеличивается независимо от основной версии. Raw TEST из PR
больше не публикуется; добавлены проверки обновления в свежем эмуляторе.

## Ветка и проверки

Ветка feature/test-apk-stable-signing, main
c0ea3d01a97dd0eb4814b59307b44169916821bc.
На момент этого отчёта изменения локальные; проверенного удалённого SHA ещё нет.
`python3 -m unittest discover -s docs -p 'test_test_*.py' -v`: 25 тестов,
24 успешны, один SDK тест пропущен до появления собранной TEST APK.
`source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test`:
запуск облачного Gradle блокирован до сборки: Could not determine a usable
wildcard IP for this machine. Это не успешная сборка/UP-TO-DATE.
CI основной/TEST сборки, JVM и эмуляторные проверки ожидаются после открытия PR.

## Ограничения и ручные действия

Старый приватный TEST ключ не найден в доступной среде; старый полный SHA
не предоставлен (Issue сообщает только 5F3A...). Android не позволяет обновить
старую установку новым ключом. Ни удаление, ни очистка данных не выполняются.
Нужны постоянный TEST keystore и четыре TEST_* Secrets плюс полный публичный
TEST_CERT_SHA256. Возможности безопасно записать Secrets через доступные
инструменты нет; владелец настраивает их в GitHub. Производственная подписанная
TEST APK пока не создана, её будущий сертификат неизвестен.
Текущий main Room20: floor21 намеренно блокирует downgrade старой установки21.
JSON сохраняет только Room, не все файлы/настройки. Реальный телефон и данные
не доступны; доказательство эмулятора относится только к синтетическим данным.
Подробный безопасный порядок: docs/test-apk-signing.md. PR не объединять.

## Pull Request

Draft PR #28: https://github.com/Arch-relic/AgroNaliz/pull/28. Открыт, Draft,
не объединён. CI https://github.com/Arch-relic/AgroNaliz/actions/runs/38451789433
выполняется для 0cd90d698df58fdcc8a2bd3773e89804ffb2a03e. Итог будет обновлён.
