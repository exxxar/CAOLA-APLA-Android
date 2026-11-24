package ru.caola.apla.ui

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.caola.apla.data.ApiRepository
import ru.caola.apla.data.AppMode
import ru.caola.apla.data.DemoRepository
import ru.caola.apla.data.DemoState
import ru.caola.apla.data.DebugLog
import com.google.gson.JsonObject
import com.google.gson.JsonElement

class DashboardActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (session.mode == AppMode.PRODUCTION) {
            renderServerDashboard()
            return
        }
        val demo=DemoRepository(this); val profile=demo.section("profile"); val dash=demo.section("dashboard"); val state=DemoState(this)
        title("Привет, ${profile.optString("name").substringBefore(" ")}!", "Продолжим учиться небольшими шагами")
        card { cardText(this,"ВАША ЦЕЛЬ",10f,mint,true); cardText(this,profile.optString("goal"),17f,white,true); cardText(this,"${profile.optString("target_language")}  ·  ${profile.optString("level")}  ·  ${profile.optInt("daily_minutes")} минут в день",12f,muted) }
        val metrics=card { orientation=LinearLayout.HORIZONTAL }
        listOf(Pair("${dash.optInt("streak")} дн.","серия"),Pair("${dash.optInt("weekly_minutes")} мин","за неделю"),Pair("${dash.optInt("mastered")}","освоено")).forEach { (v,l) -> val col=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f) }; metric(col,v,l); metrics.addView(col) }
        section("Рекомендация на сейчас")
        card { cardText(this,"✦  CAOLA ПРЕДЛАГАЕТ",10f,mint,true); cardText(this,dash.optString("lesson_title"),20f,white,true); cardText(this,"${dash.optInt("lesson_minutes")} минут  ·  ${dash.optInt("due_reviews")} элемента на повторение",12f,muted); cardText(this,dash.optString("recommendation_reason"),13f) }
        primary("Начать занятие  →") { startActivity(Intent(this,LessonActivity::class.java)) }
        section("Ваш учебный маршрут")
        card { cardText(this,"▣  Микроуроки",16f,white,true); cardText(this,"Короткие практические занятия на каждый день",12f,muted); cardText(this,"Открыть каталог  →",12f,mint,true); setOnClickListener { startActivity(Intent(this@DashboardActivity,LessonCatalogActivity::class.java)) } }
        card { cardText(this,"◷  Мой прогресс",16f,white,true); cardText(this,"Динамика, точность ответов и освоенные темы",12f,muted); cardText(this,"Посмотреть  →",12f,mint,true); setOnClickListener { startActivity(Intent(this@DashboardActivity,ProgressActivity::class.java)) } }
        section("Практика"); action("Аудирование",ListeningActivity::class.java); action("Диалоговая практика",DialogueActivity::class.java); action("Словарь и материалы",DictionaryActivity::class.java)
        section("Система"); action("Настройки и контекст",SettingsActivity::class.java); action("Отладка приложения",DebugActivity::class.java)
        text("Завершено занятий в этой демо-сессии: ${state.completedLessons}",11f)
    }

    private fun renderServerDashboard() {
        title("Личный кабинет", "Данные загружаются из серверного API")
        val status = TextView(this).apply { text = "Подключаемся к серверу…"; textSize = 13f; setTextColor(muted); setPadding(0, dp(8), 0, dp(12)) }
        root.addView(status)
        primary("Повторить загрузку") { loadServerDashboard(status) }
        action("Настройки подключения", SettingsActivity::class.java)
        action("Отладка и журнал запросов", DebugActivity::class.java)
        loadServerDashboard(status)
    }

    private fun loadServerDashboard(status: TextView) {
        status.text = "Загрузка GET /learning/dashboard…"
        lifecycleScope.launch {
            try {
                val response = ApiRepository(this@DashboardActivity).dashboard()
                if (!response.isSuccessful) {
                    status.text = when (response.code()) {
                        401, 403 -> "Сервер доступен, но сессия не авторизована. Проверьте access token в настройках. HTTP ${response.code()}"
                        404 -> "Маршрут /learning/dashboard не найден. Проверьте версию backend API."
                        else -> "Ошибка сервера: HTTP ${response.code()} ${response.message()}"
                    }
                    DebugLog.add("WARN", "Dashboard API ответил HTTP ${response.code()}")
                    return@launch
                }
                val data = response.body() ?: throw IllegalStateException("Пустой ответ dashboard")
                // API may return null for optional sections until the user has data.
                // Never call getAsJsonObject() directly on a nullable JsonElement:
                // Gson throws ClassCastException when the value is JsonNull.
                val profile = data.objectOrNull("profile")
                val metrics = data.objectOrNull("metrics")
                val recommendation = data.objectOrNull("recommendation")
                root.removeViews(0, root.childCount)
                title("Личный кабинет", "Данные получены от сервера")
                card {
                    cardText(this, "ВАША ЦЕЛЬ", 10f, mint, true)
                    cardText(this, profile.stringOr("goal", "Учебная цель не задана"), 17f, white, true)
                    cardText(this, "${profile.stringOr("target_language", "Язык не выбран")} · ${profile.stringOr("level", "Уровень не указан")}", 12f, muted)
                }
                val metricCard = card { orientation = LinearLayout.HORIZONTAL }
                listOf(
                    Pair("${metrics.intOr("streak_days")} дн.", "серия"),
                    Pair("${metrics.intOr("weekly_minutes")} мин", "за неделю"),
                    Pair("${metrics.intOr("mastered_items")}", "освоено")
                ).forEach { (value, label) ->
                    val col = LinearLayout(this@DashboardActivity).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
                    metric(col, value, label); metricCard.addView(col)
                }
                section("Рекомендация CAOLA")
                card {
                    cardText(this, recommendation.stringOr("title", "Нет активной рекомендации"), 19f, white, true)
                    cardText(this, "${recommendation.intOr("duration_minutes")} минут · ${recommendation.intOr("due_reviews")} элементов на повторение", 12f, muted)
                    cardText(this, recommendation.stringOr("reason", "Сервер пока не сформировал рекомендацию"), 13f)
                }
                section("Разделы")
                action("Каталог обучения", LessonCatalogActivity::class.java)
                action("Прогресс", ProgressActivity::class.java)
                action("CAOLA", CaolaActivity::class.java)
                action("APLA", AplaActivity::class.java)
                action("Настройки и контекст", SettingsActivity::class.java)
                action("Отладка", DebugActivity::class.java)
                DebugLog.add("INFO", "Dashboard загружен с сервера")
            } catch (e: Exception) {
                status.text = "Не удалось загрузить данные. ${e.localizedMessage ?: e.javaClass.simpleName}"
                DebugLog.add("ERROR", "Dashboard API: ${e.stackTraceToString()}")
            }
        }
    }

    private fun JsonObject?.stringOr(key: String, fallback: String): String {
        val value: JsonElement = this?.get(key) ?: return fallback
        if (value.isJsonNull || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) return fallback
        return runCatching { value.asString }.getOrDefault(fallback).ifBlank { fallback }
    }

    private fun JsonObject?.intOr(key: String, fallback: Int = 0): Int {
        val value: JsonElement = this?.get(key) ?: return fallback
        if (value.isJsonNull || !value.isJsonPrimitive) return fallback
        return runCatching { value.asInt }.getOrDefault(fallback)
    }

    private fun JsonObject.objectOrNull(key: String): JsonObject? {
        val value = get(key) ?: return null
        return if (value.isJsonObject) value.asJsonObject else null
    }
}
