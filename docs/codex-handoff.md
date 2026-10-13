# Передача контекста: Issue #27, стабильная TEST подпись

Дата: 2026-10-13. Статус: безопасная реализация в Draft PR #28, CI успешен.
Постоянный выпуск и обновление старой установки блокированы отсутствующим ключом. Основной APK signer не изменён.

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
Проверенный код: 2c39d60359f9e198b50fb1ef9d6554a844eafd55 (CI #64).
Предыдущий 0cd90d698df58fdcc8a2bd3773e89804ffb2a03e также прошёл CI #63.
Отчёт сохраняется следующим отдельным коммитом; его собственный SHA здесь
не указан намеренно. Код после проверенного SHA не меняется, только этот отчёт.
Локальное зеркало остаётся незакоммиченным; изменения опубликованы через GitHub
Git API в отдельной feature-ветке. Main не менялся.
`python3 -m unittest discover -s docs -p 'test_test_*.py' -v`: 25 тестов,
24 успешны, один SDK тест пропущен до появления собранной TEST APK.
`source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test`:
запуск облачного Gradle блокирован до сборки: Could not determine a usable
wildcard IP for this machine. Это не успешная сборка/UP-TO-DATE.
GitHub Actions #64: https://github.com/Arch-relic/AgroNaliz/actions/runs/38451873836
Все steps успешны, свежие запуски, не UP-TO-DATE:
- `./gradlew -PtestVersionCode=1000064 :app:assembleDebug :app:assembleDebugTest :app:testDebugUnitTest :app:testDebugTestUnitTest :app:assembleDebugAndroidTest --no-daemon --stacktrace --console=plain` — успешен.
- `./gradlew -PtestVersionCode=1000064 -PtestBuildType=debugTest :app:assembleDebugTestAndroidTest --no-daemon --stacktrace --console=plain` — успешен.
- JVM: 67 debug + 67 debugTest тестов, ошибок/пропусков нет (XML CI #63,
  тот же набор; оба JVM tasks также реально выполнены в CI #64).
- `python3 -m unittest discover -s docs -p 'test_validate_document_catalog.py' -v` — 13 pass.
- `python3 docs/verify-build-variants.py` — ID/providers/изоляция успешны.
- `python3 -m unittest discover -s docs -p 'test_test_*.py' -v` — 25 pass,
  0 skip, включая реальный SDK signer, отрицательный SHA и удаление temp key.
  Проверены main ID/versionCode1 и TEST ID/versionCode1000064 в самих APK.
- `./gradlew -PtestVersionCode=1000065 :app:assembleDebugTest --no-daemon --stacktrace --console=plain` — успешен.
- `python3 docs/run-build-isolation-checks.py` и
  `python3 docs/run-test-signing-checks.py` — успешны, 11 одиночных запусков
  instrument methods в одноразовом API35 эмуляторе. Обновление одним ключом
  сохранило Room/preferences/files, другим ключом отклонено без изменения
  данных; приватный JSON скопирован read-only и синтетически восстановлен;
  основная установка остаётся неизменной. SYNTHETIC fixtures, не телефон.
- `python3 -m py_compile ...`, YAML parse, `git diff --check` — успешны локально.

Artifacts CI #64: только обычная main debug APK и диагностика.
Стабильно подписанной пользовательской TEST APK в этом PR нет намеренно.
Production workflow пока не запускался: только main после отдельного merge
владельцем и настройки TEST ключа/pin. Не выдавать fixture подписи за постоянную.

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
не объединён. CI #63 и #64 успешны. Финальный отчёт — документационный коммит после
проверенного кода; повторная Android сборка для одного отчёта не обязательна
по AGENTS.md. Новые автоматически запущенные проверки нужно оценивать отдельно.
