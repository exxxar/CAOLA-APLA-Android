package ru.caola.apla.ui

import android.content.Intent
import android.os.Bundle
import ru.caola.apla.data.AppMode
import ru.caola.apla.data.DebugLog

class ModeSelectionActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title("CAOLA / APLA")
        text("Персонализированное изучение иностранных языков", 16f)
        text("Выберите режим работы приложения.")
        primary("Демо-режим — полный функционал на тестовых данных") {
            session.mode = AppMode.DEMO
            DebugLog.add("INFO", "Выбран демо-режим")
            startActivity(Intent(this, DashboardActivity::class.java)); finish()
        }
        primary("Основной режим — подключение к серверу") {
            session.mode = AppMode.PRODUCTION
            DebugLog.add("INFO", "Выбран основной режим")
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        action("Настройки", SettingsActivity::class.java)
        action("Отладка и журнал ошибок", DebugActivity::class.java)
    }
}
