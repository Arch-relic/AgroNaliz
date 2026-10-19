# АгроНализ1.0.9 — этап3: карточка результатов

Ветка feature/harvest-1-0-9-results-card, база актуального main
20b10c8cad0aea6ef501ad4bbe000b61591ec440, treefbe011c88bb00900e1d66c0b643f64e24d955807.
Main содержит объединённые PR35/36. Локальный72bf822 имеет то же дерево;
проверено через GitHub API. Git fetch недоступен (proxy8080); remote commit
создаётся API с parent точного main. Перед началом status clean.
PR ещё не создан, изменения ещё не опубликованы; финальный SHA/CI будут ниже.

## Решения

HarvestResultsCardView заменяет4старых текстовых итога единым компонентом
перед неизменённым календарём. Сетка2×2: площадь/бункеры/масса/урожайность
т/га+ц/га. Под заголовком toggle Подробнее/Свернуть со стрелкой; по умолчанию
свёрнуто. Короткий notice виден и в свёрнутом состоянии. Details: объём, source,
историческая/фактическая/убранная/целевая площади, progress, остаток, unknown days,
отдельная harvested yield если доступна, progress bar и предупреждения.
HarvestResultsCardModel форматирует готовый Metrics.Result без пересчёта массы/
урожайности; сохраняет диапазоны/единицы, unknown=прочерк, partial mass помечена.

Изолированная правка Metrics: selectArea/все формулы yield/масса/плотность/объём
не изменены. Добавлены selectTargetArea(actual,snapshot), nullable targetArea/source
в конце Result с default для совместимости. Progress/remaining/exceedsActual
теперь по target actual→snapshot→UNKNOWN, никогда по COMPLETED_DAYS.
При завершённой кампании90га/actual100: yieldArea90, progress90%, remaining10.
Сигнатура calculate неизменна. Прежнее ожидание progress100 при days90/actual95
исправлено на90/95×100 и остаток5; исходные правила урожайности сохраняются.

При частичных гектарах точное предупреждение ТЗ, известная часть помечена.
Масса partial/отсутствие yield/active day имеют объяснения. Значения >100% и
negative remainder сохраняются текстом, только визуальная полоса clamp0..100.
Если target неизвестна, bar скрыт и прогресс неизвестен (прочерк+объяснение).
Expanded — только View+Bundle, сохраняется при update/recreate; Room onResume
перечитывается прежней загрузкой. Удалены старые блоки итогов, дублей нет.
Новые зависимости не нужны, значения wrap/multiline/weight2колонки/ScrollView.

## Файлы

Созданы:
- app/src/main/java/ru/fo6osik/workjournal/HarvestResultsCardModel.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestResultsCardView.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestResultsCardModelTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestResultsCardUiTest.kt
Изменены:
- app/src/main/java/ru/fo6osik/workjournal/HarvestCampaignMetrics.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestProcessActivity.kt
- app/src/main/res/layout/activity_harvest_process.xml
- app/src/test/java/ru/fo6osik/workjournal/HarvestCampaignMetricsTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestCampaignStateUiTest.kt
  (только readiness lookup удалённого text ID заменён на currentModel карточки,
  все прежние проверки кнопки/навигации сохранены)
- docs/run-harvest-108-checks.py (добавлены5UItests+повтор compactLargeFont
  на720x960/font1.6; reset вfinally; все старые проверки сохранены)
- docs/codex-handoff.md
Удалённых файлов нет. Room23/миграции/версия/code/signatures/applicationId/build,
календарь PR36, day creation/edit, события/таймер/напоминания/backup/density
не изменены. Старые поля хранения не удаляются, реальные данные не затронуты.

## Проверки

Добавлены5target JVM tests (metrics теперь26) и10card model tests; ожидаемый
полный JVM набор162 (147+15), фактический запуск вCI ещё предстоит.
5UItests: grid/collapse/toggle; Room edit area/day/bunker +recreate; range/partial/
excess; неизменность calendar selection; smallscreen/largefont (также повтор
в отдельной конфигурации эмулятора). Cleanup только owned synthetic campaign,
проверки package TEST+emulator. Старые календарные navigation tests сохранены.
Локально python3 -m unittest discover -s docs -p 'test_*.py':82/80pass/2SDKskip,
вложенные24 не суммировать (23pass/1SDKskip). git diff --check clean.
source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test
не стартовал: FileLockContentionHandler / Could not determine a usable wildcard
IP for this machine. Конфигурация ради локальной среды не менялась.

## Следующий шаг/ограничения

Создать DraftPR, выполнить полный существующий CI MAIN/TEST/JVM/instrumental,
проверить XML/логи и записать точный проверенный SHA/run URLs. Не объявлять
непроведённые проверки успешными. Ручная проверка на смартфоне не выполнялась.
Не публиковать APK, не merge/release/sign production, не устанавливать на реальные
устройства, не работать с пользовательскими базами. Остановиться для review.
