# АгроНализ 1.0.9 — этап 2: календарь рабочих дней

19 октября2026. Ветка feature/harvest-1-0-9-calendar, база main
130d18eb6e189a57d49d589f544df2dd0ccbd387, treeee2e2c4348cb8c65ef230cee3b7ba728d9d800db.
Main уже содержит объединённый пользователем PR35. Локальный исходныйa80fb66
имеет идентичное дерево, проверено GitHub API. Git fetch заблокирован proxy8080;
новый remote commit будет создан API с parent точного main, без старых feature commits.
До начала status clean. Сейчас изменения ещё не опубликованы/PR не создан.

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
Новые тесты пока не запускались: нужен CI.
python3 -m unittest discover -s docs -p 'test_*.py':82tests,80pass/2SDKskip;
вложенные24tests не суммировать повторно (23pass/1SDKskip). Diff whitespace clean.
source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test:
Gradle не стартовал: FileLockContentionHandler / Could not determine a usable
wildcard IP for this machine. Настройки проекта ради среды не менялись.

## Риски/следующие действия

Опубликовать отдельный DraftPR, выполнить существующий полный CI MAIN/TEST,
проверить XML JVM и UI logs, записать точный реально проверенный SHA и run URLs.
Ручная проверка смартфона не выполнялась. Malformed workDate безопасно не
размещается в сетке; данные не исправляются. Ячейки адаптируются к ширине/шрифту,
весь экран уже в ScrollView. Date API java.util (minSdk23), зависимости не нужны.
Не merge/release/APK publication/устройства/пользовательские данные. После review
остановиться. Production signer и TEST artifact publisher не запускаются.
