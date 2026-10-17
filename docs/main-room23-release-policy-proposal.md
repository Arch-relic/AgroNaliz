# Предложение политики MAIN Room23 — НЕ ПРИМЕНЕНО

17 октября 2026. Это документ для отдельного решения, не разрешение подписывать APK.
Исторические MAIN/TEST имеют один сертификат SHA256
3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C,
но разные applicationId ru.fo6osik.workjournal и ru.fo6osik.workjournal.debugtest.
Сохранить оригинальную MAIN подпись через ANDROID_* Secrets; совпадение с TEST
не требует смены сертификата и не блокирует обновление. TEST_* для MAIN не использовать.

## Текущие блокирующие условия

main-apk-publish.yml build/sign: только refs/heads/main, repository_owner,
run_attempt==1. Нет автоматического push/PR signing. room!=22 блокируется
`New Room version needs a separate verified migration audit`. installed_room
разрешает20/21/22; запрещает downgrade. versionCode=3000000+номер нового run,
строго больше указанного установленного code, с проверкой порядка прошлых runs.
Отдельный sign job без checkout/Gradle, публичный pin, проверка package/code,
временный ключ0600, cleanup и транспортный artifact остаются обязательными.

## Точный перечень предлагаемых будущих изменений

Только после успешного нового MAIN22→23 audit и ОТДЕЛЬНОГО разрешения:
1. В main-apk-publish.yml сохранить существующую ветку room22 и все её guards.
   Добавить явную ветку targetRoom23 с installedRoom==22 для первого выпуска1.0.8;
   прочие новые пары схем отклонять. Не заменять проверку на room>=22.
2. В Room23 ветке до выдачи ключа потребовать успешный MAIN Room22 to23 migration
   audit именно для публикуемого source SHA, проверить status/conclusion и matching
   head_sha через GitHub API. Проверять также стабильный workflow_id/путь (сейчас387641531/main-room23-checks.yml),
   head_repository==свой репозиторий, head_branch==main, event==workflow_dispatch
   и actor==repository_owner. Чужой/fork run или совпавшее название недостаточны.
   При отсутствии/ошибке/неоднозначности — fail closed.
   Это новый положительный guard, не отключение прежних проверок.
3. Проверять expected versionName1.0.8/package ru.fo6osik.workjournal/room23 и
   versionCode>фактического floor (последний опубликованныйMAIN3000003). Не менять
   offset3000000, workflow identity, concurrency, run_attempt, историю выпусков.
4. В verification.json сохранить существующие certificate/apkSha256/sourceSha/
   installedVersionCode/installedRoomVersion, добавить auditRunId/sourceSha/Room23
   evidence. Подписанная APK должна повторно пройти apksigner и aapt2 после sign.
5. В docs/test_main_apk_release.py и docs/test_app_version.py заменить безусловный
   Room23 отказ только на узко проверенную позитивную ветку + негативные тесты:
   неверный audit SHA/status, неверные installedRoom/package/version/cert, API failure,
   меньший/equal code, rerun/неowner/неmain. Все старые негативные проверки сохранить.
6. Обновить docs/main-original-update.md и release-инструкцию: подтверждённая пара22→23,
   новые требуемые evidence, фактические значения installed metadata.

Не применять branch exception/PR signing для доставки ключа feature-ветке.
Штатный будущий путь: отдельное разрешённое владельцем слияние в main (сейчас
запрещено), ручной audit на точном main SHA через workflow_dispatch, затем прежний
owner-only main signer с новым разрешённым Room23 preflight. До такого решения
выпуск блокирован. Для pre-merge подписанной MAIN потребовалось бы отдельное,
самостоятельно рассмотренное изменение доверенной инфраструктуры, не входит сюда.

## Риски и контроль

Простая замена room22 на23 обходит целевой audit — недопустимо. Проверять status
чужого SHA/лишь название workflow недостаточно. Re-run не должен обходить sequence.
CI использует одноразовый debug certificate и не доказывает свежее состояние
production Secrets; финальный signer обязан проверить оригинальный полный pin.
APK не следует публиковать при любом mismatch. Исторические разные applicationId
с одним certificate сохраняются; смена key нарушит совместимость старой MAIN.
Фактическая текущая MAIN на телефоне может отличаться от последнего артефакта:
до установки сверяются публичные package/certificate/code/Room, без передачи ключей.

Никакая часть этого предложения не применена к защищённому signer в этой задаче.
