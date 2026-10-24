Актуальный этап: AGN-49, синхронизация Draft PR #49 с main, 24.10.2026; отчёт в конце.

Актуальный этап: Issue48 / Draft PR49, MAIN1.0.11; новый отчёт в конце.

Актуальный этап: PR47, вариант1 отзыва полномочий по решению владельца21.10.2026; новый отчёт в конце.

Актуальный этап: повторный аудит и синхронизация Agent Core, Draft PR #47, 21.10.2026; отчёт в конце.

Актуальный этап: Agent Core v1.0, этап 1, Draft PR #47; итог и ограничения в конце документа.

Актуальный этап: issue44 / Draft PR45, безопасное MAIN1.0.10 Room24→24; итог в конце. Предыдущая история сохранена.

Актуальный этап: issue #42, Draft PR #43; итог в конце документа. Предыдущие записи исторические.

Актуальный этап: подготовка безопасного MAIN 1.0.9, Draft PR #41.
Итоговый отчёт и ограничения находятся в конце документа; прежняя история сохранена.

Актуальное дополнение: этап 5, Draft PR #39. Предыдущая история сохранена ниже;
последние результаты находятся в разделе «АгроНализ 1.0.9 — этап 5».

# АгроНализ1.0.9 — этап4: ползунки и Room24

Согласованное расширение Room23→24 после preflight, ветка feature/harvest-1-0-9-sliders.
База main861024e7852c16adbd733eca84e4ef11f53a32be, tree3f4c461290ce221ceef52c2b1d43c703b6abfe27,
с PR35/36/37. Локальный f059c36 имеет идентичное дерево. Чистое начало, публикация
GitHub API с parent актуального main (локальный git proxy8080 недоступен).
Draft PR: https://github.com/Arch-relic/AgroNaliz/pull/38 (не объединён).
Дата отчёта: 19.10.2026. Проверяемый код:27253e342a60dc09686653dbffa2041c91c2924f,
tree08b0e6c129ecf31f0db0c69111ebe50cf2aeede6 (локальный96f6e10 — то же дерево).
Следующий отчётный коммит изменяет только этот документ, не код/проверки.

## Реализация

Room24: reminderStepMinutes INTEGER NOT NULL DEFAULT1 в harvest_days, явная
MIGRATION_23_24 зарегистрирована после всех прежних. Никакого destructive fallback.
Новое поле добавлено последним параметром entity, старые колонки/конструкторы
сохранены. Для каждого дня собственный шаг, новый/старый мигрированный день=1.
DAO.saveSliderSettings сохраняет5интервалов и шаг одной транзакцией. Существующий
updateDayReminderSettings не изменён. Старые Custom флаги при неизменённых
значениях сохраняются; при новом значении отражают прежний смысл preset/custom,
UI от них не зависит. Исторические значения вне новых диапазонов сохраняются
без clamping при открытии/неподвижном сохранении.

5стандартных SeekBar, общий шаг1/5, сетка1,5,10...max. Переключение шага не
изменяет значение32; округляется только пользовательское движение. Текущие
значения/пример сумм обновляются сразу, Room только на Save, Cancel без записи.
Диалог ScrollView, labels multiline, крупные touch targets. Напоминания toggle,
ограничения завершённого дня и старый notification save-path сохранены. Сам
HarvestReminderManager/таймеры/прошлые события не изменены.
Плотность320..900/шаг5 независим от времени. Каталог MIN/AVERAGE/MAX берётся
точно; CUSTOM SeekBar. Старая дробная/вне диапазона плотность остаётся точной,
без прикосновения Save не нормализует её. Новая плотность только по явному
выбору, через старый DAO, бункеры не изменяются. Для Другого доступен CUSTOM.

Backup JSON formatVersion не менялся: новый столбец экспортируется общим SQL
экспортом. Full restore24 читает18..23 и24; merge сохраняет прежние16,18..22 и
добавляет23/24. Старые копии получают step1, повторный импорт не перезаписывает
пользовательский шаг. Валидация шага1/5 до изменения/очистки базы; неверные
структуры отклоняются, transaction rollback сохраняется. Room24-копии нельзя
восстанавливать старой Room23 — это штатный запрет downgrade, не обещать обратное.

## Защита MAIN

main-apk-publish.yml байт-в-байт неизменён, SHA256
27a24a645fd97a6b7ec6d99c8a18e466196823f0023d57eba444c368599c7d91.
Новый Python negative test требует отказа publisher на реальной Room24.
Исторический sandbox одобренного Room23 патча использует frozen version-test
fixture и schema23 только в TemporaryDirectory: прежние manifest hashes/guards
не меняются, production допуск не расширен. Live version tests требуют24.

Отдельный readonly room24-update-checks.yml: pinned MAIN/TEST Room23 APK из861024e,
current synthetic Room24 APK, original applicationId, строго возрастающие
CI-only versionCode flags и общий disposable debug certificate (production pin
явно запрещён). Fresh emulator guards, сравнение всех старых столбцов/таблиц,
foreign keys/active states/fuel/events/files/preferences/certificate, отдельный
restore и restart. Ни Secrets, ни APK upload, ни release. Исходные versionName/
versionCode/build/signatures не менялись. Старые CI обновляют22/21/20→current24;
исторические имена тестов/шагов сохранены, проверка текущей схемы стала24.
Production MAIN остаётся заблокирована до отдельного решения по политике.

## Файлы

- .github/workflows/room24-update-checks.yml
- app/src/androidTest/java/ru/fo6osik/workjournal/AppVersionUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestAreaUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFuelTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFullBackup108Test.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestMigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestRoom23MigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestSlidersUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/MainRoom23MigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/MainUpdateTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/Room24UpdateTest.kt
- app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt
- app/src/main/java/ru/fo6osik/workjournal/DataImportManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseBackupManager.kt
- app/src/main/java/ru/fo6osik/workjournal/DatabaseProvider.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestBackupCompatibility.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDao.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDay.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDayActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestSliderRules.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestValueSliderView.kt
- app/src/main/res/layout/dialog_harvest_day_settings.xml
- app/src/test/java/ru/fo6osik/workjournal/HarvestCampaignMetricsTest.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestSliderRulesTest.kt
- docs/codex-handoff.md
- docs/export-installed-test-backup.py
- docs/test_test_backup_export.py
- docs/fixtures/room23-app-version.txt
- docs/run-harvest-108-checks.py
- docs/run-room24-update-checks.py
- docs/test_app_version.py
- docs/test_main_room23_policy_patch.py

Удалённых файлов нет. Calendar/results card/утверждённые расчёты не изменены.
Широкий diff обусловлен заменой пресетов и поддержкой миграции/backup/тестов,
не рефакторингом других функций.

## Проверки и следующий этап

Локально python3 -m unittest discover -s docs -p 'test_*.py':85/83pass/2SDKskip,
вложенные24 legacy tests не суммировать (23pass/1SDKskip). Diff whitespace clean.
agrogradle :app:assembleDebug :app:test не стартовал: FileLockContentionHandler /
Could not determine a usable wildcard IP for this machine. Конфигурация проекта
ради локальной среды не менялась. Android/JVM фактически выполняются в CI.

На предыдущем коде11ff454: JVM168/168 в MAIN и TEST, успешно прошли MAIN safe
update, MAIN22→24, новый MAIN/TEST23→24 audit и policy checks. Последний Android
прогон39218340966 прошёл99 инструментальных тестов (45 вызовов), включая5 новых
UI-тестов, повтор с увеличенным шрифтом и7 полных backup-тестов, затем остановился:
read-only export-installed-test-backup.py ещё разрешал только Room20..23.
Исправление27253e3 добавляет24 с обязательным integer step1/5 и прежним запретом
downgrade; новые2 Python-теста покрывают допустимые/недопустимые значения.
Скрипт остаётся только чтением по adb run-as, без записи на устройства.

Ранее исправлены две подтверждённые причины падений тестового окружения:
- audit39216905207: TEST backup destination file:// запрещён существующей
  BackupStoragePolicy. Исправлена только fixture: PrivateTestBackups destination.
  Защита приложения не менялась; последующие MAIN/TEST audit прошли.
- Android39216412413: Espresso click5мин при font1.6/720x960 не видел90%контрола.
  Тест прокручивает ScrollView к контролу, ограничения click/проверки сохранены.
  Последующий39218340966 прошёл этот сценарий и весь HarvestSlidersUiTest.

Полный повтор CI для27253e3 (PR merge2e97a2d3f6c34b60316f3822b7f0ff49ce1a74a0
имеет то же дерево08b0e6c...):
- Android PR checks #127: https://github.com/Arch-relic/AgroNaliz/actions/runs/39220430531 — success;104инструментальных/50вызовов,49Python.
- MAIN safe update #50: https://github.com/Arch-relic/AgroNaliz/actions/runs/39220430578 — success;3инструментальных,26Python.
- MAIN Room22→current #20: https://github.com/Arch-relic/AgroNaliz/actions/runs/39220430499 — success;3инструментальных.
- Room24 synthetic MAIN TEST audit #6: https://github.com/Arch-relic/AgroNaliz/actions/runs/39220430524 — success;6инструментальных.
- Detached policy checks #10: https://github.com/Arch-relic/AgroNaliz/actions/runs/39220430611 — success.

XML нового audit: testDebugUnitTest168/168 и testDebugTestUnitTest168/168,
0errors/0failures/0skips. Не считать повторные прогоны уникальными сценариями.
Артефакт12171338532 содержит verification.json и диагностику,0APK. Metadata:
sourceSha2e97a2d...; MAIN23/code3000004→24/code3000005, TEST23/code1000201→
24/code1000202; dataRetained/separateRestore/restart=true для обоих.
Коды только CI flags, не изменение версий исходников. Ephemeral certificate
338D8AFB5D610C3B60B1868BD804ADCDBA582D81A78D0146CA03696514C853EE,
productionReleaseAuthorized=false. Это не оригинальный MAIN certificate и не
подтверждение допуска production подписи. Исторические названия шагов MAIN22→23
сохранены, фактические assertions требуют currentRoom24.

Всего116 инструментальных выполнений:104+3+3+6, все успешны.
Android: HarvestSlidersUiTest5/5 плюс1 повтор при font1.6/720x960;
HarvestFullBackup108Test7/7; TEST SIGNING SUCCESS подтверждает совместимый
synthetic update, отказ при другом ключе без потери данных, read-only JSON export
и restore. Android49Python+MAIN26Python=75, policy job отдельно10; вложенные
sandbox tests не считать уникальными дополнительно. Android diagnostics12171813987,
без APK; installable artifacts/publish workflows не запускались.

Ручной смартфон не проверяется/не затрагивается; все fixture data синтетические.
Новый риск потери данных не обнаружен. MAIN production Room24 не разрешена:
потребуется отдельное согласование политики, которая здесь не изменена.
После CI требуется review, не merge/APK publication/настоящее MAIN signing.

---

# Дополнение от 19.10.2026 — инструкции Codex / Karpathy Skills (Draft PR #40)

Цель: дополнить `AGENTS.md` принципами предварительного анализа, простоты,
точечных изменений и проверяемого результата с отдельной защитой Room,
резервных копий, пользовательских данных и подписи APK.

Ветка: `docs/codex-karpathy-guidelines`. Опорный коммит main при сравнении PR:
`113d5c0e01b7d436ac119ebc752d9039ad03a35a`.
Проверенный коммит с изменениями инструкций:
`ce0b6d327d43ef3d13e829aefefa68d6984de03f`.
Последующие коммиты изменяли только отчёт; SHA самого отчётного коммита
не фиксируется внутри него.

Изменённые файлы: `AGENTS.md`, `docs/codex-handoff.md`.
Созданных и удалённых файлов нет. Исходный код Android, Room, CI,
подпись APK и правила main не менялись.

Проверки: прочитан исходный `AGENTS.md` из main; проверен diff PR #40,
включая полный перечень файлов. Все прежние строки `AGENTS.md` сохранены,
добавлена 61 строка. Исходный отчёт о Room24 сохранён выше без изменений.
Android-сборка и тесты не запускались, поскольку изменена документация.
Для последнего коммита GitHub combined statuses не вернул записей;
это не подтверждает успешный CI. На смартфоне не проверялось.

Ограничения: вложенные `AGENTS.md` не проверены исчерпывающим обходом.
Не выполнено практическое испытание новых инструкций на отдельной задаче Codex.

Draft PR #40: https://github.com/Arch-relic/AgroNaliz/pull/40
Состояние: открыт для проверки, не объединён. Следующий шаг: финальная
проверка diff, затем отдельное решение пользователя о слиянии с main.


---

# АгроНализ 1.0.9 — этап 5: история изменений и предрелизная проверка PR #39

Дата: 19.10.2026. Ветка: `feature/harvest-1-0-9-changelog`.
PR: https://github.com/Arch-relic/AgroNaliz/pull/39 — Draft, не объединён.
Статус: реализация и предрелизная проверка завершены, все пять заданий CI успешны.
Остановлено для review; PR остаётся Draft.

## База, история и проверяемый результат

Прочитаны актуальные `AGENTS.md` и полный `docs/codex-handoff.md` из main.
Вложенных AGENTS.md не найдено (`rg --files --hidden -g AGENTS.md -g '!.git'`).
Начальная база этапа: `113d5c0e01b7d436ac119ebc752d9039ad03a35a`.
Актуальная база после PR #40: `874aebf7c17f88541fd8cd604d612d43fb3ebd15`.
После начальной базы main изменила только инструкции и отчёт; PR #35–38 уже включены.

Синхронизация выполнена настоящим merge-коммитом
`9408b568ac6c84353853e97a891465234efb20f1` с родителями
`8aa4f18a67e57e54760f5093c89bd4d4abb342db` и
`874aebf7c17f88541fd8cd604d612d43fb3ebd15`.
История не переписана, force/rebase не применялись. Main не изменялась.
AGENTS.md соответствует main и не входит в итоговый diff PR.
Полная прежняя история handoff сохранена выше дословно; её статусы исторические:
PR #38 и #40 впоследствии объединены владельцем, PR #39 остаётся Draft.

Проверенный код: `2dd2c335e06b25285977ae29814eb99e0cfab305`.
CI проверил PR merge `048e65d8ceea5c8912984384d14ae5b60f333f87`;
дерево обоих коммитов идентично: `da3f195bab4161565661b318db0ca1c4115ca264`.
Локальный служебный коммит `5dcaad4` имеет то же дерево; публикация через GitHub API,
поскольку git fetch в этой среде не работает (proxy port 8080 unavailable).
Отчёт сохраняется следующим коммитом только документации: его SHA не следует
выдавать за новый проверенный Android-код. Незакоммиченных функциональных правок нет.

## Что реализовано

Кнопка «История изменений» добавлена в существующий блок «Для разработчика»
экрана «Опции». Отдельный неэкспортируемый ChangelogActivity использует стандартные
ScrollView, FrameLayout, LinearLayout, Button и TextView, без новых зависимостей.

Данные находятся в локальной Kotlin-модели AppChangelog. Экран не обращается
к Room, сети, файлам или preferences. Интернет не требуется; у приложения нет
INTERNET permission, это проверяет инструментальный тест. Записи не обновляются
при запуске. Правила добавления версий и источники: [changelog-history-sources.md](changelog-history-sources.md).

Версии идут от новых к старым. Блоки раскрываются независимо, состояние включая
полностью свёрнутый экран хранится в Bundle и восстанавливается при пересоздании.
По умолчанию раскрывается только запись, совпадающая с BuildConfig.VERSION_NAME.
Он остаётся 1.0.8; будущая 1.0.9 свёрнута и явно помечена «В разработке».
Версия Room не участвует в определении версии приложения.
Пустые категории и отсутствующие даты не выводятся. Текст переносится,
контент прокручивается на компактном экране с увеличенным шрифтом.

Добавленные записи:
- **1.0.9, «В разработке»**: расчёты кампании, календарь, карточка результатов,
  ползунки времени/плотности, история изменений. Подтверждены PR #35–38 и этим PR.
- **1.0.8**: единое завершение дня, продолжение уборки, независимые площади,
  общий бэкап, изменения «Опций»; PR #33 и документация проверки 1.0.8.
- **1.0.7**: учёт топлива/заправок и исправление TimePicker; docs/version-history.md.
- **«Ранние этапы разработки»**: подтверждённые основные разделы из того же
  документа, без придуманных номеров и дат; запись явно неполная.

Достоверных дат выпуска нет: все releaseDate=null. Время merge/CI не используется
как дата релиза. Технические коммиты не выдаются за пользовательские изменения.

## Диагностика и исправление

Android #130 (39226210319) и #131 (39228168537) падали при scrollTo к 1.0.7
после раскрытия 1.0.9. Гипотеза ожидания кадра не подтвердилась: PreDraw не помог.

Focused UI #1 (39238865915) вывел геометрию в instrumentation stream:
layoutRequested=false, content=3262 px, viewport=1773 px, scrollY=1321.
Кнопка 1.0.7 высотой 126 px была видна только на 63 px (Rect 1794..1857).
Причина в новом экране: fitsSystemWindows на ScrollView оставлял системные
отступы внутри viewport; прокрутка считала кнопку видимой, хотя половина обрезалась.

Исправление `627f3a9`: fitsSystemWindows перенесён во внешний FrameLayout,
как в существующем экране «Опции». ScrollView теперь измеряется в реально
доступном пространстве. Ненужное ожидание PreDraw/latch удалено.
Обычные Espresso scrollTo/click/visibility сохранены, retry/пропусков нет.
Диагностический dump остаётся только при ошибке и содержит статический UI.
Focused UI #2 и весь Android #137 на этом исправлении успешно прошли.

Последующий `2dd2c33` меняет только тест: перед просмотром создаётся собственная
синтетическая кампания, затем сравниваются все таблицы до/после просмотра и
пересоздания Activity. В finally удаляется только запись этого теста.
Сценарий прошёл в focused UI #3 и в полном Android #138 на том же коммите.

## Изменённые файлы относительно актуальной main

- `.github/workflows/android-pr-checks.yml`
- `.github/workflows/changelog-checks.yml`
- `app/src/androidTest/java/ru/fo6osik/workjournal/ChangelogUiTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/ru/fo6osik/workjournal/AppChangelog.kt`
- `app/src/main/java/ru/fo6osik/workjournal/ChangelogActivity.kt`
- `app/src/main/java/ru/fo6osik/workjournal/OptionsActivity.kt`
- `app/src/main/res/layout/activity_options.xml`
- `app/src/main/res/values/ids.xml`
- `app/src/test/java/ru/fo6osik/workjournal/AppChangelogTest.kt`
- `docs/changelog-history-sources.md`
- `docs/codex-handoff.md`
- `docs/run-changelog-checks.py`
- `docs/run-harvest-108-checks.py`

Всего 14 файлов: 8 новых и 6 изменённых. Удалённых файлов нет.
Изменение android-pr-checks.yml добавляет только каталог PNG в диагностический
артефакт. Новый focused workflow ускоряет проверку именно этого экрана;
все прежние общие проверки продолжают выполняться.

## Проверки

Локально:
- `python3 -m unittest discover -s docs -p 'test_*.py'`: 85 тестов,
  83 успешных, 2 пропуска из-за отсутствующих SDK-инструментов.
  Вложенные sandbox-тесты не считаются дополнительными уникальными сценариями.
- Реальный Kotlin 2.0.21 / JUnit 4.13.2 из установленного Gradle distribution:
  K2JVMCompiler компилирует AppChangelog.kt и AppChangelogTest.kt в новый
  TemporaryDirectory, затем JUnitCore запускает AppChangelogTest: **4/4 успешно**.
  Это новый запуск, не UP-TO-DATE; не заменяет сборку Android.
- `python3 -m py_compile docs/run-changelog-checks.py docs/run-harvest-108-checks.py`
  и `git diff --check`: успешно.
- `source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test`
  повторно завершается до компиляции с exit 1:
  FileLockContentionHandler / Could not determine a usable wildcard IP for this machine.
  Конфигурация проекта ради локальной среды не менялась.

CI на `2dd2c33`:
- [Android PR checks #138](https://github.com/Arch-relic/AgroNaliz/actions/runs/39240658911):
  success, **107 инструментальных / 53 запуска**, **49 Python**.
  Все 3 ChangelogUiTest, прежние HarvestSlidersUiTest (5 + повтор при большом
  шрифте), HarvestFullBackup108Test (7) и TEST signing/update проверки прошли.
- [Offline changelog UI #3](https://github.com/Arch-relic/AgroNaliz/actions/runs/39240658929):
  success, **3/3** — навигация/независимость раскрытия/восстановление,
  offline и неизменность непустой БД, компактный экран и font_scale=1.6.
- [MAIN safe update #59](https://github.com/Arch-relic/AgroNaliz/actions/runs/39240658888):
  success, **3 инструментальных**, **26 Python**.
- [MAIN Room22→current #29](https://github.com/Arch-relic/AgroNaliz/actions/runs/39240658880):
  success, **3 инструментальных**.
- [Room23→24 MAIN/TEST #15](https://github.com/Arch-relic/AgroNaliz/actions/runs/39240658909):
  success, **6 инструментальных**, сохранность данных/связей/файлов/preferences,
  отдельное восстановление и restart для обоих синтетических пакетов.

XML последнего аудита (артефакт 12180904016): **172/172 JVM** в каждом варианте
testDebugUnitTest и testDebugTestUnitTest, 0 ошибок/падений/пропусков.
Новые 4 JVM-сценария проверяют порядок/ключи, неизвестные даты/отделение Room,
статус разработки/категории и независимое раскрытие/восстановление.
Повторные прогоны одного набора не считаются уникальными тестами.
Всего 122 инструментальных выполнения: 107 + 3 + 3 + 6 + 3; последние 3 —
повтор сценариев changelog в отдельном focused workflow, не новые уникальные тесты.
Android и MAIN jobs также выполнили 75 Python-проверок (49 + 26).
Артефакт Android 12182550297 содержит отчёты и PNG; APK не публиковались.

verification.json: sourceSha=048e65d8..., dataRetained/separateRestore/restart=true
для обоих пакетов, productionReleaseAuthorized=false. CI-only versionCode flags
не меняют исходники. Ключи одноразовые, production certificate запрещён аудитором.
Исторические названия некоторых migration workflows содержат Room21/23;
фактические current assertions проверяют Room24.

## Скриншот

[ZIP со скриншотом на коммите 2dd2c33](https://github.com/Arch-relic/AgroNaliz/actions/runs/39240658929/artifacts/12181835893)
содержит только history.png, без APK; срок хранения до 26.10.2026.
Снимок 720×960 / font_scale=1.6 извлечён и визуально просмотрен.
Полное название и текст блоков переносятся; нижние записи доступны прокруткой.
Стандартная верхняя ActionBar сокращает длинный заголовок при таком шрифте,
поэтому полное название также размещено в прокручиваемом содержимом.

## Границы, риски и следующий шаг

Room24, миграции, импорт/экспорт/восстановление, backup-пути и TEST-защита,
таймеры/напоминания, расчёты, календарь/карточка результатов, ползунки,
versionName/versionCode, applicationId и подписи в итоговом diff отсутствуют.
`main-apk-publish.yml` неизменён, SHA-256:
`27a24a645fd97a6b7ec6d99c8a18e466196823f0023d57eba444c368599c7d91`.
Другие production/signing workflows тоже не изменены. Публикация APK при PR
остаётся запрещена; артефакты содержат только отчёты/метаданные/PNG.

Исторические записи неполные, даты неизвестны — владелец может дополнить их позднее.
На физическом смартфоне не проверялось; все установки и данные только в
одноразовых CI-эмуляторах. Локальная Gradle-сборка заблокирована средой;
реальные Android-сборки/JVM выполнялись через CI.
Production-выпуск MAIN с Room24 по-прежнему требует отдельного согласования
политики; PR #39 её не расширяет и не является разрешением на выпуск.

Полный CI завершён. После этого отчётного коммита остановиться для review. Не выполнять merge,
публикацию APK/Release, настоящее production signing или действия с реальными данными.

---

# Подготовка безопасного MAIN 1.0.9 — 19.10.2026

Ветка feature/harvest-1-0-9-release-preparation. База main
8b8e6dfac0d2f42946006461e6cbb094b938996d, tree87d2bd41bf3b9033e4c50e0657a03b3b24dbae1f.
Проверены фактические main/AGENTS/handoff; PR35–40 уже объединены владельцем.
Локальный4d2aaa7 — то же дерево; из-за недоступного proxy8080 git fetch не работает,
публикация через GitHub API с родителем актуальной main. Прежняя история сохранена.
Статус первоначальной записи: подготовлен код; окончательные результаты и Draft PR #41 — в итоговом дополнении ниже.

## Аудит и изменения

Подтверждённый опубликованный MAIN1.0.8 — run39044119058 (успех), code3000005,
Room23, sourcead66e4b452ec3c93866f4f6b8163087274166d45. Run4 неуспешен.
Прежняя документация о baseline3000004 была неполной: фактический код5 проверен
в существующем verification.json, SHA APK, aapt2 и apksigner без установки/ключа.
applicationId ru.fo6osik.workjournal; сертификат
3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C;
SHA старой APK a44cc8b0e1ba2029c7110b16548723c94d452ff7f5f37108d4da7eff34e0b47c.
Текущее состояние смартфона/Secrets не проверено и не подменяется историей Actions.

versionName1.0.9, без изменения versionCode/default1/формулы3000000+run_number,
applicationId/сертификата/MAIN-TEST изоляции. Changelog автоматически раскрывает1.0.9,
оставляет «В разработке», без даты выпуска; соответствующие UI expectations обновлены.

MAIN publisher расширен только в Draft ветке: target24 требует source23,
installed code>=3000005, product1.0.9, точный SHA, owner/main/manual/attempt1,
успех audit workflow389125371 room24-update-checks.yml/job room24-update с успешными
build current/baseline/update-integrity-restore-restart шагами. PR-run, неверный SHA,
actor/status/attempt/fork/неполные jobs/steps отвергаются. Прежние22/23 ветви сохранены.
Signer проверяет оригинальный pin, MAIN id до декодирования ключа (TEST запрещён),
версию/code/Room/source SHA, временные ключи0700/0600/cleanup, secret-isolated sign job.
Исходные секреты и pin не менялись, настоящий workflow не запускался.

Исторический approved Room23 patch/manifest не изменяется: frozen copies его4целевых
файлов проверяются в /tmp, с sandbox versionName1.0.8/Room23. Текущая live политика
проверяется отдельными10тестами, включая регрессии22/23. Это не ослабление admission.

Существующий audit23→24 использован повторно, синтетический MAIN1.0.8/code3000005→
1.0.9/code3000006, TEST отдельно. Новые ТО/сезонные записи/связи дополняют прежние
кампании/дни/бункеры/топливо/события; сравнение всех старых колонок/ID после update
и restart, step1, timer prefs, внутренние/внешние документы, отдельный restore,
открытие текущего Changelog/карточки результатов/календаря после миграции.
Оригинальный keystore не используется, только disposable CI cert, APK не загружаются.
Старые UI/полный backup7тестов/merge23/repeat/corrupt rollback сохранены без переписывания.
Room24, migrations, backup format/production data logic не менялись.

Расширена read-only проверка будущей APK: только явные22/23/24 пары, original cert,
code/hash/package и exact audit metadata23/24. Ничего не устанавливает и не читает БД.
План дальнейшего выпуска и backup — docs/main-original-update.md (только после согласования).

## Проверки и ограничения

Локально Python96:94pass/2SDKskip; вложенные исторические sandbox tests не суммировать.
Gradle source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test
не доходит до компиляции: FileLockContentionHandler / Could not determine a usable wildcard IP.
Конфигурация проекта ради окружения не меняется. Реальные сборки/JVM/UI/migration
результаты должны быть получены из CI, пока не объявлены успешными.
История release1.0.8 подтверждена, наличие/содержимое Secrets сейчас не проверялось;
не обещать выпуск без owner/main audit точного будущего SHA и оригинального signer.
Физический смартфон, реальные данные, production signing/Release/публикация MAIN и merge
не выполнялись. Следующий шаг после CI: review Draft PR, не выпуск.

## Файлы этого этапа (относительно корня)

- `.github/workflows/main-apk-checks.yml`
- `.github/workflows/main-apk-publish.yml`
- `.github/workflows/main-policy-proposal-checks.yml`
- `.github/workflows/room24-update-checks.yml`
- `app/build.gradle.kts`
- `app/src/androidTest/java/ru/fo6osik/workjournal/AppVersionUiTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/ChangelogUiTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/Main109UpdateUiTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/MainRoom23MigrationTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/Room24UpdateTest.kt`
- `docs/codex-handoff.md`
- `docs/fixtures/room23-main-apk-publish.yml.txt`
- `docs/fixtures/room23-main-original-update.md.txt`
- `docs/fixtures/room23-test-main-apk-release.py.txt`
- `docs/main-original-update.md`
- `docs/run-main-room23-checks.py`
- `docs/run-room24-update-checks.py`
- `docs/test_app_version.py`
- `docs/test_main_apk_release.py`
- `docs/test_main_room23_policy_patch.py`
- `docs/test_main_room24_release.py`
- `docs/test_verify_main_update.py`
- `docs/verify-build-variants.py`
- `docs/verify-main-apk-update.py`
- `docs/verify-main-room23-inputs.py`
- `docs/version-history.md`

Удалённых файлов нет.

## Итог подготовки MAIN 1.0.9 — 19.10.2026

Draft PR #41: https://github.com/Arch-relic/AgroNaliz/pull/41,
ветка `feature/harvest-1-0-9-release-preparation`, base main
`8b8e6dfac0d2f42946006461e6cbb094b938996d`. PR35–40 включены в main.
Проверенный код: `393c9cf5cea501c2e943c7fcc083d5ce5d0dbfc2`,
дерево `013491663c776342f752a6b21bd162fff218d80b`.
CI использует PR merge SHA `fe4f655613a800638536be39afea52154721e031`:
его дерево проверено и совпадает с деревом указанного head. Это технический
merge-ref GitHub, а не объединение PR в main. Локальный коммит `1d8c84f`
имеет то же дерево. Следующий коммит меняет только данный отчёт, не проверенный код.

### Фактическая проверка обновления и резервных копий

Room24 audit #23, run39296012508:
https://github.com/Arch-relic/AgroNaliz/actions/runs/39296012508 — success.
Артефакт `room24-update-diagnostics`, id12207428538, проверен после скачивания:
APK нет; `room24-update-verification.json` содержит точный CI SHA выше,
для обоих пакетов `dataRetained=true`, `separateRestore=true`, `restart=true`,
`productionReleaseAuthorized=false`.

Проверен синтетический MAIN1.0.8/code3000005/Room23 из опубликованного исходного
SHA `ad66e4b452ec3c93866f4f6b8163087274166d45` → MAIN1.0.9/code3000006/Room24.
TEST проверен независимо:1000201→1000202, id с `.debugtest`.
Обе пары используют одноразовый CI-сертификат
`B598D1B27B0EC26E5A01C91783D903BC02E6B36A9911DE4DA1E232BCFC5B6745`,
отличный от оригинального; проверено его совпадение до/после обновления.
Это не production-подпись и не назначение будущего production versionCode.

Всего10 инструментальных вызовов этого audit: seed23, проверка после install-r,
после force-stop, открытие экранов1.0.9 и повторная проверка данных после UI —
для каждого пакета. Сравнены все старые таблицы/строки/колонки/ID и внешние ключи:
техника/ТО, поля/связи, сезонные работы/дни, события, активная и завершённая уборка,
дни/бункеры/события после бункеров, влажность/плотность, топливо и обе области заправок.
Сохранены32/9/22/23/41 минут, Custom-флаги, старое значение плотности555.125,
у прежних дней `reminderStepMinutes=1`. Сохранены настройки напоминаний,
внутренние/внешние тестовые документы и preferences. Доставка реального уведомления
ОС на смартфоне не проверялась. В отдельной in-memory БД успешно восстановлен
JSON Room23, выполнены экспорт/восстановление Room24 и сравнение записей.
Рабочая БД не очищалась и не использовалась для тестового восстановления.

После обновления открыты главный экран с продолжением уборки, история изменений
(1.0.9 раскрыта, «В разработке»), карточка результатов и календарь.
Производственные модели/DAO/миграции/backup-код/расчёты/таймеры/UI не изменены.
Единственное изменение приложения вне тестов — versionName1.0.8→1.0.9;
default versionCode1, формула Actions, package и подпись сохранены.

### CI и команды

XML из диагностики audit: `testDebugUnitTest`172/172 и
`testDebugTestUnitTest`172/172, в каждом0failures/0errors/0skipped;
13suite в каждом варианте. Это реальные выполненные CI-тесты, не локальный Gradle.

Все шесть CI завершились успешно на указанном коде; дополнительно к Room24 audit:

- Android PR checks #147, run39296012462 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39296012462.
  Сборки MAIN/TEST и instrumentation APK; Python13catalog+7version+29TEST-signer;
  107инструментальных тестовых выполнений в53вызовах. Включены Harvest108Test5,
  HarvestFullBackup108Test7 (полный roundtrip, Room23/default1, повторный merge,
  сохранение локального шага, повреждённый/некорректный JSON и rollback),
  миграции старых20/21/22→current24, настройки/ползунки, календарь, карточка,
  история, быстрый доступ и MAIN/TEST isolation. Реальное обновление TEST
  временным одинаковым ключом сохраняет данные; Android отвергает иной ключ
  с INSTALL_FAILED_UPDATE_INCOMPATIBLE, данные остаются прежними.
  Артефакт12208033941 содержит только диагностику, APK-публикация на PR пропущена.

- MAIN safe update #67, run39296012481 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39296012481.
  Python18 signer +9 read-only, 3инструментальных вызова Room20→current→current.
  Историческое имя шага «Room20 to21 then21 to21» сохранено; текущая схема24.
- MAIN Room22 regression #37, run39296012454 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39296012454.
  3инструментальных вызова: старый MAIN1.0.7/3000003/Room22 → текущий Room24,
  сохранность/отдельное восстановление/restart. Историческое имя workflow
  «Room22 to23» не означает, что текущая схема23. Этот regression-run
  не разрешает production22→24: publisher разрешает только23→24 для1.0.9.
- Offline changelog #11, run39296012367 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39296012367.
  3UI-теста, включая offline/состояние/крупный шрифт и маленькое окно;
  опубликованы только скриншоты.
- Policy #18, run39296012533 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39296012533.
  10исторических +10live тестов. Вложенный архивный sandbox24теста
  (23pass/1SDKskip) не суммировать с верхнеуровневыми тестами.

Итого верхнеуровневых Python в CI:96 (49Android+27MAIN-safe+20policy), без ошибок.
Инструментальных выполнений во всех шести workflow:126 (107+10+3+3+3),
включая намеренные повторные проверки и focused UI; это не126уникальных методов.
Дублирующиеся JVM-прогоны разных workflow не складываются:172на каждый вариант.

Команды CI: `./gradlew :app:assembleDebug :app:assembleDebugTest
:app:assembleDebugAndroidTest :app:testDebugUnitTest :app:testDebugTestUnitTest`,
затем `-PtestBuildType=debugTest :app:assembleDebugTestAndroidTest`;
CI-only versionCode передаются workflow. Audit использует
`python3 docs/run-room24-update-checks.py`, остальные драйверы —
`docs/run-main-update-checks.py`, `docs/run-main-room23-checks.py`,
`docs/run-changelog-checks.py`. Политики:
`python3 -m unittest discover -s docs -p 'test_main_room23_policy_patch.py' -v`
и аналогично `test_main_room24_release.py`.

Локальный Python96:94passed/2SDKskipped; локальная Android-команда
`source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test`
завершилась до компиляции: FileLockContentionHandler /
Could not determine a usable wildcard IP for this machine. Для проверки Android
использованы приведённые CI, конфигурация приложения ради окружения не менялась.

### Найденные причины прежних падений

Android #143/run39293721768: `docs/verify-build-variants.py` ещё требовал literal1.0.8
при фактической новой1.0.9. Исправлено ожидание, все package/provider/isolation
assertions сохранены. Room24 #21/run39294744659: ещё до seed23 JUnit рефлексией
загружал методы тестового класса, чьи синтетические lambda-сигнатуры ссылались
на ChangelogActivity, отсутствующую в настоящей старой APK1.0.8. Это ошибка
совместимости тестового harness, не миграции: seed/запись ещё не начинались.
UI-тест вынесен в отдельный `Main109UpdateUiTest`, запускаемый только после update;
все UI assertions и последующее сравнение данных сохранены. Итоговый audit #23 зелёный.
Проверки не отключались и их строгость не уменьшалась.

### Политика, риски и следующий шаг

Положительные/отрицательные тесты исполняют inline preflight/signer из текущего
workflow с fake API/tools и временными ключами. Проверены owner/main/manual/attempt1,
отсутствующий/неуспешный/неверный SHA audit, чужой workflow/fork/actor,
неполные/пропущенные/упавшие шаги, неподдерживаемая Room/downgrade/низкий или
устаревший code, неверные сертификат/package/version/source SHA. TEST запрещён
ещё до декодирования ключа даже при общем историческом сертификате. Проверены
сохранение ветвей22/23, изоляция sign job, удаление временных ключей и отсутствие
выходного артефакта при отказе. Никакой настоящий production workflow не запускался.

Патч publisher находится только в Draft ветке; main и её защищённая политика
не изменены. SHA256 publisher на main:
`27a24a645fd97a6b7ec6d99c8a18e466196823f0023d57eba444c368599c7d91`,
в предлагаемом патче:
`e1ccb376204ea248cdde303b89f243a3592917fd3188c9e7c914560e8a9961fe`.
Формат backup и миграция23→24 остаются исходными. Прежние snapshots/patch-manifest
Room23 не изменены; их проверка перенесена в sandbox с frozen copies, live24
проверяется независимо. Полный список26изменённых файлов — выше; удалений нет.

Готовность: подготовлен кандидат для review, не разрешение production-выпуска.
После отдельного merge владельцем нужны CI и новый owner/main manual audit
точного итогового main SHA, затем отдельное разрешение на настоящий выпуск.
Порядок backup/проверки установленной APK/будущего signer/обновления —
`docs/main-original-update.md`. Код нового MAIN назначит прежний счётчик workflow,
он должен быть больше фактически установленного (подтверждённый выпуск8=3000005).
Не обещать произвольный production3000006: это только синтетический fixture.

Текущее наличие оригинального keystore/Secrets не проверено настоящей подписью;
метаданные установленного смартфона не считаны. Исторический original pin проверен
по подписанной1.0.8 и совпадает с прежним; MAIN/TEST имеют разные applicationId,
поэтому общий исторический cert не требует смены ключа. JSON-backup не является
копией произвольных файлов/preferences — перед будущим обновлением нужные документы
сохранять отдельно, backup держать вне приложения. Downgrade24→23 не поддержан.
Модельные данные и эмулятор не доказывают работу на физическом смартфоне.

Production APK не опубликована; Release не создан; production signing и оригинальный
keystore не использованы; реальные данные не затронуты; установки на физический
смартфон не было; PR не объединён. После отчёта остановиться для review.


# Issue #42 — интерфейс и диагностика установленной сборки, 20.10.2026

Задача: https://github.com/Arch-relic/AgroNaliz/issues/42. Ветка
`feature/issue-42-ui-diagnostics`, база main
`6da84bf09cf0cd3e9d1b5a40887a134bf25d7fe4`, tree
`de8717d46fc94b279f9a44ff1270f019fb54eb16` (совпадает с локальным efd7cf1).
Прочитаны актуальные main/AGENTS.md и handoff; прежняя история сохранена.
Статус этой первоначальной записи: реализация подготовлена, реальные результаты CI
и номер Draft PR будут добавлены по завершении, пока они не объявлены успешными.

Карточка результатов сохраняет все строки модели/форматирование/арифметику.
ReplacementSpan делает числа и составные единицы неделимыми при переносе.
Если две колонки не вмещают отформатированные показатели, используется одна колонка;
размер шрифта не уменьшается. Диапазон может переноситься между границами, число — нет.
Раскрытие/модель/расчёты не меняются. Главный заголовок и MAIN app_name — «АгроНализ»;
TEST сохраняет собственное имя «АгроНализ TEST». Package/имя файла БД не переименованы.

Options читает PackageManager только собственного установленного пакета: имя/код
версии, SHA256 всех текущих подписантов (не историческую signing lineage), API28+
GET_SIGNING_CERTIFICATES/apkContentsSigners, до28 GET_SIGNATURES. Полные отпечатки
сортируются/без дубликатов, доступны выделение и явная кнопка копирования. При ошибке
подпись/код неизвестны, кнопка выключена; имя может безопасно браться из BuildConfig
того же APK. Ключей/Secrets/сетевых запросов нет. Room schema берётся из генерируемой
BuildConfig.ROOM_SCHEMA_VERSION: Gradle читает фактический literal @Database.version
при сборке, без дублирующей ручной константы и без открытия/создания пользовательской БД.

Новые3 JVM-теста SHA256; отдельный UiPolishTest: реальные installed metadata/clipboard,
брендинг, пересоздание Options без изменения БД, ошибка metadata; переносы/отсутствие
обрезки на280/360/600dp и font1/1.6. Отдельный CI API23/35 проверяет MAIN и TEST.
Существующая проверка compactLargeFont карточки учитывает адаптивную компоновку и
дополнительно проверяет отсутствие разрыва внутри числа; остальные assertions сохранены.

Локально `python3 -m unittest discover -s docs -p 'test_*.py' -q`:96тестов,
94passed/2SDKskipped; вложенные архивные проверки не суммировать. Первая проверка
обнаружила несовместимость с прежним source guard BuildConfig.VERSION_NAME; оставлен
корректный fallback на compiled versionName при недоступных PackageManager metadata,
защитные тесты не менялись. Локальная обязательная команда
`source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test`
не доходит до компиляции: FileLockContentionHandler / Could not determine a usable
wildcard IP for this machine. Причина — среда, настройки проекта ради неё не менялись.
Android/JVM/instrumentation результаты должны быть получены из CI.

versionName1.0.9/default versionCode/Actions-механизм без изменений. Для будущего
выпуска нужен отдельный выбор версии и стандартный возрастающий код, аудит точного SHA
и разрешение; этот PR ничего не объявляет опубликованным. Подписи/production workflows,
Room24/миграции, backup, таймеры, расчёты и рабочие данные не изменяются.
Физический смартфон не проверялся. Production signing/publish/Release/merge запрещены.

Изменённые/созданные файлы (полные пути от корня):
- `.github/workflows/ui-polish-checks.yml`
- `app/build.gradle.kts`
- `app/src/main/java/ru/fo6osik/workjournal/HarvestResultsCardView.kt`
- `app/src/main/java/ru/fo6osik/workjournal/InstalledAppDiagnostics.kt`
- `app/src/main/java/ru/fo6osik/workjournal/OptionsActivity.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_options.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/ru/fo6osik/workjournal/InstalledAppDiagnosticsTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/UiPolishTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/HarvestResultsCardUiTest.kt`
- `docs/run-ui-polish-checks.py`
- `docs/codex-handoff.md`

Удалённых файлов нет. Итоговые SHA/CI/PR — следующим дополнением после проверки.

## Итог issue #42 — Draft PR #43, 20.10.2026

PR: https://github.com/Arch-relic/AgroNaliz/pull/43, open/Draft, не объединён.
Ветка `feature/issue-42-ui-diagnostics`; база main
`6da84bf09cf0cd3e9d1b5a40887a134bf25d7fe4`. Проверенный код:
`99f958f023e05140a17494959bd6999bfc903789`, tree
`afc53eddec5c674961d24649f8028da94e5ddd17`. Локальный c295ee5 имеет то же дерево.
CI проверяет технический merge-ref `d5fc1aab3535ea08f01ba7a141a7c44c203fde1b`,
дерево которого проверено и совпадает с head. Это не merge PR в main. Следующий
отчётный коммит меняет только docs/codex-handoff.md, не проверенный Android-код.

Все13путей этого этапа перечислены выше. Удалений нет; прежний main handoff сохранён
целиком. Код приложения не поднимает versionName1.0.9/default versionCode1, не меняет
Actions versionCode policy. В app/build.gradle.kts только6строк генерации диагностической
константы схемы по фактическому исходнику Room. AppDatabase/DatabaseProvider/миграции,
сущности/DAO, backup/импорт, арифметика/округление, события/напоминания/таймеры,
календарь и исходные production workflows не изменены. Сетевых запросов диагностика
не делает. Package/сертификаты/имя файла agrouchet_database остаются прежними.

### Проверенные UI-сценарии

Новая матрица API23 и35, run39394699765/#1 — success:
https://github.com/Arch-relic/AgroNaliz/actions/runs/39394699765.
В каждом API по6инструментальных выполнений (3сценария × MAIN/TEST), всего12:
- Фактические PackageManager versionName/versionCode/сертификат совпадают с экраном;
  полный SHA256 сравнивается независимым MessageDigest от установленной подписи.
  Копирование возвращает полный отпечаток без потерь; Options recreation сохраняет
  корректные сведения; MAIN label «АгроНализ», TEST label «АгроНализ TEST», главный
  заголовок совпадает с label. Снимок файлов БД до/после Options одинаков, новая БД
  не создаётся. Проверены обе ветви Android API23 GET_SIGNATURES и35 SigningInfo.
- SecurityException на запросе metadata безопасно возвращает unknown, без придуманного
  сертификата/кода. Отсутствующие сертификаты также покрыты JVM-тестом; UI выключает
  копирование и показывает локализованный текст ошибки. Имя версии при недоступных
  PackageManager metadata берётся из compiled BuildConfig, не из релизного literal.
- Числа22,28–22,43 и длинные12345678,12–12345679,34, отрицательная площадь, единицы,
  ширины280/360/600dp и font1/1.6: текст модели не изменился, у каждого числового
  токена и единицы одна строка, нет ellipsis/выхода строк за ширину/обрезки по высоте.
  Отформатированные диапазоны не усредняются, шрифт не уменьшается.

Матрица публикует только test-results, без APK. XML из артефакта12251397809
проверен:14suite,175tests/0failures/0errors/0skipped для testDebugUnitTest и
то же для testDebugTestUnitTest. Новые3 JVM-теста: полный известный SHA256 для abc,
несколько подписантов/deduplication/устойчивый порядок, отсутствие сертификата.

Зелёные дополнительные проверки:
- Android PR #150/run39394699877 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39394699877.
  53инструментальных вызова/107тестовых выполнений, включая обновлённую карточку,
  исходную сетку2×2, адаптивный compact/large font, Room refresh/recreation,
  сохранение выбора календаря, быстрый доступ, ползунки, полный backup/merge/repeat/
  corrupt rollback и TEST/MAIN isolation. Python13catalog+7version+29TEST signer=49.
  Реальное synthetic TEST обновление одинаковым временным ключом прошло;
  иной ключ отвергнут Android с сохранением данных. Upload debug APK — skipped,
  опубликован только android-pr6-diagnostics/id12251768789.
- Offline changelog #13/run39394699689:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39394699689. 3UI-выполнения:
  навигация из Options/независимое раскрытие/recreation, offline/no DB changes,
  маленький экран/крупный шрифт и скриншот.
- MAIN safe update #69/run39394699716:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39394699716.
  Python18signer+9read-only и3инструментальных вызова, данные/preferences/таймеры/файлы
  сохранены после update и restart. Только disposable CI key.
- MAIN Room22 regression #39/run39394699684:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39394699684.
  3инструментальных вызова старой1.0.7/Room22→current24: все старые таблицы/колонки,
  связи/статусы/топливо/события/файлы/preferences сохранены, отдельный restore и restart.
- Room24 synthetic MAIN/TEST #26/run39394699805 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39394699805.
  10вызовов: MAIN1.0.8/Room23→current24 и TEST отдельно; old data/files/preferences/
  certificate сохранены, step1/JSON separate restore/restart/UI1.0.9 проверены.
  Старые имена «Room22 to23»/«Room20 to21» — исторические имена проверок, текущая
  схема24. Они не означают новый production-допуск или использование original key.

Все6workflow успешны на проверенном коде (матрица содержит2jobs). Итого138
инструментальных выполнений:107Android+12UI-polish+10Room24+3MAIN-safe+
3Room22-regression+3changelog. Это включает намеренные повторения, а не138уникальных
методов. Python в CI76верхнеуровневых (49Android+27MAIN-safe), без ошибок;
архивная/live политика дополнительно проверена локальным набором96с2SDKskip.
Повторные JVM-прогоны разных workflow не складывать:175на каждый вариант.

Команды: стандартные workflow Gradle assembleDebug/assembleDebugTest/оба
assembleAndroidTest и testDebugUnitTest/testDebugTestUnitTest;
`python3 docs/run-ui-polish-checks.py` на свежем эмуляторе проверяет оба пакета,
отказывается при наличии чужой/старой установки и не использует production Secrets.
Локально `python3 -m unittest discover -s docs -p 'test_*.py' -q` —96tests,
94passed/2SDKskipped (вложенные архивные sandbox-проверки не суммировать).
`git diff --check` и XML parsing3разметок/ресурсов без ошибок.
Локальный Gradle не смог стартовать из-за FileLockContentionHandler/wildcard IP;
настройки проекта ради среды не менялись. git fetch также не работает из-за proxy8080,
поэтому публикация через GitHub API с parent фактического main и сверкой дерева.

### Ограничения и следующий шаг

Issue сообщает об успешной установке MAIN1.0.9 владельцем; это пользовательское
сведение, а не проведённая здесь проверка физического устройства. Смартфон в этой
задаче не подключался, реальные данные не читались/не менялись. Доставка уведомлений
и действия с рабочей БД на смартфоне не выполнялись. Оригинальный keystore не
использован; production signing/workflow/publish/Release не запускались, APK не
публиковались. PR43 не объединять без отдельного решения.

Стратегия будущей версии: этот UI PR сохраняет1.0.9 и не выпускает повторный релиз.
После review владелец отдельно выбирает номер следующей версии/допуск выпуска;
код назначает существующий MAIN workflow, строго выше фактически установленного.
Если понадобится адаптация production versionName gate, это отдельная согласованная
задача, не скрытая часть UI-патча. Перед публикацией нужен аудит точного итогового SHA.
Дополнительный подтверждённый барьер следующего выпуска: действующий publisher
при targetRoom24 требует installedRoom23. MAIN1.0.9 уже имеет Room24, поэтому
будущий выпуск поверх неё требует отдельно согласованного и проверенного допуска
Room24→24 (и выбранного product versionName). Это не дефект UI и не повод менять
production-политику в issue42; здесь она остаётся побайтно прежней.

Ручной review: проверить карточку с диапазоном/длинными числами на узком экране и
крупном шрифте; переключить подробности и календарь; в Options сверить код/Room/
SHA256 с системными сведениями и скопировать отпечаток; проверить сохранность
TEST-маркировки. Сейчас разрешения на APK/установку нет, такой тест не заявлен.


# Issue44 — подготовка MAIN1.0.10, Room24→24, 20.10.2026

Ветка feature/main-1-0-10-room24-update, base main
e77e6241885e8ecbe031aed475312b6664b5eb58/tree7a3ea68619d51b0519ca29eb540d2088d30f60ae
(локальный eda5aee — то же дерево). AGENTS/main handoff прочитаны, старая история сохранена.
Baseline подтверждён по успешному run39378839045/artifact12244079517: MAIN1.0.9,
code3000007, Room24, source6da84bf09cf0cd3e9d1b5a40887a134bf25d7fe4, исходный cert
3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C,
APK hash56ca5f9e54e94e773d1a053ad617a484e8a1c4f29f76f70ffa6a95d2d402d22b.
Проверены verification.json/aapt2/apksigner/исходник Room24. Оригинальная APK не
устанавливалась, original keystore не использовался. META-INF warnings apksigner
о неподписанных отдельных metadata entries не помешали успешной проверке подписи;
APK не изменялась. Git fetch/proxy и Gradle будут проверены отдельно.

Текущий статус первоначального коммита: Draft PR/регистрация нового workflow.
Новый gate временно fail-closed с workflowID0, пока GitHub не зарегистрирует новый
main-room24-same-checks.yml. После получения реального ID он будет закреплён и
выполнен полный финальный CI. Эта промежуточная версия не готова к выпуску/merge.
Версия1.0.10, схема24/миграции/backup/бизнес-логика не меняются. Новый seed24 сохраняет
step5/1, all-column JSON и schema_version/sqlite_master; upgrade без удаления,
отдельный restore/restart/MAIN и TEST. История1.0.10 «В разработке»,1.0.9 без
недостоверной даты и без прежнего статуса разработки (публикация подтверждена).
План смартфона/резервной копии/точного owner-main audit — docs/main-original-update.md.

Локально Python106:104pass/2SDKskip до регистрации ID. Прежние22/23/23→24 правила
проверяются в отдельных historical product sandboxes, их assertions не отключены.
Финальные команды/полные пути/SHA/PR/результаты CI будут добавлены по завершении.
Ни production signing, ни публикация, ни merge не разрешены и не выполнялись.


Регистрация завершена: Draft PR45 https://github.com/Arch-relic/AgroNaliz/pull/45.
GitHub зарегистрировал main-room24-same-checks.yml как workflow390524624. Этот ID
закреплён в live gate/signer/read-only verifier/tests, временный fail-closed0 удалён.
Теперь выполняется финальный CI, production signing по-прежнему не запускается.
Локальная Gradle-команда до компиляции падает с FileLockContentionHandler/wildcard IP;
git fetch — proxy8080 unavailable. Проект ради среды не меняется, публикация API.

## Итог подготовки MAIN1.0.10 — issue44, 20.10.2026

Draft PR45: https://github.com/Arch-relic/AgroNaliz/pull/45, open/Draft, не merge.
Ветка feature/main-1-0-10-room24-update; base main
`e77e6241885e8ecbe031aed475312b6664b5eb58`. Проверенный код
`ac8c251d10d15148967744ecb729cb1d89ddef4a`, tree
`7b257ce9a599c59ed0ed865ffea8c42e15140b59`; локальный347d1b0 имеет то же дерево.
Технический PR merge-ref CI `7e3ef404d004f9e3e8a12852099c5f3f001ead92` проверен:
его дерево совпадает с head. Это не объединение PR в main. Следующий отчётный
коммит меняет только docs/codex-handoff.md и docs/version-history.md, не код/CI.

### Результат и границы изменения

versionName1.0.10 (единый для MAIN/TEST), default versionCode1 и прежние отдельные
Actions-механизмы не изменены. Новый MAIN code назначит только publisher по
3000000+run_number; он должен быть строго выше фактически установленного.
В changelog1.0.10 «В разработке», без даты/утверждения о публикации;1.0.9 больше
не помечена разработкой, её опубликованный артефакт подтверждён, дата не выдумана.

Новая отдельная ветвь admission: targetRoom24 + product1.0.10 + installedRoom24
и code>=3000007. Нужны прежние owner/main/manual/attempt1, строго больший штатный
code и проверка последовательности предыдущих run. Audit workflow390524624,
path .github/workflows/main-room24-same-checks.yml, job room24-same-update, точный
head SHA публикуемого кода, оригинальный repo/head_repo, owner/triggering_owner,
workflow_dispatch/main/attempt1/completed/success. Все3обязательных build baseline/
build candidate/update-schema-data-restore-restart шага успешны; missing/ambiguous/
truncated jobs отвергаются. Прежние22/23/23→24 ветви сохранены и проверены отдельно.
Signer до ключа проверяет packageMAIN/product/baseline/floor/аудит/source SHA,
после подписи — original cert и повторно identity. TEST не допускается даже при
том же cert. Secret-isolated sign job/permissions/cleanup0700/0600 не изменены.

Ни AppDatabase/DatabaseProvider/миграции/Room24, ни entities/DAO/backup/импорт/формат,
ни таймеры/расчёты/поля/бункеры/события/календарь/ползунки не изменены. Из production
Kotlin только локальная редакционная история. Остальное — версия, предложенный
publisher, новая тестовая инфраструктура/ожидания текущей версии/документация.
Физический смартфон и реальные пользовательские данные не использовались.

### Подтверждённый baseline и синтетический аудит

Проверена опубликованная MAIN1.0.9/3000007/Room24/source6da84bf09cf0cd3e9d1b5a40887a134bf25d7fe4:
https://github.com/Arch-relic/AgroNaliz/actions/runs/39378839045, artifact12244079517.
Подпись и identity сверены отдельными subprocess(check=True): apksigner/aapt2 exit0;
SHAAPK совпал с verification.json. Публичные данные — docs/main109-baseline.json.
Не спутать с неуспешным publisher run6. Это не считывание текущего смартфона.

Новый audit #2/run39423864511 — success:
https://github.com/Arch-relic/AgroNaliz/actions/runs/39423864511.
Артефакт main-room24-same-diagnostics/id12264713908 скачан и проверен: APK нет;
main-room24-same-verification.json содержит точный CI SHA выше, publishedBaseline
с настоящими public metadata, schemaUnchanged=true, productionReleaseAuthorized=false;
для MAIN/TEST dataRetained/separateRestore/restart=true. Синтетический MAIN3000007→
3000008, TEST1000301→1000302. Оба сертификата до/после равны одноразовому CI pin
F59374CE45B20356910A31EFE6843B45D70EB5F9E0D72A72D33D716E5AE93821, он отличается
от original3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C.
Оригинальная APK не устанавливалась; baseline пересобран из pinned published source,
а не из текущего main. Оригинальный приватный ключ не требовался/не использовался.

10инструментальных вызовов (по5на пакет): seed24→install-r candidate→verify→force-stop
и verify→UI1.0.10→verify. Все старые таблицы/колонки/строки/ID и FK сравниваются с
JSON до обновления; техника/ТО/поля/связи/сезонные работы/события/активная и завершённая
уборка/дни/бункеры/влажность/плотность/топливо/заправки, интервалы/Custom flags и
шаг5/1 сохранены. schema_version и sqlite_master SQL до/после одинаковы: нет
пересоздания/миграции схемы. Сохранены внутренние/внешние документы и reminder prefs.
Экспорт Room24/restore в отдельную in-memory БД/roundtrip/restart прошли. Приложение
повторно открывает активную кампанию, историю1.0.10, результаты и календарь.

XML из того же артефакта:14suite/175tests для testDebugUnitTest и столько же
для testDebugTestUnitTest; в каждом0failures/0errors/0skipped. Это реальные CI
прогоны; повторные JVM-прогоны разных workflow не складываются в число уникальных тестов.

### Остальные проверки и команды

- Android PR #154/run39423864443 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39423864443.
  53инструментальных вызова/107выполнений; карта результатов/календарь/навигация/
  ползунки/быстрый доступ, Harvest108Test5 и FullBackup108Test7, roundtrip/merge/
  repeat/corrupt rollback/старые форматы, топливо/события и реальные synthetic TEST
  update same-key/different-key rejection с сохранением данных. Python49 (13+7+29).
  Upload debug APK skipped; только android-pr6-diagnostics/id12266286267.
- Policy #21/run39423864437 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39423864437.
  10frozen Room23 +10live Room23→24 +10live Room24→24 тестов. Вложенные исторические
  sandbox tests24 (одинSDKskip) не суммируются с30верхнеуровневыми. Проверены owner/
  ref/event/repo/attempt, wrong/no audit ID/SHA/workflow/path/actor/fork/status,
  failed/skipped/missing/truncated steps/jobs, wrong Room/code/product, out-of-order,
  TEST/signed TEST/wrong cert/source/package, cleanup и отсутствие output при отказе.
  Старые product sandboxes нормализуют только тестовые копии versionName и сохраняют
  все прежние assertions; frozen patch/manifest/snapshots не изменены.
- MAIN safe update #72/run39423864576 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39423864576.
  Python18signer+10read-only,3инструментальных вызова, включая повторный update/restart.
  Read-only verifier24→24 требует1.0.10/codefloor7/newauditID/exactSHA и original cert;
  прежние22/23/23→24 пары сохранены. Новый test отвергает старую версию/старыйaudit/
  wrong SHA/baseline. Скрипт не устанавливает APK и не открывает БД телефона.
- MAIN Room22 regression #42/run39423864491 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39423864491.
  3вызова old1.0.7/3000003→current1.0.10/Room24, сохранность/restore/restart.
  Его synthetic code3000004 не является будущим release code и не допускается
  production gate1.0.10 (нужен floor>=3000007). Имена старых workflow исторические.
- Room23→24 #29/run39423864510 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39423864510.
  10вызовов обоих пакетов, старые данные/файлы/preferences/defaultStep1/restore/restart.
- UI-polish #4/run39423864435 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39423864435.
  API23/35, оба пакета,12вызовов: installed metadata/полный cert/copy/recreation/no DB
  changes/error metadata/числа на280/360/600dp при font1/1.6.
- Changelog #16/run39423864430 — success:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39423864430.
  3UI-вызова: независимое раскрытие, текущая1.0.10, offline/no DB changes, recreation,
  маленький экран/крупный шрифт и screenshot.

Все8workflow на проверенном коде завершились success. Итого148инструментальных
выполнений (107+10+10+12+3+3+3), включая намеренные повторения, не148уникальных методов.
Python107верхнеуровневых в CI (49Android+28MAIN-safe+30policy), без ошибок;
вложенные архивные sandbox tests не суммируются, их1SDKskip явно отмечен выше.
В финальной локальной повторной проверке107tests/105pass/2SDKskip, ошибок нет.

Команды: ./gradlew assembleDebug/assembleDebugTest/оба assembleAndroidTest/
testDebugUnitTest/testDebugTestUnitTest с CI-only mainVersionCode/testVersionCode;
новый python3 docs/run-main-room24-same-checks.py; прежние run-room24-update-checks.py,
run-main-update-checks.py, run-main-room23-checks.py, run-ui-polish-checks.py,
run-changelog-checks.py и полный Android набор сохранены. Политики:
python3 -m unittest discover -s docs -p 'test_main_room24_same_release.py' -v
и старые23/24 release suites. Локально полный test_*.py:107tests,105pass/2SDKskip,
без ошибок (вложенные sandbox tests не суммировать). Обязательная локальная Gradle
команда не доходит до компиляции: FileLockContentionHandler / Could not determine
a usable wildcard IP. Git fetch не подключается к proxy8080, публикация API с
родителем фактического main и сверкой дерева. Настройки приложения ради среды не менялись.

### Файлы относительно корня (полный перечень)
- `.github/workflows/main-apk-publish.yml`
- `.github/workflows/main-policy-proposal-checks.yml`
- `.github/workflows/main-room24-same-checks.yml`
- `app/build.gradle.kts`
- `app/src/androidTest/java/ru/fo6osik/workjournal/AppVersionUiTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/ChangelogUiTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/Main109UpdateUiTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/MainRoom23MigrationTest.kt`
- `app/src/androidTest/java/ru/fo6osik/workjournal/Room24UpdateTest.kt`
- `app/src/main/java/ru/fo6osik/workjournal/AppChangelog.kt`
- `app/src/test/java/ru/fo6osik/workjournal/AppChangelogTest.kt`
- `docs/codex-handoff.md`
- `docs/main-original-update.md`
- `docs/main109-baseline.json`
- `docs/run-main-room23-checks.py`
- `docs/run-main-room24-same-checks.py`
- `docs/run-room24-update-checks.py`
- `docs/test_app_version.py`
- `docs/test_main_apk_release.py`
- `docs/test_main_room23_policy_patch.py`
- `docs/test_main_room24_release.py`
- `docs/test_main_room24_same_release.py`
- `docs/test_verify_main_update.py`
- `docs/verify-build-variants.py`
- `docs/verify-main-apk-update.py`
- `docs/verify-main-room23-inputs.py`
- `docs/version-history.md`

Всего27файлов; удалённых нет.

### Готовность, риски и следующий шаг

Подготовлено для review; выпуск не разрешён этим PR или зелёным PR-CI. После
отдельного merge нужен новый owner/main/manual/attempt1 аудит390524624 точного
итогового main SHA; потом отдельное разрешение на production workflow. Изменился
SHA — нужен новый аудит, даже если diff только документационный. Подробный план
backup/диагностики/dispatch/read-only verify/обычного обновления — docs/main-original-update.md.

Текущий installed code/сертификат смартфона и наличие original Secrets не проверялись
непосредственно: подтверждены опубликованные metadata1.0.9/code7. До будущего
выпуска сверить фактическую установку, MAIN JSON сохранить вне приложения; нужные
пользовательские документы отдельно (JSON не полная копия произвольных files/prefs).
В опубликованной1.0.9 ещё может не быть новой диагностики PR43: перед обновлением
использовать системные сведения/просмотрщик APK и Room24 из backup, не угадывать.
Не устанавливать CI fixtures, не удалять приложение, не clear, не выполнять downgrade.
Доставку уведомлений ОС на физическом телефоне CI не подтверждает; проверены
сохранение таймерных полей/preferences и прежние алгоритмы.

Production APK/Release не опубликованы; production signing/workflow не запускались;
original keystore не использован; реальные данные не читались/не менялись; физического
устройства не было; PR45 остаётся Draft и не объединён. После отчёта остановиться.


# Issue48 — MAIN1.0.11, аудит и реализация, 21.10.2026

ТЗ уточнено и явно утверждено владельцем в сессии: исторические состояния переводятся
на25/50/75/100%, паспортная вместимость — одно число, range вместимости исключён.
Аудит завершён, технически массовая миграция не нужна. Реализация готова к CI/review;
Draft PR и фактические результаты будут добавлены после публикации. Статус не release.

Новая функция считает четверти перед умножением, без округления бункеров.
Никакой перезаписи исторических fillLevel/объёмов/дней/событий/площадей/плотностей/связей:
вычисляемые итоги обновляются при чтении. Повторный запуск не преобразует значения
снова; старый backup хранит те же коды и после restore использует новую формулу.
Room24/DatabaseProvider/entities/backup format неизменны. Существующий снимок объёма
кампании сохранён в базе; для расчёта приоритет имеет ТТХ связанного комбайна,
снимок используется при недоступной карточке. Чтение не меняет старую строку. Нет придуманной константы или второго поля ввода/диапазона вместимости.

UI проценты/точный объём бункера/одно значение массы и урожайности; объём и масса
до3знаков, внутренняя арифметика без промежуточного округления. Общий ReplacementSpan
связывает число с единицей. Площадь/прогресс/режимы плотности без изменения семантики.
Полный протокол аудита и перехода: docs/issue48-fixed-fill.md.

База main2a37bfe05c9b60f04035f7b64fae7f726117d647, деревоf085d292e79424376cdc019e7c3c5912f7aa42b1.
Отдельный worktree/ветка feature/issue-48-fixed-fill. Из-за недоступного git proxy базу
импортировали через GitHub API: все27отличающихся blobs и итоговый tree сверены;
локальныйad45fca имеет точное дерево main. PR будет создан с настоящим parent main,
локальные технические import commits в PR не попадут. Исходная ветка Agent Core сохранена.
Код до финальной чистки форматирования:61b9342 (локальный); точный проверенный SHA
и CI добавляются следующим отчётным коммитом. Незакоммиченные на момент записи:
чистка форматирования и этот отчёт; не объявлять их SHA уже проверенным.

Локально новые запуски:
- python3 docs/run-harvest-jvm-checks.py --gradle-lib <Gradle8.13lib>:62JUnit/0ошибок.
  Это реальные Kotlin/JUnit исходники Calculator/CampaignMetrics/CardModel/FixedFill;
  не Android/Room и не замена Gradle-сборки.
- python3 -m unittest discover -s docs -p 'test_*.py' -q:108tests,106pass/2SDKskip,
  0failures/0errors. Вложенные frozen-policy sandboxes не суммировать.
- source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test:
  Gradle не стартует: FileLockContentionHandler / Could not determine a usable wildcard IP.
  Конфигурация проекта ради среды не менялась. Android build/UI/upgrade пока не проверены.
- Python compile/YAML parsing нового CI/diff whitespace:pass.

Все существующие .github/workflows побайтно прежние; добавлен только безопасный
PR workflow без production secrets. Production admission/signer всё ещё запрещают
product1.0.11: изменение допуска не входит в Issue48. VersionName candidate1.0.11
и обновлённые read-only test expectations не являются разрешением публикации.
Не запускать production signing, APK/Release, merge или действия с реальной MAIN1.0.10.

Новая instrumentation-проверка pinned MAIN1.0.10→candidate1.0.11: оба debug пакета,
полный JSON таблиц/SQL схемы/файлы/preferences/старый backup/restore/restart/edit;
кроме синтетических данных ничего не используется. Её фактический успех пока не заявлен.
Следующий шаг: CI, исправления при необходимости, Draft PR и остановка для review.
Удалённых файлов нет. Полный список изменённых путей будет добавлен итоговым дополнением.


## Issue48 — публикация Draft PR49 и аудит состава

PR: https://github.com/Arch-relic/AgroNaliz/pull/49 — open/Draft, не объединён.
Ветка feature/issue-48-fixed-fill. Опубликованный и локально проверенный код:
168fded5d12745705292e08f93eb75666c258e37, дерево056054086156fece5e8394edee6466966349c614,
совпадает с локальным bd5d8251910370682aef2a5e795798244d96b5fc. Parent main:
2a37bfe05c9b60f04035f7b64fae7f726117d647. Следующий отчётный коммит меняет handoff,
не проверенный Android-код. Изменений main не было.

Состав33пути относительно репозитория (удалений нет):
- .github/workflows/harvest-fixed-fill-checks.yml
- app/build.gradle.kts
- app/src/androidTest/java/ru/fo6osik/workjournal/AppVersionUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/ChangelogUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFixedFillUpgradeTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFullBackup108Test.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestResultsCardUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/Main109UpdateUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/MainRoom23MigrationTest.kt
- app/src/main/java/ru/fo6osik/workjournal/AppChangelog.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestCalculator.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDao.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDayActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestProcessActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestResultsCardView.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestResultsFormatter.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestValueText.kt
- app/src/test/java/ru/fo6osik/workjournal/AppChangelogTest.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestCalculatorTest.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestCampaignMetricsTest.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestFixedFillTest.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestResultsCardModelTest.kt
- docs/codex-handoff.md
- docs/issue48-fixed-fill.md
- docs/run-harvest-fixed-fill-checks.py
- docs/run-harvest-jvm-checks.py
- docs/run-main-room23-checks.py
- docs/run-main-room24-same-checks.py
- docs/run-room24-update-checks.py
- docs/test_app_version.py
- docs/test_main_room23_policy_patch.py
- docs/test_main_room24_release.py
- docs/verify-build-variants.py

Дополнительно отдельно исполнены4JUnit AppChangelogTest реальным Kotlin-компилятором:
4/4pass,0ошибок. Это не повторные62теста расчётов. Сборка/JVM/instrumentation в CI
на момент записи выполнялись. Policy CI #23/run39552980179 уже success:
https://github.com/Arch-relic/AgroNaliz/actions/runs/39552980179.
Новый fixed-fill CI #1/run39552980141: в процессе; успех Android не предполагается
по наличию тестов. Фактические итоги добавляются следующим дополнением.

Повторная статическая сверка: AppDatabase/DatabaseProvider/HarvestBunker/HarvestDay/
HarvestProcess/DatabaseBackupManager/DataImportManager, AGENTS.md и все10ранее
существующих workflow побайтно прежние; handoff история полностью сохранена.
Production secrets/ключи не читались; APK/Release не публиковались; никакого
merge/действий с установленной MAIN1.0.10 физического устройства.


### Уточнение приоритета вместимости при проверке ТЗ

Расчёт использует реальную ТТХ связанного комбайна в первую очередь, как требует
пункт3. Сохранённый snapshot не перезаписывается и используется только при отсутствии
доступной допустимой ТТХ (например, удалена техника). Добавлены readonly assertions
на различающиеся ТТХ/snapshot и неизвестный объём; сравнение всех таблиц остаётся.
Первоначальное описание приоритета snapshot в PR заменяется итоговой реализацией.


# Итог Issue48 — MAIN1.0.11, Draft PR49, 21.10.2026

Статус: реализация/сборка/тесты завершены, подготовлено для review владельца.
PR https://github.com/Arch-relic/AgroNaliz/pull/49 — open/Draft, не объединён.
Ветка feature/issue-48-fixed-fill. Настоящий parent main:
2a37bfe05c9b60f04035f7b64fae7f726117d647. Проверенный GitHub код:
ab8bb449bd11aecc1b6af406272c56fccb97cb63; дерево0e22ca86681df6c39facd20a136f33967207445f
совпало с локальным e36c34dcaf0e658b625c343b61dd67db1a6b75d4.
CI использовал технический PR merge-ref c377d8b37879b4f69299d0594736209a2f027dc9;
его tree независимо сверено через GitHub API и совпадает. Это не merge в main.
Следующий коммит меняет только docs/issue48-fixed-fill.md и этот handoff; его SHA
не подменяет проверенный код. Автоматический повтор CI после отчёта не считается
уже проверенным, Android-код при этом тот же.

## Окончательное поведение и сохранность

Старые/новые FULL/HALF/MORE_THAN_HALF/LESS_THAN_HALF →100/50/75/25%.
Сначала сумма четвертей, потом умножение на вместимость, без округления каждого
бункера. Никакой физической конвертации записей/массового UPDATE/новых колонок:
Room24 и identity/schema прежние. Даты, время, события, интервалы, связи, ID,
площади, плотности и stored snapshot не переписываются. Повторные открытия и
старые backup не дают повторного умножения/конвертации. Backup/import реализация
и четыре строковых кода прежние. Range-контейнер совместимости имеет равные границы;
UI выводит одно значение, неожиданный настоящий интервал — неизвестное, не среднее.

Приоритет вместимости — реальная ТТХ связанного комбайна согласно пункту3ТЗ.
Снимок кампании остаётся нетронутым резервом при недоступной/невалидной карточке.
Это введённое прежде значение, не произвольная константа. Нет обоих — невозможность
расчёта. Новые кампании получают значение автоматически из существующей ТТХ.
Readonly проверки подтверждают приоритет карточки при отличающемся snapshot;
реальные таблицы после вычислений не меняются.

NOVA4.7:25%=1.175,50%=2.35,75%=3.525,100%=4.7м³;
10FULL+25%=48.175м³; MIN720→34.686т; при10га→3.4686т/га→34.686ц/га.
Массы разных дней считаются с их собственными плотностями. MIN/AVERAGE/MAX/CUSTOM,
выбор площади и правила неполной кампании сохранены. Объём/масса форматируются
до3знаков, урожайность по прежнему правилу до2; внутренние числа не округляются.
Проценты добавлены к подписям, точный объём — к бункеру; число+единица неразрывны.
Только количество шире всей карточки прокручивается горизонтально без уменьшения
шрифта/потери текста; обычные значения сохраняют прежнюю адаптивную компоновку.

## Фактически выполненные проверки

Все9workflow на ab8bb449 завершились success:
- Новый MAIN1.0.10→1.0.11, #5/run39555161368:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39555161368.
 12инструментальных вызовов:6на каждый disposable MAIN/TEST. Pinned source1.0.10
 2a37bfe пересобран с CI debug ключом, не оригинальной production подписью.
 До update проверен старый47–49.35м³, после —48.175м³/34.686т/3.4686т/га.
 Сравнены все JSON таблицы, SQL schema, файлы/preferences; старый backup в отдельную
 in-memory БД, roundtrip, повторный запуск, редактирование/возврат старого бункера,
 неизвестные/различающиеся вместимости и неразрывность единиц. MAIN fixture code
 3000008→3000009, TEST1000301→1000302 — только синтетические коды, не обещанный release code.
- Android PR #163/run39555161356:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39555161356.
 53инструментальных вызова/107тестовых выполнений: UI результатов/календарь/навигация/
 интервалы/плотности, все прежние backup/merge/repeat/corrupt rollback, миграции,
 MAIN/TEST isolation и реальные synthetic TEST update same-key/different-key rejection
 с сохранением данных. Python50:13catalog+8version+29TEST tools, без ошибок.
 Upload debug APK skipped; только android-pr6-diagnostics/id12325783120.
- MAIN safe update #80/run39555161373:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39555161373.
 Python18signer+10read-only,3инструментальных вызова Room20→24→24/restart;
 данные/preferences/files/timers/certificate сохранены. Production signer реально
 отверг candidate1.0.11до декодирования ключа, output отсутствует. Синтетическое
 обновление использует уже подписанные Gradle debug fixtures с проверкой настоящих
 подписей/одного disposable cert/package/product/increasing code; production signer
 для разрешения тестового update больше не вызывается.
- MAIN Room22 regression #50/run39555161469:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39555161469.
 3вызова1.0.7/Room22→1.0.11/24, все старые поля/таблицы/FK/файлы/preferences,
 отдельный restore/restart. Масса4состояний при8м³/750 — одно15т, не прежние12–18.
- Room23→24 #37/run39555161567:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39555161567 —10вызовов обоих пакетов.
- Room24→24 #9/run39555161479:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39555161479 —10вызовов обоих пакетов.
 Старые names1.0.9→1.0.10/Room22→23 исторические; read-only candidate assertions
 обновлены на1.0.11. Эти проверки не допускают1.0.11к production публикации.
- UI-polish #12/run39555161499:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39555161499 —API23/35, MAIN/TEST,
 12вызовов: metadata/cert/copy/recreation, width280/360/600dp/font1/1.6,
 полные числа+единицы и доступность горизонтального overflow без обрезки/уменьшения.
- Changelog #22/run39555161379:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39555161379 —3UI-вызова.
- Policy #27/run39555161461:
  https://github.com/Arch-relic/AgroNaliz/actions/runs/39555161461 —30верхнеуровневых
 Python:10frozen Room23+10live23→24+10live24→24. Старые manifests/fixtures/guards
 сохранены, candidate1.0.11не получает release admission; временные sandboxes
 нормализуют только свои product metadata. Вложенные архивные проверки не суммировать.

Всего106инструментальных вызовов/160тестовых выполнений (107+12+10+10+12+3+3+3),
включая намеренные повторения, не160уникальных методов. Python108верхнеуровневых в CI
(50Android+28MAIN-safe+30policy), без ошибок; вложенные sandboxes не прибавляются.

Артефакт нового CI12325603142 fixed-fill-test-reports реально скачан и разобран.
SHA256 ZIP69313873f9f84a28f72a92bd49a26a20d5f570adcd55587028e2a1ced1fa0550
совпал с GitHub. APK/AAB/keys в ZIP нет. verification.json содержит sourceSha
c377d8b37879b4f69299d0594736209a2f027dc9, baseline2a37bfe, schemaUnchanged/rowsUnchanged/
legacyBackupRestored/restart=true для обоих пакетов, productionReleaseAuthorized=false.
XML:15suite/181tests в testDebugUnitTest и15suite/181в testDebugTestUnitTest;
0failures/0errors/0skip в каждом. Повторные JVM-прогоны9workflow не суммировать.

Локальные новые прогоны:62Kotlin/JUnit расчётов/карточки/FixedFill плюс отдельно4
AppChangelogTest, все успешны. Python108tests:106pass/2SDKskip,0ошибок.
Команды: python3 docs/run-harvest-jvm-checks.py --gradle-lib <Gradle8.13lib>;
python3 -m unittest discover -s docs -p 'test_*.py' -q; отдельно K2JVMCompiler/JUnitCore
для AppChangelog. Python compile, новый YAML parse и diff whitespace —pass.
Обязательная локальная source /workspace/agronaliz-env.sh;
agrogradle :app:assembleDebug :app:test не стартует из-за FileLockContentionHandler/
Could not determine a usable wildcard IP; конфигурация ради среды не менялась.
Android build/assembleDebug/assembleDebugTest/обаAndroidTest/обаJVM выполнены в CI;
результаты не подменены локальным обходным JVM-прогоном.

Первые CI обнаружили устаревшие ожидания версии/интервала и зависимость synthetic
update от production signer; они исправлены только в тестовой инфраструктуре.
Случай широкой неразрывной единицы выявлен и исправлен адаптивной прокруткой.
Утверждение полного старого update до этих исправлений не считать успешным;
окончательные ссылки/результаты приведены выше.

## Полный окончательный список файлов

Созданы (6):
- .github/workflows/harvest-fixed-fill-checks.yml
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFixedFillUpgradeTest.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestValueText.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestFixedFillTest.kt
- docs/issue48-fixed-fill.md
- docs/run-harvest-fixed-fill-checks.py

Изменены (31):
- app/build.gradle.kts
- app/src/androidTest/java/ru/fo6osik/workjournal/AppVersionUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/ChangelogUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestFullBackup108Test.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/HarvestResultsCardUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/Main109UpdateUiTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/MainRoom23MigrationTest.kt
- app/src/androidTest/java/ru/fo6osik/workjournal/UiPolishTest.kt
- app/src/main/java/ru/fo6osik/workjournal/AppChangelog.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestCalculator.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDao.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestDayActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestProcessActivity.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestResultsCardView.kt
- app/src/main/java/ru/fo6osik/workjournal/HarvestResultsFormatter.kt
- app/src/test/java/ru/fo6osik/workjournal/AppChangelogTest.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestCalculatorTest.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestCampaignMetricsTest.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestResultsCardModelTest.kt
- docs/codex-handoff.md
- docs/run-harvest-jvm-checks.py
- docs/run-main-room23-checks.py
- docs/run-main-room24-same-checks.py
- docs/run-main-update-checks.py
- docs/run-room24-update-checks.py
- docs/test_app_version.py
- docs/test_main_apk_release.py
- docs/test_main_room23_policy_patch.py
- docs/test_main_room24_release.py
- docs/verify-build-variants.py
- docs/verify-main-room23-inputs.py

Удалений нет, всего37путей. Все10существующих workflows, AGENTS.md, Room/entities/
DatabaseProvider и backup/import реализация побайтно прежние; история handoff main
сохранена целиком. Из старых release files менялись только тесты/read-only verifier,
не publisher/signer/admission и не подписи/applicationId. Dependencies не обновлялись.

## Ограничения и следующий шаг

Это candidate1.0.11, не выпущенная версия. Production admission/signer всё ещё
запрещают1.0.11; будущий допуск/точный итоговый SHA/возрастающий release code/подпись
и публикация — отдельное согласованное задание после review. Не использовать старые
зелёные аудиты или синтетические коды как разрешение выпуска. Смена SHA снимает
любые будущие exact-source допуски согласно прежней политике.

Физический смартфон и установленная MAIN1.0.10не подключались; фактические значения
его ТТХ/кода/сертификата/данные напрямую не проверялись. Работает реальное сохранённое
значение вместимости, не константа по названию модели. Приватный production key не
требовался и не использовался. Production signing/workflows/публикация APK/Release,
merge/main push, удаление пользовательских данных не выполнялись.

Следующее действие — review владельца. Проверить подписи25/50/75/100%, последний
NOVA25%при4.7→1.175, объём/массу кампании и обычный/крупный шрифт. На телефон ничего
не устанавливать без отдельного разрешения и прежнего backup/release процесса.
После этого отчёта остановиться; PR оставить Draft.

# Agent Core v1.0 — этап 1, 20.10.2026

Цель: управляющая основа задач/согласований/полномочий без автоматического Codex.
Задание и техническая автономность явно утверждены пользователем в текущей сессии.
Статус: реализация и локальные проверки завершены, подготовка Draft PR; результаты
реального GitHub owner/main approval не заявлены. После публикации — остановка для review.
Прежняя история handoff сохранена, AGENTS.md прочитан и побайтно не изменён.

## Результат и решения

Версионируемые JSON Schema Draft2020-12 для задач/рекомендаций, append-only редакции,
решения и история переходов; SHA256 канонического spec, строгая матрица состояний.
Approval привязан к реальному GitHub workflow_dispatch: pinned owner ID240867647,
main, attempt1, successful run, workflow ID/path, task/spec/operations/executor/sequence.
История и редакция независимо сверяются с задачей в неизменяемом run.head_sha.
Комментарии/метки/файл PR не являются полномочиями. Самосогласование запрещено.
Изменение ТЗ, блокировка/отмена/повторное согласование снимают действующий допуск.
Сохранены причины и прошлые approvals. Защищённые операции всегда fail-closed;
протокол отдельного разрешения не имитируется.

PR unit job не имеет secrets/переданного токена/записи. Trusted pull_request_target
читает код/схемы только из base SHA и JSON PR через API; не выполняет код PR.
Смена governance/workflow (включая rename), удаление задач, неполная выдача API,
несовпадение текущего head/base отклоняются. Weekly понедельник06:17UTC и manual
session_start проверяют контракт рекомендаций; ИИ/уведомления/исполнитель не подключены.
Варианты и причина выбора owner/main dispatch вместо комментариев или отдельного
приватного signer описаны в docs/agent-core.md. Новый ключ/backup не требуется.

## Файлы

Созданы:
- .github/workflows/agent-core.yml
- agent_core/__init__.py
- agent_core/core.py
- agent_core/check.py
- agent_core/requirements.txt
- agent_core/schemas/task-v1.json
- agent_core/schemas/recommendation-v1.json
- agent_core/tasks/.gitkeep
- agent_core/recommendations/.gitkeep
- tests/agent_core/test_core.py
- tests/agent_core/test_trusted_checks.py
- docs/agent-core.md

Изменён только docs/codex-handoff.md. Удалённых файлов нет.

## Ветка, SHA, проверки

Ветка feature/agent-core-stage1. GitHub API подтвердил базу main
`e77e6241885e8ecbe031aed475312b6664b5eb58`, совпадающую с локальным HEAD до изменений;
origin/main локально устарел, git fetch/gh недоступны из-за proxy8080. Подключённый
GitHub API доступен, публикация выполняется через него без изменения main.
Проверенный локальный коммит кода `a4007ba3054d4fc55e164476583a1638adbca76b`,
дерево `0ad8e86a83ebdd9e99e1ee42b30459b0487b7507`.
Следующий отчётный коммит добавляет этот handoff; его SHA не подменяет проверенный код.

Новые запуски:
- `python3 -m unittest discover -s tests/agent_core -q`:51tests,0failures/0errors/0skip.
  Проверены регистрация, все основные состояния, недопустимый переход, missing approval,
  конкретный grant, изменение/recomputed hash, outsider/self-approval, forged/replayed run,
  blocked/cancelled, missing fields, сохранение решений/истории, protected operations,
  PR/workflow/schema/init injection и rename, incomplete API/offline, unknown operations,
  owner/main/first-attempt dispatch, параметры, session_start, weekly schedule, JSON/schema.
- `python3 -m agent_core.check local`:success для пустых рабочих каталогов; это проверка
  структуры, не доказательство реального approval. Фиктивные задачи не зарегистрированы.
- `python3 -m unittest discover -s docs -p 'test_*.py' -q`:96tests,94passed/2SDKskipped,
  0failures/0errors; вложенные временные patch-tests не прибавляются к96.
- `python3 -m compileall -q agent_core tests/agent_core`:success.
- YAML parsing нового workflow и проверка набора triggers/jobs:success.
- `git diff --check` / `git diff --cached --check`:success.
- Проверено отсутствие diff AGENTS.md, всего app/ и существующих workflows.

Android не собирался: его код не менялся. Эмулятор/физическое устройство/реальные
данные не использовались. Room24/миграции/backup/подписи MAIN и TEST/production admission
и версии приложения без изменений. Production secrets не читались и не использовались;
APK/Release не публиковались, merge/main push отсутствуют.

## Аудит безопасности, ограничения и следующий этап

Gate минимизирует privileges, не сохраняет credentials checkout, не интерполирует
PR-текст в shell и fail-closed при неполных/противоречивых данных. Реальная выдача
согласования и schedule требуют наличия workflow на main после отдельного review/merge
владельцем. Этот bootstrap PR не может доказать доверенный запуск нового gate из main.
Тесты approval воспроизводимы с синтетическим GitHub API; реальный owner-run не запускался.
Автоинтеграция Codex не готова и не заявляется готовой.

API ветки сообщил protected:false, rulesets вернул []; отдельный branch-protection API
недоступен интеграции403. Защита/настройки репозитория не менялись. Требуется отдельное
решение владельца о required trusted check и защите governance. Пока статус проверки
не обязателен в GitHub, CI не способен технически запретить обход пользователем с write.
Проверяется модель зарегистрированных задач, не семантика всех Android PR и не новая
обязательная привязка legacy PR к task ID. Будущий исполнитель обязан использовать
доверенные core/API/идентичность и authorize перед каждым действием; это следующий этап.
Сохранность исторического run зависит от GitHub: удаление/недоступность — отказ.

Следующее: review текущего протокола владельцем; отдельно merge; реальная тестовая
регистрация/owner approval/отрицательные проверки на main; затем согласованная интеграция
Codex и уведомлений. Не выполнять merge, signing, выпуск или изменение production gates.

PR пока не создан: публикация ветки/дерева и Draft PR следующим действием через API;
номер/URL и результаты CI будут добавлены отдельным дополнением в той же ветке.


## Публикация Agent Core — Draft PR #47, 20.10.2026

https://github.com/Arch-relic/AgroNaliz/pull/47 — open/Draft, не объединён.
Ветка feature/agent-core-stage1; parent main e77e6241885e8ecbe031aed475312b6664b5eb58.
Опубликованный код с первоначальным отчётом: 2ff804583ef677c2fec6720a7149441b2d4ff179,
дерево cbd891ea89ea31d5b374e117a2f7dc940cf94f10 совпало с локальным 4fcce11.
API PR подтвердил ровно13ожидаемых путей (12новых и этот handoff), удалений нет.
Все9прежних workflow, весь app/ и AGENTS.md побайтно прежние; прежний handoff
целиком сохранён внутри дополненного документа.

Новый CI Agent Core #1 завершился успешно:
https://github.com/Arch-relic/AgroNaliz/actions/runs/39474407982.
Job tests/id118806522463:51tests,0errors/0failures; лог фактически прочитан.
Unit job имеет Contents:read, persist-credentials:false. Trusted-validation skipped
для pull_request — ожидаемое разделение, это не проверенный owner/main grant.
CI checkout технического merge-ref832027bb998a71647f45937cde4d51ac7b0ad3ee;
это GitHub PR-ref, не merge в main. Новый отчётный коммит меняет только handoff;
повтор CI может запуститься автоматически и не считается уже проверенным здесь.

Дополнительные существующие Android PR workflows запущены GitHub автоматически,
на момент этой записи ещё выполнялись; их успех не заявляется. Production publish
не запускались. Android-код этой задачей не менялся; целевой Agent Core CI зелёный.
Состояние main/rulesets и ограничения bootstrap остаются описанными выше.

Далее только review владельцем. Merge/production signing/APK/Release запрещены.
Фактическую интеграцию Codex и live owner approval не объявлять готовыми.


# Agent Core — синхронизация PR #47 и повторный аудит, 21.10.2026

Задание: синхронизировать feature/agent-core-stage1 с актуальным main без потери
изменений, повторить тесты/Gate/Actions/применимые CI, подготовить review.
PR https://github.com/Arch-relic/AgroNaliz/pull/47 остаётся open/Draft; merge запрещён.

Main: 2a37bfe05c9b60f04035f7b64fae7f726117d647. Проверенный GitHub SHA:
5e80cb913811e48f8da4bd918b496bfd15032be6, tree ac9ff4bb5da7c4e14438ce4e379321f079015f3d.
Локальный эквивалент с тем же деревом: 5b6d260fba70ec8c7e838b414a7cc38752ed995b.
Remote merge имеет двух родителей: прежний feature 2d329de2876961181505113c3c932e2db56c424c
и актуальный main. GitHub ref обновлён fast-forward с expected_sha; force push нет.
Локальный git использовал ранее побайтно проверенное дерево main (ad45fca), поэтому
локальный SHA отличается. Отчётный коммит последует отдельно и меняет только документы;
указанные SHA обозначают реально проверенный код, не содержащий отчёт коммит.

Конфликт только docs/codex-handoff.md: полный текущий main handoff и вся история
Agent Core сохранены. App/AGENTS.md/10 прежних workflows точно совпадают с main.
Наследованные изменения MAIN1.0.10 сохранены без новых изменений production-политик.
Issue48/PR49 в эту ветку не включены. Room24/backup/подписи не менялись.

Аудит выявил ошибку строгого Base64-декодирования: стандартные переносы строк
GitHub Contents API блокировали реальный approval. Удаляются только CR/LF перед
validate=True; недопустимые символы отвергаются. Архитектура протокола прежняя.
Добавлены два регрессионных теста. Полный аудит: docs/agent-core-security-audit.md.

Пути изменений этой итерации относительно прежнего PR47 (кроме наследования main):
- agent_core/core.py — исправление декодирования GitHub evidence;
- tests/agent_core/test_core.py — два автоматических теста;
- docs/codex-handoff.md — объединение историй и этот отчёт;
- docs/agent-core-security-audit.md — новый отчёт.
Удалений нет. Полный diff PR относительно main содержит прежние 13 путей Agent Core
и новый отчёт; приложение и существующие политики вне diff.

Новые локальные запуски:
- python3 -m unittest discover -s tests/agent_core -q: 53 tests, 0 errors/failures/skips.
- python3 -m unittest discover -s docs -p 'test_*.py' -q: 107 tests, 0 errors/failures,
  2 SDK skips; вложенные временные patch-tests отдельно в 107 не прибавляются.
- python3 -m agent_core.check local: passed, пустые рабочие каталоги, не live approval.
- python3 -m compileall -q agent_core tests/agent_core: passed.
- YAML parsing всех 11 workflows, проверка triggers/jobs/permissions: passed.
- git diff --check, сохранность обеих историй, отсутствие diff app/AGENTS/existing workflows: passed.
Android локально не собирался: код приложения прежний. CI выполняет сборки и эмуляторы.

Результаты CI проверенного SHA внесены в docs/agent-core-security-audit.md.
Agent Core run39560669437: 53 tests успешно, лог job119090876285 прочитан;
trusted-validation skipped для pull_request ожидаемо. Это не реальный owner/main grant.
Changelog и detached policy CI не запустились из-за paths filters; все существующие
Python policy tests вошли в 107 локальных тестов. Публикационные workflows не запускались.

Аудит подтверждает fail-closed для forged/replayed approval, другой редакции,
самосогласования, protected operations, missing data/API failure, изменения управляющего
кода/workflow/rename. PR code не исполняется доверенным job; permissions только read,
checkout credentials не сохраняются. Задачи/решения/редакции append-only.

Ограничения: main API protected:false, required checks off, rulesets[]; настройки не
изменены. Обязательность Gate требует отдельного решения владельца. Bootstrap workflow
ещё отсутствует в main: live owner/main approval и weekly schedule не проверены;
после отдельного owner review/решения о первом включении нужен реальный синтетический
прогон и отрицательные проверки. Автоисполнитель, ИИ и уведомления не подключены.
Gate проверяет зарегистрированные задачи, не семантику всех Android изменений.
Полную эксплуатационную готовность до live-проверки/решения о защите main не заявлять.
Изменение архитектуры не предпринималось. Для следующего этапа — review владельца.

Main не обновлялся; merge PR, production signing, APK/Release публикация, реальные
пользовательские данные и физический смартфон не затрагивались.


## Блокирующий результат повторного аудита: отзыв полномочий

PR не готов к merge. Воспроизведено: authorize принимает старый approved JSON после
перевода актуальной задачи в blocked, cancelled или awaiting_approval. Функция читает
исторический GitHub run и task на run.head_sha, но не текущее доверенное состояние.
53 зелёных теста проверяют отказ на обновлённом JSON, не этот stale-snapshot сценарий.
Ранее перечисленные гарантии отказа относятся к переданному актуальному снимку;
они не обеспечивают отзыв при использовании устаревшего снимка.

По прямому условию пользователя об архитектуре реализация отзыва остановлена.
В docs/agent-core-security-audit.md приведены воспроизведение и варианты:
1. Рекомендуется определить main task как источник актуального разрешения и отзыва,
   читать его по закреплённому SHA перед authorize, проверять соответствие и гонки;
   отзыв вступает в силу после попадания метаданных в main.
2. Отдельный доверенный журнал отзывов/состояния — более крупное изменение протокола.

Нужно согласовать источник и момент вступления отзыва в силу. Автоматического
исполнителя нет; несанкционированная автоматическая операция не выполнялась.
Логика Gate после исправления Base64 не менялась. Дальше — решение владельца,
исправление с отрицательными тестами и повторный аудит; merge пока не рекомендован.


## Финал повторных CI на проверенном коде

Все 7 применимых workflow SHA5e80cb913811e48f8da4bd918b496bfd15032be6 — success:
AgentCore39560669437, AndroidPR39560669436, MAINsafe39560669423,
Room22regression39560669450, Room23→24 39560669441, Room24same39560669480,
UIAPI23/35 39560669438. Логи всех 8 выполнявшихся jobs прочитаны.
Android: сборки MAIN/TEST и JVM tests успешно; 53 инструментальных вызова,
107 исполнений. Остальные инструментальные вызовы: 3+3+10+10+12.
В артефактах всех 7 runs только diagnostics/reports, APK отсутствуют.
Результаты и ссылки — в docs/agent-core-security-audit.md. Последующий docs-only
коммит может автоматически запустить повторные CI; их успех здесь не заявлен.
Готовность к merge остаётся заблокированной дефектом отзыва, несмотря на зелёный CI.


# PR47 — согласованный вариант 1 отзыва полномочий, 21.10.2026

Источник задания: прямой запрос пользователя и решение владельца
https://github.com/Arch-relic/AgroNaliz/pull/47#issuecomment-6312800007.
Автор решения GitHub ID 240867647 проверен через API. Это разрешение на доработку PR;
комментарии не становятся машинными task approvals. PR47 open/Draft, не объединён.

Ветка feature/agent-core-stage1. Main2a37bfe05c9b60f04035f7b64fae7f726117d647.
Проверенный код GitHub34b89e4e8a544a5ee833640fa2a6b079aa7323cc,
tree 5fc57eb7ba976c4d7086b5fc2cc0f804772e8ec4. Локальный эквивалент с тем же деревом
83976df6ea3b626f6f6d6b900cd661cd4487123c. Публикация через API с expected_sha,
без force push. Отчётный коммит после кода меняет только документацию и не подменяет
SHA проверенного кода. Старые аудиты/решения/история handoff сохранены.

Реализовано:
- authorize(task, operation) больше не принимает api/actor_id от вызывающего кода.
- Фиксированный GitHub transport и identity из run API; repo/head repo/main/SHA,
  workflow path/ID/ref, dispatch/attempt1/active run, actor=triggering_actor и runner
  GITHUB_ACTOR_ID проверяются. Недоверенный процесс/PR не должен управлять runtime,
  GH_TOKEN, GITHUB_*, импортами или monkeypatch доверенного процесса.
- Main SHA через refs API → task строго по SHA → полное сравнение snapshot →
  validate/history/hash/approval/state/revision/scope → повтор main SHA.
  Ошибки/404/неполные или противоречивые данные/смена main — Denied без fallback.
- Отзыв действует после изменения task в main. Проверять перед каждым действием;
  нет кэша/lease, немедленной остановки уже запущенной операции или атомарности
  проверки с исполнением. Force-push/обход write-пользователем требует защиты main.
- Старый defect stale-approved snapshot закрыт. Прежние policy-тесты адаптированы
  к новому API; отдельные full-context tests не подменяют execution_actor.

Изменены:
agent_core/core.py
agent_core/check.py
tests/agent_core/test_core.py
tests/agent_core/test_trusted_checks.py
docs/agent-core.md
docs/agent-core-security-audit.md
docs/codex-handoff.md
Создан tests/agent_core/test_revocation.py. Удалений нет.
App/, AGENTS.md и10прежних workflows точно совпадают с main. Новый agent-core.yml
не изменялся этой итерацией. Protected production policies/Room24/backup/подписи/
release admission/main settings не менялись. Код Issue48/PR49 не включён.

Новые локальные запуски:
python3 -m unittest discover -s tests/agent_core -q —69 tests,0 failures/errors/skips.
python3 -m unittest discover -s docs -p 'test_*.py' -q —107 tests,0 failures/errors,2 SDK skips;
вложенные временные patch-tests не прибавляются к 107.
python3 -m agent_core.check local — passed, пустые каталоги (не live approval).
python3 -m compileall -q agent_core tests/agent_core — passed.
YAML parsing11workflows, git diff --check, scope comparison — passed.
Android локально не собирался: код не менялся, сборки/эмуляторы выполняются CI.

16новых regression tests с subcases: текущий approval/implementing/review; stale
blocked/cancelled/awaiting_approval/новаяредакция; matching revoked snapshot; replay;
новое действие после отзыва; main SHA race; outage на каждом API чтении; missing task;
смена executor/scope/history/links; неподдерживаемые api/actor parameters; untrusted
run/runner; неполные main/task ответы и duplicate JSON/invalid Base64.
AgentCore CI39579506821 —69 tests success; trusted-validation skipped для PR ожидаемо.
Лог job119153311247 прочитан. Все применимые CI исправления и ссылки внесены
в docs/agent-core-security-audit.md по завершении; старые результаты не переиспользуются.

Независимо от unit fixtures выполнен ручной аудит входов, транспорта, последовательности
чтений/отказов и workflow trust boundary. Реальный API main task directory вернул404,
старый AgentCore PR run имеет feature branch/completed/PR event и не подходит для
execution context. Main protected:false/rulesets[]; настройки не менялись.
Live owner/main positive/negative approval и weekly run не проверены из-за bootstrap;
после отдельного owner review/решения о включении нужны реальные проверки.
Текущий workflow не вызывает authorize для реальных действий, исполнитель не подключён;
execution context пока допускает только trusted AgentCore main dispatch. Future executor
integration/workflow contexts — отдельное согласование. AI recommendations не подключены.

Статус: исправление варианта 1 реализовано, для review владельца; полной эксплуатационной
готовности не заявлять. Остаточные риски и будущие шаги — live bootstrap validation,
защита main/required checks и отдельно согласованная интеграция исполнителя.
PR https://github.com/Arch-relic/AgroNaliz/pull/47 оставить Draft и остановиться для review.
Merge, публикация APK/Release, production secrets и смартфон не использовались.


## Android CI: первый сбой и повтор

Все 6 остальных workflow исправления завершились success. Общий AndroidPR run39579506701
attempt1/job119153310556 прошёл сборки и JVM suites (175+175 tests,0 errors/failures/skips,
подтверждено XML из diagnostic artifact12336965208), но остановился на первом
AppVersionUiTest#showsInstalledVersion: Espresso RootViewWithoutFocusException,
окно не получило focus за 10 секунд. Это не assertion проверки Agent Core.
APK в diagnostic archive нет. Сохранены логи/первый результат; код не менялся.
Перезапущен только этот job на том же code SHA: attempt2/job119159373964.
Это не approval workflow, повтор не выдаёт полномочий. Окончательный результат
внесён ниже и в docs/agent-core-security-audit.md после завершения.


## Финал CI варианта 1 — review владельца, 21.10.2026

Все 7 применимых CI проверенного code SHA 34b89e4e8a544a5ee833640fa2a6b079aa7323cc — success:
- AgentCore39579506821:69 tests.
- AndroidPR39579506701 attempt2/job119159373964: сборки MAIN/TEST, JVM-тесты,53 инструментальных
  вызова/107 исполнений; первая попытка с focus error сохранена выше, код не менялся.
- MAINsafe39579506619:28 Python и 3 инструментальных вызова.
- Room22regression39579506708:3 инструментальных вызова.
- Room23→24 39579506618:10 инструментальных вызовов.
- Room24same39579506666:10 инструментальных вызовов.
- UIAPI23/35 39579506650:6+6 инструментальных вызовов.
Логи8 успешных jobs и исходного failed Android job прочитаны. Последующий отчётный
коммит изменяет только docs/codex-handoff.md и docs/agent-core-security-audit.md;
его возможные автоматические CI не приравниваются к уже проверенному коду.
Все подробности/ссылки/ограничения: docs/agent-core-security-audit.md, новый раздел
«Вариант 1: устранение дефекта отзыва». Предыдущий блокирующий аудит относится к старому
SHA5e80cb9; на новом SHA согласованный defect устранён, предыдущая история сохранена.

Статус: готово для review владельца, PR47 оставить Draft. Эксплуатационная готовность
без live bootstrap/защиты main/согласованной интеграции исполнителя не заявляется.
Main остался 2a37bfe,protected:false; production policies/Room24/backup/signatures/
release admission не изменены. Merge,APK/Release публикация,production secrets,
физический смартфон и автоматический Codex не затрагивались. Дальше — только review.
## G2 Offline Agent Identity v2 — 21.10.2026

Цель: реализовать согласованную offline часть Issue54 без live entrypoint/credentials.
Разрешение владельца:
https://github.com/Arch-relic/AgroNaliz/issues/54#issuecomment-6315717967.
Статус: offline код и аудит готовы к Draft PR/review; live и merge не разрешены.
Архитектурный источник: PR53 head96d8535ec544fcd13e93335e3d0f096c966da0f8,
G1 D01–D12 утверждены. PR51 и PR53 не объединялись и не cherry-pick целиком.

Реализованы task/registry/policy schemas v2, typed App subject, immutable approval
context, append-only state/history/requests, owner/run/source/policy verification,
fresh-main offline controller, фиксированный smoke, disabled credential boundary,
safe audit и synthetic fakes/fixtures. V1 интерфейсы и документы не меняются;
App не исполняет старые grants, никаких миграций пользовательских данных нет.

Созданы файлы:
- agent_core/schemas/task-v2.json
- agent_core/schemas/registry-v2.json
- agent_core/schemas/policy-v2.json
- agent_core/v2/__init__.py
- agent_core/v2/model.py
- agent_core/v2/task.py
- agent_core/v2/approval.py
- agent_core/v2/trust.py
- agent_core/v2/controller.py
- agent_core/v2/credentials.py
- agent_core/v2/audit.py
- tests/agent_core/v2_fakes.py
- tests/agent_core/test_v2_schema.py
- tests/agent_core/test_v2_approval.py
- tests/agent_core/test_v2_controller.py
- tests/agent_core/test_v2_compatibility.py
- tests/agent_core/fixtures/v2/policy.json
- tests/agent_core/fixtures/v2/registry.json
- tests/agent_core/fixtures/v2/request-task.json
- docs/agent-identity-v2/g2-offline-implementation.md
- docs/agent-identity-v2/acceptance-matrix.md

Изменены только отчёты:
- docs/agent-core-security-audit.md
- docs/codex-handoff.md

Удалений нет. core.py/check.py/task-v1/requirements/v1 tests/workflows/tasks/
recommendations/app сравнивались с main: изменений нет. Permissions/secrets,
main policies, Room24/backup/signing/release admission не менялись.

Ветка: feature/agent-identity-v2-g2-offline.
Base: e5e0aeae9474307562a90ca4d3d075dc11800841.
Проверенный локальный code SHA: 6e0cdb90ce91f23cc391c94ca4ea517f8bf70b6a,
tree4d68c4e06b2bd38640bb52f8cd412c636ee3dd6e. Его содержимое проверялось до
создания commit, без последующего изменения кода; отчёты добавляются отдельным commit.
Git fetch недоступен: proxy connection refused. Main commit восстановлен из GitHub API
signed payload с проверкой точного SHA; worktree создан от этого main, не от старой ветки.
Для публикации используется connector; remote commit SHA может отличаться, tree
проверяется независимо. Точный remote head и CI относятся к PR, не подменяют local SHA.

Команды/результаты:
- python3 -m unittest discover -s tests/agent_core -v: passed до двух финальных
  дополнительных boundary tests; окончательный запуск всей suite ниже включает их.
- Полная unittest discovery с socket.socket=forbidden и очищенным os.environ:
  140 methods, 0 failures/errors, 342 subtest cases; 69 прежних +71 новых методов.
  Код воспроизведения в g2-offline-implementation.md. Это новый запуск, не UP-TO-DATE.
- python3 -m agent_core.check local: passed, действующие directories пусты;
  не доказательство реального approval или регистрации v2.
- python3 -m compileall -q agent_core/v2 tests/agent_core/test_v2_*.py: passed.
- git diff --check и scope diff для защищённых файлов: passed.
- Первый запуск имел SyntaxError в новом тесте; исправлен до итогового запуска.

Матрица A01–A32 содержит отдельные passed/not_run/blocked по O/N/L.
Live тестов не было. Не проверены реальные App/install credentials, pagination adapter,
environment/key release, platform isolation, token revoke и branch protection.
ABA/in-flight ограничения явно воспроизведены/описаны; полной атомарности нет.
Finally не гарантируется при forced runner kill; blocked cleanup после smoke не
отменяет уже выполненный read. Outcome и fake source/complete не являются live authority.

PR ещё не открыт на момент этого отчётного commit; следующий разрешённый шаг —
публикация Draft PR с кодом/документацией и ссылкой на этот handoff.
Ожидается блокировка trusted PR gate из-за agent_core/: это штатный запрет self-approval
governance changes. Фактический результат/URL/remote SHA будут указаны при создании PR.
Если gate отклонит PR, не исправлять/обходить/перезапускать ради авторизации: остановиться
для отдельного owner review. Автоматические CI не называются успешными заранее.

G3–G6 не разрешены: App/environments/keys/tokens/secrets не создавать; live task,
approval/dispatch, production APK/release, merge и write permissions не выполнять.
Следующий исполнитель сначала читает PR и точный head/CI, затем ждёт решения владельца.

### Публикация G2 Draft PR — 21.10.2026

Открыт Draft PR55: https://github.com/Arch-relic/AgroNaliz/pull/55.
Первый remote head: 4839e0fd4cdb55054a881d36cd07b74dc01d5f52,
base e5e0aeae9474307562a90ca4d3d075dc11800841, tree169ce78cbb7cf4d8c7344fc0ca5adaba030861e7
совпадает с локальным c18ecf5afb107b1de2a43743c26203ecb1108b2d.
Этот следующий commit только фиксирует URL публикации; код не изменён.
Remote CI запущены автоматически; итоговый статус/run/SHA проверять в PR55.
Trusted gate не обходить; его отказ требует остановки для review, не исправления защиты.
## Issue58 — Agent Environment Gate, 22.10.2026

Цель: подготовить минимальный workflow environment smoke-test без запуска/merge.
Источник: https://github.com/Arch-relic/AgroNaliz/issues/58 и прямое задание владельца.
Статус: YAML и security scope проверены локально; live gate НЕ проверен.

Созданы:
- .github/workflows/agent-environment-gate-smoke.yml
- docs/agent-environment-gate-smoke.md
Изменён только docs/codex-handoff.md, append-only. Удалений нет.

Workflow: только workflow_dispatch без inputs; permissions={}; один job ubuntu-latest,
timeout3, environment agronaliz-agent-readonly; единственный run echo заданной строки.
Нет checkout/uses, secret/token references, API/network commands, App, artifacts/cache,
outputs/env или repository write. Job permissions не расширяются. Workflow не запускался,
команда вывода не исполнялась локально. Agent Core и существующие workflows не менялись.

Ветка: feature/agent-environment-gate-smoke.
Base main: 0b41d1251363696b5bc99a5109752baa02911ae8 (PR55 уже merged владельцем).
Проверенный локальный implementation/doc SHA: de7ef9fefcf1078ac7989dd80e5184872f1ecf80,
tree c6d200f1e545924aaf5f776e80f82a4b1bcaa15f. Проверки выполнены до commit на тех же
файлах; следующий отчётный commit только добавляет этот handoff. База восстановлена
из GitHub API signed payload с проверкой exact SHA; worktree от актуального main.
При публикации connector SHA может отличаться, полный tree сверяется отдельно.

Выполнены проверки:
- PyYAML SafeLoader с duplicate-key rejection и exact structure assertions: passed.
- Запрет uses/expressions/secrets/token refs/network commands/env/inputs/auto triggers/
  write в новом YAML: passed. В репозитории не добавлены dependencies или test files.
- bash -n для единственной команды, без её исполнения: passed.
- git diff --check и защищённый scope diff: passed.
actionlint не установлен; его запуск не заявляется. Android/Agent Core suite вручную
не запускались, так как код приложения/Core не затронут. Новых workflow runs/dispatch/
rerun/approval этим этапом не создавалось. Автоматические CI PR существуют отдельно
от нового manual-only workflow и не доказывают environment approval.

Настройки environment (owner reviewer, self-review allowed, bypass disabled, main-only,
no secrets) сообщены владельцем в Issue58; live независимо не проверены. Environment/
secrets/permissions/settings/main/protection/Room/backup/signing/release не менялись.
В будущем нужны отдельные разрешения на merge, затем dispatch ref=main; проверить
Waiting for review, owner UI approval и success. Сейчас это not_run. Не ослаблять
main-only deployment rule и не создавать заменяющий environment ради запуска из PR.

Trusted gate защищает .github/workflows/: ожидается governance failure, его не менять
и не обходить. Ни этот smoke, ни его будущий success не выдают machine/task consent.
G3–G6 не открываются. APK/Release не публиковались, main не изменялся этим заданием.

На момент записи Draft PR ещё не открыт; следующий шаг — публикация workflow/report
в отдельный Draft PR и фиксация его URL в этой ветке. После этого остановиться для
owner review. Не выполнять merge, Ready, environment approval, dispatch или cleanup.

### Публикация Issue58

Открыт Draft PR59: https://github.com/Arch-relic/AgroNaliz/pull/59.
Первый remote head79311884b3d477d5baecef24a6133d65f7b7bdbc,
tree69a8124954aaa381b88fd17156438ae93bf4989b совпадает с local4c90404b.
Этот следующий commit фиксирует только URL публикации. Новый environment smoke
workflow не запускался; state Waiting for review/approval/success остаётся not_run.
Main и settings не менялись. PR оставить Draft и остановиться для owner review.


## Этап 3 / B — mock-first v1.4 Final с поправкой WORKER RESULT, 23.10.2026

Цель: реализовать только локальную synthetic модель controller → approvals → fake
worker → independent validator → simulated publisher → evidence/checkpoint.
Статус: реализация и локальные проверки завершены; требуется owner review Draft PR.

### Основание и решения

Владелец отдельно утвердил план v1.4 Final (исходная дата 22.10.2026), реализацию,
feature-ветку, тесты, Draft PR и только отчётное изменение этого handoff. SHA256
исходного вложения: 223d45ceb4c01b086d564202154fab908aa49d2a3a7180c2648e08934ea5e409.
План и нормативная поправка к §13.2 воспроизведены в docs/agent-mock-b/protocol-v1.md.
Исходное вложение и AGENTS.md не изменены. Только конечные пробелы строк копии плана
в новой документации удалены для git diff --check.

Официальная внешняя библиотека rfc8785==0.1.4 используется через PYTHONPATH, без
копирования исходников, новой установки или изменения зависимостей приложения.
Контроллер проверяет пакет при импорте. RFC-векторы (float/negative zero, Unicode,
escaping, UTF-16 key order) проверены до кода и повторно в тестах. Доменные схемы
запрещают float, duplicate keys, неизвестные поля, surrogate, invalid integer.

WORKER RESULT сохраняет доверенный workerResponseDigest; replay восстанавливает
его только из checkpointed prefix. VALIDATOR и simulated publisher проверяют связь
подтверждённых результатов одного run с разными operationId. Journal fsync и
независимый monotonic checkpoint моделируют durable authorization/action evidence.
Budget резервируется только WORKER INTENT, не возвращается после отказов/crash и не
обнуляется новым approval. Trusted adapter закрепляет один retained log на taskDigest.

Snapshot/diff проверяются по immutable bytes; scope/denylist, Base64/UTF-8/hash/mode,
case collisions и ресурсные пределы проверяются независимо от worker report.
workerKind=mock, publishAllowed=false, externalCalls=0. Никакого настоящего агента,
GitHub publisher, сети или рабочих credentials в runtime/тестах. Создание Draft PR —
отдельное разрешённое действие разработки, не simulated publisher.

### Файлы

Созданы:
- docs/agent-mock-b/protocol-v1.md
- tools/agent_mock_b/README.md
- tools/agent_mock_b/__init__.py
- tools/agent_mock_b/approval_store.py
- tools/agent_mock_b/budget.py
- tools/agent_mock_b/canonical.py
- tools/agent_mock_b/checkpoint.py
- tools/agent_mock_b/contracts.py
- tools/agent_mock_b/controller.py
- tools/agent_mock_b/diff_validator.py
- tools/agent_mock_b/evidence.py
- tools/agent_mock_b/fake_worker.py
- tools/agent_mock_b/fixtures/README.md
- tools/agent_mock_b/recovery.py
- tools/agent_mock_b/repository_store.py
- tools/agent_mock_b/simulated_publisher.py
- tools/agent_mock_b/snapshot.py
- tools/agent_mock_b/state_machine.py
- tools/agent_mock_b/tests/test_acceptance_15.py
- tools/agent_mock_b/tests/test_contracts.py
- tools/agent_mock_b/tests/test_snapshot_diff.py
- tools/agent_mock_b/tests/test_state_recovery.py

Изменён существующий docs/codex-handoff.md только этим отчётом. Удалений нет.

### Ветка и проверенный результат

Ветка: feature/agent-mock-b-v1-4.
Base remote main: e13fd1e19f531824e148fff1d17d481098929e43; дважды сверён через GitHub
перед реализацией. Локальный HEAD совпадал с ним. Устаревший origin/main ранее был
28f34f8e95785daf62ea54bd08dd4b97131b5eca (отставание 124 commit); ссылка актуализирована.
Проверенный implementation commit: 0b774e040fc62be0d38376c953817e8e4dd2c795,
tree c148287d4092c3fcdc1404e2b892fa783d7b6c42. Тесты выполнены на точном содержимом
этого commit до его фиксации. Следующий commit добавляет только этот handoff;
его SHA не подменяет SHA проверенного кода. Remote tree при публикации сверяется.

### Проверки

Новый полный запуск:

```bash
PYTHONPATH=/tmp/codex-rfc8785-igo5yiag:$PWD python -B -m unittest discover -s tools/agent_mock_b/tests -q
```

Результат: 49 test methods, 0 failures, 0 errors, 5.347 s; все 15 acceptance groups
выполнены, включая утверждённые дополнительные подслучаи. Ранние проходы 15/44/48
были промежуточными; окончательный результат — 49. Это новый запуск, не UP-TO-DATE.
Подслучаи сгруппированы через subTest и циклы и не выдаются за отдельные test methods.
Матрица и synthetic fixture metadata: tools/agent_mock_b/fixtures/README.md.

Также: git diff --cached --check; AST parse; проверка точного file scope; отсутствие
runtime network/process/legacy imports; исходный SHA256 плана сохранился. Проверен
итоговый diff на посторонние изменения и секреты: только synthetic тестовые данные.
Android build, emulator, смартфон и прежние v1/v2 suites не запускались: их файлы,
workflows, схемы Room, APK/signing/release и grants/hashes не изменены.

### Ограничения и дальнейшая работа

Это single-writer модель, не OS sandbox и не production tamper-proof store. Trusted
checkpoint anchor и operation ledger — отдельные synthetic adapters в памяти;
при потере adapters нет заявления о восстановлении доверия из файлов. Повторное
чтение использует retained independent anchor. Замена trusted store вне модели угроз.
UTC и monotonic clock инъецированы. Worker factory — trusted fixture, не недоверенный
Python. Тесты ограничивают буферизацию контроллера, не память генератора fixture.

Recovery read-only классифицирует prefix/tail и независимый worker receipt. Unknown
tail/pending outcome не принимается и не запускается повторно; автоматического
resume нет. Controller.stop допускает protective cancel без approval и фиксирует
CANCELLED только после synthetic acknowledgment; при outage только недоверенный
STOP_REQUESTED_UNVERIFIED. Reconciliation требует отдельной разрешённой процедуры.
Временный пакет может исчезнуть с Cloud session; при отсутствии остановиться,
не заменять JCS. Никаких гарантий реального worker/publisher эти тесты не дают.

Следующее действие: owner review кода, поправки и evidence/recovery ограничений.
Не выполнять merge, Ready for review, dispatch, реальный worker или production actions.

### Draft PR

На момент этого отчётного commit Draft PR ещё не создан; публикация — следующий
разрешённый шаг. Номер/URL и подтверждённый Draft status будут добавлены отдельным
отчётным commit в эту же ветку. Remote CI пока не запускался этой локальной проверкой.


### Публикация этапа B

Создан Draft PR #62: https://github.com/Arch-relic/AgroNaliz/pull/62.
Состояние при создании: open, draft=true, merged=false. Base main остаётся
e13fd1e19f531824e148fff1d17d481098929e43.
Первый remote implementation/report commit: 7f551c4bf869fb25ed3e0cf4ac7be48c0150cb54,
tree 9e547118cfca25373ee142b170191faf9212f8af точно совпал с local report commit
53bfce6f74caa8defe85dd386cad9daa0e7ef6d3. Код идентичен проверенному local
0b774e040fc62be0d38376c953817e8e4dd2c795; различаются только commit metadata и
группировка implementation/handoff в истории. Следующий commit меняет только этот
отчёт, фиксируя PR URL; тестируемый код не меняется. Local worktree после commit чист.
Автоматические remote CI не выдаются за локальные 49 прошедших тестов; их итог
на момент этой записи не проверен. Workflows/rerun/dispatch вручную не запускались.
PR оставить Draft. После этой отчётной публикации остановиться для owner review;
не merge, не Ready, не запускать настоящего worker/publisher и production actions.

## PR62: аудит защищённых путей, 23.10.2026

Обход подтверждён на исходном PR head cc6a93c28f20b7746387d7f275e7625f439dde55:
при явном allowlist synthetic ADD для APP/example.txt и .GIT/config проходил validator.
Причина: системный denylist сравнивал исходный регистр, тогда как только sensitive
keywords и snapshot collision check уже использовали lower(). Case-only DELETE/ADD
также обходил проверку конечного snapshot, поскольку исходный alias удалялся.

Исправление не меняет протокол, wire schema, specification/response bytes, JCS,
digests или case-sensitive allowlist. Только проверки защиты сравнивают ASCII lower
для системного denylist и protected selectors с прежними границами сегментов.
Все операции одного diff проверяются на одинаковый lower(path), включая DELETE/ADD
в обоих порядках. Ранее существовавшая проверка коллизий с неизменёнными файлами
сохранена. Нет case normalization записываемых путей и ослабления denylist.

Изменены tools/agent_mock_b/diff_validator.py,
tools/agent_mock_b/tests/test_snapshot_diff.py,
tools/agent_mock_b/tests/test_acceptance_15.py и только этот docs/codex-handoff.md.
Созданных/удалённых файлов нет. AGENTS.md, утверждённый план, main, workflows,
production policies, identity v1/v2 и Android/Room не менялись.

Отрицательные тесты сначала воспроизвели ошибку (100 failing assertions/subcases
в 12 методах snapshot/diff suite). После исправления новый полный запуск:

```bash
PYTHONPATH=/tmp/codex-rfc8785-igo5yiag:$PWD python -B -m unittest discover -s tools/agent_mock_b/tests -q
```

Результат: 54 test methods, 0 failures/errors, 5.485 s, все 15 групп приёмки, включая
все ранее утверждённые проверки. Добавлены upper/mixed-case denylist для ADD/MODIFY/
DELETE, custom protection, case-only DELETE/ADD, границы сегментов и сохранение
case-sensitive allowlist; end-to-end примеры дают PROTECTED_PATH, REJECTED validation,
zero publisherCalls и ни одного SIMULATED_* события. git diff --check passed.
Проверенный local code commit: 481bb5977047bbb93508a2e69950d9b8e7f9dd30;
следующий commit содержит только этот отчёт. Ветка feature/agent-mock-b-v1-4,
base main e13fd1e19f531824e148fff1d17d481098929e43.

Существующий PR https://github.com/Arch-relic/AgroNaliz/pull/62 остаётся Draft.
Предыдущий head cc6a93c... имел 7 успешных CI workflows, но это не проверка исправления.
Новый push запускает весь штатный PR CI; результаты исправленного head проверяются
отдельно и будут отражены в PR. Нет ручного dispatch/rerun, изменения workflow или
ослабления защиты. Модель остаётся synthetic; вывод не является гарантией OS sandbox.
После обновления Draft PR и проверки полного CI — остановиться для owner review.


## AGN-49 — подготовка PR #49 к внедрению, 24.10.2026

Цель: синхронизировать feature/issue-48-fixed-fill с актуальным main без
переписывания истории, сохранить точные проценты и совместимость данных.
Статус этого отчёта: локальная проверка завершена частично, remote CI ожидается.

Основа main: 4c21c59acf8febc71d123e3582b74eeac613b49e (сверена GitHub API).
Исходный PR head: cbec8d21d2dbf37443d086d2969c6bc14959e575. Локальная ветка
feature/issue-48-fixed-fill. Проверены изменения перед merge-коммитом; его SHA
будет зафиксирован следующим отчётным коммитом после проверки CI.

Git fetch не работал: proxy:8080 connection refused. Через GitHub API получены
37 отсутствующих blobs, дерево head и шесть оригинальных commits; все Git SHA
побайтно проверены перед записью локальных объектов. История PR восстановлена
точно, без synthetic замены commits. Выполнен git merge --no-commit --no-ff origin/main.
Единственный конфликт: docs/codex-handoff.md, два блока (вводная и добавленные
отчёты). Сохранены обе стороны, включая Issue48 и новые этапы main. Android-код
и тесты PR не потребовали исправлений. Новые файлы main сохранены без изменений.

На этапе AGN-49 изменён только docs/codex-handoff.md плюс включение изменений
main merge-коммитом. Созданных и удалённых специально для AGN-49 файлов нет.
Перечень файлов исходной реализации Issue48 приведён в предыдущем отчёте выше.
Схема Room24, entities, DatabaseProvider, DatabaseBackupManager, DataImportManager,
production workflows/policies и подпись не менялись относительно main.
FULL/MORE_THAN_HALF/HALF/LESS_THAN_HALF = 100/75/50/25%; суммирование четвертей
до умножения, без UPDATE старых записей. Вместимость из ТТХ связанного комбайна,
исторический снимок только как резерв; расчёт не записывает разрешённое значение
обратно. Объём и масса выводятся до трёх десятичных знаков без промежуточного
округления. Пользовательские базы и устройства не подключались.

Новые локальные запуски:
- source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:assembleDebugTest :app:test --console=plain
  — FAILED до компиляции: FileLockContentionHandler / Could not determine a usable wildcard IP for this machine.
- python3 docs/run-harvest-jvm-checks.py --gradle-lib <Gradle 8.13 lib>
  — OK, 62 JUnit tests, 0 failures/errors. Автономный Kotlin, не Android/Room.
- python3 -m unittest discover -s docs -p 'test_*.py' — 108 tests,
  106 passed, 2 skipped (необходим построенный APK/SDK); 0 failures/errors.
- git diff --cached --check — passed. Схема/backup/import/production paths
  сравнены с main, различий в реализации нет.

Локально нет /dev/kvm и emulator images; API23/35, Room migration, upgrade
1.0.10→1.0.11 и backup/restore ожидают штатного PR CI. Исторические success
из предыдущего отчёта не выдаются за проверки нового merge. Конфигурация проекта
и production допуск ради запуска не менялись.

PR #49: https://github.com/Arch-relic/AgroNaliz/pull/49 — open, draft=true,
merged=false перед публикацией. Следующий этап: публикация merge в ту же
feature-ветку без force и ожидание всех CI; исправление подтверждённых ошибок
в рамках AGN-49. Не merge в main, не Ready, не APK/Release, не production dispatch.
После отчёта остановиться и ожидать решения владельца.

### AGN-49: опубликован merge и исправлена точность малых значений

Опубликован remote merge 37b0b1c5e599c7bf0df1bb99e68545326c4bf13b,
parents cbec8d21d2dbf37443d086d2969c6bc14959e575 +
4c21c59acf8febc71d123e3582b74eeac613b49e, tree
5c80663a27e550f25f6338a1ba416b94c962ef6b (совпадает с локальным a70740b).
Обновление ref force=false с expected_sha; main не менялся. GitHub подтвердил
head/base, mergeable=true, draft=true, merged=false. Отсутствие конфликтов
не означает разрешение merge.

Найден дефект исходного PR: исключение number() для малых площадей действовало
и для объёма/массы <0.005, выводя больше трёх десятичных знаков. Новый тест сначала
воспроизвёл ошибку: 63 tests / 1 failure (0,001175 м³ вместо 0,001 м³).
Исправление ограничивает исключение прежней двухзначной точностью; объём/масса
округляются только при отображении до 3 знаков. Правило малых площадей и точность
хранимых/расчётных значений сохранены. Изменены дополнительно:
- app/src/main/java/ru/fo6osik/workjournal/HarvestResultsFormatter.kt
- app/src/test/java/ru/fo6osik/workjournal/HarvestFixedFillTest.kt
- docs/codex-handoff.md (это дополнение).
Созданных/удалённых файлов нет. Новый автономный Kotlin/JUnit запуск: 63 passed,
0 failures/errors. Новый Python: 108 tests, 106 passed / 2 APK-dependent skips,
0 failures/errors. git diff --check passed. CI исходного merge не заменяет CI
исправления; SHA проверенного результата и финальные runs будут записаны ниже.

Блокер CI: Agent Core trusted-validation, pull_request_target из main,
https://github.com/Arch-relic/AgroNaliz/actions/runs/40081154410,
job 120834818874: DENIED: Governance/workflow changes require separate owner review;
gate cannot authorize itself. Причина — исходное добавление workflow
.github/workflows/harvest-fixed-fill-checks.yml относительно main. Unit suite
Agent Core и production policy tests на merge прошли; trusted denial отдельно
не выдаётся за success. Защита не менялась, обход/ослабление/rerun не выполнялись.
Для полностью зелёного допуска требуется отдельное решение владельца по workflow;
AGN-49 не разрешает менять производственные правила. Остальные уже запущенные
Android checks ожидаются, окончательная готовность к merge не заявляется.


### AGN-49 — итоговые проверки и передача владельцу, 24.10.2026

Статус: синхронизация и проверки приложения завершены; полный допуск BLOCKED
отдельным trusted governance gate. PR #49 остаётся Draft, разрешение merge отсутствует.

Проверенный remote code commit: 3fb976d83d03a3d719c5064bd6f6c0c99526959e,
tree 41b28f408b4039524f0f97f61308fd38aa33ffcb. Его содержимое точно совпало с
локальным 78491dafe2881c96735f4e37bd16d487b2bff5a3; локальная ветка затем
приведена к точному remote commit. Основа main повторно сверена перед сдачей:
4c21c59acf8febc71d123e3582b74eeac613b49e. Merge 37b0b1c имеет обоих исходных
родителей; force/rebase/main push не выполнялись. Последующий отчётный коммит
изменяет только docs/codex-handoff.md, не код/тесты. Его автоматические повторные
CI не подменяют приведённые ниже результаты проверенного code commit.

Все новые штатные CI на 3fb976d завершились; 10 workflows success, 1 trusted gate failure:
- Main APK safe update checks [pull_request]: success; https://github.com/Arch-relic/AgroNaliz/actions/runs/40081459510
- Offline changelog UI checks [pull_request]: success; https://github.com/Arch-relic/AgroNaliz/actions/runs/40081459534
- Installed diagnostics and harvest typography [pull_request]: success; https://github.com/Arch-relic/AgroNaliz/actions/runs/40081459606
- MAIN Room22 to23 migration audit [pull_request]: success; https://github.com/Arch-relic/AgroNaliz/actions/runs/40081459536
- Fixed bunker percentages and MAIN 1.0.10 update [pull_request]: success; https://github.com/Arch-relic/AgroNaliz/actions/runs/40081459547
- MAIN 1.0.9 to1.0.10 Room24 same-schema audit [pull_request]: success; https://github.com/Arch-relic/AgroNaliz/actions/runs/40081459505
- Room24 synthetic MAIN TEST update audit [pull_request]: success; https://github.com/Arch-relic/AgroNaliz/actions/runs/40081459537
- Detached MAIN release policy proposal checks [pull_request]: success; https://github.com/Arch-relic/AgroNaliz/actions/runs/40081459528
- Agent Core validation [pull_request]: success; https://github.com/Arch-relic/AgroNaliz/actions/runs/40081459530
- Android PR checks [pull_request]: success; https://github.com/Arch-relic/AgroNaliz/actions/runs/40081459549
- Agent Core validation [pull_request_target]: failure; https://github.com/Arch-relic/AgroNaliz/actions/runs/40081457906

Подтверждённые результаты по завершённым job logs:
- MAIN/debug и TEST/debugTest собраны; JVM-задачи Gradle успешны. CI использует
  :app:assembleDebug :app:assembleDebugTest :app:assembleDebugAndroidTest
  :app:testDebugUnitTest :app:testDebugTestUnitTest; отдельный запуск с
  -PtestBuildType=debugTest собирает TEST instrumentation. Полное число JVM tests
  из XML в этой сессии не извлекалось; их отчёты приложены к Actions artifacts.
- Общая Android job 120835722001: 53 инструментальных вызова / 107 выполненных
  тестов, включая группы и повторы. Python в этой job: 13 + 8 + 29 tests passed.
- Fixed-fill 1.0.10→1.0.11: 12 вызовов / 12 tests (MAIN и TEST). Проверены SQL
  schema и все tables без изменений, исторические коды, backup→отдельная БД→
  roundtrip, повторный запуск, редактирование с возвратом, приоритет ТТХ.
- Room23→24 и Room24→24: по 10 вызовов / 10 tests; обе job подтвердили
  data/files/preferences/certificate retention, отдельный restore и restart.
- Room22 regression: 3 вызова / 3 tests. MAIN safe update: 3 / 3 tests;
  Python 18 + 10 tests passed.
- UI API23 и API35: по 6 вызовов / 6 tests; changelog UI: 3 / 3 tests.
- Итого по всем Android workflows: 106 инструментальных вызовов / 160
  выполненных tests, 0 failures. Повторы не выдаются за уникальные сценарии.
- Agent Core unit suite: 140 tests passed. Detached policy: 3 набора по 10 tests.
- Локальные финальные 63 Kotlin/JUnit и 108 Python (106 passed / 2 skips)
  описаны выше; они не смешиваются с CI и не называются эмуляторными.
- CI на merge 37b0b1c — промежуточный, не основание итоговых результатов выше.
- git diff --check passed; 7 файлов Room/entities/backup/import и все 12
  существующих workflows побайтно совпадают с main. Production workflow
  .github/workflows/main-apk-publish.yml SHA256:
  fd9c73989583e47ed7337a7f7fa904dde844fa2b864ea06a0e9666d9ab330712.

Fixed-fill artifact: 12555831642, fixed-fill-test-reports, API digest
sha256:61b0200c7c3889c9dd5f347093cf34b82093e7e0ec449f65ccf943925e7a6d5d.
Это метаданные GitHub; ZIP этого artifact локально не распаковывался и digest
скачанных байтов не проверялся. Фактические проверки подтверждены success job
и её логом с 12 OK (1 test), а не вымышленным локальным анализом JSON artifact.
Workflows загружают отчёты/diagnostics, публикация APK/Release не выполнялась.

Оставшийся блокер: trusted-validation job 120835718313, run 40081457906,
pull_request_target с кодом main, отказ:
DENIED: Governance/workflow changes require separate owner review; gate cannot authorize itself.
В PR присутствует исходный новый workflow .github/workflows/harvest-fixed-fill-checks.yml.
Gate не допускает любые workflow changes. Это не дефект расчётов и не повод
ослаблять защиту; gate/production policies не менялись. Rerun/bypass/manual
approval/dispatch не выполнялись. Даже при 10 success задача НЕ удовлетворяет
критерию «все автоматические проверки успешны»; требуется отдельное решение
владельца о допустимой процедуре review workflow, без автоматического merge.

Финальные изменения AGN-49 сверх исходного PR и включения main:
- docs/codex-handoff.md — оба конфликтовавших блока сохранены, отчёт дополнен;
- app/src/main/java/ru/fo6osik/workjournal/HarvestResultsFormatter.kt —
  исправлено отображение малых объёмов/масс до 3 десятичных знаков;
- app/src/test/java/ru/fo6osik/workjournal/HarvestFixedFillTest.kt —
  регрессия малых значений и сохранение прежнего отображения малой площади.
Созданных/удалённых специально для AGN-49 файлов нет. Незакоммиченных правок
кода при сдаче нет. Полный исходный scope Issue48 сохранён и описан выше.

Все данные тестов synthetic на одноразовых эмуляторах; физический телефон и
реальные пользовательские данные не использовались. API23/35 emulator success
не означает проверку на реальном устройстве. Room24 и backup совместимы по
пройденным миграциям/restore, downgrade не обещается. Локальный Gradle по-прежнему
ограничен wildcard IP; успех Android относится именно к CI, не локальной сборке.
Production 1.0.11 по-прежнему не допущена отдельной политикой выпуска.

PR: https://github.com/Arch-relic/AgroNaliz/pull/49. Перед отчётным commit:
open, draft=true, merged=false, mergeable=true (только отсутствие конфликтов).
Описание PR обновляется итогом AGN-49 и ссылкой на этот handoff из feature-ветки.
Следующий этап: решение владельца по blocked trusted gate и review реализации.
Остановиться; не Ready, не merge, не APK/Release, не менять подпись/production admission.

## AGN-49 — разделение приложения и инфраструктуры, 24.10.2026

Цель владельца: два отдельных Draft PR от актуального main, сохранение истории,
кода приложения, тестов и документации; при любом новом отказе безопасности
остановиться. Merge, APK/Release, обход защиты и изменение admission запрещены.
Текущая часть: приложение, feature/issue-48-fixed-fill, существующий Draft PR #49:
https://github.com/Arch-relic/AgroNaliz/pull/49.

Основа main: 4c21c59acf8febc71d123e3582b74eeac613b49e, сверена GitHub API.
Исходный head до разделения: 6bdf03a8a38ea5bb138d6e5df18082a4322e81d0.
История сохраняется добавленным commit, без rebase/force. SHA проверяемого
разделённого head будет указан в итогах проверки, не как хеш этого же отчёта.

Удалены из текущей ветки для переноса в feature/issue-49-fixed-fill-ci:
- .github/workflows/harvest-fixed-fill-checks.yml;
- docs/run-harvest-fixed-fill-checks.py — единственный вызывающий workflow,
  сценарий запуска специального upgrade-аудита; общие drivers не переносятся.
Изменён docs/codex-handoff.md только этим дополнением. Созданных файлов нет.
Все app/**, включая HarvestFixedFillUpgradeTest, расчёты/форматирование/DAO и
остальные тесты, а также docs/issue48-fixed-fill.md сохранены побайтно.
Прежние отчёты и результаты выше — исторические; они не заменяют новый CI head.

В PR #49 больше нет различий .github/** и agent_core/** с main. Все существующие
проверки pull_request остаются без изменений и должны запуститься на новом head.
Специальный fixed-fill workflow исключён из этого PR как инфраструктура;
существующие Android/Room/backup/UI/policy проверки не отключаются.
Room24, backup/import, signing, production admission и пользовательские данные
не меняются. Все проверки приложения используют synthetic данные.

Локальная проверка: сравнение дерева app/** и документации с исходным head,
scope diff и git diff --check. Повторная Android-сборка для удаления workflow и
переноса driver локально не запускалась; новый штатный CI и security checks
ожидаются после публикации head. Предыдущие 63 Kotlin и 160 instrumentation
результаты не выдаются за новый запуск. Независимая проверка уже подключена:
agronaliz-security-controller / agronaliz-security-gate и security-preview.
Исходный head имел исторический отказ PROTECTED_CHANGE (24.10 09:38 UTC),
до этой задачи; текущая задача явно разрешает разделить этот scope.

Проверки Agent Core trusted-validation и независимого controller выполнятся
автоматически при обновлении PR; при новом отказе остановиться, без rerun/bypass
и без последующей публикации инфраструктурного PR. Если application head принят,
следующий этап — инфраструктурный Draft PR от main, его отдельные отчёт и проверки.
Инфраструктурный PR пока не создан, номер неизвестен. Новый workflow остаётся
protected change; отдельное review не даёт автоматического разрешения merge.
Статус этого commit: разделение приложения подготовлено; новые проверки not_run.
