# Agent Identity v2 — матрица приёмки

Статус: **проект будущих проверок, не результаты запуска**. Никакая строка не
разрешает live действие сейчас. ТЗ — [R01–R16](README.md), угрозы — [T01–T20](threat-model.md),
разрешения этапов — [G0–G6](rollout-and-decisions.md).

Уровни: O — offline deterministic fixtures/clock/transport fault injection;
L — отдельно разрешённый live read-only интеграционный тест после provisioning.
N — review конфигурации/изоляции; проверять на отдельном разрешённом стенде, не
ослаблять AgroNaliz или выполнять запрещённую production операцию ради теста.
В live нельзя подделывать runtime/API для получения положительного результата.

| ID | Требования | Сценарий | Уровень | Наблюдаемый результат и evidence |
| --- | --- | --- | --- | --- |
| A01 | R01,R02 | Верные human approver, App/install/repo и trusted run | O,L | Разрешена только read_identity_smoke; GitHub metadata, source SHA и grant digest сохранены |
| A02 | R02 | Совпали числа user/App/install IDs; bot login похож на owner | O | Нет приведения типов/namespace; чужой subject DENIED |
| A03 | R02,R08 | App API возвращает другой app_id либо подставлен client ID | O,L | DENIED до операции; response path/status и reason без token |
| A04 | R02,R08 | Другой installation ID/account, в том числе тот же App | O,L | DENIED; проверена association, не только имя App |
| A05 | R02 | Другой repo ID при том же имени, transfer/rename/reinstall | O,L | DENIED, нужна новая привязка owner; нет silent remapping |
| A06 | R08,R10 | Нет explicit repository_ids/permissions, scope шире или listing неполный | O,N | Mint/использование заблокированы; проверены args, returned permissions/repos и pagination |
| A07 | R04 | Approval от App/бота/постороннего пользователя | O,L | DENIED; API actor/triggering_actor не human owner |
| A08 | R04 | Подмена actor input, automation с user token владельца | O,N | API/actor не принимаются из task; OAuth/PAT automation отсутствует в config/source |
| A09 | R05,R06 | Согласование по comment/label/session_start/зелёному PR check | O,L | Не является grant; state approved без допустимого evidence DENIED |
| A10 | R06 | Иной workflow/event/ref/source/title, attempt2 или incomplete run | O,L | Каждый вариант DENIED с конкретной причиной; нет fallback на v1 title |
| A11 | R05 | Spec/operations изменены, hash пересчитан после approval | O,L | Старый context/evidence не подходит; требуется новый request |
| A12 | R02,R05,R11 | Изменены executor tuple, policy_version/hash или controller source | O | DENIED, даже при прежнем task ID и прежнем run success |
| A13 | R03,R06 | Изменены prefix/position, прежняя revision/history переписана | O | DENIED; append-only проверяется относительно доверенного previous |
| A14 | R06 | Run/nonce повторены для новой задачи/редакции/transition | O,L | DENIED; продолжение разрешённых reads не ошибочно считается replay consent |
| A15 | R11,R12 | Старый approved snapshot после cancelled/blocked/awaiting_approval | O,L | Следующая операция запрещена по свежему main; старые evidence остаются историей |
| A16 | R05,R12 | not_before в будущем, not_after истёк, token refreshed | O | DENIED; clock boundary тесты, token refresh не продлевает consent |
| A17 | R11 | Main SHA сменился между чтениями, даже task не менялся | O | DENIED; следующая попытка выполняет все проверки заново |
| A18 | R07 | Поддельные GITHUB_*, run ID/actor из untrusted output | O,L | Trusted API metadata не совпадают — DENIED; environment не authority |
| A19 | R07,R11 | Fork/PR/unapproved workflow code, cached policy или старая source version | O,N | Нет key release/исполнения; evidence закрепляет проверенные source/policy hashes |
| A20 | R09 | Untrusted script/checkout/import/cache пытается прочитать credentials | O,N | У untrusted job нет key/token, общего writable workspace и credential outputs |
| A21 | R07,R09 | PR меняет action/dependency, script/path/URL input инъецирует код | O,N | Только pinned trusted source и фиксированный operation handler; DENIED до secrets |
| A22 | R09,R14 | Исключение, отмена job, debug logging/cleanup | O,N | Без утечки canary secrets в logs/artifacts/cache; revoke/failure audit; raw token не хранится |
| A23 | R02,R08,R13 | App permissions выросли, installation переустановлена или repo scope изменился | O,L | DENIED до новой owner-привязки/policy/consent, без использования старого grant |
| A24 | R08,R13 | Installation suspended/uninstalled, token revoked | O,L | Следующий read DENIED; API refusal не маскируется cached success |
| A25 | R09,R10 | Token expires до операции; consent истекает во время проверки | O,L | DENIED, операция не начата; revoke outcome и remaining expiry в audit |
| A26 | R08,R11 | Timeout,401/403/404/429/5xx, bad JSON, duplicate keys, pagination truncation на каждом read | O | Ни одна injected error не приводит к allowed; безопасная причина/нет headers в logs |
| A27 | R03 | Исторические v1 tasks/grants и попытка исполнить их как App | O | V1 bytes/hashes/evidence сохранены; App authorization DENIED без нового v2 consent |
| A28 | R10 | Scope test используется как произвольный command, implement или draft_pr | O,N | Только read_identity_smoke; остальные операции DENIED |
| A29 | R10,R16 | Protected ops или mutation endpoints с read-only credentials | O,L,N | Local gate deny; при отдельно разрешённом тесте изолированного стенда API403; никаких production mutation попыток |
| A30 | R07,R09 | Protected environment недоступен; self-review policy требует второго человека | N | STOP, нет fallback на repo-wide secret/фиктивного reviewer; выбран D02 owner вариант |
| A31 | R14,R15 | Удалён evidence run, audit недоступен, неизвестная protocol version | O | DENIED; архив worker не восстанавливает grant, version unknown fail closed |
| A32 | R11,R13,R16 | In-flight запрос, ABA main и запрос write на незащищённый repo | O,N | Не заявлять атомарность/мгновенный stop; write этап BLOCKED, права не выданы |

## Разделение offline и live свидетельств

O-проверки будущей реализации должны работать без GitHub secrets и пользовательских
данных: полный fake transport, управляемое время, fault на каждом чтении, строгая
проверка ожидаемого reason/outcome и количества выполненных операций (0 для отказа).
Подмена transport в unit test не становится поддерживаемым публичным callback API.

L-проверки подтверждают реальные identity/scope/run/source связи и поведение API.
Положительный случай не доказывается одной успешной unit suite. Нужны реальная App
installation и конкретное owner approval на синтетическую v2-задачу. Live отрицательные
cases выполнять только в разрешённом наборе; не делать серию dispatch автоматически.
Для expiry допустимо дождаться expiry; для revoke/suspension нужно отдельное owner
разрешение. Чужие Apps/accounts не создавать и новых прав ради тестов не выдавать.

Read-only integration включает credential issuance/revocation и создание run evidence:
это не «все HTTP-запросы GET», но не допускает repository mutations от App.
Metadata tasks/requests могут попасть в main только через отдельные owner-approved
PR/merge. В текущем docs PR никаких таких state changes нет.

## Evidence для каждой проверки

Сохранять test ID, точный source/main/policy SHA, ожидаемый и фактический outcome,
UTC, task/revision/context/executor hashes, request position/expiry, run/attempt,
workflow/event/actor IDs, App/install/repo IDs, ссылки на PR/check/log и reason code.
Секреты заменять безопасными test-canaries только в изолированных tests; реальные
значения не публиковать. Evidence из artifact не authority: verifier перепроверяет API.

## Критерии готовности этапов

- Docs этап сейчас: четыре согласованных документа, покрытие всех пунктов Issue #52,
  рекомендации D01–D12, проверенные Markdown links/diff/scope, отдельный Draft PR и handoff.
- Offline implementation: A01–A32 для O/N-частей с проверенными изоляционными
  предпосылками, v1 regression и source audit; пока без утверждения live identity.
- Live read-only: положительный A01 и согласованный subset L с реальными evidence;
  остальное явно not_run/blocked. Не объявлять полную матрицу пройденной по subset.
- Write: не часть этой приёмки; даже 100% read-only tests не разрешают расширение прав.

Текущие результаты подготовки документации записываются только в handoff/PR;
строки этой матрицы остаются будущими требованиями и не отмечаются pass заранее.
