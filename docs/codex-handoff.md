# Передача контекста: Google Drive stage2

Дата: 11.10.2026. Проект Arch-relic/AgroNaliz.
Цель: завершить полную opt-in интеграцию Google Drive по ТЗ Draft PR #7.
Статус: реализация завершена, Android сборка и автоматические тесты зелёные.
Реальная Google авторизация/Picker/PDF на смартфоне требуют настроек и ручной приёмки владельцем; они не объявлены проверенными.

## Выполненные задачи и архитектура

- Нативные AuthorizationClient/AccountPicker/Google Picker; только drive.file, opt-out прежних scopes, никаких files.list/широких scopes/custom redirect/client secret.
- Аудит заготовки: собственный PKCE/callback/token exchange/refresh storage заменены поддерживаемым Android SDK. Приложение не получает refresh token; refresh credentials управляются Google Play services. Собственный PKCE не объявляется протестированным.
- Access token только в памяти, SDK восстановление/обновление, один retry 401, сериализация refresh/invalidation/revoke; callback одноразовый, TTL, отдельный requestCode, rotation через ViewModel, process death отвергает старую попытку.
- Приватные account/file bindings: Android Keystore AES-256-GCM, noBackupFilesDir, AAD, ciphertext fsync+rename, защита от потери ключа и конкурентных записей. Persistent pending-revoke позволяет повторить отзыв после offline-ошибки; смена аккаунта требует отключения старого.
- Отдельные HTTP client/backend/transport/source, fixed HTTPS origin, запрет redirects, timeouts, allowlist выбранных IDs, MIME/size/version/ETag/trashed/canDownload, ограниченные metadata/media и закрытие потоков. Все сетевые операции, SHA и файловый IO на Dispatchers.IO.
- Доверие только к обеим SHA-256 bundled catalog для 20 пар. Force refresh не позволяет подменить проверку нового облачного файла совпавшим кешем. Ревизии не выдуманы. Атомарная активация и прежняя копия при ошибке сохранены. Отдельная короткая lease-блокировка исключает ожидание длительного SHA при закрытии viewer.
- Экран «Опции → Технические документы: Google Drive»: connect/disconnect, выбор PDF/индекса, статус/последний update, проверка версий, ручная синхронизация/отмена, offline-open, reset повреждённых настроек. SDK создаётся лениво, offline-open не зависит от авторизации/привязок.
- Factory.create остаётся Assets; createWithGoogleDrive opt-in. Viewer.fromFile использует проверенный lease и индекс того же поколения; прежние fromAsset/поиск/подсветка/zoom/navigation сохранены. HarvestDayActivity изменена только сигнатура onNewIntent для AndroidX.
- Room/версия БД, documentId/revision/hash/sources/пути bundled catalog, существующие PDF/JSON не изменены и не перемещены. PDF, ключи, токены и реальные приватные привязки в Git не добавлены.

## Файлы

Созданы относительно main:

- `app/src/main/java/ru/fo6osik/workjournal/GoogleDriveSettingsActivity.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveCredentialStore.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveHttpTransport.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveNativeAuthorization.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveSelection.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveServices.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveTokenSession.kt`
- `app/src/test/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveStage2Test.kt`
- `docs/google-drive-stage2.md`

Изменены относительно main:

- `.github/workflows/android-pr-checks.yml`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestDayActivity.kt`
- `app/src/main/java/ru/fo6osik/workjournal/OptionsActivity.kt`
- `app/src/main/java/ru/fo6osik/workjournal/PdfViewerActivity.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/DefaultDocumentRepository.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/DocumentRepository.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/DocumentRepositoryFactory.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/LocalDocumentStore.kt`
- `app/src/main/res/layout/activity_options.xml`
- `docs/codex-handoff.md`
- `docs/document-repository.md`
- `docs/document-storage.md`
- `docs/run-document-jvm-tests.py`

Удалений относительно main нет. При аудите исходного PR head 0aba648e3e5301ab8c05a5da4e9950b9e7177c9c удалены неиспользуемые прототипы и их тесты (они никогда не находились в main):

- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveHttpTokenExchange.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveKeystoreRefreshStorage.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveOAuthAttempt.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveOAuthCallback.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveOAuthPkce.kt`
- `app/src/main/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveTokenExchange.kt`
- `app/src/test/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveOAuthAttemptTest.kt`
- `app/src/test/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveOAuthCallbackTest.kt`
- `app/src/test/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveOAuthPkceTest.kt`
- `app/src/test/java/ru/fo6osik/workjournal/documents/sources/GoogleDriveTokenSessionTest.kt`

## Ветка, коммиты и публикация

- Ветка: `feature/google-drive-auth-stage2`.
- Опорный main: `2c0f6cb21acae3b60a25f45ecef447a67df25b4e` (merge PR #6).
- Исходный PR head для аудита: `0aba648e3e5301ab8c05a5da4e9950b9e7177c9c`.
- Реально проверенный итоговый код: `bdb085f614c27df37a5184ffa63f6c18bb6086cb`.
- Его Git tree: `781dc350dda34399401893bd3cb700a93399710c`, совпадает с локальным снимком. Локальный mirror-коммит `a490243` имеет другой parent/history; не подменяет опубликованный проверенный SHA.
- Shell git fetch/push блокируется proxy среды; публикация выполнена GitHub API коммитами поверх исходного PR head с expected_sha lease, без force и без изменения main. Деревья локального/удалённого снимка сверены.
- Этот отчёт сохраняется отдельным следующим коммитом документации. Указанный SHA относится к проверенному коду, а не к самому содержащему отчёт коммиту. Последний SHA документации доступен в head PR.
- Незакоммиченных изменений кода после проверки нет; следующий коммит содержит только этот отчёт.

## Проверки и результаты

Проверенный код: [GitHub Actions run #25](https://github.com/Arch-relic/AgroNaliz/actions/runs/38118081525), success.

- `./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon --stacktrace --console=plain`: BUILD SUCCESSFUL, 2m 5s. APK собран. compileDebugKotlin/packageDebug/testDebugUnitTest действительно выполнены; это новый запуск, не UP-TO-DATE.
- JUnit: **96 tests, 0 failures, 0 errors, 0 skipped**, включая 33 новых stage2 теста. Фейковые SDK backend/HTTP transport, expiry/revoked/account change/concurrent refresh/stale 401, callback TTL/replay/old request, 403/404/412/503/network, malformed metadata/size/MIME/bounds, closed/truncated/oversized downloads, catalog trust/force refresh/offline preservation, AES-GCM/tamper/key loss/atomic concurrency, lease release при параллельной загрузке.
- `python3 -m unittest discover -s docs -p 'test_validate_document_catalog.py' -v`: **13 tests, 0 failures/errors**, новый запуск в CI. Также локально повторно успешно выполнен.
- `PYTHONDONTWRITEBYTECODE=1 python3 docs/validate-document-catalog.py`: 20 точных сопоставлений PDF checksum/path, 20 index hashes, 1535 страниц индексов, 309111 слов; PDF bytes checked **0/20**, compatibility NOT established.
- `git diff --check`: успешно. Изменения assets/Room отсутствуют в PR diff.
- Использованы проектные Kotlin 2.2.20, AGP 8.12.0, Gradle 8.13, compileSdk 36, minSdk 23, JVM 1.8; настройки проекта не повышены. Auth SDK закреплён 21.6.0 для API 23.
- Diagnostics artifact: `android-pr7-diagnostics`, run #25, ID `11694116876` (JUnit XML/HTML и APK output metadata, срок хранения семь дней).

Обнаруженные и исправленные ошибки CI:
1. Run #22: Auth 22.0.0 требует minSdk 24 → заменён на 21.6.0, без overrideLibrary/повышения minSdk.
2. Run #23: AndroidX non-null onNewIntent contract → минимальное исправление сигнатуры HarvestDayActivity.
3. Run #24: APK успешно собран, JUnit initializationError из-за возврата Object из coroutine-теста → explicit runBlocking<Unit>. Эти неуспешные запуски не считаются зелёными; итоговый run #25 проверил все 96 тестов.

## Ограничения среды, риски и непроверенное

- Предписанная команда `source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:testDebugUnitTest` в локальной sandbox не стартовала: FileLockContentionHandler / Could not determine a usable wildcard IP for this machine. Это ограничение сокетов среды до компиляции; полноценная проверка выполнена в GitHub Actions.
- `source /workspace/agronaliz-env.sh; python3 docs/run-document-jvm-tests.py` не выполнена до тестов: отсутствует cached org.json 20240303. Fallback не заменяет Android CI и не проверяет native SDK/UI.
- Реальные Google Cloud consent, Android client/подпись, Google Play services/native Picker, account revoke/restore, большие PDF, нехватка места и UI на устройстве не проверены. Совместимость сборки с API 23 не гарантирует наличие Picker в GMS конкретного старого телефона.
- Оригинальных PDF в checkout нет. SHA-256 не доказывает текст/координаты PDF ↔ индекс. Android integrity validator не извлекает текст и проверяет рендер первой страницы, не всех страниц. Unit storage fixtures используют fake validator; это не Android instrumentation проверка.
- Только известные 20 пары; новые PDF/релизы с другими байтами требуют доверенного catalog update либо отдельного signed manifest. Version/ETag не превращаются в ревизию комплекта. Upload/create/scheduling не реализованы для текущего read/Picker сценария.
- Отмена блокирующего SDK/read ограничена timeout, не мгновенная. При незавершённом revoke/потере Keystore требуется повтор онлайн или ручной отзыв через Google Account.
- Старые поколения не удаляются автоматически; долгие многократные sync могут расходовать место. Existing explicit prune защищает active/current leases.
- Atomic rename рассчитан на private filesystem одного процесса; отдельная многопроцессная/потеря питания fsync-dir проверка не проведена.

## Следующий этап: обязательные ручные настройки и приёмка

Полная инструкция и чеклист: [google-drive-stage2.md](google-drive-stage2.md).

1. Включить Google Drive API и Google Picker API; consent/audience/test users, только drive.file.
2. Android OAuth client для `ru.fo6osik.workjournal` и фактической SHA-1 подписи debug/release/Play App Signing; локально проверить SHA-1/SHA-256 через signingReport/keytool. Не добавлять Web secret/custom redirect или временную CI подпись как production.
3. Проверить GMS/native Picker на телефоне; выбрать реальные PDF/JSON с обеими pinned суммами.
4. Пройти отмену/rotation/process death, expiry/reconnect/revoke/account change, offline-open после отключения/перезапуска, обрыв/конфликт/мало места/потерю ключа.
5. Проверить реальные 20 пар: все страницы, поиск/текст/координаты/подсветка/zoom и существующие экраны техники/рабочих записей. Android instrumentation/UI acceptance остаются ручными.
6. Для новых документов отдельно спроектировать доверенный manifest/обновление каталога; не расширять scopes для обхода Picker.

## Pull Request

[Draft PR #7](https://github.com/Arch-relic/AgroNaliz/pull/7) → main, открыт, не объединён. Main не изменён. Отчёт следует читать из head feature-ветки, не из main. Слияние не выполнялось.
