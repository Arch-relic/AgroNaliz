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
