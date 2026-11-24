package ru.caola.apla.ui

import android.os.Bundle
import android.content.Intent
import android.widget.TextView
import ru.caola.apla.data.DebugLog

class DebugActivity : BaseActivity() {
    private lateinit var output: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title("Отладка приложения")
        text("Журнал событий, предупреждений, сетевых ошибок и исключений. Хранится между запусками.")
        output = TextView(this).apply {
            setTextColor(white); textSize = 11f; setPadding(10, 10, 10, 10)
            setBackgroundColor(0xFF0B1422.toInt())
            text = DebugLog.all().ifBlank { "Журнал пока пуст." }
        }
        root.addView(output)
        primary("Обновить журнал") { output.text = DebugLog.all().ifBlank { "Журнал пока пуст." } }
        primary("Очистить журнал") { DebugLog.clear(); output.text = "Журнал очищен." }
        primary("Скопировать журнал") {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("CAOLA debug log", DebugLog.all()))
        }
        primary("Поделиться журналом") {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Журнал отладки CAOLA / APLA")
                putExtra(Intent.EXTRA_TEXT, DebugLog.all())
            }
            startActivity(Intent.createChooser(intent, "Экспорт журнала"))
        }
    }
}
