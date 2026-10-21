# Agent Identity v2 — варианты решений и поэтапное внедрение

Статус: рекомендации для review по Issue #52. Принято владельцем только направление
App installation + Actions controller, сначала read-only. Выборы D01–D12 не являются
разрешением provisioning/реализации и требуют явного утверждения вместе с редакцией ТЗ.
Ни один вопрос ниже не оставлен без вариантов и рекомендуемого ответа.

## Решения для утверждения

| ID | Варианты | Рекомендуемый выбор и обоснование | Что должен подтвердить владелец |
| --- | --- | --- | --- |
| D01 Controller | On-demand GitHub-hosted Actions; внешний token broker с OIDC; постоянный server/self-hosted runner | Actions без daemon: соответствует текущему orchestrator, меньше инфраструктуры/долгоживущих процессов. Внешний broker только если потребуется внешняя граница credentials | Отдельный trusted controller workflow и запрет untrusted code; внешний server не входит |
| D02 Key store | Protected Actions environment secret; внешний KMS/secret manager с OIDC; repo-wide secret | Для первого read-only proof protected environment, main-only deployment и ручной owner review key release. Нет нового облачного сервиса. Repo-wide secret без такого барьера отклонить; KMS выбрать при недоступности достаточных env controls | Имя/доступность environment, кто может менять secrets/approvals, source SHA перед release. App key только будущий отдельный provisioning |
| D03 Registry | Owner-reviewed registry в main с digest; только environment variables; PR-supplied config | Registry version/hash в main плюс owner-approved controller/source digest при key release. Main даёт историю, env gate препятствует автоматической выдаче ключа любому коду main. PR config не authority | Конкретные App/install/account/repo IDs после создания, approved hashes. При unprotected main не заявлять независимую защиту от admin/write actor |
| D04 V1 compatibility | Перезаписать executor_id; in-place migration; отдельные v2 tasks с predecessor link | Новые v2 tasks и новое consent. Исторические v1 bytes/hashes/grants не меняются; меньше неявной миграции полномочий | V1 остаётся историческим/user протоколом, App controller его не исполняет; отмена старой задачи отдельно |
| D05 Consent binding | Комментарий/label; прежний v1 title с App ID; context-hash protocol v2 | Immutable request на main, полный typed subject/policy/scope/history/expiry/nonce в context_hash, owner run evidence. Старый протокол семантически иной | Поля R05, version2 title и запрет автоматического переноса v1 grants |
| D06 Consent lifetime | До отмены;24часа;8 часов | Для первого synthetic read-only proof максимум 8 часов, not_after задан в request до owner review; ограничивает окно старого согласования и укладывается в рабочий день. Нужен новый request/approval при expiry | 8 часов — проектное предложение, не GitHub token TTL и не уже действующая policy |
| D07 Session/token | Общий token на несколько tasks; token worker; один controller run/task/request | Один run/task/request, session timeout 10 минут, token не выходит из controller, explicit downscope/revoke. GitHub token может жить 1 час, session короче за счёт контроллера, не выдуманного API параметра TTL | Лимит10 минут и новая полная проверка перед каждым read; никакого automatic consent renewal |
| D08 Revocation | Только token expiry; только task cancellation; оба слоя | Task отмена в main + revoke/suspend/stop mint при инциденте. Сроки/история разделены, никто не обещает in-flight stop | Кто выполняет owner incident actions; App не получает Administration для самоотзыва |
| D09 Audit | Только worker artifact; GitHub evidence+handoff; отдельный audit server | GitHub API evidence и append-only ссылки/сводки в tasks/handoff, минимально 90 дней безопасной диагностики по будущей retention настройке. Нет нового сервера. Archive не заменяет live grant verification | 90 дней/платформенные пределы/стоимость: если недоступно, явно согласовать меньший срок до live. Удаление run => deny |
| D10 Future writes | Contents:write на целевой repo; изолированный fork/publisher; остаться read-only | Сейчас read-only. Будущий write-проект сначала исследует отдельный publisher/изолированную write-среду без raw creds у worker. Broad contentswrite нельзя назвать feature-only и нельзя отделить от release API одним scope | До любых write — отдельное ТЗ/security review и реальные protection/required checks. App без bypass; текущий PR ничего не меняет |
| D11 Live proof | Сразу AgroNaliz; synthetic fixture без реальной installation; изолированный test repo затем AgroNaliz | Сначала отдельно разрешённый изолированный репозиторий с той же политикой, затем новый owner-reviewed AgroNaliz binding/read-only proof. Это снижает риск случайных mutations при негативных tests; fake ID не заменяет identity | Дополнительный test repo/installation также требует явного разрешения; если не разрешён, не создавать и live оставить blocked |
| D12 Execution triggers | Авто workflow_run/PR artifacts; owner manual dispatch; schedule | Первый controller только manual owner/main/attempt1. Actor остаётся human initiator, executor — проверенная App. Автоматические triggers добавляют другую trust boundary, не нужны для proof | Отдельный controller trigger и никакого запуска вследствие непроверенного worker success |

### Единственная человеческая identity

Owner и App — разные роли, но независимых двух human reviewers нет. Требование
environment «Prevent self-review» при owner-initiated run может заблокировать
единственного владельца. Рекомендация D02: явно разрешить owner review собственного
запуска только для этого read-only controller; запрет самосогласования App остаётся.
Не выдавать это за four-eyes control и не создавать фиктивного второго reviewer.
Если политика/тариф не допускает требуемый вариант — STOP и выбор KMS/другого
согласованного key store, а не silent fallback на открытый secret.
KMS/OIDC означает отдельное согласование провайдера, trust policy и потенциального
job permission id-token:write; оно не входит в предлагаемый минимальный начальный
профиль и не выдаётся этим ТЗ автоматически. Сам KMS не заменяет owner approval.

### Параметры, которые сейчас нельзя правдиво заполнить

App/install IDs, fingerprint будущего ключа, конкретное имя environment/test repo,
approved v2 workflow ID/SHA ещё не существуют. Проект предлагает, **как** их выбрать:
владелец создаёт/разрешает ресурсы только после review G1/G3, IDs независимо читаются
через GitHub API и закрепляются в registry; ключевой fingerprint записывается без PEM.
До этого live action blocked. Эти значения нельзя заменить123, именем bot или
владельцем. Это условия отдельного provisioning, а не вопросы без рекомендации.

Если владелец отвергает любую рекомендацию, переработать соответствующие R/A/T до
реализации. Существенная новая архитектура требует нового review, не выбора агентом.

## План внедрения и отдельные разрешения

| Gate | Артефакт/действие | Условие входа | Условие выхода и остановка |
| --- | --- | --- | --- |
| G0 Сейчас | Docs-only ветка/Draft PR, ТЗ+threats+matrix+решения+handoff | Прямой запрос пользователя, Issue #52 | Review владельца; не merge автоматически; никакие live этапы не начаты |
| G1 Design approval | Owner утверждает редакцию ТЗ, D01–D12, scope и ограничения | G0 review | Только после явного утверждения можно запросить отдельное задание на offline реализацию; merge docs не provisioning consent |
| G2 Offline implementation | Новый feature/Draft PR: schema/verifier/controller tests, v1 compatibility, заглушенная credential boundary | Отдельно утверждённое задание и архитектура | O/N acceptance с source audit; owner review/merge отдельно. Никаких реальных App/keys/tokens |
| G3 Provisioning | Создание App, selected-repo read-only install, approved key store, реальные IDs/secret placement | Отдельное разрешение владельца на каждый ресурс/credentials; проверенные env controls | Проверка identity/scope и registry, source policy; никакой task approval автоматически |
| G4 Live synthetic approval | Реальная v2 test issue/task/request, owner/main consent и доказательство | Отдельно утверждённые identity, issue, spec, operation/expiry, metadata PR и owner merge | Проверенные owner run/context hashes; отдельно разрешить controller test, grant не исполняет код сам |
| G5 Live read-only execution/revocation | Конкретный matrix subset, trusted controller, identity smoke, отмена/revoke | Отдельное разрешение на live runs/negative cases и metadata transitions | Реальные evidence на точном SHA, cancelled task в main через owner merge, token revoke; остальные A rows not_run/blocked |
| G6 Future write | Отдельный проект, protected main/required checks и независимый publisher security review | Новое ТЗ и явное разрешение; доказаны platform границы прав | До выполнения всех условий write запрещён, успешный G5 не открывает G6 |

Каждая future metadata branch/PR/merge и registry/secret/workflow modification должна
попадать в согласованный объём соответствующего задания. Нельзя считать label,
произвольный comment, успешный CI или установку App универсальным разрешением.
Owner decision о разработке ТЗ не становится task approval в машинном протоколе.

## Связь с PR #51

PR #51 остаётся offline планом, его файлы не переносятся и не активируются этим PR.
P1–P4, которым нужна независимая identity, блокированы до отдельно согласованных
G2–G4. Не использовать executor_id=123 и не подставлять App ID в v1 schema. После
готовности v2 предложить отдельное согласованное уточнение bootstrap plan и новую
synthetic v2 task; не переписывать старый evidence как будто live уже состоялся.

## Отчётность и завершение текущего задания

Для текущего Draft PR указать фактический main SHA, ветку, checked SHA, список пяти
Markdown paths, diff/stat, проверки ссылок/Markdown/полноты/scope, отсутствие секретов
и активных изменений. Если GitHub сам запустил старые CI, отличать их от live
dispatch и будущей acceptance matrix; ничего вручную не dispatch/re-run ради docs.
Сохранить технический handoff, оставить Draft и остановиться для review владельца.
