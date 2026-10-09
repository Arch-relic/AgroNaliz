package ru.fo6osik.workjournal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "farm_events",
    indices = [
        Index(
            value = ["eventUid"],
            unique = true
        ),
        Index(
            value = ["targetModule"]
        ),
        Index(
            value = ["eventYear", "eventMonth"]
        )
    ]
)
data class FarmEvent(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /*
     * Постоянный уникальный идентификатор события.
     * Не меняется при экспорте/импорте.
     */
    val eventUid: String,

    /*
     * Например:
     * PURCHASE
     * REPAIR
     * WEATHER
     * FIELD_WORK
     */
    val eventType: String,

    /*
     * Раздел, с которым связано событие.
     *
     * Например:
     * MATERIALS
     * VEHICLES
     * FIELDS
     */
    val targetModule: String,

    /*
     * Короткое название события.
     */
    val title: String,

    /*
     * Подробное описание.
     */
    val description: String? = null,

    /*
     * Дата хранится раздельно специально,
     * чтобы не придумывать точный день,
     * если известен только месяц.
     */
    val eventYear: Int,

    val eventMonth: Int? = null,

    val eventDay: Int? = null,

    /*
     * DAY   — известна точная дата
     * MONTH — известен только месяц
     * YEAR  — известен только год
     */
    val datePrecision: String,

    /*
     * Структурированные дополнительные данные.
     *
     * Например для закупки:
     * материал, масса, количество упаковок,
     * норма внесения и т.д.
     */
    val payloadJson: String? = null,

    /*
     * В будущем сюда можно будет записать UID
     * конкретной записи целевого раздела.
     *
     * Например запись партии удобрения.
     */
    val targetRecordUid: String? = null,

    val createdAt: Long = System.currentTimeMillis()
)