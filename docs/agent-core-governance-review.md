# ТЗ: одноразовый governance review Draft PR #55

Редакция 1, 21.10.2026. Основание: [Issue #56](https://github.com/Arch-relic/AgroNaliz/issues/56).
Статус: **проект процедуры для review**, не governance admission и не разрешение merge.
Документ не является исполняемой policy и не меняет полномочия Agent Core.
PR55 остаётся Draft. G3–G6 не разрешены.

## 1. Цель и объём

Спроектировать минимальный проверяемый порядок отдельного человеческого owner review
конкретных offline governance changes PR55. Сохранить штатный отказ trusted gate,
разделить review, решение о принятии риска и отдельное разрешение на merge.

Текущий этап допускает только документацию и read-only проверку evidence.
Нельзя менять код, workflows, permissions, settings/rulesets/protection, secrets,
policy/tasks/approvals, App/environment, main; выполнять GitHub review approval,
admission, dispatch/re-run, merge, APK/release. Описанные ниже будущие шаги не выполняются.

Изменения этого документа и его docs-only PR не добавляются в PR55. Их возможное
слияние изменит main и сделает текущую пару head/base предметом повторной проверки.
Слияние документации само по себе не допускает PR55 и не разрешает G3.

## 2. Фактическая база

Read-only snapshot 21.10.2026:

| Объект | Значение |
| --- | --- |
| Репозиторий | Arch-relic/AgroNaliz, repository ID1411593273 |
| Владелец | GitHub User ID240867647, Arch-relic |
| Main/base PR55 | e5e0aeae9474307562a90ca4d3d075dc11800841 |
| PR55 head | f6f6707dff9bfca4c9634c3119f240902093ff95 |
| Head tree | 77d2927c89061526a01fc0b5c21bceb08d31597a |
| Состояние | open, draft=true, merged=false; автор PR — тот же owner |
| Diff | 23 файла; 21 добавление, 2 изменения docs; удалений/переименований нет |
| Архитектура G1 | PR53 head96d8535ec544fcd13e93335e3d0f096c966da0f8, D01–D12 |
| G2 разрешение | Issue54 comment6315717967, не merge authorization |

Проверены GET branches/main, rulesets, rulesets?includes_parents=true, PR55/files,
reviews, head check-runs/status, workflow runs/jobs и trusted job log.
PR55 reviews=[]; это не доказательство отсутствия любых обсуждений, но формального
GitHub APPROVED review на момент проверки нет.

### 2.1. Trusted gate: source, условия, результат

Исходники закреплены на base SHA:
- [workflow](https://github.com/Arch-relic/AgroNaliz/blob/e5e0aeae9474307562a90ca4d3d075dc11800841/.github/workflows/agent-core.yml#L41).
- [PR validator](https://github.com/Arch-relic/AgroNaliz/blob/e5e0aeae9474307562a90ca4d3d075dc11800841/agent_core/check.py#L84).

Workflow ID390876734, path `.github/workflows/agent-core.yml`.
Обычный `pull_request` запускает job `tests`; `trusted-validation` там skipped.
`pull_request_target` opened/synchronize/reopened запускает trusted-validation с
checkout **event.pull_request.base.sha**, persist-credentials=false, permissions
contents/actions/pull-requests=read. Лог job119224610271 подтверждает checkout base e5e0aeae.
PR-код не импортируется доверенной проверкой; candidate JSON читается через API по SHA.

`check.run('pr')` проверяет repository/event/attempt1, неизменность head/base через
current_pr(), main как target, task histories и полноту списка changed files.
Учитывает filename и previous_filename. Отклоняет пути agent_core/ и .github/workflows/,
кроме agent_core/tasks/ и agent_core/recommendations/, у которых собственная валидация.
После успешного пути выполняет повторный current_pr(). При раннем отказе финального
current_pr нет; отказ всё равно не выдаёт разрешения.

В PR mode нет owner override, allowlist PR numbers, label/comment/review consumer,
governance dispatch mode или механизма сменить failure на success по решению владельца.
Owner-only task approval в другом режиме — отдельный v1 протокол, не admission этого PR.
Даже owner-authored PR с успешными tests должен получить этот отказ на governance paths.

Подтверждённый отказ на head f6f6707d:
[run39600837769](https://github.com/Arch-relic/AgroNaliz/actions/runs/39600837769),
event=pull_request_target, attempt1, job/check119224610271, app GitHub Actions ID15368:

> DENIED: Denied: Governance/workflow changes require separate owner review; gate cannot authorize itself

### 2.2. Check context нельзя определять только по имени

На одном head есть два `trusted-validation` от одного Actions App:
- check119224610271 — failure из pull_request_target run39600837769;
- check119224620151 — skipped из pull_request run39600840702.

Skipped не отменяет failure. Review evidence идентифицирует SHA + workflow ID/path +
event + run/attempt + job/check ID + conclusion; нельзя выбрать «последний check с этим
именем». Combined commit status API вернул pending/total_count=0/statuses=[]: это legacy
statuses, не агрегат Checks API и не свидетельство незавершённости всех CI.
Будущий formal admission должен иметь однозначные check identities; это потребует
отдельного разрешения на workflow/policy, сейчас ничего не переименовывать.

### 2.3. Main protection и техническая возможность merge

GET branches/main: protected=false; protection.enabled=false;
required_status_checks.enforcement_level=off, contexts=[], checks=[].
GET rulesets и includes_parents=true: оба []. Полный GET branches/main/protection
вернул **403 Resource not accessible by integration**. Дополнительные admin permissions
не запрашивались. 403 не трактуется как отсутствие защиты или согласие владельца.

Доступные данные указывают, что этот failure сейчас не закреплён как required check.
Следовательно, после отдельного решения владельца и снятия Draft им самим GitHub
может технически допустить обычный ручной merge без изменения gate/защиты. Это условный
вывод, а не проверенная merge-операция: владелец должен подтвердить актуальные effective
rules и merge UI. `mergeable=true` означает отсутствие git-конфликта, не governance допуск.

Если UI показывает required failing check, запрет merge, неизвестное правило или
необходимость bypass/admin override — STOP. Ни данный документ, ни owner acknowledgement
не разрешают отключать/обходить такое правило. Требуется новое самостоятельное решение.
Незащищённый main — остаточный риск admin/write actors, не предлагаемое средство защиты.

### 2.4. Обычные CI на pinned head

Все следующие runs завершились success, attempt1; это проверено по GitHub metadata,
а не новым запуском suite в этом docs-only этапе:

| Run | Проверка |
| --- | --- |
| [39600840702](https://github.com/Arch-relic/AgroNaliz/actions/runs/39600840702) | Agent Core, tests job119224618060: 140 tests |
| [39600840811](https://github.com/Arch-relic/AgroNaliz/actions/runs/39600840811) | Android PR |
| [39600840775](https://github.com/Arch-relic/AgroNaliz/actions/runs/39600840775) | MAIN safe update |
| [39600840790](https://github.com/Arch-relic/AgroNaliz/actions/runs/39600840790) | MAIN Room22→23 |
| [39600840793](https://github.com/Arch-relic/AgroNaliz/actions/runs/39600840793) | Room24 MAIN/TEST update |
| [39600840872](https://github.com/Arch-relic/AgroNaliz/actions/runs/39600840872) | Room24 same-schema |
| [39600840802](https://github.com/Arch-relic/AgroNaliz/actions/runs/39600840802) | UI API23/35 |

Наличие зелёного head CI не доказывает проверку будущего merge commit на изменившемся
main, отсутствие всех дефектов, живую App identity или разрешение governance changes.

## 3. Trust boundary и доступные механизмы owner review

Владелец принимает человеческое решение о конкретном diff и остаточных рисках.
CI доказывает только результаты конкретных checks на конкретном source/context.
Gate продолжает фиксировать неподдерживаемый автоматический governance admission.
Ни один результат этого review не становится task grant, App consent, live execution
permission, разрешением write, выпуска или production readiness.

GitHub PR review полезен для независимого reviewer с подходящими правами, но owner
является автором PR55: **нельзя требовать от него GitHub APPROVED review собственного PR**.
Не создавать фиктивного второго человека и не выдавать App за human reviewer.
Делегирование независимому человеку потребовало бы отдельного решения владельца.

В текущем одноразовом варианте owner acknowledgement — явная человеческая запись
по полному пакету evidence. Допустимо оформить её в Issue56 с проверкой API author
ID240867647 и точных SHA/hash. Это не «любой комментарий достаточен»: произвольный текст,
label, реакция, edited PR body или чужая копия решения не подходят. Запись **не является
машиночитаемым grant и не потребляется gate**; исполнитель не запускает от неё merge.
Отдельное актуальное решение о merge и само ручное действие остаются у владельца.
ID/author metadata подтверждают аккаунт, не физическое присутствие и не отсутствие
компрометации owner/PAT/session. При сомнениях — STOP и восстановление вне агента.

## 4. Варианты допуска

| Вариант | Действия и границы | Плюсы | Ограничения |
| --- | --- | --- | --- |
| V1: одноразовое ручное owner review, затем отдельный owner merge | Gate остаётся failure. Владелец принимает pinned evidence, явно подтверждает риск unprotected main; отдельно разрешает и сам выполняет обычный merge, только если нет required blocker/override | Не меняет код, workflows, права или gate; соответствует сообщению о separate owner review; минимальный scope для offline PR55 | Не даёт технического enforcement; зависит от owner, актуальных settings и проверки SHA; не решает TOCTOU полностью |
| V2: сначала formal governance admission | Отдельное ТЗ/PR/разрешение на независимую verifier boundary, protocol, уникальные checks, scope/hash/expiry/revocation и required rules; тесты до эксплуатации | Проверяемый воспроизводимый допуск будущих governance changes; меньше ручной неоднозначности | Не реализован; изменяет governance архитектуру/политики, требует отдельного bootstrap и owner approvals; не разблокирует PR55 сам по себе |
| V3: сохранить Draft и отложить | Не допускать PR55 до появления согласованного enforcement/независимого review | Без изменений существующей доверенной базы; fail-closed при неполных данных | G2 остаётся не merged; интеграция задерживается |

**Рекомендация: V1 только для этого pinned offline PR55**, если владелец отдельно
утвердит процедуру и подтвердит отсутствие required blocker в effective settings.
Причина: код gate намеренно требует внешнего owner review, но не реализует admission;
строить новый privileged механизм ради одного offline PR расширяет доверенную базу.
V1 сохраняет failure, его evidence и историю; не подменяет check success и не использует
bypass. Недостаток отсутствия enforcement принимается явно, а не скрывается за CI.

Если владелец требует платформенно обязательного admission либо не принимает риск
unprotected main/гонок — выбрать V3 сейчас и спроектировать V2 отдельно.

Для V2 недостаточно добавить ещё один зелёный check: при required старом gate его
безусловный отказ останется блокером. Новая семантика gate/admission и конфигурация
required checks нуждаются в отдельном утверждении/PR и доверенном bootstrap. Нельзя
включать их в PR55, этот docs PR или выдавать permanent exception для всего agent_core/.
Также запрещены trusted code из PR, широкие write scopes, secret-bearing untrusted jobs,
маскировка failure через neutral/skipped, удаление check и отключение protection.

## 5. Пакет evidence для одноразового review

Пакет фиксирует repository ID/name, PR number, base/head/tree, revision/hash этого ТЗ,
manifest digest и полный diff с filename/previous_filename/status/blob SHA. Дополнительно:
- Owner user ID, время UTC, источник/ID записи решения и её исходное содержимое;
  редактирование/удаление записи до merge требует повторного подтверждения.
- Все check/run/job IDs, event, attempt, source/base SHA, app, conclusions и ссылки на logs.
- Expected governance denial с точной причиной, отсутствие других необъяснённых failures.
- Scope audit: v1 hashes/interfaces и файлы core/check/task-v1/workflows/requirements
  неизменны; нет live entrypoint/credentials/network adapter, активных registry/tasks,
  новых permissions, Android/Room/backup/signing/release changes.
- Результаты offline tests 69 v1 +71 v2, ограничения A01–A32, ABA/in-flight/cleanup.
- Settings snapshot + отдельное подтверждение владельца через UI без изменения settings.

Архив/скриншот/отчёт — указатель на evidence, не замена свежей API/UI проверке.
Не включать credentials, personal data, PEM, tokens или содержимое secret settings.

Manifest digest текущих 23 записей:
`b624e7b5703ac1291a5e2ee59480840142d4334b82b64565e251b77b446e3b44`.
Алгоритм: отсортированный по path JSON array объектов {path,status,blob_sha};
SHA256(json.dumps(...,sort_keys=True,separators=(',',':'),ensure_ascii=True).encode()).
Blob SHA — Git blob identity, не SHA256 файла. Manifest не заменяет review содержимого.
Полный manifest приведён в приложении; пересчёт и свежий diff обязательны перед решением.

## 6. Будущая последовательность: отдельные точки разрешения

Все шаги этого раздела — требования к будущей процедуре. Сейчас не выполнены.

1. **R0 — принять ТЗ процедуры.** Владелец утверждает конкретную редакцию/commit этого
   документа и V1 или выбирает V2/V3. Принятие ТЗ не равно допуску PR55 или merge.
2. **Снять свежий snapshot.** Повторно прочитать PR55/main/settings/checks/reviews.
   При любом несовпадении head/base/tree/manifest остановиться и сформировать новый
   evidence package; даже docs-only commit или merge этого docs PR в main делает
   прежнее основание устаревшим. Не переиспользовать старое решение автоматически.
3. **Проверить diff и границы.** Прочитать все 23 файла и соответствие архитектуре;
   подтвердить сохранность v1, изоляцию модулей и отсутствие live/credential paths.
   Не принимать отчёт самого PR вместо чтения кода и сравнения с trusted base.
4. **Проверить CI и gate.** Все применимые ordinary CI на проверяемом source завершены
   success. Trusted failure только ожидаемый governance-path отказ с проверенным base
   source; PR changed/API error/другая причина — STOP, не считать это тем же отказом.
   Skipped trusted job из другого event не удовлетворяет проверке.
5. **Проверить безопасность и settings.** Владелец подтверждает current effective rules,
   отсутствие required blocker и понимание unprotected-main риска. Нельзя проверять
   разрешённость пробным merge или расширять integration permissions ради чтения.
6. **R1 — owner acknowledgement результата review.** Отдельная явная запись связывает
   PR/head/base/tree/manifest/редакцию ТЗ/evidence и принятие перечисленных ограничений.
   Формулировка означает только «review принят для этого snapshot». Она не даёт агенту
   merge/App/task/write permissions. Текущий документ такую запись не создаёт.
7. **R2 — отдельное разрешение на ручной owner merge.** После R1 владелец отдельно
   указывает тот же exact snapshot, допустимый merge method и одноразовое действие.
   Рекомендуемый method — обычный merge commit: сохраняет проверенные commits и явную
   пару родителей, проще post-merge audit. Squash/rebase потребуют согласования другой
   проверки ancestry/tree. Рекомендуемое окно R2 — не более 1 часа, с немедленной
   утратой применимости при любых изменениях. Это процедурное окно, не реализованный TTL.
8. **Последняя проверка непосредственно перед действием.** Владелец ещё раз проверяет
   head/base/main/settings и conclusions, затем сам переводит PR из Draft в Ready и
   выполняет обычный merge только в рамках R2. Если UI требует override — STOP.
   Агент не делает merge и не нажимает Ready по этому ТЗ.
9. **Post-merge verification — отдельный будущий read-only этап.** Получить merge SHA,
   проверить родителей base/head и результирующий tree ожидаемого merge; проверить
   отсутствие посторонних изменений, неизменность workflow/gate и отсутствие live
   integration. Перечитать main после проверки. Запуск дополнительных CI — только
   автоматически по существующим событиям или по отдельному разрешению; не dispatch.
   Исходный PR55 denial сохраняется; merge не превращает его в success.
10. **Зафиксировать результат.** Handoff/evidence содержат точный merge/source SHA,
    выполненные проверки и ограничения. Если после R2 появились изменения — остановиться
    и вернуться к R1/R2 с новым пакетом. Даже успешный merge G2 не открывает G3–G6.

### Поля будущего решения владельца (описание, не готовое approval evidence)

Обязательны: decision ID/UTC; owner User ID; target PR55; точные head/base/tree;
manifest hash; revision/commit ТЗ; evidence URLs/IDs; отдельно review acceptance,
acknowledged expected gate failure, risks/unprotected main, затем отдельный merge
authorization с method/expiry/executor=owner. Отсутствие поля или противоречие — STOP.
Не размещать заполненный «approved» шаблон от имени владельца и не автоматически
считать текст/label подходящим решением. Ответственного owner нельзя заменить App.

## 7. Stale SHA, гонки, ошибки и инциденты

| Событие | Требуемое поведение |
| --- | --- |
| Новый head, включая docs commit | R1/R2 утрачивают применимость; новый diff, checks, review |
| Main/base изменён, даже без конфликта | Новый пакет для новой пары SHA; старый head CI не доказывает merge result |
| Required checks/rules изменились или неполны | STOP; owner подтверждает настройки, никакого bypass/автоматического fallback |
| Failure ordinary CI или иная причина trusted failure | STOP, отдельная диагностика; rerun/исправления только в новом разрешённом scope |
| API403/404/429/timeout, удалённый run/log/review | Нет cached success; собрать доступные evidence, STOP до восстановления проверяемости |
| Owner acknowledgement изменён/отозван/неясен | STOP; новое прямое подтверждение owner с теми же либо новыми pins |
| Подозрение на compromised owner/session/PAT | STOP; независимое восстановление owner; не признавать новые команды сомнительного аккаунта доказанно безопасными |
| Main/head/settings гонка | Повторить read-only snapshot; если нельзя обеспечить приемлемое окно — V3/V2, не заявлять атомарность |
| После merge обнаружен неверный tree/scope | Incident: остановить последующие этапы, сохранить SHA/logs, уведомить владельца отчётом; никаких автоматических откатов |

Повторные чтения не устраняют ABA и изменение base между проверкой и merge.
Даже REST merge parameter sha защищает ожидаемый **head**, не является CAS для base
или settings; API merge в этом этапе не используется. Manual UI также не даёт
криптографической атомарности пары head/base. Остаточный риск должен быть явно принят;
при требовании строгой атомарности одноразовый V1 не подходит.

Rollback/incident response: сохранить diff/parents/source/check evidence; не reset/force-push
main, не удалять историю/данные, не выполнять destructive rollback. Возможный revert —
новый reviewable PR с тестами и отдельным решением владельца, также governance change
под тем же gate. Не предполагать автоматический пропуск revert или emergency override.
G2 не содержит live credentials; это не разрешение отзывать/создавать какие-либо реальные
ключи или менять инфраструктуру вне отдельно разрешённого incident scope.

## 8. Матрица приёмки процедуры

Статусы ниже относятся к **подготовке ТЗ**, а не выполнению admission. P=passed evidence
inspection; B=blocked до отдельного действия/решения; N=not_run будущий этап.

| ID | Проверка/негативный сценарий | Критерий и действие | Сейчас |
| --- | --- | --- | --- |
| GR01 | Source gate и checkout | Trusted base e5e0aeae, workflow/run/job связаны; PR code не исполняется gate | P |
| GR02 | Governance diff | 23 записи и manifest совпадают; code paths должны вызывать отдельный owner review | P |
| GR03 | Expected denial | Именно governance reason; unknown/API/race denial не подменяется ожидаемым | P |
| GR04 | Одноимённый skipped check | Проверен failed target job, skipped PR job не считается admission | P |
| GR05 | Effective protection | Read metadata сняты; полный endpoint403; требуется owner UI confirmation без settings edits | B |
| GR06 | Ordinary CI | Семь runs на f6f6707d success; no rerun этим этапом | P |
| GR07 | V1 и offline-only scope | Source/diff и G2 audit доступны; owner обязан независимо принять код в R1 | B |
| GR08 | Owner identity/self-review | Автор=owner; собственный APPROVED review не предлагается, fake reviewer запрещён | P |
| GR09 | Фальшивый comment/label/worker artifact | Не machine grant; нет автоматического admission, required evidence+R1/R2 | P (проект требования) |
| GR10 | Stale head/base/settings | Любое изменение аннулирует применимость snapshot/R1/R2 | P (проект требования) |
| GR11 | Missing evidence/API failures | Fail-closed STOP, archived/cached success недостаточно | P (проект требования) |
| GR12 | R0 принятие процедуры | Явное решение владельца по редакции и варианту | B |
| GR13 | R1 review acknowledgement | Только после scope/security/settings review; привязка ко всем pins | B |
| GR14 | R2 merge authorization | Отдельно от R0/R1; method, owner action, expiry и fresh snapshot | B |
| GR15 | Required failing gate в UI | Не merge; не менять/обходить protection; отдельное решение по архитектуре | N |
| GR16 | Ready/manual merge | Только owner в отдельно разрешённом будущем этапе; сейчас Draft | N |
| GR17 | Post-merge parents/tree/source | Точное соответствие одобренной паре; no unexpected changes | N |
| GR18 | TOCTOU/ABA/compromised owner | Остаточные риски явно приняты либо STOP/V3; нет ложных гарантий | P (проект требования) |
| GR19 | Rollback | Только новый согласованный PR; no destructive reset/force-push/data deletion | P (проект требования) |
| GR20 | Scope текущего docs этапа | Только этот документ + handoff, PR55 и settings не изменены; нет admission/live | P после итогового diff |

Негативные сценарии сейчас проверены как полнота проектируемой процедуры, не live
экспериментами. Никаких поддельных reviews, dispatch, пробных merge или permissions
для демонстрации сценариев не создавать. Будущие автоматические tests V2 отдельным ТЗ.

## 9. Чек-лист владельца и условия остановки

Перед R1/R2 владелец подтверждает:
- [ ] Утверждена конкретная редакция процедуры и выбран V1; риск unprotected main принят.
- [ ] Head/base/tree/manifest актуальны; все файлы и G2 security ограничения прочитаны.
- [ ] Полные effective settings проверены в UI; нет required blocker или override requirement.
- [ ] Семь ordinary CI success на нужном source; trusted failure — ожидаемый и сохранён.
- [ ] Учтена неоднозначность одноимённых checks и отсутствие собственного APPROVED review.
- [ ] R1 отделён от R2; текущий текст ТЗ, label или G2 разрешение не принимаются за merge consent.
- [ ] До R2 и непосредственно перед действием повторно проверены SHA/settings/evidence.
- [ ] G3–G6, live App/task approvals, credentials и APK не разрешены этим review.
- [ ] Известны TOCTOU/ABA, post-merge verification и недеструктивный incident/revert порядок.

STOP при любом неполном/противоречивом evidence, изменении SHA/политик, неизвестном
failure, невозможном owner approval механизме, подозрении на компрометацию, требовании
обхода required gate, расширении scope либо отсутствии отдельного решения R0/R1/R2.
На текущем этапе R0/R1/R2 **не получены**; итог — только проект ТЗ и остановка для review.

## 10. Что требует нового задания

Любые изменения core/check/schema v1, условий/path filters/contexts gate, новый admission
workflow, status writer, required checks, rulesets/protection, bypass lists, permissions,
CODEOWNERS/review policy или key release требуют отдельного явного согласования scope,
нового PR где применимо, тестов и безопасности bootstrap. GitHub settings не имеют
автоматического PR-enforcement: сначала описать конкретный diff настроек и получить
отдельное разрешение владельца, затем проверить фактическое применение.
Этот документ не предлагает выполнять такие изменения ради текущего V1.

Будущая formal схема V2 должна закреплять owner/repository/PR/head/base/manifest,
approved source policy, scope, nonce/expiry, отзыв и одноразовость, независимо проверять
evidence и не исполнять candidate code с credentials. Bootstrap изменения самого gate
не может быть самосогласован новым кодом из того же PR; требуется отдельный owner review.

## Приложение A. Pinned manifest PR55

Ниже перечислены Git blob identities на head f6f6707d; сверять с актуальным PR API.

| Path | Status | Blob SHA |
| --- | --- | --- |
| agent_core/schemas/policy-v2.json | added | fe3148ad3b9081e292662ac3537e3f100efe59b0 |
| agent_core/schemas/registry-v2.json | added | 78fa142718b076e62154a41d692136abd804e20c |
| agent_core/schemas/task-v2.json | added | 795a6ec1b299e86b40a5e3a37553c0d31de1ccc6 |
| agent_core/v2/__init__.py | added | 7aaff02cf9c74b94687413daab1847cd28c08993 |
| agent_core/v2/approval.py | added | 4ba4139afbc742303789d039784bad071bcbcc0f |
| agent_core/v2/audit.py | added | 2c33428680fae691215e13d13246e8661e2cdc96 |
| agent_core/v2/controller.py | added | fc2890a7415310e4506de65684603a1564aa39b7 |
| agent_core/v2/credentials.py | added | 27ef045b6aae4bd8cfc9920f38aedc1c50f35b01 |
| agent_core/v2/model.py | added | 7767e0eae71cbb01f0687dd104f3aa95125b2182 |
| agent_core/v2/task.py | added | 839dee8c1de1e452196b112fa958546e02826d22 |
| agent_core/v2/trust.py | added | 7c363bff55ecd6bf967935a6b581ec625b2b06aa |
| docs/agent-core-security-audit.md | modified | 2ac6f995d8ee911b24a6d17ceb16dc5b7bd4bb49 |
| docs/agent-identity-v2/acceptance-matrix.md | added | 70af6710d8beb2dc45a98a034dc90f156f61c554 |
| docs/agent-identity-v2/g2-offline-implementation.md | added | 29ad30e9f3aa1743cbcc57e674e83a8e503db48e |
| docs/codex-handoff.md | modified | 6df804324f599883b77c5dd352e16ae76368d716 |
| tests/agent_core/fixtures/v2/policy.json | added | a4a067f048c8738d8f561107b9757af73772badf |
| tests/agent_core/fixtures/v2/registry.json | added | 8c56b6445cb98254bd398398be51a077d021d73e |
| tests/agent_core/fixtures/v2/request-task.json | added | 8d472cacb211561ac8b70a14d6a9f132401ad2c2 |
| tests/agent_core/test_v2_approval.py | added | f9498b7a4b2ad22612c5231d54764faae9dcc873 |
| tests/agent_core/test_v2_compatibility.py | added | f86755e10f0c9ce52b3798ddae44bfd6ef74dd9c |
| tests/agent_core/test_v2_controller.py | added | 32af7c41d17482d73e82e1ee982049382b80d6d7 |
| tests/agent_core/test_v2_schema.py | added | bc54fb490874af8a0c7a25e7d1bdb396a68e6c9c |
| tests/agent_core/v2_fakes.py | added | 159f61404ad5b7f3430f9eb0b57367b8b1afd583 |

## Справочные ссылки GitHub для будущего owner review

- [Protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches).
- [Approving a PR with required reviews](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/approving-a-pull-request-with-required-reviews).
- [REST merge PR: ожидаемый head sha](https://docs.github.com/en/rest/pulls/pulls#merge-a-pull-request).
- [Rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets).

Общая документация не заменяет проверку текущего effective UI/settings репозитория.
