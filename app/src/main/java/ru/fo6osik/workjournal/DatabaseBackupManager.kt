package ru.fo6osik.workjournal

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Base64
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object DatabaseBackupManager {

    private const val BACKUP_FORMAT_VERSION = 1

    private val SYSTEM_TABLES = setOf(
        "android_metadata",
        "sqlite_sequence",
        "room_master_table"
    )

    suspend fun exportBackup(
        context: Context,
        uri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {

        runCatching {

            val roomDatabase = DatabaseProvider.getDatabase(context)
            val database = roomDatabase.openHelper.readableDatabase

            val root = JSONObject()

            root.put("backupFormatVersion", BACKUP_FORMAT_VERSION)
            root.put("databaseVersion", database.version)
            root.put("createdAt", System.currentTimeMillis())

            val tablesJson = JSONObject()

            val tableNames = getTableNames(database)

            for (tableName in tableNames) {

                val rows = JSONArray()

                database.query(
                    "SELECT * FROM `${tableName}`"
                ).use { cursor ->

                    while (cursor.moveToNext()) {

                        val row = JSONObject()

                        for (columnIndex in 0 until cursor.columnCount) {

                            val columnName =
                                cursor.getColumnName(columnIndex)

                            when (cursor.getType(columnIndex)) {

                                Cursor.FIELD_TYPE_NULL -> {
                                    row.put(
                                        columnName,
                                        JSONObject.NULL
                                    )
                                }

                                Cursor.FIELD_TYPE_INTEGER -> {
                                    row.put(
                                        columnName,
                                        cursor.getLong(columnIndex)
                                    )
                                }

                                Cursor.FIELD_TYPE_FLOAT -> {
                                    row.put(
                                        columnName,
                                        cursor.getDouble(columnIndex)
                                    )
                                }

                                Cursor.FIELD_TYPE_STRING -> {
                                    row.put(
                                        columnName,
                                        cursor.getString(columnIndex)
                                    )
                                }

                                Cursor.FIELD_TYPE_BLOB -> {

                                    val blob =
                                        cursor.getBlob(columnIndex)

                                    val blobObject =
                                        JSONObject()

                                    blobObject.put(
                                        "__type",
                                        "blob"
                                    )

                                    blobObject.put(
                                        "value",
                                        Base64.encodeToString(
                                            blob,
                                            Base64.NO_WRAP
                                        )
                                    )

                                    row.put(
                                        columnName,
                                        blobObject
                                    )
                                }
                            }
                        }

                        rows.put(row)
                    }
                }

                tablesJson.put(
                    tableName,
                    rows
                )
            }

            root.put(
                "tables",
                tablesJson
            )

            val outputStream =
                context.contentResolver
                    .openOutputStream(uri)
                    ?: error(
                        "Не удалось открыть файл для записи"
                    )

            outputStream
                .bufferedWriter()
                .use { writer ->

                    writer.write(
                        root.toString(2)
                    )
                }
        }
    }

    suspend fun importBackup(
        context: Context,
        uri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {

        runCatching {

            val inputStream =
                context.contentResolver
                    .openInputStream(uri)
                    ?: error(
                        "Не удалось открыть файл"
                    )

            val jsonText =
                inputStream
                    .bufferedReader()
                    .use { reader ->
                        reader.readText()
                    }

            val root =
                JSONObject(jsonText)

            val backupFormatVersion =
                root.getInt(
                    "backupFormatVersion"
                )

            if (
                backupFormatVersion !=
                BACKUP_FORMAT_VERSION
            ) {
                error(
                    "Неподдерживаемая версия резервной копии: " +
                            backupFormatVersion
                )
            }

            val roomDatabase =
                DatabaseProvider.getDatabase(context)

            val database =
                roomDatabase.openHelper.writableDatabase

            val backupDatabaseVersion =
                root.getInt(
                    "databaseVersion"
                )

            val compatibleLegacyToV20 =
                database.version == 20 &&
                    (
                        backupDatabaseVersion == 18 ||
                        backupDatabaseVersion == 19
                    )

            if (
                backupDatabaseVersion !=
                    database.version &&
                !compatibleLegacyToV20
            ) {
                error(
                    "Версия базы резервной копии: " +
                            backupDatabaseVersion +
                            ", текущая версия базы: " +
                            database.version
                )
            }

            val tablesJson =
                root.getJSONObject(
                    "tables"
                )

            val currentTables =
                getTableNames(database).toSet()

            val backupTables =
                tablesJson
                    .keys()
                    .asSequence()
                    .toSet()

            /*
             * Обычно восстановление разрешено только в ту же
             * версию структуры базы. Для v20 разрешаем резервные
             * копии v18/v19: недостающие поля рабочих настроек
             * получают безопасные значения по умолчанию.
             *
             * Это защищает от частичного или неправильного
             * восстановления.
             */
            if (
                currentTables != backupTables
            ) {
                error(
                    "Структура резервной копии " +
                            "не совпадает с текущей базой"
                )
            }
			
			validateBackupRows(
    database = database,
    tablesJson = tablesJson,
    tableNames = backupTables,
    backupDatabaseVersion = backupDatabaseVersion
)

            database.execSQL(
                "PRAGMA foreign_keys = OFF"
            )

            database.beginTransaction()

            try {

                /*
                 * Сначала очищаем существующие данные.
                 */
                for (tableName in currentTables) {

                    validateTableName(tableName)

                    database.execSQL(
                        "DELETE FROM `${tableName}`"
                    )
                }

                /*
                 * Затем восстанавливаем таблицы.
                 */
                for (tableName in backupTables) {

                    validateTableName(tableName)

                    val rows =
                        tablesJson.getJSONArray(
                            tableName
                        )

                    for (
                        rowIndex in
                        0 until rows.length()
                    ) {

                        val row =
                            rows.getJSONObject(
                                rowIndex
                            )

                        val values =
                            ContentValues()

                        val columnNames =
                            row.keys()

                        while (
                            columnNames.hasNext()
                        ) {

                            val columnName =
                                columnNames.next()

                            val value =
                                row.get(
                                    columnName
                                )

                            when (value) {

                                JSONObject.NULL -> {
                                    values.putNull(
                                        columnName
                                    )
                                }

                                is Int -> {
                                    values.put(
                                        columnName,
                                        value
                                    )
                                }

                                is Long -> {
                                    values.put(
                                        columnName,
                                        value
                                    )
                                }

                                is Double -> {
                                    values.put(
                                        columnName,
                                        value
                                    )
                                }

                                is String -> {
                                    values.put(
                                        columnName,
                                        value
                                    )
                                }

                                is Boolean -> {
                                    values.put(
                                        columnName,
                                        if (value) 1 else 0
                                    )
                                }

                                is JSONObject -> {

                                    if (
                                        value.optString(
                                            "__type"
                                        ) == "blob"
                                    ) {

                                        val blob =
                                            Base64.decode(
                                                value.getString(
                                                    "value"
                                                ),
                                                Base64.NO_WRAP
                                            )

                                        values.put(
                                            columnName,
                                            blob
                                        )
                                    } else {

                                        values.put(
                                            columnName,
                                            value.toString()
                                        )
                                    }
                                }

                                else -> {
                                    values.put(
                                        columnName,
                                        value.toString()
                                    )
                                }
                            }
                        }

                        val result =
                            database.insert(
                                tableName,
                                SQLiteDatabase.CONFLICT_REPLACE,
                                values
                            )

                        if (result == -1L) {
                            error(
                                "Ошибка восстановления " +
                                        "таблицы $tableName"
                            )
                        }
                    }
                }

                database.setTransactionSuccessful()

            } finally {

                database.endTransaction()

                database.execSQL(
                    "PRAGMA foreign_keys = ON"
                )
            }
        }
    }

    private fun getTableNames(
        database: SupportSQLiteDatabase
    ): List<String> {

        val result =
            mutableListOf<String>()

        database.query(
            """
            SELECT name
            FROM sqlite_master
            WHERE type = 'table'
            ORDER BY name
            """.trimIndent()
        ).use { cursor ->

            val nameIndex =
                cursor.getColumnIndexOrThrow(
                    "name"
                )

            while (cursor.moveToNext()) {

                val tableName =
                    cursor.getString(
                        nameIndex
                    )

                if (
                    tableName !in SYSTEM_TABLES &&
                    !tableName.startsWith(
                        "sqlite_"
                    )
                ) {
                    result.add(
                        tableName
                    )
                }
            }
        }

        return result
    }

    private fun validateTableName(
        tableName: String
    ) {

        val valid =
            Regex(
                "^[A-Za-z_][A-Za-z0-9_]*$"
            )

        if (
            !valid.matches(tableName)
        ) {
            error(
                "Недопустимое имя таблицы"
            )
        }
    }
	private fun validateBackupRows(
    database: SupportSQLiteDatabase,
    tablesJson: JSONObject,
    tableNames: Set<String>,
    backupDatabaseVersion: Int
) {

    for (tableName in tableNames) {

        validateTableName(tableName)

        val expectedColumns =
            getColumnNames(
                database,
                tableName
            )

        val rows =
            tablesJson.optJSONArray(
                tableName
            )
                ?: error(
                    "Таблица $tableName имеет неправильный формат"
                )

        for (
            rowIndex in
            0 until rows.length()
        ) {

            val row =
                rows.optJSONObject(
                    rowIndex
                )
                    ?: error(
                        "Повреждена запись " +
                                "${rowIndex + 1} " +
                                "в таблице $tableName"
                    )

            val actualColumns =
                row.keys()
                    .asSequence()
                    .toSet()

            val missingColumns =
                expectedColumns -
                    actualColumns

            val extraColumns =
                actualColumns -
                    expectedColumns

            val allowedMissingColumns =
                when {
                    database.version == 20 &&
                        (
                            backupDatabaseVersion == 18 ||
                            backupDatabaseVersion == 19
                        ) &&
                        tableName == "harvest_days" -> {
                        setOf(
                            "workIntervalMinutes",
                            "unloadIntervalMinutes",
                            "lunchIntervalMinutes",
                            "dinnerIntervalMinutes",
                            "waitingTransportIntervalMinutes",
                            "workIntervalCustom",
                            "unloadIntervalCustom",
                            "lunchIntervalCustom",
                            "dinnerIntervalCustom",
                            "waitingTransportIntervalCustom",
                            "reminderSettingsInitialized"
                        )
                    }

                    database.version == 20 &&
                        backupDatabaseVersion == 18 &&
                        tableName == "harvest_bunkers" -> {
                        setOf(
                            "postEventType"
                        )
                    }

                    else -> {
                        emptySet()
                    }
                }

            val invalidMissingColumns =
                missingColumns -
                    allowedMissingColumns

            val hasInvalidStructure =
                extraColumns.isNotEmpty() ||
                    invalidMissingColumns.isNotEmpty()

            if (
                hasInvalidStructure
            ) {

                val message =
                    buildString {

                        append(
                            "Неверная структура таблицы $tableName."
                        )

                        if (
                            invalidMissingColumns.isNotEmpty()
                        ) {

                            append(
                                "\nОтсутствуют поля: "
                            )

                            append(
                                invalidMissingColumns.joinToString()
                            )
                        }

                        if (
                            extraColumns.isNotEmpty()
                        ) {

                            append(
                                "\nНеизвестные поля: "
                            )

                            append(
                                extraColumns.joinToString()
                            )
                        }
                    }

                error(message)
            }

            /*
             * Дополнительно проверяем BLOB,
             * если такие данные присутствуют.
             */
            for (
                columnName in actualColumns
            ) {

                val value =
                    row.get(
                        columnName
                    )

                if (
                    value is JSONObject &&
                    value.optString(
                        "__type"
                    ) == "blob"
                ) {

                    if (
                        !value.has(
                            "value"
                        )
                    ) {
                        error(
                            "Повреждены двоичные данные " +
                                    "в таблице $tableName"
                        )
                    }

                    try {

                        Base64.decode(
                            value.getString(
                                "value"
                            ),
                            Base64.NO_WRAP
                        )

                    } catch (
                        e: IllegalArgumentException
                    ) {

                        error(
                            "Повреждены двоичные данные " +
                                    "в таблице $tableName"
                        )
                    }
                }
            }
        }
    }
}

private fun getColumnNames(
    database: SupportSQLiteDatabase,
    tableName: String
): Set<String> {

    validateTableName(
        tableName
    )

    val columns =
        mutableSetOf<String>()

    database.query(
        "PRAGMA table_info(`${tableName}`)"
    ).use { cursor ->

        val nameIndex =
            cursor.getColumnIndexOrThrow(
                "name"
            )

        while (
            cursor.moveToNext()
        ) {

            columns.add(
                cursor.getString(
                    nameIndex
                )
            )
        }
    }

    if (
        columns.isEmpty()
    ) {
        error(
            "Не удалось определить структуру таблицы $tableName"
        )
    }

    return columns
  }
}