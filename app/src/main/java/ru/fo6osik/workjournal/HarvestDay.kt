package ru.fo6osik.workjournal

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "harvest_days",
    foreignKeys = [
        ForeignKey(
            entity = HarvestProcess::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dayUid"], unique = true),
        Index(value = ["processId"]),
        Index(value = ["workDate"]),
        Index(
            value = ["processId", "workDate"],
            unique = true
        )
    ]
)
data class HarvestDay(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val dayUid: String,

    val processId: Int,

    // YYYY-MM-DD.
    // Рабочие дни могут идти с пропусками.
    val workDate: String,

    // HH:mm
    val startTime: String,

    // HH:mm; null = рабочий день ещё идёт.
    val endTime: String? = null,

    // ACTIVE / COMPLETED
    val status: String = "ACTIVE",

    /*
     * Настройки напоминаний именно этого рабочего дня.
     *
     * Новый рабочий день по умолчанию:
     * работа 30 мин + разгрузка 5 мин.
     */
    @ColumnInfo(defaultValue = "30")
    val workIntervalMinutes: Int = 30,

    @ColumnInfo(defaultValue = "5")
    val unloadIntervalMinutes: Int = 5,

    @ColumnInfo(defaultValue = "15")
    val lunchIntervalMinutes: Int = 15,

    @ColumnInfo(defaultValue = "15")
    val dinnerIntervalMinutes: Int = 15,

    @ColumnInfo(defaultValue = "30")
    val waitingTransportIntervalMinutes: Int = 30,

    /*
     * Флаги нужны, чтобы при повторном открытии настроек
     * приложение помнило: был выбран готовый вариант
     * или пункт «Своё значение».
     */
    @ColumnInfo(defaultValue = "0")
    val workIntervalCustom: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    val unloadIntervalCustom: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    val lunchIntervalCustom: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    val dinnerIntervalCustom: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    val waitingTransportIntervalCustom: Boolean = false,

    /*
     * У старых дней после миграции v19 -> v20 = false.
     * При первом открытии такого дня переносим старый
     * основной интервал из SharedPreferences в БД.
     *
     * У новых дней Room получает true из конструктора.
     */
    @ColumnInfo(defaultValue = "0")
    val reminderSettingsInitialized: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
)
