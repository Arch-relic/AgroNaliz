package ru.fo6osik.workjournal

import android.content.ContentValues
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONObject

/** Validation shared by full restore and merge import. Old files have no fuel tables. */
object HarvestFuelBackup {
    fun validate(table:String,row:JSONObject) {
        val names=when(table) {
            "harvest_days" -> listOf("moisturePercent")
            "harvest_fuel_days" -> listOf("startPercent","endPercent")
            "harvest_refuelings" -> listOf("beforePercent","afterPercent")
            else -> emptyList()
        }
        for(name in names)if(row.has(name) && !row.isNull(name)) require(HarvestFuelCalculator.valid(row.getDouble(name))) { "Некорректное значение $name" }
        if(table=="harvest_refuelings") {
            require(row.getDouble("afterPercent")>=row.getDouble("beforePercent"))
            require(row.getString("scope") in setOf("DAY","BEFORE_DAY"))
            if(row.has("time") && !row.isNull("time"))HarvestChronology.minutes(row.getString("time"))
        }
    }
    fun importFuel(db:SupportSQLiteDatabase,tables:JSONObject,days:Map<Long,Long>,vehicles:Map<Long,Long>) {
        for(table in listOf("harvest_fuel_days","harvest_refuelings")) {
            val rows=tables.optJSONArray(table) ?: continue
            for(i in 0 until rows.length()) {
                val row=rows.getJSONObject(i);validate(table,row)
                val mapped=requireNotNull(days[row.getLong("dayId")]) { "Нет рабочего дня для топлива" }
                val uid=db.query("SELECT dayUid FROM harvest_days WHERE id=?",arrayOf(mapped)).use { check(it.moveToFirst());it.getString(0) }
                val values=ContentValues().apply {
                    put("dayId",mapped);put("dayUid",uid)
                    val vehicle=if(row.isNull("vehicleId"))null else vehicles[row.getLong("vehicleId")]
                    require(row.isNull("vehicleId") || vehicle!=null) { "Нет техники для топлива" }
                    if(vehicle==null)putNull("vehicleId")else put("vehicleId",vehicle)
                    put("vehicleNameSnapshot",row.getString("vehicleNameSnapshot"))
                    if(table=="harvest_fuel_days") {
                        for(key in listOf("startPercent","endPercent"))if(row.isNull(key))putNull(key)else put(key,row.getDouble(key))
                    } else {
                        put("refuelUid",row.getString("refuelUid"));put("scope",row.getString("scope"))
                        put("beforePercent",row.getDouble("beforePercent"));put("afterPercent",row.getDouble("afterPercent"))
                        if(row.isNull("time"))putNull("time")else put("time",row.getString("time"))
                        put("createdAt",row.getLong("createdAt"))
                    }
                }
                // Merge import preserves existing readings and records on repeated/old imports.
                val exists=if(table=="harvest_fuel_days") db.query("SELECT 1 FROM harvest_fuel_days WHERE dayId=?",arrayOf(mapped)).use { it.moveToFirst() }
                    else db.query("SELECT 1 FROM harvest_refuelings WHERE refuelUid=?",arrayOf(row.getString("refuelUid"))).use { it.moveToFirst() }
                if(!exists)db.insert(table,android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT,values)
            }
        }
    }
}
