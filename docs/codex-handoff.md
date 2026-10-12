# Передача контекста: АгроНализ v1.0 — масса и урожайность

Дата: 2026-10-11. Статус: задача выполнена, код и полные проверки успешны.
Draft PR #23: https://github.com/Arch-relic/AgroNaliz/pull/23 — открыт в main,
не объединён. Слияние требует отдельного решения владельца.

## Ветка и проверенный результат

- Ветка GitHub: `feature/harvest-yield`, создана от актуального main
  `c0ea3d01a97dd0eb4814b59307b44169916821bc` (Room v20); main напрямую не изменялся.
- Проверенный коммит кода: `2f0b6e740c06fdbf7d0d55bb88230078f141fc33`.
- Полный успешный workflow #47: https://github.com/Arch-relic/AgroNaliz/actions/runs/38177873746.
- Actions использовал проверочный PR merge-коммит
  `5c4ad56c31f9225793b9c689a2755400d0a0d18f`, объединяющий проверенный head с
  указанным main только для CI. **PR с main не объединён.** Поэтому короткий
  SHA в имени артефакта относится к проверочному merge-коммиту.
- Этот окончательный отчёт сохраняется следующим коммитом в той же ветке,
  меняющим только `docs/codex-handoff.md`. SHA выше обозначает реально
  проверенный код, а не хеш самого отчёта. Автоматический workflow для
  документального коммита может повторить те же проверки; его актуальный
  статус доступен во вкладке Checks PR.
- Все изменения опубликованы и закоммичены в GitHub. В изолированном локальном
  worktree сохранена незакоммиченная копия этих файлов; неопубликованного кода
  или незавершённых функциональных изменений нет. Остальные рабочие ветки сохранены.

## Поведение и архитектура

- Завершение дня принимает необязательную положительную дробную площадь, га;
  точка и запятая поддерживаются, пустое поле сохраняет null, а не ноль.
  Прежний вопрос о завершении кампании сохранён. Отмена каждого шага ничего
  не записывает; площадь/завершение дня/необязательное завершение кампании
  сохраняются одной транзакцией.
- Завершённый день имеет блок «Результаты уборки» и действие добавления/изменения
  площади, работающее и после завершения кампании. Пустым полем площадь можно
  снова сделать неизвестной. SQL обновляет только площадь, без открытия дня,
  изменения таймеров, времени окончания или статуса кампании. После записи
  карточка обновляется; Room-записи выполняются в фоновом потоке.
- `HarvestCalculator` содержит все формулы и прежние четыре коэффициента
  заполнения FULL [1;1], MORE_THAN_HALF [0,5;1], HALF [0,5;0,5], LESS_THAN_HALF
  [0;0,5]. Дублирующие расчёты объёма/среднего в двух Activity удалены.
  Масса и обе урожайности сохраняют диапазоны. Площадь не нужна для массы;
  неположительные/нечисловые значения и переполнение не приводят к делению
  на ноль или показу NaN/Infinity. Малые положительные площади не округляются в ноль.
- `HarvestDensityCatalog` задаёт восемь ориентировочных плотностей кг/м³;
  ручная корректировка хранится в конкретном процессе. null означает
  справочник, если культура известна. Для «Другое» нужна ручная плотность,
  иначе масса/урожайность не рассчитываются. При смене культуры корректировка
  сбрасывается. Она действует на все дни кампании, в том числе прошлые.
- Итоги кампании считают все бункеры, объём/массу всех дней и сумму известных
  площадей. При хотя бы одной неизвестной площади общая урожайность не
  рассчитывается, показывается предупреждение. При полных данных делится
  суммарная масса на суммарную площадь, не усредняются дневные урожайности.
- `HarvestResultsFormatter` занимается представлением, а не формулами.
  Справочник, допущения и формулы: `docs/harvest-results.md`.
- В двух карточках уборки ScrollView находится в FrameLayout, учитывающем
  системные отступы: нижние кнопки доступны при обязательном edge-to-edge
  на Android 15. Другие экраны не изменялись.

## Room и резервные копии

Версия базы: **21**, миграция **20 → 21** зарегистрирована в DatabaseProvider.
Добавлены nullable REAL `harvest_days.harvestedAreaHa` и
`harvest_processes.bulkDensityKgM3`. Все старые значения остаются на месте;
историческая площадь не угадывается. Прежние миграции, индексы, FK и настройки
напоминаний сохранены, destructive migration не применяется.

Экспорт v21 включает новые поля. Полное восстановление продолжает принимать
v18/v19 и теперь v20: пропущенные новые колонки остаются null; прежние разрешённые
пропуски таймеров/событий сохранены. Копия v21 обязана содержать все колонки.
Невалидные новые числовые значения проверяются до замены данных при restore.
Выборочный импорт сохраняет новые поля и прежнюю дедупликацию/переназначение ID;
принимаются v16, v18, v19, v20, v21.

Миграция проверена обновлением **реальной базовой TEST APK** с main-коммита выше:
Room v20 создаёт синтетические записи и копию; затем `adb install -r` обновляет
TEST до v21 с тем же applicationId и ключом внутри одного CI job. Room проверяет
полную схему v21. Сравниваются все старые значения всех таблиц, число записей,
FK, бункеры/события, одиннадцать полей напоминаний и отдельные SharedPreferences;
затем восстанавливается настоящая копия v20. Реальные данные пользователя не использовались.

## Файлы

Созданы (15):

- `app/src/androidTest/java/ru/fo6osik/workjournal/HarvestAreaUiTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/HarvestBackupTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/HarvestCampaignUiTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/HarvestMigrationTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/HarvestPersistenceTest.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestBackupCompatibility.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestCalculator.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestDensityCatalog.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestResultsFormatter.kt`
- `app/src/main/res/layout/dialog_harvest_number.xml`
- `app/src/main/res/values/harvest_results.xml`
- `app/src/test/java/ru/fo6osik/workjournal/HarvestBackupCompatibilityTest.kt`
- `app/src/test/java/ru/fo6osik/workjournal/HarvestCalculatorTest.kt`
- `docs/harvest-results.md`
- `docs/run-harvest-checks.py`

Изменены (14):

- `.github/workflows/android-pr-checks.yml`
- `app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt`
- `app/src/main/java/ru/fo6osik/workjournal/DataImportManager.kt`
- `app/src/main/java/ru/fo6osik/workjournal/DatabaseBackupManager.kt`
- `app/src/main/java/ru/fo6osik/workjournal/DatabaseProvider.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestDao.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestDay.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestDayActivity.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestProcess.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestProcessActivity.kt`
- `app/src/main/res/layout/activity_harvest_day.xml`
- `app/src/main/res/layout/activity_harvest_process.xml`
- `app/src/main/res/layout/dialog_finish_harvest_day.xml`
- `docs/codex-handoff.md`

Удалённых/перемещённых файлов нет. PDF, поисковые индексы, зависимости Gradle,
applicationId основной версии, исходная изоляция TEST и облачные интеграции не менялись.

## Команды и результаты проверок

Облачный GitHub Actions #47, Java 17 / Kotlin 2.2.20 / AGP 8.12.0 / Room 2.8.5:

```bash
./gradlew :app:assembleDebug :app:assembleDebugTest :app:testDebugUnitTest :app:testDebugTestUnitTest :app:assembleDebugAndroidTest --no-daemon --stacktrace --console=plain
./gradlew -PtestBuildType=debugTest :app:assembleDebugTestAndroidTest --no-daemon --stacktrace --console=plain
python3 -m unittest discover -s docs -p 'test_validate_document_catalog.py' -v
python3 docs/verify-build-variants.py
.migration-v20/gradlew -p .migration-v20 :app:assembleDebugTest --no-daemon --stacktrace --console=plain
python3 docs/run-build-isolation-checks.py
python3 docs/run-harvest-checks.py
```

- Основная debug и отдельная debugTest APK, обе инструментальные APK и
  старая v20 TEST APK: BUILD SUCCESSFUL.
- JVM: новый запуск `testDebugUnitTest` и `testDebugTestUnitTest`, **95 тестов
  на вариант, 190 выполнений**, 0 failures/errors/skipped. XML-отчёты из
  опубликованного diagnostics ZIP скачаны и суммированы. Это не UP-TO-DATE.
  Новые классы: HarvestCalculatorTest (24), HarvestBackupCompatibilityTest (4).
- Python-каталог: **13 тестов**, 0 ошибок.
- Android API 35 x86_64: **17 новых инструментальных тестов**, 0 ошибок:
  migration 2, persistence 5, day UI 5, campaign UI 2, backup/import 3.
  Проверены обе отмены, пустая/дробная площадь, запрет нуля, добавление и изменение
  после завершения кампании, ручная плотность/снятие корректировки, диапазоны
  кампании и неполные площади, rollback, backup v20/v21 и additive import v21.
- Существующие проверки изоляции выполнены 7 раз с контекстами main/TEST,
  включая повторы проверки main после действий/удаления TEST. Всего с новыми
  проверками: **24 инструментальных выполнения**, 0 ошибок.
- Провайдеры имеют разные authorities, общий UID отсутствует; проверены разные
  UID/Room/настройки/files/cache/noBackupFilesDir/документный store/внешние app-scoped
  пути, приватный FileProvider TEST и отказ записи в общий SAF. Main остаётся
  неизменённым после новой миграции, редактирования и удаления синтетической TEST.
- `git diff --check`, XML-разбор и Python syntax: успешны.

Предусмотренная локальная команда также запускалась:

```bash
source /workspace/agronaliz-env.sh
agrogradle :app:assembleDebug :app:test
```

Gradle не смог стартовать: `Could not determine a usable wildcard IP for this machine`.
Это ограничение среды, локальная Android-сборка не объявляется успешной.
Дополнительно новый запуск cached Kotlin 2.0.21 / JVM 1.8 / JUnit 4.13.2
(K2JVMCompiler + JUnitCore с HarvestCalculatorTest/HarvestBackupCompatibilityTest)
дал **28 тестов, 0 ошибок**; он не заменяет успешный Actions на Kotlin 2.2.20.

Исправленные ошибки CI:

1. #43/#44: RoomDatabase этой версии не поддерживает `use` — заменено на
   try/finally/close в новых тестах.
2. #45: окна не имели фокуса после сна/блокировки эмулятора — runner явно
   пробуждает свежий эмулятор, снимает keyguard, удерживает экран включённым;
   при ошибке печатает диагностику окна/питания.
3. #46: последняя кнопка после scrollTo попадала на границу системной навигации
   Android 15 — исправлены отступы двух карточек, все UI-тесты прошли в #47.

## APK и скачивание

Actions: https://github.com/Arch-relic/AgroNaliz/actions/runs/38177873746.

| Вариант | applicationId | Артефакт |
| --- | --- | --- |
| Основная debug | `ru.fo6osik.workjournal` | `app-debug-feature-harvest-yield-5c4ad56c31f9` |
| «АгроНализ TEST» | `ru.fo6osik.workjournal.debugtest` | `app-TEST-feature-harvest-yield-5c4ad56c31f9` |

TEST ZIP: https://github.com/Arch-relic/AgroNaliz/actions/runs/38177873746/artifacts/11716593301.
Артефакты доступны до 2026-10-25T21:57:37Z; после истечения срока можно
запустить workflow_dispatch для этой ветки/после принятия PR для main.
Две ZIP скачаны и распакованы, реальные APK проверены `aapt2 dump badging` и
`apksigner verify --print-certs`. Подписи валидны, идентификаторы/имя TEST верны.
SHA-256 TEST APK: `cc63bdbaeccc4b93577ff9927e3a0f10610a31d269f52cc382c97dfa8e4e7383`.
SHA-256 основной debug APK: `befa396fa8d4824fbda4d4af75a680cb0972754f5aa6012880589c77b2a7c6d7`.
Это хеши файлов APK, не ZIP артефактов.

Для телефона:

1. Открыть Actions выше, войти в GitHub при необходимости, выбрать **app-TEST-***.
2. Скачать ZIP и извлечь `app-debugTest.apk`.
3. Открыть APK на Android; при запросе разрешить установку из используемого
   файлового приложения/браузера.
4. Установить «АгроНализ TEST» рядом с основной версией. **Основную не удалять,
   не переустанавливать и не устанавливать вместо неё app-debug.apk.**

Отладочный ключ CI может отличаться между запусками. Если уже установленная
TEST сообщает о конфликте подписи, сначала сохранить данные именно TEST и
решить обновление этой отдельной установки; основную версию не затрагивать.

## Ограничения, риски и следующие ручные действия

- Масса/урожайность приблизительны; фиксированные справочные плотности не
  измеряют конкретную партию. Влажность, примеси, сорт и уплотнение требуют
  ручной корректировки и сверки с весами. Диапазон отражает заполнение бункеров,
  а не статистический интервал погрешности плотности.
- Ручная плотность применяется ретроспективно ко всей кампании; при смене
  культуры она сбрасывается. Неизвестные исторические площади остаются неизвестными.
- Физический телефон и API 23 не проверялись; автоматические Android-проверки
  выполнены на API 35 со строго синтетическими данными.
- На телефоне проверить точку/запятую реальной клавиатуры, небольшой экран,
  крупный шрифт, обе навигации Android, отмену/редактирование площади и возврат
  к итогам кампании; работу напоминаний при блокировке/энергосбережении,
  события после бункера и обычное редактирование/удаление бункеров.
- CI-скрипты предназначены только для одноразового эмулятора: проверяют qemu
  и отказываются заменять существующие main/TEST установки. Не запускать их на телефоне.
- Просмотреть Draft PR и провести прикладную сверку расчётов на TEST. Выпуск/
  слияние оставлены владельцу; незавершённых задач разработки нет.
