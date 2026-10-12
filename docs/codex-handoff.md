# Передача контекста: оригинальная подпись AndroidIDE

Дата: 2026-10-12. Цель: APK для обновления основной версии без удаления данных.
Статус: Draft PR #25 открыт; сборка и проверки прошли, original signing требует корректного pin.

Ветка GitHub: feature/original-key-signing. Опорный main:
c0ea3d01a97dd0eb4814b59307b44169916821bc (Room 20).
Проверенный GitHub SHA: 5512aa5574a9b33ae13470da636218ec8eb0010d.
Исходники опубликованы в отдельной ветке; main не изменён.
Ветка harvest-yield / PR #23 не включена; остальные работы сохранены.

## Изменения и файлы

Изменены:
- app/build.gradle.kts — публичный ciVersionCode, default=1, без signing secrets.
- .github/workflows/android-pr-checks.yml — сохранены debug/TEST/JVM/catalog/изоляция,
  добавлено изолированное подписание только в доверенных контекстах.
- docs/codex-handoff.md — этот отчёт.
Созданы:
- docs/test_original_apk_signing.py — 16 проверок точного inline-кода workflow.
- docs/original-apk-signing.md — архитектура доверия, ключа и установки.
Удалённых файлов нет. Room/миграции/данные/функциональность/PDF не меняются.

Secrets доступны только отдельному заданию, без checkout/Gradle/app-кода.
Оригинальный ключ временный 0700/0600, пароли env-ссылками; stderr подавлен,
алиас не попадает в имена v1 META-INF. Сертификат/ID/versionCode проверяются,
при несовпадении нет signed artifact, временные файлы удаляются.

Владелец уточнил первоначальный неполный SHA (62 символа) и промежуточную
строку 65 символов: корректные 32 пары байтов дают pin
3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B903260EECBEA04BBDB15EE3C.
Он закреплён независимо от фактического сертификата signing Secrets.

## Проверки

python3 -m unittest discover -s docs -p 'test_original_apk_signing.py' -v:
16 тестов, новый запуск, 0 ошибок/пропусков. В том числе настоящий SDK,
временный синтетический keytool ключ, переподписание APK и отказ при
неверном pin с проверкой удаления ключа/отсутствия выходной APK.
Это проверка signing pipeline, не сборка текущей Android-версии.
YAML разобран, git diff --check успешен.

source /workspace/agronaliz-env.sh; agrogradle :app:assembleDebug :app:test:
Gradle не стартовал: Could not determine a usable wildcard IP for this machine.
Облачная сборка выполнена в GitHub Actions #50 и #51, android job успешен.

## Риски и ручные действия

Подписанная APK остаётся debug-сборкой с первоначальной подписью.
Для обновления требуется одинаковый ID/сертификат и versionCode не ниже
установленного. CI берёт номер запуска; для более высокого старого номера
использовать workflow_dispatch/main input version_code. Локальная default=1.
Никакого удаления/очистки основного приложения не выполнять.

Подробности и установка: docs/original-apk-signing.md. Ключи, пароли, алиас и
персональные сертификатные сведения в отчёт/артефакты не включаются.
PR #25: https://github.com/Arch-relic/AgroNaliz/pull/25 — Draft, открыт в main, не объединён.
Незавершённое: подтверждение правильного публичного pin владельцем,
успешный строгий контроль оригинального сертификата и публикация signed APK.
Draft уже создан; объединение не выполнялось.

## Проверка Actions #50

https://github.com/Arch-relic/AgroNaliz/actions/runs/38231417018
Коммит 1e7b8529d6ef045b423f0cfe2eb1fe262d78f860: debug/TEST сборки, JVM,
16 новых Python signing checks, 13 catalog checks и изоляция main/TEST успешны.
Original signing не опубликовал APK: ANDROID_KEY_PASSWORD недоступен, хотя
три других Secrets прошли проверку keystore. Добавлена стандартная fallback
на пароль keystore только при отсутствии отдельного пароля ключа;
явно неверный заданный пароль не заменяется, SDK обязан подтвердить расшифровку.
Тест SDK дополнительно проверяет эти два сценария, 16 тестов без ошибок.

Публичный сертификат из реально настроенного keystore:
3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C.
Он отличается от последнего присланного pin одним символом (3260EE / 32606E).
Уточнение правильной идентичности установленного приложения запрошено;
pin автоматически не заменён, signed artifact при несовпадении запрещён.
Не включать private signing outputs или Secrets в отчёты/артефакты.

## Проверка Actions #51 — итог перед уточнением сертификата

https://github.com/Arch-relic/AgroNaliz/actions/runs/38232835514
Исходный коммит 5512aa5574a9b33ae13470da636218ec8eb0010d; versionCode 51.
Android job 114756225943: успешны сборки debug и debugTest, обе JVM test tasks,
16 Python signing checks, 13 catalog checks, проверка ID/provider и Android-изоляции.
Original-signature job 114758489397: реальное подписание выполнено;
apksigner обнаружил сертификат 3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C.
Он НЕ совпал с закреплённым пользовательским pin (32606E вместо 3260EE),
поэтому job завершился ошибкой, upload signed APK пропущен, cleanup успешен.
Это проверенный отказ production pipeline при несовпадении, а не успешная публикация.
Артефакты запуска: обычные debug/TEST и diagnostics; оригинально подписанного нет.
Обычный debug НЕ использовать для обновления установленной оригинальной версии.
Нужно подтвердить публичный отпечаток установленного приложения; автоматическая
замена pin по настроенному keystore недопустима. Телефон/обновление данных не проверялись.
