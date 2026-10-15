package ru.fo6osik.workjournal

import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/** All database access stays off the UI thread. Fuel is never a timer event. */
class HarvestFuelDialog(private val host:AppCompatActivity,private val day:HarvestDay,private val changed:()->Unit) {
    private val db get()=DatabaseProvider.getDatabase(host.applicationContext)
    private lateinit var start:EditText
    private lateinit var end:EditText
    private lateinit var list:LinearLayout
    private lateinit var result:TextView
    private var rows=emptyList<HarvestRefueling>()
    private var loading=true
    private fun column()=LinearLayout(host).apply { orientation=LinearLayout.VERTICAL;setPadding(24,8,24,8) }
    private fun input(label:String,value:Double?=null)=EditText(host).apply {
        hint=label;contentDescription=label;inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        setText(value?.let(HarvestResultsFormatter::number) ?: "")
    }
    private fun work(block:()->Unit) { Thread { try { block() } catch(e:Exception) { host.runOnUiThread { Toast.makeText(host,e.message ?: "Не удалось сохранить",Toast.LENGTH_LONG).show() } } }.start() }
    fun show() {
        val body=column();start=input("Топливо на начало дня, %");end=input("Топливо на конец дня, %")
        body.addView(start);body.addView(end);result=TextView(host);body.addView(result)
        body.addView(TextView(host).apply { text="Оценка по датчику, % вместимости бака" })
        val save=Button(host).apply { text="Сохранить показания" };body.addView(save)
        list=column();body.addView(list)
        val add=Button(host).apply { text="Добавить заправку";setOnClickListener { edit(null) } };body.addView(add)
        val dialog=AlertDialog.Builder(host).setTitle("Топливо за день").setView(ScrollView(host).apply { addView(body) })
            .setPositiveButton("Закрыть",null).create()
        save.setOnClickListener {
            if(loading)return@setOnClickListener
            try {
                val a=HarvestFuelCalculator.parse(start.text.toString());val b=HarvestFuelCalculator.parse(end.text.toString())
                work { db.harvestFuelDao().saveLevels(day.id,a,b);host.runOnUiThread { changed();render() } }
            } catch(e:IllegalArgumentException) { result.text=e.message }
        }
        dialog.show()
        work {
            val fuel=db.harvestFuelDao().getDay(day.id) ?: db.harvestFuelDao().initial(day.id)
            val refills=db.harvestFuelDao().getRefills(day.id)
            val previous=fuel?.vehicleId?.let { db.harvestFuelDao().previous(day.id,it) }
            host.runOnUiThread {
                start.setText(fuel?.startPercent?.let(HarvestResultsFormatter::number) ?: "")
                end.setText(fuel?.endPercent?.let(HarvestResultsFormatter::number) ?: "")
                body.addView(TextView(host).apply { text="Техника: ${fuel?.vehicleNameSnapshot ?: "—"}" },0)
                if(previous?.endPercent!=null) body.addView(TextView(host).apply {
                    text="Предыдущий остаток: ${HarvestResultsFormatter.number(previous.endPercent)}%. Рост уровня между сменами можно записать как заправку перед этим днём."
                },1)
                rows=refills;loading=false;render()
            }
        }
    }
    private fun render() {
        list.removeAllViews()
        list.addView(TextView(host).apply { text="Заправки" })
        for(row in rows) {
            val line=column();line.addView(TextView(host).apply {
                text="${if(row.scope=="BEFORE_DAY") "Между сменами, перед этим днём" else "За день"}: ${HarvestResultsFormatter.number(row.beforePercent)}% → ${HarvestResultsFormatter.number(row.afterPercent)}% · ${row.time ?: "время —"}"
            })
            line.addView(Button(host).apply { text="Изменить";setOnClickListener { edit(row) } })
            line.addView(Button(host).apply { text="Удалить";setOnClickListener {
                AlertDialog.Builder(host).setMessage("Удалить эту заправку?").setNegativeButton("Отмена",null).setPositiveButton("Удалить") { _,_->
                    work { db.harvestFuelDao().deleteRefill(row.id);reload() }
                }.show()
            } });list.addView(line)
        }
        try {
            val fuel=HarvestFuelCalculator.calculate(HarvestFuelCalculator.parse(start.text.toString()),HarvestFuelCalculator.parse(end.text.toString()),
                rows.map { HarvestFuelCalculator.Refill(it.beforePercent,it.afterPercent,it.scope=="BEFORE_DAY") },day.harvestedAreaHa)
            result.text="Расход за сутки: ${fuel.consumed?.let { "≈ ${HarvestResultsFormatter.number(it)}% бака" } ?: "—"}\nРасход на гектар: ${fuel.perHa?.let { "≈ ${HarvestResultsFormatter.number(it)}% бака/га" } ?: "—"}"+
                (fuel.warning?.let { "\n$it" } ?: "")
        }catch(e:IllegalArgumentException){result.text=e.message}
    }
    private fun reload() { val values=db.harvestFuelDao().getRefills(day.id);host.runOnUiThread { rows=values;render();changed() } }
    private fun edit(row:HarvestRefueling?) {
        val form=column();val before=input("До заправки, %",row?.beforePercent);val after=input("После заправки, %",row?.afterPercent)
        val time=EditText(host).apply { hint="Время, ЧЧ:ММ (необязательно)";setText(row?.time ?: "") }
        val scope=CheckBox(host).apply { text="Между сменами, перед этим рабочим днём";isChecked=row?.scope=="BEFORE_DAY" }
        form.addView(before);form.addView(after);form.addView(time);form.addView(scope)
        val dialog=AlertDialog.Builder(host).setTitle(if(row==null)"Добавить заправку" else "Изменить заправку")
            .setView(form).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            try {
                val a=requireNotNull(HarvestFuelCalculator.parse(before.text.toString())) { "Укажите оба показания" }
                val b=requireNotNull(HarvestFuelCalculator.parse(after.text.toString())) { "Укажите оба показания" }
                require(b>=a) { "После заправки уровень не ниже, чем до" }
                val t=time.text.toString().trim().takeIf { it.isNotEmpty() };if(t!=null)HarvestChronology.minutes(t)
                val period=if(scope.isChecked)"BEFORE_DAY" else "DAY"
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=false
                work { db.harvestFuelDao().saveRefill(day.id,row?.id,a,b,t,period);reload();host.runOnUiThread { dialog.dismiss() } }
            }catch(e:IllegalArgumentException){after.error=e.message}
        } };dialog.show()
    }
}
