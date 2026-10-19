# АгроНализ 1.0.9 — этап 1: расчётная основа кампании

18 октября 2026. Реализована логика выбора площади, без изменения хранения данных.
Ветка feature/harvest-1-0-9-calculations. База актуального main:
ad66e4b452ec3c93866f4f6b8163087274166d45, tree47c7ae77fffd19b76e452b69a8d26d472839316c.
main содержит объединённый пользователем PR33; это проверено GitHub compare.
Локальная исходная копия a161f8a имеет точно то же дерево; git fetch недоступен
(Failed to connect to proxy port8080), поэтому remote commit создаётся через
GitHub API непосредственно с parent main ad66e4b. Старые коммиты feature1.0.8
не входят в новый PR. Рабочая копия перед началом была чистой.

## Выполнено

Изучены HarvestCampaignMetrics, HarvestCalculator, HarvestProcess, HarvestDay,
HarvestDao, HarvestProcessActivity, HarvestCampaignMetricsTest. DAO запрещает
завершить кампанию с ACTIVE днём, но selector всё равно проверяет все allDays,
не только переданную completed-подвыборку. Данные/статусы DAO не изменены.

SelectedArea содержит nullable hectares и AreaSource: COMPLETED_DAYS только для
завершённой кампании с непустым списком всех дней и положительной конечной
площадью каждого; иначе ACTUAL_PLANTED, FIELD_SNAPSHOT, UNKNOWN. NaN/Infinity/
ноль/отрицательные значения отвергаются. Переполнение суммы не даёт ложный итог.
Масса/диапазоны и индивидуальная плотность каждого дня остаются в исходном
HarvestCalculator. WholeAreaYield теперь использует выбранный источник;
yieldCentnersPerHa = оба предела т/га ×10 без промежуточного округления.
При неизвестной/частичной массе урожайность кампании null; частичная масса
остаётся отдельно с явной отметкой. Сохранён отдельный harvestedAreaYield.
Result также отдаёт bunkerCount/volumeM3, известные гектары завершённых дней,
число дней без площади, remaining/progress и progressComplete. Частичный
progress считается по известным завершённым гектарам, помечен неполным.
Превышение100% и отрицательный остаток не обрезаются, есть предупреждение.
Activity передаёт статус/исторический снимок из текущей модели при каждом
пересчёте. Изменены только текст источника и метки неполноты в существующем
текстовом блоке; layout/календарь/визуальное оформление не менялись.

## Файлы

Изменены:
- app/src/main/java/ru/fo6osik/workjournal/HarvestCampaignMetrics.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestProcessActivity.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestCampaignMetricsTest.kt
- docs/codex-handoff.md

Созданных/удалённых файлов нет. Room23/entity/DAO/миграции, versionName1.0.8,
versionCode, build config, package/signature, workflows, таймеры/события/напоминания
не изменены. Это расчётная основа1.0.9, не выпуск APK1.0.9.

## Проверки и статус

HarvestCampaignMetricsTest:21метод (8исходных +13новых), покрывают12сценариевТЗ,
невалидные площади, полный allDays вместо subset, overflow и partial progress.
Старое ожидание null progress при частичной площади обновлено по новому ТЗ;
остальные исходные ожидания сохранены. Тесты Kotlin выполнены в GitHub CI, результаты ниже; локально Gradle не стартует.
python3 -m unittest discover -s docs -p 'test_*.py':82tests,80pass/2SDKskip.
Вложенные24tests не прибавлять повторно (23pass/1SDKskip).
source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test:
не стартовал Gradle: FileLockContentionHandler / Could not determine a usable
wildcard IP for this machine. Конфигурация проекта ради среды не менялась.
git diff --check успешен. Проверенный remote SHA38d165032ce5415bde140c00e4f103874cf8ccba
(локальный a8756fa, одинаковое дерево234fdd2e5438821e9e1bde1cb90c9f8946705b62).
Создан Draft PR #35: https://github.com/Arch-relic/AgroNaliz/pull/35,
base main ad66e4b; mergeable, не объединён. Этот итоговый отчёт сохраняется
следующим docs-only коммитом; его SHA не объявляется повторно проверенным CI.

Полный CI проверенного SHA успешно завершён:
- Android PR checks #112 https://github.com/Arch-relic/AgroNaliz/actions/runs/39145983671
  MAIN/TEST APK сборки успешны, testDebugUnitTest133/133 и
  testDebugTestUnitTest133/133,0failures/errors/skips. Metrics21/21 в каждом варианте.
  Числа подтверждены скачанным android-pr6-diagnostics artifact12144010971,
  XML test-results обеих сборок. Оба Gradle test tasks реально исполнены,
  не UP-TO-DATE. Python46/46. Instrumental84/84 в45вызовах: Harvest108Test5/5,
  HarvestFullBackup108Test4/4, HarvestQuickAccessUiTest2/2, migration23 в3вызовах,
  навигация/layout, backup roundtrip/merge и TEST update/restart сохранность.
- Main APK safe update #38 https://github.com/Arch-relic/AgroNaliz/actions/runs/39145983668
  SUCCESS: Python26/26, instrumental3/3, сборки/JVM успешны; старый MAIN
  update fixture сохранён без изменения тестовых данных.
- MAIN Room22 to23 #8 https://github.com/Arch-relic/AgroNaliz/actions/runs/39145983690
  SUCCESS: build/JVM133/133 (XML artifact12143263989), unsigned release analysis,
  3instrumental: synthetic1.0.7/3000003/Room22→1.0.8/3000004/Room23,
  все old columns/tables/active states/fuel/events/files/preferences/timers
  сохранены; отдельная restore database и restart успешны. Только CI debug key.
Итого инструментальных90pass, Python72pass в Android/MAIN jobs. JVM133 уникальных
теста повторяются по build variants/jobs, не считать их разными сценариями.
CI документов/политики из82локальных и вложенные24 не суммировать с CI72.

Обнаруженные проблемы ограничены локальной средой (Gradle wildcard IP и git
proxy), обходов/проектных правок не делалось. GitHub SDK builds и все tests
успешны. Предупреждения компилятора о reified intersection/deprecated versionCode
в существующих migration tests не относятся к изменённой логике и не исправлялись.
Непроверенное: ручная проверка на реальном смартфоне; она не выполнялась.
Изменений БД нет, миграция не требуется. Сохраняется риск исходных неполных
данных: результат явно помечен/урожайность null, неизвестное не подменяется нулём.

## Ограничения и следующий шаг

CI завершён, требуется независимый review PR35. После публикации отчёта остановиться.
Не переходить к календарю, ползункам и истории. Не менять версию/Room/сборку.
Не merge, не signing/release, не реальные устройства/пользовательские базы.
