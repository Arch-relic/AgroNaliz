# Передача контекста: АгроНализ v1.0 — масса и урожайность

Дата: 2026-10-11. Статус: реализация опубликована в Draft PR #23; GitHub Actions выполняется.

Ветка: `feature/harvest-yield`; опорный актуальный main:
`c0ea3d01a97dd0eb4814b59307b44169916821bc` (Room v20).
Локальный worktree использует сохранённый эквивалент файлов PR #22; публикация
наследует дерево реального main, включая все неизменённые файлы.
Проверенный SHA GitHub ещё не определён: локальные проверки проводились над
незакоммиченными изменениями; после CI отчёт будет обновлён фактическими данными.

## Изменения

- Необязательная дробная площадь при завершении дня; запятая/точка, null вместо нуля.
- Добавление/редактирование/очистка площади завершённого дня без открытия кампании.
- Центральные расчёты сохраняют все четыре диапазона заполнения; масса и две урожайности.
- Восемь справочных плотностей, ручная корректировка кампании, «Другое» без выдуманной плотности.
- Итоги кампании не вычисляют урожайность при неполных площадях.
- Room 20 → 21: два nullable REAL поля; прежние записи, таймеры и события не меняются.
- Совместимость backup v18–20, экспорт/restore/import v21 и проверка новых значений.
- CI сохраняет прежние проверки, строит main/TEST и проверяет реальную миграцию APK.

## Файлы

- `.github/workflows/android-pr-checks.yml`
- `app/src/androidTest/java/ru/fo6osik/workjournal/HarvestAreaUiTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/HarvestBackupTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/HarvestMigrationTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/HarvestPersistenceTest.kt`
- `app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt`
- `app/src/main/java/ru/fo6osik/workjournal/DataImportManager.kt`
- `app/src/main/java/ru/fo6osik/workjournal/DatabaseBackupManager.kt`
- `app/src/main/java/ru/fo6osik/workjournal/DatabaseProvider.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestBackupCompatibility.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestCalculator.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestDao.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestDay.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestDayActivity.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestDensityCatalog.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestProcess.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestProcessActivity.kt`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestResultsFormatter.kt`
- `app/src/main/res/layout/activity_harvest_day.xml`
- `app/src/main/res/layout/activity_harvest_process.xml`
- `app/src/main/res/layout/dialog_finish_harvest_day.xml`
- `app/src/main/res/layout/dialog_harvest_number.xml`
- `app/src/main/res/values/harvest_results.xml`
- `app/src/test/java/ru/fo6osik/workjournal/HarvestBackupCompatibilityTest.kt`
- `app/src/test/java/ru/fo6osik/workjournal/HarvestCalculatorTest.kt`
- `docs/harvest-results.md`
- `docs/run-harvest-checks.py`
- `app/src/androidTest/java/ru/fo6osik/workjournal/HarvestCampaignUiTest.kt`
- `docs/codex-handoff.md`

Удалённых файлов нет. PDF, индексы, основная идентичность приложения, изоляция
TEST, зависимости и облачные интеграции не изменялись.

## Проверки

- `source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test`:
  НЕ выполнены сборка/тесты: Gradle не запустился,
  `Could not determine a usable wildcard IP for this machine`.
- Прямой запуск cached Kotlin 2.0.21 / JVM 1.8 / JUnit 4.13.2 для
  HarvestCalculatorTest и HarvestBackupCompatibilityTest: новый запуск,
  28 тестов, 0 ошибок. Это не Android-сборка на Kotlin 2.2.20.
- `python3 -m unittest discover -s docs -p 'test_validate_document_catalog.py' -v`:
  13 тестов, 0 ошибок.
- XML разобран стандартным XML parser; Python-скрипт проверен py_compile.
- Android API 35 UI/Room/backup/изоляция и две APK: ожидают запуска GitHub Actions.

Первый запуск Actions #43 (38175943332) обнаружил несовместимость `RoomDatabase.use`
в новых инструментальных тестах; исправлено на явный try/finally/close.
Actions #45 (38176250072) собрал обе APK, прошёл JVM/catalog/identifiers,
существующую изоляцию и реальные Room20→21/legacy-backup/persistence тесты.
UI получил RootViewWithoutFocusException на эмуляторе; в CI-скрипт добавлены
явное пробуждение/снятие keyguard/stay-on и диагностика окна/питания.
Actions #46 (38177055407): Room/legacy backup/persistence прошли вновь;
4 из 5 UI-тестов дня прошли. Последний scrollTo/click попал на системную
навигацию Android 15 (insets/nav boundary), диалог не открылся.
В обеих карточках уборки добавлен корневой FrameLayout с fitsSystemWindows,
чтобы ScrollView/нижние кнопки находились внутри безопасной области.
Полный повторный запуск ожидается.

## Ограничения и ручные проверки

Оценки плотности и наполнения приблизительны: сверять с весами; справочник
и формулы описаны в `docs/harvest-results.md`. Ручная плотность применяется
ретроспективно ко всем дням кампании; при смене культуры сбрасывается.
Проверить клавиатуру телефона, крупный шрифт/маленький экран, таймеры и
напоминания при блокировке; реальные пользовательские данные/телефон не затрагивались.

Основная APK `ru.fo6osik.workjournal`, TEST `ru.fo6osik.workjournal.debugtest`.
Для установки TEST рядом с основной не удалять/переустанавливать основную.
Путь загрузки: будущий Actions run → Artifacts → app-TEST-* → ZIP → app-debugTest.apk.

PR: #23, https://github.com/Arch-relic/AgroNaliz/pull/23 — Draft, открыт в main, не объединён.
Незавершённое: облачная сборка, исправление найденных ошибок, проверка APK,
обновление Draft PR и этого отчёта точными ссылками/SHA/числами.
