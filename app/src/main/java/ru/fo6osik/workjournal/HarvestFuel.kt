package ru.fo6osik.workjournal

import androidx.room.*
import java.util.UUID

@Entity(tableName="harvest_fuel_days", foreignKeys=[
    ForeignKey(entity=HarvestDay::class,parentColumns=["id"],childColumns=["dayId"],onDelete=ForeignKey.CASCADE),
    ForeignKey(entity=Vehicle::class,parentColumns=["id"],childColumns=["vehicleId"],onDelete=ForeignKey.SET_NULL)
], indices=[Index(value=["dayUid"],unique=true),Index(value=["vehicleId"])])
data class HarvestFuelDay(@PrimaryKey val dayId:Int, val dayUid:String, val vehicleId:Int?,
    val vehicleNameSnapshot:String, val startPercent:Double?=null, val endPercent:Double?=null)

@Entity(tableName="harvest_refuelings",foreignKeys=[
    ForeignKey(entity=HarvestDay::class,parentColumns=["id"],childColumns=["dayId"],onDelete=ForeignKey.CASCADE),
    ForeignKey(entity=Vehicle::class,parentColumns=["id"],childColumns=["vehicleId"],onDelete=ForeignKey.SET_NULL)
],indices=[Index(value=["refuelUid"],unique=true),Index(value=["dayId"]),Index(value=["vehicleId"])])
data class HarvestRefueling(@PrimaryKey(autoGenerate=true) val id:Int=0,
    val refuelUid:String=UUID.randomUUID().toString(), val dayId:Int, val dayUid:String,
    val vehicleId:Int?, val vehicleNameSnapshot:String, val scope:String="DAY",
    val beforePercent:Double, val afterPercent:Double, val time:String?=null,
    val createdAt:Long=System.currentTimeMillis())

@Dao
interface HarvestFuelDao {
    @Query("SELECT * FROM harvest_fuel_days WHERE dayId=:id") fun getDay(id:Int):HarvestFuelDay?
    @Query("SELECT * FROM harvest_refuelings WHERE dayId=:id ORDER BY createdAt,id") fun getRefills(id:Int):List<HarvestRefueling>
    @Query("SELECT * FROM harvest_refuelings WHERE id=:id") fun getRefill(id:Int):HarvestRefueling?
    @Insert(onConflict=OnConflictStrategy.IGNORE) fun insertDayRaw(day:HarvestFuelDay)
    @Query("UPDATE harvest_fuel_days SET startPercent=:start,endPercent=:end WHERE dayId=:id") fun levelsRaw(id:Int,start:Double?,end:Double?)
    @Insert fun insertRefillRaw(refill:HarvestRefueling):Long
    @Update fun updateRefillRaw(refill:HarvestRefueling)
    @Query("DELETE FROM harvest_refuelings WHERE id=:id") fun deleteRefill(id:Int)
    @Query("SELECT hd.dayUid FROM harvest_days hd WHERE hd.id=:id") fun dayUid(id:Int):String?
    @Query("""SELECT hd.id AS dayId, hd.dayUid AS dayUid, hp.combineVehicleId AS vehicleId,
        hp.combineNameSnapshot AS vehicleNameSnapshot, NULL AS startPercent, NULL AS endPercent
        FROM harvest_days hd JOIN harvest_processes hp ON hp.id=hd.processId WHERE hd.id=:id""") fun initial(id:Int):HarvestFuelDay?
    @Query("""SELECT fd.* FROM harvest_fuel_days fd JOIN harvest_days hd ON hd.id=fd.dayId
        JOIN harvest_days current ON current.id=:id
        WHERE fd.vehicleId=:vehicle AND hd.workDate<current.workDate
        ORDER BY hd.workDate DESC,hd.id DESC LIMIT 1""") fun previous(id:Int,vehicle:Int):HarvestFuelDay?
    @Transaction fun ensure(id:Int):HarvestFuelDay {
        getDay(id)?.let { return it }
        val fresh=requireNotNull(initial(id)) { "Рабочий день удалён" };insertDayRaw(fresh)
        return requireNotNull(getDay(id))
    }
    @Transaction fun saveLevels(id:Int,start:Double?,end:Double?) {
        require(HarvestFuelCalculator.valid(start) && HarvestFuelCalculator.valid(end)) { "Уровень: от 0 до 100%" }
        ensure(id);levelsRaw(id,start,end)
    }
    @Transaction fun saveRefill(dayId:Int,id:Int?,before:Double,after:Double,time:String?,scope:String):Long {
        require(HarvestFuelCalculator.valid(before) && HarvestFuelCalculator.valid(after) && after>=before) { "Заправка: 0–100%, после не ниже, чем до" }
        require(scope in setOf("DAY","BEFORE_DAY"))
        if(time!=null) HarvestChronology.minutes(time)
        val day=ensure(dayId)
        if(id==null) return insertRefillRaw(HarvestRefueling(dayId=dayId,dayUid=day.dayUid,vehicleId=day.vehicleId,
            vehicleNameSnapshot=day.vehicleNameSnapshot,beforePercent=before,afterPercent=after,time=time,scope=scope))
        val old=requireNotNull(getRefill(id));require(old.dayId==dayId)
        updateRefillRaw(old.copy(beforePercent=before,afterPercent=after,time=time,scope=scope));return id.toLong()
    }
}
