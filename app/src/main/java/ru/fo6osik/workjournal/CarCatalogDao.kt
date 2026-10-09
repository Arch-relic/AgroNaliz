package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CarCatalogDao {

    // -------------------------
    // Марки
    // -------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBrand(
        brand: CarBrand
    ): Long

    @Query(
        "SELECT * FROM car_brands " +
        "ORDER BY name ASC"
    )
    suspend fun getAllBrands(): List<CarBrand>

    @Query(
        "SELECT * FROM car_brands " +
        "WHERE name LIKE '%' || :query || '%' " +
        "ORDER BY name ASC"
    )
    suspend fun searchBrands(
        query: String
    ): List<CarBrand>

    @Query(
        "SELECT * FROM car_brands " +
        "WHERE name = :name COLLATE NOCASE " +
        "LIMIT 1"
    )
    suspend fun getBrandByName(
        name: String
    ): CarBrand?


    // -------------------------
    // Модели
    // -------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertModel(
        model: CarModel
    ): Long

    @Query(
        "SELECT * FROM car_models " +
        "WHERE brandId = :brandId " +
        "ORDER BY name ASC"
    )
    suspend fun getModelsByBrand(
        brandId: Int
    ): List<CarModel>

    @Query(
        "SELECT * FROM car_models " +
        "WHERE brandId = :brandId " +
        "AND name LIKE '%' || :query || '%' " +
        "ORDER BY name ASC"
    )
    suspend fun searchModels(
        brandId: Int,
        query: String
    ): List<CarModel>

    @Query(
        "SELECT * FROM car_models " +
        "WHERE brandId = :brandId " +
        "AND name = :name COLLATE NOCASE " +
        "LIMIT 1"
    )
    suspend fun getModelByName(
        brandId: Int,
        name: String
    ): CarModel?


    // -------------------------
    // Двигатели
    // -------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEngine(
        engine: CarEngine
    ): Long

    @Query(
        "SELECT * FROM car_engines " +
        "WHERE code LIKE '%' || :query || '%' " +
        "OR manufacturer LIKE '%' || :query || '%' " +
        "ORDER BY manufacturer ASC, code ASC"
    )
    suspend fun searchEngines(
        query: String
    ): List<CarEngine>

    @Query(
        """
        SELECT car_engines.*
        FROM car_engines
        INNER JOIN car_model_engines
        ON car_engines.id = car_model_engines.engineId
        WHERE car_model_engines.modelId = :modelId
        ORDER BY car_engines.manufacturer ASC,
                 car_engines.code ASC
        """
    )
    suspend fun getEnginesByModel(
        modelId: Int
    ): List<CarEngine>


    // -------------------------
    // Связь модель ↔ двигатель
    // -------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertModelEngine(
        relation: CarModelEngine
    )
	
	    @Query(
    """
    SELECT * FROM car_engines
    WHERE manufacturer COLLATE NOCASE = :manufacturer
    AND code COLLATE NOCASE = :code
    AND powerHp = :powerHp
    LIMIT 1
    """
)
suspend fun getEngine(
    manufacturer: String,
    code: String,
    powerHp: Int
): CarEngine?

@Query(
    "SELECT * FROM car_engines " +
    "WHERE id = :id LIMIT 1"
)
suspend fun getEngineById(
    id: Int
): CarEngine?

}