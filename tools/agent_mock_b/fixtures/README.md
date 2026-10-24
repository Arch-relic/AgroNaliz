# Synthetic fixtures

Fixtures генерируются в `tests/test_acceptance_15.py` классом `Fixture`: только
synthetic owner, UUID, baseSha, UTC/monotonic clock и bytes `scratch/a.txt`.
SHA значений bytes вычисляется при создании; данные реального репозитория, аккаунтов,
PDF и рабочие credentials не используются. Все runtime-файлы — во временных каталогах.

Fixture содержит spec, approvalStore, repositoryStore, snapshotBytes, workerResponse,
clock, failpoint, expectedTaskState, expectedRunState, expectedEvents, workerStarts,
publisherCalls, externalCalls, confirmedBudget и checkpointRoot. Начальные ожидания
описывают happy path; отрицательные сценарии отдельно утверждают соответствующие
коды, счётчики, checkpoint prefix/tail, budgets и состояния. Root проверяется по
независимому checkpoint и последнему подтверждённому eventHash, не выдумывается.

| Группа | Проверки в test_acceptance_15 и дополнительных тестах |
|---|---|
| 1 | Нет approval; нет фиктивного run; protective cancel без approval |
| 2 | Подмена repository/task/revision/base/spec/digest/scope |
| 3 | Stale base до worker, после worker, до simulated publish |
| 4 | UTC boundaries, future issuedAt, invalid calendar, expiry во время worker |
| 5 | Отзыв до worker, во время worker, между RESULT append/checkpoint, после validation; cancel ack и outage |
| 6 | Непустые payload/env/accessibleFiles отвергаются до worker |
| 7 | ADD/MODIFY/DELETE; paths/collisions/protected/mode/Base64/hash/UTF-8; mutation; resource guards |
| 8 | Чужие identity/run/base/diff/result; stage operation UUID; historical receipt/conflict; evidence digest link |
| 9 | Approval/repository/evidence/checkpoint outage; read/write; INTENT/action/RESULT/checkpoint failpoints; missing owner |
| 10 | Worker success не заменяет independent validation; REJECTED receipt |
| 11 | Каждый limit ниже/ровно/выше; резерв/остаток, shared budgets/new approval, no refund, concurrent last slot, CAS loser |
| 12 | Happy path с нулевым свободным budget; exact bytes; no pending; replay без скрытых данных |
| 13 | Forged envelope/store record; trusted store compromise вне модели |
| 14 | Rewrite/truncate/reorder/rehash/foreign log/checkpoint rollback/missing/tail |
| 15 | publishAllowed=true; direct bypass; forged/mismatched/REJECTED receipt; только SIMULATED_*; zero external calls |

Тесты поправки: обязательный WORKER RESULT digest; null для остальных RESULT;
неизменность WorkerResponse/OperationReceipt; receipt/digest из подтверждённого
префикса; несовпадения INTENT/RESULT; foreign/missing/rejected links; запрет publish
при неподтверждённом RESULT. Test-only `reseal` может создать заведомо некорректный
семантический журнал с synthetic anchor для независимой проверки replay. Это не
production API и не механизм восстановления или обхода integrity check.
