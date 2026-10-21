# Первый запуск Agent Core: план для review владельца

Статус: **подготовка; реальные согласования не запускать**.
Исходный main: `e5e0aeae9474307562a90ca4d3d075dc11800841`, PR #47 merged.
Дерево main: `159b09961b9f50c2d46633238de00e88330978e0`.
Дата аудита: 21.10.2026. Перед исполнением заново проверить актуальный main.

Этот PR предлагает документацию и неактивный пример. Он не регистрирует задачу,
не выдаёт разрешение и не меняет workflows/permissions/защиту main.
Автоматические read-only PR checks допустимы; это не owner approval.

## Что установлено и что остаётся непроверенным

PR #47 объединён владельцем; workflow и исправленный fresh-main authorize находятся
в main. `agent_core/tasks/` содержит только `.gitkeep`. Main API сообщает
`protected:false`, rulesets `[]`. Настройки не изменялись.
Workflow имеет contents:read; trusted job дополнительно actions:read и
pull-requests:read. Production secrets и write permissions ему не нужны.
Checkout trusted job закреплён на base SHA/main SHA, код PR не исполняется.

Наличие файла workflow не доказывает, что Actions включены и workflow активен.
Connector отклонил GET workflow-detail/runs-by-workflow как неподдерживаемые URL;
это ограничение connector, не результат проверки GitHub workflow state.
Владелец должен проверить состояние в Actions перед любым запуском. Если workflow
выключен или policy требует новых прав — остановиться, не включать и не расширять
права автоматически. История реальных approvals здесь не проверена исчерпывающе;
прежние unit/PR runs не считаются live grant. Положительный live approval этим этапом
не подтверждён.

## Синтетический пример и ограничения

`task.example.json` — схема-valid пример proposed-задачи `AGN-47001`, редакция
`bootstrap-r1`, операции строго `['test']`. Он намеренно находится в docs,
**вне `agent_core/tasks/`**. Ни core local, ни trusted task checker не регистрируют его.
Поля не являются полномочиями, ни одного approval_run нет.

ID 123 — только синтетический пример, не проверенный/назначенный исполнитель.
Issue #47 — ссылка на исходный PR Agent Core, не утверждённая live test issue.
**Не копировать этот файл в активные задачи как есть.** До регистрации владелец
выбирает существующую проверяемую GitHub identity, отличную от owner ID240867647,
и реальную issue теста. Нельзя назначать владельца собственным исполнителем,
приглашать пользователей, выдавать write, создавать токены или выдумывать identity
ради прохождения проверки. Если подходящей identity/issue нет — STOP для решения
владельца. Создание issue также отдельно согласовать; этот PR её не создаёт.

После выбора проверить уникальность task ID и имени файла на актуальном main,
согласовать конкретные executor/issue/spec/operations/constraints/риски.
Правки spec делать до утверждения с пересчётом digest; утверждённые редакции
потом не переписывать. Ввод task ID/issue/executor до регистрации — часть подготовки,
но не свидетельство согласования. Список операций не расширять для удобства тестов.

Доступный пример digest (только offline fixture, не данные реального dispatch):

- spec: `dc6ac69e60bf3931add8343d40ba26e5a04d0d371bc6ddde01689846a3901f3b`;
- operations: `ecfd160805b1b0481fd0793c745be3b45d2054582de1c4df5d9b8fa4d78e7fbc`.

## Этапы и точки остановки

| Этап | Допустимое действие | Обязательная граница разрешения |
| --- | --- | --- |
| P0, сейчас | Draft PR, offline fixture/schema/tests, read-only аудит | Остановиться после отчёта для review |
| P1 | Подготовить отдельный metadata PR с выбранными владельцем identity/issue и task | Только после разрешения на регистрацию; merge решает владелец отдельно |
| P2 | Owner/main dispatch с точными полями одной awaiting_approval-редакции | Только отдельное разрешение на конкретный live approval и scope |
| P3 | Read-only сбор/проверка run evidence; metadata PR перехода approved | Отдельное разрешение на live валидацию и изменение состояния; merge владельцем |
| P4 | Согласованные отрицательные случаи и отмена синтетической задачи | Отдельное разрешение на каждый набор dispatch/metadata изменений; merge владельцем |

Ни merge этого документа, ни метка/комментарий не считаются машинным approval.
После P0 агент не переходит к P1–P4 по умолчанию. Все действия ниже — будущая
инструкция, **не разрешение исполнять её сейчас**.

## P1: подготовка регистрации после разрешения

1. Проверить PR #47, свежий main SHA, source-файлы core/check/schema/workflow и отсутствие
   неожиданных изменений. Проверить Actions и owner numeric ID независимыми GitHub API
   данными, а не текстом комментария. Не переопределять доверенный API/actor из PR.
2. Создать отдельный metadata PR для `agent_core/tasks/AGN-47001.json` с утверждёнными
   owner identity/issue параметрами. Начальная история proposed сохраняется; добавить
   proposed→awaiting_approval с реальным UTC-временем и причиной. Операции только test.
   Никаких фиктивных approved/grant/run IDs. Исполнитель не выполняет никакие действия.
3. Проверить schema/hash/переходы и trusted PR-target check из актуального main.
   Expected: read-only validation success, не grant. Owner review отдельно, merge
   не автоматизировать. Нет write permissions в workflow даже для metadata.
4. После решения владельца о merge подтвердить содержимое задачи строго на новом
   main SHA: awaiting_approval, правильная редакция/hash, executor/issue, история,
   операции. Лишь затем готовить параметры dispatch. Изменение main до dispatch
   требует повторной сверки. Если данные расходятся — STOP.

## P2: только владелец и только после разрешения на live approval

В Actions выбрать **Agent Core → main → mode=approve**, первая попытка.
Не запускать из feature/PR, не менять permissions/secrets, не использовать rerun
как новое согласование. Ни одной такой команды этот PR не выполняет.

Поля вычисляются из задачи на конкретном проверенном main SHA. Следующий код
**только читает файл и печатает параметры**, не делает API calls или dispatch.
Его запускать из checkout проверенного main, где задача уже зарегистрирована;
не подставлять docs example как live задачу:

```python
from pathlib import Path
from agent_core.core import load_json, validate, digest, approval_title, OWNER_ID
t = load_json(Path('agent_core/tasks/AGN-47001.json').read_bytes())
validate(t)  # Первая awaiting-задача без исторических grants.
assert t['state'] == 'awaiting_approval' and t['executor_id'] != OWNER_ID
r = next(r for r in t['revisions'] if r['spec']['id'] == t['current_revision'])
assert r['spec']['operations'] == ['test'] and r['hash'] == digest(r['spec'])
print(dict(mode='approve', task=t['id'], spec_hash=r['hash'],
           operations_hash=digest(r['spec']['operations']),
           executor_id=t['executor_id'], sequence=len(t['history'])))
print(approval_title(t, r, len(t['history'])))
```

Для новой задачи без дополнительных переходов sequence=2, но брать фактическую длину
истории, не константу. Записать main SHA непосредственно до/после dispatch, run ID,
run.head_sha и точные поля. Проверить, что task из run.head_sha совпадает с редакцией,
которую владелец прочитал. Если main сдвинулся или входы противоречат spec — не считать
run разрешением, STOP и разбор; никаких автоматических повторов.

Успех job сам по себе недостаточен. Требуются event=workflow_dispatch, branch=main,
repo/head_repo=Arch-relic/AgroNaliz, owner actor и triggering_actor ID240867647,
attempt=1, completed/success, правильные workflow path/ID и точный display_title.
Run — свидетельство конкретного task/spec/operations/executor/sequence, не право
исполнить произвольный test command или создать изменения приложения.

## P3: фиксация и read-only проверка свидетельства

После отдельного разрешения подготовить metadata PR: append-only awaiting→approved,
approval_run=реальный ID проверенного owner run. Не редактировать прежний spec,
историю/решения/время; не добавлять implementing. Trusted PR checker должен независимо
прочитать run и task по run.head_sha, проверить область и точный префикс истории.
Сохранить PR/head/base SHA и результат trusted check, не только unit job.
Объединение metadata PR — отдельное действие владельца, не агента.

После такого merge read-only валидация актуального JSON через доверенный core/API
подтверждает свидетельство, не запускает исполнителя. Не передавать PR callback
в качестве trusted API. Не использовать production tokens; проверенному оператору
достаточны существующие read scopes contents/actions/pull-requests, новые права не выдавать.

## P4: отрицательная матрица и завершение после отдельных разрешений

| Случай | Безопасное доказательство | Ожидаемый результат |
| --- | --- | --- |
| Параметр spec_hash/operations_hash/executor/sequence неверен | Отдельно согласованный отрицательный owner dispatch, меняется один input | failure, DENIED, никакого grant |
| Другая ветка | Только разрешённый отрицательный запуск; не редактировать workflow ветки ради теста | отказ, не evidence |
| mode=session_start | Только после разрешения; отдельная проверка контракта анализа | может быть success, но никогда не approval evidence |
| Не-owner / self approval | Offline tests; live только если уже есть допустимая identity и отдельное разрешение | отказ; новых прав/аккаунтов не создавать |
| Rerun approval | Offline tests; live rerun только с отдельным разрешением | отказ по attempt; не использовать run как grant |
| Поддельный run ID/старый grant/другой task или revision | Отдельно согласованный negative metadata PR, реальные read-only API checks | trusted check failure; PR не merge |
| Попытка переписать историю/редакцию или удалить задачу | Offline tests, либо разрешённый negative metadata PR | trusted check failure; историю сохранить |
| Новая функциональная редакция | Новый append-only spec/hash и awaiting transition, только после разрешения metadata PR | старое разрешение новую редакцию не покрывает |
| room/backup/signing/production_policy/merge_main/publish_apk | Существующие offline deny tests; саму операцию не выполнять | отказ даже при наличии approval |
| Отсутствие API/задачи, смена main SHA, stale snapshot после отзыва | Существующие воспроизводимые offline tests | Denied без fallback; не отключать сеть GitHub/защиту ради live теста |

Failing negative PR/job — ожидаемое доказательство отказа, не ошибка, которую нужно
«исправить» ослаблением gate. Такие PR не merge; закрыть только с разрешения владельца.
Не производить серию реальных approval dispatch без отдельного согласования матрицы.

Для завершения после разрешения сделать отдельный metadata PR approved→cancelled
с причиной «синтетическая проверка завершена». Не удалять task, revisions, decisions,
историю и ссылку на run. Owner merge отдельно. Подтвердить cancelled в main по SHA.
Комментарий/закрытие issue/label/незамерженный PR **не отзывают** допуск.

## Что этот первый запуск не доказывает

`validate()` проверяет историю и approval, а `authorize()` — текущий допуск перед
действием. Успех validate для cancelled означает корректность истории, не разрешение.
Нельзя выдавать read-only валидацию за live authorize или доказательство остановки.

У текущего workflow нет executor entrypoint: owner-only dispatch и запрет self approval
не дают положительного live authorize исполнителя в этой конфигурации. Не подделывать
GITHUB_* environment, actor ID или API callback, чтобы получить зелёный live тест.
Положительный live authorize и end-to-end отзыв требуют отдельного согласования
интеграции/доверенного execution context; этот PR их не добавляет. Offline 69 tests
остаются отдельным свидетельством, не заменяют live validation.

Отзыв действует после изменения task в main. Проверять перед каждым действием;
уже запущенную операцию этот протокол немедленно не останавливает. Два SHA-чтения
не исключают изменения с возвратом к прежнему SHA; нет обязательных checks/защиты main.
Эти ограничения записать в итог, а не объявлять Agent Core полностью готовым.

## Форма будущего отчёта владельцу

Для каждого случая: разрешение на конкретный тест, ожидаемый результат, task ID,
revision/spec hash/operations hash/executor ID, main/head/base/immutable task SHA,
workflow path/ID, run ID/attempt/event/actors/conclusion/title, PR/check URL,
фактический отказ/успех и причина; дата UTC, просмотренные logs.
Писать статусы `не запускался`, `успешно`, `ожидаемый отказ`, `заблокирован` раздельно.
Не публиковать токены/ключи, event payload с частными данными или реальные backup.
Технические факты и незавершённое — дописать в `docs/codex-handoff.md`.

**Текущая точка остановки: review этого Draft PR.** Для перехода к регистрации
нужны решение владельца о test issue и executor identity и явное разрешение P1.
Для P2–P4 нужны отдельные разрешения на live dispatch/metadata transitions.
Merge, защита main, permissions, APK, production secrets, Room/backup/signing,
смартфон, автономный Codex и AI recommendations в подготовительный этап не входят.
