# Google Drive: Android интеграция

Дата: 11.10.2026. Ветка `feature/google-drive-auth-stage2`, Draft PR [#7](https://github.com/Arch-relic/AgroNaliz/pull/7).
Реальное подключение требует настройки Google Cloud и проверки на телефоне владельца.

## Авторизация и аудит

Google Play services `play-services-auth:21.6.0`, нативный AuthorizationClient и Android Google Picker через resource parameters. AccountPicker выбирает аккаунт, закрепляемый в каждом запросе. Единственный scope — `https://www.googleapis.com/auth/drive.file`; лишние scopes отвергаются. Нет files.list, широких scopes, SAF подмены или произвольных URL. PDF и индекс выбираются отдельно, по одному файлу.

Проверенные официальные источники:
- https://developer.android.com/identity/authorization
- https://developers.google.com/workspace/drive/picker/guides/desktop-mobile-picker
- https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationRequest.Builder

Версия SDK закреплена для сохранения minSdk 23; 22.0.0 требует API 24 и была отвергнута manifest merger. minSdk проекта не повышен, overrideLibrary не используется.

Неиспользованные прототипы собственного PKCE/callback/token exchange/refresh storage удалены. Custom-scheme redirect Android OAuth не подключается. Обмен и refresh credentials принадлежат Google Play services: приложение не получает refresh token, не хранит client secret. Это замена архитектуры, а не утверждение о проверке собственного PKCE.

Access token только в памяти, повторное обращение к SDK через пять минут, 401 инвалидирует токен и повторяет HTTP максимум один раз. Обновление/инвалидация/выход сериализованы; старый 401 не удаляет новый токен. Восстановление использует выбранный аккаунт и SDK; интерактивное разрешение только по действию пользователя. PendingIntent callback принимается один раз для текущей попытки с TTL пять минут на monotonic clock. Отмена, replay, обратный ход часов, потеря состояния после process death отвергаются. ViewModel сохраняет попытку при повороте, токены не попадают в SavedState/backup.

Аккаунт/IDs/версии сохраняются в noBackupFilesDir/google_drive_bindings.enc, AES-256-GCM, Android Keystore, AAD, случайный IV. Запись ciphertext: temporary + fsync + rename, единая блокировка экземпляров. Токенов там нет. Потеря ключа не создаёт новый ключ при чтении и не перезаписывает данные. Явный сброс удаляет только настройки/ключ, оставляет PDF/индексы. Отключение сначала сохраняет локальный запрет, затем revoke. Ошибка оставляет persistent pending-revoke: повторить онлайн или отозвать вручную; смена аккаунта требует успешного отключения старого. При потерянном Keystore отозвать разрешение вручную в Google Account.

## Транспорт и доверие

HTTP client, token backend, transport и DocumentSource разделены. Только HTTPS www.googleapis.com/drive/v3/files/{selectedId}; redirects выключены, connect/read 15/30 сек, SDK ожидание 30 сек. Проверяются ID, MIME, size, version, trashed, canDownload. Metadata до 128 KiB, PDF до 256 MiB, индекс до 64 MiB. Потоки проверяют фактический размер, закрывают response/connection. ETag используется как If-Match, 412 требует повторной синхронизации. 401/403/404/сетевая ошибка различаются; тела ошибок и токены не отображаются/не логируются. До скачивания проверяется место; ошибка записи сохраняет прежнее поколение. Отмена coroutine проверяется между чтениями; текущий блокирующий SDK/read завершается по ограниченному timeout, а не мгновенно.

Доверие — обе SHA-256 bundled catalog для 20 документов. Arbitrary PDF и новые байты с другими суммами отвергаются. Drive version/ETag — признаки изменения отдельных файлов, не ревизия пары и не доверенный manifest. revision не выдумывается. Новые документы/релизы требуют обновления доверенного каталога или отдельного подписанного manifest. Upload/create в текущем сценарии чтения через Picker не реализованы.

LocalDocumentStore потоково вычисляет обе SHA-256, вызывает существующий AndroidDocumentIntegrityValidator (PdfRenderer, формат/диапазоны/координаты индекса), атомарно активирует комплект. Явная синхронизация forceRefresh=true проверяет источник даже при совпавшем кеше. Старые поколения сохраняются, автоматического удаления существующих PDF/индексов нет. Возможное накопление поколений требует explicit prune в следующем этапе.

SHA-256 **не доказывает соответствие текста и координат PDF индексу**. Оригинальные PDF отсутствуют в checkout; автоматикой проверяются catalog/index hashes и фейковые пары. PdfRenderer проверяет первую страницу и диапазоны, не извлекает текст и не рендерит весь PDF. Нужна ручная проверка реальных 20 пар.

## Opt-in интерфейс

Опции → «Технические документы: Google Drive»: подключение, отключение/повтор отзыва, выбор документа/PDF/индекса, статус и последнее обновление, проверка версий, ручная синхронизация, открытие офлайн, отмена текущей операции, сброс повреждённых настроек. Автоматической фоновой синхронизации нет. Сеть, файловый IO, SHA выполняются на Dispatchers.IO; синхронные open/read/close источника запрещают главный поток.

Factory.create остаётся assets-only. createWithGoogleDrive — opt-in; runtime catalog добавляет приватные привязки, bundled catalog не изменяется. Экран явно загружает через repository. Офлайн открытие использует проверенную локальную пару без авторизации/сети, в том числе после отключения или потери ключа. Viewer сохраняет lease, поиск использует индекс того же поколения. Исходный assets-путь, конфигурация PDFView, поиск, подсветка, масштабирование и навигация сохраняются. Room/БД и существующие assets не меняются.

## Настройка владельцем

1. Google Cloud: включить Google Drive API и Google Picker API, настроить Google Auth Platform/consent screen (контакты, audience, test users при Testing), scope только drive.file. Проверить требования публикации/политики конкретной организации.
2. Создать OAuth Android client: package ru.fo6osik.workjournal, SHA-1 фактического сертификата APK. Отдельно зарегистрировать debug/release/Play App Signing при их использовании. Получить SHA-1/SHA-256 локально через ./gradlew :app:signingReport или keytool -list -v -keystore ...; не публиковать keystore/пароли. Сверить подпись установленного APK. Временная подпись CI не является production-подписью.
3. Native Android API сопоставляет package/сертификат с Android client; client ID в код не встраивается. Не нужны Web client secret, custom redirect, JS Picker key, google-services.json для этого flow. Проверить поддерживающую native Picker версию Google Play services на реальном телефоне. Неправильная подпись, отсутствие GMS или политики организации могут блокировать доступ; не обходить широкими scopes.
4. Подготовить реальные PDF и неизменённые v4 JSON, соответствующие двум суммам каталога. Поместить их в Drive самостоятельно, без PDF в Git. Выбрать правильную пару в приложении; название не заменяет проверку SHA.
5. Проверить разрешение и отзыв в Google Account → Security → Third-party connections. Pending revoke повторить онлайн; после потери ключа отозвать вручную.

## Ручная приёмка

- Debug/release авторизация, отмена chooser/consent/Picker, rotation/process death, повтор callback.
- Правильная и неправильная пара, удаление/trashed/403/отзыв, разные версии, дубли IDs.
- Перезапуск/expiry, offline, отключение/смена аккаунта, revoke offline → online retry, офлайн открытие после отключения.
- Большой PDF, обрыв/отмена/изменение во время скачивания, мало места; UI отзывчив, прежняя копия открывается.
- Реальные 20 пар: все страницы, текст поиска/координаты/подсветка/zoom/поворот, прежний assets-путь и локальный путь, обновление при открытом viewer.
- Потеря/повреждение Keystore, reset/reconnect; существующие документы и Room сохранены.

Конкретные результаты и SHA: [codex-handoff.md](codex-handoff.md).
