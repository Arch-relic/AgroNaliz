# G2 Offline Implementation — Issue #54

21.10.2026. Статус: offline-реализация для review; эксплуатационное включение запрещено.

Основания:
- [G1: D01–D12](https://github.com/Arch-relic/AgroNaliz/pull/53#issuecomment-6315563748).
- [Review плана](https://github.com/Arch-relic/AgroNaliz/issues/54#issuecomment-6315705884).
- [Явное разрешение G2](https://github.com/Arch-relic/AgroNaliz/issues/54#issuecomment-6315717967).
- Архитектура PR53 зафиксирована на `96d8535ec544fcd13e93335e3d0f096c966da0f8`:
  [R01–R16](https://github.com/Arch-relic/AgroNaliz/blob/96d8535ec544fcd13e93335e3d0f096c966da0f8/docs/agent-identity-v2/README.md),
  [threat model](https://github.com/Arch-relic/AgroNaliz/blob/96d8535ec544fcd13e93335e3d0f096c966da0f8/docs/agent-identity-v2/threat-model.md).

Main/base: `e5e0aeae9474307562a90ca4d3d075dc11800841`.
PR53 и PR51 не объединяются и не переносятся целиком в эту ветку. PR51 остаётся
отдельным bootstrap-планом v1; его synthetic executor не является App identity.

## Реализованные границы

Новые schemas: `agent_core/schemas/{task,registry,policy}-v2.json`.
Модули `agent_core/v2/` не импортируются существующими CLI/workflows.
Нет `__main__`, dispatch mode, production factory, HTTP/JWT/PEM adapter,
чтения environment/secrets, выдачи реального token или произвольного shell handler.
`CredentialBoundary` по умолчанию отказывает с `LIVE_CREDENTIALS_DISABLED`.
Установка App, environment и активные registry/policy/tasks не создаются.

| Модуль | Ответственность |
| --- | --- |
| model.py | JSON без duplicate keys/NaN/float, strict version, schema, UTC, безопасные reason codes |
| task.py | State machine v2, immutable identity, append-only revisions/history/decisions/requests |
| approval.py | Owner evidence, immutable request, context/title, history position, scope и время |
| trust.py | Типизированные offline anchors, registry/policy digests, run/source/installation bindings |
| credentials.py | Неактивный credential contract и opaque synthetic Lease без token string |
| controller.py | OfflineController.simulate, повторные проверки, фиксированный smoke, cleanup |
| audit.py | Allowlisted Outcome с offline=True, без raw transport errors и credentials |

Это исполняемая модель протокола в тестовом процессе, **не доверенная среда для
произвольного Python-кода**. Переданные fakes/anchors могут лгать. Никакой результат
`simulate()` не является live grant и не должен передаваться действующему исполнителю.

## Форматы и согласования

Schema v2 сохраняет названия состояний и EDGES v1. `executor_id` запрещён;
`executor.kind=github_app_installation` связывает app/install/account/repository IDs
и policy_version. Типизированный executor неизменяем; изменение требует новой задачи.
`predecessor_task_id` — только историческая ссылка, не перенос разрешения.

SHA256 canonicalization использует неизменённый `core.digest`: sorted keys,
compact separators, ensure_ascii, UTF-8. `context_hash` хранится вне хешируемого
context; SHA содержащего request коммита берётся из независимого run evidence.

Request содержит protocol_version=2, task/revision/spec/operations, полный executor,
repository/policy version/hash, history_position, nonce >=128 бит, not_before/not_after.
Все request timestamps строго `YYYY-MM-DDTHH:MM:SSZ`; duration >0 и <=8 часов.
Request добавляется в awaiting_approval на позиции len(history). Для позиции активен
последний request; предыдущие сохранены. При сравнении с previous новые requests
допустимы только в awaiting_approval, до отдельного approved transition.

Approved event содержит run_id/nonce/context_hash. Run должен быть owner User в обеих
actor ролях, dispatch/main/attempt1/completed/success и соответствовать pinned workflow,
source и `approve-v2:<task>:<context_hash>`. Immutable task из run.head_sha сверяется
с префиксами history/revisions/requests. Approval run создан и завершён в consent window,
а approved transition датирован не раньше его завершения. Старые nonce/run нельзя
использовать для нового перехода. Один grant допускает несколько проверенных reads.

В G2 только scope `['test']` и фиксированный `read_identity_smoke` допускаются verifier
и контроллером. Остальные операции, включая protected/write, отклоняются, даже если
под них пересчитаны все hashes. Schema сохраняет названия операций для описания задач,
но перечисление операции в документе не выдаёт полномочия.

## Offline transport и clock

`transport.read(resource)` возвращает независимую копию нормализованных synthetic
данных; отсутствующие/неполные/противоречивые данные и исключения ведут к отказу:

| Resource | Контракт |
| --- | --- |
| main | Полный lowercase commit SHA40 |
| task/ID@SHA | JSON задачи строго на указанном immutable SHA |
| registry@SHA, policy@SHA | JSON, проверенный по schemas и заранее закреплённым hashes |
| run/ID | Нормализованные metadata run, repository, actors, source evidence и timestamps |
| installation | App/install/account, active/suspended, exact selected repo/permissions, complete |
| server_time | Независимое UTC время для проверки skew |

`source_sha/source_hash` и `complete` — **контракт нормализованного evidence**, а не
утверждение, что такие готовые поля возвращает GitHub Runs API. В будущем независимый
доверенный adapter обязан сам получить source blobs/metadata, проверить hashing,
полноту pagination, provenance, immutable refs и server Date. Он не реализован в G2.
Нельзя подставить PR JSON с `complete:true` и считать identity доказанной.

`clock.now()` — UTC; `clock.monotonic()` — длительность сессии. Clock skew >60 секунд,
истечение consent/token и длительность >=10 минут запрещают действие. Интервал
consent/token полуоткрытый: равенство not_after/expires_at уже отказ. Проверяется также
возраст controller run. Refresh token не продлевает consent; refresh в модели отсутствует.

Перед действием читаются свежие main/task/registry/policy, проверяется execution run,
вся цепочка approval и scope, installation и downscoped lease. Затем installation,
main и время проверяются повторно. Смена main приводит к отказу без cached fallback.
Каждый simulate выполняет проверки заново, даже после предыдущего успешного чтения.

`read_identity_smoke(lease, repository_id)` — единственный фиксированный transport
handler, без пользовательской команды/URL/path/callback. Fake возвращает только
repository_id и read_only marker; это не проверка реальных API credentials.

## Credential boundary и audit

Mock acquire получает explicit installation_id, единственный repository_id и exact
permissions: metadata/contents/actions/pull_requests=read. Проверяются returned lease
scope, installation и expires_at. Lease не содержит реального token; handle не выводится
в repr и не попадает в Outcome. Ключей, issuer, token parser или secret source нет.

После получения lease выполняется revoke в finally, в том числе при исключении и
KeyboardInterrupt в handler. Ошибка cleanup даёт blocked/CLEANUP_FAILED, failed cleanup
и безопасный expiry. Если smoke уже выполнен, ошибка cleanup не отменяет сделанное:
в тесте результат blocked с одной выполненной read-операцией. Pre-action denial имеет
ноль целевых операций. Acquire, который не вернул lease, нельзя считать подтверждённо
отозванным. Принудительный kill runner не гарантирует finally.

Outcome содержит только outcome/reason/UTC/main SHA/task ID/context hash/token expiry/
cleanup/offline. Это минимальный G2 диагностический результат; полного live evidence
и durable audit store нет. Details источников доступны в synthetic fixtures/test traces;
будущий live audit расширяется по R14 отдельно, не через raw API dumps.

## Совместимость

- core.py, check.py, task-v1.json, существующие tests/workflows/dependencies не изменены.
- app_task строго требует integer version=2; неизвестная/отсутствующая версия запрещена.
- V1 сохраняет свой validator, User executor, документы, hashes и evidence.
- App controller отвергает v1 до чтений transport/acquire; fallback/migration grants нет.
- V1 validator отвергает v2; смешанные документы со старым executor_id отвергаются v2.
- Никаких миграций Room, backup или действующих задач; fixtures только в tests/.

## Проверки и воспроизведение

```bash
python3 -m unittest discover -s tests/agent_core -v
python3 -m agent_core.check local
python3 -m compileall -q agent_core/v2 tests/agent_core/test_v2_*.py
git diff --check
```

Также вся suite выполнена с запрещённым socket и очищенным environment:

```bash
python3 - <<'PY'
import os, socket, unittest
from unittest.mock import patch
suite = unittest.defaultTestLoader.discover('tests/agent_core')
with patch.object(socket, 'socket', side_effect=AssertionError('OFFLINE_NETWORK_FORBIDDEN')), patch.dict(os.environ, {}, clear=True):
    result = unittest.TextTestRunner(verbosity=2).run(suite)
raise SystemExit(not result.wasSuccessful())
PY
```

Результаты, точный проверенный SHA и CI — в handoff/security audit.
Матрица [A01–A32](acceptance-matrix.md) различает O/N/L; live строки не объявляются passed.
`check local` проверяет действующие v1 directories; при отсутствии задач успех не
доказывает наличие consent и не проверяет live v2 регистрацию.

## Остаточные риски и остановки

- Два main чтения не обеспечивают атомарность: ABA между наблюдениями не обнаруживается.
  Это отдельно воспроизведено тестом. In-flight операция не отменяется задним числом.
- Нет реальной App identity, выдачи/revoke credentials, platform/environment isolation
  и защиты main от admin/write actor. Политики репозитория не менялись.
- Protected environment/self-review и key-release source binding требуют проверки G3.
  Нельзя молча перейти к repo-wide secret или фиктивному второму reviewer.
- G4 требует отдельных workflow integration, реальной синтетической задачи и owner
  consent; G5 — конкретных live runs. G6/write не разрешены.
- Trusted PR gate намеренно запрещает изменение agent_core/. При его отказе зафиксировать
  blocked и остановиться: не менять gate, не переименовывать пути, не создавать bypass
  workflow и не перезапускать его ради получения разрешения.
- Merge только по отдельному решению владельца; Draft PR не является merge authorization.
