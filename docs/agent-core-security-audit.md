# Agent Core: повторный аудит PR #47 — 21.10.2026

PR: https://github.com/Arch-relic/AgroNaliz/pull/47 (Draft).
Ветка: `feature/agent-core-stage1`.
Опорный main: `2a37bfe05c9b60f04035f7b64fae7f726117d647`.
Проверенный GitHub SHA: `5e80cb913811e48f8da4bd918b496bfd15032be6`.
Дерево: `ac9ff4bb5da7c4e14438ce4e379321f079015f3d`.
Локальный эквивалент с тем же деревом: `5b6d260fba70ec8c7e838b414a7cc38752ed995b`.
Отчёт сохраняется последующим коммитом документации; SHA выше не обозначает этот коммит.

## Синхронизация и исправление

В feature добавлен merge-коммит с родителями прежнего PR head
`2d329de2876961181505113c3c932e2db56c424c` и указанного main. История не переписана,
force push не применён. Конфликт был только в `docs/codex-handoff.md`: сохранены
полный актуальный handoff main и все записи Agent Core. Код Issue #48/PR #49 не включён.

Исправлена техническая ошибка `agent_core/core.py`: GitHub Contents API возвращает
Base64 с переводами строк, несовместимыми с прежним строгим декодированием.
Теперь удаляются только CR/LF, затем применяется `validate=True`. Неверный алфавит
по-прежнему отвергается. Добавлены положительный и отрицательный тесты в
`tests/agent_core/test_core.py`. Протокол и архитектура не изменены.

## Границы доверия и результат аудита

- PR выполняет unit-тесты с `contents: read`; checkout закреплён SHA, credentials
  не сохраняются. Токен/production secrets тестам не передаются.
- Trusted job читает код и схемы из точного base SHA; данные кандидата получает
  через API. Код PR не checkout/import/run. PR head/base повторно сверяются.
- Согласование проверяется независимо через GitHub API: числовой ID владельца,
  actor и triggering_actor, репозиторий, owner/main dispatch, первая попытка,
  успешный завершённый run, workflow ID/path и точный scope в display_title.
- Неизменяемый task из run.head_sha связывает разрешение с редакцией ТЗ, её SHA256,
  операциями, исполнителем и точным префиксом истории. Комментарии и метки
  полномочий не дают. Изменение ТЗ, подделка/повтор grant, самосогласование,
  missing evidence/API errors приводят к отказу.
- Редакции, решения и история append-only относительно base. Удаление задач,
  изменение governance/workflow, в том числе через rename, отклоняются.
- Room/backup/signing/production_policy/merge_main/publish_apk всегда запрещены
  в этапе 1. Проверка не выдаёт отдельное разрешение на эти операции.
- Trusted permissions ограничены contents/actions/pull-requests: read. Shell
  не исполняет выражения из task/PR. Записи в main, уведомлений, сервера,
  автоматического Codex и ИИ-анализа нет. Schedule проверяет только контракт.

Все 10 существующих workflow, `AGENTS.md` и весь `app/` побайтно совпадают с main.
Наследованные изменения MAIN1.0.10 при синхронизации сохранены, новых изменений
production-политик/подписей/Room24/backup нет. Publish workflow остаются ручными
и не запускались. APK/Release не публиковались; физическое устройство не использовалось.

## Воспроизводимые локальные проверки

Новые запуски на указанном дереве:

```bash
python3 -m unittest discover -s tests/agent_core -q
python3 -m unittest discover -s docs -p 'test_*.py' -q
python3 -m agent_core.check local
python3 -m compileall -q agent_core tests/agent_core
git diff --check
```

Agent Core: 53 tests, 0 failures/errors/skips. Существующие Python-проверки:
107 tests, 0 failures/errors, 2 SDK skips. Вложенные временные patch-tests не
прибавляются к 107. Local validation успешно проверяет пустые рабочие каталоги,
не доказывает реальное согласование. Все 11 YAML workflow разобраны, triggers
и разделение jobs проверены. Схемы проверяются автоматическими тестами.
Android локально не собирался: код приложения не изменён; сборки и эмуляторы — CI.

## CI проверенного SHA

Все 7 применимых workflow завершились success на проверенном SHA.
Это новые runs, а не прежние результаты PR47/PR49.
Ссылки в таблице ведут к фактически просмотренным jobs/logs.

| Проверка | Run | Результат |
| --- | --- | --- |
| Agent Core | [39560669437](https://github.com/Arch-relic/AgroNaliz/actions/runs/39560669437) | success: 53 теста; trusted-validation skipped для PR |
| Android PR checks | [39560669436](https://github.com/Arch-relic/AgroNaliz/actions/runs/39560669436) | success: сборки MAIN/TEST, JVM tests, 53 инструментальных вызова (107 исполнений) |
| MAIN safe update | [39560669423](https://github.com/Arch-relic/AgroNaliz/actions/runs/39560669423) | success: 28 Python + 3 инструментальных вызова |
| Room22→23 regression | [39560669450](https://github.com/Arch-relic/AgroNaliz/actions/runs/39560669450) | success: 3 инструментальных вызова |
| Room23→24 MAIN/TEST | [39560669441](https://github.com/Arch-relic/AgroNaliz/actions/runs/39560669441) | success: 10 инструментальных вызовов |
| Room24→24 MAIN1.0.10 | [39560669480](https://github.com/Arch-relic/AgroNaliz/actions/runs/39560669480) | success: 10 инструментальных вызовов |
| UI API23/API35 | [39560669438](https://github.com/Arch-relic/AgroNaliz/actions/runs/39560669438) | success: 6 + 6 инструментальных вызовов |

Changelog и detached-policy workflows не применяются к diff PR (paths filters).
Политики проверены в составе 107 локальных Python-тестов. Оба publish workflow
не запускались. Число инструментальных вызовов не следует смешивать с количеством
уникальных методов или JVM-тестов. Отчётный коммит меняет только документацию;
автоматические повторные CI на нём не подменяют результаты проверенного SHA.

## Ограничения и готовность к merge

**Блокирующий результат аудита: PR не готов к merge.** `authorize()` не сверяет
переданный снимок задачи с её актуальным доверенным состоянием. Старый approved
JSON продолжает давать `implement`, даже когда текущая задача уже blocked,
cancelled или awaiting_approval. Проверяются только исторический run и задача
на `run.head_sha`; свежий статус/редакция не читаются. Это пробел отзыва допуска,
а не подделка GitHub identity. Автоисполнителя сейчас нет, поэтому фактический
автоматический запуск не произошёл, но библиотеку нельзя считать безопасным
самостоятельным источником полномочий для будущего исполнителя.

Воспроизведение на указанном SHA (синтетические данные, реальные данные не нужны):

```bash
python3 - <<'PY'
import sys, copy, base64, json
sys.path.insert(0, 'tests/agent_core')
from test_core import CoreTests, move
from agent_core.core import authorize
for state in ['blocked', 'cancelled', 'awaiting_approval']:
    f = CoreTests(); f.setUp()
    stale = copy.deepcopy(f.t)
    current = copy.deepcopy(f.t); move(current, state)
    reads = []
    def api(path):
        reads.append(path)
        if path == 'contents/agent_core/tasks/AGN-1.json?ref=main':
            return {'content': base64.b64encode(json.dumps(current).encode()).decode()}
        return f.api(path)
    authorize(stale, 'implement', api, actor_id=123)
    print('STALE ACCEPTED:', state, 'API reads:', reads)
PY
```

Фактический результат: все три вызова приняты; каждый читает только run,
workflow identity и contents по историческому SHA. Чтения текущей задачи нет.
Существующие тесты отмены/блокировки проверяют уже обновлённый JSON; этот сценарий
с устаревшим снимком они не покрывают. Зелёный CI не снимает найденный дефект.

Варианты, требующие согласования владельца по условию текущего задания:

1. Рекомендуется: сделать актуальную задачу в main доверенным источником отзыва.
   Перед authorize получать main SHA, читать задачу по этому SHA, сверять снимок,
   актуальную редакцию/состояние и повторно проверять main SHA; при несовпадении
   или ошибке — отказ. Исполнитель должен работать только по актуальному допуску
   и повторять проверку перед каждым действием. Нужны тесты устаревших снимков,
   смены main во время проверки, отзыва и смены редакции. Это определяет момент
   вступления отзыва в силу (после попадания метаданных в main).
2. Отдельный доверенный журнал отзывов/состояния в GitHub, независимый от PR.
   Потребуются новый протокол, права и правила гонок; это более крупное изменение.

Логика отзыва не изменена: пользователь требует остановиться для согласования
при необходимости изменения архитектуры. Следующий шаг — выбрать источник
актуального состояния и момент вступления отзыва в силу, затем отдельное исправление.

API main сообщает `protected: false`, required checks выключены, rulesets `[]`.
Права администрации интеграции отсутствуют; настройки не изменялись. Поэтому
workflow не способен технически запретить обход пользователю с правом записи.
Установку обязательных checks и защиту governance следует согласовать отдельно.

Bootstrap: новый trusted workflow ещё отсутствует в main. Для pull_request
`trusted-validation` ожидаемо skipped. Реальный owner/main approval и weekly run
не проверены; моки и зелёные unit-тесты не заменяют такую проверку. После отдельного
решения владельца о первом включении нужны регистрация синтетической задачи,
положительный owner dispatch и отрицательные проверки реального протокола.

Gate проверяет зарегистрированные задачи, не семантику произвольного Android diff.
Будущий исполнитель обязан использовать доверенные core/API и независимо
установленный ID при каждом authorize. Интеграция Codex не готова и не заявляется.
Удаление/недоступность run evidence приводит к отказу.

PR остаётся Draft; merge не рекомендуется из-за описанного пробела отзыва. Полную эксплуатационную готовность Approval
Gate до live-проверки и решения об обязательности checks подтверждать нельзя.
Merge PR, изменение архитектуры/production-политик и выпуск не выполняются.
