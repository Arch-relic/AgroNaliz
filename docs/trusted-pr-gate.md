# Этап 4.3 — trusted-pr-gate, проект к owner review

Статус: реализация для Draft PR, **не действующая защита main**. Репозиторий
Arch-relic/AgroNaliz (id 1411593273), base main
4c21c59acf8febc71d123e3582b74eeac613b49e. Rulesets/Branch Protection, App permissions,
production secrets и существующие workflows не изменяются этим проектом.

## План и границы реализации

1. Сохранить существующий agent-core/trusted-validation неизменным. Новый gate —
   отдельный self-contained stdlib скрипт и отдельная policy в доверенном base.
2. Workflow `.github/workflows/trusted-pr-gate.yml` запускается pull_request_target
   для main на opened/synchronize/reopened/edited/ready_for_review, включая Draft.
   Нет path filters, job if, needs, approval inputs, caches, artifacts или dispatch.
3. Checkout закреплённого event base SHA через SHA-pinned checkout action,
   sparse path только `.github/trusted_pr_gate`, persist-credentials=false,
   submodules/lfs=false. Python -I -B игнорирует PYTHONPATH и пользовательский site.
   Ни кода, ни зависимости из PR в этом runner не выполняются. Права только
   contents:read, pull-requests:read, actions:read; используется только штатный
   read-only GITHUB_TOKEN. Нет credentials App, publication/signing secrets.
4. Через GET API проверить event/run identity, repository id/name, workflow ref/SHA,
   run attempt, current open PR, head/base и текущую main ref. Повторить PR/ref read
   после проверки. Base должен быть предком head — stale/diverged PR отказывается.
5. Проверить полную пагинацию files, число changed_files и независимо вычисленный
   состав изменений из полных recursive trees. Truncated/unknown/oversize/неполные
   данные, API/JSON ошибки, редиректы, изменение SHA — отказ. Все HTTP GET ограничены
   временем/размером. Нет повторов API или действий после сбоя.
6. Сверить пути с policy из base; обнаружить protected path и previous_filename.
   Rename/copy не поддерживаются и не получают автоматический допуск. Разрешены
   только regular blobs mode100644, ADD/MODIFY/DELETE. Case aliases, symlink,
   submodule/chmod, path traversal, неизвестный статус и несоответствие tree/file SHA
   отвергаются. Diff/PR blobs никогда не импортируются и не исполняются.
7. Отдельный pull_request workflow `trusted-pr-gate-tests` тестирует candidate code
   без secrets/write permissions. Его результат **не является** trusted-pr-gate.
8. Выполнить десять обязательных synthetic API сценариев, регрессию Agent Core,
   записать ограничения, создать Draft PR, проверить доступные CI и остановиться.

## Предлагаемая политика

Policy v1 в `.github/trusted_pr_gate/policy.json` — предложение для owner review.
После будущего отдельного merge читается только её версия из base; policy из PR,
labels/comments/approval text или результат модели не являются разрешением.

Предложен узкий allowlist: docs/*.md/*.txt (рекурсивно), Kotlin/Java в существующих
app/src/main/java, app/src/test/java и app/src/androidTest/java. Неизвестные пути и
расширения закрыты. Это path/composition policy, **не семантическое доказательство
безопасности Android-кода** и не замена owner review и отдельных CI.

Независимо от allowlist защищены .github, .git, agent_core, tools/agent_mock_b,
tests/agent_core, tests/trusted_pr_gate, gradle, root AGENTS/build/settings/config,
все расположения CODEOWNERS и пути с маркерами permissions/policy/credentials/
secrets/publisher/signing/keystore/release/trusted/agent security. Проверки защиты
case-insensitive для ASCII; исходные пути не нормализуются. Коллизии конечного
snapshot дополнительно сверяются casefold по всем файлам, включая неизменённые.
Отчёт docs/codex-handoff.md не является источником policy/authorization.

Защищённый PR всегда получает отказ и требует отдельного решения владельца.
Нет параметра, label или callback, позволяющего проверке разрешить собственную
модификацию. Отчётный receipt с PR/head/base/policy hash/run ID — диагностический
результат, не machine grant, не разрешение запускать агента и не право merge.
Policy hash — локальный audit hash JSON policy, не новый digest/grant протокола B.

## Точное имя и источник результата

| Поле | Значение / статус |
|---|---|
| Workflow | `.github/workflows/trusted-pr-gate.yml`, `Trusted PR gate` |
| Job id и явное job/check name | `trusted-pr-gate` |
| Триггер | pull_request_target, main |
| Исполняемый скрипт/policy | event pull_request.base.sha, совпадающий с current main |
| Декларируемый источник check | GitHub Actions App, ID15368, slug github-actions |
| Head результата | current PR head; run metadata head_sha сверяется с ним |
| Execution/base | GITHUB_SHA/GITHUB_WORKFLOW_SHA/base ref сверяются с event base |
| Candidate tests | `trusted-pr-gate-tests`, **другое имя и недоверенный source context** |
| Live check identity нового workflow | Не подтверждена: workflow ещё отсутствует в main |
| Required enforcement / non-spoofability | **Не доказаны и не включены** |

App ID15368 получен из реальных check-runs PR62, а не придуман. В одном head
230e399f... API уже показывает одноимённые trusted-validation от двух запусков
с одним App ID: pull_request=skipped и pull_request_target=success. Это наблюдение
подтверждает неоднозначность селектора name+App, но не заменяет live negative test.

## Критическое ограничение: check name + Actions App не закрепляют workflow

Контент PR способен создать job с тем же именем `trusted-pr-gate`, который будет
иметь тот же Actions App ID. Выбор этого App как expected source не различает
workflow path, event и доверенный base. Прикладная проверка собственного run этих
ограничений GitHub merge enforcement не меняет. Гонка «fake check появился позже»
не устраняется одним просмотром списка checks внутри gate.

GitHub документирует проходные required conclusions success/skipped/neutral;
условно пропущенный job может считаться проходным. Отсутствие/фильтрация workflow
может оставить Pending. У нового gate нет if/path filters/needs, однако это **не
доказательство**, что злоумышленник не добавит другой одноимённый job. Без нового
события изменение main после финального чтения также не устраняется самим скриптом.
Merge queue (merge_group) этим минимальным проектом не поддерживается.

Официальные источники, прочитанные при подготовке:

- https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches
- https://docs.github.com/en/pull-requests/how-tos/merge-and-close-pull-requests/troubleshooting-required-status-checks

Тест `test_controlled_name_and_actions_app_collision_is_not_a_trust_proof` —
контролируемая synthetic контрмодель **небезопасной** настройки: trusted/forged jobs
имеют одинаковый селектор name+App; skipped/neutral также не доказывают выполнение.
Он показывает, почему эту настройку нельзя принимать. Это не live GitHub proof
невозможности обхода, и результат не выдаётся за него.

## Bootstrap и следующие отдельные решения

В main нового target workflow ещё нет; этот Draft PR не может авторитетно проверить
сам себя новой версией gate. Изменение workflows должно получить штатный отказ от
существующего agent-core/trusted-validation. Его нельзя отключать, переименовывать
или обходить ради зелёного CI. Отказ bootstrap ожидаем и не означает, что новый
trusted-pr-gate уже исполнялся. Для candidate unit tests существует отдельный
непривилегированный workflow, без заявления об авторитетности его результата.

До включения enforcement владелец должен отдельно выбрать поддерживаемый механизм:
платформенно обязательный конкретный trusted workflow, если это реально поддержано
текущим account/repo/тарифом, либо отдельный независимый App/controller, с идентичностью проверки,
недоступной candidate Actions. Второй путь потребует отдельной архитектуры,
точных ограниченных permissions и согласования; этот PR ничего такого не выдаёт.
Обычный required name+github-actions **не является допустимым fallback**.

После отдельного разрешения нужны live отрицательные испытания: PR с одноимённым
успешным/пропущенным job, отсутствием настоящего результата, wrong/stale SHA и
несовпадающим source; проверка фактического запрета merge. Нужны строгая актуальность
base, контроль смены source, владельческий review и отсутствие bypass агента.
Если механизм недоступен или результаты неоднозначны — L1/L2 остаются закрытыми.
Ни настройка Rulesets, ни проверочный merge, ни установка App этим ТЗ не разрешены.

## Матрица локальных испытаний

| № | Сценарий | Результат |
|---|---|---|
| 1 | Обычный docs PR | PASS, receipt связан с head/base |
| 2 | Workflow/rename из workflow | Отказ PROTECTED_CHANGE |
| 3 | Checker/policy/publisher | Отказ PROTECTED_CHANGE |
| 4 | Agent permissions/signing | Отказ PROTECTED_CHANGE |
| 5 | CODEOWNERS во всех расположениях/регистре | Отказ PROTECTED_CHANGE |
| 6 | Изменение head/base во время проверки | Отказ PR_CHANGED/BASE_CHANGED |
| 7 | API outage на каждом чтении | Ошибка, CLI возвращает nonzero |
| 8 | Неполные files/trees/unknown/tree mismatch | Отказ |
| 9 | Неисполнение PR code/dependencies | Только metadata/tree GET; static workflow checks |
| 10 | Rerun | Отказ RERUN_FORBIDDEN; старый receipt не используется |

Дополнительно: страницы100/101/200, truncated tree, case aliases, symlink/submodule/
chmod, JSON duplicates/NaN, unknown policy/event/SHA, отсутствующие данные и
контролируемая контрмодель подмены check source. Никаких live exploit/merge попыток.

```bash
python -I -B -m unittest discover -s tests/trusted_pr_gate -v
python -B -m unittest discover -s tests/agent_core -q
```

Новые запуски: 16 gate tests и 140 существующих Agent Core tests, все passed.
Это отдельные test methods; циклические подслучаи не выданы за отдельные тесты.
AST/YAML parse выполнен локально, без установки зависимостей.
Live end-to-end самого нового target gate и его неподменяемость остаются непроверенными.
Готовность Draft PR означает готовность к рассмотрению, а не завершённую защиту main.
