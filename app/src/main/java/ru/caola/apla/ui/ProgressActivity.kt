package ru.caola.apla.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.caola.apla.data.ApiRepository
import ru.caola.apla.data.AppMode
import ru.caola.apla.data.DebugLog
import ru.caola.apla.data.DemoRepository
import ru.caola.apla.data.DemoState

class ProgressActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (session.mode == AppMode.PRODUCTION) { renderServerProgress(); return }
        val repo=DemoRepository(this); val data=repo.section("progress"); val state=DemoState(this)
        title("Ваш прогресс","Учебная статистика и динамика освоения")
        val metrics=card { orientation=LinearLayout.HORIZONTAL }
        listOf(Pair("${data.optInt("completion")}%","освоено"),Pair("${state.completedLessons}","уроков"),Pair("${if(state.totalAnswers>0) state.correctAnswers*100/state.totalAnswers else 0}%","точность")).forEach { (v,l) -> val col=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f) }; metric(col,v,l); metrics.addView(col) }
        section("Активность за неделю")
        val weekly=data.optJSONArray("weekly"); val vals=mutableListOf<Int>(); if(weekly!=null) for(i in 0 until weekly.length()) vals.add(weekly.optInt(i))
        card { cardText(this,"Минуты обучения",12f,muted); addView(chart(vals,listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс"))) }
        section("Освоение по направлениям")
        val topics=data.optJSONArray("topics"); if(topics!=null) for(i in 0 until topics.length()) { val t=topics.getJSONObject(i); card { progress(this,t.optString("name"),t.optInt("value"),if(i%2==0) mint else blue) } }
        section("Приоритеты повторения APLA")
        val items=repo.section("apla").optJSONArray("priority_items"); if(items!=null) for(i in 0 until items.length()) { val it=items.getJSONObject(i); card { cardText(this,it.optString("name"),15f,white,true); progress(this,"Приоритет повторения",(it.optDouble("priority")*100).toInt(),blue); cardText(this,it.optString("reason"),12f,muted) } }
    }

    private fun renderServerProgress() {
        title("Ваш прогресс", "Сводка и динамика из серверного API")
        val status = TextView(this).apply { text = "Загрузка GET /progress/summary…"; setTextColor(muted); textSize = 13f }
        root.addView(status)
        lifecycleScope.launch {
            try {
                val response = ApiRepository(this@ProgressActivity).progressSummary()
                if (!response.isSuccessful) { status.text = "Не удалось загрузить прогресс: HTTP ${response.code()}"; return@launch }
                val data = response.body() ?: throw IllegalStateException("Пустой ответ progress/summary")
                root.removeAllViews(); title("Ваш прогресс", "Данные получены от сервера")
                val metrics = card { orientation = LinearLayout.HORIZONTAL }
                listOf(
                    Pair("${data.get("completion_percent")?.asInt ?: 0}%", "освоено"),
                    Pair("${data.get("lessons_completed")?.asInt ?: 0}", "уроков"),
                    Pair("${data.get("accuracy_percent")?.asInt ?: 0}%", "точность")
                ).forEach { (v, l) -> val col = LinearLayout(this@ProgressActivity).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }; metric(col, v, l); metrics.addView(col) }
                section("Активность за неделю")
                val weekly = data.getAsJsonArray("weekly_minutes")
                val values = weekly?.map { it.asInt } ?: emptyList()
                card { cardText(this, "Минуты обучения", 12f, muted); addView(chart(values, listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"))) }
                section("Освоение по направлениям")
                data.getAsJsonArray("topics")?.forEach { item ->
                    val topic = item.asJsonObject
                    card { progress(this, topic.get("name")?.asString ?: "Тема", topic.get("mastery_percent")?.asInt ?: 0) }
                }
                action("История занятий", HistoryActivity::class.java)
            } catch (e: Exception) { status.text = "Ошибка загрузки: ${e.localizedMessage ?: e.javaClass.simpleName}"; DebugLog.add("ERROR", "Progress API: ${e.stackTraceToString()}") }
        }
    }
}
