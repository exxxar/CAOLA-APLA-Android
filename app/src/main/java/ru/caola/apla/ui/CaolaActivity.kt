package ru.caola.apla.ui

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.caola.apla.data.ApiRepository
import ru.caola.apla.data.AppMode
import ru.caola.apla.data.DemoRepository
import ru.caola.apla.data.DebugLog
import java.time.Instant
import java.time.temporal.ChronoUnit

class CaolaActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (session.mode == AppMode.PRODUCTION) { renderServerRecommendation(); return }
        title("CAOLA","Контекстно-адаптивное планирование обучения")
        card { cardText(this,"КАК ЭТО РАБОТАЕТ",10f,mint,true); cardText(this,"CAOLA ищет подходящее время, длительность и формат занятия с учётом разрешённого контекста.",14f); cardText(this,"Важно: рекомендация не запускает урок автоматически. Решение всегда остаётся за пользователем.",12f,muted) }
        val windows=DemoRepository(this).section("caola").optJSONArray("candidate_windows") ?: return
        section("Подходящие временные окна")
        for(i in 0 until windows.length()) { val w=windows.getJSONObject(i); val score=(w.optDouble("score")*100).toInt()
            card {
                cardText(this,w.optString("time"),25f,white,true)
                cardText(this,"${w.optString("duration")}  ·  ${w.optString("format")}",13f,mint,true)
                progress(this,"Оценка уместности",score)
                cardText(this,w.optString("reason"),13f)
                cardText(this,"Риск прерывания: ${w.optString("risk")}",11f,muted)
                val controls=LinearLayout(this@CaolaActivity).apply { orientation=LinearLayout.HORIZONTAL }
                fun small(label:String, color:Int, action:()->Unit) { controls.addView(Button(this@CaolaActivity).apply { text=label; isAllCaps=false; textSize=11f; setTextColor(color); background=rounded(panel2,12); setOnClickListener { action() } },LinearLayout.LayoutParams(0,dp(42),1f).apply { marginEnd=dp(5) }) }
                small("Принять",mint) { DebugLog.add("INFO","CAOLA: пользователь принял окно ${w.optString("time")}"); Toast.makeText(this@CaolaActivity,"Рекомендация принята. Можно начать занятие.",Toast.LENGTH_SHORT).show() }
                small("Позже",blue) { DebugLog.add("INFO","CAOLA: рекомендация отложена ${w.optString("time")}"); Toast.makeText(this@CaolaActivity,"Напоминание отложено в демосценарии",Toast.LENGTH_SHORT).show() }
                small("Нет",muted) { DebugLog.add("INFO","CAOLA: рекомендация отклонена ${w.optString("time")}"); Toast.makeText(this@CaolaActivity,"Рекомендация отклонена",Toast.LENGTH_SHORT).show() }
                addView(controls)
            }
        }
        section("Параметры контекста")
        action("Посмотреть разрешения и расписание",ContextActivity::class.java)
    }

    private fun renderServerRecommendation() {
        title("CAOLA", "Рекомендации контекстного алгоритма с сервера")
        val status = TextView(this).apply { text = "Загрузка GET /learning/recommendation…"; setTextColor(muted); textSize = 13f }
        root.addView(status)
        lifecycleScope.launch {
            try {
                val response = ApiRepository(this@CaolaActivity).recommendation()
                if (!response.isSuccessful) { status.text = "Не удалось получить рекомендацию: HTTP ${response.code()}"; return@launch }
                val data = response.body() ?: throw IllegalStateException("Пустой ответ recommendation")
                val rec = if (data.has("recommendation") && data.get("recommendation").isJsonObject) data.getAsJsonObject("recommendation") else data
                root.removeAllViews(); title("CAOLA", "Рекомендация получена от сервера")
                card { cardText(this, "КОНТЕКСТНОЕ ПРЕДЛОЖЕНИЕ", 10f, mint, true); cardText(this, rec.get("title")?.asString ?: "Учебное окно", 20f, white, true); cardText(this, "${rec.get("window_start")?.asString ?: "Время уточняется"} · ${rec.get("duration_minutes")?.asInt ?: 0} минут · ${rec.get("format")?.asString ?: "Формат не указан"}", 12f, muted); progress(this, "Оценка уместности", ((rec.get("score")?.asDouble ?: 0.0) * 100).toInt()); cardText(this, rec.get("reason")?.asString ?: "Сервер не вернул пояснение", 13f) }
                text("Рекомендация не запускает урок автоматически. Выберите действие.", 12f)
                val id = rec.get("id")?.asString
                if (id.isNullOrBlank()) { text("В ответе отсутствует id рекомендации — действия недоступны.", 12f); return@launch }
                primary("Принять рекомендацию") { sendDecision(id, "accept", status) }
                secondary("Отложить на 30 минут") { sendDecision(id, "postpone", status, Instant.now().plus(30, ChronoUnit.MINUTES).toString()) }
                secondary("Отклонить") { sendDecision(id, "reject", status) }
                action("Настройки контекста", ContextActivity::class.java)
            } catch (e: Exception) { status.text = "Ошибка загрузки: ${e.localizedMessage ?: e.javaClass.simpleName}"; DebugLog.add("ERROR", "CAOLA API: ${e.stackTraceToString()}") }
        }
    }

    private fun sendDecision(id: String, decision: String, status: TextView, postponeUntil: String? = null) {
        lifecycleScope.launch {
            try {
                val response = ApiRepository(this@CaolaActivity).decideRecommendation(id, decision, postponeUntil)
                val message = if (response.isSuccessful) "Решение '$decision' сохранено на сервере" else "Ошибка сохранения решения: HTTP ${response.code()}"
                status.text = message
                Toast.makeText(this@CaolaActivity, message, Toast.LENGTH_LONG).show()
                DebugLog.add(if (response.isSuccessful) "INFO" else "WARN", "CAOLA decision=$decision HTTP ${response.code()}")
            } catch (e: Exception) { status.text = "Ошибка: ${e.localizedMessage}"; DebugLog.add("ERROR", e.stackTraceToString()) }
        }
    }
}
