# АгроНализ1.0.9 — этап3: карточка результатов

19 октября2026. Ветка feature/harvest-1-0-9-results-card, база актуального main
20b10c8cad0aea6ef501ad4bbe000b61591ec440, treefbe011c88bb00900e1d66c0b643f64e24d955807.
Main содержит объединённые PR35/36. Локальный72bf822 имеет то же дерево;
проверено через GitHub API. Git fetch недоступен (proxy8080); remote commit
создаётся API с parent точного main. Перед началом status clean.
Создан DraftPR37: https://github.com/Arch-relic/AgroNaliz/pull/37, не объединён.
Реально проверенный remote SHAa39acb027842ada636ae3c06efa5a97322203353
(локальныйffc9031, tree1eb3a610df26c5131962888a88c49c9af8b07126).
Этот итоговый отчёт сохраняется следующим docs-only коммитом; его собственный
SHA не выдаётся за повторно проверенный Android-код.

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
полный JVM набор162 (147+15), фактически успешно выполнен вCI.

Полный CI проверенного SHA завершён SUCCESS:
- Android PR#119 https://github.com/Arch-relic/AgroNaliz/actions/runs/39210013484
  MAIN/TEST builds успешны. JVM testDebugUnitTest162/162 и
  testDebugTestUnitTest162/162,0failures/errors/skips. Metrics26/26,
  ResultsCardModel10/10 в обоих вариантах. Подтверждено скачанным
  artifact12167329936/XML; задачи test*UnitTest выполнены, не UP-TO-DATE.
  Python46/46; instrumental95/95 в48вызовах: новые5/5UItests карточки + повтор
  compactLargeFont на720x960/font1.6, старый calendar5/5 и все navigation/harvest/
  backup/Room23/TEST update/read-only JSON/restore/restart tests успешны.
- MAIN safety#43 https://github.com/Arch-relic/AgroNaliz/actions/runs/39210013341
  build/JVM, Python26/26 и instrumental3/3 SUCCESS. Старый synthetic update
  сохраняет все прежние данные/preferences/files/timers без uninstall/clear.
- MAIN Room22→23#13 https://github.com/Arch-relic/AgroNaliz/actions/runs/39210013276
  builds/JVM162/162 (XML artifact12167138695), unsigned release analysis,
  instrumental3/3 SUCCESS: все old columns/tables/связи/active states/fuel/events/
  files/preferences/timers сохранены, отдельный restore и restart успешны.
Итого101instrumental и72Python в Android/MAIN jobs. JVM162 повторяются по
вариантам/jobs, не суммировать дубли. Локальные82/вложенные24 не суммировать сCI72.
Выложены только diagnostics artifacts, в Android artifact0APK (проверен ZIP).
Реальный production signer и TEST artifact publisher не запускались.
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

CI/проверка XML/логов завершены, требуется независимый review DraftPR37.
Выявленные проблемы: только локальная среда (wildcard IP/proxy), настройки
проекта и защитные workflows ради неё не менялись. При компиляции остаются
прежние warnings о reified intersection/deprecated versionCode в migration tests;
к изменённой логике не относятся. Android builds и все новые/старые тесты успешны.
Ручная проверка на смартфоне не выполнялась. Малый экран/крупный шрифт проверены
автоматически в CI, визуальная оценка на реальных моделях устройств остаётся
следующим этапом. Новых DB/data-loss рисков нет: структура и запись данных
не менялись. Existing partial data остаётся неизвестным/помеченным, не нулём.
Не публиковать APK, не merge/release/sign production, не устанавливать на реальные
устройства, не работать с пользовательскими базами. Остановиться для review.
