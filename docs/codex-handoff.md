# Передача контекста: версия 1.0.7

Дата: 16 октября 2026. Реализация подготовлена, публикация Draft PR и Android CI
пока не завершены. Цель: единый MAJOR.MINOR.PATCH и строка версии главного экрана.

Ветка feature/app-version-1-0-7, опорный main
160566ba888938bb7dd5065666a3cbe770d7b174 (merge PR31 с TimePicker).
Локальный worktree создан из файлово идентичного зеркала; коммит GitHub будет
иметь настоящим родителем указанный main. Проверенный SHA и номер PR будут
записаны после фактических проверок; реализация готова к первому коммиту.

Общий versionName установлен 1.0.7. MainActivity отображает BuildConfig.VERSION_NAME;
суффикс TEST выбирается по BuildConfig.BUILD_TYPE == debugTest. Строка по центру,
12sp, между заголовком «Для разработчика» и кнопками. Номер в XML не дублируется.
Главный экран обёрнут ScrollView с fillViewport: сохраняется нижний блок на большом
экране и доступ к нему на небольшом. Существующие кнопки и обработчики сохранены.
Правила и восемь этапов 1.0.0–1.0.7 документированы отдельно.

versionCode не менялся: main выпуск 3000000+run_number, TEST выпуск
2000000+run_number, preview TEST 1000000+run_number; параметры Gradle и защиты
от равного/меньшего кода и старого запуска сохранены. Локальный defaultCode=1
не является опубликованным обновлением. Пакеты, ключи/сертификаты и защищённые
workflow выпуска не изменялись. Room остаётся v22; миграций в этой задаче нет.

Изменённые/новые файлы:
- app/build.gradle.kts
- app/src/main/java/ru/fo6osik/workjournal/MainActivity.kt
- app/src/main/res/layout/activity_main.xml
- app/src/main/res/values/strings.xml
- app/src/androidTest/java/ru/fo6osik/workjournal/AppVersionUiTest.kt (новый)
- .github/workflows/android-pr-checks.yml
- docs/run-build-isolation-checks.py
- docs/run-test-signing-checks.py
- docs/verify-build-variants.py
- docs/test_app_version.py (новый)
- docs/version-history.md (новый)
- docs/codex-handoff.md
Удалений нет. PDF-просмотрщик, модели, DAO, AppDatabase, миграции и backup-код
не изменялись. Проверить git diff перед отправкой.

Проверки:
- python3 -m unittest discover -s docs -p 'test_app_version.py' -v: 5/5 успешно.
  Выполняется настоящий публичный preflight main/TEST с поддельным HTTP:
  рост code, отказ равному/меньшему code и публикации старого запуска.
- python3 -m unittest discover -s docs -p 'test_*py' -q: 69 тестов,
  67 успешно, 2 пропущены без APK, ошибок нет.
- git diff --check: успешно; отдельный git diff --exit-code для AppDatabase,
  HarvestDay, HarvestFuel, HarvestDao, DatabaseBackupManager и двух защищённых
  publish workflow подтвердил отсутствие изменений.
- source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test:
  локальная среда остановила Gradle до компиляции: Could not determine a usable
  wildcard IP for this machine (FileLockContentionHandler). Не считать сборку
  успешной; требуется подтверждение GitHub Actions.
- AppVersionUiTest запускается на main и TEST в изолированном CI-эмуляторе,
  затем повторно после TEST install -r с тем же синтетическим ключом и более
  высоким code. Проверяет установленный versionName, точную подпись версии,
  TEST-маркер, центрирование, размер текста, прокрутку/доступность кнопок, Room22.
- verify-build-variants.py проверяет versionName и versionCode реальных APK.
- Все прежние проверки изоляции, миграций, топлива/уборки/TimePicker и подписи
  остаются обязательными; их результат для новой ветки пока неизвестен.

Публикации APK и установки на пользовательский телефон нет. Никакие данные
пользователя не очищаются. Android CI работает исключительно с синтетическими
данными одноразового эмулятора, как и существующие проверки проекта.
Следующий этап: завершить CI и Draft PR, записать реальные результаты. После
проверки владельцем отдельно выпустить TEST и проверить строку на смартфоне.
Не выполнять merge и не запускать выпуск основной APK.
