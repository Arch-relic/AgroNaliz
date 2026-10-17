# MAIN1.0.8: патч защищённой политики для согласования — НЕ ПРИМЕНЁН

17 октября2026. feature/harvest-1-0-8, [Draft PR33](https://github.com/Arch-relic/AgroNaliz/pull/33).
Никакого merge, изменения действующего signer, release dispatch, установки или
действий с реальной базой. Это проверяемое предложение, не разрешение выпуска.

## HEAD и последующие изменения

HEAD до этой задачи: dfc3baa6a0569fad6fcbc11fe04ca3ce2ad90a11.
После проверенного9fe8fa2f922c8e62dc124307663993387a56b2cf изменены только:
- docs/codex-handoff.md
- docs/main-1.0.8-release-audit.md
- docs/main-1.0.8-verification.json
- docs/main-room23-ci-review.md
- docs/main-room23-release-policy-proposal.md

Это отчёты и уточнение предложенного audit guard. Продуктовый код, тесты миграции
и workflows на этом промежутке совпадают. Main остаётся4d9ecfe7b7fb83a12f191d40855fdcd1c13cbaee.
Точный сценарий MAIN22→23 ранее прошёл в [audit#1](https://github.com/Arch-relic/AgroNaliz/actions/runs/39035077568).
[Полный предыдущий CI-отчёт](main-room23-ci-review.md):72 Python/90 инструментальных
выполнений. Они не выдаются за новый запуск тестов предложенного signer.

## Что именно подготовлено

[Полный unified diff](patches/main-room23-release-policy.patch),234 строки с контекстом.
[Manifest SHA256 до/после четырёх целевых файлов](patches/main-room23-release-policy.json).
Статус PROPOSAL_NOT_APPLIED. git apply --check проходит; сам git apply выполняется
только в TemporaryDirectory теста. Действующие целевые файлы остаются по beforeSha256.
Патч связан с их текущим содержимым; при drift нужен повторный review/регенерация.
Тест поддерживает также полностью применённое после отдельного разрешения состояние,
проверяя reverse --check, и отказывает смешанному/неизвестному состоянию.

Будущий патч изменил бы РОВНО четыре файла:
1. .github/workflows/main-apk-publish.yml:
   - defaults3000003/Room22, optional migration_audit_run_id (обязателен толькоRoom23);
   - прежняя ветка target22 со всеми прежними floor-парами20/21/22 сохранена;
   - новая узкая ветка target23 только installed22 и floor>=3000003, versionName1.0.8;
   - до сборки/ключей: owner/main/attempt1, run ID>0; API подтверждает id,
     workflow_id387641531, путь main-room23-checks.yml, точный source SHA, branchmain,
     свой repository/head_repository, owner actor/triggering_actor, manual dispatch,
     attempt1, status completed/conclusion success;
   - API jobs проверяет полный ответ, единственный job main-room23 и успешные четыре
     обязательных шага: сборка новой fixture, baseline22, input identity/unsigned,
     миграция/целостность/separate restore/restart. Skipped/нет шага — отказ;
   - audit_run_id передаётся в sign только через needs.build.outputs (не напрямую input);
   - actual MAIN package/code проверяется как раньше, дляRoom23 дополнительно
     versionName1.0.8 и проверенный audit output до key decode и после sign;
   - verification.json дополняется versionName/auditRunId/auditSourceSha/auditWorkflowId.
2. docs/test_main_apk_release.py: только synthetic signer helper получает Room22
   и fake audit ID123 для unit SDK fixture target23. Не меняет production guard;
   исходный старый20→23 эмуляторный тест и его данные остаются прежними.
3. docs/test_app_version.py: ожидание Room23 отказа уточняется как отказ без audit,
   с подходящим floor3000003. Позитивные/негативные ветки покрыты отдельным harness.
4. docs/main-original-update.md: новая инструкция1.0.8 с audit ID, точным SHA,
   backup и update; старая инструкция остаётся историей.

Прежние ограничения не убираются: только main/owner/attempt1, исходные ANDROID_*
Secrets, оригинальный полный pin, versionCode3000000+run_number и floor/порядок,
никаких downgrade, отдельный build без ключей и sign без checkout/Gradle,
private temp key0600, cleanup/transport expiry, publish только после проверок.
Нет PR/fork/push signing, новых постоянных ключей, изменения applicationId/Room/миграций.
Совпадение исторических сертификатов MAIN/TEST сохраняется, смена не требуется.

## Почему TEST не может случайно стать MAIN

Сборочная цель осталась assembleDebug, путь input app/debug/app-debug.apk.
Оба package checks требуют точное ru.fo6osik.workjournal, не prefix. TEST
ru.fo6osik.workjournal.debugtest отвергается до декодирования ключа даже при том же
сертификате. После sign package проверяется снова; попытка подмены на этом этапе
также отвергнута тестом. Signer использует ANDROID_*, а не TEST_*; публичный MAIN
pin сохранён. Проверка сертификата сама по себе НЕ используется для различения
MAIN и TEST — оно обеспечивается точным applicationId и закреплённым pipeline.

## Тесты предложения и фактические пределы проверки

docs/test_main_room23_policy_patch.py применяет diff только во временной копии,
проверяет хеши, компилирует точные предложенные preflight/signer блоки и исполняет
их с synthetic GitHub API/SDK/key fixtures. Реальной сети, Secrets и подписи APK нет.
10 методов проверяют позитивный owner/main audit и отказы: чужой SHA/repo/actor/
workflow/ref/event, rerun, statusfailure/skipped, missing/failed/truncated job/step,
API error, неверную схему/версию/floor/порядок, TEST/иной package, post-sign подмену,
неверный сертификат и отсутствие audit output. Legacy target22 ветка сохранена.
Во временной копии также проходят18 старых MAIN tests (17pass/1SDKskip) и6 version tests.

Локальный полный набор python3 -m unittest discover -s docs -p 'test_*py' -q:
82 всего,80 успешно,2 без APK пропущены (10 новых включены в82, не суммировать ещё раз).
Вложенные24 tests:23pass/1SDKskip, отдельно от верхнего счётчика. git apply --check,
проверка SHA manifest, py_compile и diff --check успешны. Android-код не менялся;
новая Android-сборка ради патча не выполнялась (известное wildcard IP ограничение).

READONLY_CI_PENDING

Нельзя обещать успешный production sign сейчас: действующая политика всё ещё
блокируетRoom23; наличие и pin оригинального ключа проверится только разрешённым
будущим signer. Исторический MAIN run#3 успешно использовал этот ключ, но это
не заменяет свежую криптографическую проверку. Synthetic SDK unit tests не являются
новой подписью реальной APK. Регулярные Android CI отчётного коммита — отдельные
автоматические запуски; production-код совпадает с проверенным9fe8fa2.

## Точный будущий порядок (сейчас НЕ ВЫПОЛНЯТЬ)

1. Отдельно разрешить конкретный patch. Только затем применить его в feature-ветке,
   проверить четыре target SHA/дифф, прогнать CI и снова review. Одно слияние PR33
   с НЕПРИМЕНЁННЫМ patch-файлом Room23 не разрешает.
2. Отдельно разрешить merge PR33; проверить Draft/CI/окончательный diff, выполнить
   штатное слияние без force. Записать новый main SHA M (не PR head/не audit9fe8fa2).
3. В установленной MAIN1.0.7 сверить публичные metadata: ru.fo6osik.workjournal,
   фактический code (baseline3000003), сертификат и Room22 из её JSON-backup.
   При другом/неизвестном состоянии не угадывать floor, остановиться.
4. В MAIN1.0.7 открыть существующий экран «Резервное копирование» (старый developer
   блок), «Создать резервную копию», сохранить через SAF вне песочницы приложения.
   Проверить, что файл доступен независимо от приложения, корректный JSON с
   backupFormatVersion1/databaseVersion22/tables; сохранить приватную вторую копию.
   Не брать TEST backup и не отправлять рабочий JSON в GitHub/чат/Actions.
   JSON копирует DB, не все preferences/files; это не полный образ телефона.
   Не восстанавливать backup в рабочее приложение ради проверки. Проверка реальной
   копии, если нужна, только отдельно и в приватной временной базе.
5. Owner запускает MAIN Room22 to23 migration audit через workflow_dispatch на main M.
   Ждёт полного success, записывает audit ID A. При изменении main перед публикацией
   требуется audit нового SHA. Нынешний PR-triggered#1 не подходит новой строгой policy:
   она намеренно требует owner/main/manual audit именно M.
6. Owner запускает Publish original main APK / main с minimum_installed_version_code
   =фактическому code (3000003 baseline), minimum_room_version=22,
   migration_audit_run_id=A. Build фиксирует SHA M и проверяет evidence.
   Следующий N workflow даёт3000000+N (при следующем#4 —3000004), не задавать/сбрасывать
   code вручную. Не Re-run: при ошибке исправление и новый dispatch по правилам sequence.
7. После success скачать только app-main-original-main-… ZIP, не internal transport.
   verification.json: sourceSha/auditSourceSha=M, auditRunId=A, applicationId MAIN,
   versionName1.0.8/Room23, versionCode>установленного, оригинальный certificate,
   apkSha256. Независимо сверить SHA APK и её фактические package/cert/code в App Manager.
   Публичного GitHub Release не создавать. Если ключ недоступен/несовместим — остановиться.
8. Только после отдельного разрешения владельца на реальном устройстве открыть APK
   и обновить существующую MAIN без удаления/очистки. Сейчас агент это НЕ выполняет.
   Не устанавливать TEST вместо MAIN. При ошибке подписи/code остановиться, не обходить её.
9. После update проверить свои технику/поля/рабочие записи, кампании/дни/бункеры/
   события/топливо, активный день, настройки/файлы и новую Options. Сохранить отдельный
   новый backup23, не перезаписывая22. У старых кампаний новые площади NULL — ожидаемо.
   Не откатывать APK до Room22 и не восстанавливать поверх рабочей базы без отдельного
   безопасного плана: DB downgrade может быть несовместим.

Риски: неизменность workflow ID/step names важна; при их смене guard блокирует выпуск
до review. API failure также блокирует. Одноразовый сертификат CI не доказывает
оригинальный production ключ; обязательна его последующая проверка. Телефон/OEM и
текущее состояние реальной базы не проверялись. Никаких данных/ключей не получали.

## Файлы этой задачи

- docs/patches/main-room23-release-policy.patch (новый, не применён)
- docs/patches/main-room23-release-policy.json (новый manifest)
- docs/test_main_room23_policy_patch.py (новый temporary-copy test harness)
- .github/workflows/main-policy-proposal-checks.yml (новый readonly CI теста, без Secrets/APK)
- docs/main-room23-policy-patch-review.md (новый отчёт)
- docs/codex-handoff.md
Удалений нет. Защищённые workflows и их тесты в рабочей ветке не менялись;
предложенные правки видны только внутри diff-файла. После отчёта остановиться.
