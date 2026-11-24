package ru.caola.apla.ui

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ru.caola.apla.data.ApiRepository
import ru.caola.apla.data.AppMode
import ru.caola.apla.data.DebugLog
import java.time.ZoneId

class OnboardingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title("Настройка обучения", "Заполните профиль, чтобы CAOLA и APLA могли персонализировать занятия")
        text("Эти параметры сохраняются на сервере и используются при построении учебной траектории.", 13f)
        val targetLanguage = field("Изучаемый язык", "English")
        val nativeLanguage = field("Родной язык", "Русский")
        val currentLevel = field("Текущий уровень (A1–C2)", "A1")
        val targetLevel = field("Желаемый уровень (A1–C2)", "B1")
        val goal = field("Цель обучения", "comprehensive")
        val dailyMinutes = field("Минут в день", "15")
        val formats = field("Форматы через запятую", "text,audio")
        val status = TextView(this).apply { textSize = 12f; setTextColor(muted) }
        root.addView(status)
        primary("Сохранить и продолжить") {
            val minutes = dailyMinutes.text.toString().toIntOrNull()
            if (targetLanguage.text.isNullOrBlank() || nativeLanguage.text.isNullOrBlank() || minutes == null || minutes !in 5..240) {
                status.text = "Укажите языки и корректную длительность от 5 до 240 минут."
                return@primary
            }
            if (session.mode == AppMode.DEMO) {
                DebugLog.add("INFO", "Параметры обучения сохранены локально (демо)")
                startActivity(Intent(this, DashboardActivity::class.java)); finish(); return@primary
            }
            status.text = "Сохраняем настройки на сервере…"
            lifecycleScope.launch {
                try {
                    val body = JsonObject().apply {
                        addProperty("native_language", nativeLanguage.text.toString().trim())
                        addProperty("target_language", targetLanguage.text.toString().trim())
                        addProperty("timezone", ZoneId.systemDefault().id)
                        addProperty("current_level", currentLevel.text.toString().trim().uppercase())
                        addProperty("target_level", targetLevel.text.toString().trim().uppercase())
                        add("goals", JsonArray().apply { add(goal.text.toString().trim().ifBlank { "comprehensive" }) })
                        addProperty("daily_minutes", minutes)
                        add("preferred_formats", JsonArray().apply {
                            formats.text.toString().split(",").map { it.trim().lowercase() }.filter { it in setOf("text", "audio", "visual", "dialogue") }.distinct().forEach { add(it) }
                        })
                        add("quiet_hours", JsonObject())
                        add("consents", JsonArray())
                    }
                    val response = ApiRepository(this@OnboardingActivity).completeOnboarding(body)
                    if (response.isSuccessful) {
                        DebugLog.add("INFO", "Серверная настройка обучения завершена")
                        Toast.makeText(this@OnboardingActivity, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@OnboardingActivity, DashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    } else {
                        status.text = "Сервер отклонил настройки: HTTP ${response.code()}"
                        DebugLog.add("WARN", "Onboarding API HTTP ${response.code()}")
                    }
                } catch (e: Exception) {
                    status.text = "Не удалось сохранить: ${e.localizedMessage ?: e.javaClass.simpleName}"
                    DebugLog.add("ERROR", "Onboarding API: ${e.stackTraceToString()}")
                }
            }
        }
        secondary("Позже") { startActivity(Intent(this, DashboardActivity::class.java)); finish() }
    }
}
