# Issue58 — Agent Environment Gate

22.10.2026. Статус: workflow подготовлен к Draft PR/review, **не запускался**.
Источник: https://github.com/Arch-relic/AgroNaliz/issues/58.
Base main: `0b41d1251363696b5bc99a5109752baa02911ae8`.

## Реализация и границы

Новый файл: `.github/workflows/agent-environment-gate-smoke.yml`.
Только workflow_dispatch, без inputs и автоматических событий. Один job на
ubuntu-latest, timeout-minutes=3, environment=agronaliz-agent-readonly.
Permissions={} на уровне workflow; job их не расширяет. Нет checkout, uses,
секретов/credential references, API/network commands, App, env variables, artifacts,
cache, outputs, файловых изменений, repository write или публикации APK.
Единственная команда — echo "Environment gate approved"; это единственный
пользовательский stdout. GitHub дополнительно формирует стандартные служебные логи.
Платформенный GITHUB_TOKEN не запрашивается/не передаётся команде и не используется.

Agent Core, существующие workflows, активные задачи/approvals, приложение, Room,
backup/signing/release и production policies не изменены. Environment и его protection
rules не создаются и не редактируются этим PR. Указание environment в YAML само
по себе не устанавливает required reviewers и не доказывает ручное подтверждение.

По сообщению владельца в Issue58 environment уже настроен: reviewer Arch-relic,
self-review разрешён, admin bypass выключен, deployment branch rule только main,
секретов нет. Эти настройки не были независимо проверены в live запуске.
Проверка environment не является approval задачи, G2 machine grant или разрешением G3–G6.

## Статическая проверка

YAML разбирается PyYAML SafeLoader с отказом на duplicate keys. Ключ 'on' заключён
в кавычки, чтобы YAML1.1 parser не трактовал его как boolean; для Actions это обычный on.
Проверяется точное содержимое структуры, а не только корректность синтаксиса:

```python
from pathlib import Path
import yaml

class UniqueLoader(yaml.SafeLoader):
    pass

def unique_mapping(loader, node):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node)
        if key in result:
            raise ValueError('Duplicate YAML key')
        result[key] = loader.construct_object(value_node)
    return result

UniqueLoader.add_constructor(
    yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, unique_mapping)
path = Path('.github/workflows/agent-environment-gate-smoke.yml')
workflow = yaml.load(path.read_text(), Loader=UniqueLoader)
assert workflow == {
    'name': 'Agent Environment Gate',
    'on': {'workflow_dispatch': None},
    'permissions': {},
    'jobs': {'environment-gate': {
        'runs-on': 'ubuntu-latest',
        'timeout-minutes': 3,
        'environment': 'agronaliz-agent-readonly',
        'steps': [{'run': 'echo "Environment gate approved"'}],
    }},
}
```

Дополнительно: git diff --check; полный diff только из этого нового YAML, этого
документа и append-only handoff. Существующие tracked файлы вне этих путей не меняются.
Actionlint не установлен в среде; его успешный запуск не заявляется. Зависимости
репозитория ради проверки не добавляются. Android build не нужен: app не меняется.

## Приёмка и оставшиеся проверки

| Проверка | Результат подготовки |
| --- | --- |
| YAML parse, отсутствие duplicate keys и exact workflow structure | passed |
| Только manual trigger, один job, нужный environment, timeout3 | passed |
| permissions={}, отсутствие actions/checkout/secrets/tokens/API/write | passed по исходнику |
| Только заданная строка пользовательского вывода | passed по исходнику; workflow не исполнялся |
| Scope/diff, сохранность Agent Core/app/production policies | passed |
| Waiting for review и owner approval | not_run, нужно отдельное разрешение |
| Job success после owner approval, live отсутствие write | not_run |

Trusted PR gate защищает также .github/workflows/. Поэтому этот PR может получить
штатный governance отказ. Его нельзя ослаблять/обходить, маскировать skipped check
или менять production/main policies. Фактические автоматические CI отражаются в PR;
они не запускают новый manual-only workflow и не доказывают environment approval.

## Будущий live тест — только по отдельным разрешениям владельца

1. Review Draft PR и точного head/base/diff, проверка настроек environment владельцем.
2. Отдельное разрешение на merge workflow в main; без автоматического merge.
3. Отдельное разрешение на workflow_dispatch именно с ref=main.
4. Убедиться, что job ожидает required review и команда ещё не выполнялась.
5. Владелец Arch-relic подтверждает deployment в GitHub UI; агент не подтверждает за него.
6. Проверить job success и единственный пользовательский вывод, отсутствие unexpected
   steps/write/secret use; сохранить run/attempt/source SHA и approval evidence.
7. Удалять workflow либо менять environment только по отдельному решению, без auto-cleanup.

Если job стартовал без ожидаемого review, ref не main, settings изменились или появились
неожиданные steps/permissions — STOP и отчёт владельцу. Не ослаблять main-only rule,
не создавать другой environment или credential ради обхода. До отдельного разрешения
не выполнять dispatch, rerun, approve, merge или изменения settings. Сейчас — review.
