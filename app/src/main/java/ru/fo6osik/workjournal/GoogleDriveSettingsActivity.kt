package ru.fo6osik.workjournal

import android.accounts.AccountManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.AccountPicker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import ru.fo6osik.workjournal.documents.DocumentCatalog
import ru.fo6osik.workjournal.documents.DocumentDescriptor
import ru.fo6osik.workjournal.documents.sources.*
import java.io.FileNotFoundException

/** Explicit opt-in entry point; no existing document screen starts authorization. */
class GoogleDriveSettingsActivity : AppCompatActivity() {
    private val model: DriveSettingsModel by viewModels()
    private lateinit var status: TextView
    private lateinit var documents: Spinner
    private val controls = mutableListOf<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        setContentView(ScrollView(this).apply { addView(content) })
        content.addView(TextView(this).apply { text = "Google Drive"; textSize = 24f })
        content.addView(TextView(this).apply {
            text = "Выберите PDF и индекс для документа из каталога. Поддерживаются только файлы с известными контрольными суммами."
        })
        status = TextView(this).also { content.addView(it) }
        documents = Spinner(this).also { content.addView(it) }
        documents.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { model.select(position) }
        }
        fun button(label: String, action: () -> Unit) {
            controls += Button(this).apply { text = label; setOnClickListener { action() }; content.addView(this) }
        }
        button("Подключить / восстановить доступ") {
            val requestCode = model.beginAccountChoice()
            if (requestCode != null) {
                @Suppress("DEPRECATION")
                val chooser = AccountPicker.newChooseAccountIntent(null, null, arrayOf("com.google"), false, null, null, null, null)
                try { startActivityForResult(chooser, requestCode) } catch (_: Exception) { model.cancelAuthorization() }
            }
        }
        button("Отключить и отозвать доступ") { model.disconnect() }
        button("1. Выбрать PDF") { model.pick(true) }
        button("2. Выбрать индекс и скачать") { model.pick(false) }
        button("Проверить обновления") { model.checkUpdates() }
        button("Синхронизировать вручную") { model.sync() }
        button("Открыть скачанный документ офлайн") {
            model.selectedDocument()?.let { document ->
                startActivity(Intent(this, PdfViewerActivity::class.java)
                    .putExtra("localDocumentId", document.documentId)
                    .putExtra("assetPath", document.assetsSource?.pdfPath)
                    .putExtra("documentTitle", document.title))
            }
        }
        button("Сбросить повреждённые настройки") { model.reset() }
        content.addView(Button(this).apply {
            text = "Отменить текущую операцию"
            setOnClickListener { model.cancelWork() }
        })
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ui.collect { state ->
                    status.text = state.message + model.lastUpdate()
                    controls.forEach { it.isEnabled = !state.busy }
                    documents.isEnabled = !state.busy
                    if (documents.adapter == null && model.catalog.isNotEmpty()) {
                        documents.adapter = ArrayAdapter(this@GoogleDriveSettingsActivity,
                            android.R.layout.simple_spinner_dropdown_item, model.catalog.map { it.title })
                        documents.setSelection(model.selected)
                    }
                    model.takeResolution()?.let { pending ->
                        try { startIntentSenderForResult(pending.first.intentSender, pending.second, null, 0, 0, 0) }
                        catch (_: Exception) { model.cancelAuthorization() }
                    }
                }
            }
        }
    }
    @Deprecated("Activity result is routed to a retained one-use authorization attempt")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode in 6100..65000) model.result(requestCode, if (resultCode == RESULT_OK) data else null)
    }

}

internal data class DriveSettingsUi(val message: String = "Загрузка настроек…", val busy: Boolean = true)
internal class DriveSettingsModel(application: Application) : AndroidViewModel(application) {
    val ui = MutableStateFlow(DriveSettingsUi())
    val services = GoogleDriveServices.get(application)
    var catalog = emptyList<DocumentDescriptor>(); private set
    var selected = 0; private set
    private data class Purpose(val account: String?, val documentId: String?, val pdf: Boolean?, val chooser: Boolean = false)
    private val attempts = DriveAuthorizationAttempt<Purpose>(SystemClock::elapsedRealtime)
    private var resolution: Pair<PendingIntent, Int>? = null
    private var selectedPdf: Pair<String, String>? = null
    private var job: Job? = null
    init { work { services.initialize(); catalog = withContext(Dispatchers.IO) { DocumentCatalog.load(application.assets).documents }; summary() } }
    fun selectedDocument() = catalog.getOrNull(selected)
    fun select(position: Int) {
        if (selected != position) {
            selectedPdf = null; selected = position
            if (!ui.value.busy) ui.value = DriveSettingsUi(summary() + "\n" + (selectedDocument()?.title ?: ""), false)
        }
    }
    fun lastUpdate(): String {
        val timestamp = services.state.bindings.singleOrNull { it.documentId == selectedDocument()?.documentId }?.lastUpdated
            ?: return "\nВыбранный документ ещё не синхронизирован с Drive."
        return "\nПоследняя успешная синхронизация: " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(timestamp))
    }
    private fun summary(): String {
        val state = services.state
        return when {
            state.pendingRevoke != null -> "Локальный доступ отключён. Отзыв доступа не завершён: повторите отключение при наличии сети или отзовите доступ в аккаунте Google."
            state.account == null -> "Google Drive не подключён. Скачанные документы доступны офлайн."
            else -> "Google Drive подключён. Проверенных привязок: ${state.bindings.size}. Синхронизация запускается вручную."
        }
    }
    private fun work(block: suspend () -> String) {
        if (job?.isActive == true || attempts.isPending()) return
        job = viewModelScope.launch {
            ui.value = DriveSettingsUi("Выполняется…", true)
            try { ui.value = DriveSettingsUi(block(), attempts.isPending()) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                attempts.cancel(); resolution = null
                val message = when (error) {
                    is DriveAuthorizationRequired -> "Требуется восстановить доступ к Google Drive."
                    is DriveAccessDenied -> "Нет доступа к выбранному файлу. Выберите его заново."
                    is FileNotFoundException -> "Выбранный файл отсутствует или удалён."
                    is DriveRemoteConflict -> "Файл изменился во время загрузки. Повторите синхронизацию."
                    is IllegalArgumentException -> "Файл, индекс или разрешения не соответствуют каталогу. Предыдущая копия сохранена."
                    else -> "Операция не выполнена. Проверьте сеть, свободное место и настройки Google Drive. Предыдущая копия сохранена."
                }
                ui.value = DriveSettingsUi(message, false)
            }
        }
    }
    fun beginAccountChoice(): Int? {
        if (ui.value.busy || job?.isActive == true || services.state.pendingRevoke != null) return null
        val code = attempts.begin(Purpose(null, null, null, true))
        ui.value = DriveSettingsUi("Выберите аккаунт Google…", true)
        return code
    }
    private fun accountChosen(account: String?) {
        if (account.isNullOrBlank() || account.length > 320 || account.any { it.code < 32 }) { cancelAuthorization(); return }
        work {
            val old = services.state.account
            if (old != null && old != account && !services.disconnect()) return@work summary()
            authorize(Purpose(account, null, null))
        }
    }
    fun cancelWork() {
        job?.cancel(); attempts.cancel(); resolution = null; selectedPdf = null
        ui.value = DriveSettingsUi("Операция отменена. Проверенная локальная копия сохранена.", false)
    }
    fun cancelAuthorization() { attempts.cancel(); resolution = null; selectedPdf = null; ui.value = DriveSettingsUi("Выбор отменён. " + summary(), false) }
    fun takeResolution(): Pair<PendingIntent, Int>? = resolution.also { resolution = null }
    fun pick(pdf: Boolean) {
        val document = selectedDocument() ?: return
        val account = services.state.account ?: run { ui.value = DriveSettingsUi("Сначала подключите Google Drive.", false); return }
        if (!pdf && selectedPdf?.first != document.documentId) { ui.value = DriveSettingsUi("Сначала выберите PDF этого документа.", false); return }
        work { authorize(Purpose(account, document.documentId, pdf)) }
    }
    private suspend fun authorize(purpose: Purpose): String {
        val mime = when (purpose.pdf) { true -> "application/pdf"; false -> "application/json,text/json,text/plain,application/octet-stream"; null -> null }
        val result = withContext(Dispatchers.IO) { services.authorization.request(purpose.account!!, mime) }
        if (result.hasResolution()) {
            val code = attempts.begin(purpose)
            resolution = (result.pendingIntent ?: run { attempts.cancel(); throw DriveAuthorizationRequired() }) to code
            return "Подтвердите доступ и выбор файла в Google Drive…"
        }
        return accept(purpose, services.authorization.grant(result))
    }
    fun result(requestCode: Int, data: Intent?) {
        val purpose = runCatching { attempts.consume(requestCode) }.getOrNull()
        if (purpose == null) { if (!attempts.isPending()) cancelAuthorization(); return }
        if (data == null) { cancelAuthorization(); return }
        if (purpose.chooser) {
            accountChosen(if (data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE) == "com.google")
                data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) else null)
            return
        }
        work {
            val grant = withContext(Dispatchers.IO) { services.authorization.grant(services.authorization.callback(data)) }
            accept(purpose, grant)
        }
    }
    private suspend fun accept(purpose: Purpose, grant: DriveGrant): String {
        val account = purpose.account ?: throw DriveAuthorizationRequired()
        if (purpose.documentId != null && (services.state.account != account || selectedDocument()?.documentId != purpose.documentId)) throw DriveAuthorizationRequired()
        services.connect(account, grant)
        when (purpose.pdf) {
            true -> { selectedPdf = purpose.documentId!! to DriveSelection.pickedFile(grant.pickedIds); return "PDF выбран. Теперь выберите соответствующий поисковый индекс." }
            false -> {
                val pdf = selectedPdf?.takeIf { it.first == purpose.documentId }?.second ?: throw DriveAuthorizationRequired()
                val binding = DriveBinding(purpose.documentId!!, pdf, DriveSelection.pickedFile(grant.pickedIds))
                services.downloadSelection(binding, account)
                selectedPdf = null
                return "PDF и индекс проверены и сохранены. Документ доступен офлайн."
            }
            null -> return summary()
        }
    }
    fun disconnect() = work { selectedPdf = null; services.disconnect(); summary() }
    fun reset() = work {
        selectedPdf = null
        // Valid settings use normal revocation; reset is reserved for lost/unreadable Keystore state.
        if (services.state.account != null || services.state.pendingRevoke != null) {
            services.disconnect(); summary()
        } else {
            services.resetInvalidSettings()
            catalog = withContext(Dispatchers.IO) { DocumentCatalog.load(getApplication<Application>().assets).documents }
            "Настройки сброшены. Если доступ был выдан ранее, отзовите его в аккаунте Google. Локальные документы сохранены."
        }
    }
    fun checkUpdates() = work {
        if (services.checkUpdates(selectedDocument()!!.documentId)) "Версии файлов изменились. Синхронизируйте вручную; новые данные должны соответствовать контрольным суммам каталога."
        else "Версии файлов не изменились. Это не заменяет проверку содержимого при скачивании."
    }
    fun sync() = work { services.synchronize(selectedDocument()!!.documentId); "Синхронизация завершена. PDF и индекс проверены." }
}
