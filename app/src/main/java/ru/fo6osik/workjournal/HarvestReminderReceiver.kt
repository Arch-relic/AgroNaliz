package ru.fo6osik.workjournal

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class HarvestReminderReceiver :
    BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID =
            "harvest_recording_reminders_v3"

        private const val CHANNEL_NAME =
            "Контроль записи уборки"

        private fun notificationId(
            dayId: Int
        ) =
            50_000 +
                dayId

        fun ensureNotificationChannel(
            context: Context
        ) {
            val manager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {
                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager
                            .IMPORTANCE_HIGH
                    ).apply {
                        description =
                            "Контроль пропущенной записи бункера"

                        lockscreenVisibility =
                            Notification.VISIBILITY_PUBLIC

                        enableVibration(
                            true
                        )
                    }

                manager.createNotificationChannel(
                    channel
                )
            }
        }

        fun cancelNotification(
            context: Context,
            dayId: Int
        ) {
            val manager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.cancel(
                notificationId(
                    dayId
                )
            )
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val dayId =
            intent.getIntExtra(
                HarvestReminderManager.EXTRA_DAY_ID,
                -1
            )

        if (
            dayId <=
            0
        ) {
            return
        }

        val pendingResult =
            goAsync()

        Thread {
            try {
                if (
                    !HarvestReminderManager
                        .isEnabled(
                            context,
                            dayId
                        )
                ) {
                    return@Thread
                }

                val dao =
                    DatabaseProvider
                        .getDatabase(
                            context.applicationContext
                        )
                        .harvestDao()

                val day =
                    dao.getDayById(
                        dayId
                    )

                val process =
                    day?.let {
                        dao.getProcessById(
                            it.processId
                        )
                    }

                /*
                 * Технический предохранитель:
                 * цикл существует только пока день ACTIVE
                 * и сама уборочная кампания IN_PROGRESS.
                 */
                if (
                    day == null ||
                    process == null ||
                    day.status !=
                        "ACTIVE" ||
                    process.status !=
                        "IN_PROGRESS"
                ) {
                    HarvestReminderManager
                        .cancelForFinishedDay(
                            context,
                            dayId
                        )

                    return@Thread
                }

                when (
                    intent.action
                ) {
                    HarvestReminderManager.ACTION_SNOOZE -> {
                        val minutes =
                            intent.getIntExtra(
                                HarvestReminderManager
                                    .EXTRA_SNOOZE_MINUTES,
                                10
                            )

                        cancelNotification(
                            context,
                            dayId
                        )

                        /*
                         * +5 / +10 / +15 не считается
                         * подтверждением записи.
                         */
                        HarvestReminderManager
                            .markWaitingForResponse(
                                context,
                                dayId
                            )

                        HarvestReminderManager
                            .snooze(
                                context =
                                    context,
                                dayId =
                                    dayId,
                                minutes =
                                    minutes
                            )
                    }

                    HarvestReminderManager.ACTION_REMIND -> {
                        val wasAlreadyWaiting =
                            HarvestReminderManager
                                .isWaitingForResponse(
                                    context,
                                    dayId
                                )

                        HarvestReminderManager
                            .markWaitingForResponse(
                                context,
                                dayId
                            )

                        showNotification(
                            context =
                                context,
                            dayId =
                                dayId,
                            repeated =
                                wasAlreadyWaiting
                        )

                        /*
                         * С этого момента основной интервал
                         * больше не используется.
                         * Пока нет реакции — каждые 2:30.
                         */
                        HarvestReminderManager
                            .scheduleWaitingRepeat(
                                context,
                                dayId
                            )
                    }
                }

            } catch (
                _: Throwable
            ) {
                /*
                 * BroadcastReceiver не должен ронять приложение.
                 * Следующее открытие рабочего дня восстановит
                 * расписание через ensureScheduled().
                 */

            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun showNotification(
        context: Context,
        dayId: Int,
        repeated: Boolean
    ) {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        ensureNotificationChannel(
            context
        )

        /*
         * Повторный сигнал должен снова привлечь внимание,
         * поэтому старое уведомление сначала убираем.
         */
        if (
            repeated
        ) {
            notificationManager.cancel(
                notificationId(
                    dayId
                )
            )
        }

        val openIntent =
            Intent(
                context,
                HarvestDayActivity::class.java
            ).apply {
                putExtra(
                    HarvestDayActivity.EXTRA_DAY_ID,
                    dayId
                )

                putExtra(
                    HarvestDayActivity.EXTRA_OPEN_ADD_BUNKER,
                    true
                )

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val contentIntent =
            PendingIntent.getActivity(
                context,
                300_000 +
                    dayId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val builder =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {
                Notification.Builder(
                    context,
                    CHANNEL_ID
                )
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(
                    context
                )
            }

        builder
            .setSmallIcon(
                android.R.drawable
                    .ic_popup_reminder
            )
            .setContentTitle(
                if (
                    repeated
                ) {
                    "Бункер не записан — добавь запись"
                } else {
                    "Не забудь добавить бункер!"
                }
            )
            .setContentText(
                if (
                    repeated
                ) {
                    "Повтор через 2:30, пока бункер не записан или день не завершён."
                } else {
                    "Если запись пропущена, напоминание повторится через 2:30."
                }
            )
            .setAutoCancel(
                true
            )
            .setContentIntent(
                contentIntent
            )
            .setPriority(
                Notification.PRIORITY_HIGH
            )
            .setCategory(
                Notification.CATEGORY_REMINDER
            )
            .setVisibility(
                Notification.VISIBILITY_PUBLIC
            )
            .setShowWhen(
                true
            )
            .setOnlyAlertOnce(
                false
            )
            .addAction(
                Notification.Action.Builder(
                    0,
                    "+5 мин",
                    snoozePendingIntent(
                        context,
                        dayId,
                        5
                    )
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    0,
                    "+10 мин",
                    snoozePendingIntent(
                        context,
                        dayId,
                        10
                    )
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    0,
                    "+15 мин",
                    snoozePendingIntent(
                        context,
                        dayId,
                        15
                    )
                ).build()
            )

        notificationManager.notify(
            notificationId(
                dayId
            ),
            builder.build()
        )
    }

    private fun snoozePendingIntent(
        context: Context,
        dayId: Int,
        minutes: Int
    ): PendingIntent {
        val intent =
            Intent(
                context,
                HarvestReminderReceiver::class.java
            ).apply {
                action =
                    HarvestReminderManager.ACTION_SNOOZE

                putExtra(
                    HarvestReminderManager.EXTRA_DAY_ID,
                    dayId
                )

                putExtra(
                    HarvestReminderManager.EXTRA_SNOOZE_MINUTES,
                    minutes
                )
            }

        return PendingIntent.getBroadcast(
            context,
            400_000 +
                dayId *
                    100 +
                minutes,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
    }
}
