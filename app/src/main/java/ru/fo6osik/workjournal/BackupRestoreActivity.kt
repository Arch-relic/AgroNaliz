package ru.fo6osik.workjournal

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreActivity : AppCompatActivity() {

    private lateinit var buttonCreateBackup: Button
	private lateinit var buttonImportData: Button
    private lateinit var buttonRestoreBackup: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var textStatus: TextView

    companion object {
    private const val REQUEST_CREATE_BACKUP = 1001
    private const val REQUEST_RESTORE_BACKUP = 1002
    private const val REQUEST_IMPORT_DATA = 1003
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_backup_restore)

        buttonCreateBackup =
            findViewById(R.id.buttonCreateBackup)
			
			buttonImportData =
    findViewById(R.id.buttonImportData)

        buttonRestoreBackup =
            findViewById(R.id.buttonRestoreBackup)

        progressBar =
            findViewById(R.id.progressBarBackup)

        textStatus =
            findViewById(R.id.textBackupStatus)

        buttonCreateBackup.setOnClickListener {
            openCreateBackupDialog()
        }

        buttonRestoreBackup.setOnClickListener {
            openRestoreBackupDialog()
        }
		
		buttonImportData.setOnClickListener {
    openImportDataDialog()
}
    }

    private fun openCreateBackupDialog() {

        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {

                addCategory(Intent.CATEGORY_OPENABLE)

                type = "application/json"

                putExtra(
                    Intent.EXTRA_TITLE,
                    createDefaultBackupFileName()
                )
            }

        startActivityForResult(
            intent,
            REQUEST_CREATE_BACKUP
        )
    }
	
	private fun openImportDataDialog() {

    val intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {

            addCategory(Intent.CATEGORY_OPENABLE)

            type = "*/*"
        }

    startActivityForResult(
        intent,
        REQUEST_IMPORT_DATA
    )
}

    private fun openRestoreBackupDialog() {

        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {

                addCategory(Intent.CATEGORY_OPENABLE)

                /*
                 * Разрешаем выбрать файл независимо от того,
                 * каким MIME-типом Android определил JSON.
                 *
                 * Сам DatabaseBackupManager затем проверит
                 * содержимое файла.
                 */
                type = "*/*"
            }

        startActivityForResult(
            intent,
            REQUEST_RESTORE_BACKUP
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (resultCode != RESULT_OK) {
            return
        }

        val uri =
            data?.data
                ?: return

        when (requestCode) {

            REQUEST_CREATE_BACKUP -> {
                createBackup(uri)
            }

            REQUEST_RESTORE_BACKUP -> {
                confirmRestore(uri)
            }
			
			REQUEST_IMPORT_DATA -> {
            confirmImport(uri)
            }
        }
    }

    private fun createBackup(
        uri: Uri
    ) {

        setLoading(
            true,
            "Создание резервной копии..."
        )

        lifecycleScope.launch {

            val result =
                DatabaseBackupManager.exportBackup(
                    applicationContext,
                    uri
                )

            result
                .onSuccess {

                    setLoading(
                        false,
                        "Резервная копия успешно создана"
                    )

                    Toast
                        .makeText(
                            this@BackupRestoreActivity,
                            "Резервная копия создана",
                            Toast.LENGTH_LONG
                        )
                        .show()
                }
                .onFailure { error ->

                    setLoading(
                        false,
                        "Ошибка создания резервной копии"
                    )

                    showError(error)
                }
        }
    }

    private fun confirmRestore(
        uri: Uri
    ) {

        AlertDialog
            .Builder(this)
            .setTitle(
                "Восстановление данных"
            )
            .setMessage(
                """
                Текущие данные приложения будут заменены данными из резервной копии.

                Перед восстановлением рекомендуется создать резервную копию текущих данных.

                Продолжить?
                """.trimIndent()
            )
            .setPositiveButton(
                "Восстановить"
            ) { _, _ ->

                restoreBackup(uri)
            }
            .setNegativeButton(
                "Отмена",
                null
            )
            .show()
    }

    private fun restoreBackup(
        uri: Uri
    ) {

        setLoading(
            true,
            "Восстановление данных..."
        )

        lifecycleScope.launch {

            val result =
                DatabaseBackupManager.importBackup(
                    applicationContext,
                    uri
                )

            result
                .onSuccess {

                    setLoading(
                        false,
                        "Данные успешно восстановлены"
                    )

                    Toast
                        .makeText(
                            this@BackupRestoreActivity,
                            "Резервная копия восстановлена",
                            Toast.LENGTH_LONG
                        )
                        .show()

                    recreate()
                }
                .onFailure { error ->

                    setLoading(
                        false,
                        "Ошибка восстановления"
                    )

                    showError(error)
                }
        }
    }

    private fun setLoading(
        loading: Boolean,
        status: String
    ) {

        progressBar.visibility =
            if (loading) {
                View.VISIBLE
            } else {
                View.GONE
            }

        buttonCreateBackup.isEnabled =
            !loading
			
			buttonImportData.isEnabled = 
			!loading

        buttonRestoreBackup.isEnabled =
            !loading

        textStatus.text =
            status
    }

    private fun showError(
        error: Throwable
    ) {

        AlertDialog
            .Builder(this)
            .setTitle(
                "Ошибка"
            )
            .setMessage(
                error.message
                    ?: "Неизвестная ошибка"
            )
            .setPositiveButton(
                "ОК",
                null
            )
            .show()
    }

    private fun createDefaultBackupFileName(): String {

        val date =
            SimpleDateFormat(
                "yyyy-MM-dd_HH-mm",
                Locale.getDefault()
            ).format(Date())

        return "Agronaliz_backup_$date.json"
    }
	
	private fun confirmImport(
    uri: Uri
) {

    AlertDialog
        .Builder(this)
        .setTitle(
            "Импорт данных"
        )
        .setMessage(
            """
            Данные из выбранного файла будут добавлены к существующим данным.

            Текущие записи удаляться не будут.

            Совпадающие записи будут пропущены.

            Продолжить?
            """.trimIndent()
        )
        .setPositiveButton(
            "Импортировать"
        ) { _, _ ->

            importData(uri)
        }
        .setNegativeButton(
            "Отмена",
            null
        )
        .show()
  }

private fun importData(
    uri: Uri
) {

    setLoading(
        true,
        "Импорт данных..."
    )

    lifecycleScope.launch {

        val result =
            DataImportManager.importData(
                applicationContext,
                uri
            )

        result
            .onSuccess { summary ->

                setLoading(
                    false,
                    "Импорт завершён"
                )

                AlertDialog
                    .Builder(
                        this@BackupRestoreActivity
                    )
                    .setTitle(
                        "Импорт завершён"
                    )
                    .setMessage(
                        """
                        Добавлено записей: ${summary.added}

                        Пропущено совпадающих: ${summary.skipped}
                        """.trimIndent()
                    )
                    .setPositiveButton(
                        "ОК",
                        null
                    )
                    .show()
            }
            .onFailure { error ->

                setLoading(
                    false,
                    "Ошибка импорта"
                )

                showError(error)
            }
    }
  }
}