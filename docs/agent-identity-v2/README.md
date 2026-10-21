# Agent Identity v2 — проект технического задания

Статус: **Draft для review; только документация, ничего не внедрено**.
Редакция проекта: 1, 21.10.2026. Источник: [Issue #52](https://github.com/Arch-relic/AgroNaliz/issues/52)
и [архитектурное review владельца](https://github.com/Arch-relic/AgroNaliz/pull/51#issuecomment-6315029722).
Целевая архитектура принята как направление; конкретные решения ниже — предложения
для утверждения. Merge документации не заменяет разрешение создавать App или запускать тесты.

Документы составляют одно ТЗ:

- этот файл — требования R01–R16;
- [модель угроз](threat-model.md) — границы доверия и остаточные риски;
- [матрица приёмки](acceptance-matrix.md) — проверки A01–A32 и необходимые свидетельства;
- [решения и этапы](rollout-and-decisions.md) — варианты D01–D12, рекомендации и разрешения.

## 1. Фактическая база и границы работ

Проверен main `e5e0aeae9474307562a90ca4d3d075dc11800841`, tree
`159b09961b9f50c2d46633238de00e88330978e0`; PR #47 merged. Владелец —
GitHub user ID `240867647`, repository ID `1411593273`, `Arch-relic/AgroNaliz`.
Main API сообщает `protected:false`, rulesets `[]`; это не проверенная защита ветки.
PR #51 — open/Draft, head `1248890d81b109ff7a955daa273454a2361a4765`,
offline bootstrap plan. Его executor_id=123 не является действующим исполнителем.

Исследованы текущие `agent_core/core.py`, `agent_core/check.py`, task-v1 schema,
`.github/workflows/agent-core.yml` и актуальный handoff из main. В v1 executor_id
сравнивается с GitHub actor; owner-only dispatch не предоставляет отдельную машину.
Fresh-main revalidation уже существует. Версии App-протокола сейчас нет.

Разрешённый результат Issue #52 — только этот docs-only Draft PR и handoff.
Не изменять код, schema, workflow, действующую policy, permissions, secrets, tasks,
approvals, приложение, Room24, backup, MAIN/TEST signing, release admission или main.
Не создавать/устанавливать App, ключи, токены, live tasks, dispatch, release/APK,
автономный Codex, webhooks/server. Требования ниже описывают **будущую** реализацию.

## 2. Цель и сравнение идентичностей — R01

Разделить человеческое согласование, машинного субъекта API-запросов и доверенный
контекст исполнения. Одно доказательство не заменяет остальные.

| Признак | Actions actor/run | Отдельная GitHub App installation |
| --- | --- | --- |
| Смысл | Инициатор и происхождение конкретного запуска | Приложение, установленное в аккаунт с доступом к репозиторию |
| Идентификаторы | actor/triggering_actor, run/attempt, workflow/ref/SHA/event | app_id, installation_id, account ID, repository ID |
| Даёт | Проверяемый execution context | Отдельную машинную identity и ограниченные API credentials |
| Не даёт | Независимого машинного исполнителя только из owner dispatch | Owner approval, доверенность исполняемого кода или scope задачи |
| Отзыв | Остановка/запрет запуска | Отзыв credentials, suspension/uninstall |

GITHUB_TOKEN — installation token встроенного Actions App, а не выделенного Dev Agent.
Собственный App token не меняет actor уже запущенного workflow. Контроллер вправе
быть запущен владельцем: initiator=User и executor=App — разные типизированные роли.
Отдельную человеческую учётную запись для обхода ограничений не создавать.

## 3. Типизированный субъект и доверенная регистрация — R02

Предложить task schema_version=2 с отдельным executor object:

| Поле | Требование |
| --- | --- |
| kind | Строго github_app_installation |
| app_id | Реальный положительный integer, подтверждённый App API |
| installation_id | Реальный installation ID именно этого App |
| installation_account_id | Закреплённый аккаунт установки, первоначально owner 240867647 |
| repository_id | Для AgroNaliz закреплён ID 1411593273 и ожидаемое имя; изолированный стенд имеет отдельный явно согласованный subject/registry |
| policy_version | Неизменяемая версия доверенной политики с проверяемым hash |

User/App/installation IDs не взаимозаменяемы, даже при совпадении чисел. Bot login,
автор коммита, имя App, client ID, токеновый префикс и environment сами по себе
не доказывают identity. Владелец App может быть владельцем проекта; запрещено
**машинное самосогласование**, а не человеческое владение App.

Будущий registry хранит проверенные IDs, допустимые repository selection, scope,
workflow IDs/paths и approved controller/policy hashes. Он не приходит из PR или
task parameters. Рекомендуемое размещение/проверка — D03; это описание registry,
не создаваемый этим PR файл policy. Неизвестные IDs не заполнять фиктивными значениями.
Отсутствие конкретных IDs до создания App — блокер provisioning, не пробел проектирования.
Смена владельца/репозитория/App/installation требует отдельного owner review.

## 4. Совместимость v1 — R03

Сохранить исходные JSON v1, executor_id как user ID, редакции, hashes, grants,
ссылки на runs и историю. Не переинтерпретировать v1 ID как App ID, не пересчитывать
исторические hashes и не объявлять старый grant разрешением v2.

Рекомендация: новые v2-задачи с уникальными ID и optional predecessor_task_id для
исторической связи. В рамках App controller v1 допускается читать/проверять как
историю, но нельзя исполнять от имени App. Действующий v1 validator не ломать.
Для продолжения старой задачи нужна отдельная v2-задача и новое owner approval;
старую отменять только через согласованный append-only переход, не удалять.
Task states сохранить из v1; approval requests/evidence хранить append-only.
Реализация schema/validator будет отдельным PR, не частью текущей документации.

## 5. Approval v2 — R04–R06

R04. Единственный approver — `kind=github_user`, ID 240867647. Существующая
owner/main/attempt1 проверка остаётся основой; App review/comment/check/label,
installation и пользовательский OAuth token App не становятся owner consent.
Автоматизации не получать GitHub App user access tokens/PAT владельца и не
имитировать человеческий dispatch. GitHub actor подтверждает аккаунт; протокол
не доказывает физическое присутствие человека и не защищает от компрометации owner.

R05. На main до approval хранится immutable approval request с контекстом:

- protocol_version=2, task ID, revision ID, spec_hash, operations_hash;
- полный typed executor либо его canonical digest с независимо прочитанным object;
- repository_id, policy_version и policy_hash;
- history_position, уникальный request nonce, UTC not_before/not_after.

Предлагается append-only approval_requests array. Добавлять request можно только
в awaiting_approval, history_position равен текущей длине history. Для этой позиции
активен последний добавленный request; предыдущие не удаляются, но не могут дать
новый approved transition. Approved event явно ссылается на request nonce/context_hash
и run ID. После этого новые requests для уже использованной позиции запрещены.
Verifier сверяет выбор request с previous task на main и immutable task из run.head_sha.
Для повторного согласования сначала append-only переход в awaiting_approval;
нельзя обновить request незаметно в approved/implementing/review.

Typed executor закреплён на весь v2 task. Смена App/install/account/repo/policy_version
требует нового v2 task с predecessor link и нового consent; редактирование executor
задним числом запрещено. Новый spec в рамках того же subject — новая revision,
awaiting_approval и новый request. Это не автоматическая миграция истории.

Рекомендуемые hash/canonicalization — существующий SHA256 digest v1:
sort_keys, compact separators, ensure_ascii, UTF-8, запрет NaN/duplicate keys.
Для нового request используется отдельный context_hash; inputs не являются authority.
Даты request — строгий UTC RFC3339 с секундами, без неоднозначных локальных времён.
Интервал полуоткрытый: not_before <= now < not_after. Доверенные UTC-часы controller;
при обнаруженном расхождении с серверным временем GitHub более60секунд — отказ.
После not_after нет grace period, clock skew не продлевает consent. Эти значения
— рекомендуемая политика для review, не заявленная гарантия синхронности GitHub API.
Рекомендуемый срок owner consent — максимум 8 часов (D06); token lifetime иной.
Nonce — 128 бит из криптографического RNG при подготовке request; он не secret.

Владелец запускает будущий approval-v2 workflow из main, только после отдельного
разрешения. Trusted job читает request по собственному GITHUB_SHA и сверяет весь
context. Предложенный run title: approve-v2:<task>:<context_hash>; хранить также
доказательство protocol version и approved workflow source SHA. Старый v1 title
не принимается как v2. Доверенный verifier проверяет формат/hash независимо.

Не включать SHA содержащего request коммита в его собственное хешируемое тело:
это циклическая зависимость. Immutable task/request commit связывается через
run.head_sha GitHub; evidence сохраняется следующим metadata commit.

R06. Верифицировать через GitHub API: repository ID/name и head repository,
workflow ID/path и разрешённую версию source, event=workflow_dispatch, main,
actor=triggering_actor=owner, attempt1, completed/success, точный context_hash.
Прочитать request/task из run.head_sha и сверить revision, identity, scope,
policy и точный history prefix/position. Grant привязать к одному переходу approved;
старый nonce/run нельзя повторно использовать для нового approval/другой задачи.
В пределах действующего scope допускаются несколько действий с новым authorize
перед каждым; nonce не означает «ровно один API read на всю задачу».

Изменение spec/identity/operations/policy/request или истечение срока требуют
нового request и нового owner approval. Обновление installation token consent не
продлевает. Immutable request/history не переписывать; при конфликте — отказ.

## 6. Контроллер и проверка credentials — R07–R10

R07. Рекомендуется отдельный on-demand GitHub-hosted Actions controller (D01),
логически отделённый от approval workflow и от любого untrusted job. Нет daemon
или нового сетевого сервера. Первый execution trigger — ручной owner dispatch,
но запуск контроллера сам по себе не выдаёт grant. Другие triggers запрещены,
пока отдельно не согласованы. Код, imports, dependencies и policy — из проверенного
main SHA, actions закреплены SHA. Никакого PR checkout/import/run, cache восстановления
недоверенного кода или выполнения script/path из inputs в credential-bearing job.

Проверить независимо через GitHub API и согласованную source policy: repository,
head repository, run ID/attempt1, event/ref/main, workflow ID/path, точный source SHA,
активный статус run, initiator/triggering_actor IDs и утверждённый запуск owner.
GITHUB_* сверяются с серверными данными, не считаются самостоятельным доказательством.
Существующую execution_actor() не заменять App ID в строке; нужен отдельный v2
execution context и verifier, не fallback на старую user identity.

R08. Controller владеет credential lifecycle. App JWT подписывается ключом в
изолированной среде; GET /app подтверждает app_id. Через App-authenticated API
проверить installation, account, repo association, suspended_at и granted permissions.
JWT и installation token — разные credentials; GET /app не принимает installation
token как замену JWT. Связь API response с доверенным mint request обязательна.

При mint явно задать единственный repository_id и точный permissions object;
проверить returned scope/repos/expiry и независимую доступность нужного repository ID.
Полные списки получать с pagination; отсутствие/обрезка/неоднозначность — DENIED.
Token из worker/input/PR никогда не подставлять в trusted transport. Не пытаться
доказать identity декодированием токенового префикса/формата; GitHub форматы меняются.

R09. Секреты только у контроллера (D02). Worker/test job не получает App key/token,
не использует persistent credentials и не разделяет mutable workspace с controller.
Для первого read-only proof worker вообще не требуется: controller выполняет
фиксированную smoke operation. Ключ не сохранять в репозитории, artifacts, outputs,
кэшах или logs; masked logging не заменяет изоляцию. Не печатать JWT/HTTP auth headers.
Token — только память/минимальный ограниченный lifecycle, revoke в finally;
при невозможности отзыва остановить новые действия, сообщить оставшийся срок.
У private key нет автоматического срока истечения: нужен процесс ротации/инцидента.

R10. Read-only installation permissions:

| Permission | Уровень | Цель |
| --- | --- | --- |
| Metadata | Read, обязательный GitHub baseline | Repository identity |
| Contents | Read | Tasks, refs, trusted source/policy |
| Actions | Read | Run evidence и metadata |
| Pull requests | Read | PR/base/head/files validation |

Только selected repository AgroNaliz; не all repositories. Все остальные права
не запрашивать, включая Issues, Checks/Statuses write, Actions write, Workflows,
Administration, Secrets, Packages. Actions job GITHUB_TOKEN также остаётся read-only;
права App и GITHUB_TOKEN — отдельные слои, одно не расширяет другое.
Для первого изолированного proof D11 выбран только его test repository вместо
AgroNaliz, с отдельными registry/task/consent. При переходе убрать прежний repo
из selection, проверить отсутствие лишнего доступа, затем отдельно разрешить
AgroNaliz binding. Не требовать двух installations одного App на одном аккаунте:
installation может остаться той же, но repository subject и consent обязательно новые.
Нельзя одновременно использовать test binding как допуск к AgroNaliz или выдавать
оба репозитория одному token для удобства.
У GitHub installation token обычный срок 1 час; использовать returned expires_at,
не обещать API TTL 5 минут. Предлагаемый controller session timeout 10 минут,
не больше оставшихся сроков token/consent; один run — один task/request.

`test` в первой v2-задаче означает только фиксированную операцию
read_identity_smoke: GET identity/repository/task/run metadata и запись безопасного
отчёта стандартным CI-механизмом. Это не произвольный shell test command.
Нет implement/technical_fix/draft_pr или записи metadata от App. Даже активный
grant не разрешает room, backup, signing, production_policy, merge_main, publish_apk.

## 7. Проверка каждого действия и отзыв — R11–R13

R11. Проверка выполняется доверенным controller, а не по заверению worker:

1. Проверить trusted execution context и App/install identity, получить ограниченные credentials.
2. Прочитать main SHA и task/registry/policy строго по этому immutable SHA.
3. Сравнить snapshot и полный executor subject; проверить state, revision, scope,
   request/evidence, history, not_before/not_after и policy hash.
4. Заново проверить active installation/repository access/permissions и token expiry;
   устаревший cached registry/token не считается новым доказательством.
5. Повторно получить main SHA, execution status и срок consent/token непосредственно
   перед фиксированной операцией. Смена SHA даже без изменения задачи — отказ.
6. Выполнить одну разрешённую операцию; следующая снова требует всех проверок.

Если source policy поменялась относительно approved controller source, не исполнять
новую policy старым кодом: потребовать новый согласованный controller/run и consent.
Никаких пользовательских API endpoints/callback/actor IDs. HTTPS verification
обязательна, секреты не передаются на произвольные redirect hosts. Timeout/403/404/
rate limit/incomplete data/missing fields всегда DENIED с безопасной причиной.

R12. Task revocation: blocked/cancelled/awaiting_approval, новый spec/executor/policy
или истечение consent запрещают новое действие. Изменение task вступает в силу
после фиксации в main, не от comment/label/закрытия issue/незамерженного PR.
История сохраняется. Deleted task/evidence — отказ, не восстановление полномочий.

R13. Infrastructure revocation: запрет новых controller jobs/выдачи credentials,
revoke token, suspension/uninstall installation; при компрометации ключа — owner
rotation и удаление старого ключа плюс отдельная обработка выданных tokens.
Не считать смену ключа доказательством отзыва всех существующих tokens. После
reinstall/изменения permission set — повторная привязка identity и новый consent.
Не требовать Administration write от исполняющей App для её собственного отзыва:
действия выполняет владелец вне executor.

Это point-in-time authorization. Нельзя обещать атомарность проверки с GitHub API
действием, немедленную остановку in-flight запроса или обнаружение main ABA
(смена SHA и возврат между чтениями). Предусмотреть прекращение последующих действий
и журнал; уже завершённые действия не откатывать разрушительными операциями.

## 8. Audit evidence, критерии и исключения — R14–R16

R14. Для решения хранить UTC, outcome=allowed/denied/blocked/not_run, reason code,
task/revision/spec/context/executor/policy hashes, repository/app/install IDs,
main/source SHA, run ID/attempt/workflow/event/actors, request position, expiry,
операцию и ссылки на metadata PR/check. Token/key/полный event payload не хранить.
Отчёт worker не authority: evidence verifier читает GitHub API заново.
Недоступность исторического run означает отказ. Рекомендуется 90 дней audit retention
как предложение D09; expired grants не становятся активными после потери журнала.
Технические отчёты продолжать в docs/codex-handoff.md, не дублировать их новой системой.

R15. Приёмка реализации — A01–A32 с отчётом по точному source SHA, отдельными
offline/live результатами. Текущий docs PR не создаёт schema v2 или тестовую систему
и не выдаёт результаты будущих проверок за выполненные. PR #51 остаётся offline:
P1–P4 с независимой identity блокированы до отдельно разрешённой интеграции.

R16. Любые write permissions — отдельное ТЗ/security review/разрешение владельца,
проверенная branch protection и обязательные независимые checks до выдачи прав.
Не добавлять App в bypass list. Contents:write не ограничивается feature branches
и охватывает releases; branch protection сама по себе не запрещает release API.
Даже после защиты main нельзя объявить такой token «только Draft PR».
Первое внедрение остаётся read-only; варианты будущей записи — D10.

## 9. Официальные источники и границы фактов

Сверены 21.10.2026; платформенные возможности перед реализацией проверить заново.
Наши TTL/controller/schema решения — проектные рекомендации, не возможности GitHub API.

- [GITHUB_TOKEN](https://docs.github.com/en/actions/concepts/security/github_token): встроенный App token и job lifecycle; не путать с initiator.
- [Installation tokens](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-an-installation-access-token-for-a-github-app): mint, явные repo/permissions, expires_at.
- [App API](https://docs.github.com/en/rest/apps/apps): App JWT, installation association и App IDs.
- [Installation API](https://docs.github.com/en/rest/apps/installations): repository access и revoke token.
- [Private keys](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/managing-private-keys-for-github-apps): хранение/ротация.
- [Permissions/endpoints](https://docs.github.com/en/rest/authentication/permissions-required-for-github-apps): реальные границы прав.
- [OIDC claims](https://docs.github.com/en/actions/reference/security/oidc): возможная будущая external credential boundary, сейчас не требуется.
