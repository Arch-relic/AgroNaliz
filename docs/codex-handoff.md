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
