package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TractorCatalogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBrand(
        brand: TractorBrand
    ): Long

    @Query(
        "SELECT * FROM tractor_brands " +
        "ORDER BY name ASC"
    )
    suspend fun getAllBrands(): List<TractorBrand>

    @Query(
        "SELECT * FROM tractor_brands " +
        "WHERE name = :name COLLATE NOCASE " +
        "LIMIT 1"
    )
    suspend fun getBrandByName(
        name: String
    ): TractorBrand?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertModel(
        model: TractorModel
    ): Long

    @Query(
        "SELECT * FROM tractor_models " +
        "WHERE brandId = :brandId " +
        "ORDER BY name ASC"
    )
    suspend fun getModelsByBrand(
        brandId: Int
    ): List<TractorModel>

    @Query(
        "SELECT * FROM tractor_models " +
        "WHERE brandId = :brandId " +
        "AND name = :name COLLATE NOCASE " +
        "LIMIT 1"
    )
    suspend fun getModelByName(
        brandId: Int,
        name: String
    ): TractorModel?
	
	@Query(
    """
    UPDATE tractor_models
    SET baseModelId = :baseModelId
    WHERE id = :modelId
    """
)
suspend fun updateBaseModel(
    modelId: Int,
    baseModelId: Int?
)

@Query(
    """
    SELECT * FROM tractor_models
    WHERE id = :modelId
    LIMIT 1
    """
)
suspend fun getModelById(
    modelId: Int
): TractorModel?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertModelEngine(
        relation: TractorModelEngine
    )

    @Query(
        """
        SELECT car_engines.*
        FROM car_engines
        INNER JOIN tractor_model_engines
        ON car_engines.id = tractor_model_engines.engineId
        WHERE tractor_model_engines.modelId = :modelId
        ORDER BY car_engines.manufacturer ASC,
                 car_engines.code ASC
        """
    )
    suspend fun getEnginesByModel(
        modelId: Int
    ): List<CarEngine>
}