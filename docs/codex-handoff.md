# Передача контекста: результаты уборки и исправления PR #26

Дата: 2026-10-13. Статус: реализация и регрессионные проверки успешны.
PR #26: https://github.com/Arch-relic/AgroNaliz/pull/26 — открыт, Draft, не объединён.
Ветка: feature/harvest-day-density. Основа main:
c0ea3d01a97dd0eb4814b59307b44169916821bc (Room v20).
Проверенный кодовый SHA: 4a45158b66c75fd35036406883c848546c296d88.
Actions проверил PR merge ref b6a69fd4390c с этим head и указанной main.
Этот итоговый отчёт сохраняется отдельным последующим коммитом документации;
он не выдаёт SHA содержащего его коммита за уже проверенный код.
Локальный worktree содержит незакоммиченные зеркала; реальные коммиты и ветка
опубликованы через GitHub API. Посторонние рабочие деревья сохранены.

## Реализованные функции

Единый справочник плотностей соответствует всем восьми культурам и тройкам
MIN/AVERAGE/MAX из технического задания. CUSTOM — конечное положительное число.
Каждый новый HarvestDay получает MIN по текущей культуре, а не настройку прошлого дня.
«Другое» и неподдерживаемые культуры остаются без плотности до ручного ввода.
Плотность изменяется и у завершённого дня; сохранённый CUSTOM повторно отображается.
При смене культуры существующие плотности остаются, пользователь видит предупреждение.

Шестерёнка разделяет настройки событий и плотности. Старые интервалы и таймеры
сохранены. Площадь необязательна при завершении, поддерживает запятую и точку;
после завершения её можно добавить, изменить или очистить, не открывая день.
При повторном завершении сохранённая площадь предложена в поле ввода.

Общий HarvestCalculator сохраняет FULL=1, HALF=0,5, MORE_THAN_HALF=0,5–1,
LESS_THAN_HALF=0–0,5. Масса=объём×плотность/1000; ц/га=масса×10/площадь;
т/га=масса/площадь. Границы сохраняются, результаты помечены приблизительными.
Кампания суммирует известные индивидуальные массы и известные площади;
урожайность только по дням с одновременно известными массой и площадью.
Неполные сведения явно обозначены; неизвестная масса не превращается в ноль.

## Исправления замечаний

HarvestChronology и транзакции HarvestDao проверяют сохранение времени:
окончание не раньше последнего бункера, редактируемый бункер не позже окончания,
начало не позже первого бункера. Строгий формат HH:mm. Изменение начала не может
молча перенести существующий бункер на другую сторону полуночи.
Для 10:47 / бункера 11:47 / окончания 10:48 сохранение отклонено с конфликтующим временем.
Бункеры и интервалы сортируются относительно начала смены, включая полночь.
Старые времена автоматически не исправляются и записи не удаляются.

Повторное открытие дня атомарно открывает кампанию. Новый день запрещён при
другом активном дне и в закрытой кампании. Кампания с активным днём показывает
«Продолжить рабочий день», без активного — «Новый день». Закрытие кампании до
окончания активного дня запрещено. Повторное завершение сохраняет площадь,
плотность, события и таймеры. Закрытие более раннего дня не сокращает конечную
дату всей истории кампании. Проверены пересоздание экранов и реальный restart TEST-процесса.

## Room и совместимость

Миграция 20→21 добавляет только harvestedAreaHa REAL, bulkDensityKgM3 REAL,
densityMode TEXT в harvest_days, все nullable. Старые поддерживаемые культуры
получают минимальную плотность, неизвестные — null. Старые поля и строки не меняются.
Версия остаётся 21; исправления хронологии не требуют дополнительной миграции.
Destructive migration не применяется. Настройки, техника, поля, события,
кампании, дни и бункеры сохраняются. Полный backup v18–20 совместим с v21;
backup/import v21 сохраняют новые значения и проверяют числовую корректность.
Схемы и логика прочих рабочих записей не меняются. Подпись APK и PR #25 не изменены.

## Файлы

Изменены:
- .github/workflows/android-pr-checks.yml
- app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt
- app/src/main/java/ru/fo6osik/workjournal/DataImportManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseBackupManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseProvider.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDao.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDay.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDayActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestProcessActivity.kt
- app/src/main/res/layout/activity_harvest_day.xml
- app/src/main/res/layout/activity_harvest_process.xml
- app/src/main/res/layout/dialog_add_harvest_bunker.xml
- app/src/main/res/layout/dialog_edit_harvest_day.xml
- app/src/main/res/layout/dialog_finish_harvest_day.xml
- docs/codex-handoff.md

Созданы:
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestAreaUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestBackupTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestCampaignStateUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestLifecycleTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestMigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestPersistenceTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestRestartTest.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestBackupCompatibility.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestCalculator.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestChronology.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDensityCatalog.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestResultsFormatter.kt
- app/src/main/res/layout/dialog_harvest_number.xml
- app/src/main/res/values/harvest_results.xml
- app/src/test/java/ru/fo6osik/workjournal/HarvestCalculatorTest.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestChronologyTest.kt
- docs/harvest-results.md
- docs/run-harvest-checks.py

Удалённых файлов нет. PDF, ключи, конфигурации авторизации и пользовательские данные не добавлены.

## Проверки

Успешный Actions #61:
https://github.com/Arch-relic/AgroNaliz/actions/runs/38370424111
Job 115205751174: success, все шаги успешны.

Новые исполнения (JVM задачи не UP-TO-DATE):
./gradlew :app:assembleDebug :app:assembleDebugTest :app:testDebugUnitTest :app:testDebugTestUnitTest :app:assembleDebugAndroidTest --no-daemon --stacktrace --console=plain
./gradlew -PtestBuildType=debugTest :app:assembleDebugTestAndroidTest --no-daemon --stacktrace --console=plain
JVM: 99 тестов debug и 99 debugTest, 0 failures/errors/skipped, подтверждено XML отчётами.
В их составе 20 HarvestCalculatorTest и 12 HarvestChronologyTest для каждого варианта.

python3 -m unittest discover -s docs -p 'test_validate_document_catalog.py' -v:
13 тестов, успешно. python3 docs/verify-build-variants.py: ID/provider isolation SUCCESS.
Эмулятор API 35: python3 docs/run-build-isolation-checks.py и python3 docs/run-harvest-checks.py.
26 harvest instrument runs: 2 migration, 5 persistence, 7 area/density/edit UI,
3 backup/import, 5 lifecycle/chronology, 2 campaign UI, 2 process restart.
Все успешны. Реальное обновление исходной v20 TEST APK до v21 сохраняет каждое
старое значение каждой таблицы; Room валидирует схему. Synthetic main проверена
после тестов и после удаления установленной только для CI TEST-версии.
Process restart: force-stop исключительно TEST на одноразовом эмуляторе,
следующий instrumentation в новом процессе проверяет данные и повторное завершение.

Скачан TEST artifact и независимо проверен aapt2: ru.fo6osik.workjournal.debugtest,
название «АгроНализ TEST», versionCode 1. Архив содержит только app-debugTest.apk;
SHA-256 архива совпал с metadata GitHub. Основной ID ru.fo6osik.workjournal проверен CI.
TEST APK:
https://github.com/Arch-relic/AgroNaliz/actions/runs/38370424111/artifacts/11798259938
Диагностика/отчёты: artifact 11798264964. Основная debug APK: artifact 11799300939.
Артефакты APK хранятся 14 дней, отчёты 7 дней.

Локально source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test
не стартовал: Could not determine a usable wildcard IP for this machine.
Это ограничение локальной среды; успешные сборки и тесты выполнены GitHub Actions.
Локальные XML syntax check, git diff --check и SQL smoke миграции успешны.

Ранние CI прерывались на подготовке теста с двумя активными днями и выборе
native Spinner popup в AlertDialog. Исправлены тестовые сценарии, guards и
проверки приложения не отключались. Последний полный запуск успешен.

## Ограничения и ручные проверки

Существующая модель HH:mm без даты каждого бункера описывает смену менее 24 часов.
Время до начала относится к следующим суткам. Для смен 24 часа и более нужны
отдельные даты/полные timestamps — сейчас они не поддерживаются.
Плотности ориентировочные; расчёт не заменяет взвешивание и измерение влажности.
Физический телефон, доставка уведомлений и особенности клавиатур не проверены.

На TEST вручную проверить 10:47→11:47→10:48 (отказ), ночную смену 23:00→00:30→01:00,
редактирование времени, цикл закрыть→открыть→продолжить→закрыть, ввод/очистку площади,
CUSTOM с запятой, смену культуры с предупреждением и настройки событий.
Основное приложение не удалять и не переустанавливать ради этих проверок.
Скачать artifact ZIP из Actions и извлечь app-debugTest.apk для отдельной TEST-установки.
Незавершённой разработки и известных падений автоматических проверок нет;
ручные проверки на телефоне остаются владельцу. PR не объединять автоматически.
