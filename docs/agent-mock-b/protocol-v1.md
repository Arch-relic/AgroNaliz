# Протокол mock B: утверждённая реализация

Исходный план v1.4 Final, SHA-256 файла:
`223d45ceb4c01b086d564202154fab908aa49d2a3a7180c2648e08934ea5e409`.
Ниже воспроизведён его текст для review; удалены только конечные пробелы строк. Предварительные статусы
исходного текста заменены отдельным утверждением владельца от 22.10.2026,
переданным в текущем задании. Владелец отдельно разрешил реализацию, feature-ветку,
тесты и Draft PR, а также только отчётное изменение docs/codex-handoff.md.
Запреты исходного текста на реальные ветки/PR относятся к mock runtime, не отменяют
прямое разрешение владельца на разработку и подготовку этого Draft PR.

После исходного текста расположена утверждённая в этой сессии нормативная поправка
к §13.2. Она имеет приоритет для payload RESULT; остальные контракты сохраняются.
Исходное вложение и AGENTS.md не изменялись.

---

# Агронализ — этап 3 / B: нормативный план mock-реализации

**Версия протокола:** `agronaliz.mock-b/1` · **Редакция документа:** 1.4 · **Дата:** 22.10.2026
**Статус:** проект для технического review, **не утверждён для реализации**.
**Основание:** утверждённое ТЗ mock-first v1.1; два раунда review планов B v1.0–v1.2.
**Контекстный SHA main:** `e13fd1e19f531824e148fff1d17d481098929e43` (не подтверждён повторно; не является разрешением на работу).

## 1. Непреодолимые границы

Только локальная детерминированная модель: controller → synthetic approval store → fake worker → independent validator → simulated publisher → evidence/checkpoint. Константы `workerKind="mock"`, `publishAllowed=false`, `externalCalls=0`. Никакого настоящего Codex, GitHub API, сети, токенов, секретов, PR, веток, изменения main, workflows, правил защиты, APK или Room. Текущие Agent Identity v1/v2 и их hashes/grants остаются неизменными. Все состояния и гарантии ниже — гарантии *модели*, не доказательство OS-level sandbox.

## 2. Протокол, сериализация, хеши

**Нормативная реализация JCS:** RFC 8785; для будущего Python-модуля закрепить `rfc8785==0.1.4` (версию и доступность пакета проверить read-only до реализации; при несовместимости — остановка и новая редакция, не подмена `json.dumps(sort_keys=True)`). JSON разбирается с отклонением повторных ключей, NaN/Infinity, суррогатов Unicode, неизвестных полей, bool вместо integer, чисел вне безопасного целого диапазона `[0,9007199254740991]`. Схемы не содержат дробных чисел. Временные метки — календарно валидные UTC `YYYY-MM-DDTHH:mm:ss.SSSZ`, без leap second; парсить строго, сравнивать UTC. Тесты JCS включают UTF-16 порядок ключей, escaping, Unicode, negative zero/float из RFC как отдельные библиотечные векторы, а доменные схемы запрещают float. До кодирования тест-векторы RFC сверяются с установленной библиотекой; несовпадение блокирует реализацию.

Входные set-arrays (`allowedPaths`, `protectedPaths`, `allowedOperations`, `scope`) отвергают дубликаты **до** сортировки. Затем сортируются по Unicode scalar/code point для схемных значений (все пути ограничены ASCII, см. §5); JCS сортирует ключи по UTF-16. `acceptanceCriteria` и журнал событий сохраняют исходный порядок. Поле `taskDigest` **не входит** в specification.

`digest(domain, object) = lowercase_hex(SHA256(UTF8(domain) || JCS(object)))`; domain — буквальные байты с NUL:

- `agronaliz.mock-b/1/spec\0`
- `agronaliz.mock-b/1/snapshot\0`
- `agronaliz.mock-b/1/evidence\0`
- `agronaliz.mock-b/1/genesis\0`
- `agronaliz.mock-b/1/operation\0`

`genesisHash=digest(genesis,{"logId":...,"protocolVersion":"agronaliz.mock-b/1","taskId":...})`; каждый `logId` уникален, номера `sequenceNo` начинаются с 1. Evidence hash вычисляется от полного event **без** `eventHash`, с `previousEventHash` = genesisHash для первого события. Snapshot hash — от manifest (без bytes, но bytes проверяются независимо). Ни один старый digest/grant v1/v2 не пересчитывается.

## 3. Контракты входных данных

В JSON все поля обязательны, если не отмечено `?`; `additionalProperties=false` рекурсивно.

**Specification**: `{protocolVersion, repository, taskId, revision, baseSha, snapshotDigest, allowedPaths, protectedPaths, allowedOperations, acceptanceCriteria, limits, workerKind, publishAllowed}`. `repository`: строго `Arch-relic/AgroNaliz` для текущего scope (ASCII, case-sensitive, сверка с trusted expectedRepository); `taskId`: UUID; `revision`: positive integer; `baseSha`: 40 lowercase hex; digest: 64 lowercase hex; `workerKind="mock"`, `publishAllowed=false`; `allowedOperations` subset of `["ADD","MODIFY","DELETE"]`; `acceptanceCriteria`: непустой массив уникальных строк 1–256 символов. `protectedPaths` включает неизменяемый системный denylist (см. §5), и пользовательский allowlist не может его ослабить.

**Limits**: `{maxRunsPerTask,maxOperationsPerRun,maxDiffFiles,maxDiffLines,maxOutputBytes,maxWallDurationMs,maxTotalOutputBytesPerTask}`. Каждый параметр — положительное целое (bool запрещён), не более `9007199254740991`. Область: maxRuns и maxTotalOutputBytes — на taskDigest во всех approval и попытках; остальные — на run. Сравнение `actual <= max`; при превышении — отказ до нового действия. Длительность: monotonic elapsed milliseconds от подтверждённого начала run до проверки каждого нового действия; время approval — UTC wall-clock. Повторное approval не обнуляет task budget.

**ApprovalRecord** (только synthetic trusted store): `{approvalId,taskId,revision,taskDigest,ownerId,issuedAt,expiresAt,scope,status,storeVersion}`. `status=ACTIVE|REVOKED|DENIED`; `issuedAt < expiresAt`; разрешение действительно только при `issuedAt <= now < expiresAt`; `ownerId` равен закреплённому trusted expectedOwnerId, не полученному из envelope/store. Входной `ApprovalEnvelope` содержит `{approvalId,taskDigest,ownerId}` и никогда не служит доказательством: контроллер запрашивает store и сравнивает *все* относящиеся поля. Отзыв — отдельное append-only trusted событие, не изменение specification; effective status определяется последним подтверждённым событием. Модель угроз: forged request обнаруживается; компрометация доверенного store вне mock-гарантий.

**RepositoryState** (отдельный synthetic store): `{repository,baseSha,snapshotDigest,storeVersion}` плюс immutable bytes snapshot по `snapshotDigest`; значения недоступны на запись worker. Проверять `repository == specification.repository == trusted expectedRepository` и актуальный baseSha до запуска, после получения результата, перед симулированной публикацией. Ни одно значение от worker не является источником актуального SHA.

**OperationReceipt**: `{taskId,runId,operationId,inputDigest,phase,outcome,evidenceSequenceNo?}`. `operationId` UUID, digest — domain-separated hash аргументов. Повтор того же ключа с тем же digest возвращает **только исторический receipt** (read-only), даже после отзыва; не выполняет действие и не выдаёт новое authorization. Другой digest с тем же ключом — `IDEMPOTENCY_CONFLICT`.

## 4. Два автомата состояний и защитные действия

**Task** относится к паре `(taskDigest,approvalId)`, не к произвольно возобновляемому taskId. Состояния: `PROPOSED → APPROVED → ACTIVE → REVIEWABLE → CLOSED`; из PROPOSED возможен `DENIED`; из APPROVED/ACTIVE/REVIEWABLE — `REVOKED` или `EXPIRED`. `DENIED,CLOSED,REVOKED,EXPIRED` терминальны для **этой пары**. Новый approval создаёт отдельный lifecycle, но общий budget по taskDigest сохраняется. `REVIEWABLE` означает историческую готовность результата, а не разрешение новых операций.

**Run**: `CREATED → AUTHORIZED → INTENT_RECORDED → RUNNING → RESULT_RECEIVED → VALIDATING → READY_FOR_OWNER_REVIEW`. `INTENT_RECORDED` — абстрактное состояние перед запуском worker; последующие операции validation/publish имеют собственные INTENT/RESULT evidence, не меняя Run обратно. Из любого нетерминального состояния допускаются защитные `FAILED`, `CANCELLED`, `CANCELLATION_PENDING` (только запрос отмены без подтверждения), `RECOVERY_REQUIRED` (неизвестный результат операции). `READY_FOR_OWNER_REVIEW,FAILED,CANCELLED` терминальны. `RECOVERY_REQUIRED` и `CANCELLATION_PENDING` — блокирующие состояния: автоматического возобновления нет; после проверки evidence/receipt допустим только защитный переход в FAILED/CANCELLED либо возврат в сохранённое предыдущее состояние **по отдельной recovery-процедуре** с проверкой approval, а не обычным переходом FSM.

**Рабочие переходы** (APPROVED, ACTIVE, AUTHORIZED, RUNNING, RESULT_RECEIVED, VALIDATING, REVIEWABLE, READY_FOR_OWNER_REVIEW) требуют свежего approval, актуального base, бюджета, CAS и подтверждённого durable evidence. **Защитные переходы** (DENIED, REVOKED, EXPIRED, FAILED, CANCELLATION_PENDING, RECOVERY_REQUIRED, CANCELLED после подтверждённой остановки) **не требуют действующего approval**; при недоступном evidence store фиксируется только in-memory `STOP_REQUESTED_UNVERIFIED`/ошибка, а не ложное durable событие. Новые рабочие действия запрещены. Best-effort cancel можно вызвать даже при недоступном checkpoint. `CANCELLED` нельзя заявлять до достоверного подтверждения остановки.

Ошибки schema до создания run → task отказ/ошибка в контроллере, `workerStarts=0`; не создавать фиктивный FAILED run. Отзыв после результата, до финального checkpoint блокирует рабочий переход, сохраняет историю; после READY_FOR_OWNER_REVIEW не даёт новых прав.

## 5. Immutable snapshot, пути, diff

Snapshot — map path → immutable bytes и manifest, отсортированный по path: `{path,byteLength,sha256,mode,contentType}`. Только regular-file, mode **`100644`**, contentType **`text/plain; charset=utf-8`**; UTF-8 строгий (без BOM), NUL запрещён; newline bytes не нормализуются. Manifest должен совпадать с фактическими bytes при получении, до worker, после worker и до validation. Мутация bytes при неизменном manifest → `SNAPSHOT_TAMPERED`.

Пути — ASCII `[A-Za-z0-9._/-]`, относительные POSIX, не пустые, без пустых сегментов, `.`/`..`, начального `/`, конечного `/`, `\\`, drive-prefix, symlink, submodule, device, case-fold collision (включая неизменённые файлы). Поскольку ASCII, Unicode normalization collisions невозможны внутри допустимых путей; Unicode пути отвергаются. `allowedPaths` — `file:<path>` или `dir:<path>/` с точной границей сегмента; никакого glob. Protected denylist независимо от spec: `.git/`, `.github/`, `app/`, `gradle/`, `gradlew`, `gradlew.bat`, `settings.gradle`, `settings.gradle.kts`, `build.gradle`, `build.gradle.kts`, `local.properties`, `docs/codex-handoff.md` (этот файл разрешается менять **только отдельным явным исключением владельца в будущем scope**, а не через worker diff). Кроме того, защищены все существующие файлы Agent Identity v1/v2, ключи, signing, policy, даже если не перечислены явно. Путь в allowlist не снимает denylist.

**DiffOp** — строго `{op,path,oldSha256,newSha256,newContentBase64,newMode}`; все шесть полей присутствуют, неиспользуемые = `null`. ADD: old=null, new sha/base64 обязательны, newMode=`100644`, файл отсутствует. MODIFY: old sha соответствует исходным bytes, new sha/base64 обязательны, newMode=`100644`, файл существует и mode не меняется. DELETE: old sha обязателен, new sha/base64/newMode=null. Никаких rename, binary, patch, chmod, symlink. Не более одной операции на path, включая одинаковые дубликаты. Декодирование Base64 строгое (RFC 4648 canonical alphabet/padding; re-encode == input); предварительно проверять encoded length против remaining bytes budget, затем decoded length и hash. Validator независимо применяет diff к копии bytes, проверяет итоговый snapshot целиком, не доверяет worker report.

`maxDiffFiles` — число операций. `maxDiffLines` — сумма строк вставки и удаления из редактирующих opcodes (не гарантированно минимальных), полученных `difflib.SequenceMatcher(None, oldLines, newLines, autojunk=False).get_opcodes()` по массивам строк `splitlines(keepends=True)` после **только для подсчёта** замены CRLF и CR на LF. Для ADD сравнить с пустым массивом; DELETE — с пустым массивом; `replace` считает удалённые + добавленные строки; `equal` не считает. Содержимое/хеши не нормализуются. Примеры: `"a\n"→"a\nb\n"` = 1; `"a\n"→"b\n"` = 2; `"a\r\n"→"a\n"` = 0 по метрике (но sha различны).

`maxOutputBytes` — верхняя граница сырых UTF-8 bytes **полного worker response JSON**, включая Base64, report, metadata; фактический поток ограничивается более строгим `reservedOutputBytes` текущего run **до буферизации**; `maxTotalOutputBytesPerTask` суммирует зарезервированные output bytes всех runs, в том числе неуспешных, без возврата бюджета. Только первый подтверждённый `INTENT` с `operationKind=WORKER` нового run резервирует слот и `reservedOutputBytes = min(maxOutputBytes, remainingTaskOutputBudget)`; ноль означает `BUDGET_EXHAUSTED` до запуска; обязательно `actualOutputBytes <= reservedOutputBytes <= maxOutputBytes`; превышение резерва означает `LIMIT_EXCEEDED`, без validator/publisher, а резерв остаётся израсходованным; подтверждённый резерв никогда не возвращается и не уменьшается, в том числе при crash; fail closed при невозможности измерения. Последующие `INTENT` с `operationKind=VALIDATOR|SIMULATED_PUBLISH` имеют `reservedRunSlot=0` и `reservedOutputBytes=0`; они не проверяют свободный output budget повторно. Нулевой остаток бюджета не блокирует продолжение уже обеспеченного резервом run. Recovery того же `operationId` использует старый резерв и не списывает его повторно. `maxOperationsPerRun` включает каждое подтверждённое INTENT действия worker/validator/simulated publisher, но не read-only receipt retrieval и защитный cancel.

## 6. Evidence как источник истины, checkpoint, recovery

Авторитетное состояние модели и расход бюджетов — **только подтверждённый префикс evidence**, связанный с checkpoint; task/run state, receipts, counters — проекции, восстанавливаемые детерминированным replay. Synthetic approval store и repository store — независимые авторитетные источники *разрешения* и *base/bytes*, не заменяют evidence для фактов действий. Контроллер сериализует операции через per-task lock + CAS `(logId,sequenceNo,rootHash,storeVersion)`; подтверждение INTENT резервирует slot/bytes budget **до** worker. CAS loser ничего не расходует и не запускает. Для B допускается один writer per log; мультипроцессная устойчивость не заявляется.

Evidence event schema: `{protocolVersion,logId,eventId,sequenceNo,taskId,runId,operationId,eventType,actor,timestamp,inputDigest,payload,outcome,previousEventHash,eventHash}`. `payload` типизирован по eventType согласно §13; replay не использует исходные аргументы контроллера. eventType enum: `APPROVAL_CHECKED,APPROVAL_DENIED,REVOKED,EXPIRED,INTENT,RESULT,VALIDATION_REJECTED,STOP_REQUESTED,CANCEL_CONFIRMED,RECOVERY_REQUIRED,SIMULATED_PUBLISH_REQUESTED,SIMULATED_PUBLISHED`; последние два **обязательно** имеют в `payload` поля `simulated=true,externalEffects=0`; `outcome` остаётся enum `OK|REJECTED|UNKNOWN` согласно §13.2. `PUBLISH_REQUESTED` и `PUBLISHED` запрещены даже с флагом simulation. `eventHash` исключён из хешируемого объекта. Чужой logId, stale checkpoint, gap, reordering, rehashed chain и rollback → fail closed. Checkpoint: `{logId,sequenceNo,rootHash,storeVersion}`, отдельно закреплённый независимым synthetic adapter, с monotonic conditional update; невозможность чтения → `INTEGRITY_UNVERIFIED`, несовпадение → `INTEGRITY_FAILED`. Без независимой защиты store это только проверка модели, не реальная tamper-proof гарантия.

Для каждого действия: fresh authorization → reserve via `INTENT` append+fsync → checkpoint CAS+durable → действие с operationId → `RESULT` append+fsync → checkpoint CAS+durable → receipt. Между fsync и checkpoint возможен **неподтверждённый хвост**: его нельзя молча принять или отбросить; recovery сравнивает prefix, checkpoint, operation receipt и требует отдельного разрешённого reconciliation. Действие после INTENT без checkpoint запрещено.

| Crash point | Действие / восстановление |
|---|---|
| До INTENT | Нет резерва, нет side effect; новый запрос возможен после fresh checks |
| После append INTENT, до checkpoint | Не выполнять; tail quarantined, `RECOVERY_REQUIRED` до сверки |
| После checkpoint INTENT, до action | Резерв занят; можно исполнить **тот же** operationId после fresh checks |
| После action, до RESULT | Нельзя повторить вслепую; сверить независимый mock receipt, иначе `RECOVERY_REQUIRED` |
| После RESULT append, до checkpoint | RESULT не подтверждён; сверка хвоста и receipt, без повторного действия |
| После RESULT checkpoint | Вернуть historical receipt; не запускать повторно |
| Конкурирующий CAS | Проигравший не запускает worker, резерв не расходуется |
| Недоступен checkpoint/evidence | Блокировать новые действия; best-effort cancel, без ложного `CANCELLED` |

Неизвестный хвост не разрешает дальнейшие действия. При восстановлении из подтверждённого префикса проекции состояния/бюджета/receipts должны совпадать с ожидаемыми; если нет — остановка. Фиксация checkpoint не является доказательством атомарности внешнего действия. Mock worker ведёт отдельный synthetic operation receipt ledger; потеря обоих доказательств → неопределённость, а не повтор.

## 7. Mock publisher и запрет внешних эффектов

Publisher — чистая функция модели, без GitHub client, network imports, tokens. Validation receipt принимается только по `(logId,sequenceNo,eventHash)` из подтверждённого evidence; объект от worker/вызывающего кода не считается подтверждением. Обязателен подтверждённый `RESULT` с `payload.operationKind=VALIDATOR`, `actor="validator"`, `validationReceipt.validationStatus="PASSED"` и совпадающими `workerResponseDigest`, `diffDigest`, `resultSnapshotDigest`, идентичностями задачи, repository и run. Проверяется принадлежность run действующему approval lifecycle; статус `REJECTED` запрещает симуляцию даже для подлинного receipt. Разные стадии имеют разные `operationId`, их нельзя требовать равными. Прямой вызов обязан проверить `publishAllowed=false`, актуальный approval/base, validation receipt и integrity checkpoint; `publishAllowed=true` отвергается **до worker**. При успешной симуляции записываются только `SIMULATED_PUBLISH_REQUESTED` и `SIMULATED_PUBLISHED` с `simulated=true,externalEffects=0`. Это не создание ветки, PR или публикация. Отзыв между validation и симуляцией блокирует симуляцию. `READY_FOR_OWNER_REVIEW` не даёт новых прав.

## 8. Ошибки и обязательные отрицательные результаты

Коды: `INVALID_SCHEMA`, `INVALID_CANONICAL_JSON`, `DUPLICATE_SET_VALUE`, `INVALID_APPROVAL`, `APPROVAL_EXPIRED`, `APPROVAL_REVOKED`, `STALE_BASE`, `STORE_UNAVAILABLE`, `INTEGRITY_UNVERIFIED`, `INTEGRITY_FAILED`, `SNAPSHOT_TAMPERED`, `INVALID_PATH`, `PROTECTED_PATH`, `INVALID_DIFF`, `INVALID_UTF8`, `HASH_MISMATCH`, `LIMIT_EXCEEDED`, `BUDGET_EXHAUSTED`, `IDEMPOTENCY_CONFLICT`, `RECOVERY_REQUIRED`, `CANCELLATION_UNCONFIRMED`, `REAL_PUBLISH_FORBIDDEN`. Любая неизвестная версия/enum/поле — `INVALID_SCHEMA`. Для каждого отказа: workerStarts=0, если отказ до запуска; publisherCalls=0, если до стадии publisher; externalCalls всегда 0. При невозможности durable evidence не заявлять зафиксированный отказ — возвращать недоверенный локальный error и останавливать работу.

## 9. Матрица приёмки: 15 групп (подслучаи обязательны)

Все fixtures указывают `spec,approvalStore,repositoryStore,snapshotBytes,workerResponse,clock,failpoint,expectedTaskState,expectedRunState,expectedEvents,workerStarts,publisherCalls,externalCalls,confirmedBudget,checkpointRoot`.

| № | Проверки / ожидаемое |
|---|---|
| 1 | Нет approval; отказ без разрешения на защитный переход; worker=0, publisher=0 |
| 2 | Подмена repository/taskId/revision/digest/scope/base/существенного поля spec; отказ |
| 3 | Stale base до worker, во время worker, до simulated publisher; отказ без публикации |
| 4 | now<issuedAt / =issuedAt / <expiry / =expiry / >expiry; неверный issued/expires; expiry во время worker |
| 5 | Отзыв до запуска, во время worker, после result до checkpoint, после validation; cancel requested ≠ confirmed; evidence store down |
| 6 | Попытка передать credentials в payload/env/доступные файлы; отказ модели, без заявлений о sandbox |
| 7 | ADD/MODIFY/DELETE, duplicate path, collision с неизменённым файлом, protected, mode, malformed Base64, wrong hash, invalid UTF-8, snapshot bytes changed after verification |
| 8 | Replay другого task/revision/run/base; подстановка другого diff/result к validation receipt; корректные разные operationId стадий и неверные межстадийные связи; идентичный operationId после отзыва — только исторический receipt; другой inputDigest — conflict |
| 9 | Approval/repository/evidence/checkpoint store down на каждом read/write; fail closed, защитный cancel разрешён; failpoints INTENT/action/RESULT/checkpoint |
| 10 | Worker report success + invalid diff; независимый validator отклоняет |
| 11 | Для каждого limit ниже/ровно/выше; shared task budget, точная формула резерва и replay после crash; actualOutputBytes=reserve−1/reserve/reserve+1, в том числе когда response < maxOutputBytes, но > reserve; validator не резервирует повторно; два конкурентных runs на последнем слоте, loser без расхода; reserve failure |
| 12 | Happy path при нулевом свободном бюджете после WORKER INTENT: Task REVIEWABLE, Run READY_FOR_OWNER_REVIEW, bytes совпадают, нет pending INTENT, replay projections без скрытых аргументов совпадают, externalCalls=0 |
| 13 | Forged envelope/approval store input; компрометация trusted store явно вне модели |
| 14 | Evidence rewrite, truncate, reorder, recompute hash chain, foreign logId, missing/stale/rollback checkpoint, unknown tail; fail closed |
| 15 | publishAllowed=true до worker, прямой publisher bypass, forged/mismatched validation receipt, подлинный REJECTED receipt, реальные event names запрещены; только SIMULATED_*; externalCalls=0 |

## 10. Предлагаемый точный file scope (только после отдельного разрешения)

Новые файлы (при отсутствии конфликтов после read-only осмотра дерева):

```
tools/agent_mock_b/__init__.py
tools/agent_mock_b/contracts.py
tools/agent_mock_b/canonical.py
tools/agent_mock_b/approval_store.py
tools/agent_mock_b/repository_store.py
tools/agent_mock_b/state_machine.py
tools/agent_mock_b/budget.py
tools/agent_mock_b/snapshot.py
tools/agent_mock_b/diff_validator.py
tools/agent_mock_b/fake_worker.py
tools/agent_mock_b/simulated_publisher.py
tools/agent_mock_b/evidence.py
tools/agent_mock_b/checkpoint.py
tools/agent_mock_b/recovery.py
tools/agent_mock_b/controller.py
tools/agent_mock_b/tests/test_contracts.py
tools/agent_mock_b/tests/test_state_recovery.py
tools/agent_mock_b/tests/test_snapshot_diff.py
tools/agent_mock_b/tests/test_acceptance_15.py
tools/agent_mock_b/fixtures/README.md
tools/agent_mock_b/README.md
docs/agent-mock-b/protocol-v1.md
```

`docs/codex-handoff.md` — **отдельно разрешаемое изменение только документации** для отчёта о передаче; до разрешения не изменять. Никаких `.github/workflows`, Android/Room, v1/v2 и иных файлов. Если дерево репозитория, инструменты тестирования или инструкции handoff требуют иной scope, остановиться и вернуть предложение на review, не расширять автоматически.

## 11. Gate перед кодом и порядок будущих действий

1. Codex проводит **только review** этой единой редакции v1.4; проверяет схемы, совместимость JCS-пакета, форматы, recovery и матрицу. При необходимости корректировки — новая редакция документа, не код.
2. Владелец отдельно утверждает план B v1.4.
3. Владелец **отдельно** разрешает конкретные операции реализации: ветка, файлы, тесты, Draft PR; реальный Codex worker и GitHub publisher не разрешаются.
4. Перед реализацией read-only сверить SHA, дерево, наличие файлов, локальные инструкции и зависимости; расхождения → остановка.
5. Реализация по порядку: contracts/canonical → stores → FSM/budget → snapshot/diff → evidence/recovery → fake worker/publisher → controller → 15 групп tests → handoff/review.

**Текущий статус:** только документ для review. Никаких разрешений на создание ветки, изменение репозитория, запуск тестов, реальных агентов, публикацию или merge.

## 12. История документа

v1.0 — исходная архитектура; v1.1 — уточнения Task/Run, bytes, checkpoint; **v1.2 — полная замена прежних текстов**, без конфликтующих дополнений: синтетические имена событий, защитные переходы без approval, RECOVERY_REQUIRED, строгие множества, авторитетный replay evidence, фиксированные контракты и file scope.


## 13. Нормативное дополнение v1.3: связность результата и воспроизводимый replay

Этот раздел — часть единой редакции v1.3; при противоречии с описательной формулировкой выше применяются нижеследующие строгие контракты. Для реализации схемы должны быть проверены тестами до запуска worker.

### 13.1 Идентичности и результат

`expectedRepository="Arch-relic/AgroNaliz"` и `expectedOwnerId` — независимая read-only конфигурация контроллера; пустой/неизвестный `expectedOwnerId` означает fail closed. `repository` обязательно во всех Specification и RepositoryState, включено в JCS specification digest. Любое несовпадение запрещает запуск.

**WorkerResponse** (никаких дополнительных полей): `{protocolVersion,repository,taskId,revision,taskDigest,baseSha,snapshotDigest,runId,operationId,diffOps,report}`. `diffOps` — упорядоченный массив DiffOp §5, `report` — UTF-8 строка до 4096 символов. `diffDigest=digest("agronaliz.mock-b/1/diff\0", {"diffOps": diffOps})`; порядок операций значим, а дубликаты путей запрещены. `workerResponseDigest=digest("agronaliz.mock-b/1/worker-response\0", WorkerResponse)`. Полученные байты и их размер измеряются до парсинга; worker не задаёт доверенный digest. `WorkerResponse.operationId` равен operationId подтверждённого WORKER INTENT/RESULT. Его нельзя приравнивать к validator или simulated publisher operationId.

**ValidationReceipt**: `{protocolVersion,repository,taskId,revision,taskDigest,baseSha,snapshotDigest,runId,operationId,workerResponseDigest,diffDigest,resultSnapshotDigest,validationStatus,validatorVersion}`; `validationStatus=PASSED|REJECTED`. `resultSnapshotDigest` — hash manifest итоговых bytes по §2, а не заявленное worker значение. `ValidationReceipt.operationId` равен operationId собственной VALIDATOR операции и **отличается** от WORKER и SIMULATED_PUBLISH operationId; связь с worker устанавливается через `workerResponseDigest` и подтверждённый WORKER RESULT. Publisher ссылается на подтверждённый validation event `(logId,sequenceNo,eventHash)` и создаёт собственный operationId. Receipt формирует только trusted validator и сохраняет целиком в подтверждённом `RESULT` evidence с `actor="validator"`; publisher ищет его по доверенному event reference, заново сверяет все поля, привязку к конкретным diff/result и свежие approval/base/checkpoint. Ссылка на чужой, неподтверждённый или подменённый receipt — отказ. Нормативно: publisher принимает только `RESULT` с `payload.operationKind="VALIDATOR"`, `actor="validator"`, `validationStatus="PASSED"`, всеми совпадающими привязками, действующими approval/base и проверенным checkpoint. Подлинный `REJECTED` receipt также запрещён. Даже PASSED не означает права на новую операцию.

### 13.2 Типизированные события и восстановление

Каждое событие несёт `payload` с `kind` и строго перечисленными полями, `additionalProperties=false`. Для `INTENT`: `{kind:"INTENT",operationKind,operationId,inputDigest,taskDigest,approvalId,runId,fromTaskState,toTaskState,fromRunState,toRunState,reservedRunSlot,reservedOutputBytes,remainingTaskOutputBudgetBefore,baseSha,snapshotDigest}`. `reservedRunSlot` — 0 или 1, `reservedOutputBytes` — неотрицательное целое; резерв подтверждается единожды. Для `RESULT`: `{kind:"RESULT",operationKind,operationId,inputDigest,runId,fromTaskState,toTaskState,fromRunState,toRunState,actualOutputBytes,receipt,validationReceipt}` с nullable receipt-полями; результат не освобождает резерв. `APPROVAL_CHECKED/APPROVAL_DENIED/REVOKED/EXPIRED`: `{kind,approvalId,taskDigest,ownerId,checkedAt,reason}`. `VALIDATION_REJECTED/STOP_REQUESTED/CANCEL_CONFIRMED/RECOVERY_REQUIRED`: `{kind,runId,operationId,reason,confirmedStop}`. `SIMULATED_PUBLISH_REQUESTED/SIMULATED_PUBLISHED`: `{kind,runId,validationEventSequenceNo,diffDigest,resultSnapshotDigest,simulated:true,externalEffects:0}`. `outcome` — enum `OK|REJECTED|UNKNOWN`, а не свободный объект; данные симуляции хранятся в payload. Отсутствующие ссылки передаются `null` по явной схеме; `operationKind=WORKER|VALIDATOR|SIMULATED_PUBLISH`. Переходы FSM из события должны соответствовать §4, иначе журнал недействителен.

При replay применяется **только подтверждённый checkpoint префикс**. Из последовательности INTENT/RESULT восстанавливаются Task/Run состояния, использованные слоты, зарезервированные байты, receipts и outstanding intents. Внешние approval/repository stores запрашиваются только для **нового** действия; не подставляются задним числом в replay. Неподтверждённый хвост изолируется; до reconciliation нет нового запуска. Для `RESULT` требуется предшествующий подтверждённый INTENT с тем же `(runId,operationId,inputDigest)`; дубли и несоответствие — `INTEGRITY_FAILED`. Если операция была исполнена, но receipt не подтверждён, запрещено повторять её без независимого ledger; неопределённость означает `RECOVERY_REQUIRED`. Проекция и checkpoint обновляются под одним per-task lock; CAS не доказывает атомарность внешнего действия.

### 13.3 Ресурсные границы

До `SequenceMatcher` trusted validator проверяет, что суммарный размер исходных и результирующих текстовых bytes по всем затронутым файлам не превышает **2 MiB** и число строк в каждом файле не превышает **20 000**; иначе `LIMIT_EXCEEDED`. Эти пределы независимы от `maxDiffLines`. Проверка длины Base64 выполняется до декодирования; общий лимит raw WorkerResponse — `maxOutputBytes`. `reservedOutputBytes = min(maxOutputBytes, maxTotalOutputBytesPerTask - alreadyConfirmedReservedOutputBytes)`; при нуле отказ до INTENT. Фактические сырые байты WorkerResponse обязаны удовлетворять `actualOutputBytes <= reservedOutputBytes <= maxOutputBytes`; поток останавливается до буферизации при превышении резерва, `LIMIT_EXCEEDED`, без VALIDATOR/SIMULATED_PUBLISH. `reservedOutputBytes` резервируется только WORKER INTENT нового run; VALIDATOR и SIMULATED_PUBLISH INTENT записывают нулевой резерв и не требуют положительного свободного остатка. При recovery того же действия используется исходный резерв. Для каждой попытки резерв учитывается единожды; даже crash и новый approval не возвращают его. Неуспешный CAS не создаёт подтверждённого INTENT и не расходует бюджет.

### 13.4 Приёмка дополнительных подслучаев

К группам 2/4/8/11/12/15 добавить соответственно: чужой repository; будущий issuedAt; чужой diff/result при валидном receipt; точный резерв при каждом failpoint; replay без скрытых параметров; forged receipt и прямой обход publisher. Группа 9 проверяет fail closed при недоступности независимой конфигурации expectedOwnerId. Все проверки без сети и реального publisher.

## 14. История изменений и решение владельца

v1.3: привязка repository identity, worker/validator/publisher, типизированные replay-события, временная граница approval, ресурсные пределы.

v1.4: фактический поток WorkerResponse ограничен резервом, только WORKER INTENT нового run резервирует output; publisher принимает только подтверждённый PASSED; operationId независимы по стадиям и связаны digest/evidence. Добавлены подслучаи групп 8/11/12/15.

Это **кандидат для технического review**, не разрешение на написание кода. После review требуется отдельное утверждение владельца и затем отдельное разрешение на реализацию.


---

# Утверждённая нормативная поправка к §13.2

В строгую схему `payload` события `RESULT` добавить обязательное поле
`workerResponseDigest: string | null`.

Для успешного `RESULT` с `operationKind="WORKER"` поле содержит digest полного
`WorkerResponse`, независимо вычисленный доверенным контроллером по §13.1:

`digest("agronaliz.mock-b/1/worker-response\0", WorkerResponse)`.

Значение — строка из 64 строчных шестнадцатеричных символов. Для неуспешного WORKER
RESULT и всех RESULT иных стадий значение равно `null`. Отсутствующее поле нарушает схему.

WORKER RESULT считается подтверждённым только после durable-записи в evidence и
закрепления соответствующего префикса независимым checkpoint.

При replay проверяются целостность журнала и соответствие WORKER INTENT/RESULT по
`(runId, operationId, inputDigest)`. `workerResponseDigest` восстанавливается
исключительно из подтверждённого WORKER RESULT, без скрытых данных контроллера.

VALIDATOR обязан сверить `workerResponseDigest` своего ValidationReceipt со значением
подтверждённого успешного WORKER RESULT того же run.

Simulated publisher обязан проверить эту связь через подтверждённый VALIDATOR RESULT
со статусом `PASSED` и соответствующий подтверждённый WORKER RESULT. Разные стадии
сохраняют разные `operationId`.

Отсутствие обязательного digest, несовпадение значений либо отсутствие подтверждённого
RESULT блокирует дальнейшее действие и симулированную публикацию.

Схемы OperationReceipt и WorkerResponse не изменяются. Остальные положения плана сохраняются.

## Утверждённые дополнительные тесты

- test_contracts.py: обязательность/формат digest, null для остальных RESULT,
  неизменность OperationReceipt и WorkerResponse.
- test_state_recovery.py: replay без исходных аргументов, INTENT/RESULT mismatch,
  неподтверждённый хвост, crash до/после checkpoint, подмена digest и hash chain.
- Группы 8/10: привязка validation к WORKER RESULT того же run, подмена ответа/digest,
  различные operationId, независимость validator от worker report.
- Группы 9/14: недоступные stores/checkpoint, неподтверждённый RESULT,
  rewrite/rehashed chain, отсутствие новых действий.
- Группы 12/15: happy path/replay сохраняют связь; publisher отклоняет отсутствующий,
  чужой, несовпадающий digest и неподтверждённые результаты; externalCalls=0.
