package ru.fo6osik.workjournal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Nova340BulletinsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nova340_bulletins)

        setupBulletinButton(R.id.buttonBulletin19, "S300.19 БЭ — Замена колес", "manuals/bulletins/s300_19_be.pdf")
        setupBulletinButton(R.id.buttonBulletin20, "S300.20 БЭ — Смазка кардана выгрузного шнека. Антифриз", "manuals/bulletins/s300_20_be.pdf")
        setupBulletinButton(R.id.buttonBulletin21, "S300.21 БЭ — Аппликация РСМ-10Б.22.01.009", "manuals/bulletins/s300_21_be.pdf")
        setupBulletinButton(R.id.buttonBulletin24, "S300.24 БЭ — Тормозная система с МВК Lovol", "manuals/bulletins/s300_24_be.pdf")
        setupBulletinButton(R.id.buttonBulletin26, "S300.26 БЭ — Смазка обоймы наклонной камеры", "manuals/bulletins/s300_26_be.pdf")
        setupBulletinButton(R.id.buttonBulletin27, "S300.27 БЭ — Сертификация", "manuals/bulletins/s300_27_be.pdf")
        setupBulletinButton(R.id.buttonBulletin29, "S300.29 БЭ — Зарядка и обслуживание АКБ", "manuals/bulletins/s300_29_be.pdf")
        setupBulletinButton(R.id.buttonBulletin31, "S300.31 БЭ — Технические характеристики", "manuals/bulletins/s300_31_be.pdf")
        setupBulletinButton(R.id.buttonBulletin32, "S300.32 БЭ — Мост управляемых колес ведущий", "manuals/bulletins/s300_32_be.pdf")
        setupBulletinButton(R.id.buttonBulletin33, "S300.33 БЭ — Идентификационный номер комбайна на паспортной табличке", "manuals/bulletins/s300_33_be.pdf")
        setupBulletinButton(R.id.buttonBulletin34, "S300.34 БЭ — Система дистанционного мониторинга", "manuals/bulletins/s300_34_be.pdf")
        setupBulletinButton(R.id.buttonBulletin35, "S300.35 БЭ — Техническое обслуживание датчика системы оценки возврата на домолот", "manuals/bulletins/s300_35_be.pdf")
        setupBulletinButton(R.id.buttonBulletin36, "S300.36 БЭ — Заглушки. Гидрооборудование наклонной камеры", "manuals/bulletins/s300_36_be.pdf")
        setupBulletinButton(R.id.buttonBulletin37, "S300.37 БЭ — Крепление первичных средств пожаротушения", "manuals/bulletins/s300_37_be.pdf")
        setupBulletinButton(R.id.buttonBulletin38, "S300.38 БЭ — Очистка датчика сигнализатора засоренности", "manuals/bulletins/s300_38_be.pdf")
    }

    private fun setupBulletinButton(
        buttonId: Int,
        documentTitle: String,
        assetPath: String
    ) {
        findViewById<Button>(buttonId).setOnClickListener {
            val intent = Intent(this@Nova340BulletinsActivity, PdfViewerActivity::class.java)
            intent.putExtra("assetPath", assetPath)
            intent.putExtra("documentTitle", documentTitle)
            startActivity(intent)
        }
    }
}
