# Безопасная тестовая сборка «АгроНализ TEST»

Обычные debug/release сохраняют applicationId ru.fo6osik.workjournal.
Новый build type debugTest создаёт ru.fo6osik.workjournal.debugtest, название
«АгроНализ TEST». Namespace исходного Kotlin-кода остаётся прежним.

## Сборка и скачивание

- Основная Debug: ./gradlew :app:assembleDebug
- TEST: ./gradlew :app:assembleDebugTest
- Unit tests: ./gradlew :app:testDebugUnitTest :app:testDebugTestUnitTest
- Instrumented APK основной версии: ./gradlew :app:assembleDebugAndroidTest
- Instrumented APK TEST: ./gradlew -PtestBuildType=debugTest :app:assembleDebugTestAndroidTest

В Actions → Android PR checks → успешный запуск → Artifacts скачать
app-TEST-<ветка>-<короткий SHA>. ZIP содержит app-debugTest.apk.
Артефакты основной APK и TEST публикуются отдельно и хранятся 14 дней.
Существующие PR, push/main и ручной выбор ветки сохранены.
Перед публикацией CI проверяет обе APK и совместную установку на эмуляторе.

В браузере телефона войти в GitHub, скачать и распаковать ZIP, открыть
app-debugTest.apk и разрешить установку выбранному файловому менеджеру.
Не удалять основное приложение. После установки рядом появится «АгроНализ TEST».
Если Android предлагает обновить основное приложение, отменить установку:
выбран неверный артефакт. При конфликте подписи обновления TEST не удалять
основную версию; debug-ключ TEST может отличаться между CI runners.

## Изоляция

Разные applicationId означают разные Android UID и песочницы: Room-файл
agrouchet_database, SharedPreferences, filesDir, cacheDir и noBackupFilesDir
не разделяются между приложениями. Documents остаются в собственном
noBackupFilesDir/documents. App-scoped external files также разделены по пакету.
Схема Room (версия 20) и её миграции не меняются; данные не копируются.
Приложение не использует общие жёстко заданные пути /sdcard или /storage,
общий UID или контекст другого пакета для хранения.

В TEST кнопка создания резервной копии пишет в private filesDir/test-backups,
не открывая диалог выбора общего файла. Имена содержат TEST и случайный UUID.
Экспорт менеджера дополнительно отвергает внешние SAF/file URI в TEST, даже
если пользователь предоставил доступ. TEST FileProvider имеет authority
ru.fo6osik.workjournal.debugtest.testbackups, exported=false и открывает только
test-backups, не корень файлов или внешнего хранилища. Основной вариант сохраняет
прежний SAF-экспорт; тестовый provider в его APK отсутствует.
Облачный Auto Backup и device transfer TEST отключены/исключены manifest-правилами.

TEST-копии остаются внутри приложения. На тестовом устройстве с ADB их можно
просмотреть командой adb shell run-as ru.fo6osik.workjournal.debugtest ls files/test-backups
и сохранить выбранную копию через adb exec-out run-as ... cat files/test-backups/<имя>.
Для проверки использовать синтетические данные. Удаление TEST удаляет его
приватные копии, не основную базу или рабочие копии основной версии.

Импорт и восстановление читают лишь явно выбранный SAF-файл и меняют базу
текущего приложения. Автоматического переноса основной базы/настроек нет.
Файлы, которые пользователь самостоятельно отдаст другой программе или
скопирует во внешнее хранилище, уже находятся вне этой песочницы.

## Проверка CI и ручная проверка

CI запускает unit tests обоих вариантов, проверяет applicationId/название APK,
merged authorities, отсутствие sharedUserId и TEST backup-правила. Затем на
одноразовом API 35 эмуляторе устанавливает обе APK, пишет разные синтетические
строки Room/настройки/файлы/кеш/документы/external-files, проверяет разные UID,
отказ доступа TEST к основной песочнице, ограничение FileProvider и экспорта,
проверяет сохранность основных данных до и после удаления TEST.
Production пользовательские данные при этих проверках недоступны.
Скрипт проверки отказывается работать на физическом устройстве или эмуляторе
с уже установленным main/TEST; он не обновляет существующие пакеты.

На реальном телефоне проверить две иконки, независимые записи и настройки,
создание TEST-копии, перезапуск, а затем удаление TEST с сохранностью основной
версии. Эмуляторная проверка не заменяет проверку особенностей конкретного OEM.
PDF не возвращаются в assets; облачные интеграции в этом этапе не меняются.
