package ru.fo6osik.workjournal

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

object DataImportManager {

    private const val SUPPORTED_BACKUP_FORMAT_VERSION = 1

    data class ImportSummary(
        val added: Int,
        val skipped: Int
    )

    private data class Counter(
        var added: Int = 0,
        var skipped: Int = 0
    ) {
        fun added() {
            added++
        }

        fun skipped() {
            skipped++
        }
    }

    suspend fun importData(
        context: Context,
        uri: Uri
    ): Result<ImportSummary> = withContext(Dispatchers.IO) {

        runCatching {

            val inputStream =
                context.contentResolver
                    .openInputStream(uri)
                    ?: error("Не удалось открыть файл")

            val jsonText =
                inputStream
                    .bufferedReader()
                    .use { it.readText() }

            val root =
                JSONObject(jsonText)

            val backupFormatVersion =
                root.getInt("backupFormatVersion")

            if (
                backupFormatVersion !=
                SUPPORTED_BACKUP_FORMAT_VERSION
            ) {
                error(
                    "Неподдерживаемая версия файла: " +
                            backupFormatVersion
                )
            }

            val roomDatabase =
                DatabaseProvider.getDatabase(context)

            val database =
                roomDatabase.openHelper.writableDatabase

            val sourceDatabaseVersion =
                root.getInt("databaseVersion")

            if (
                sourceDatabaseVersion != 16 &&
                sourceDatabaseVersion != 18 &&
                sourceDatabaseVersion != 19 &&
                sourceDatabaseVersion != database.version
            ) {
                error(
                    "Версия базы в файле: " +
                            sourceDatabaseVersion +
                            ", текущая версия базы: " +
                            database.version
                )
            }

            val tables =
                root.getJSONObject("tables")

            validateRequiredTables(
                tables,
                sourceDatabaseVersion
            )

            val counter =
                Counter()

            database.beginTransaction()

            try {

                /*
                 * 1. Двигатели.
                 *
                 * Они используются автомобилями,
                 * тракторами, комбайнами и техникой.
                 */
                val engineMap =
                    importEngines(
                        database,
                        tables.getJSONArray("car_engines"),
                        counter
                    )

                /*
                 * 2. Автомобильный справочник.
                 */
                val carBrandMap =
                    importBrands(
                        database,
                        tables.getJSONArray("car_brands"),
                        "car_brands",
                        counter
                    )

                val carModelMap =
                    importModels(
                        database,
                        tables.getJSONArray("car_models"),
                        "car_models",
                        carBrandMap,
                        counter
                    )

                importModelEngineLinks(
                    database,
                    tables.getJSONArray(
                        "car_model_engines"
                    ),
                    "car_model_engines",
                    carModelMap,
                    engineMap,
                    counter
                )

                /*
                 * 3. Справочник комбайнов.
                 */
                val combineBrandMap =
                    importBrands(
                        database,
                        tables.getJSONArray(
                            "combine_brands"
                        ),
                        "combine_brands",
                        counter
                    )

                val combineModelMap =
                    importModels(
                        database,
                        tables.getJSONArray(
                            "combine_models"
                        ),
                        "combine_models",
                        combineBrandMap,
                        counter
                    )

                importModelEngineLinks(
                    database,
                    tables.getJSONArray(
                        "combine_model_engines"
                    ),
                    "combine_model_engines",
                    combineModelMap,
                    engineMap,
                    counter
                )

                /*
                 * 4. Справочник тракторов.
                 */
                val tractorBrandMap =
                    importBrands(
                        database,
                        tables.getJSONArray(
                            "tractor_brands"
                        ),
                        "tractor_brands",
                        counter
                    )

                val tractorModelMap =
                    importTractorModels(
                        database,
                        tables.getJSONArray(
                            "tractor_models"
                        ),
                        tractorBrandMap,
                        counter
                    )

                importModelEngineLinks(
                    database,
                    tables.getJSONArray(
                        "tractor_model_engines"
                    ),
                    "tractor_model_engines",
                    tractorModelMap,
                    engineMap,
                    counter
                )

                /*
                 * 5. Типы работ.
                 */
                val workTypeMap =
                    importWorkTypes(
                        database,
                        tables.getJSONArray(
                            "work_types"
                        ),
                        counter
                    )

                /*
                 * 6. Поля.
                 */
                val fieldMap =
                    importFields(
                        database,
                        tables.getJSONArray(
                            "fields"
                        ),
                        counter
                    )
					
                                    

                /*
                 * 6.1. Агрегаты.
                 *
                 * aggregateUid — постоянный идентификатор.
                 * Родительские связи восстанавливаются вторым проходом.
                 */
                val aggregateMap =
                    importAggregates(
                        database,
                        tables.getJSONArray(
                            "aggregates"
                        ),
                        counter
                    )
val seasonProcessMap =
                    importSeasonProcesses(
                        database,
                        tables.getJSONArray(
                            "season_processes"
                        ),
                        fieldMap,
                        counter
                    )


                importFarmEvents(
                        database,
                        tables.getJSONArray(
                            "farm_events"
                        ),
                        counter
                    )

                    /*
                     * Связи общего события с полями.
                     * Поля уже импортированы/сопоставлены,
                     * события уже существуют.
                     */
                    importFarmEventFields(
                        database,
                        tables.getJSONArray(
                            "farm_event_fields"
                        ),
                        fieldMap,
                        counter
                    )

                /*
                 * 7. Техника.
                 */
                val vehicleMap =
                    importVehicles(
                        database,
                        tables.getJSONArray(
                            "vehicles"
                        ),
                        engineMap,
                        counter
                    )


                if (
                    sourceDatabaseVersion >= 18
                ) {
                    importCombineSpecs(
                        database,
                        tables.optJSONArray(
                            "combine_specs"
                        ) ?: JSONArray(),
                        vehicleMap,
                        counter
                    )
                }

                /*
                 * 7.1. Уборка урожая.
                 *
                 * Для старых файлов БД v16 этих таблиц ещё нет,
                 * поэтому используем пустые массивы.
                 */
                val harvestProcessMap =
                    importHarvestProcesses(
                        database,
                        tables.optJSONArray(
                            "harvest_processes"
                        ) ?: JSONArray(),
                        fieldMap,
                        vehicleMap,
                        counter
                    )

                val harvestDayMap =
                    importHarvestDays(
                        database,
                        tables.optJSONArray(
                            "harvest_days"
                        ) ?: JSONArray(),
                        harvestProcessMap,
                        counter
                    )

                importHarvestBunkers(
                    database,
                    tables.optJSONArray(
                        "harvest_bunkers"
                    ) ?: JSONArray(),
                    harvestDayMap,
                    counter
                )

                /*
                 * 8. История ТО.
                 */
                importMaintenanceRecords(
                    database,
                    tables.getJSONArray(
                        "maintenance_records"
                    ),
                    vehicleMap,
                    counter
                )

                /*
                 * События MAINTENANCE / REPAIR, в payloadJson
                 * которых указана конкретная техника,
                 * автоматически материализуем в карточке техники.
                 */
                syncMaintenanceEvents(
                    database,
                    tables.getJSONArray(
                        "farm_events"
                    ),
                    vehicleMap,
                    counter
                )

                /*
                 * 9. Сезонные работы.
                 */
                val seasonWorkMap =
                    importSeasonWorks(
                        database,
                        tables.getJSONArray(
                            "season_works"
                        ),
                        fieldMap,
                        workTypeMap,
                        seasonProcessMap,
                        counter
                    )

                /*
                 * 10. Дочерние дни/этапы каскадных работ.
                 */
                importSeasonWorkDays(
                    database,
                    tables.getJSONArray(
                        "season_work_days"
                    ),
                    seasonWorkMap,
                    counter
                )

                /*
                 * 11. Связи:
                 * сезонная работа -> техника.
                 */
                importSeasonWorkVehicles(
                    database,
                    tables.getJSONArray(
                        "season_work_vehicles"
                    ),
                    seasonWorkMap,
                    vehicleMap,
                    counter
                )


                /*
                 * 12. Связи событий с агрегатами.
                 */
                importFarmEventAggregates(
                    database,
                    tables.getJSONArray(
                        "farm_event_aggregates"
                    ),
                    aggregateMap,
                    counter
                )

                /*
                 * 13. Связи событий с техникой.
                 */
                importFarmEventVehicles(
                    database,
                    tables.getJSONArray(
                        "farm_event_vehicles"
                    ),
                    vehicleMap,
                    counter
                )

                /*
                 * 14. Связи событий с рабочими процессами.
                 */
                importFarmEventProcesses(
                    database,
                    tables.getJSONArray(
                        "farm_event_processes"
                    ),
                    seasonProcessMap,
                    counter
                )

                /*
                 * 15. Связи сезонных операций с агрегатами.
                 */
                importSeasonWorkAggregates(
                    database,
                    tables.getJSONArray(
                        "season_work_aggregates"
                    ),
                    seasonWorkMap,
                    aggregateMap,
                    counter
                )

                database.setTransactionSuccessful()

            } finally {

                database.endTransaction()
            }

            ImportSummary(
                added = counter.added,
                skipped = counter.skipped
            )
        }
    }
	
	private fun importFarmEvents(
    database: SupportSQLiteDatabase,
    rows: JSONArray,
    counter: Counter
) {

    for (index in 0 until rows.length()) {

        val row =
            rows.getJSONObject(index)

        val eventUid =
            row.getString("eventUid")

        val exists =
            queryExists(
                database,
                """
                SELECT 1
                FROM farm_events
                WHERE eventUid = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(eventUid)
            )

        if (exists) {

            counter.skipped()

            continue
        }

        val values =
            ContentValues().apply {

                put(
                    "eventUid",
                    eventUid
                )

                put(
                    "eventType",
                    row.getString("eventType")
                )

                put(
                    "targetModule",
                    row.getString("targetModule")
                )

                put(
                    "title",
                    row.getString("title")
                )

                putNullableString(
                    this,
                    "description",
                    jsonNullableString(
                        row,
                        "description"
                    )
                )

                put(
                    "eventYear",
                    row.getInt("eventYear")
                )

                val month =
                    jsonNullableInt(
                        row,
                        "eventMonth"
                    )

                if (month == null) {
                    putNull("eventMonth")
                } else {
                    put("eventMonth", month)
                }

                val day =
                    jsonNullableInt(
                        row,
                        "eventDay"
                    )

                if (day == null) {
                    putNull("eventDay")
                } else {
                    put("eventDay", day)
                }

                put(
                    "datePrecision",
                    row.getString(
                        "datePrecision"
                    )
                )

                putNullableString(
                    this,
                    "payloadJson",
                    jsonNullableString(
                        row,
                        "payloadJson"
                    )
                )

                putNullableString(
                    this,
                    "targetRecordUid",
                    jsonNullableString(
                        row,
                        "targetRecordUid"
                    )
                )

                put(
                    "createdAt",
                    row.getLong("createdAt")
                )
            }

        insertAndGetId(
            database,
            "farm_events",
            values
        )

        counter.added()
    }
}


    private fun importFarmEventFields(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        fieldMap: Map<Long, Long>,
        counter: Counter
    ) {

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val eventUid =
                row.getString(
                    "eventUid"
                )

            val oldFieldId =
                row.getLong(
                    "fieldId"
                )

            val newFieldId =
                fieldMap[oldFieldId]
                    ?: error(
                        "Не найдено поле для связи события: " +
                            "eventUid=$eventUid, fieldId=$oldFieldId"
                    )

            val eventExists =
                queryExists(
                    database,
                    """
                    SELECT 1
                    FROM farm_events
                    WHERE eventUid = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        eventUid
                    )
                )

            if (
                !eventExists
            ) {
                error(
                    "Не найдено событие для связи с полем: " +
                        eventUid
                )
            }

            val exists =
                queryExists(
                    database,
                    """
                    SELECT 1
                    FROM farm_event_fields
                    WHERE eventUid = ?
                      AND fieldId = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        eventUid,
                        newFieldId
                    )
                )

            if (
                exists
            ) {

                counter.skipped()

                continue
            }

            val values =
                ContentValues().apply {

                    put(
                        "eventUid",
                        eventUid
                    )

                    put(
                        "fieldId",
                        newFieldId
                    )
                }

            insertAndGetId(
                database,
                "farm_event_fields",
                values
            )

            counter.added()
        }
    }

    private fun validateRequiredTables(
        tables: JSONObject,
        sourceDatabaseVersion: Int
    ) {

        val requiredTables =
            mutableListOf(
                "car_brands",
                "car_engines",
                "car_model_engines",
                "car_models",
                "combine_brands",
                "combine_model_engines",
                "combine_models",
                "fields",
                "maintenance_records",
                "season_work_vehicles",
                "season_processes",
                "season_works",
                "season_work_days",
                "tractor_brands",
                "tractor_model_engines",
                "tractor_models",
                "vehicles",
                "work_types",
                "farm_events",
                "farm_event_fields",
                "aggregates",
                "farm_event_aggregates",
                "farm_event_vehicles",
                "farm_event_processes",
                "season_work_aggregates"
            )

        if (sourceDatabaseVersion >= 17) {
            requiredTables.addAll(
                listOf(
                    "harvest_processes",
                    "harvest_days",
                    "harvest_bunkers"
                )
            )
        }


        if (sourceDatabaseVersion >= 18) {
            requiredTables.add(
                "combine_specs"
            )
        }

        for (tableName in requiredTables) {

            if (
                tables.optJSONArray(tableName) == null
            ) {
                error(
                    "В файле отсутствует таблица: $tableName"
                )
            }
        }
    }


    /*
     * ---------------------------------------------------------
     * АГРЕГАТЫ
     * ---------------------------------------------------------
     */

    private fun importAggregates(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        counter: Counter
    ): Map<Long, Long> {

        val idMap =
            mutableMapOf<Long, Long>()

        val newlyInserted =
            mutableSetOf<Long>()

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            val aggregateUid =
                row.getString("aggregateUid")

            val existingId =
                querySingleId(
                    database,
                    """
                    SELECT id
                    FROM aggregates
                    WHERE aggregateUid = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        aggregateUid
                    )
                )

            val newId =
                if (
                    existingId != null
                ) {

                    counter.skipped()

                    existingId

                } else {

                    val values =
                        ContentValues().apply {

                            put(
                                "aggregateUid",
                                aggregateUid
                            )

                            put(
                                "name",
                                row.getString(
                                    "name"
                                )
                            )

                            put(
                                "category",
                                row.getString(
                                    "category"
                                )
                            )

                            putNull(
                                "parentAggregateId"
                            )

                            val workingWidthM =
                                jsonNullableDouble(
                                    row,
                                    "workingWidthM"
                                )

                            if (
                                workingWidthM == null
                            ) {
                                putNull(
                                    "workingWidthM"
                                )
                            } else {
                                put(
                                    "workingWidthM",
                                    workingWidthM
                                )
                            }

                            put(
                                "note",
                                row.optString(
                                    "note",
                                    ""
                                )
                            )

                            put(
                                "isActive",
                                row.optInt(
                                    "isActive",
                                    1
                                )
                            )
                        }

                    insertAndGetId(
                        database,
                        "aggregates",
                        values
                    ).also {

                        newlyInserted.add(
                            oldId
                        )

                        counter.added()
                    }
                }

            idMap[oldId] =
                newId
        }

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            if (
                oldId !in newlyInserted
            ) {
                continue
            }

            val oldParentId =
                jsonNullableLong(
                    row,
                    "parentAggregateId"
                )

            if (
                oldParentId == null
            ) {
                continue
            }

            val newId =
                idMap[oldId]
                    ?: continue

            val newParentId =
                idMap[oldParentId]
                    ?: error(
                        "Не найден родительский агрегат: " +
                            "aggregateId=$oldId, parentAggregateId=$oldParentId"
                    )

            database.execSQL(
                """
                UPDATE aggregates
                SET parentAggregateId = ?
                WHERE id = ?
                """.trimIndent(),
                arrayOf(
                    newParentId,
                    newId
                )
            )
        }

        return idMap
    }

    /*
     * ---------------------------------------------------------
     * СОБЫТИЕ -> АГРЕГАТ
     * ---------------------------------------------------------
     */

    private fun importFarmEventAggregates(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        aggregateMap: Map<Long, Long>,
        counter: Counter
    ) {

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val eventUid =
                row.getString(
                    "eventUid"
                )

            val newAggregateId =
                aggregateMap[
                    row.getLong(
                        "aggregateId"
                    )
                ] ?: error(
                    "Не найден агрегат для события: $eventUid"
                )

            if (
                !queryExists(
                    database,
                    """
                    SELECT 1
                    FROM farm_events
                    WHERE eventUid = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        eventUid
                    )
                )
            ) {
                error(
                    "Не найдено событие для связи с агрегатом: $eventUid"
                )
            }

            val exists =
                queryExists(
                    database,
                    """
                    SELECT 1
                    FROM farm_event_aggregates
                    WHERE eventUid = ?
                      AND aggregateId = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        eventUid,
                        newAggregateId
                    )
                )

            if (
                exists
            ) {

                counter.skipped()

                continue
            }

            val values =
                ContentValues().apply {

                    put(
                        "eventUid",
                        eventUid
                    )

                    put(
                        "aggregateId",
                        newAggregateId
                    )

                    put(
                        "relationType",
                        row.optString(
                            "relationType",
                            "RELATED"
                        )
                    )

                    putNullableString(
                        this,
                        "note",
                        jsonNullableString(
                            row,
                            "note"
                        )
                    )
                }

            insertAndGetId(
                database,
                "farm_event_aggregates",
                values
            )

            counter.added()
        }
    }

    /*
     * ---------------------------------------------------------
     * СОБЫТИЕ -> ТЕХНИКА
     * ---------------------------------------------------------
     */

    private fun importFarmEventVehicles(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        vehicleMap: Map<Long, Long>,
        counter: Counter
    ) {

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val eventUid =
                row.getString(
                    "eventUid"
                )

            val newVehicleId =
                vehicleMap[
                    row.getLong(
                        "vehicleId"
                    )
                ] ?: error(
                    "Не найдена техника для события: $eventUid"
                )

            if (
                !queryExists(
                    database,
                    """
                    SELECT 1
                    FROM farm_events
                    WHERE eventUid = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        eventUid
                    )
                )
            ) {
                error(
                    "Не найдено событие для связи с техникой: $eventUid"
                )
            }

            val exists =
                queryExists(
                    database,
                    """
                    SELECT 1
                    FROM farm_event_vehicles
                    WHERE eventUid = ?
                      AND vehicleId = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        eventUid,
                        newVehicleId
                    )
                )

            if (
                exists
            ) {

                counter.skipped()

                continue
            }

            val values =
                ContentValues().apply {

                    put(
                        "eventUid",
                        eventUid
                    )

                    put(
                        "vehicleId",
                        newVehicleId
                    )

                    put(
                        "relationType",
                        row.optString(
                            "relationType",
                            "RELATED"
                        )
                    )

                    putNullableString(
                        this,
                        "note",
                        jsonNullableString(
                            row,
                            "note"
                        )
                    )
                }

            insertAndGetId(
                database,
                "farm_event_vehicles",
                values
            )

            counter.added()
        }
    }

    /*
     * ---------------------------------------------------------
     * СОБЫТИЕ -> РАБОЧИЙ ПРОЦЕСС
     * ---------------------------------------------------------
     */

    private fun importFarmEventProcesses(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        seasonProcessMap: Map<Long, Long>,
        counter: Counter
    ) {

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val eventUid =
                row.getString(
                    "eventUid"
                )

            val newProcessId =
                seasonProcessMap[
                    row.getLong(
                        "processId"
                    )
                ] ?: error(
                    "Не найден рабочий процесс для события: $eventUid"
                )

            if (
                !queryExists(
                    database,
                    """
                    SELECT 1
                    FROM farm_events
                    WHERE eventUid = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        eventUid
                    )
                )
            ) {
                error(
                    "Не найдено событие для связи с процессом: $eventUid"
                )
            }

            val exists =
                queryExists(
                    database,
                    """
                    SELECT 1
                    FROM farm_event_processes
                    WHERE eventUid = ?
                      AND processId = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        eventUid,
                        newProcessId
                    )
                )

            if (
                exists
            ) {

                counter.skipped()

                continue
            }

            val values =
                ContentValues().apply {

                    put(
                        "eventUid",
                        eventUid
                    )

                    put(
                        "processId",
                        newProcessId
                    )
                }

            insertAndGetId(
                database,
                "farm_event_processes",
                values
            )

            counter.added()
        }
    }

    /*
     * ---------------------------------------------------------
     * СЕЗОННАЯ ОПЕРАЦИЯ -> АГРЕГАТ
     * ---------------------------------------------------------
     */

    private fun importSeasonWorkAggregates(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        seasonWorkMap: Map<Long, Long>,
        aggregateMap: Map<Long, Long>,
        counter: Counter
    ) {

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val newSeasonWorkId =
                seasonWorkMap[
                    row.getLong(
                        "seasonWorkId"
                    )
                ] ?: error(
                    "Не найдена сезонная операция для связи с агрегатом"
                )

            val newAggregateId =
                aggregateMap[
                    row.getLong(
                        "aggregateId"
                    )
                ] ?: error(
                    "Не найден агрегат для сезонной операции"
                )

            val exists =
                queryExists(
                    database,
                    """
                    SELECT 1
                    FROM season_work_aggregates
                    WHERE seasonWorkId = ?
                      AND aggregateId = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        newSeasonWorkId,
                        newAggregateId
                    )
                )

            if (
                exists
            ) {

                counter.skipped()

                continue
            }

            val values =
                ContentValues().apply {

                    put(
                        "seasonWorkId",
                        newSeasonWorkId
                    )

                    put(
                        "aggregateId",
                        newAggregateId
                    )

                    put(
                        "role",
                        row.optString(
                            "role",
                            "PRIMARY"
                        )
                    )

                    putNullableString(
                        this,
                        "note",
                        jsonNullableString(
                            row,
                            "note"
                        )
                    )
                }

            insertAndGetId(
                database,
                "season_work_aggregates",
                values
            )

            counter.added()
        }
    }

    /*
     * ---------------------------------------------------------
     * БРЕНДЫ
     * ---------------------------------------------------------
     */

    private fun importBrands(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        tableName: String,
        counter: Counter
    ): Map<Long, Long> {

        val idMap =
            mutableMapOf<Long, Long>()

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            val name =
                row.getString("name")

            val existingId =
                querySingleId(
                    database,
                    """
                    SELECT id
                    FROM `$tableName`
                    WHERE name = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(name)
                )

            val newId =
                if (existingId != null) {

                    counter.skipped()

                    existingId

                } else {

                    val values =
                        ContentValues().apply {

                            put(
                                "name",
                                name
                            )

                            put(
                                "userCreated",
                                row.optInt(
                                    "userCreated",
                                    0
                                )
                            )
                        }

                    insertAndGetId(
                        database,
                        tableName,
                        values
                    ).also {
                        counter.added()
                    }
                }

            idMap[oldId] =
                newId
        }

        return idMap
    }

    /*
     * ---------------------------------------------------------
     * ОБЫЧНЫЕ МОДЕЛИ
     * car_models / combine_models
     * ---------------------------------------------------------
     */

    private fun importModels(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        tableName: String,
        brandMap: Map<Long, Long>,
        counter: Counter
    ): Map<Long, Long> {

        val idMap =
            mutableMapOf<Long, Long>()

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            val oldBrandId =
                row.getLong("brandId")

            val newBrandId =
                brandMap[oldBrandId]
                    ?: error(
                        "Не найдена марка для модели"
                    )

            val name =
                row.getString("name")

            val existingId =
                querySingleId(
                    database,
                    """
                    SELECT id
                    FROM `$tableName`
                    WHERE brandId = ?
                      AND name = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        newBrandId,
                        name
                    )
                )

            val newId =
                if (existingId != null) {

                    counter.skipped()

                    existingId

                } else {

                    val values =
                        ContentValues().apply {

                            put(
                                "brandId",
                                newBrandId
                            )

                            put(
                                "name",
                                name
                            )

                            put(
                                "userCreated",
                                row.optInt(
                                    "userCreated",
                                    0
                                )
                            )
                        }

                    insertAndGetId(
                        database,
                        tableName,
                        values
                    ).also {
                        counter.added()
                    }
                }

            idMap[oldId] =
                newId
        }

        return idMap
    }

    /*
     * ---------------------------------------------------------
     * ТРАКТОРНЫЕ МОДЕЛИ
     *
     * Они дополнительно имеют baseModelId,
     * поэтому импорт идёт в два прохода.
     * ---------------------------------------------------------
     */

    private fun importTractorModels(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        brandMap: Map<Long, Long>,
        counter: Counter
    ): Map<Long, Long> {

        val idMap =
            mutableMapOf<Long, Long>()

        val newlyInserted =
            mutableSetOf<Long>()

        /*
         * Первый проход:
         * создаём/находим все модели.
         */
        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            val newBrandId =
                brandMap[
                    row.getLong("brandId")
                ] ?: error(
                    "Не найдена марка трактора"
                )

            val name =
                row.getString("name")

            val existingId =
                querySingleId(
                    database,
                    """
                    SELECT id
                    FROM tractor_models
                    WHERE brandId = ?
                      AND name = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        newBrandId,
                        name
                    )
                )

            if (existingId != null) {

                idMap[oldId] =
                    existingId

                counter.skipped()

            } else {

                val values =
                    ContentValues().apply {

                        put(
                            "brandId",
                            newBrandId
                        )

                        put(
                            "name",
                            name
                        )

                        put(
                            "userCreated",
                            row.optInt(
                                "userCreated",
                                0
                            )
                        )

                        putNull(
                            "baseModelId"
                        )
                    }

                val newId =
                    insertAndGetId(
                        database,
                        "tractor_models",
                        values
                    )

                idMap[oldId] =
                    newId

                newlyInserted.add(
                    oldId
                )

                counter.added()
            }
        }

        /*
         * Второй проход:
         * восстанавливаем baseModelId
         * только у новых записей.
         */
        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            if (
                oldId !in newlyInserted
            ) {
                continue
            }

            val oldBaseModelId =
                jsonNullableLong(
                    row,
                    "baseModelId"
                )

            if (
                oldBaseModelId != null
            ) {

                val newId =
                    idMap[oldId]
                        ?: continue

                val newBaseModelId =
                    idMap[oldBaseModelId]
                        ?: error(
                            "Не найдена базовая модель трактора"
                        )

                database.execSQL(
                    """
                    UPDATE tractor_models
                    SET baseModelId = ?
                    WHERE id = ?
                    """.trimIndent(),
                    arrayOf(
                        newBaseModelId,
                        newId
                    )
                )
            }
        }

        return idMap
    }

    /*
     * ---------------------------------------------------------
     * ДВИГАТЕЛИ
     * ---------------------------------------------------------
     */

    private fun importEngines(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        counter: Counter
    ): Map<Long, Long> {

        val idMap =
            mutableMapOf<Long, Long>()

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            val existingId =
                findExistingEngine(
                    database,
                    row
                )

            val newId =
                if (existingId != null) {

                    counter.skipped()

                    existingId

                } else {

                    val values =
                        ContentValues().apply {

                            put(
                                "manufacturer",
                                row.getString(
                                    "manufacturer"
                                )
                            )

                            put(
                                "code",
                                row.getString(
                                    "code"
                                )
                            )

                            put(
                                "displacementCc",
                                row.getInt(
                                    "displacementCc"
                                )
                            )

                            put(
                                "powerHp",
                                row.getInt(
                                    "powerHp"
                                )
                            )

                            put(
                                "cylinders",
                                row.getInt(
                                    "cylinders"
                                )
                            )

                            val valves =
                                jsonNullableInt(
                                    row,
                                    "valves"
                                )

                            if (
                                valves == null
                            ) {
                                putNull(
                                    "valves"
                                )
                            } else {
                                put(
                                    "valves",
                                    valves
                                )
                            }

                            put(
                                "fuelType",
                                row.getString(
                                    "fuelType"
                                )
                            )

                            put(
                                "userCreated",
                                row.optInt(
                                    "userCreated",
                                    0
                                )
                            )
                        }

                    insertAndGetId(
                        database,
                        "car_engines",
                        values
                    ).also {
                        counter.added()
                    }
                }

            idMap[oldId] =
                newId
        }

        return idMap
    }

    private fun findExistingEngine(
        database: SupportSQLiteDatabase,
        row: JSONObject
    ): Long? {

        val manufacturer =
            row.getString("manufacturer")

        val code =
            row.getString("code")

        val displacement =
            row.getInt("displacementCc")

        val power =
            row.getInt("powerHp")

        val cylinders =
            row.getInt("cylinders")

        val valves =
            jsonNullableInt(
                row,
                "valves"
            )

        val fuelType =
            row.getString("fuelType")

        database.query(
            """
            SELECT *
            FROM car_engines
            WHERE manufacturer = ?
              AND code = ?
            """.trimIndent(),
            arrayOf(
                manufacturer,
                code
            )
        ).use { cursor ->

            while (
                cursor.moveToNext()
            ) {

                val same =
                    cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                            "displacementCc"
                        )
                    ) == displacement &&

                    cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                            "powerHp"
                        )
                    ) == power &&

                    cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                            "cylinders"
                        )
                    ) == cylinders &&

                    cursorNullableInt(
                        cursor,
                        "valves"
                    ) == valves &&

                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            "fuelType"
                        )
                    ) == fuelType

                if (same) {

                    return cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            "id"
                        )
                    )
                }
            }
        }

        return null
    }

    /*
     * ---------------------------------------------------------
     * СВЯЗИ МОДЕЛЬ <-> ДВИГАТЕЛЬ
     * ---------------------------------------------------------
     */

    private fun importModelEngineLinks(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        tableName: String,
        modelMap: Map<Long, Long>,
        engineMap: Map<Long, Long>,
        counter: Counter
    ) {

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val modelId =
                modelMap[
                    row.getLong("modelId")
                ] ?: error(
                    "Не найдена модель"
                )

            val engineId =
                engineMap[
                    row.getLong("engineId")
                ] ?: error(
                    "Не найден двигатель"
                )

            val exists =
                queryExists(
                    database,
                    """
                    SELECT 1
                    FROM `$tableName`
                    WHERE modelId = ?
                      AND engineId = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        modelId,
                        engineId
                    )
                )

            if (exists) {

                counter.skipped()

            } else {

                val values =
                    ContentValues().apply {

                        put(
                            "modelId",
                            modelId
                        )

                        put(
                            "engineId",
                            engineId
                        )
                    }

                database.insert(
                    tableName,
                    SQLiteDatabase.CONFLICT_ABORT,
                    values
                )

                counter.added()
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * ТИПЫ РАБОТ
     * ---------------------------------------------------------
     */

    private fun importWorkTypes(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        counter: Counter
    ): Map<Long, Long> {

        val idMap =
            mutableMapOf<Long, Long>()

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            val name =
                row.getString("name")

            val existingId =
                querySingleId(
                    database,
                    """
                    SELECT id
                    FROM work_types
                    WHERE name = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(name)
                )

            val newId =
                if (
                    existingId != null
                ) {

                    counter.skipped()

                    existingId

                } else {

                    val values =
                        ContentValues().apply {

                            put(
                                "name",
                                name
                            )

                            put(
                                "isActive",
                                row.optInt(
                                    "isActive",
                                    1
                                )
                            )
                        }

                    insertAndGetId(
                        database,
                        "work_types",
                        values
                    ).also {
                        counter.added()
                    }
                }

            idMap[oldId] =
                newId
        }

        return idMap
    }

    /*
     * ---------------------------------------------------------
     * ПОЛЯ
     * ---------------------------------------------------------
     */

    private fun importFields(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        counter: Counter
    ): Map<Long, Long> {

        val idMap =
            mutableMapOf<Long, Long>()

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            val existingId =
                findExistingField(
                    database,
                    row
                )

            val newId =
                if (
                    existingId != null
                ) {

                    counter.skipped()

                    existingId

                } else {

                    val values =
                        ContentValues().apply {

                            put(
                                "name",
                                row.getString(
                                    "name"
                                )
                            )

                            put(
                                "areaHa",
                                row.getDouble(
                                    "areaHa"
                                )
                            )

                            putNullableString(
                                this,
                                "location",
                                jsonNullableString(
                                    row,
                                    "location"
                                )
                            )

                            putNullableString(
                                this,
                                "note",
                                jsonNullableString(
                                    row,
                                    "note"
                                )
                            )

                            put(
                                "isActive",
                                row.optInt(
                                    "isActive",
                                    1
                                )
                            )
                        }

                    insertAndGetId(
                        database,
                        "fields",
                        values
                    ).also {
                        counter.added()
                    }
                }

            idMap[oldId] =
                newId
        }

        return idMap
    }

    private fun findExistingField(
        database: SupportSQLiteDatabase,
        row: JSONObject
    ): Long? {

        /*
         * Для импорта исторических пакетов поле сопоставляем
         * по его текущему названию, а не по полному снимку
         * area/location/note/isActive.
         *
         * Иначе изменение примечания или площади в живой базе
         * могло приводить к созданию дубля того же поля.
         */
        val name =
            row.getString(
                "name"
            )

        return querySingleId(
            database,
            """
            SELECT id
            FROM fields
            WHERE name = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                name
            )
        )
    }

    /*
     * ---------------------------------------------------------
     * ТЕХНИКА
     * ---------------------------------------------------------
     */

    private fun importVehicles(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        engineMap: Map<Long, Long>,
        counter: Counter
    ): Map<Long, Long> {

        val idMap =
            mutableMapOf<Long, Long>()

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            val oldEngineId =
                jsonNullableLong(
                    row,
                    "engineId"
                )

            val newEngineId =
                if (
                    oldEngineId == null
                ) {
                    null
                } else {
                    engineMap[oldEngineId]
                        ?: error(
                            "Не найден двигатель техники"
                        )
                }

            val existingId =
                findExistingVehicle(
                    database,
                    row,
                    newEngineId
                )

            val newId =
                if (
                    existingId != null
                ) {

                    counter.skipped()

                    existingId

                } else {

                    val values =
                        ContentValues().apply {

                            put(
                                "type",
                                row.getString(
                                    "type"
                                )
                            )

                            put(
                                "brand",
                                row.getString(
                                    "brand"
                                )
                            )

                            put(
                                "model",
                                row.getString(
                                    "model"
                                )
                            )

                            put(
                                "number",
                                row.optString(
                                    "number",
                                    ""
                                )
                            )

                            val year =
                                jsonNullableInt(
                                    row,
                                    "year"
                                )

                            if (
                                year == null
                            ) {
                                putNull("year")
                            } else {
                                put("year", year)
                            }

                            putNullableString(
                                this,
                                "note",
                                jsonNullableString(
                                    row,
                                    "note"
                                )
                            )

                            if (
                                newEngineId == null
                            ) {
                                putNull(
                                    "engineId"
                                )
                            } else {
                                put(
                                    "engineId",
                                    newEngineId
                                )
                            }

                            putNullableString(
                                this,
                                "engineName",
                                jsonNullableString(
                                    row,
                                    "engineName"
                                )
                            )
                        }

                    insertAndGetId(
                        database,
                        "vehicles",
                        values
                    ).also {
                        counter.added()
                    }
                }

            idMap[oldId] =
                newId
        }

        return idMap
    }

    private fun findExistingVehicle(
        database: SupportSQLiteDatabase,
        row: JSONObject,
        mappedEngineId: Long?
    ): Long? {

        val type =
            row.getString("type")

        val brand =
            row.getString("brand")

        val model =
            row.getString("model")

        val number =
            row.optString(
                "number",
                ""
            )

        /*
         * Если есть номер техники,
         * используем его как сильный признак
         * конкретной машины.
         */
        if (
            number.isNotBlank()
        ) {

            return querySingleId(
                database,
                """
                SELECT id
                FROM vehicles
                WHERE type = ?
                  AND brand = ?
                  AND model = ?
                  AND number = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(
                    type,
                    brand,
                    model,
                    number
                )
            )
        }

        /*
         * Если номера нет, сравниваем
         * остальные параметры полностью.
         */
        val year =
            jsonNullableInt(
                row,
                "year"
            )

        val note =
            jsonNullableString(
                row,
                "note"
            )

        val engineName =
            jsonNullableString(
                row,
                "engineName"
            )

        database.query(
            """
            SELECT *
            FROM vehicles
            WHERE type = ?
              AND brand = ?
              AND model = ?
              AND number = ?
            """.trimIndent(),
            arrayOf(
                type,
                brand,
                model,
                number
            )
        ).use { cursor ->

            while (
                cursor.moveToNext()
            ) {

                val same =
                    cursorNullableInt(
                        cursor,
                        "year"
                    ) == year &&

                    cursorNullableString(
                        cursor,
                        "note"
                    ) == note &&

                    cursorNullableLong(
                        cursor,
                        "engineId"
                    ) == mappedEngineId &&

                    cursorNullableString(
                        cursor,
                        "engineName"
                    ) == engineName

                if (same) {

                    return cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            "id"
                        )
                    )
                }
            }
        }

        return null
    }

    /*
     * ---------------------------------------------------------
     * ТО
     * ---------------------------------------------------------
     */

    private fun importMaintenanceRecords(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        vehicleMap: Map<Long, Long>,
        counter: Counter
    ) {

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val vehicleId =
                vehicleMap[
                    row.getLong("vehicleId")
                ] ?: error(
                    "Не найдена техника для записи ТО"
                )

            val exists =
                findExistingMaintenance(
                    database,
                    row,
                    vehicleId
                )

            if (
                exists
            ) {

                counter.skipped()

                continue
            }

            val eventUid =
                jsonNullableString(
                    row,
                    "eventUid"
                )
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }

            val datePrecision =
                row.optString(
                    "datePrecision",
                    "DAY"
                )
                    .trim()
                    .ifEmpty {
                        "DAY"
                    }

            val meterValue =
                jsonNullableInt(
                    row,
                    "meterValue"
                )

            val values =
                ContentValues().apply {

                    put(
                        "vehicleId",
                        vehicleId
                    )

                    put(
                        "date",
                        row.getLong(
                            "date"
                        )
                    )

                    if (
                        meterValue == null
                    ) {

                        putNull(
                            "meterValue"
                        )

                    } else {

                        put(
                            "meterValue",
                            meterValue
                        )
                    }

                    put(
                        "meterType",
                        row.optString(
                            "meterType",
                            ""
                        )
                    )

                    put(
                        "work",
                        row.getString(
                            "work"
                        )
                    )

                    putNullableString(
                        this,
                        "note",
                        jsonNullableString(
                            row,
                            "note"
                        )
                    )

                    putNullableString(
                        this,
                        "eventUid",
                        eventUid
                    )

                    put(
                        "datePrecision",
                        datePrecision
                    )
                }

            insertAndGetId(
                database,
                "maintenance_records",
                values
            )

            counter.added()
        }
    }

    private fun findExistingMaintenance(
        database: SupportSQLiteDatabase,
        row: JSONObject,
        vehicleId: Long
    ): Boolean {

        val eventUid =
            jsonNullableString(
                row,
                "eventUid"
            )
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }

        if (
            eventUid != null
        ) {

            return queryExists(
                database,
                """
                SELECT 1
                FROM maintenance_records
                WHERE eventUid = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(
                    eventUid
                )
            )
        }

        val date =
            row.getLong("date")

        val meterValue =
            jsonNullableInt(
                row,
                "meterValue"
            )

        val meterType =
            row.optString(
                "meterType",
                ""
            )

        val work =
            row.getString("work")

        val note =
            jsonNullableString(
                row,
                "note"
            )

        database.query(
            """
            SELECT *
            FROM maintenance_records
            WHERE vehicleId = ?
              AND date = ?
            """.trimIndent(),
            arrayOf(
                vehicleId,
                date
            )
        ).use { cursor ->

            while (
                cursor.moveToNext()
            ) {

                val same =
                    cursorNullableInt(
                        cursor,
                        "meterValue"
                    ) == meterValue &&

                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            "meterType"
                        )
                    ) == meterType &&

                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            "work"
                        )
                    ) == work &&

                    cursorNullableString(
                        cursor,
                        "note"
                    ) == note

                if (same) {
                    return true
                }
            }
        }

        return false
    }

    private fun syncMaintenanceEvents(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        vehicleMap: Map<Long, Long>,
        counter: Counter
    ) {

        for (
            index in
            0 until rows.length()
        ) {

            val event =
                rows.getJSONObject(index)

            val eventType =
                event.optString(
                    "eventType"
                )

            if (
                eventType != "MAINTENANCE" &&
                eventType != "REPAIR"
            ) {
                continue
            }

            val payloadText =
                jsonNullableString(
                    event,
                    "payloadJson"
                )
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: continue

            val payload =
                try {

                    JSONObject(
                        payloadText
                    )

                } catch (_: Exception) {
                    continue
                }

            val oldVehicleId =
                jsonNullableLong(
                    payload,
                    "vehicleId"
                )

            val vehicleId =
                when {

                    oldVehicleId != null ->
                        vehicleMap[
                            oldVehicleId
                        ]

                    else -> {

                        val vehicleName =
                            jsonNullableString(
                                payload,
                                "vehicle"
                            )
                                ?.trim()
                                ?.takeIf {
                                    it.isNotEmpty()
                                }
                                ?: continue

                        findLocalVehicleByDisplayName(
                            database,
                            vehicleName
                        )
                    }
                }
                    ?: error(
                        "Не найдена техника для события: " +
                            event.optString(
                                "title"
                            )
                    )

            val eventUid =
                event.getString(
                    "eventUid"
                )

            val exists =
                queryExists(
                    database,
                    """
                    SELECT 1
                    FROM maintenance_records
                    WHERE eventUid = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        eventUid
                    )
                )

            if (
                exists
            ) {

                counter.skipped()

                continue
            }

            val datePrecision =
                event.optString(
                    "datePrecision",
                    "DAY"
                )
                    .trim()
                    .ifEmpty {
                        "DAY"
                    }

            val date =
                eventDateToMillis(
                    year =
                        event.getInt(
                            "eventYear"
                        ),
                    month =
                        jsonNullableInt(
                            event,
                            "eventMonth"
                        ),
                    day =
                        jsonNullableInt(
                            event,
                            "eventDay"
                        ),
                    datePrecision =
                        datePrecision
                )

            val meterValue =
                jsonNullableInt(
                    payload,
                    "meterValue"
                )

            val meterType =
                payload.optString(
                    "meterType",
                    ""
                )

            val work =
                jsonNullableString(
                    payload,
                    "work"
                )
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: event.optString(
                        "title",
                        "Техническое обслуживание"
                    )

            val noteParts =
                mutableListOf<String>()

            jsonNullableString(
                event,
                "description"
            )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    noteParts.add(
                        it
                    )
                }

            val plannedOilChange =
                jsonNullableInt(
                    payload,
                    "plannedOilChangeMeterValue"
                )

            if (
                plannedOilChange != null
            ) {

                val unit =
                    if (
                        meterType == "km"
                    ) {
                        "км"
                    } else {
                        "м/ч"
                    }

                val planLine =
                    "Следующая замена масла запланирована на " +
                        "$plannedOilChange $unit."

                if (
                    noteParts.none {
                        it.contains(
                            plannedOilChange.toString()
                        )
                    }
                ) {

                    noteParts.add(
                        planLine
                    )
                }
            }

            val values =
                ContentValues().apply {

                    put(
                        "vehicleId",
                        vehicleId
                    )

                    put(
                        "date",
                        date
                    )

                    if (
                        meterValue == null
                    ) {

                        putNull(
                            "meterValue"
                        )

                    } else {

                        put(
                            "meterValue",
                            meterValue
                        )
                    }

                    put(
                        "meterType",
                        meterType
                    )

                    put(
                        "work",
                        work
                    )

                    put(
                        "note",
                        noteParts.joinToString(
                            "\n"
                        )
                    )

                    put(
                        "eventUid",
                        eventUid
                    )

                    put(
                        "datePrecision",
                        datePrecision
                    )
                }

            insertAndGetId(
                database,
                "maintenance_records",
                values
            )

            counter.added()
        }
    }

    private fun findLocalVehicleByDisplayName(
        database: SupportSQLiteDatabase,
        requestedName: String
    ): Long? {

        val normalizedRequested =
            normalizeVehicleName(
                requestedName
            )

        database.query(
            """
            SELECT id, brand, model, number
            FROM vehicles
            """.trimIndent()
        ).use { cursor ->

            while (
                cursor.moveToNext()
            ) {

                val id =
                    cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            "id"
                        )
                    )

                val brand =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            "brand"
                        )
                    )

                val model =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            "model"
                        )
                    )

                val number =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            "number"
                        )
                    )

                val baseName =
                    normalizeVehicleName(
                        "$brand $model"
                    )

                val fullName =
                    normalizeVehicleName(
                        "$brand $model $number"
                    )

                if (
                    normalizedRequested == baseName ||
                    normalizedRequested == fullName
                ) {
                    return id
                }
            }
        }

        return null
    }

    private fun normalizeVehicleName(
        value: String
    ): String {

        return value
            .trim()
            .lowercase()
            .replace(
                Regex("\\s+"),
                " "
            )
    }

    private fun eventDateToMillis(
        year: Int,
        month: Int?,
        day: Int?,
        datePrecision: String
    ): Long {

        val safeMonth =
            (month ?: 1)
                .coerceIn(
                    1,
                    12
                )

        val safeDay =
            if (
                datePrecision == "DAY"
            ) {
                (day ?: 1)
                    .coerceAtLeast(
                        1
                    )
            } else {
                1
            }

        val calendar =
            Calendar.getInstance(
                TimeZone.getTimeZone(
                    "UTC"
                )
            ).apply {

                clear()

                set(
                    Calendar.YEAR,
                    year
                )

                set(
                    Calendar.MONTH,
                    safeMonth - 1
                )

                set(
                    Calendar.DAY_OF_MONTH,
                    safeDay
                )

                set(
                    Calendar.HOUR_OF_DAY,
                    12
                )
            }

        return calendar.timeInMillis
    }

    /*
     * ---------------------------------------------------------
     * ОБЩИЕ СЕЗОННЫЕ ПРОЦЕССЫ
     * ---------------------------------------------------------
     */
    private fun importSeasonProcesses(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        fieldMap: Map<Long, Long>,
        counter: Counter
    ): Map<Long, Long> {

        val idMap =
            mutableMapOf<Long, Long>()

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            val processUid =
                row.getString(
                    "processUid"
                )

            val existingId =
                querySingleId(
                    database,
                    """
                    SELECT id
                    FROM season_processes
                    WHERE processUid = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        processUid
                    )
                )

            if (
                existingId != null
            ) {

                idMap[oldId] =
                    existingId

                counter.skipped()

                continue
            }

            val oldFieldId =
                jsonNullableLong(
                    row,
                    "fieldId"
                )

            val newFieldId =
                if (
                    oldFieldId == null
                ) {

                    null

                } else {

                    fieldMap[
                        oldFieldId
                    ] ?: error(
                        "Не найдено поле сезонного процесса"
                    )
                }

            val values =
                ContentValues().apply {

                    put(
                        "processUid",
                        processUid
                    )

                    put(
                        "title",
                        row.getString(
                            "title"
                        )
                    )

                    if (
                        newFieldId == null
                    ) {
                        putNull("fieldId")
                    } else {
                        put(
                            "fieldId",
                            newFieldId
                        )
                    }

                    put(
                        "startDate",
                        row.getString(
                            "startDate"
                        )
                    )

                    putNullableString(
                        this,
                        "endDate",
                        jsonNullableString(
                            row,
                            "endDate"
                        )
                    )

                    put(
                        "status",
                        row.optString(
                            "status",
                            "IN_PROGRESS"
                        )
                    )

                    putNullableString(
                        this,
                        "crop",
                        jsonNullableString(
                            row,
                            "crop"
                        )
                    )

                    putNullableString(
                        this,
                        "goal",
                        jsonNullableString(
                            row,
                            "goal"
                        )
                    )

                    val areaHa =
                        jsonNullableDouble(
                            row,
                            "areaHa"
                        )

                    if (
                        areaHa == null
                    ) {
                        putNull("areaHa")
                    } else {
                        put(
                            "areaHa",
                            areaHa
                        )
                    }

                    putNullableString(
                        this,
                        "note",
                        jsonNullableString(
                            row,
                            "note"
                        )
                    )
                }

            val newId =
                insertAndGetId(
                    database,
                    "season_processes",
                    values
                )

            idMap[oldId] =
                newId

            counter.added()
        }

        return idMap
    }


    /*
     * ---------------------------------------------------------
     * СЕЗОННЫЕ РАБОТЫ
     * ---------------------------------------------------------
     */

    private fun importSeasonWorks(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        fieldMap: Map<Long, Long>,
        workTypeMap: Map<Long, Long>,
        seasonProcessMap: Map<Long, Long>,
        counter: Counter
    ): Map<Long, Long> {

        val idMap =
            mutableMapOf<Long, Long>()

        for (index in 0 until rows.length()) {

            val row =
                rows.getJSONObject(index)

            val oldId =
                row.getLong("id")

            val eventUid =
                jsonNullableString(
                    row,
                    "eventUid"
                )
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: error(
                        "У сезонной работы id=$oldId отсутствует eventUid"
                    )

            val existingId =
                querySingleId(
                    database,
                    """
                    SELECT id
                    FROM season_works
                    WHERE eventUid = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(eventUid)
                )

            if (existingId != null) {

                idMap[oldId] =
                    existingId

                counter.skipped()

                continue
            }

            val journalEventExists =
                queryExists(
                    database,
                    """
                    SELECT 1
                    FROM farm_events
                    WHERE eventUid = ?
                      AND eventType = 'FIELD_WORK'
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(eventUid)
                )

            if (!journalEventExists) {
                error(
                    "Не найдено событие журнала для сезонной работы " +
                        "id=$oldId, eventUid=$eventUid"
                )
            }

            val newWorkTypeId =
                workTypeMap[
                    row.getLong("workTypeId")
                ] ?: error(
                    "Не найден тип работы"
                )

            val oldFieldId =
                jsonNullableLong(
                    row,
                    "fieldId"
                )

            val newFieldId =
                if (oldFieldId == null) {
                    null
                } else {
                    fieldMap[oldFieldId]
                        ?: error(
                            "Не найдено поле"
                        )
                }

            val oldProcessId =
                jsonNullableLong(
                    row,
                    "processId"
                )

            val newProcessId =
                if (
                    oldProcessId == null
                ) {

                    null

                } else {

                    seasonProcessMap[
                        oldProcessId
                    ] ?: error(
                        "Не найден сезонный процесс"
                    )
                }

            val values =
                ContentValues().apply {

                    put(
                        "eventUid",
                        eventUid
                    )

                    put(
                        "workDate",
                        row.getString("workDate")
                    )

                    putNullableString(
                        this,
                        "endDate",
                        jsonNullableString(
                            row,
                            "endDate"
                        )
                    )

                    put(
                        "status",
                        row.optString(
                            "status",
                            "COMPLETED"
                        )
                    )

                    put(
                        "workTypeId",
                        newWorkTypeId
                    )

                    if (newFieldId == null) {
                        putNull("fieldId")
                    } else {
                        put(
                            "fieldId",
                            newFieldId
                        )
                    }

                    if (
                        newProcessId == null
                    ) {

                        putNull(
                            "processId"
                        )

                    } else {

                        put(
                            "processId",
                            newProcessId
                        )
                    }

                    val areaHa =
                        jsonNullableDouble(
                            row,
                            "areaHa"
                        )

                    if (areaHa == null) {
                        putNull("areaHa")
                    } else {
                        put(
                            "areaHa",
                            areaHa
                        )
                    }

                    putNullableString(
                        this,
                        "note",
                        jsonNullableString(
                            row,
                            "note"
                        )
                    )
                }

            val newId =
                insertAndGetId(
                    database,
                    "season_works",
                    values
                )

            idMap[oldId] =
                newId

            counter.added()
        }

        return idMap
    }

    /*
     * ---------------------------------------------------------
     * ДНИ / ЭТАПЫ КАСКАДНОЙ СЕЗОННОЙ РАБОТЫ
     * ---------------------------------------------------------
     */
    private fun importSeasonWorkDays(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        seasonWorkMap: Map<Long, Long>,
        counter: Counter
    ) {

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val dayUid =
                row.getString(
                    "dayUid"
                )

            val exists =
                queryExists(
                    database,
                    """
                    SELECT 1
                    FROM season_work_days
                    WHERE dayUid = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        dayUid
                    )
                )

            if (
                exists
            ) {

                counter.skipped()

                continue
            }

            val seasonWorkId =
                seasonWorkMap[
                    row.getLong(
                        "seasonWorkId"
                    )
                ] ?: error(
                    "Не найдена родительская сезонная работа " +
                        "для этапа $dayUid"
                )

            val values =
                ContentValues().apply {

                    put(
                        "dayUid",
                        dayUid
                    )

                    put(
                        "seasonWorkId",
                        seasonWorkId
                    )

                    put(
                        "workDate",
                        row.getString(
                            "workDate"
                        )
                    )

                    putNullableString(
                        this,
                        "endDate",
                        jsonNullableString(
                            row,
                            "endDate"
                        )
                    )

                    val areaHa =
                        jsonNullableDouble(
                            row,
                            "areaHa"
                        )

                    if (
                        areaHa == null
                    ) {
                        putNull(
                            "areaHa"
                        )
                    } else {
                        put(
                            "areaHa",
                            areaHa
                        )
                    }

                    val cumulativeAreaHa =
                        jsonNullableDouble(
                            row,
                            "cumulativeAreaHa"
                        )

                    if (
                        cumulativeAreaHa == null
                    ) {
                        putNull(
                            "cumulativeAreaHa"
                        )
                    } else {
                        put(
                            "cumulativeAreaHa",
                            cumulativeAreaHa
                        )
                    }

                    put(
                        "stage",
                        row.optString(
                            "stage",
                            "WORK"
                        )
                    )

                    putNullableString(
                        this,
                        "note",
                        jsonNullableString(
                            row,
                            "note"
                        )
                    )
                }

            insertAndGetId(
                database,
                "season_work_days",
                values
            )

            counter.added()
        }
    }


    /*
     * ---------------------------------------------------------
     * СВЯЗЬ СЕЗОННОЙ РАБОТЫ С ТЕХНИКОЙ
     * ---------------------------------------------------------
     */

    private fun importSeasonWorkVehicles(
        database: SupportSQLiteDatabase,
        rows: JSONArray,
        seasonWorkMap: Map<Long, Long>,
        vehicleMap: Map<Long, Long>,
        counter: Counter
    ) {

        for (
            index in
            0 until rows.length()
        ) {

            val row =
                rows.getJSONObject(index)

            val seasonWorkId =
                seasonWorkMap[
                    row.getLong(
                        "seasonWorkId"
                    )
                ] ?: error(
                    "Не найдена сезонная работа"
                )

            val vehicleId =
                vehicleMap[
                    row.getLong(
                        "vehicleId"
                    )
                ] ?: error(
                    "Не найдена техника"
                )

            val exists =
                queryExists(
                    database,
                    """
                    SELECT 1
                    FROM season_work_vehicles
                    WHERE seasonWorkId = ?
                      AND vehicleId = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        seasonWorkId,
                        vehicleId
                    )
                )

            if (exists) {

                counter.skipped()

                continue
            }

            val values =
                ContentValues().apply {

                    put(
                        "seasonWorkId",
                        seasonWorkId
                    )

                    put(
                        "vehicleId",
                        vehicleId
                    )

                    put(
                        "vehicleNameSnapshot",
                        row.optString(
                            "vehicleNameSnapshot",
                            ""
                        )
                    )
                }

            database.insert(
                "season_work_vehicles",
                SQLiteDatabase.CONFLICT_ABORT,
                values
            )

            counter.added()
        }
    }


/*
 * ---------------------------------------------------------
 * УБОРКА УРОЖАЯ — ПРОЦЕССЫ
 * ---------------------------------------------------------
 */

private fun importHarvestProcesses(
    database: SupportSQLiteDatabase,
    rows: JSONArray,
    fieldMap: Map<Long, Long>,
    vehicleMap: Map<Long, Long>,
    counter: Counter
): Map<Long, Long> {

    val idMap =
        mutableMapOf<Long, Long>()

    for (index in 0 until rows.length()) {

        val row =
            rows.getJSONObject(index)

        val oldId =
            row.getLong("id")

        val processUid =
            row.getString("processUid")

        val existingId =
            querySingleId(
                database,
                """
                SELECT id
                FROM harvest_processes
                WHERE processUid = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(processUid)
            )

        if (existingId != null) {
            idMap[oldId] =
                existingId

            counter.skipped()
            continue
        }

        val oldFieldId =
            jsonNullableLong(
                row,
                "fieldId"
            )

        val newFieldId =
            if (oldFieldId == null) {
                null
            } else {
                fieldMap[oldFieldId]
                    ?: error(
                        "Не найдено поле для уборочного процесса"
                    )
            }

        val oldVehicleId =
            jsonNullableLong(
                row,
                "combineVehicleId"
            )

        val newVehicleId =
            if (oldVehicleId == null) {
                null
            } else {
                vehicleMap[oldVehicleId]
                    ?: error(
                        "Не найден комбайн для уборочного процесса"
                    )
            }

        val values =
            ContentValues().apply {

                put(
                    "processUid",
                    processUid
                )

                put(
                    "startDate",
                    row.getString(
                        "startDate"
                    )
                )

                putNullableString(
                    this,
                    "endDate",
                    jsonNullableString(
                        row,
                        "endDate"
                    )
                )

                put(
                    "status",
                    row.getString(
                        "status"
                    )
                )

                if (newFieldId == null) {
                    putNull("fieldId")
                } else {
                    put(
                        "fieldId",
                        newFieldId
                    )
                }

                put(
                    "fieldNameSnapshot",
                    row.optString(
                        "fieldNameSnapshot",
                        ""
                    )
                )

                put(
                    "crop",
                    row.getString(
                        "crop"
                    )
                )

                if (newVehicleId == null) {
                    putNull(
                        "combineVehicleId"
                    )
                } else {
                    put(
                        "combineVehicleId",
                        newVehicleId
                    )
                }

                put(
                    "combineNameSnapshot",
                    row.optString(
                        "combineNameSnapshot",
                        ""
                    )
                )

                val bunkerVolumeM3 =
                    jsonNullableDouble(
                        row,
                        "bunkerVolumeM3"
                    )

                if (bunkerVolumeM3 == null) {
                    putNull(
                        "bunkerVolumeM3"
                    )
                } else {
                    put(
                        "bunkerVolumeM3",
                        bunkerVolumeM3
                    )
                }

                put(
                    "createdAt",
                    row.optLong(
                        "createdAt",
                        System.currentTimeMillis()
                    )
                )
            }

        val newId =
            insertAndGetId(
                database,
                "harvest_processes",
                values
            )

        idMap[oldId] =
            newId

        counter.added()
    }

    return idMap
}



/*
 * ---------------------------------------------------------
 * ТТХ КОМБАЙНОВ
 * ---------------------------------------------------------
 */
private fun importCombineSpecs(
    database: SupportSQLiteDatabase,
    rows: JSONArray,
    vehicleMap: Map<Long, Long>,
    counter: Counter
) {
    for (
        index in
        0 until rows.length()
    ) {
        val row =
            rows.getJSONObject(
                index
            )

        val oldVehicleId =
            row.getLong(
                "vehicleId"
            )

        val newVehicleId =
            vehicleMap[
                oldVehicleId
            ]

        if (
            newVehicleId ==
            null
        ) {
            counter.skipped()
            continue
        }

        val values =
            ContentValues().apply {
                put(
                    "vehicleId",
                    newVehicleId
                )

                if (
                    row.isNull(
                        "grainTankVolumeM3"
                    )
                ) {
                    putNull(
                        "grainTankVolumeM3"
                    )
                } else {
                    put(
                        "grainTankVolumeM3",
                        row.getDouble(
                            "grainTankVolumeM3"
                        )
                    )
                }
            }

        database.insert(
            "combine_specs",
            SQLiteDatabase.CONFLICT_REPLACE,
            values
        )

        counter.added()
    }
}


/*
 * ---------------------------------------------------------
 * УБОРКА УРОЖАЯ — РАБОЧИЕ ДНИ
 * ---------------------------------------------------------
 */

private fun importHarvestDays(
    database: SupportSQLiteDatabase,
    rows: JSONArray,
    harvestProcessMap: Map<Long, Long>,
    counter: Counter
): Map<Long, Long> {

    val idMap =
        mutableMapOf<Long, Long>()

    for (index in 0 until rows.length()) {

        val row =
            rows.getJSONObject(index)

        val oldId =
            row.getLong("id")

        val dayUid =
            row.getString("dayUid")

        val existingId =
            querySingleId(
                database,
                """
                SELECT id
                FROM harvest_days
                WHERE dayUid = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(dayUid)
            )

        if (existingId != null) {
            idMap[oldId] =
                existingId

            counter.skipped()
            continue
        }

        val processId =
            harvestProcessMap[
                row.getLong(
                    "processId"
                )
            ] ?: error(
                "Не найден уборочный процесс для рабочего дня"
            )

        val values =
            ContentValues().apply {

                put(
                    "dayUid",
                    dayUid
                )

                put(
                    "processId",
                    processId
                )

                put(
                    "workDate",
                    row.getString(
                        "workDate"
                    )
                )

                put(
                    "startTime",
                    row.getString(
                        "startTime"
                    )
                )

                putNullableString(
                    this,
                    "endTime",
                    jsonNullableString(
                        row,
                        "endTime"
                    )
                )

                put(
                    "status",
                    row.getString(
                        "status"
                    )
                )

                put(
                    "workIntervalMinutes",
                    row.optInt(
                        "workIntervalMinutes",
                        30
                    )
                )

                put(
                    "unloadIntervalMinutes",
                    row.optInt(
                        "unloadIntervalMinutes",
                        5
                    )
                )

                put(
                    "lunchIntervalMinutes",
                    row.optInt(
                        "lunchIntervalMinutes",
                        15
                    )
                )

                put(
                    "dinnerIntervalMinutes",
                    row.optInt(
                        "dinnerIntervalMinutes",
                        15
                    )
                )

                put(
                    "waitingTransportIntervalMinutes",
                    row.optInt(
                        "waitingTransportIntervalMinutes",
                        30
                    )
                )

                put(
                    "workIntervalCustom",
                    if (
                        row.optInt(
                            "workIntervalCustom",
                            0
                        ) != 0
                    ) 1 else 0
                )

                put(
                    "unloadIntervalCustom",
                    if (
                        row.optInt(
                            "unloadIntervalCustom",
                            0
                        ) != 0
                    ) 1 else 0
                )

                put(
                    "lunchIntervalCustom",
                    if (
                        row.optInt(
                            "lunchIntervalCustom",
                            0
                        ) != 0
                    ) 1 else 0
                )

                put(
                    "dinnerIntervalCustom",
                    if (
                        row.optInt(
                            "dinnerIntervalCustom",
                            0
                        ) != 0
                    ) 1 else 0
                )

                put(
                    "waitingTransportIntervalCustom",
                    if (
                        row.optInt(
                            "waitingTransportIntervalCustom",
                            0
                        ) != 0
                    ) 1 else 0
                )

                put(
                    "reminderSettingsInitialized",
                    if (
                        row.has(
                            "reminderSettingsInitialized"
                        )
                    ) {
                        if (
                            row.optInt(
                                "reminderSettingsInitialized",
                                0
                            ) != 0
                        ) 1 else 0
                    } else {
                        0
                    }
                )

                put(
                    "createdAt",
                    row.optLong(
                        "createdAt",
                        System.currentTimeMillis()
                    )
                )
            }

        val newId =
            insertAndGetId(
                database,
                "harvest_days",
                values
            )

        idMap[oldId] =
            newId

        counter.added()
    }

    return idMap
}


/*
 * ---------------------------------------------------------
 * УБОРКА УРОЖАЯ — БУНКЕРЫ
 * ---------------------------------------------------------
 */

private fun importHarvestBunkers(
    database: SupportSQLiteDatabase,
    rows: JSONArray,
    harvestDayMap: Map<Long, Long>,
    counter: Counter
) {

    for (index in 0 until rows.length()) {

        val row =
            rows.getJSONObject(index)

        val bunkerUid =
            row.getString(
                "bunkerUid"
            )

        val exists =
            queryExists(
                database,
                """
                SELECT 1
                FROM harvest_bunkers
                WHERE bunkerUid = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(bunkerUid)
            )

        if (exists) {
            counter.skipped()
            continue
        }

        val dayId =
            harvestDayMap[
                row.getLong(
                    "dayId"
                )
            ] ?: error(
                "Не найден рабочий день для бункера"
            )

        val values =
            ContentValues().apply {

                put(
                    "bunkerUid",
                    bunkerUid
                )

                put(
                    "dayId",
                    dayId
                )

                put(
                    "fillTime",
                    row.getString(
                        "fillTime"
                    )
                )

                put(
                    "fillLevel",
                    row.getString(
                        "fillLevel"
                    )
                )

                if (
                    row.has(
                        "postEventType"
                    ) &&
                    !row.isNull(
                        "postEventType"
                    )
                ) {
                    put(
                        "postEventType",
                        row.getString(
                            "postEventType"
                        )
                    )
                }

                put(
                    "createdAt",
                    row.optLong(
                        "createdAt",
                        System.currentTimeMillis()
                    )
                )
            }

        database.insert(
            "harvest_bunkers",
            SQLiteDatabase.CONFLICT_ABORT,
            values
        )

        counter.added()
    }
}


    /*
     * ---------------------------------------------------------
     * ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
     * ---------------------------------------------------------
     */

    private fun insertAndGetId(
        database: SupportSQLiteDatabase,
        tableName: String,
        values: ContentValues
    ): Long {

        val result =
            database.insert(
                tableName,
                SQLiteDatabase.CONFLICT_ABORT,
                values
            )

        if (
            result == -1L
        ) {
            error(
                "Не удалось добавить запись в $tableName"
            )
        }

        return result
    }

    private fun querySingleId(
        database: SupportSQLiteDatabase,
        sql: String,
        args: Array<Any?>
    ): Long? {

        database.query(
            sql,
            args
        ).use { cursor ->

            if (
                cursor.moveToFirst()
            ) {

                return cursor.getLong(0)
            }
        }

        return null
    }

    private fun queryExists(
        database: SupportSQLiteDatabase,
        sql: String,
        args: Array<Any?>
    ): Boolean {

        database.query(
            sql,
            args
        ).use { cursor ->

            return cursor.moveToFirst()
        }
    }

    private fun jsonNullableString(
        json: JSONObject,
        key: String
    ): String? {

        return if (
            !json.has(key) ||
            json.isNull(key)
        ) {
            null
        } else {
            json.getString(key)
        }
    }

    private fun jsonNullableLong(
        json: JSONObject,
        key: String
    ): Long? {

        return if (
            !json.has(key) ||
            json.isNull(key)
        ) {
            null
        } else {
            json.getLong(key)
        }
    }

    private fun jsonNullableInt(
        json: JSONObject,
        key: String
    ): Int? {

        return if (
            !json.has(key) ||
            json.isNull(key)
        ) {
            null
        } else {
            json.getInt(key)
        }
    }

    private fun jsonNullableDouble(
        json: JSONObject,
        key: String
    ): Double? {

        return if (
            !json.has(key) ||
            json.isNull(key)
        ) {
            null
        } else {
            json.getDouble(key)
        }
    }

    private fun cursorNullableString(
        cursor: Cursor,
        columnName: String
    ): String? {

        val index =
            cursor.getColumnIndexOrThrow(
                columnName
            )

        return if (
            cursor.isNull(index)
        ) {
            null
        } else {
            cursor.getString(index)
        }
    }

    private fun cursorNullableLong(
        cursor: Cursor,
        columnName: String
    ): Long? {

        val index =
            cursor.getColumnIndexOrThrow(
                columnName
            )

        return if (
            cursor.isNull(index)
        ) {
            null
        } else {
            cursor.getLong(index)
        }
    }

    private fun cursorNullableInt(
        cursor: Cursor,
        columnName: String
    ): Int? {

        val index =
            cursor.getColumnIndexOrThrow(
                columnName
            )

        return if (
            cursor.isNull(index)
        ) {
            null
        } else {
            cursor.getInt(index)
        }
    }

    private fun cursorNullableDouble(
        cursor: Cursor,
        columnName: String
    ): Double? {

        val index =
            cursor.getColumnIndexOrThrow(
                columnName
            )

        return if (
            cursor.isNull(index)
        ) {
            null
        } else {
            cursor.getDouble(index)
        }
    }

    private fun putNullableString(
        values: ContentValues,
        key: String,
        value: String?
    ) {

        if (
            value == null
        ) {
            values.putNull(key)
        } else {
            values.put(
                key,
                value
            )
        }
    }
}