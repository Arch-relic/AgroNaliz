package ru.fo6osik.workjournal

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Vehicle::class,
        MaintenanceRecord::class,

        CarBrand::class,
        CarModel::class,
        CarEngine::class,
        CarModelEngine::class,

        TractorBrand::class,
        TractorModel::class,
        TractorModelEngine::class,

        CombineBrand::class,
        CombineModel::class,
        CombineModelEngine::class,

        FieldEntity::class,

        WorkType::class,
        SeasonProcess::class,
        SeasonWork::class,
        SeasonWorkDay::class,
        SeasonWorkVehicle::class,
		
		FarmEvent::class,
        FarmEventField::class,

        Aggregate::class,
        FarmEventAggregate::class,
        FarmEventVehicle::class,
        FarmEventProcess::class,
        SeasonWorkAggregate::class,

        HarvestProcess::class,
        HarvestDay::class,
        HarvestBunker::class,
        CombineSpec::class
    ],
    version = 20,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao

    abstract fun maintenanceDao(): MaintenanceDao

    abstract fun carCatalogDao(): CarCatalogDao

    abstract fun tractorCatalogDao(): TractorCatalogDao

    abstract fun combineCatalogDao(): CombineCatalogDao

    abstract fun fieldDao(): FieldDao

    abstract fun workTypeDao(): WorkTypeDao

    abstract fun seasonProcessDao(): SeasonProcessDao

    abstract fun seasonWorkDao(): SeasonWorkDao
	
	abstract fun farmEventDao(): FarmEventDao

    abstract fun farmEventFieldDao(): FarmEventFieldDao

    abstract fun aggregateDao(): AggregateDao

    abstract fun objectLinkDao(): ObjectLinkDao

    abstract fun harvestDao(): HarvestDao

    abstract fun combineSpecDao(): CombineSpecDao
}
