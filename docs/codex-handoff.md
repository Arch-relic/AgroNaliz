# Передача контекста: версия 1.0.7

Дата: 16 октября 2026. Реализация завершена, Draft PR создан; исходный код и все проверки прошли CI. Цель: единый MAJOR.MINOR.PATCH и строка версии главного экрана.

Ветка feature/app-version-1-0-7, опорный main
160566ba888938bb7dd5065666a3cbe770d7b174 (merge PR31 с TimePicker).
Локальный worktree создан из файлово идентичного зеркала; коммиты GitHub
основаны на указанном настоящем main. Draft PR #32: https://github.com/Arch-relic/AgroNaliz/pull/32 (Draft, не объединён).
Первый опубликованный SHA: 513a0b586a4aa27208aa81f18a7298bf65754ef6.
Для него Main APK safe update checks 38852372791 прошёл; Android PR checks
38852372731 выявил ошибку нового UI-теста размера текста: XML округляет 12sp
до 32px, тест ошибочно ждал 31.5px. Исправлен только тест: TypedValue.applyDimension
и roundToInt, допуск 0.1px сохранён. Текст версии и центрирование уже прошли.
После исправления полный CI повторён успешно для коммита
**a6cf1c4ad739ed8313373ad6d4d74980b15eed3c** (оба запуска с первой попытки).
Никаких изменений приложения ради ошибки размера теста не сделано.
Этот итоговый отчёт сохраняется следующим коммитом: после проверенного a6cf1c4
меняется только документация, исходники и тесты остаются теми же.

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
  остаются обязательными и прошли в перечисленных ниже запусках.

Публикации APK и установки на пользовательский телефон нет. Никакие данные
пользователя не очищаются. Android CI работает исключительно с синтетическими
данными одноразового эмулятора, как и существующие проверки проекта.
Итоговые проверки для a6cf1c4:

- Android PR checks #91: https://github.com/Arch-relic/AgroNaliz/actions/runs/38853218550 — SUCCESS.
  assembleDebug, assembleDebugTest, обе instrumentation APK и обе JVM-задачи
  testDebugUnitTest/testDebugTestUnitTest прошли. JVM-задачи выполнены заново
  (не UP-TO-DATE); число JVM-тестов стандартный лог Gradle не выводит, XML есть
  в diagnostics. 13 проверок каталога, 5 новых проверок версий и 25 проверок
  инструментов TEST прошли, пропусков/ошибок нет.
  63 инструментальных выполнения: три запуска AppVersionUiTest (main, TEST,
  TEST после install-r); все прежние 60 выполнений сохранены, включая обе
  серии HarvestAreaUiTest по 7 тестов и 5 HarvestFuelUiTest.
  Проверены Room20 →22, Room21 →22, старые/новые backup, события, таймеры,
  площадь/плотность/влажность/топливо, перезапуск, изоляция пользовательских
  пространств, отказ Android от другого TEST-ключа с сохранением данных.
- Main APK safe update checks #19: https://github.com/Arch-relic/AgroNaliz/actions/runs/38853218661 — SUCCESS.
  18+8 Python-проверок, три Android-проверки обновления без очистки. Начальная
  синтетическая APK versionCode53/Room20 обновлена до текущей Room22 и затем
  повторно с повышенным code, база/настройки/файлы/сертификат сохранены.
  Старое имя шага и итоговая строка Room21 в этом workflow не менялись;
  фактическая текущая версия 22 подтверждается тестами и AppDatabase.
- Реальные APK: main ru.fo6osik.workjournal, versionName1.0.7, default debug
  versionCode1; TEST ru.fo6osik.workjournal.debugtest, versionName1.0.7,
  preview code1000091. При TEST-обновлении code1000092 строго выше1000091.
  Code1 не является номером опубликованного основного обновления: он не
  менялся; защищённый выпуск по-прежнему задаёт mainVersionCode=3000000+run.
- Runtime AppVersionUiTest подтвердил Room22 во всех трёх запусках. Diff с main
  не содержит изменений моделей, DAO, AppDatabase, миграций или backup-кода.
- Upload debug APK = SKIPPED: основная APK не опубликована. Workflow выпуска
  main/TEST не запускались, production signing secrets не использовались.

Проверенные исходники и тесты закоммичены; незакоммиченной разработки нет.
Непроверенное: реальный смартфон, нестандартный масштаб шрифта и разные размеры
экранов. Ручная проверка остаётся владельцу; числовой код установленного TEST
и реальные данные телефона здесь не запрашивались и не менялись.

Следующий этап: владелец проверяет Draft PR; только после своего решения о
слиянии запускает существующий защищённый test-apk-publish.yml на main, указывает
фактический установленный TEST versionCode/Room и проверяет обновление с прежним
сертификатом на смартфоне. На главном экране должно быть «Версия 1.0.7 · TEST»;
только затем отдельно рассматривается обновление основной версии.
Не выполнять merge и не запускать выпуск основной APK.
