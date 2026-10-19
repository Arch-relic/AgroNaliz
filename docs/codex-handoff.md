# АгроНализ 1.0.9 — этап5: история изменений

Дата:19.10.2026. Ветка feature/harvest-1-0-9-changelog.
База main113d5c0e01b7d436ac119ebc752d9039ad03a35a, содержит объединённые PR35–38.
Локальный304be2b имеет то же дерево52a117b6bf6665b8f96a196a502e84134520c0b5.
Git fetch невозможен: proxy port8080 unavailable; API compare подтвердил отсутствие
файловых различий с main, публикация через GitHub API с parent актуальной main.
Статус: реализация подготовлена; Draft PR и реальные результаты CI будут записаны
следующим отчётным коммитом. Не объявлять ещё не выполненный CI успешным.

## Реализация

Кнопка «История изменений» в существующем блоке «Для разработчика» OptionsActivity.
Отдельный неэкспортируемый ChangelogActivity, стандартные ScrollView/LinearLayout/
Button/TextView, без новых зависимостей. Сам экран не использует сеть, Room,
файлы или preferences. У приложения нет INTERNET permission; UI-тест проверяет
это и сравнивает содержимое всех таблиц до/после просмотра и пересоздания Activity.

AppChangelog — локальная Kotlin-модель, все записи встроены. Новые сверху:
1.0.9 «В разработке»,1.0.8,1.0.7, «Ранние этапы разработки». Даты неизвестны,
поэтому null и не выводятся. Источники и инструкция расширения:
docs/changelog-history-sources.md. Даты merge не выдаются за даты выпуска.
Текущая версия только BuildConfig.VERSION_NAME (по-прежнему1.0.8), не Room24.
Автоматически раскрывается совпадающая текущая версия; будущая1.0.9 свёрнута.
Блоки независимы, состояние включая полностью свёрнутый экран сохраняется в
Bundle; неизвестные восстановленные id отбрасываются. Только непустые категории.
ScrollView, многострочные кнопки без фиксированной высоты, sp поддерживают шрифт.

## Файлы (относительно корня репозитория)

- .github/workflows/android-pr-checks.yml — добавить только каталог PNG в diagnostics.
- app/src/main/AndroidManifest.xml
- app/src/main/java/ru/fo6osik/workjournal/OptionsActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/AppChangelog.kt (новый)
- app/src/main/java/ru/fo6osik/workjournal/ChangelogActivity.kt (новый)
- app/src/main/res/layout/activity_options.xml
- app/src/main/res/values/ids.xml (новый)
- app/src/test/java/ru/fo6osik/workjournal/AppChangelogTest.kt (новый)
- app/src/androidTest/java/ru/fo6osik/workjournal/ChangelogUiTest.kt (новый)
- docs/run-harvest-108-checks.py
- docs/changelog-history-sources.md (новый)
- docs/codex-handoff.md

Удалённых файлов нет. Room24/миграции/backup/расчёты/таймеры/ползунки/версии/
applicationId/подписи и защищённый main-apk-publish.yml не менялись.
APK upload PR workflow уже запрещён при pull_request, это правило не менялось.

## Проверки

Локально python3 -m unittest discover -s docs -p 'test_*.py':85,83pass,2SDKskip.
Вложенные24 sandbox tests не считать дополнительными уникальными (23pass/1skip).
Diff whitespace clean. Требуемая source /workspace/agronaliz-env.sh;
agrogradle :app:assembleDebug :app:test не дошла до компиляции:
FileLockContentionHandler / Could not determine a usable wildcard IP for this machine.
Конфигурация не изменялась ради среды.

Добавлено4 JVM-теста: порядок/ключи, null даты/независимость версии от Room,
status/categories, независимое раскрытие/восстановление/пустое состояние.
Добавлено3 инструментальных сценария: навигация/состояние, offline+неизменность
всех таблиц, компактный экран/font1.6+скриншот. Они включены в существующий CI
runner, прежние проверки не отключены. Screenshot создаётся только на временном
TEST эмуляторе, извлекается в diagnostics до удаления синтетической установки.
Фактические сборки/JVM/UI/скриншот: ожидание CI.

## Ограничения и следующий этап

История1.0.7/ранних этапов неполная, даты не установлены. Записи редактируются
в исходниках и не обновляются автоматически. Ручные устройства не проверялись.
Никаких APK publish/Release/production signing/merge/изменений реальных данных.
После CI и отчёта остановиться для review. MAIN Room24 production policy не
расширена и требует отдельного решения, как и до этого этапа.
