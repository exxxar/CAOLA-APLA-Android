package ru.caola.apla.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private val lines = mutableListOf<String>()
    private val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS", Locale("ru"))
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("caola_debug", Context.MODE_PRIVATE)
        synchronized(this) {
            lines.clear()
            lines.addAll(prefs?.getString("entries", "")?.lines()?.filter { it.isNotBlank() } ?: emptyList())
        }
        add("INFO", "Журнал отладки инициализирован")
    }
    @Synchronized fun add(level: String, message: String) {
        lines.add("[${formatter.format(Date())}] [$level] $message")
        while (lines.size > 1000) lines.removeAt(0)
        prefs?.edit()?.putString("entries", lines.joinToString("\n"))?.apply()
    }
    @Synchronized fun all(): String = lines.joinToString("\n")
    @Synchronized fun clear() { lines.clear(); prefs?.edit()?.remove("entries")?.apply() }
    fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            add("FATAL", "Поток ${thread.name}: ${error.stackTraceToString()}")
            previous?.uncaughtException(thread, error)
        }
    }
}
