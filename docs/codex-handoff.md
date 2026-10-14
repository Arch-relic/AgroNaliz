# PR #26: синхронизация с main после PR #28

Дата: 2026-10-14. Статус: конфликты разрешены, проверка объединённого результата
ожидает CI. Обновляется только feature/harvest-day-density; PR не объединяется.

## Опорные версии

Main: 3c24b3ebc5d5f36aef4f3fa2f5e4a654dadcfd63 (merge PR #28).
Исходный head PR #26: c7d360245c3d8d416fca232de115588b2579610f.
Общий предок: c0ea3d01a97dd0eb4814b59307b44169916821bc.
Будет создан merge-коммит с обоими родителями, без переписывания истории.
На момент записи нового проверенного SHA нет. Локальное зеркало изолировано
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

Проверки объединённого результата ещё не выполнены; прежние успешные CI
#61 (уборка) и #64 (подпись) не заменяют проверку этого merge.
Предусмотрены оба APK, JVM, catalog/signing Python, изоляция, миграция,
DAO/UI/lifecycle/restart/backup уборки и стабильная подпись TEST в эмуляторе.
Команды и фактические результаты будут добавлены после CI.

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
После CI проверить mergeable и передать владельцу; самостоятельно не объединять.
