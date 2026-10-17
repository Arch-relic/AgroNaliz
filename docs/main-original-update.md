# MAIN1.0.8 / Room23: защищённый штатный выпуск

После отдельного разрешения и применения этого патча сохраняются main/owner/attempt1,
оригинальный ANDROID_* signer, pin, package/code/cleanup. Для первого Room23 выпуска
разрешена только установленная Room22, floor>=3000003 и успешный owner/main manual
audit main-room23-checks.yml (workflow_id387641531) точно публикуемого SHA, включая
успешные шаги миграции, целостности, отдельного restore и restart. TEST пакет
ru.fo6osik.workjournal.debugtest отвергается до декодирования ключа даже при общем cert.

Одно лишь слияние PR с файлом-предложением не разрешает Room23. После применения
политики и отдельного merge: дождаться CI, owner запускает audit на точном main SHA,
затем Publish original main APK / main с minimum_installed_version_code=фактический
(3000003 для проверенного baseline), minimum_room_version=22 и migration_audit_run_id.
Если main продвинулся между audit и signer, новый audit обязателен. Run number
вычисляет новый code3000000+N; не фиксировать/сбрасывать его вручную. При re-run нужен
новый dispatch. verification.json содержит auditRunId/auditSourceSha/versionName,
оригинальный сертификат и SHA APK. Только временный Actions artifact, не Release.

Создать штатный MAIN JSON-backup до обновления, сохранить вне песочницы приложения
через SAF, проверить доступность файла и databaseVersion22; не использовать TEST
backup и не публиковать рабочий JSON. JSON не является полной копией preferences/files.
Не восстанавливать его поверх рабочей базы ради теста. Обновлять существующую MAIN
без удаления/очистки; при mismatch package/cert/code остановиться. Физическое устройство
и текущее состояние production Secrets подтверждаются отдельно, не синтетическим CI.

---

История прежнего MAIN-процесса:

# Безопасное обновление основного приложения

Пакет: ru.fo6osik.workjournal. Подтверждённые владельцем исходные данные:
versionCode53, Room20. Текущий main b59df2ed7be777fd69a4848111d93f86b3e3c3db
содержит уборку PR #26 и Room21, а также стабильную TEST подпись PR #28.

## Ключ и историческая проверка

Оригинальный SHA-256:
3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C

В PR #25 существующий ключ уже выдал этот сертификат, но выпуск был остановлен
из-за несовпадения с тогдашним ошибочным ожидаемым pin (3260EE вместо 32606E).
Теперь владелец явно предоставил совпадающий pin. PR #25 не меняется и не
объединяется этой задачей. Историческое совпадение не заменяет проверку новой
готовой APK; новый workflow проверяет её повторно и блокирует несовпадение.

Используются только существующие Secrets ANDROID_KEYSTORE_BASE64,
ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD. Новый
постоянный ключ не создаётся. Пустой key password использует store password,
неверный явно заданный password не подменяется.

## Защищённый выпуск

Workflow .github/workflows/main-apk-publish.yml — Publish original main APK.
Только ручной запуск владельцем из main, первая попытка. Из PR либо другого
ref секреты не доступны этому процессу. Build job без Secrets собирает main
и запускает JVM tests. Sign job без checkout/Gradle восстанавливает keystore
в temp0700/file0600, передаёт пароли через env-ссылки, проверяет apksigner,
полный pin, package и версию. До успешной проверки подписанная APK не
публикуется. Секретный alias не попадает в META-INF (ORIGINAL); stdout/stderr
SDK захватываются, публикуются только безопасные метаданные. Ключ удаляется
finally и always cleanup. Отдельный artifact transport удаляется после sign
и истекает через день; APK internal-unsigned-* устанавливать нельзя.

MAIN versionCode = 3000000 + номер нового workflow run, строго выше введённого
установленного versionCode, максимум2100000000. Файл workflow и offset нельзя
переименовывать/сбрасывать. Concurrency и история завершённых runs блокируют
запоздавшую публикацию меньшего номера; re-run jobs блокирован, используйте
новый Run workflow. Gradle mainVersionCode влияет только на debug main APK.
TEST versionCode, testVersionCode и TEST signing workflow не меняются.
Обычный debug main без свойства остаётся versionCode1 и не годится для
обновления исходной AndroidIDE APK. Основной подписанный выпуск — debug APK,
переподписанная оригинальным ключом, не minified release build.

Ввод Room обязателен: проверены установленная20→новая21 и21→21. Другие исходные
версии блокируются до отдельного аудита миграций. Destructive migration,
очистка и переустановка не используются; схема и существующие migrations
в этом PR не меняются.

## Следующий шаг владельца

До отдельного merge нового Draft PR workflow в main отсутствует. Codex не
объединяет PR. Владелец после проверки объединяет именно новый PR (не #25),
затем Actions → Publish original main APK → Run workflow → main:
minimum_installed_version_code=53, minimum_room_version=20. Если установленное
приложение уже обновлялось, укажите новые фактические числа вместо defaults.

После success скачайте artifact app-main-original-main-<SHA>-v<versionCode>,
распакуйте app-main-original.apk и verification.json. В отчёте должны быть
applicationId ru.fo6osik.workjournal, приведённый полный certificateSha256,
versionCode>53, roomVersion21 и installedRoomVersion20, SHA APK/source.
Workflow пока не запускался с настоящими Secrets; готовая original APK до
этого шага не подтверждена и здесь не обещается.

## Проверка на телефоне перед установкой

Сохраните существующую резервную копию основного приложения приватно вне Git.
JSON содержит Room, не полную копию настроек/файлов. Не публикуйте пользовательские
данные. Не удаляйте приложение, не очищайте данные, не устанавливайте TEST
или обычную debug APK вместо оригинального обновления.

С разрешённым USB debugging и Android SDK Build Tools35/Java17 запустите:

```
python3 docs/verify-main-apk-update.py --serial SERIAL \
  --apk /PRIVATE-DIRECTORY/app-main-original.apk \
  --report /PRIVATE-DIRECTORY/verification.json --installed-room-version 20
```

При необходимости задайте --adb, --aapt2 и --apksigner путями к SDK. Скрипт
только получает pm path, читает установленную base.apk через adb pull во
временную приватную копию и сравнивает её сертификат/версию с новой APK и
report, проверяет SHA APK и Room floor. Базу/данные телефона не открывает,
ничего не устанавливает/удаляет и не использует root/run-as. При отказе
нельзя продолжать установку: устраните несовпадение и повторите выпуск с
фактическими metadata. Полный сертификат установленной APK проверяется
этим шагом; из одного applicationId совместимость подписи не следует.

После READ-ONLY UPDATE CHECK PASS откройте APK на телефоне: должно предлагаться
обновление основной версии. Установите поверх неё без удаления/очистки.
После запуска проверьте записи, технику/поля, кампании/бункеры/события, таймеры,
площадь/плотность и расчёты. До проверки всех данных сохраняйте резервную копию.
Фактический пользовательский телефон в CI недоступен; бесусловной гарантии
сохранности произвольных неподтверждённых данных нет.

## Проверки CI

Существующий Android PR checks сохранён: оба APK/JVM, уборка, Room migration,
TEST signer и изоляция. Дополнительный Main APK safe update checks не получает
production Secrets. Использует только уже созданный Gradle debug fixture key
одноразового runner, без генерации нового постоянного ключа. Проверяет exact
inline signer, отрицательный pin, package/version/Room gates, неизменный TEST
versionCode1 при main override. В свежем эмуляторе устанавливается закреплённый
main Room20 код с fixture versionCode53, затем signed main3000001 и3000002.
Контролируются все прежние значения всех таблиц синтетической БД, файлы,
настройки/таймеры, сертификат, новые площадь/плотность и force-stop/restart.
Это подтверждает механизм обновления, а не текущий сертификат новой production
APK или восстановление реальных данных телефона.
