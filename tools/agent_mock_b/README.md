# Агронализ: mock B

Локальная детерминированная модель `agronaliz.mock-b/1`, план v1.4 Final и
отдельно утверждённая поправка к WORKER RESULT. Нормативный текст и поправка:
[`docs/agent-mock-b/protocol-v1.md`](../../docs/agent-mock-b/protocol-v1.md).

## Запуск тестов

Требуется официальный внешний пакет **rfc8785==0.1.4**. Исходники библиотеки не
включены в репозиторий; `canonical.py` проверяет установленную версию при импорте.
Установка зависимостей не является действием этой модели. В проверенной сессии:

```bash
PYTHONPATH=/tmp/codex-rfc8785-igo5yiag:$PWD python -B -m unittest discover -s tools/agent_mock_b/tests -v
```

Путь временный и может исчезнуть с сессией. При отсутствии/несовместимости пакета
остановиться; не подменять JCS собственным сериализатором. Установка в другом
окружении требует отдельного разрешения владельца, данная команда ничего не
устанавливает и не обращается в сеть. Тесты используют стандартный `unittest`.

## Архитектура и границы

`Controller.execute` принимает Specification, synthetic ApprovalEnvelope, run UUID
и три различных operation UUID. Trusted `Trust`, `ApprovalStore`, `RepositoryStore`,
`Evidence`, `Checkpoint`, synthetic clock и FakeWorker передаются отдельно.
Worker получает только копию identity request; его тестовый response factory
формирует synthetic bytes. Контекст payload/env/accessibleFiles допускается только
пустой. Реальные файлы/переменные среды/credentials worker не получает. Это API
модели, **не OS sandbox** и не способ безопасно исполнять недоверенный Python.

Модель не содержит сетевого клиента, subprocess, GitHub API, реального агента,
Git publisher, автоматических workflows или production credentials.
`workerKind=mock`, `publishAllowed=false`, `externalCalls=0`. Создание этого Draft PR
выполняется отдельно инструментом разработчика по прямому разрешению владельца;
модель сама не создаёт ни веток, ни PR.

Snapshot — копия immutable bytes, только UTF-8 regular files. Validator повторно
применяет структурированный diff и проверяет итог целиком. Simulated publisher
проверяет цепочку WORKER RESULT → VALIDATOR RESULT через подтверждённые evidence
references и повторно проверяет bytes/diff; его результат — только события
`SIMULATED_*`, а не публикация. Недостоверный результат не даёт новых прав.

## Evidence, бюджеты и восстановление

Evidence и checkpoint записываются только в каталог, выбранный trusted test harness
(тесты всегда используют `TemporaryDirectory` вне репозитория). Journal append
выполняет fsync; независимый checkpoint adapter — conditional monotonic update,
fsync файла, replace и fsync каталога. Его отдельный in-memory anchor моделирует
доверенное независимое хранилище. Для повторного чтения сохраняется тот же adapter;
восстановление доверенного anchor только из файлов не поддерживается и не заявляется.
Потеря/подмена независимого доверия требует остановки. Нет multiprocess-гарантии.

Per-log lock сериализует действия, checkpoint CAS закрепляет подтверждённый префикс.
Trusted approval adapter привязывает taskDigest к одному retained log в пределах
одной модели: новый log не обнуляет общий budget. Замена доверенных adapters —
другая модель, не способ восстановления прежней. Replay не читает текущие approval,
repository, worker response factory или исходные аргументы контроллера.

WORKER INTENT резервирует слот и output bytes один раз, включая неуспешные попытки.
VALIDATOR/SIMULATED_PUBLISH не резервируют output повторно. Длительность измеряется
инъецированным monotonic clock; UTC approval проверяется отдельно. В evidence
переход может охватывать упорядоченные transient-состояния AUTHORIZED/RUNNING;
успешный RESULT подтверждает выполнение, один INTENT — только намерение.

`recovery_decision` возвращает read-only результат проверки checkpoint prefix и
независимого synthetic worker ledger. Неизвестный хвост не принимается и не
отбрасывается; pending action не запускается повторно. Обычный `execute` не
возобновляет незавершённый run: после подтверждённого WORKER RESULT выдаётся только
исторический receipt. Дальнейшее reconciliation требует отдельного разрешённого
решения; автоматического resume нет. `Controller.stop` — защитная процедура без
approval: CANCELLED фиксируется лишь при synthetic acknowledgment. Недоступность
journal/checkpoint даёт только `STOP_REQUESTED_UNVERIFIED`, а не durable CANCELLED.

Эти тесты доказывают свойства synthetic модели и обработку failpoints, а не
защиту ОС, реальных credentials или crash consistency независимого production store.
