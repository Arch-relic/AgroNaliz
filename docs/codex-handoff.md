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
остальные исходные ожидания сохранены. Тесты Kotlin пока не выполнены локально.
python3 -m unittest discover -s docs -p 'test_*.py':82tests,80pass/2SDKskip.
Вложенные24tests не прибавлять повторно (23pass/1SDKskip).
source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test:
не стартовал Gradle: FileLockContentionHandler / Could not determine a usable
wildcard IP for this machine. Конфигурация проекта ради среды не менялась.
git diff --check успешен. Коммит ещё не опубликован/PR пока не создан; после
создания будет отдельная запись точного проверенного SHA/PR и нового CI.
Не объявлять непроведённые Android tests успешными.

## Ограничения и следующий шаг

Выполнить существующий GitHub CI для нового PR (MAIN/TEST builds/JVM/emulator),
проверить все21metrics tests, записать реальные результаты и URL. Затем review.
Не переходить к календарю, ползункам и истории. Не менять версию/Room/сборку.
Не merge, не signing/release, не реальные устройства/пользовательские базы.
