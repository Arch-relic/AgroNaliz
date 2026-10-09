package ru.fo6osik.workjournal

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HarvestReminderManager {

    const val DEFAULT_INTERVAL_MINUTES = 50
    const val DEFAULT_UNLOAD_MINUTES = 5

    /*
     * У v19 ключа разгрузки ещё не существовало.
     * Пока старый текущий таймер не перезапущен новым бункером,
     * считаем его по старой схеме (+0 разгрузки).
     */
    private const val LEGACY_UNLOAD_FALLBACK_MINUTES = 0

    /*
     * Быстрые варианты основного рабочего интервала.
     * Произвольное значение задаётся в настройках рабочего дня.
     */
    val intervalOptions =
        intArrayOf(
            30,
            40,
            50,
            60
        )

    /*
     * После первого пропущенного напоминания
     * приложение переходит в режим ожидания реакции.
     *
     * 2 минуты 30 секунд = 150 секунд.
     */
    const val WAITING_REPEAT_MILLIS =
        150_000L


    /*
     * Льготы к основному интервалу после добавления бункера.
     *
     * Продолжение работы: +0 мин
     * Обед / ужин:       +15 мин
     * Ожидание транспорта:+30 мин
     */
    const val GRACE_NONE_MINUTES =
        0

    const val GRACE_MEAL_MINUTES =
        15

    const val GRACE_WAITING_TRANSPORT_MINUTES =
        30

    const val ACTION_REMIND =
        "ru.fo6osik.workjournal.HARVEST_REMIND"

    const val ACTION_SNOOZE =
        "ru.fo6osik.workjournal.HARVEST_SNOOZE"

    const val EXTRA_DAY_ID =
        "harvest_reminder_day_id"

    const val EXTRA_SNOOZE_MINUTES =
        "harvest_reminder_snooze_minutes"

    private const val PREFS =
        "harvest_reminder_prefs"

    private fun enabledKey(
        dayId: Int
    ) =
        "enabled_$dayId"

    private fun intervalKey(
        dayId: Int
    ) =
        "interval_$dayId"

    private fun unloadKey(
        dayId: Int
    ) =
        "unload_$dayId"

    private fun unloadIncludedKey(
        dayId: Int
    ) =
        "unload_included_$dayId"

    private fun nextAtKey(
        dayId: Int
    ) =
        "next_at_$dayId"

    private fun waitingKey(
        dayId: Int
    ) =
        "waiting_$dayId"


    private fun graceKey(
        dayId: Int
    ) =
        "grace_$dayId"

    fun isEnabled(
        context: Context,
        dayId: Int
    ): Boolean {
        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getBoolean(
                enabledKey(
                    dayId
                ),
                true
            )
    }

    fun isWaitingForResponse(
        context: Context,
        dayId: Int
    ): Boolean {
        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getBoolean(
                waitingKey(
                    dayId
                ),
                false
            )
    }

    fun getIntervalMinutes(
        context: Context,
        dayId: Int
    ): Int {
        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getInt(
                intervalKey(
                    dayId
                ),
                DEFAULT_INTERVAL_MINUTES
            )
    }

    fun getUnloadMinutes(
        context: Context,
        dayId: Int
    ): Int {
        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getInt(
                unloadKey(
                    dayId
                ),
                LEGACY_UNLOAD_FALLBACK_MINUTES
            )
    }

    private fun isUnloadIncluded(
        context: Context,
        dayId: Int
    ): Boolean {
        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getBoolean(
                unloadIncludedKey(
                    dayId
                ),
                false
            )
    }

    /*
     * Синхронизирует сохранённые в БД настройки рабочего дня
     * с runtime-настройками AlarmManager.
     *
     * Важно: текущий уже запущенный таймер здесь НЕ пересчитывается.
     * Новые значения применятся при следующем новом цикле.
     */
    fun syncDaySettings(
        context: Context,
        dayId: Int,
        workIntervalMinutes: Int,
        unloadIntervalMinutes: Int
    ) {
        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putInt(
                intervalKey(
                    dayId
                ),
                workIntervalMinutes
                    .coerceAtLeast(
                        1
                    )
            )
            .putInt(
                unloadKey(
                    dayId
                ),
                unloadIntervalMinutes
                    .coerceAtLeast(
                        0
                    )
            )
            .apply()
    }

    fun getNextAt(
        context: Context,
        dayId: Int
    ): Long {
        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getLong(
                nextAtKey(
                    dayId
                ),
                0L
            )
    }


    fun getGraceMinutes(
        context: Context,
        dayId: Int
    ): Int {
        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getInt(
                graceKey(
                    dayId
                ),
                GRACE_NONE_MINUTES
            )
    }

    fun setInterval(
        context: Context,
        dayId: Int,
        minutes: Int
    ) {
        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                enabledKey(
                    dayId
                ),
                true
            )
            .putInt(
                intervalKey(
                    dayId
                ),
                minutes
            )
            .apply()

        /*
         * Изменение настройки НЕ считается ответом
         * на уже показанное уведомление.
         */
        if (
            isWaitingForResponse(
                context,
                dayId
            )
        ) {
            scheduleWaitingRepeat(
                context,
                dayId
            )
        } else {
            scheduleBaseFromNow(
                context =
                    context,
                dayId =
                    dayId,
                graceMinutes =
                    getGraceMinutes(
                        context,
                        dayId
                    ),
                includeUnload =
                    isUnloadIncluded(
                        context,
                        dayId
                    )
            )
        }
    }

    fun ensureScheduled(
        context: Context,
        dayId: Int
    ) {
        if (
            !isEnabled(
                context,
                dayId
            )
        ) {
            return
        }

        val now =
            System.currentTimeMillis()

        val nextAt =
            getNextAt(
                context,
                dayId
            )

        if (
            nextAt >
            now
        ) {
            scheduleAt(
                context =
                    context,
                dayId =
                    dayId,
                triggerAt =
                    nextAt
            )

        } else {
            if (
                isWaitingForResponse(
                    context,
                    dayId
                )
            ) {
                scheduleWaitingRepeat(
                    context,
                    dayId
                )
            } else {
                scheduleBaseFromNow(
                    context =
                        context,
                    dayId =
                        dayId,
                    includeUnload =
                        isUnloadIncluded(
                            context,
                            dayId
                        )
                )
            }
        }
    }

    /*
     * Обычный режим:
     * следующий сигнал через выбранный пользователем интервал.
     */
    fun scheduleBaseFromNow(
        context: Context,
        dayId: Int,
        graceMinutes: Int =
            GRACE_NONE_MINUTES,
        anchorAtMillis: Long =
            System.currentTimeMillis(),
        includeUnload: Boolean =
            false
    ) {
        if (
            !isEnabled(
                context,
                dayId
            )
        ) {
            return
        }

        val safeGrace =
            graceMinutes
                .coerceAtLeast(
                    0
                )

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putInt(
                graceKey(
                    dayId
                ),
                safeGrace
            )
            .putBoolean(
                unloadIncludedKey(
                    dayId
                ),
                includeUnload
            )
            .apply()

        val unloadMinutes =
            if (
                includeUnload
            ) {
                getUnloadMinutes(
                    context,
                    dayId
                )
            } else {
                0
            }

        val totalMinutes =
            getIntervalMinutes(
                context,
                dayId
            ) +
                unloadMinutes +
                safeGrace

        val triggerAt =
            anchorAtMillis +
                totalMinutes *
                60_000L

        /*
         * Обычно пользователь подтверждает вариант сразу.
         * Если окно каким-то образом оставалось открытым дольше
         * всего рассчитанного интервала, ставим сигнал практически
         * сразу, а не переносим ещё на один полный интервал.
         */
        val safeTriggerAt =
            triggerAt.coerceAtLeast(
                System.currentTimeMillis() +
                    1_000L
            )

        scheduleAt(
            context =
                context,
            dayId =
                dayId,
            triggerAt =
                safeTriggerAt
        )
    }

    /*
     * Режим ожидания реакции:
     * повтор каждые 2 минуты 30 секунд.
     */
    fun scheduleWaitingRepeat(
        context: Context,
        dayId: Int
    ) {
        if (
            !isEnabled(
                context,
                dayId
            )
        ) {
            return
        }

        setWaitingForResponse(
            context,
            dayId,
            true
        )

        scheduleFromNowMillis(
            context =
                context,
            dayId =
                dayId,
            delayMillis =
                WAITING_REPEAT_MILLIS
        )
    }

    /*
     * +5 / +10 / +15 минут.
     *
     * Это разовая отсрочка. Режим ожидания ответа
     * НЕ прекращается. После следующего сигнала
     * снова включается цикл 2:30.
     */
    fun snooze(
        context: Context,
        dayId: Int,
        minutes: Int
    ) {
        if (
            !isEnabled(
                context,
                dayId
            )
        ) {
            return
        }

        setWaitingForResponse(
            context,
            dayId,
            true
        )

        scheduleFromNowMillis(
            context =
                context,
            dayId =
                dayId,
            delayMillis =
                minutes *
                60_000L
        )
    }

    /*
     * Реальный ответ пользователя №1:
     * новый бункер сохранён.
     *
     * Цикл 2:30 прекращается, показанное уведомление
     * убирается и запускается обычный интервал.
     */
    fun resetAfterBunkerAdded(
        context: Context,
        dayId: Int,
        graceMinutes: Int =
            GRACE_NONE_MINUTES,
        bunkerAddedAtMillis: Long =
            System.currentTimeMillis(),
        includeUnload: Boolean =
            true
    ) {
        if (
            !isEnabled(
                context,
                dayId
            )
        ) {
            return
        }

        setWaitingForResponse(
            context,
            dayId,
            false
        )

        HarvestReminderReceiver
            .cancelNotification(
                context,
                dayId
            )

        scheduleBaseFromNow(
            context =
                context,
            dayId =
                dayId,
            graceMinutes =
                graceMinutes,
            anchorAtMillis =
                bunkerAddedAtMillis,
            includeUnload =
                includeUnload
        )
    }

    fun markWaitingForResponse(
        context: Context,
        dayId: Int
    ) {
        setWaitingForResponse(
            context,
            dayId,
            true
        )
    }

    fun disable(
        context: Context,
        dayId: Int
    ) {
        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                enabledKey(
                    dayId
                ),
                false
            )
            .putBoolean(
                waitingKey(
                    dayId
                ),
                false
            )
            .remove(
                nextAtKey(
                    dayId
                )
            )
            .remove(
                graceKey(
                    dayId
                )
            )
            .remove(
                unloadIncludedKey(
                    dayId
                )
            )
            .apply()

        cancelAlarm(
            context,
            dayId
        )

        HarvestReminderReceiver
            .cancelNotification(
                context,
                dayId
            )
    }

    fun enable(
        context: Context,
        dayId: Int
    ) {
        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                enabledKey(
                    dayId
                ),
                true
            )
            .putBoolean(
                waitingKey(
                    dayId
                ),
                false
            )
            .putInt(
                graceKey(
                    dayId
                ),
                GRACE_NONE_MINUTES
            )
            .putBoolean(
                unloadIncludedKey(
                    dayId
                ),
                false
            )
            .apply()

        scheduleBaseFromNow(
            context =
                context,
            dayId =
                dayId,
            graceMinutes =
                GRACE_NONE_MINUTES
        )
    }

    /*
     * Реальный ответ пользователя №2:
     * рабочий день больше не ACTIVE.
     */
    fun cancelForFinishedDay(
        context: Context,
        dayId: Int
    ) {
        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                waitingKey(
                    dayId
                ),
                false
            )
            .remove(
                nextAtKey(
                    dayId
                )
            )
            .remove(
                graceKey(
                    dayId
                )
            )
            .remove(
                unloadIncludedKey(
                    dayId
                )
            )
            .apply()

        cancelAlarm(
            context,
            dayId
        )

        HarvestReminderReceiver
            .cancelNotification(
                context,
                dayId
            )
    }

    fun canUseExactAlarms(
        context: Context
    ): Boolean {
        if (
            android.os.Build.VERSION.SDK_INT <
            android.os.Build.VERSION_CODES.S
        ) {
            return true
        }

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        return alarmManager
            .canScheduleExactAlarms()
    }

    fun nextReminderText(
        context: Context,
        dayId: Int
    ): String {
        if (
            !isEnabled(
                context,
                dayId
            )
        ) {
            return "Напоминание выключено"
        }

        val nextAt =
            getNextAt(
                context,
                dayId
            )

        val mode =
            if (
                canUseExactAlarms(
                    context
                )
            ) {
                "точное"
            } else {
                "обычное"
            }

        if (
            nextAt <=
            0L
        ) {
            return if (
                isWaitingForResponse(
                    context,
                    dayId
                )
            ) {
                "Ожидание записи • повтор каждые 2:30 • $mode"
            } else {
                val workInterval =
                    getIntervalMinutes(
                        context,
                        dayId
                    )

                val unloadInterval =
                    if (
                        isUnloadIncluded(
                            context,
                            dayId
                        )
                    ) {
                        getUnloadMinutes(
                            context,
                            dayId
                        )
                    } else {
                        0
                    }

                val grace =
                    getGraceMinutes(
                        context,
                        dayId
                    )

                val totalInterval =
                    workInterval +
                        unloadInterval +
                        grace

                val intervalText =
                    formatIntervalText(
                        baseInterval =
                            workInterval,
                        unloadInterval =
                            unloadInterval,
                        graceMinutes =
                            grace,
                        totalInterval =
                            totalInterval
                    )

                "Напоминание: $intervalText • $mode"
            }
        }

        val time =
            SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(
                Date(
                    nextAt
                )
            )

        return if (
            isWaitingForResponse(
                context,
                dayId
            )
        ) {
            "Ожидание записи • повтор $time • каждые 2:30 • $mode"
        } else {
            val baseInterval =
                getIntervalMinutes(
                    context,
                    dayId
                )

            val unloadInterval =
                if (
                    isUnloadIncluded(
                        context,
                        dayId
                    )
                ) {
                    getUnloadMinutes(
                        context,
                        dayId
                    )
                } else {
                    0
                }

            val grace =
                getGraceMinutes(
                    context,
                    dayId
                )

            val totalInterval =
                baseInterval +
                    unloadInterval +
                    grace

            val intervalText =
                formatIntervalText(
                    baseInterval =
                        baseInterval,
                    unloadInterval =
                        unloadInterval,
                    graceMinutes =
                        grace,
                    totalInterval =
                        totalInterval
                )

            "Напоминание: $intervalText • следующее $time • $mode"
        }
    }

    private fun formatIntervalText(
        baseInterval: Int,
        unloadInterval: Int,
        graceMinutes: Int,
        totalInterval: Int
    ): String {
        return when {
            unloadInterval > 0 &&
                graceMinutes > 0 ->
                "$baseInterval + $unloadInterval + $graceMinutes = " +
                    "$totalInterval мин"

            unloadInterval > 0 ->
                "$baseInterval + $unloadInterval = " +
                    "$totalInterval мин"

            graceMinutes > 0 ->
                "$baseInterval + $graceMinutes = " +
                    "$totalInterval мин"

            else ->
                "$baseInterval мин"
        }
    }

    private fun setWaitingForResponse(
        context: Context,
        dayId: Int,
        waiting: Boolean
    ) {
        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                waitingKey(
                    dayId
                ),
                waiting
            )
            .apply()
    }

    private fun scheduleFromNowMillis(
        context: Context,
        dayId: Int,
        delayMillis: Long
    ) {
        val triggerAt =
            System.currentTimeMillis() +
                delayMillis

        scheduleAt(
            context =
                context,
            dayId =
                dayId,
            triggerAt =
                triggerAt
        )
    }

    private fun scheduleAt(
        context: Context,
        dayId: Int,
        triggerAt: Long
    ) {
        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val pendingIntent =
            reminderPendingIntent(
                context,
                dayId
            )

        alarmManager.cancel(
            pendingIntent
        )

        when {
            android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.S &&
                alarmManager.canScheduleExactAlarms() -> {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }

            android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.M &&
                android.os.Build.VERSION.SDK_INT <
                android.os.Build.VERSION_CODES.S -> {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }

            android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.M -> {

                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }

            else -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putLong(
                nextAtKey(
                    dayId
                ),
                triggerAt
            )
            .apply()
    }

    private fun cancelAlarm(
        context: Context,
        dayId: Int
    ) {
        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        alarmManager.cancel(
            reminderPendingIntent(
                context,
                dayId
            )
        )
    }

    private fun reminderPendingIntent(
        context: Context,
        dayId: Int
    ): PendingIntent {
        val intent =
            Intent(
                context,
                HarvestReminderReceiver::class.java
            ).apply {
                action =
                    ACTION_REMIND

                putExtra(
                    EXTRA_DAY_ID,
                    dayId
                )
            }

        return PendingIntent.getBroadcast(
            context,
            100_000 +
                dayId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
    }
}
