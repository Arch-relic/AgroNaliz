# Передача Google Drive stage2

Дата: 11.10.2026. Цель: полная opt-in Google Drive интеграция по PR #7.
Статус: реализация и unit tests подготовлены; выполняется Android CI. Реальное подключение требует настроек владельца.

## Архитектура

Native AuthorizationClient/Google Picker, drive.file, без custom redirect и refresh token в приложении. Encrypted Keystore bindings, сериализация токенов, HTTP allowlist выбранных IDs, trusted catalog SHA-256, атомарный кеш, офлайн viewer. Подробности: google-drive-stage2.md. Room и assets сохранены.

## Ветка/коммиты/PR

feature/google-drive-auth-stage2; main base 2c0f6cb21acae3b60a25f45ecef447a67df25b4e.
Предыдущий PR head 0aba648e3e5301ab8c05a5da4e9950b9e7177c9c.
Изменения пока не проверены Android сборкой; окончательный SHA/перечень файлов будет записан после CI отдельным отчётным коммитом.
Draft PR https://github.com/Arch-relic/AgroNaliz/pull/7 открыт, не объединён.

## Проверки

Новый запуск python3 -m unittest discover -s docs -p 'test_*.py': 13 tests, 0 failures/errors.
source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:testDebugUnitTest: Gradle не стартовал, FileLockContentionHandler / Could not determine a usable wildcard IP for this machine. Это ограничение локальной sandbox, не ошибка компиляции. Android проверка выполняется в GitHub Actions проектными Kotlin/AGP.

## Риски и следующие действия

Нужны Android OAuth client package+сертификат, Google Drive API/consent/test users, GMS с native Picker и реальный аккаунт. Настройки/чеклист: google-drive-stage2.md. PDF отсутствуют в checkout; семантическое соответствие PDF/индекса не доказано. Поддержаны только pinned пары 20 документов, не произвольные PDF. Реальные runtime сценарии и подписи необходимо проверить на телефоне.
