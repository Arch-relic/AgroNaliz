# Передача результатов Codex — «АгроНализ»

## Дата, этап, цель и статус

11 октября 2026 года. Публикация тестовой app-debug.apk через GitHub Actions.
Workflow реализован. GitHub Actions успешно выполнил сборку, тесты и публикацию.
ZIP скачан, APK и подпись проверены. Открыт Draft PR №21.
Приложение и интеграции не изменялись.

## Изменения и решения

Существующие Android-сборка, JVM-тесты, проверка каталога и always-публикация
диагностики сохранены. Добавлен push в main; workflow_dispatch использует
штатный выбор ветки GitHub без жёсткого checkout main. PR продолжает собирать
merge-коммит. После успешных проверок APK публикуется отдельным артефактом
app-debug-<безопасное имя ветки>-<12 символов фактического checkout SHA>.
Upload завершается ошибкой при отсутствии файла. Срок хранения APK 14 дней.
Ветка передаётся через env, не вставляется в исполняемый shell-код.

## Файлы

Изменены:
- .github/workflows/android-pr-checks.yml
- docs/codex-handoff.md

Создан:
- docs/debug-apk.md

Удалений нет. Android-код, Room, зависимости, PDF, индексы и данные не изменены.
Ключи, токены, PDF и APK в Git не добавлены. Новых облачных интеграций нет.

## Ветка и коммиты

Ветка feature/debug-apk-artifacts создана GitHub API непосредственно от актуального
main 2c0f6cb21acae3b60a25f45ecef447a67df25b4e. main не изменяется.
Коммит реализации workflow: 6634b407b0fd5071262bc901e646484543f35751.
Коммит с инструкцией: 67926191b8ebee33ee9608dd1d47597ea85eae6a.
Проверенный head PR: da3a597f9e1cb2dc338ecb994218b6c6dce527f2.
Фактически собранный PR merge SHA: 2c603a5476290bc4d860c3f072550283b4ec4801.
Этот отчёт сохраняется следующим коммитом документации; указанные SHA обозначают
реально проверенный результат, а не хеш самого содержащего отчёт коммита.
Все изменения опубликованы в отдельной ветке, незакоммиченных изменений проекта нет.
Предыдущая локальная рабочая ветка сохранена; устаревший локальный origin/main
не использован для создания опубликованной ветки.

## Проверки

GitHub Actions run 38127076784: SUCCESS, все шаги завершились успешно.
https://github.com/Arch-relic/AgroNaliz/actions/runs/38127076784
Штатные команды существующего workflow:
- ./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon --stacktrace --console=plain
- python3 -m unittest discover -s docs -p 'test_validate_document_catalog.py' -v

Gradle: BUILD SUCCESSFUL in 2m 19s. Debug-сборка и testDebugUnitTest выполнены
новым запуском, не UP-TO-DATE (вспомогательные preBuild/preDebugBuild/
generateDebugAssets/preDebugUnitTestBuild были UP-TO-DATE).
JUnit XML скачанного diagnostics artifact: 4 suites, 63 tests,
0 failures, 0 errors, 0 skipped. Каталог: Ran 13 tests, OK, новый запуск.
Java 17, API 36, штатные проектные Kotlin/AGP и Gradle Wrapper сохранены.
Локальная Android-сборка в этой задаче не запускалась: проверка выполнена
в GitHub Actions, а не объявлена успешной на основании прежних результатов.

Локальная статическая проверка через Python/PyYAML BaseLoader: YAML корректен,
каждый существовавший шаг сохранён (только название checkout обобщено),
проверены push/main, workflow_dispatch, success-only upload после тестов
и if-no-files-found:error. Доступного actionlint нет.
GitHub changed-files подтверждает только три перечисленных файла.

Artifact ID 11696774195:
app-debug-feature-debug-apk-artifacts-2c603a547629,
ZIP 26295413 байт, app-debug.apk 38096438 байт, срок до 25.10.2026.
ZIP скачан через GitHub artifact API; zipfile.testzip() успешен для ZIP и APK,
архив содержит ровно app-debug.apk, APK содержит AndroidManifest.xml/classes.dex.
SHA-256 ZIP совпал с GitHub digest:
c5a82462fdfd1ae6eebc5f6356132380ec038a40f90c575bb0be64126b36bee3.
SHA-256 самой APK:
26ad31d644724056c7f68aafa267f4473fd1c21bb83759db2c34e88aa8875194.
Команда apksigner verify app-debug.apk: exit 0. Выданы предупреждения об
элементах META-INF вне защиты JAR-подписи; APK не модифицировалась.
Существующий android-pr6-diagnostics также успешно опубликован и скачан.

## Ограничения и ручные действия

До слияния владельцем новые push/main настройки не действуют в main.
Проверка APK из PR доказывает путь сборки/публикации; ручной dispatch main с
новой версией workflow и push main потребуют слияния. Автоматического слияния нет.
Ручная установка на Android и работа экранов не проверяются облачной сборкой.
Debug-подпись может отличаться между runners: не удалять приложение с данными
при конфликте подписи, использовать тестовое устройство/профиль.
Не добавлены отсутствующие исходные PDF; проверка содержимого документов
не входит в этот этап. Инструкция: docs/debug-apk.md.

## Pull Request

[PR №21](https://github.com/Arch-relic/AgroNaliz/pull/21): Draft, открыт,
base main, head feature/debug-apk-artifacts, не объединён.
Отчёт и результаты добавлены в эту же ветку после создания PR.
Решение о слиянии принимает владелец; затем проверить ручной запуск из main
и первый автоматический push/main. Для скачивания на телефон следовать
инструкции docs/debug-apk.md; требуется вход в GitHub с доступом к репозиторию.
