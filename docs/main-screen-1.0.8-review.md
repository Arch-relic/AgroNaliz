# АгроНализ 1.0.8: окончательная компоновка главного экрана

17 октября 2026. Работа завершена в существующей ветке `feature/harvest-1-0-8`.
[PR #33](https://github.com/Arch-relic/AgroNaliz/pull/33) остаётся открытым Draft, без merge.
Опорный main: `4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee`.
Head до задачи: `5c1c74b9299f13537369da6180747140063f7ab5`.

Проверенный код: `51570722d4b909ae3fa203eccb2678f89e1c6392`.
Локальный эквивалент: `20811d4`, одинаковый tree
`3013b840f499c7cdecc3bae76ae516bd19d51ad7`. SHA различаются из-за публикации через API.
Следующий коммит содержит только этот отчёт и handoff; он не выдаётся за проверенный
Android-коммит. Автоматический CI документационного коммита — отдельный запуск.

## Результат

В самый низ «Опций» целиком перенесены разделитель, заголовок «Для разработчика»,
динамический номер версии с прежней TEST-пометкой, «Справочник техники» и
«Резервное копирование». Сохранены переходы в DeveloperCatalogActivity и
BackupRestoreActivity. Прежняя кнопка «Данные и резервные копии» по-прежнему
открывает DataBackupActivity. Реализация резервного копирования не менялась.

На Main остались техника, поля, рабочие записи, опции и условное продолжение уборки.
Прежний отступ перед «Опциями» сохранён: Space 56dp и marginTop 16dp.
«ПРОДОЛЖИТЬ УБОРКУ» — последний элемент после гибкого свободного пространства:
ширина прежняя, minHeight 96dp, wrap_content для крупного текста, центрирование,
нижний padding 24dp. При GONE нет отдельной обёртки фиксированной высоты.

Оба экрана используют ScrollView/fillViewport: свободное место сжимается, при
нехватке высоты появляется прокрутка. Системные отступы применены через
fitsSystemWindows к внешнему FrameLayout, вне области прокрутки.
Логика refreshHarvest/openHarvest/quickAccessGeneration, Room23, DAO, миграции,
backup, подписи, applicationId, версии, MAIN/TEST и workflows не менялись.

## Обнаруженная проблема и исправление

[Android #98](https://github.com/Arch-relic/AgroNaliz/actions/runs/39015587271)
успешно собрал приложение и прошёл новые проверки навигации и геометрии, но
compact/font1.6 упал на scrollTo(buttonWorkNotes): элемент не стал достаточно видимым.
[Диагностический #99](https://github.com/Arch-relic/AgroNaliz/actions/runs/39016636938)
воспроизвёл тот же сбой. Вывод геометрии через println не попал в Actions console;
координаты не используются как доказательство.

В исходном native ScrollView расчёт computeScrollDeltaToGetChildRectOnScreen
использует всю высоту, не вычитая padding. Системные inset на самом ScrollView
создавали такую внутреннюю область. Исправление: вынести fitsSystemWindows на
внешнюю рамку обоих экранов. Тот же строгий compact-тест после изменения прошёл.
Проверки видимости, полноты текста и геометрии не ослаблены, тайм-ауты не увеличены.

## Проверки последнего кода

- [Android PR checks #100 — SUCCESS](https://github.com/Arch-relic/AgroNaliz/actions/runs/39016802399).
  Собраны debug/TEST и обе instrumentation APK; `testDebugUnitTest` и
  `testDebugTestUnitTest` выполнены заново, не UP-TO-DATE. Стандартный Gradle-лог
  не печатает число JVM-тестов; отчёты находятся в diagnostics.
  Python: 13 catalog + 6 version + 27 TEST tools = 46 успешно, без пропусков.
  Инструментальные: 84 успешных выполнения в 45 вызовах, ошибок нет.
- MainScreenLayoutUiTest: 3/3 — перенесённая навигация, нижняя геометрия/GONE,
  compact 720×960 со шрифтом 160%, прокрутка и отсутствие обрезанного текста.
  Геометрия проверяет ширину, высоту 1,8–2,2 обычной кнопки, центрирование,
  сохранённый отступ перед Options и нижние 24dp.
- HarvestQuickAccessUiTest: 2/2 — 0/1/несколько кампаний, выбор и переходы,
  возврат/перезапуск и скрытие после завершения последней кампании.
- Harvest108Test: 5/5; HarvestFullBackup108Test: 4/4.
- HarvestRoom23MigrationTest: 3 успешных вызова — seed22, verify23,
  verify23 после force-stop/перезапуска. Исторические NULL сохранены.
- Безопасное обновление TEST: тот же синтетический ключ и больший versionCode
  сохраняют Room/preferences/files; другой ключ отвергнут без потери данных;
  read-only JSON export и синтетическое восстановление успешны, MAIN не изменён.
- AppVersionUiTest теперь проверяет OptionsActivity: динамическую версию,
  TEST-маркер, центрирование 12sp и доступность двух developer-кнопок. Все прежние
  запуски MAIN/TEST/TEST после обновления сохранены и прошли.
- [Main APK safe update checks #27 — SUCCESS](https://github.com/Arch-relic/AgroNaliz/actions/runs/39016802395).
  Сборка и JVM-задача выполнены, 26 Python и 3 инструментальные проверки успешны.
  MAIN — только синтетическая APK в ранее разрешённом одноразовом CI-эмуляторе.
- Итого два последних CI: 72 успешных Python и 87 инструментальных выполнений.
- Локально `python3 -m unittest discover -s docs -p 'test_*py' -q`:
  72 теста, 70 успешно, 2 пропущены без APK. XML parse и `git diff --check` успешны.
- Локальная проектная команда `source /workspace/agronaliz-env.sh`, затем
  `agrogradle :app:assembleDebug :app:test` остановилась до компиляции:
  FileLockContentionHandler / `Could not determine a usable wildcard IP for this machine`.
  Локальная Android-сборка не считается успешной; Android проверен в Actions.

Runner изменяет размер/шрифт только в одноразовом CI-эмуляторе и восстанавливает их
в finally. Новые UI-тесты требуют debugtest + ranchu/goldfish + отсутствие активных
кампаний; After удаляет только собственные фикстуры. Пользовательская база не используется.

## Изменённые файлы относительно начала задачи

- `app/src/main/java/ru/fo6osik/workjournal/MainActivity.kt`
- `app/src/main/java/ru/fo6osik/workjournal/OptionsActivity.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_options.xml`
- `app/src/androidTest/java/ru/fo6osik/workjournal/AppVersionUiTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/MainScreenLayoutUiTest.kt` — новый
- `docs/run-harvest-108-checks.py`
- `docs/test_app_version.py`
- `docs/main-screen-1.0.8-review.md` — новый
- `docs/codex-handoff.md`

Удалённых файлов нет. Подписи, id, защищённые workflows и пользовательские данные
не затронуты. Релиз, merge, установка на смартфон не выполнялись.

## Ограничения и ручная проверка TEST

Физический телефон, специфические OEM-режимы и экстремальные масштабы за пределами
синтетического compact/font1.6 остаются непроверенными. Риск для данных от этой
корректировки не выявлен: меняются UI, маршруты двух кнопок перенесены без изменения
целевых Activity. Предыдущий опубликованный TEST APK не содержит эту корректировку;
в рамках задачи новая устанавливаемая APK не публиковалась.

При последующей согласованной подготовке TEST со совместимой подписью проверить:
1. Без активной кампании Main содержит только четыре обычные кнопки; уборка скрыта.
2. При активной кампании уборка внизу, примерно вдвое выше и той же ширины;
   перед Options сохранён прежний большой отступ.
3. В Options нижний блок содержит заголовок, версию TEST и обе работающие кнопки;
   прежние «Данные и резервные копии» тоже доступны.
4. При увеличенном шрифте всё прокручивается и не перекрывается системной навигацией.
5. Продолжение уборки открывает день/кампанию или выбор при нескольких кампаниях.

Не удалять приложение и не очищать данные. Работа остановлена для независимого review.
