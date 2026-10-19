# АгроНализ 1.0.9 — этап 2: календарь рабочих дней

19 октября2026. Ветка feature/harvest-1-0-9-calendar, база main
130d18eb6e189a57d49d589f544df2dd0ccbd387, treeee2e2c4348cb8c65ef230cee3b7ba728d9d800db.
Main уже содержит объединённый пользователем PR35. Локальный исходныйa80fb66
имеет идентичное дерево, проверено GitHub API. Git fetch заблокирован proxy8080;
новый remote commit будет создан API с parent точного main, без старых feature commits.
До начала status clean. Создан DraftPR36:
https://github.com/Arch-relic/AgroNaliz/pull/36, не объединён, base130d18e.
Реально проверенный remote SHA65f0f202ee8c693703413a475ded0cc70f8bc2f1
(локальный110ffa7, tree715e48ff98b2614b458ce43d15ce6cfb6c03bd7f).
Первый коммит f1ee14f содержал реализацию, следующий уточнил только nullable
accessibility label в UI assertion; оба запуска прошли. Итоговый отчёт сохраняется
следующим docs-only коммитом; его собственный SHA не выдаётся за новый Android run.

## Архитектура и поведение

Изучены HarvestProcessActivity.renderDays/loadProcess, layout, HarvestDao /
HarvestDaySummary, HarvestDayActivity.EXTRA_DAY_ID и существующие UI runners.
Вместо вертикального списка TextView используется HarvestDayCalendarView.
HarvestDayCalendar — модель UI состояния/группировки, не расчётов кампании.
7столбцов, понедельник, русский месяц/год, только записанные даты интерактивны,
последний непустой месяц при первом входе, исходно без выбора. Зелёный — все
площади записей даты положительны/конечны, янтарный — иначе; рамка выбора
сохраняет цвет. Стрелки скрыты для одного месяца, переходят через непустые
месяцы/годы, смена месяца снимает выбор. Повторное нажатие даты снимает выбор.
Сводка: дата/бункеры/гектары/статус/время. Неизвестная площадь — прочерк и
неполнота; несколько записей группируются только в UI, площадь суммируется
лишь при полноте, доступны отдельные короткие ссылки. Overflow не становится
конечной площадью. Room unique(processId,workDate) остаётся как есть.

Activity перечитывает Room при onResume, сохраняет месяц/дату в Bundle,
отбрасывает недействительный выбор. Generation guards не допускают результатов
старой фоновой загрузки/навигации после ухода. Перед открытием дня отдельная
актуальная проверка Room dayId/processId/date/существования кампании; unavailable
не открывается, карточка обновляется. Переход использует прежний EXTRA_DAY_ID.
Создание/редактирование дней, расчёты PR35, Room23/миграции, versionName/code,
подписи/applicationId/сборка, таймер/события/уведомления не изменены. Другие этапы
(новая карточка результатов, ползунки, история) не реализованы.

## Полные пути файлов

Созданы:
- app/src/main/java/ru/fo6osik/workjournal/HarvestDayCalendar.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDayCalendarView.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestDayCalendarTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestCalendarUiTest.kt
Изменены:
- app/src/main/java/ru/fo6osik/workjournal/HarvestProcessActivity.kt
- app/src/main/res/layout/activity_harvest_process.xml
- docs/run-harvest-108-checks.py (добавлен запуск5новыхUItests, прежние сохранены)
- docs/codex-handoff.md
Удалённых файлов нет.

## Проверки

14JVM model tests покрывают месяцы/годы/skip/offset/leap/colors/invalid/empty/
selection/reset/summary/grouping/refresh/restore. 5UItests: цвета/selection/summary/
recreate/refresh, arrows, точный EXTRA_DAY_ID и stale-delete guard, multi-record
links без изменения Room constraint, empty campaign. Guard TEST package + CI
emulator, cleanup только созданной кампании; нет wipe пользовательской базы.
Новый CI проверенного SHA успешно завершён, XML отчёты скачаны и проверены:

- Android PR checks #116:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39203868837
  MAIN и TEST сборки успешны. JVM147/147 в каждом варианте,
  failures/errors/skips=0. HarvestDayCalendarTest14/14 и PR35 metrics21/21 в
  обоих вариантах. Подтверждение artifact12164984915/XML test-results;
  tasks testDebugUnitTest/testDebugTestUnitTest реально исполнены, не UP-TO-DATE.
  Instrumental89/89 в46вызовах, включая все5 HarvestCalendarUiTest.
  Прежние quick access/harvest108/full backup/Room23 migration/TEST update и
  read-only JSON/отдельное restore/restart успешны. Python46/46.
- Main APK safe update #41:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39203868846
  SUCCESS: build/JVM, Python26/26 и instrumental3/3, прежний synthetic update.
- MAIN Room22→23 migration #11:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39203868843
  SUCCESS: builds/JVM147/147 (artifact12165157373/XML), unsigned release
  analysis без публикации APK, instrumental3/3, старые столбцы/таблицы/связи/
  active states/fuel/events/files/preferences/timers сохранены, отдельный restore
  и restart успешны. Только disposable CI certificate, не production Secrets.

Итого95instrumental и72Python в Android/MAIN jobs. JVM147 уникальных сценариев
повторены по вариантам/jobs, не считать дубль новыми тестами. Локальные82 и
вложенные24 не суммировать с CI72. Опубликованы только diagnostics artifacts;
android-pr6-diagnostics содержит0APK, подписанный TEST/MAIN publisher не запускался.
Все5 новых UItests реально исполнились: selection/colors/summary/recreate/edit
refresh; arrows; точный dayId + отказ после удаления; grouped record links;
empty campaign. Существующие проверки не отключались/не ослаблялись.
python3 -m unittest discover -s docs -p 'test_*.py':82tests,80pass/2SDKskip;
вложенные24tests не суммировать повторно (23pass/1SDKskip). Diff whitespace clean.
source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test:
Gradle не стартовал: FileLockContentionHandler / Could not determine a usable
wildcard IP for this machine. Настройки проекта ради среды не менялись.

## Риски/следующие действия

CI/проверка XML завершены; требуется независимый review DraftPR36.
Из выявленных проблем — только ограничения локальной среды Gradle wildcard IP
и git proxy, обходов нет. Компиляция/новые и старые тесты в CI успешны.
Реальный смартфон, ручная оценка календаря на разных устройствах/шрифтах не
проверялись. Layout использует7равных колонок, wrap heights и внешний ScrollView;
дополнительный визуальный review на малом экране/крупном шрифте рекомендован.
Ручная проверка смартфона не выполнялась. Malformed workDate безопасно не
размещается в сетке; данные не исправляются. Ячейки адаптируются к ширине/шрифту,
весь экран уже в ScrollView. Date API java.util (minSdk23), зависимости не нужны.
Не merge/release/APK publication/устройства/пользовательские данные. После review
остановиться. Production signer и TEST artifact publisher не запускаются.
