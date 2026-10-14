# PR #26: синхронизация с main после PR #28

Дата: 2026-10-14. Статус: конфликты разрешены, объединённый результат прошёл CI.
GitHub mergeable=true; технически готов к объединению владельцем. Обновляется только feature/harvest-day-density; PR не объединяется.

## Опорные версии

Main: 3c24b3ebc5d5f36aef4f3fa2f5e4a654dadcfd63 (merge PR #28).
Исходный head PR #26: c7d360245c3d8d416fca232de115588b2579610f.
Общий предок: c0ea3d01a97dd0eb4814b59307b44169916821bc.
Создан merge-коммит 7dd7ba47c7ce702e0b9b6445cbb33e825b8f3366 с обоими
родителями, без переписывания истории. Это проверенный SHA, CI #67 success.
Этот отчёт публикуется следующим документационным коммитом поверх него;
собственный SHA отчёта намеренно не записывается в содержащий его файл. Локальное зеркало изолировано
от прежних рабочих каталогов; публикация выполняется GitHub Git API.

## Разрешение конфликтов и сохранённое поведение

Пересечение изменений — .github/workflows/android-pr-checks.yml и этот отчёт.
Workflow объединяет оба набора тестов и сохраняет отказ от публикации
временной TEST APK. Порядок эмулятора: изоляция → уборка/реальный Room20→21
upgrade → подпись/сохранность обновления. Harvest runner удаляет только свою
синтетическую TEST в одноразовом эмуляторе, поэтому signer runner получает
нужное чистое исходное состояние без удаления реальных пользовательских данных.

Весь код уборки PR #26 сохранён без изменений: плотности каждого дня,
необязательная площадь, диапазоны объёма/массы/урожайности, неполные кампании,
проверки хронологии с переходом через полночь, безопасное повторное открытие
дня/кампании, таймеры/события, backup/import и Room20→21 (три nullable-поля).
Новых миграций нет, destructive migration отсутствует.
Все компоненты PR #28 сохранены без изменений: TEST-only versionCode,
отдельный signer workflow, SHA-256 pin, owner/main-only secret context,
read-only экспорт копии, JVM/Python/emulator signing tests. Основная подпись
и PR #25 не затронуты. Код и данные основного приложения не меняются этим merge.

## Файлы

Относительно прежнего head PR #26 созданные (из main/PR #28):
- .github/workflows/test-apk-publish.yml
- app/src/androidTest/java/ru/fo6osik/workjournal/TestSigningUpdateTest.kt
- docs/export-installed-test-backup.py
- docs/run-test-signing-checks.py
- docs/test-apk-signing.md
- docs/test_test_apk_signing.py
- docs/test_test_backup_export.py

Изменённые (main либо разрешение конфликта):
- .github/workflows/android-pr-checks.yml
- .gitignore
- app/build.gradle.kts
- docs/codex-handoff.md

Удалений нет. Остальные 31 файл функционала/тестов уборки побайтово сохранены
из head PR #26; у PR относительно актуального main они остаются в diff.

## Проверки

CI #67: https://github.com/Arch-relic/AgroNaliz/actions/runs/38528515013 — success.
Реально выполнены, не UP-TO-DATE:
- `./gradlew -PtestVersionCode=1000067 :app:assembleDebug :app:assembleDebugTest :app:testDebugUnitTest :app:testDebugTestUnitTest :app:assembleDebugAndroidTest --no-daemon --stacktrace --console=plain` — success.
- `./gradlew -PtestVersionCode=1000067 -PtestBuildType=debugTest :app:assembleDebugTestAndroidTest --no-daemon --stacktrace --console=plain` — success.
- 99 JVM-тестов debug и 99 debugTest; 0 failures/errors/skipped, подтверждено XML artifact.
- `python3 -m unittest discover -s docs -p 'test_validate_document_catalog.py' -v` — 13 pass.
- `python3 -m unittest discover -s docs -p 'test_test_*.py' -v` — 25 pass, 0 skip,
  включая точный inline signer с временным тестовым ключом, неверный SHA,
  очистку ключа, actual main versionCode1 и TEST ID/versionCode1000067.
- `python3 docs/verify-build-variants.py` — ID/providers/изоляция pass.
- `./gradlew -PtestVersionCode=1000068 :app:assembleDebugTest --no-daemon --stacktrace --console=plain` — success; подготовлена вторая версия TEST для обновления.
- `.migration-v20/gradlew -p .migration-v20 :app:assembleDebugTest --no-daemon --stacktrace --console=plain` — success; точный исходный Room20 commit c0ea3d0, без TEST version override.
- `python3 docs/run-build-isolation-checks.py`, `python3 docs/run-harvest-checks.py`,
  `python3 docs/run-test-signing-checks.py` — success на одноразовом API35 эмуляторе.
  39 исполнений инструментальных test methods (26 уборки, плюс подпись/изоляция
  и контроль main). Реальный APK update20→21 сохраняет старые записи, таймеры,
  бункеры, события; проверены площадь, плотность, хронология/полночь,
  повторное открытие/закрытие кампании, backup/import, force-stop/restart.
  После этого обновление TEST тем же fixture ключом сохраняет Room/preferences/files;
  другой ключ отвергнут Android с данными нетронутыми. Проверены read-only
  экспорт приватного JSON и синтетический restore v21 (включая площадь/плотность).
- Локально catalog13 pass, signing/backup25: 24 pass и 1 SDK skip до сборки.
  `git diff --check`, YAML parse, Python py_compile — pass.
- `source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test` —
  не стартует: Could not determine a usable wildcard IP for this machine.
  Это ограничение локальной среды; успешная сборка выполнена GitHub Actions.

Artifacts: обычная main debug APK и диагностика, временная TEST APK не публикуется.
Стабильный production TEST signer сохранён, не запускался из PR. Ни пользовательские
данные телефона, ни настоящий постоянный ключ не использовались в тестах.
Документационный коммит после проверенного кода не меняет Android или workflow;
новый автоматический CI оценивается отдельно, повторная ручная сборка одного
отчёта по AGENTS.md не обязательна.

## Ограничения и ручные проверки

Не объявлять совместимость старой установленной TEST с новым ключом:
оригинальный приватный ключ ранее не найден. Постоянный выпуск требует
TEST_* Secrets и полного TEST_CERT_SHA256; signer workflow не запускается
из PR. Room станет 21 после отдельного merge владельцем; до того main20.
Проверки проводятся на синтетических данных; физический телефон недоступен.
Ручные сценарии уборки: docs/harvest-results.md. Подпись/безопасная копия:
docs/test-apk-signing.md (JSON включает Room, не весь state приложения).

## Pull Request

https://github.com/Arch-relic/AgroNaliz/pull/26 — существующий Draft PR.
Проверено после CI: mergeable=true, Draft=true, merged=false; main не менялся.
Владелец решает перевод из Draft и объединение. Самостоятельно не объединять.
