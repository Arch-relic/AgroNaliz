# MAIN 1.0.11 — безопасная подготовка выпуска по Issue #48

Дата подготовки: 26.10.2026. Статус: **только Draft, производственный выпуск НЕ разрешён**.

## Проверенная отправная точка

- После owner-approved PR #66: `AgroNaliz/main` = `fb4492c67f251802b032b7f226063c39ab747028`, Room24.
- В `main` находятся расчёты заполненности 25/50/75/100% и исправленные Android backup/Room тесты. Результаты **в коде**, а не в установленной APK.
- Пользователь ранее подтвердил установленную MAIN **1.0.10**, versionCode **3000008**, Room24, и наличие резервной копии от 24.10.2026. Перед установкой данные телефона и актуальная копия проверяются заново.
- Подтверждённый оригинальный сертификат APK: SHA256 `3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C`. Приватный ключ/секреты не читать и не копировать.
- `app/build.gradle.kts` пока содержит **1.0.10**; действующий `.github/workflows/main-apk-publish.yml` допускает Room24→24 только для **1.0.9→1.0.10**, а `.github/workflows/main-room24-same-checks.yml` закрепляет старый published 1.0.9 baseline. Эти правила **не подтверждают** выпуск 1.0.11.
- Последний PR #49 с исходной полной реализацией версии является старым Draft и имел неуспешный independent security gate. Не сливать его и не использовать как источник разрешения production.
- Изменения этой ветки ограничены редакторской историей 1.0.11, её тестами и документами. Записи Room, versionName, номер версии в APK, release workflows, подпись, security rulesets **не меняются**.

## Границы согласования

**Обычная подготовка в Draft PR (сделано):** новая запись в AppChangelog без выдуманной даты публикации; уже установленная 1.0.10 больше не помечается как «В разработке»; тесты порядка, статуса, интерфейса и динамического определения текущей версии. Целевая версия для будущего выпуска — **1.0.11**; product `versionName` пока прежний.

**Отдельное разрешение владельца на защищённые файлы (ещё не дано):**
1. `app/build.gradle.kts` — `versionName = "1.0.11"` при неизменном `applicationId`, `targetSdk`, Room, `versionCode = 1` по умолчанию и независимых MAIN/TEST code overrides.
2. Production `.github/workflows/main-apk-publish.yml` — расширение строгой цепочки **только** для установленной MAIN1.0.10/Room24 → candidate MAIN1.0.11/Room24, без замены исторических веток 1.0.8–1.0.10 и без прямого доступa к ключам в PR.
3. Новый read-only CI workflow (отдельный путь/ID, после review) и инструменты эмуляторного тестирования обновления. Привязанные version-dependent `docs/*.py` и Kotlin tests согласовать отдельно вместе с защищёнными путями, после подтверждения allowlist независимого gate. Не искать обход запретов пути через rename/копии.

## Контракт нового read-only аудита 1.0.10 → 1.0.11

- **Официально опубликованная MAIN1.0.10 подтверждена GitHub API:** owner/main manual publish run `39492121711`, `workflow_id=384685122`, path `.github/workflows/main-apk-publish.yml`, run number `8`, `attempt=1`, `conclusion=success`, `sourceSha=2a37bfe05c9b60f04035f7b64fae7f726117d647`, artifact ID `12296776599`, artifact `app-main-original-main-2a37bfe05c9b60f04035f7b64fae7f726117d647-v3000008`. Build и Sign jobs оба `success`, в том числе `Sign and verify stable MAIN certificate` и `Publish verified MAIN APK`. На pinned source подтверждены `versionName=1.0.10`, Room24, `applicationId=ru.fo6osik.workjournal`. Оригинальный cert pin `3FE57967...15EE3C` указан в trusted signer; **байтовый SHA256 опубликованной APK и содержание verification.json пока независимо не извлечены**, их нельзя придумывать. Перед официальным pin baseline получить и сверить именно этот verification.json и неизменность артефакта с GitHub. Не заменять pinned baseline исходниками 1.0.9 или HEAD PR.
- В CI строить закреплённую опубликованную 1.0.10 и candidate 1.0.11 как одноразовые, **не производственно подписанные** фикстуры, обе с ожидаемыми package ID, версией и Room24. Тестировать MAIN и изолированную TEST отдельно. Не использовать production keystore и реальные данные.
- До обновления засеять старую Room24: 10 FULL + 1 LESS_THAN_HALF бункер при 4,7 м³, плотность720 кг/м³, конкретные даты/UID/события/топливо/поля/площади, preference и файл; проверить историческое интервальное вычисление и JSON backup. При обновлении установить новую фикстуру **поверх** старой без clear/reinstall.
- После обновления: те же таблицы/значения/ID/FK/схема, файлы и preferences; новые вычисления **48.175 м³, 34.686 т, 3.4686 т/га** при 10 га и без диапазона. Проверить: изменение старого неполного бункера и возврат, повторный пересчёт, restart, JSON backup→отдельную БД→roundtrip, отсутствие миграции/массового UPDATE. Проверять сохранность завершённых и активных дней, событий и настроек.
- Результат аудита привязан к exact candidate source SHA, commit main, owner-run `workflow_dispatch`, первой попытке, trusted workflow path/ID, успеху каждого обязательного job/step; если хоть одно свидетельство отсутствует, **отказ до подписи**.
- PR CI не становится автоматическим разрешением на production: финальный owner/main audit нужно запустить **после отдельного merge** и подтвердить по GitHub API exact SHA. Отчёты/JSON без APK, без production secrets.

## Новый строго ограниченный production admission — только предложение

В текущем `main-apk-publish.yml` сохранить все старые маршруты и ветку `Room24 same_room` для **1.0.10**. Добавить *отдельный*, однозначный маршрут: `installedRoom == 24`, `targetRoom == 24`, `product == "1.0.11"`, `installedVersionCode >= 3000008`, ожидаемый опубликованный baseline1.0.10. Предусмотреть reject для неизвестного исходного продукта, неправильного Room, неподтверждённого исходного code и неподтверждённого audit run.

Проверять через GitHub API exact `workflow_id`, `path`, `head_sha`, `head_branch=main`, `event=workflow_dispatch`, `run_attempt=1`, `actor/triggering_actor=owner`, repository/head_repository, `conclusion=success`, полноту job/steps и отсутствие неоднозначностей. `head_sha` аудита обязан равняться source SHA выпуска. При отсутствии реального run нельзя заменять evidence строками в документации.

Повторить ту же проверку product/version/Room/code floor в отдельном подписывающем job; не менять ORIGINAL certificate pin, secrets boundary, cleanup, `applicationId`, механизм `versionCode = 3000000 + GITHUB_RUN_NUMBER`, запрет rerun/out-of-order и ограничения ручного owner/main запуска. Подписанная APK проверяется через `aapt2/apksigner`, SHA256, old-code strictly lower; TEST не становится MAIN.

**Обязательные отрицательные тесты:** legacy 1.0.8/1.0.9/1.0.10 не сломаны; неправильный/пустой audit ID, чужой SHA/actor/branch/fork, rerun, stale run, wrong workflow, skipped/failed/duplicate/missing jobs, неподтверждённая 1.0.10, Room mismatch, equal/lower/new code, неверный package/cert/signature, недоступный GitHub API, попытка TEST/APK pre-merge. Секреты при раннем отказе не декодируются.

## Дальнейший порядок — без иных задач

1. Проверить Draft PR с этой подготовкой: путь/состав файлов, CI, UX, отсутствие protected paths.
2. **Отдельно согласовать** конкретные изменения защищённых файлов, выпускной workflow и тесты по версии 1.0.11; после решения реализовать их в контролируемом scope. Не расширять права Dev Agent.
3. Провести независимые negative/positive тесты и CI на точном кодовом SHA, пройти правила protected main и получить отдельное разрешение на merge.
4. После успешного merge выполнить **отдельно разрешённый owner/main** audit exact source SHA, затем отдельно согласовать производственный signing и публикацию APK.
5. Перед установкой сделать свежий личный JSON backup и сохранить вне приложения; проверить реальные текущие versionCode/Room/подпись на телефоне, а также сохранность файлов/preferences. После сверки официального signed APK обновлять только поверх установленной MAIN. Проверить рабочие записи, бункеры, события, площади, топливо и расчёты на физическом устройстве.

**Ни эта запись, ни успешный Draft PR не являются разрешением на выпуск.**

## Установленная защита и результаты PR #74

Обязательный независимый App `agronaliz-security-gate` (ID5441843) на SHA `5cefab9abe42877a40347b9572d6c2f35729cff7` сообщил `failure: PROTECTED_CHANGE`. `agronaliz-security-preview` — `failure/protected_change`. Полные API-сведения о конкретном запрещённом файле не раскрыты; **не угадывать путь и не обходить охрану через rename, другой файл или workflow**. `main` использует ruleset 25782758: обязательные `agronaliz-security-gate` и `android`, active enforcement, no bypass. Владелец согласовал отдельное рассмотрение protected changes, но не снятие защиты, merge, owner/main dispatch, подпись или публикацию.

Результаты обычных Android/Changelog CI не заменяют отрицательный independent gate. Текущие правки PR #74 нельзя считать разрешёнными к merge. Для финала необходим **поддерживаемый владельцем и независимо проверенный процесс допуска protected paths**, а не правка candidate-side CI, не подделка check name и не отключение required status. Если административный путь недоступен — выпуск остановлен, а релиз остаётся заблокированным.

Подписанный production artifact 1.0.11 и реальная проверка на смартфоне не создавались.
