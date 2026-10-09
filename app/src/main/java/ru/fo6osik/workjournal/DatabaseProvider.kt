package ru.fo6osik.workjournal

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `maintenance_records` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `vehicleId` INTEGER NOT NULL,
                    `date` INTEGER NOT NULL,
                    `meterValue` INTEGER,
                    `meterType` TEXT NOT NULL,
                    `work` TEXT NOT NULL,
                    `note` TEXT NOT NULL,
                    FOREIGN KEY(`vehicleId`)
                    REFERENCES `vehicles`(`id`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_maintenance_records_vehicleId`
                ON `maintenance_records` (`vehicleId`)
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `car_brands` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `userCreated` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                `index_car_brands_name`
                ON `car_brands` (`name`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `car_models` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `brandId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `userCreated` INTEGER NOT NULL,
                    FOREIGN KEY(`brandId`)
                    REFERENCES `car_brands`(`id`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_car_models_brandId`
                ON `car_models` (`brandId`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                `index_car_models_brandId_name`
                ON `car_models` (`brandId`, `name`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `car_engines` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `manufacturer` TEXT NOT NULL,
                    `code` TEXT NOT NULL,
                    `displacementCc` INTEGER,
                    `powerHp` INTEGER,
                    `cylinders` INTEGER,
                    `valves` INTEGER,
                    `fuelType` TEXT NOT NULL,
                    `userCreated` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_car_engines_code`
                ON `car_engines` (`code`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `car_model_engines` (
                    `modelId` INTEGER NOT NULL,
                    `engineId` INTEGER NOT NULL,
                    PRIMARY KEY(`modelId`, `engineId`),
                    FOREIGN KEY(`modelId`)
                    REFERENCES `car_models`(`id`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE,
                    FOREIGN KEY(`engineId`)
                    REFERENCES `car_engines`(`id`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_car_model_engines_modelId`
                ON `car_model_engines` (`modelId`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_car_model_engines_engineId`
                ON `car_model_engines` (`engineId`)
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {

            database.execSQL(
                """
                ALTER TABLE `vehicles`
                ADD COLUMN `engineId` INTEGER
                """.trimIndent()
            )

            database.execSQL(
                """
                ALTER TABLE `vehicles`
                ADD COLUMN `engineName` TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )
        }
    }
	
	private val MIGRATION_4_5 = object : Migration(4, 5) {

    override fun migrate(
        database: SupportSQLiteDatabase
    ) {

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tractor_brands` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `userCreated` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_tractor_brands_name`
            ON `tractor_brands` (`name`)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tractor_models` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `brandId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `userCreated` INTEGER NOT NULL,
                FOREIGN KEY(`brandId`)
                REFERENCES `tractor_brands`(`id`)
                ON UPDATE NO ACTION
                ON DELETE CASCADE
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_tractor_models_brandId`
            ON `tractor_models` (`brandId`)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_tractor_models_brandId_name`
            ON `tractor_models` (`brandId`, `name`)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tractor_model_engines` (
                `modelId` INTEGER NOT NULL,
                `engineId` INTEGER NOT NULL,
                PRIMARY KEY(`modelId`, `engineId`),
                FOREIGN KEY(`modelId`)
                REFERENCES `tractor_models`(`id`)
                ON UPDATE NO ACTION
                ON DELETE CASCADE,
                FOREIGN KEY(`engineId`)
                REFERENCES `car_engines`(`id`)
                ON UPDATE NO ACTION
                ON DELETE CASCADE
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_tractor_model_engines_modelId`
            ON `tractor_model_engines` (`modelId`)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_tractor_model_engines_engineId`
            ON `tractor_model_engines` (`engineId`)
            """.trimIndent()
        )
    }
}

private val MIGRATION_5_6 =
    object : Migration(5, 6) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {

            database.execSQL(
                """
                ALTER TABLE `tractor_models`
                ADD COLUMN `baseModelId` INTEGER
                """.trimIndent()
            )
        }
    }
	
	private val MIGRATION_6_7 =
    object : Migration(6, 7) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `combine_brands` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `userCreated` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                `index_combine_brands_name`
                ON `combine_brands` (`name`)
                """.trimIndent()
            )


            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `combine_models` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `brandId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `userCreated` INTEGER NOT NULL,
                    FOREIGN KEY(`brandId`)
                    REFERENCES `combine_brands`(`id`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_combine_models_brandId`
                ON `combine_models` (`brandId`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                `index_combine_models_brandId_name`
                ON `combine_models` (`brandId`, `name`)
                """.trimIndent()
            )


            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `combine_model_engines` (
                    `modelId` INTEGER NOT NULL,
                    `engineId` INTEGER NOT NULL,
                    PRIMARY KEY(`modelId`, `engineId`),
                    FOREIGN KEY(`modelId`)
                    REFERENCES `combine_models`(`id`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE,
                    FOREIGN KEY(`engineId`)
                    REFERENCES `car_engines`(`id`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_combine_model_engines_modelId`
                ON `combine_model_engines` (`modelId`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_combine_model_engines_engineId`
                ON `combine_model_engines` (`engineId`)
                """.trimIndent()
            )
        }
    }


	private val MIGRATION_7_8 =
    object : Migration(7, 8) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `fields` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `areaHa` REAL,
                    `location` TEXT,
                    `note` TEXT,
                    `isActive` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }


    private val MIGRATION_8_9 =
        object : Migration(8, 9) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `work_types` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_work_types_name`
                    ON `work_types` (`name`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `season_works` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `workDate` TEXT NOT NULL,
                        `workTypeId` INTEGER NOT NULL,
                        `fieldId` INTEGER,
                        `areaHa` REAL,
                        `note` TEXT,
                        FOREIGN KEY(`workTypeId`)
                            REFERENCES `work_types`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE RESTRICT,
                        FOREIGN KEY(`fieldId`)
                            REFERENCES `fields`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_season_works_workTypeId`
                    ON `season_works` (`workTypeId`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_season_works_fieldId`
                    ON `season_works` (`fieldId`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_season_works_workDate`
                    ON `season_works` (`workDate`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `season_work_vehicles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `seasonWorkId` INTEGER NOT NULL,
                        `vehicleId` INTEGER,
                        `vehicleNameSnapshot` TEXT NOT NULL,
                        FOREIGN KEY(`seasonWorkId`)
                            REFERENCES `season_works`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE,
                        FOREIGN KEY(`vehicleId`)
                            REFERENCES `vehicles`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_season_work_vehicles_seasonWorkId`
                    ON `season_work_vehicles` (`seasonWorkId`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_season_work_vehicles_vehicleId`
                    ON `season_work_vehicles` (`vehicleId`)
                    """.trimIndent()
                )

                // Стартовый набор видов работ.
                // Пользователь сможет добавлять свои виды позже.
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `work_types`
                    (`name`, `isActive`)
                    VALUES
                    ('Дискование', 1),
                    ('Культивация', 1),
                    ('Посев', 1),
                    ('Опрыскивание', 1),
                    ('Внесение удобрений', 1),
                    ('Уборка', 1),
                    ('Транспортировка', 1),
                    ('Прочее', 1)
                    """.trimIndent()
                )
            }
        }
		
		private val MIGRATION_9_10 =
    object : Migration(9, 10) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `farm_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `eventUid` TEXT NOT NULL,
                    `eventType` TEXT NOT NULL,
                    `targetModule` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `eventYear` INTEGER NOT NULL,
                    `eventMonth` INTEGER,
                    `eventDay` INTEGER,
                    `datePrecision` TEXT NOT NULL,
                    `payloadJson` TEXT,
                    `targetRecordUid` TEXT,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                `index_farm_events_eventUid`
                ON `farm_events` (`eventUid`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_farm_events_targetModule`
                ON `farm_events` (`targetModule`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_farm_events_eventYear_eventMonth`
                ON `farm_events` (`eventYear`, `eventMonth`)
                """.trimIndent()
            )
        }
    }
	
	private val MIGRATION_10_11 =
    object : Migration(10, 11) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {

            /*
             * 1. Добавляем связь сезонной работы
             * с главным журналом.
             */
            database.execSQL(
                """
                ALTER TABLE season_works
                ADD COLUMN eventUid TEXT
                """.trimIndent()
            )

            /*
             * 2. Для всех уже существующих сезонных работ
             * создаём события общего журнала.
             *
             * UID делаем детерминированным:
             * season-work-1
             * season-work-2
             * ...
             */
            database.execSQL(
                """
                INSERT OR IGNORE INTO farm_events (
                    eventUid,
                    eventType,
                    targetModule,
                    title,
                    description,
                    eventYear,
                    eventMonth,
                    eventDay,
                    datePrecision,
                    payloadJson,
                    targetRecordUid,
                    createdAt
                )
                SELECT
                    'season-work-' || sw.id,
                    'FIELD_WORK',
                    'SEASON_WORKS',
                    COALESCE(
                        wt.name,
                        'Сезонная работа'
                    ),
                    sw.note,
                    CAST(
                        substr(
                            sw.workDate,
                            1,
                            4
                        ) AS INTEGER
                    ),
                    CAST(
                        substr(
                            sw.workDate,
                            6,
                            2
                        ) AS INTEGER
                    ),
                    CAST(
                        substr(
                            sw.workDate,
                            9,
                            2
                        ) AS INTEGER
                    ),
                    'DAY',
                    NULL,
                    NULL,
                    CAST(
                        strftime('%s', 'now')
                        AS INTEGER
                    ) * 1000
                FROM season_works sw
                LEFT JOIN work_types wt
                    ON wt.id = sw.workTypeId
                """.trimIndent()
            )

            /*
             * 3. Привязываем существующие сезонные записи
             * к созданным событиям.
             */
            database.execSQL(
                """
                UPDATE season_works
                SET eventUid =
                    'season-work-' || id
                WHERE eventUid IS NULL
                """.trimIndent()
            )

            /*
             * 4. Одна специализированная запись
             * на одно событие.
             */
            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_season_works_eventUid
                ON season_works(eventUid)
                """.trimIndent()
            )
        }
    }


    private val MIGRATION_11_12 =
        object : Migration(11, 12) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                /*
                 * Связь общего события с одним или несколькими полями.
                 *
                 * eventUid используется вместо локального id события,
                 * поэтому связь переживает импорт/экспорт и перенос данных.
                 */
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `farm_event_fields` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventUid` TEXT NOT NULL,
                        `fieldId` INTEGER NOT NULL,
                        FOREIGN KEY(`eventUid`)
                            REFERENCES `farm_events`(`eventUid`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE,
                        FOREIGN KEY(`fieldId`)
                            REFERENCES `fields`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_farm_event_fields_eventUid_fieldId`
                    ON `farm_event_fields` (`eventUid`, `fieldId`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_farm_event_fields_fieldId`
                    ON `farm_event_fields` (`fieldId`)
                    """.trimIndent()
                )

                /*
                 * Существующие сезонные работы уже знают fieldId и eventUid.
                 * Автоматически переносим эти связи в общий слой событий.
                 */
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `farm_event_fields` (
                        `eventUid`,
                        `fieldId`
                    )
                    SELECT
                        `eventUid`,
                        `fieldId`
                    FROM `season_works`
                    WHERE `eventUid` IS NOT NULL
                      AND `fieldId` IS NOT NULL
                    """.trimIndent()
                )
            }
        }


    private val MIGRATION_12_13 =
        object : Migration(12, 13) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                /*
                 * Привязываем специализированную запись ТО
                 * к главному событию журнала.
                 *
                 * Старые записи остаются с eventUid = null.
                 */
                database.execSQL(
                    """
                    ALTER TABLE `maintenance_records`
                    ADD COLUMN `eventUid` TEXT
                    """.trimIndent()
                )

                /*
                 * DAY — обычная точная дата.
                 * MONTH — известны только месяц и год.
                 */
                database.execSQL(
                    """
                    ALTER TABLE `maintenance_records`
                    ADD COLUMN `datePrecision`
                    TEXT NOT NULL DEFAULT 'DAY'
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_maintenance_records_eventUid`
                    ON `maintenance_records` (`eventUid`)
                    """.trimIndent()
                )
            }
        }


    private val MIGRATION_13_14 =
        object : Migration(13, 14) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                /*
                 * season_works становится родительской записью:
                 * дата начала + дата окончания + статус.
                 */
                database.execSQL(
                    """
                    ALTER TABLE `season_works`
                    ADD COLUMN `endDate` TEXT
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `season_works`
                    ADD COLUMN `status`
                    TEXT NOT NULL DEFAULT 'COMPLETED'
                    """.trimIndent()
                )

                /*
                 * Все старые сезонные записи были одиночными.
                 * Для них начало = окончание.
                 */
                database.execSQL(
                    """
                    UPDATE `season_works`
                    SET `endDate` = `workDate`
                    WHERE `endDate` IS NULL
                    """.trimIndent()
                )

                /*
                 * Дочерняя хронология дней/этапов работы.
                 */
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `season_work_days` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dayUid` TEXT NOT NULL,
                        `seasonWorkId` INTEGER NOT NULL,
                        `workDate` TEXT NOT NULL,
                        `endDate` TEXT,
                        `areaHa` REAL,
                        `cumulativeAreaHa` REAL,
                        `stage` TEXT NOT NULL,
                        `note` TEXT,
                        FOREIGN KEY(`seasonWorkId`)
                            REFERENCES `season_works`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_season_work_days_dayUid`
                    ON `season_work_days` (`dayUid`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_season_work_days_seasonWorkId`
                    ON `season_work_days` (`seasonWorkId`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_season_work_days_workDate`
                    ON `season_work_days` (`workDate`)
                    """.trimIndent()
                )

                /*
                 * Добавляем недостающий тип сезонной работы.
                 * INSERT OR IGNORE защищает от дубля, если он уже есть.
                 */
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `work_types` (
                        `name`,
                        `isActive`
                    )
                    SELECT
                        'Пахота',
                        1
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM `work_types`
                        WHERE `name` = 'Пахота'
                    )
                    """.trimIndent()
                )

                /*
                 * Для каждой существующей одиночной работы
                 * создаём один дочерний этап.
                 */
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `season_work_days` (
                        `dayUid`,
                        `seasonWorkId`,
                        `workDate`,
                        `endDate`,
                        `areaHa`,
                        `cumulativeAreaHa`,
                        `stage`,
                        `note`
                    )
                    SELECT
                        'season-work-day-' || `id`,
                        `id`,
                        `workDate`,
                        NULL,
                        `areaHa`,
                        `areaHa`,
                        'WORK',
                        `note`
                    FROM `season_works`
                    """.trimIndent()
                )
            }
        }


    private val MIGRATION_14_15 =
        object : Migration(14, 15) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `season_processes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `processUid` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `fieldId` INTEGER,
                        `startDate` TEXT NOT NULL,
                        `endDate` TEXT,
                        `status` TEXT NOT NULL,
                        `crop` TEXT,
                        `goal` TEXT,
                        `areaHa` REAL,
                        `note` TEXT,
                        FOREIGN KEY(`fieldId`)
                            REFERENCES `fields`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_season_processes_processUid`
                    ON `season_processes` (`processUid`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_season_processes_fieldId`
                    ON `season_processes` (`fieldId`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_season_processes_startDate`
                    ON `season_processes` (`startDate`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `season_works`
                    ADD COLUMN `processId` INTEGER
                    REFERENCES `season_processes`(`id`)
                    ON UPDATE NO ACTION
                    ON DELETE SET NULL
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_season_works_processId`
                    ON `season_works` (`processId`)
                    """.trimIndent()
                )

                /*
                 * Уже загруженный апрель v14:
                 * посев + повторное боронование
                 * объединяем в один рабочий процесс.
                 */
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `season_processes` (
                        `processUid`,
                        `title`,
                        `fieldId`,
                        `startDate`,
                        `endDate`,
                        `status`,
                        `crop`,
                        `goal`,
                        `areaHa`,
                        `note`
                    )
                    SELECT
                        'process-pea-sowing-field-2-2026',
                        'Посевная гороха — Поле №2',
                        `fields`.`id`,
                        '2026-04-23',
                        '2026-05-02',
                        'COMPLETED',
                        'Горох',
                        'Подготовка почвы и посев гороха на Поле №2',
                        87.0,
                        'Общий агротехнологический процесс: боронование и посев.'
                    FROM `fields`
                    WHERE `fields`.`name` = 'Поле №2'
                      AND EXISTS (
                          SELECT 1
                          FROM `season_works`
                          WHERE `eventUid` =
                              '1ffe1080-14d0-5035-a914-5e2cb24af14a'
                      )
                    LIMIT 1
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    UPDATE `season_works`
                    SET `processId` = (
                        SELECT `id`
                        FROM `season_processes`
                        WHERE `processUid` =
                            'process-pea-sowing-field-2-2026'
                        LIMIT 1
                    )
                    WHERE `eventUid` IN (
                        '1ffe1080-14d0-5035-a914-5e2cb24af14a',
                        '97a2cff1-5f64-5108-b84a-4680a7bead99'
                    )
                      AND EXISTS (
                          SELECT 1
                          FROM `season_processes`
                          WHERE `processUid` =
                              'process-pea-sowing-field-2-2026'
                      )
                    """.trimIndent()
                )

                /*
                 * Повторное боронование было частью посевной
                 * на Поле №2 — уточняем структурную связь.
                 */
                database.execSQL(
                    """
                    UPDATE `season_works`
                    SET `fieldId` = (
                        SELECT `id`
                        FROM `fields`
                        WHERE `name` = 'Поле №2'
                        LIMIT 1
                    )
                    WHERE `eventUid` =
                        '97a2cff1-5f64-5108-b84a-4680a7bead99'
                      AND `fieldId` IS NULL
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `farm_event_fields` (
                        `eventUid`,
                        `fieldId`
                    )
                    SELECT
                        '97a2cff1-5f64-5108-b84a-4680a7bead99',
                        `id`
                    FROM `fields`
                    WHERE `name` = 'Поле №2'
                      AND EXISTS (
                          SELECT 1
                          FROM `farm_events`
                          WHERE `eventUid` =
                              '97a2cff1-5f64-5108-b84a-4680a7bead99'
                      )
                    LIMIT 1
                    """.trimIndent()
                )
            }
        }


    private val MIGRATION_15_16 =
        object : Migration(15, 16) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `aggregates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `aggregateUid` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `parentAggregateId` INTEGER,
                        `workingWidthM` REAL,
                        `note` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        FOREIGN KEY(`parentAggregateId`)
                            REFERENCES `aggregates`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_aggregates_aggregateUid`
                    ON `aggregates` (`aggregateUid`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_aggregates_parentAggregateId`
                    ON `aggregates` (`parentAggregateId`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_aggregates_category`
                    ON `aggregates` (`category`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `aggregates` (
                        `aggregateUid`,
                        `name`,
                        `category`,
                        `parentAggregateId`,
                        `workingWidthM`,
                        `note`,
                        `isActive`
                    )
                    VALUES (
                        'aggregate-sshg-15a-001',
                        'СШГ-15А',
                        'Бороновальная сцепка',
                        NULL,
                        15.0,
                        '',
                        1
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `farm_event_aggregates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventUid` TEXT NOT NULL,
                        `aggregateId` INTEGER NOT NULL,
                        `relationType` TEXT NOT NULL,
                        `note` TEXT,
                        FOREIGN KEY(`eventUid`)
                            REFERENCES `farm_events`(`eventUid`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE,
                        FOREIGN KEY(`aggregateId`)
                            REFERENCES `aggregates`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_farm_event_aggregates_eventUid_aggregateId`
                    ON `farm_event_aggregates` (
                        `eventUid`,
                        `aggregateId`
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_farm_event_aggregates_aggregateId`
                    ON `farm_event_aggregates` (`aggregateId`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `farm_event_vehicles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventUid` TEXT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `relationType` TEXT NOT NULL,
                        `note` TEXT,
                        FOREIGN KEY(`eventUid`)
                            REFERENCES `farm_events`(`eventUid`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE,
                        FOREIGN KEY(`vehicleId`)
                            REFERENCES `vehicles`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_farm_event_vehicles_eventUid_vehicleId`
                    ON `farm_event_vehicles` (
                        `eventUid`,
                        `vehicleId`
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_farm_event_vehicles_vehicleId`
                    ON `farm_event_vehicles` (`vehicleId`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `farm_event_processes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventUid` TEXT NOT NULL,
                        `processId` INTEGER NOT NULL,
                        FOREIGN KEY(`eventUid`)
                            REFERENCES `farm_events`(`eventUid`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE,
                        FOREIGN KEY(`processId`)
                            REFERENCES `season_processes`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_farm_event_processes_eventUid_processId`
                    ON `farm_event_processes` (
                        `eventUid`,
                        `processId`
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_farm_event_processes_processId`
                    ON `farm_event_processes` (`processId`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `season_work_aggregates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `seasonWorkId` INTEGER NOT NULL,
                        `aggregateId` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        `note` TEXT,
                        FOREIGN KEY(`seasonWorkId`)
                            REFERENCES `season_works`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE,
                        FOREIGN KEY(`aggregateId`)
                            REFERENCES `aggregates`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_season_work_aggregates_seasonWorkId_aggregateId`
                    ON `season_work_aggregates` (
                        `seasonWorkId`,
                        `aggregateId`
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_season_work_aggregates_aggregateId`
                    ON `season_work_aggregates` (`aggregateId`)
                    """.trimIndent()
                )
            }
        }


private val MIGRATION_16_17 =
    object : Migration(16, 17) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `harvest_processes` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `processUid` TEXT NOT NULL,
                    `startDate` TEXT NOT NULL,
                    `endDate` TEXT,
                    `status` TEXT NOT NULL,
                    `fieldId` INTEGER,
                    `fieldNameSnapshot` TEXT NOT NULL,
                    `crop` TEXT NOT NULL,
                    `combineVehicleId` INTEGER,
                    `combineNameSnapshot` TEXT NOT NULL,
                    `bunkerVolumeM3` REAL,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`fieldId`)
                        REFERENCES `fields`(`id`)
                        ON UPDATE NO ACTION
                        ON DELETE SET NULL,
                    FOREIGN KEY(`combineVehicleId`)
                        REFERENCES `vehicles`(`id`)
                        ON UPDATE NO ACTION
                        ON DELETE SET NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                `index_harvest_processes_processUid`
                ON `harvest_processes` (`processUid`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_harvest_processes_fieldId`
                ON `harvest_processes` (`fieldId`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_harvest_processes_combineVehicleId`
                ON `harvest_processes` (`combineVehicleId`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_harvest_processes_startDate`
                ON `harvest_processes` (`startDate`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_harvest_processes_status`
                ON `harvest_processes` (`status`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `harvest_days` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `dayUid` TEXT NOT NULL,
                    `processId` INTEGER NOT NULL,
                    `workDate` TEXT NOT NULL,
                    `startTime` TEXT NOT NULL,
                    `endTime` TEXT,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`processId`)
                        REFERENCES `harvest_processes`(`id`)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                `index_harvest_days_dayUid`
                ON `harvest_days` (`dayUid`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_harvest_days_processId`
                ON `harvest_days` (`processId`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_harvest_days_workDate`
                ON `harvest_days` (`workDate`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                `index_harvest_days_processId_workDate`
                ON `harvest_days` (`processId`, `workDate`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `harvest_bunkers` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `bunkerUid` TEXT NOT NULL,
                    `dayId` INTEGER NOT NULL,
                    `fillTime` TEXT NOT NULL,
                    `fillLevel` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`dayId`)
                        REFERENCES `harvest_days`(`id`)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                `index_harvest_bunkers_bunkerUid`
                ON `harvest_bunkers` (`bunkerUid`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_harvest_bunkers_dayId`
                ON `harvest_bunkers` (`dayId`)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_harvest_bunkers_fillTime`
                ON `harvest_bunkers` (`fillTime`)
                """.trimIndent()
            )
        }
    }


    private val MIGRATION_17_18 =
        object : Migration(17, 18) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `combine_specs` (
                        `vehicleId` INTEGER NOT NULL,
                        `grainTankVolumeM3` REAL,
                        PRIMARY KEY(`vehicleId`),
                        FOREIGN KEY(`vehicleId`)
                            REFERENCES `vehicles`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

    /*
     * v19:
     * сохраняем событие, происходящее после конкретного бункера.
     *
     * Поле nullable намеренно:
     * старые бункеры из v18 получают null, чтобы приложение
     * не считало пропущенные события обычным продолжением работы.
     */
    private val MIGRATION_18_19 =
        object : Migration(18, 19) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {
                database.execSQL(
                    """
                    ALTER TABLE `harvest_bunkers`
                    ADD COLUMN `postEventType` TEXT
                    """.trimIndent()
                )
            }
        }

    /*
     * v20:
     * индивидуальные настройки напоминаний для каждого рабочего дня.
     *
     * reminderSettingsInitialized = 0 у старых дней:
     * при первом открытии переносим прежний основной интервал
     * из SharedPreferences, чтобы не сбросить текущую настройку.
     */
    private val MIGRATION_19_20 =
        object : Migration(19, 20) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {
                database.execSQL(
                    """
                    ALTER TABLE `harvest_days`
                    ADD COLUMN `workIntervalMinutes`
                    INTEGER NOT NULL DEFAULT 30
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `harvest_days`
                    ADD COLUMN `unloadIntervalMinutes`
                    INTEGER NOT NULL DEFAULT 5
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `harvest_days`
                    ADD COLUMN `lunchIntervalMinutes`
                    INTEGER NOT NULL DEFAULT 15
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `harvest_days`
                    ADD COLUMN `dinnerIntervalMinutes`
                    INTEGER NOT NULL DEFAULT 15
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `harvest_days`
                    ADD COLUMN `waitingTransportIntervalMinutes`
                    INTEGER NOT NULL DEFAULT 30
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `harvest_days`
                    ADD COLUMN `workIntervalCustom`
                    INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `harvest_days`
                    ADD COLUMN `unloadIntervalCustom`
                    INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `harvest_days`
                    ADD COLUMN `lunchIntervalCustom`
                    INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `harvest_days`
                    ADD COLUMN `dinnerIntervalCustom`
                    INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `harvest_days`
                    ADD COLUMN `waitingTransportIntervalCustom`
                    INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE `harvest_days`
                    ADD COLUMN `reminderSettingsInitialized`
                    INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
            }
        }

    fun getDatabase(
        context: Context
    ): AppDatabase {

        return INSTANCE ?: synchronized(this) {

            val instance =
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agrouchet_database"
                ).addMigrations(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
	MIGRATION_10_11,
    MIGRATION_11_12,
    MIGRATION_12_13,
    MIGRATION_13_14,
    MIGRATION_14_15,
    MIGRATION_15_16,
    MIGRATION_16_17,
    MIGRATION_17_18,
    MIGRATION_18_19,
    MIGRATION_19_20
)
                    .build()

            INSTANCE = instance

            instance
        }
    }
}