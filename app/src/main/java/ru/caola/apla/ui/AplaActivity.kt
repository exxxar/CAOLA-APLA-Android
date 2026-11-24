package ru.caola.apla.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.caola.apla.data.ApiRepository
import ru.caola.apla.data.AppMode
import ru.caola.apla.data.DemoRepository
import ru.caola.apla.data.DebugLog

class AplaActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (session.mode == AppMode.PRODUCTION) { renderServerQueue(); return }
        title("APLA","Персональная адаптация учебного материала")
        card { cardText(this,"МОДЕЛЬ ЗНАНИЙ",10f,mint,true); cardText(this,"APLA подбирает следующие задания по уровню освоения, вероятности воспроизведения и истории ошибок.",14f); cardText(this,"Приоритеты ниже — демонстрационные данные. Они показывают принцип адаптации, а не результаты реального обучения.",12f,muted) }
        val items=DemoRepository(this).section("apla").optJSONArray("priority_items") ?: return
        section("Что стоит повторить")
        for(i in 0 until items.length()) { val item=items.getJSONObject(i)
            card {
                cardText(this,"${i+1}.  ${item.optString("name")}",17f,white,true)
                progress(this,"Приоритет повторения",(item.optDouble("priority")*100).toInt(),blue)
                progress(this,"Текущий уровень освоения",(item.optDouble("level")*100).toInt(),mint)
                cardText(this,item.optString("reason"),12f,muted)
                cardText(this,"Рекомендуемый срок: ${item.optString("due")}",11f,mint,true)
            }
        }
        section("Проверка адаптации")
        card { cardText(this,"Измените результат ответа",15f,white,true); cardText(this,"В демонстрационном сценарии правильный ответ снижает приоритет повторения, а ошибка повышает его.",12f,muted) }
        primary("Смоделировать правильный ответ") { DebugLog.add("INFO","APLA: демонстрационное обновление после правильного ответа"); Toast.makeText(this,"Модель знаний обновлена: элемент закреплён",Toast.LENGTH_SHORT).show() }
        secondary("Смоделировать ошибку") { DebugLog.add("WARN","APLA: демонстрационная ошибка повысила приоритет повторения"); Toast.makeText(this,"Приоритет повторения повышен",Toast.LENGTH_SHORT).show() }
        action("Посмотреть прогресс",ProgressActivity::class.java)
    }

    private fun renderServerQueue() {
        title("APLA", "Очередь повторения из серверной модели знаний")
        val status = TextView(this).apply { text = "Загрузка GET /review/queue…"; setTextColor(muted); textSize = 13f }
        root.addView(status)
        lifecycleScope.launch {
            try {
                val response = ApiRepository(this@AplaActivity).reviewQueue()
                if (!response.isSuccessful) { status.text = "Не удалось получить очередь: HTTP ${response.code()}"; return@launch }
                val data = response.body() ?: throw IllegalStateException("Пустой ответ review/queue")
                val items = data.getAsJsonArray("items")
                root.removeAllViews(); title("APLA", "Приоритеты повторения с сервера")
                card { cardText(this, "МОДЕЛЬ ЗНАНИЙ", 10f, mint, true); cardText(this, "Приоритеты формируются серверной APLA на основе освоения, вероятности воспроизведения и истории ошибок.", 13f); cardText(this, "Клиент отображает рекомендации, но не пересчитывает KnowledgeState самостоятельно.", 12f, muted) }
                if (items == null || items.size() == 0) { text("Сейчас повторение не требуется.", 15f); return@launch }
                items.forEachIndexed { index, element ->
                    val item = element.asJsonObject
                    card {
                        cardText(this, "${index + 1}. ${item.get("label")?.asString ?: "Учебный элемент"}", 16f, white, true)
                        progress(this, "Приоритет повторения", ((item.get("priority_score")?.asDouble ?: 0.0) * 100).toInt(), blue)
                        progress(this, "Освоение", ((item.get("mastery")?.asDouble ?: 0.0) * 100).toInt(), mint)
                        cardText(this, item.get("reason")?.asString ?: "Приоритет рассчитан сервером", 12f, muted)
                        cardText(this, "Повторить до: ${item.get("due_at")?.asString ?: "не задано"}", 11f, mint, true)
                    }
                }
            } catch (e: Exception) { status.text = "Ошибка загрузки: ${e.localizedMessage ?: e.javaClass.simpleName}"; DebugLog.add("ERROR", "APLA API: ${e.stackTraceToString()}") }
        }
    }
}
