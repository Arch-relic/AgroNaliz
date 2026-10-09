package ru.fo6osik.workjournal

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class HarvestDayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DAY_ID =
            "harvest_day_id"

        const val EXTRA_OPEN_ADD_BUNKER =
            "harvest_open_add_bunker"

        private const val MENU_DAY_SETTINGS_ID =
            91001
    }

    private lateinit var textProcessTitle: TextView
    private lateinit var textDate: TextView
    private lateinit var textTime: TextView
    private lateinit var textBunkerCount: TextView
    private lateinit var textEstimatedVolume: TextView
    private lateinit var textAverageVolume: TextView
    private lateinit var textBunkersEmpty: TextView
    private lateinit var bunkerList: LinearLayout
    private lateinit var buttonEditDay: Button
    private lateinit var buttonAddBunker: Button
    private lateinit var buttonFinishDay: Button
    private lateinit var textReminderStatus: TextView

    private var dayId: Int = -1
    private var currentDay: HarvestDay? = null
    private var currentProcess: HarvestProcess? = null
    private var allDays: List<HarvestDay> =
        emptyList()

    private var openAddBunkerAfterLoad =
        false

    private var askedNotificationPermissionThisSession =
        false

    private var askedExactAlarmAccessThisSession =
        false

    private data class IntervalChoiceState(
        var value: Int,
        var custom: Boolean
    )

    private val fillLevelLabels =
        linkedMapOf(
            "FULL" to "Полный",
            "MORE_THAN_HALF" to "Больше половины",
            "HALF" to "Половина",
            "LESS_THAN_HALF" to "Меньше половины"
        )

    private val postEventLabels =
        linkedMapOf(
            "CONTINUE" to "Продолжение рабочего процесса",
            "LUNCH" to "Обед",
            "DINNER" to "Ужин",
            "MEAL" to "Обед / ужин",
            "WAITING_TRANSPORT" to "Ожидание транспорта"
        )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_harvest_day
        )

        dayId =
            intent.getIntExtra(
                EXTRA_DAY_ID,
                -1
            )

        if (dayId <= 0) {
            Toast.makeText(
                this,
                "Не удалось открыть рабочий день",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }


        openAddBunkerAfterLoad =
            intent.getBooleanExtra(
                EXTRA_OPEN_ADD_BUNKER,
                false
            )

        textProcessTitle =
            findViewById(
                R.id.textHarvestDayProcessTitle
            )

        textDate =
            findViewById(
                R.id.textHarvestDayDate
            )

        textTime =
            findViewById(
                R.id.textHarvestDayTime
            )

        textBunkerCount =
            findViewById(
                R.id.textHarvestDayBunkerCount
            )

        textEstimatedVolume =
            findViewById(
                R.id.textHarvestDayEstimatedVolume
            )


        textAverageVolume =
            findViewById(
                R.id.textHarvestDayAverageVolume
            )

        textBunkersEmpty =
            findViewById(
                R.id.textHarvestDayBunkersEmpty
            )

        bunkerList =
            findViewById(
                R.id.harvestDayBunkerList
            )

        buttonEditDay =
            findViewById(
                R.id.buttonEditHarvestDay
            )

        buttonAddBunker =
            findViewById(
                R.id.buttonAddHarvestDayBunker
            )

        buttonFinishDay =
            findViewById(
                R.id.buttonFinishHarvestDay
            )

        textReminderStatus =
            findViewById(
                R.id.textHarvestReminderStatus
            )

        buttonEditDay.setOnClickListener {
            currentDay?.let {
                showEditDayDialog(
                    it
                )
            }
        }

        buttonAddBunker.setOnClickListener {
            showBunkerDialog(
                bunker = null
            )
        }

        buttonFinishDay.setOnClickListener {
            showFinishDayDialog()
        }
    }

    override fun onCreateOptionsMenu(
        menu: Menu
    ): Boolean {
        menu.add(
            Menu.NONE,
            MENU_DAY_SETTINGS_ID,
            Menu.NONE,
            "Настройки рабочего дня"
        ).apply {
            setIcon(
                android.R.drawable.ic_menu_preferences
            )
            setShowAsAction(
                MenuItem.SHOW_AS_ACTION_ALWAYS
            )
        }

        return true
    }

    override fun onOptionsItemSelected(
        item: MenuItem
    ): Boolean {
        if (
            item.itemId ==
            MENU_DAY_SETTINGS_ID
        ) {
            showDaySettingsDialog()
            return true
        }

        return super.onOptionsItemSelected(
            item
        )
    }

    override fun onResume() {
        super.onResume()

        currentDay?.let {
            day ->

            if (
                day.status ==
                "ACTIVE" &&
                HarvestReminderManager
                    .isEnabled(
                        this,
                        day.id
                    )
            ) {
                HarvestReminderManager
                    .ensureScheduled(
                        this,
                        day.id
                    )
            }
        }

        loadDay()
    }


    override fun onNewIntent(
        intent: android.content.Intent?
    ) {
        super.onNewIntent(
            intent
        )

        if (
            intent == null
        ) {
            return
        }

        setIntent(
            intent
        )

        openAddBunkerAfterLoad =
            intent.getBooleanExtra(
                EXTRA_OPEN_ADD_BUNKER,
                false
            )

        loadDay()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            6101
        ) {
            currentDay?.let {
                day ->

                if (
                    day.status ==
                    "ACTIVE"
                ) {
                    HarvestReminderManager
                        .ensureScheduled(
                            this,
                            day.id
                        )

                    refreshReminderStatus()
                }
            }
        }
    }

    private fun loadDay() {
        Thread {
            try {
                val dao =
                    DatabaseProvider
                        .getDatabase(
                            applicationContext
                        )
                        .harvestDao()

                var day =
                    dao.getDayById(
                        dayId
                    )
                        ?: error(
                            "Рабочий день не найден"
                        )

                /*
                 * Первый запуск v20 для рабочего дня из старой БД:
                 * переносим прежний основной интервал напоминаний
                 * из SharedPreferences в настройки самого дня.
                 */
                if (
                    !day.reminderSettingsInitialized
                ) {
                    val legacyWorkInterval =
                        HarvestReminderManager
                            .getIntervalMinutes(
                                applicationContext,
                                day.id
                            )

                    dao.updateDayReminderSettings(
                        dayId =
                            day.id,
                        workIntervalMinutes =
                            legacyWorkInterval,
                        unloadIntervalMinutes =
                            5,
                        lunchIntervalMinutes =
                            15,
                        dinnerIntervalMinutes =
                            15,
                        waitingTransportIntervalMinutes =
                            30,
                        workIntervalCustom =
                            legacyWorkInterval !in
                                intArrayOf(
                                    30,
                                    40,
                                    50,
                                    60
                                ),
                        unloadIntervalCustom =
                            false,
                        lunchIntervalCustom =
                            false,
                        dinnerIntervalCustom =
                            false,
                        waitingTransportIntervalCustom =
                            false,
                        reminderSettingsInitialized =
                            true
                    )

                    day =
                        dao.getDayById(
                            day.id
                        ) ?: day
                }

                /*
                 * Если сейчас нет уже запущенного таймера,
                 * runtime-настройки можно безопасно синхронизировать
                 * с настройками рабочего дня.
                 *
                 * Если таймер уже идёт, он остаётся без изменений;
                 * новые интервалы начнут действовать со следующего цикла.
                 */
                if (
                    day.status ==
                        "ACTIVE" &&
                    HarvestReminderManager
                        .getNextAt(
                            applicationContext,
                            day.id
                        ) <=
                        0L
                ) {
                    HarvestReminderManager
                        .syncDaySettings(
                            context =
                                applicationContext,
                            dayId =
                                day.id,
                            workIntervalMinutes =
                                day.workIntervalMinutes,
                            unloadIntervalMinutes =
                                day.unloadIntervalMinutes
                        )
                }

                val process =
                    dao.getProcessById(
                        day.processId
                    )
                        ?: error(
                            "Уборочный процесс не найден"
                        )

                val days =
                    dao.getDaysForProcess(
                        day.processId
                    )

                val bunkers =
                    dao.getBunkersForDay(
                        day.id
                    )

                runOnUiThread {
                    currentDay =
                        day

                    currentProcess =
                        process

                    allDays =
                        days

                    renderDay(
                        process,
                        day,
                        bunkers
                    )
                }

            } catch (
                e: Throwable
            ) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Ошибка загрузки рабочего дня: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun renderDay(
        process: HarvestProcess,
        day: HarvestDay,
        bunkers: List<HarvestBunker>
    ) {
        textProcessTitle.text =
            "${process.fieldNameSnapshot} — ${process.crop}"

        textDate.text =
            formatDateForUi(
                day.workDate
            )

        textTime.text =
            "Начало: ${day.startTime}\n" +
                "Конец: ${day.endTime ?: "—"}"

        textBunkerCount.text =
            "Бункеров за день: " +
                bunkerCountText(
                    bunkers.size
                )

        textEstimatedVolume.text =
            calculationText(
                label =
                    "Расчётный объём за день",
                bunkerVolumeM3 =
                    process.bunkerVolumeM3,
                bunkers =
                    bunkers
            )

        textAverageVolume.text =
            averageText(
                bunkerVolumeM3 =
                    process.bunkerVolumeM3,
                bunkers =
                    bunkers
            )

        val isActive =
            day.status ==
                "ACTIVE" &&
                process.status ==
                    "IN_PROGRESS"

        buttonAddBunker.visibility =
            if (
                isActive
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        buttonFinishDay.visibility =
            if (
                isActive
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        textReminderStatus.visibility =
            if (
                isActive
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (
            isActive
        ) {
            HarvestReminderReceiver
                .ensureNotificationChannel(
                    this
                )

            requestNotificationPermissionIfNeeded()
            requestExactAlarmAccessIfNeeded()

            HarvestReminderManager
                .ensureScheduled(
                    this,
                    day.id
                )

            refreshReminderStatus()

            if (
                openAddBunkerAfterLoad
            ) {
                openAddBunkerAfterLoad =
                    false

                showBunkerDialog(
                    bunker =
                        null
                )
            }

        } else {
            HarvestReminderManager
                .cancelForFinishedDay(
                    this,
                    day.id
                )
        }

        renderBunkers(
            bunkers
        )
    }

    private fun renderBunkers(
        bunkers: List<HarvestBunker>
    ) {
        bunkerList.removeAllViews()

        if (
            bunkers.isEmpty()
        ) {
            textBunkersEmpty.visibility =
                View.VISIBLE
            return
        }

        textBunkersEmpty.visibility =
            View.GONE

        bunkers.forEachIndexed {
                index,
                bunker ->

            if (
                index > 0
            ) {
                val previous =
                    bunkers[
                        index - 1
                    ]

                val minutes =
                    minutesBetween(
                        previous.fillTime,
                        bunker.fillTime
                    )

                if (
                    minutes != null
                ) {
                    val intervalView =
                        TextView(this).apply {
                            val eventText =
                                postEventLabel(
                                    previous.postEventType
                                )

                            text =
                                "$eventText\n" +
                                    "+$minutes " +
                                    minuteWord(
                                        minutes
                                    )

                            textSize =
                                15f

                            gravity =
                                android.view.Gravity.CENTER

                            setTextColor(
                                Color.DKGRAY
                            )

                            setPadding(
                                dp(8),
                                dp(7),
                                dp(8),
                                dp(7)
                            )

                            isClickable =
                                true

                            isFocusable =
                                true

                            setOnClickListener {
                                showPostBunkerEventDialog(
                                    previous
                                )
                            }
                        }

                    bunkerList.addView(
                        intervalView
                    )
                }
            }

            val fillText =
                fillLevelLabels[
                    bunker.fillLevel
                ] ?: bunker.fillLevel

            val view =
                TextView(this).apply {
                    text =
                        "${bunker.fillTime} — " +
                            "Бункер №${index + 1} — " +
                            fillText

                    textSize =
                        17f

                    setTextColor(
                        Color.BLACK
                    )

                    setPadding(
                        dp(12),
                        dp(10),
                        dp(12),
                        dp(10)
                    )

                    if (
                        index % 2 == 0
                    ) {
                        setBackgroundColor(
                            Color.rgb(
                                245,
                                245,
                                245
                            )
                        )
                    }

                    isClickable =
                        true

                    isFocusable =
                        true

                    setOnClickListener {
                        showBunkerActionsDialog(
                            bunker =
                                bunker,
                            bunkerNumber =
                                index + 1
                        )
                    }
                }

            bunkerList.addView(
                view
            )
        }
    }


    private fun postEventLabel(
        postEventType: String?
    ): String {
        return if (
            postEventType == null
        ) {
            "Событие не указано"
        } else {
            postEventLabels[
                postEventType
            ] ?: postEventType
        }
    }

    private fun graceMinutesForPostEvent(
        postEventType: String?,
        day: HarvestDay
    ): Int {
        return when (
            postEventType
        ) {
            "LUNCH" ->
                day.lunchIntervalMinutes

            "DINNER" ->
                day.dinnerIntervalMinutes

            /*
             * Старое значение v19.
             * Его не переименовываем автоматически, потому что
             * нельзя достоверно определить: это был обед или ужин.
             */
            "MEAL" ->
                HarvestReminderManager
                    .GRACE_MEAL_MINUTES

            "WAITING_TRANSPORT" ->
                day.waitingTransportIntervalMinutes

            else ->
                HarvestReminderManager
                    .GRACE_NONE_MINUTES
        }
    }

    private fun showBunkerActionsDialog(
        bunker: HarvestBunker,
        bunkerNumber: Int
    ) {
        val fillText =
            fillLevelLabels[
                bunker.fillLevel
            ] ?: bunker.fillLevel

        val items =
            arrayOf(
                "Редактировать бункер",
                "Событие после бункера",
                "Удалить бункер"
            )

        AlertDialog
            .Builder(this)
            .setTitle(
                "Бункер №$bunkerNumber — ${bunker.fillTime} — $fillText"
            )
            .setItems(
                items
            ) { _, which ->
                when (
                    which
                ) {
                    0 ->
                        showBunkerDialog(
                            bunker
                        )

                    1 ->
                        showPostBunkerEventDialog(
                            bunker
                        )

                    2 ->
                        showDeleteBunkerDialog(
                            bunker =
                                bunker,
                            bunkerNumber =
                                bunkerNumber
                        )
                }
            }
            .setNegativeButton(
                "Отмена",
                null
            )
            .show()
    }

    private fun showPostBunkerEventDialog(
        bunker: HarvestBunker
    ) {
        val eventTypes =
            mutableListOf<String?>(
                null,
                "CONTINUE",
                "LUNCH",
                "DINNER",
                "WAITING_TRANSPORT"
            )

        val eventLabels =
            mutableListOf(
                "Событие не указано",
                "Продолжение рабочего процесса",
                "Обед",
                "Ужин",
                "Ожидание транспорта"
            )

        /*
         * Старое объединённое событие v19 оставляем доступным
         * только для уже существующих записей, чтобы ничего
         * не потерять и не переименовать автоматически.
         */
        if (
            bunker.postEventType ==
            "MEAL"
        ) {
            eventTypes.add(
                "MEAL"
            )

            eventLabels.add(
                "Обед / ужин (старое значение)"
            )
        }

        var selectedType =
            bunker.postEventType

        val checkedItem =
            eventTypes
                .indexOf(
                    bunker.postEventType
                )
                .coerceAtLeast(
                    0
                )

        AlertDialog
            .Builder(this)
            .setTitle(
                "Событие после бункера"
            )
            .setSingleChoiceItems(
                eventLabels.toTypedArray(),
                checkedItem
            ) { _, which ->
                selectedType =
                    eventTypes[
                        which
                    ]
            }
            .setPositiveButton(
                "Сохранить"
            ) { _, _ ->
                savePostBunkerEvent(
                    bunker =
                        bunker,
                    postEventType =
                        selectedType
                )
            }
            .setNegativeButton(
                "Отмена",
                null
            )
            .show()
    }

    private fun savePostBunkerEvent(
        bunker: HarvestBunker,
        postEventType: String?
    ) {
        val day =
            currentDay
                ?: return

        Thread {
            try {
                val dao =
                    DatabaseProvider
                        .getDatabase(
                            applicationContext
                        )
                        .harvestDao()

                dao.updateBunkerPostEventType(
                    bunkerId =
                        bunker.id,
                    postEventType =
                        postEventType
                )

                val bunkers =
                    dao.getBunkersForDay(
                        day.id
                    )

                val isLatest =
                    bunkers
                        .lastOrNull()
                        ?.id ==
                        bunker.id

                if (
                    isLatest
                ) {
                    rescheduleReminderAfterHistoryChange(
                        day =
                            day,
                        dao =
                            dao
                    )
                }

                runOnUiThread {
                    loadDay()

                    Toast.makeText(
                        this,
                        "Событие сохранено",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (
                e: Throwable
            ) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Ошибка сохранения события: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun showDeleteBunkerDialog(
        bunker: HarvestBunker,
        bunkerNumber: Int
    ) {
        AlertDialog
            .Builder(this)
            .setTitle(
                "Удалить бункер №$bunkerNumber?"
            )
            .setMessage(
                "Запись будет удалена. " +
                    "Количество бункеров, объём и интервалы " +
                    "пересчитаются автоматически."
            )
            .setPositiveButton(
                "Удалить"
            ) { _, _ ->
                deleteBunker(
                    bunker
                )
            }
            .setNegativeButton(
                "Отмена",
                null
            )
            .show()
    }

    private fun deleteBunker(
        bunker: HarvestBunker
    ) {
        val day =
            currentDay
                ?: return

        Thread {
            try {
                val dao =
                    DatabaseProvider
                        .getDatabase(
                            applicationContext
                        )
                        .harvestDao()

                val bunkersBefore =
                    dao.getBunkersForDay(
                        day.id
                    )

                val wasLatest =
                    bunkersBefore
                        .lastOrNull()
                        ?.id ==
                        bunker.id

                dao.deleteBunker(
                    bunker.id
                )

                if (
                    wasLatest
                ) {
                    rescheduleReminderAfterHistoryChange(
                        day =
                            day,
                        dao =
                            dao
                    )
                }

                runOnUiThread {
                    loadDay()

                    Toast.makeText(
                        this,
                        "Бункер удалён",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (
                e: Throwable
            ) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Ошибка удаления бункера: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun rescheduleReminderAfterHistoryChange(
        day: HarvestDay,
        dao: HarvestDao
    ) {
        if (
            day.status !=
            "ACTIVE"
        ) {
            return
        }

        val process =
            dao.getProcessById(
                day.processId
            )
                ?: return

        if (
            process.status !=
            "IN_PROGRESS"
        ) {
            return
        }

        HarvestReminderManager
            .syncDaySettings(
                context =
                    applicationContext,
                dayId =
                    day.id,
                workIntervalMinutes =
                    day.workIntervalMinutes,
                unloadIntervalMinutes =
                    day.unloadIntervalMinutes
            )

        if (
            !HarvestReminderManager
                .isEnabled(
                    applicationContext,
                    day.id
                )
        ) {
            return
        }

        val latestBunker =
            dao.getBunkersForDay(
                day.id
            )
                .lastOrNull()

        if (
            latestBunker == null
        ) {
            HarvestReminderManager
                .resetAfterBunkerAdded(
                    context =
                        applicationContext,
                    dayId =
                        day.id,
                    graceMinutes =
                        HarvestReminderManager
                            .GRACE_NONE_MINUTES,
                    bunkerAddedAtMillis =
                        System.currentTimeMillis(),
                    includeUnload =
                        false
                )

            return
        }

        HarvestReminderManager
            .resetAfterBunkerAdded(
                context =
                    applicationContext,
                dayId =
                    day.id,
                graceMinutes =
                    graceMinutesForPostEvent(
                        latestBunker
                            .postEventType,
                        day
                    ),
                bunkerAddedAtMillis =
                    latestBunker
                        .createdAt
            )
    }

    private fun showDaySettingsDialog() {
        val day =
            currentDay
                ?: return

        val dialogView =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.dialog_harvest_day_settings,
                    null
                )

        val textSettingsDate =
            dialogView.findViewById<TextView>(
                R.id.textHarvestDaySettingsDate
            )

        val switchEnabled =
            dialogView.findViewById<Switch>(
                R.id.switchHarvestReminderEnabled
            )

        val textExample =
            dialogView.findViewById<TextView>(
                R.id.textReminderSettingsExample
            )

        val textApplyHint =
            dialogView.findViewById<TextView>(
                R.id.textReminderSettingsApplyHint
            )

        val buttonSave =
            dialogView.findViewById<Button>(
                R.id.buttonSaveHarvestDaySettings
            )

        val buttonCancel =
            dialogView.findViewById<Button>(
                R.id.buttonCancelHarvestDaySettings
            )

        val workCustomInput =
            dialogView.findViewById<EditText>(
                R.id.inputWorkCustom
            )

        val unloadCustomInput =
            dialogView.findViewById<EditText>(
                R.id.inputUnloadCustom
            )

        val lunchCustomInput =
            dialogView.findViewById<EditText>(
                R.id.inputLunchCustom
            )

        val dinnerCustomInput =
            dialogView.findViewById<EditText>(
                R.id.inputDinnerCustom
            )

        val waitingCustomInput =
            dialogView.findViewById<EditText>(
                R.id.inputWaitingCustom
            )

        val workState =
            IntervalChoiceState(
                value =
                    day.workIntervalMinutes,
                custom =
                    day.workIntervalCustom
            )

        val unloadState =
            IntervalChoiceState(
                value =
                    day.unloadIntervalMinutes,
                custom =
                    day.unloadIntervalCustom
            )

        val lunchState =
            IntervalChoiceState(
                value =
                    day.lunchIntervalMinutes,
                custom =
                    day.lunchIntervalCustom
            )

        val dinnerState =
            IntervalChoiceState(
                value =
                    day.dinnerIntervalMinutes,
                custom =
                    day.dinnerIntervalCustom
            )

        val waitingState =
            IntervalChoiceState(
                value =
                    day.waitingTransportIntervalMinutes,
                custom =
                    day.waitingTransportIntervalCustom
            )

        textSettingsDate.text =
            "Рабочий день: " +
                formatDateForUi(
                    day.workDate
                )

        val activeDay =
            day.status ==
                "ACTIVE" &&
                currentProcess
                    ?.status ==
                    "IN_PROGRESS"

        switchEnabled.isChecked =
            activeDay &&
                HarvestReminderManager
                    .isEnabled(
                        this,
                        day.id
                    )

        switchEnabled.isEnabled =
            activeDay

        fun refreshReminderSwitchText() {
            switchEnabled.text =
                if (
                    !activeDay
                ) {
                    "Напоминания выключены — день завершён"
                } else if (
                    switchEnabled.isChecked
                ) {
                    "Напоминания включены"
                } else {
                    "Напоминания выключены"
                }
        }

        refreshReminderSwitchText()

        if (
            !activeDay
        ) {
            textApplyHint.text =
                "Настройки сохранены в истории. Уведомления не запускаются."
        }

        switchEnabled.setOnCheckedChangeListener {
                _, _ ->

            refreshReminderSwitchText()
        }

        fun refreshExample() {
            val work =
                workState.value
                    .coerceAtLeast(
                        0
                    )

            val unload =
                unloadState.value
                    .coerceAtLeast(
                        0
                    )

            val lunch =
                lunchState.value
                    .coerceAtLeast(
                        0
                    )

            val dinner =
                dinnerState.value
                    .coerceAtLeast(
                        0
                    )

            val waiting =
                waitingState.value
                    .coerceAtLeast(
                        0
                    )

            textExample.text =
                buildString {
                    append(
                        "Обычный цикл: $work + $unload = " +
                            "${work + unload} мин\n"
                    )

                    append(
                        "Обед: $work + $unload + $lunch = " +
                            "${work + unload + lunch} мин\n"
                    )

                    append(
                        "Ужин: $work + $unload + $dinner = " +
                            "${work + unload + dinner} мин\n"
                    )

                    append(
                        "Ожидание транспорта: " +
                            "$work + $unload + $waiting = " +
                            "${work + unload + waiting} мин"
                    )
                }
        }

        fun setupSelector(
            presetViews: List<Pair<TextView, Int>>,
            customView: TextView,
            customInput: EditText,
            state: IntervalChoiceState
        ) {
            val presetValues =
                presetViews.map {
                    it.second
                }

            if (
                state.custom ||
                state.value !in
                    presetValues
            ) {
                state.custom =
                    true

                customInput.visibility =
                    View.VISIBLE

                customInput.setText(
                    state.value
                        .toString()
                )
            } else {
                customInput.visibility =
                    View.GONE
            }

            fun refreshStyles() {
                presetViews.forEach {
                        pair ->

                    applyIntervalOptionStyle(
                        view =
                            pair.first,
                        selected =
                            !state.custom &&
                                state.value ==
                                    pair.second
                    )
                }

                applyIntervalOptionStyle(
                    view =
                        customView,
                    selected =
                        state.custom
                )
            }

            presetViews.forEach {
                    pair ->

                pair.first.setOnClickListener {
                    state.value =
                        pair.second

                    state.custom =
                        false

                    customInput.visibility =
                        View.GONE

                    customInput.clearFocus()

                    refreshStyles()
                    refreshExample()
                }
            }

            customView.setOnClickListener {
                state.custom =
                    true

                customInput.visibility =
                    View.VISIBLE

                if (
                    customInput.text
                        .toString()
                        .isBlank()
                ) {
                    customInput.setText(
                        state.value
                            .toString()
                    )
                }

                refreshStyles()
                refreshExample()
            }

            customInput.addTextChangedListener(
                object : TextWatcher {

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(
                        s: Editable?
                    ) {
                        if (
                            !state.custom
                        ) {
                            return
                        }

                        s
                            ?.toString()
                            ?.toIntOrNull()
                            ?.let {
                                state.value =
                                    it

                                refreshExample()
                            }
                    }
                }
            )

            refreshStyles()
        }

        setupSelector(
            presetViews =
                listOf(
                    dialogView.findViewById<TextView>(
                        R.id.optionWork30
                    ) to 30,
                    dialogView.findViewById<TextView>(
                        R.id.optionWork40
                    ) to 40,
                    dialogView.findViewById<TextView>(
                        R.id.optionWork50
                    ) to 50,
                    dialogView.findViewById<TextView>(
                        R.id.optionWork60
                    ) to 60
                ),
            customView =
                dialogView.findViewById(
                    R.id.optionWorkCustom
                ),
            customInput =
                workCustomInput,
            state =
                workState
        )

        setupSelector(
            presetViews =
                listOf(
                    dialogView.findViewById<TextView>(
                        R.id.optionUnload5
                    ) to 5
                ),
            customView =
                dialogView.findViewById(
                    R.id.optionUnloadCustom
                ),
            customInput =
                unloadCustomInput,
            state =
                unloadState
        )

        setupSelector(
            presetViews =
                listOf(
                    dialogView.findViewById<TextView>(
                        R.id.optionLunch10
                    ) to 10,
                    dialogView.findViewById<TextView>(
                        R.id.optionLunch15
                    ) to 15,
                    dialogView.findViewById<TextView>(
                        R.id.optionLunch20
                    ) to 20
                ),
            customView =
                dialogView.findViewById(
                    R.id.optionLunchCustom
                ),
            customInput =
                lunchCustomInput,
            state =
                lunchState
        )

        setupSelector(
            presetViews =
                listOf(
                    dialogView.findViewById<TextView>(
                        R.id.optionDinner10
                    ) to 10,
                    dialogView.findViewById<TextView>(
                        R.id.optionDinner15
                    ) to 15,
                    dialogView.findViewById<TextView>(
                        R.id.optionDinner20
                    ) to 20
                ),
            customView =
                dialogView.findViewById(
                    R.id.optionDinnerCustom
                ),
            customInput =
                dinnerCustomInput,
            state =
                dinnerState
        )

        setupSelector(
            presetViews =
                listOf(
                    dialogView.findViewById<TextView>(
                        R.id.optionWaiting30
                    ) to 30,
                    dialogView.findViewById<TextView>(
                        R.id.optionWaiting40
                    ) to 40,
                    dialogView.findViewById<TextView>(
                        R.id.optionWaiting60
                    ) to 60
                ),
            customView =
                dialogView.findViewById(
                    R.id.optionWaitingCustom
                ),
            customInput =
                waitingCustomInput,
            state =
                waitingState
        )

        refreshExample()

        val dialog =
            AlertDialog
                .Builder(this)
                .setView(
                    dialogView
                )
                .create()

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        fun readValue(
            label: String,
            state: IntervalChoiceState,
            input: EditText
        ): Int? {
            if (
                !state.custom
            ) {
                return state.value
            }

            val value =
                input.text
                    .toString()
                    .trim()
                    .toIntOrNull()

            if (
                value == null ||
                value <
                    1 ||
                value >
                    600
            ) {
                Toast.makeText(
                    this,
                    "Для «$label» укажите значение от 1 до 600 минут",
                    Toast.LENGTH_LONG
                ).show()

                return null
            }

            return value
        }

        buttonSave.setOnClickListener {
            val work =
                readValue(
                    "Интервал работы",
                    workState,
                    workCustomInput
                ) ?: return@setOnClickListener

            val unload =
                readValue(
                    "Разгрузка бункера",
                    unloadState,
                    unloadCustomInput
                ) ?: return@setOnClickListener

            val lunch =
                readValue(
                    "Обед",
                    lunchState,
                    lunchCustomInput
                ) ?: return@setOnClickListener

            val dinner =
                readValue(
                    "Ужин",
                    dinnerState,
                    dinnerCustomInput
                ) ?: return@setOnClickListener

            val waiting =
                readValue(
                    "Ожидание транспорта",
                    waitingState,
                    waitingCustomInput
                ) ?: return@setOnClickListener

            buttonSave.isEnabled =
                false

            val wasEnabled =
                HarvestReminderManager
                    .isEnabled(
                        this,
                        day.id
                    )

            Thread {
                try {
                    val dao =
                        DatabaseProvider
                            .getDatabase(
                                applicationContext
                            )
                            .harvestDao()

                    dao.updateDayReminderSettings(
                        dayId =
                            day.id,
                        workIntervalMinutes =
                            work,
                        unloadIntervalMinutes =
                            unload,
                        lunchIntervalMinutes =
                            lunch,
                        dinnerIntervalMinutes =
                            dinner,
                        waitingTransportIntervalMinutes =
                            waiting,
                        workIntervalCustom =
                            workState.custom,
                        unloadIntervalCustom =
                            unloadState.custom,
                        lunchIntervalCustom =
                            lunchState.custom,
                        dinnerIntervalCustom =
                            dinnerState.custom,
                        waitingTransportIntervalCustom =
                            waitingState.custom,
                        reminderSettingsInitialized =
                            true
                    )

                    runOnUiThread {
                        val updatedDay =
                            day.copy(
                                workIntervalMinutes =
                                    work,
                                unloadIntervalMinutes =
                                    unload,
                                lunchIntervalMinutes =
                                    lunch,
                                dinnerIntervalMinutes =
                                    dinner,
                                waitingTransportIntervalMinutes =
                                    waiting,
                                workIntervalCustom =
                                    workState.custom,
                                unloadIntervalCustom =
                                    unloadState.custom,
                                lunchIntervalCustom =
                                    lunchState.custom,
                                dinnerIntervalCustom =
                                    dinnerState.custom,
                                waitingTransportIntervalCustom =
                                    waitingState.custom,
                                reminderSettingsInitialized =
                                    true
                            )

                        currentDay =
                            updatedDay

                        if (
                            activeDay
                        ) {
                            if (
                                switchEnabled
                                    .isChecked
                            ) {
                                HarvestReminderReceiver
                                    .ensureNotificationChannel(
                                        this
                                    )

                                requestNotificationPermissionIfNeeded()
                                requestExactAlarmAccessIfNeeded()

                                if (
                                    !wasEnabled ||
                                    HarvestReminderManager
                                        .getNextAt(
                                            this,
                                            day.id
                                        ) <=
                                        0L
                                ) {
                                    HarvestReminderManager
                                        .syncDaySettings(
                                            context =
                                                this,
                                            dayId =
                                                day.id,
                                            workIntervalMinutes =
                                                work,
                                            unloadIntervalMinutes =
                                                unload
                                        )

                                    if (
                                        !wasEnabled
                                    ) {
                                        HarvestReminderManager
                                            .enable(
                                                this,
                                                day.id
                                            )
                                    } else {
                                        HarvestReminderManager
                                            .ensureScheduled(
                                                this,
                                                day.id
                                            )
                                    }
                                }
                            } else {
                                HarvestReminderManager
                                    .disable(
                                        this,
                                        day.id
                                    )
                            }
                        } else {
                            HarvestReminderManager
                                .cancelForFinishedDay(
                                    this,
                                    day.id
                                )
                        }

                        dialog.dismiss()
                        refreshReminderStatus()
                        loadDay()

                        Toast.makeText(
                            this,
                            "Настройки рабочего дня сохранены",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (
                    e: Throwable
                ) {
                    runOnUiThread {
                        buttonSave.isEnabled =
                            true

                        Toast.makeText(
                            this,
                            "Ошибка сохранения настроек: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.start()
        }

        dialog.show()
    }

    private fun applyIntervalOptionStyle(
        view: TextView,
        selected: Boolean
    ) {
        view.setBackgroundResource(
            if (
                selected
            ) {
                R.drawable
                    .bg_interval_option_selected
            } else {
                R.drawable
                    .bg_interval_option_outline
            }
        )

        view.setTextColor(
            ContextCompat.getColor(
                this,
                if (
                    selected
                ) {
                    android.R.color.white
                } else {
                    R.color.colorPrimary
                }
            )
        )
    }

    private fun refreshReminderStatus() {
        val day =
            currentDay
                ?: return

        textReminderStatus.text =
            HarvestReminderManager
                .nextReminderText(
                    this,
                    day.id
                )
    }

    private fun requestExactAlarmAccessIfNeeded() {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.S
        ) {
            return
        }

        val alarmManager =
            getSystemService(
                ALARM_SERVICE
            ) as AlarmManager

        if (
            alarmManager
                .canScheduleExactAlarms()
        ) {
            return
        }

        if (
            askedExactAlarmAccessThisSession
        ) {
            return
        }

        askedExactAlarmAccessThisSession =
            true

        AlertDialog
            .Builder(
                this
            )
            .setTitle(
                "Разрешить точные напоминания"
            )
            .setMessage(
                "Чтобы напоминание приходило вовремя даже при заблокированном экране, " +
                    "нужно разрешить приложению устанавливать точные будильники и напоминания."
            )
            .setNegativeButton(
                "Позже",
                null
            )
            .setPositiveButton(
                "Открыть настройки"
            ) {
                _,
                _ ->

                try {
                    startActivity(
                        Intent(
                            Settings
                                .ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse(
                                "package:$packageName"
                            )
                        )
                    )

                } catch (
                    _: Exception
                ) {
                    startActivity(
                        Intent(
                            Settings
                                .ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        )
                    )
                }
            }
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }

        if (
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (
            askedNotificationPermissionThisSession
        ) {
            return
        }

        askedNotificationPermissionThisSession =
            true

        requestPermissions(
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS
            ),
            6101
        )
    }

    private fun showEditDayDialog(
        day: HarvestDay
    ) {
        val process =
            currentProcess
                ?: return

        val dialogView =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.dialog_edit_harvest_day,
                    null
                )

        val textEditDate =
            dialogView.findViewById<TextView>(
                R.id.textEditHarvestDayDate
            )

        val textStart =
            dialogView.findViewById<TextView>(
                R.id.textEditHarvestDayStart
            )

        val textEnd =
            dialogView.findViewById<TextView>(
                R.id.textEditHarvestDayEnd
            )

        val spinnerStatus =
            dialogView.findViewById<Spinner>(
                R.id.spinnerEditHarvestDayStatus
            )

        val buttonSave =
            dialogView.findViewById<Button>(
                R.id.buttonConfirmEditHarvestDay
            )

        val buttonCancel =
            dialogView.findViewById<Button>(
                R.id.buttonCancelEditHarvestDay
            )

        val dateCalendar =
            parseDateToCalendar(
                day.workDate
            )

        val startCalendar =
            Calendar.getInstance()

        setCalendarTime(
            startCalendar,
            day.startTime
        )

        val endCalendar =
            Calendar.getInstance()

        day.endTime?.let {
            setCalendarTime(
                endCalendar,
                it
            )
        }

        val statuses =
            listOf(
                "Текущий",
                "Завершён"
            )

        spinnerStatus.adapter =
            ArrayAdapter(
                this,
                android.R.layout
                    .simple_spinner_dropdown_item,
                statuses
            )

        spinnerStatus.setSelection(
            if (
                day.status ==
                "ACTIVE"
            ) {
                0
            } else {
                1
            }
        )

        fun updateDate() {
            textEditDate.text =
                SimpleDateFormat(
                    "dd.MM.yyyy",
                    Locale("ru")
                ).format(
                    dateCalendar.time
                )
        }

        fun updateStart() {
            textStart.text =
                formatCalendarTime(
                    startCalendar
                )
        }

        fun updateEnd(
            status: String
        ) {
            if (
                status ==
                "ACTIVE"
            ) {
                textEnd.text =
                    "—"

                textEnd.isClickable =
                    false

                textEnd.isFocusable =
                    false

                textEnd.setOnClickListener(
                    null
                )

            } else {
                if (
                    textEnd.text
                        .toString() ==
                    "—"
                ) {
                    val now =
                        Calendar.getInstance()

                    endCalendar.timeInMillis =
                        now.timeInMillis

                    textEnd.text =
                        formatCalendarTime(
                            endCalendar
                        )
                }

                textEnd.isClickable =
                    true

                textEnd.isFocusable =
                    true

                textEnd.setOnClickListener {
                    showTimePicker(
                        endCalendar
                    ) {
                        textEnd.text =
                            formatCalendarTime(
                                endCalendar
                            )
                    }
                }
            }
        }

        updateDate()
        updateStart()

        textEnd.text =
            day.endTime
                ?: "—"

        textEditDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    dateCalendar.set(
                        Calendar.YEAR,
                        year
                    )

                    dateCalendar.set(
                        Calendar.MONTH,
                        month
                    )

                    dateCalendar.set(
                        Calendar.DAY_OF_MONTH,
                        dayOfMonth
                    )

                    updateDate()
                },
                dateCalendar.get(
                    Calendar.YEAR
                ),
                dateCalendar.get(
                    Calendar.MONTH
                ),
                dateCalendar.get(
                    Calendar.DAY_OF_MONTH
                )
            ).show()
        }

        textStart.setOnClickListener {
            showTimePicker(
                startCalendar
            ) {
                updateStart()
            }
        }

        updateEnd(
            if (
                day.status ==
                "ACTIVE"
            ) {
                "ACTIVE"
            } else {
                "COMPLETED"
            }
        )

        spinnerStatus.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateEnd(
                        if (
                            position == 0
                        ) {
                            "ACTIVE"
                        } else {
                            "COMPLETED"
                        }
                    )
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }

        val dialog =
            AlertDialog
                .Builder(this)
                .setView(
                    dialogView
                )
                .create()

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        buttonSave.setOnClickListener {
            val newDate =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).format(
                    dateCalendar.time
                )

            if (
                newDate <
                process.startDate
            ) {
                Toast.makeText(
                    this,
                    "Дата рабочего дня не может быть раньше начала уборки",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            if (
                allDays.any {
                    it.id !=
                        day.id &&
                        it.workDate ==
                            newDate
                }
            ) {
                Toast.makeText(
                    this,
                    "На эту дату рабочий день уже существует",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            val newStatus =
                if (
                    spinnerStatus
                        .selectedItemPosition ==
                    0
                ) {
                    "ACTIVE"
                } else {
                    "COMPLETED"
                }

            if (
                newStatus ==
                "ACTIVE"
            ) {
                val anotherActiveDay =
                    allDays.firstOrNull {
                        it.id !=
                            day.id &&
                            it.status ==
                                "ACTIVE"
                    }

                if (
                    anotherActiveDay !=
                    null
                ) {
                    Toast.makeText(
                        this,
                        "В этом уборочном процессе уже есть текущий рабочий день",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setOnClickListener
                }
            }

            val newEndTime =
                if (
                    newStatus ==
                    "ACTIVE"
                ) {
                    null
                } else {
                    textEnd
                        .text
                        .toString()
                        .takeIf {
                            it != "—"
                        }
                }

            if (
                newStatus ==
                    "COMPLETED" &&
                newEndTime ==
                    null
            ) {
                Toast.makeText(
                    this,
                    "Для завершённого дня укажите время окончания",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            buttonSave.isEnabled =
                false

            Thread {
                try {
                    val database =
                        DatabaseProvider
                            .getDatabase(
                                applicationContext
                            )

                    database.runInTransaction {
                        val dao =
                            database
                                .harvestDao()

                        dao.updateDayDetails(
                            dayId =
                                day.id,
                            workDate =
                                newDate,
                            startTime =
                                textStart
                                    .text
                                    .toString(),
                            endTime =
                                newEndTime,
                            status =
                                newStatus
                        )

                        if (
                            newStatus ==
                            "ACTIVE"
                        ) {
                            dao.reopenProcess(
                                day.processId
                            )
                        } else {
                            dao.refreshCompletedProcessEndDate(
                                day.processId
                            )
                        }
                    }

                    runOnUiThread {
                        if (
                            newStatus ==
                            "ACTIVE"
                        ) {
                            HarvestReminderManager
                                .syncDaySettings(
                                    context =
                                        this,
                                    dayId =
                                        day.id,
                                    workIntervalMinutes =
                                        day.workIntervalMinutes,
                                    unloadIntervalMinutes =
                                        day.unloadIntervalMinutes
                                )

                            HarvestReminderManager
                                .enable(
                                    this,
                                    day.id
                                )
                        } else {
                            HarvestReminderManager
                                .cancelForFinishedDay(
                                    this,
                                    day.id
                                )
                        }

                        dialog.dismiss()
                        loadDay()
                    }

                } catch (
                    e: Throwable
                ) {
                    runOnUiThread {
                        buttonSave.isEnabled =
                            true

                        Toast.makeText(
                            this,
                            "Ошибка редактирования рабочего дня: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.start()
        }

        dialog.show()
    }

    private fun showPostBunkerTimerGraceDialog(
        dayId: Int,
        bunkerId: Int,
        bunkerAddedAtMillis: Long
    ) {
        val day =
            currentDay
                ?.takeIf {
                    it.id ==
                        dayId
                }
                ?: return

        /*
         * На момент открытия этого окна обычный таймер уже
         * запущен от ТОГО ЖЕ момента добавления бункера.
         *
         * В v20 в обычный цикл всегда входит:
         * работа + разгрузка.
         *
         * После выбора события к этому же циклу добавляется
         * его индивидуальный интервал.
         */
        val dialogView =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.dialog_post_bunker_timer_grace,
                    null
                )

        val textBaseInterval =
            dialogView.findViewById<TextView>(
                R.id.textPostBunkerBaseInterval
            )

        val radioGroup =
            dialogView.findViewById<RadioGroup>(
                R.id.radioGroupPostBunkerState
            )

        val radioContinue =
            dialogView.findViewById<RadioButton>(
                R.id.radioPostBunkerContinue
            )

        val radioLunch =
            dialogView.findViewById<RadioButton>(
                R.id.radioPostBunkerLunch
            )

        val radioDinner =
            dialogView.findViewById<RadioButton>(
                R.id.radioPostBunkerDinner
            )

        val radioWaitingTransport =
            dialogView.findViewById<RadioButton>(
                R.id.radioPostBunkerWaitingTransport
            )

        val textResult =
            dialogView.findViewById<TextView>(
                R.id.textPostBunkerResult
            )

        val buttonConfirm =
            dialogView.findViewById<Button>(
                R.id.buttonConfirmPostBunkerState
            )

        val baseInterval =
            day.workIntervalMinutes

        val unloadInterval =
            day.unloadIntervalMinutes

        radioLunch.text =
            "Обед  (+${day.lunchIntervalMinutes} мин)"

        radioDinner.text =
            "Ужин  (+${day.dinnerIntervalMinutes} мин)"

        radioWaitingTransport.text =
            "Ожидание транспорта  " +
                "(+${day.waitingTransportIntervalMinutes} мин)"

        textBaseInterval.text =
            "Работа: $baseInterval мин • " +
                "Разгрузка: $unloadInterval мин"

        fun selectedGraceMinutes(): Int {
            return when (
                radioGroup.checkedRadioButtonId
            ) {
                R.id.radioPostBunkerLunch ->
                    day.lunchIntervalMinutes

                R.id.radioPostBunkerDinner ->
                    day.dinnerIntervalMinutes

                R.id.radioPostBunkerWaitingTransport ->
                    day.waitingTransportIntervalMinutes

                else ->
                    HarvestReminderManager
                        .GRACE_NONE_MINUTES
            }
        }

        fun selectedPostEventType(): String {
            return when (
                radioGroup.checkedRadioButtonId
            ) {
                R.id.radioPostBunkerLunch ->
                    "LUNCH"

                R.id.radioPostBunkerDinner ->
                    "DINNER"

                R.id.radioPostBunkerWaitingTransport ->
                    "WAITING_TRANSPORT"

                else ->
                    "CONTINUE"
            }
        }

        fun refreshResult() {
            val grace =
                selectedGraceMinutes()

            val total =
                baseInterval +
                    unloadInterval +
                    grace

            textResult.text =
                if (
                    grace >
                    0
                ) {
                    "Следующее напоминание через " +
                        "$baseInterval + $unloadInterval + $grace = " +
                        "$total мин"
                } else {
                    "Следующее напоминание через " +
                        "$baseInterval + $unloadInterval = " +
                        "$total мин"
                }
        }

        val refreshListener =
            android.widget.CompoundButton
                .OnCheckedChangeListener {
                    _,
                    isChecked ->

                    if (
                        isChecked
                    ) {
                        refreshResult()
                    }
                }

        radioContinue.setOnCheckedChangeListener(
            refreshListener
        )

        radioLunch.setOnCheckedChangeListener(
            refreshListener
        )

        radioDinner.setOnCheckedChangeListener(
            refreshListener
        )

        radioWaitingTransport.setOnCheckedChangeListener(
            refreshListener
        )

        radioContinue.isChecked =
            true

        refreshResult()

        val dialog =
            AlertDialog
                .Builder(this)
                .setView(
                    dialogView
                )
                .setCancelable(
                    false
                )
                .create()

        dialog.setCanceledOnTouchOutside(
            false
        )

        buttonConfirm.setOnClickListener {
            val grace =
                selectedGraceMinutes()

            val postEventType =
                selectedPostEventType()

            buttonConfirm.isEnabled =
                false

            Thread {
                try {
                    val dao =
                        DatabaseProvider
                            .getDatabase(
                                applicationContext
                            )
                            .harvestDao()

                    dao.updateBunkerPostEventType(
                        bunkerId =
                            bunkerId,
                        postEventType =
                            postEventType
                    )

                    HarvestReminderManager
                        .syncDaySettings(
                            context =
                                applicationContext,
                            dayId =
                                dayId,
                            workIntervalMinutes =
                                day.workIntervalMinutes,
                            unloadIntervalMinutes =
                                day.unloadIntervalMinutes
                        )

                    HarvestReminderManager
                        .resetAfterBunkerAdded(
                            context =
                                applicationContext,
                            dayId =
                                dayId,
                            graceMinutes =
                                grace,
                            bunkerAddedAtMillis =
                                bunkerAddedAtMillis
                        )

                    runOnUiThread {
                        dialog.dismiss()

                        refreshReminderStatus()
                        loadDay()
                    }

                } catch (
                    e: Throwable
                ) {
                    runOnUiThread {
                        buttonConfirm.isEnabled =
                            true

                        Toast.makeText(
                            this,
                            "Ошибка сохранения события: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.start()
        }

        dialog.show()
    }


    private fun showBunkerDialog(
        bunker: HarvestBunker?
    ) {
        val day =
            currentDay
                ?: return

        if (
            bunker == null &&
            day.status !=
                "ACTIVE"
        ) {
            Toast.makeText(
                this,
                "Новые бункеры можно добавлять только в текущий рабочий день",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val dialogView =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.dialog_add_harvest_bunker,
                    null
                )

        val textDialogTitle =
            dialogView.findViewById<TextView>(
                R.id.textHarvestBunkerDialogTitle
            )

        val textFillTime =
            dialogView.findViewById<TextView>(
                R.id.textHarvestBunkerTime
            )

        val spinnerFill =
            dialogView.findViewById<Spinner>(
                R.id.spinnerHarvestBunkerVolume
            )

        val buttonConfirm =
            dialogView.findViewById<Button>(
                R.id.buttonConfirmHarvestBunker
            )

        val buttonCancel =
            dialogView.findViewById<Button>(
                R.id.buttonCancelHarvestBunker
            )

        textDialogTitle.text =
            if (
                bunker == null
            ) {
                "Добавить бункер"
            } else {
                "Редактировать бункер"
            }

        val timeCalendar =
            Calendar.getInstance()

        if (
            bunker != null
        ) {
            setCalendarTime(
                timeCalendar,
                bunker.fillTime
            )
        }

        fun updateTime() {
            textFillTime.text =
                formatCalendarTime(
                    timeCalendar
                )
        }

        updateTime()

        textFillTime.setOnClickListener {
            showTimePicker(
                timeCalendar
            ) {
                updateTime()
            }
        }

        val keys =
            fillLevelLabels
                .keys
                .toList()

        val labels =
            fillLevelLabels
                .values
                .toList()

        spinnerFill.adapter =
            ArrayAdapter(
                this,
                android.R.layout
                    .simple_spinner_dropdown_item,
                labels
            )

        spinnerFill.setSelection(
            if (
                bunker == null
            ) {
                0
            } else {
                keys.indexOf(
                    bunker.fillLevel
                ).coerceAtLeast(
                    0
                )
            }
        )

        val dialog =
            AlertDialog
                .Builder(this)
                .setView(
                    dialogView
                )
                .create()

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        buttonConfirm.setOnClickListener {
            val fillLevel =
                keys.getOrNull(
                    spinnerFill
                        .selectedItemPosition
                ) ?: "FULL"

            val fillTime =
                textFillTime
                    .text
                    .toString()

            buttonConfirm.isEnabled =
                false

            Thread {
                try {
                    val dao =
                        DatabaseProvider
                            .getDatabase(
                                applicationContext
                            )
                            .harvestDao()

                    var newBunkerAddedAtMillis:
                        Long? =
                            null

                    var newBunkerId:
                        Int? =
                            null

                    if (
                        bunker == null
                    ) {
                        newBunkerId =
                            dao.insertBunker(
                                HarvestBunker(
                                    bunkerUid =
                                        "harvest-bunker-" +
                                            UUID
                                                .randomUUID()
                                                .toString(),
                                    dayId =
                                        day.id,
                                    fillTime =
                                        fillTime,
                                    fillLevel =
                                        fillLevel
                                )
                            ).toInt()

                        /*
                         * С этого момента начинается новый интервал.
                         *
                         * Сразу считаем обычный рабочий процесс (+0),
                         * чтобы:
                         * 1. прекратить цикл 2:30;
                         * 2. убрать текущее уведомление;
                         * 3. не потерять таймер, даже если второе окно
                         *    неожиданно закроется вместе с Activity.
                         *
                         * После выбора события этот же таймер будет
                         * пересчитан от ТОГО ЖЕ bunkerAddedAtMillis.
                         */
                        newBunkerAddedAtMillis =
                            System.currentTimeMillis()

                        HarvestReminderManager
                            .syncDaySettings(
                                context =
                                    applicationContext,
                                dayId =
                                    day.id,
                                workIntervalMinutes =
                                    day.workIntervalMinutes,
                                unloadIntervalMinutes =
                                    day.unloadIntervalMinutes
                            )

                        HarvestReminderManager
                            .resetAfterBunkerAdded(
                                context =
                                    applicationContext,
                                dayId =
                                    day.id,
                                graceMinutes =
                                    HarvestReminderManager
                                        .GRACE_NONE_MINUTES,
                                bunkerAddedAtMillis =
                                    newBunkerAddedAtMillis
                            )

                    } else {
                        dao.updateBunker(
                            bunkerId =
                                bunker.id,
                            fillTime =
                                fillTime,
                            fillLevel =
                                fillLevel
                        )

                        rescheduleReminderAfterHistoryChange(
                            day =
                                day,
                            dao =
                                dao
                        )
                    }

                    runOnUiThread {
                        dialog.dismiss()
                        loadDay()

                        val bunkerAddedAt =
                            newBunkerAddedAtMillis

                        val insertedBunkerId =
                            newBunkerId

                        if (
                            bunkerAddedAt !=
                                null &&
                            insertedBunkerId !=
                                null
                        ) {
                            showPostBunkerTimerGraceDialog(
                                dayId =
                                    day.id,
                                bunkerId =
                                    insertedBunkerId,
                                bunkerAddedAtMillis =
                                    bunkerAddedAt
                            )
                        }
                    }

                } catch (
                    e: Throwable
                ) {
                    runOnUiThread {
                        buttonConfirm.isEnabled =
                            true

                        Toast.makeText(
                            this,
                            "Ошибка сохранения бункера: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.start()
        }

        dialog.show()
    }

    private fun showFinishDayDialog() {
        val day =
            currentDay
                ?: return

        if (
            day.status !=
            "ACTIVE"
        ) {
            return
        }

        val dialogView =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.dialog_finish_harvest_day,
                    null
                )

        val textEndTime =
            dialogView.findViewById<TextView>(
                R.id.textFinishHarvestDayTime
            )

        val buttonConfirm =
            dialogView.findViewById<Button>(
                R.id.buttonConfirmFinishHarvestDay
            )

        val buttonCancel =
            dialogView.findViewById<Button>(
                R.id.buttonCancelFinishHarvestDay
            )

        val timeCalendar =
            Calendar.getInstance()

        fun updateTime() {
            textEndTime.text =
                formatCalendarTime(
                    timeCalendar
                )
        }

        updateTime()

        textEndTime.setOnClickListener {
            showTimePicker(
                timeCalendar
            ) {
                updateTime()
            }
        }

        val dialog =
            AlertDialog
                .Builder(this)
                .setView(
                    dialogView
                )
                .create()

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        buttonConfirm.setOnClickListener {
            val endTime =
                textEndTime
                    .text
                    .toString()

            dialog.dismiss()

            showCampaignFinishedDialog(
                day,
                endTime
            )
        }

        dialog.show()
    }

    private fun showCampaignFinishedDialog(
        day: HarvestDay,
        endTime: String
    ) {
        AlertDialog
            .Builder(this)
            .setTitle(
                "Уборочная кампания окончена?"
            )
            .setMessage(
                "Рабочий день будет завершён."
            )
            .setPositiveButton(
                "Да"
            ) { _, _ ->
                finishDay(
                    day,
                    endTime,
                    true
                )
            }
            .setNegativeButton(
                "Нет"
            ) { _, _ ->
                finishDay(
                    day,
                    endTime,
                    false
                )
            }
            .setNeutralButton(
                "Отмена",
                null
            )
            .show()
    }

    private fun finishDay(
        day: HarvestDay,
        endTime: String,
        finishProcess: Boolean
    ) {
        Thread {
            try {
                val database =
                    DatabaseProvider
                        .getDatabase(
                            applicationContext
                        )

                database.runInTransaction {
                    database
                        .harvestDao()
                        .completeDay(
                            day.id,
                            endTime
                        )

                    if (
                        finishProcess
                    ) {
                        database
                            .harvestDao()
                            .completeProcess(
                                day.processId,
                                day.workDate
                            )
                    }
                }

                runOnUiThread {
                    HarvestReminderManager
                        .cancelForFinishedDay(
                            this,
                            day.id
                        )

                    Toast.makeText(
                        this,
                        if (
                            finishProcess
                        ) {
                            "Уборочный процесс завершён"
                        } else {
                            "Рабочий день завершён"
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }

            } catch (
                e: Throwable
            ) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Ошибка завершения рабочего дня: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun showTimePicker(
        calendar: Calendar,
        onChanged: () -> Unit
    ) {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(
                    Calendar.HOUR_OF_DAY,
                    hour
                )

                calendar.set(
                    Calendar.MINUTE,
                    minute
                )

                onChanged()
            },
            calendar.get(
                Calendar.HOUR_OF_DAY
            ),
            calendar.get(
                Calendar.MINUTE
            ),
            true
        ).show()
    }

    private fun formatCalendarTime(
        calendar: Calendar
    ): String {
        return String.format(
            Locale.US,
            "%02d:%02d",
            calendar.get(
                Calendar.HOUR_OF_DAY
            ),
            calendar.get(
                Calendar.MINUTE
            )
        )
    }

    private fun setCalendarTime(
        calendar: Calendar,
        value: String
    ) {
        try {
            val parts =
                value.split(":")

            if (
                parts.size == 2
            ) {
                calendar.set(
                    Calendar.HOUR_OF_DAY,
                    parts[0].toInt()
                )

                calendar.set(
                    Calendar.MINUTE,
                    parts[1].toInt()
                )
            }

        } catch (
            _: Exception
        ) {
        }
    }

    private fun minutesBetween(
        first: String,
        second: String
    ): Int? {
        return try {
            val firstParts =
                first.split(":")

            val secondParts =
                second.split(":")

            if (
                firstParts.size != 2 ||
                secondParts.size != 2
            ) {
                null

            } else {
                val firstMinutes =
                    firstParts[0].toInt() *
                        60 +
                        firstParts[1].toInt()

                val secondMinutes =
                    secondParts[0].toInt() *
                        60 +
                        secondParts[1].toInt()

                val difference =
                    secondMinutes -
                        firstMinutes

                if (
                    difference < 0
                ) {
                    null
                } else {
                    difference
                }
            }

        } catch (
            _: Exception
        ) {
            null
        }
    }

    private fun coefficientRange(
        fillLevel: String
    ): Pair<Double, Double> {
        return when (
            fillLevel
        ) {
            "FULL" ->
                1.0 to 1.0

            "HALF" ->
                0.5 to 0.5

            "MORE_THAN_HALF" ->
                0.5 to 1.0

            "LESS_THAN_HALF" ->
                0.0 to 0.5

            else ->
                0.0 to 0.0
        }
    }

    private fun totalVolumeRange(
        bunkerVolumeM3: Double?,
        bunkers: List<HarvestBunker>
    ): Pair<Double, Double>? {
        if (
            bunkerVolumeM3 ==
            null
        ) {
            return null
        }

        var minEquivalent =
            0.0

        var maxEquivalent =
            0.0

        bunkers.forEach {
            bunker ->

            val range =
                coefficientRange(
                    bunker.fillLevel
                )

            minEquivalent +=
                range.first

            maxEquivalent +=
                range.second
        }

        return (
            minEquivalent *
                bunkerVolumeM3
            ) to (
            maxEquivalent *
                bunkerVolumeM3
            )
    }

    private fun calculationText(
        label: String,
        bunkerVolumeM3: Double?,
        bunkers: List<HarvestBunker>
    ): String {
        val range =
            totalVolumeRange(
                bunkerVolumeM3,
                bunkers
            )
                ?: return "$label: не настроен"

        return if (
            kotlin.math.abs(
                range.first -
                    range.second
            ) <
            0.0001
        ) {
            "$label: ${formatNumber(range.first)} м³"
        } else {
            "$label: ${formatNumber(range.first)}–" +
                "${formatNumber(range.second)} м³"
        }
    }

    private fun averageText(
        bunkerVolumeM3: Double?,
        bunkers: List<HarvestBunker>
    ): String {
        if (
            bunkers.isEmpty()
        ) {
            return "Средний объём бункера: —"
        }

        val range =
            totalVolumeRange(
                bunkerVolumeM3,
                bunkers
            )
                ?: return "Средний объём бункера: не настроен"

        val minAverage =
            range.first /
                bunkers.size

        val maxAverage =
            range.second /
                bunkers.size

        return if (
            kotlin.math.abs(
                minAverage -
                    maxAverage
            ) <
            0.0001
        ) {
            "Средний объём бункера: " +
                "${formatNumber(minAverage)} м³"
        } else {
            "Средний объём бункера: " +
                "${formatNumber(minAverage)}–" +
                "${formatNumber(maxAverage)} м³"
        }
    }

    private fun formatNumber(
        value: Double
    ): String {
        return String.format(
            Locale("ru"),
            "%.2f",
            value
        )
            .trimEnd('0')
            .trimEnd(',')
    }

    private fun minuteWord(
        value: Int
    ): String {
        val lastTwo =
            value % 100

        val last =
            value % 10

        return if (
            lastTwo in 11..14
        ) {
            "минут"
        } else {
            when (
                last
            ) {
                1 ->
                    "минута"

                2, 3, 4 ->
                    "минуты"

                else ->
                    "минут"
            }
        }
    }

    private fun bunkerCountText(
        count: Int
    ): String {
        val lastTwo =
            count % 100

        val last =
            count % 10

        val word =
            if (
                lastTwo in 11..14
            ) {
                "бункеров"
            } else {
                when (last) {
                    1 ->
                        "бункер"

                    2, 3, 4 ->
                        "бункера"

                    else ->
                        "бункеров"
                }
            }

        return "$count $word"
    }

    private fun parseDateToCalendar(
        value: String
    ): Calendar {
        val calendar =
            Calendar.getInstance()

        try {
            val date =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).parse(
                    value
                )

            if (
                date != null
            ) {
                calendar.time =
                    date
            }

        } catch (
            _: Exception
        ) {
        }

        return calendar
    }

    private fun formatDateForUi(
        value: String
    ): String {
        return try {
            val source =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                )

            val target =
                SimpleDateFormat(
                    "dd.MM.yyyy",
                    Locale("ru")
                )

            val date =
                source.parse(
                    value
                )

            if (
                date == null
            ) {
                value
            } else {
                target.format(
                    date
                )
            }

        } catch (
            _: Exception
        ) {
            value
        }
    }

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources
                    .displayMetrics
                    .density
            ).toInt()
    }
}
