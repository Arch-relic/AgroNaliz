package ru.fo6osik.workjournal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CombineCatalogDao {

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertBrand(
        brand: CombineBrand
    ): Long


    @Query(
        """
        SELECT * FROM combine_brands
        ORDER BY name
        """
    )
    suspend fun getAllBrands():
        List<CombineBrand>


    @Query(
        """
        SELECT * FROM combine_brands
        WHERE name = :name COLLATE NOCASE
        LIMIT 1
        """
    )
    suspend fun getBrandByName(
        name: String
    ): CombineBrand?


    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertModel(
        model: CombineModel
    ): Long


    @Query(
        """
        SELECT * FROM combine_models
        WHERE brandId = :brandId
        ORDER BY name
        """
    )
    suspend fun getModelsByBrand(
        brandId: Int
    ): List<CombineModel>


    @Query(
        """
        SELECT * FROM combine_models
        WHERE brandId = :brandId
        AND name = :name COLLATE NOCASE
        LIMIT 1
        """
    )
    suspend fun getModelByName(
        brandId: Int,
        name: String
    ): CombineModel?


    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertModelEngine(
        relation: CombineModelEngine
    )


    @Query(
        """
        SELECT car_engines.*
        FROM car_engines
        INNER JOIN combine_model_engines
        ON car_engines.id =
        combine_model_engines.engineId
        WHERE combine_model_engines.modelId =
        :modelId
        ORDER BY car_engines.powerHp
        """
    )
    suspend fun getEnginesByModel(
        modelId: Int
    ): List<CarEngine>
}