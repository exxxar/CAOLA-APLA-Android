package ru.caola.apla.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import ru.caola.apla.data.ApiRepository
import ru.caola.apla.data.AppMode
import ru.caola.apla.data.DemoRepository
import ru.caola.apla.data.DebugLog
import java.util.UUID

class LessonCatalogActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (session.mode == AppMode.PRODUCTION) { renderServerTrajectory(); return }
        title("Каталог уроков","Выберите короткое занятие под вашу цель")
        val lessons=DemoRepository(this).data.optJSONArray("lessons") ?: return
        for(i in 0 until lessons.length()) { val lesson=lessons.getJSONObject(i)
            card {
                val row=LinearLayout(this@LessonCatalogActivity).apply { orientation=LinearLayout.HORIZONTAL }
                row.addView(TextView(this@LessonCatalogActivity).apply { text=lesson.optString("level"); textSize=11f; setTextColor(mint); setPadding(dp(10),dp(6),dp(10),dp(6)); background=rounded(panel2,12) }); addView(row)
                cardText(this,lesson.optString("title"),19f,white,true); cardText(this,lesson.optString("description"),13f,muted)
                cardText(this,"◷  ${lesson.optInt("minutes")} минут     •     ${lesson.optJSONArray("exercises")?.length() ?: 0} задания",12f,blue,true)
                addView(Button(this@LessonCatalogActivity).apply { text="Начать урок  →"; isAllCaps=false; setTextColor(navy); background=rounded(mint,12); setOnClickListener { startActivity(Intent(this@LessonCatalogActivity,LessonActivity::class.java).putExtra("lesson_id",lesson.optString("id"))) } },LinearLayout.LayoutParams(-1,dp(46)).apply { topMargin=dp(8) })
            }
        }
        section("Дополнительная практика"); action("Аудирование",ListeningActivity::class.java); action("Диалоговая практика",DialogueActivity::class.java)
    }

    private fun renderServerTrajectory() {
        title("Каталог обучения", "Микроуроки и траектория с сервера")
        val status = TextView(this).apply { text = "Загрузка GET /learning/trajectory…"; textSize = 13f; setTextColor(muted) }
        root.addView(status)
        lifecycleScope.launch {
            try {
                val response = ApiRepository(this@LessonCatalogActivity).trajectory()
                if (!response.isSuccessful) { status.text = "Не удалось загрузить траекторию: HTTP ${response.code()}"; return@launch }
                val data = response.body() ?: throw IllegalStateException("Пустой ответ trajectory")
                val lessons = data.getAsJsonArray("lessons") ?: data.getAsJsonArray("items")
                root.removeAllViews(); title("Каталог обучения", "Доступные микроуроки с сервера")
                if (lessons == null || lessons.size() == 0) { text("Сервер пока не вернул доступных уроков.", 14f); return@launch }
                lessons.forEach { element ->
                    val lesson = element.asJsonObject
                    val id = lesson.get("id")?.asString ?: ""
                    card {
                        cardText(this, lesson.get("level")?.asString ?: "Уровень не указан", 11f, mint, true)
                        cardText(this, lesson.get("title")?.asString ?: "Микроурок", 18f, white, true)
                        cardText(this, lesson.get("description")?.asString ?: "", 13f, muted)
                        cardText(this, "${lesson.get("duration_minutes")?.asInt ?: 0} минут · ${lesson.get("exercises_count")?.asInt ?: 0} заданий", 12f, blue, true)
                        addView(Button(this@LessonCatalogActivity).apply {
                            text = "Начать урок  →"; isAllCaps = false; setTextColor(navy); background = rounded(mint, 12)
                            isEnabled = id.isNotBlank()
                            setOnClickListener { startServerLesson(id) }
                        }, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(8) })
                    }
                }
            } catch (e: Exception) { status.text = "Ошибка загрузки: ${e.localizedMessage ?: e.javaClass.simpleName}"; DebugLog.add("ERROR", "Trajectory API: ${e.stackTraceToString()}") }
        }
    }

    private fun startServerLesson(trajectoryItemId: String) {
        lifecycleScope.launch {
            try {
                val body = JsonObject().apply { addProperty("request_id", UUID.randomUUID().toString()); addProperty("trajectory_item_id", trajectoryItemId) }
                val response = ApiRepository(this@LessonCatalogActivity).startLesson(body)
                if (!response.isSuccessful) { android.widget.Toast.makeText(this@LessonCatalogActivity, "Не удалось начать урок: HTTP ${response.code()}", android.widget.Toast.LENGTH_LONG).show(); return@launch }
                val lessonId = response.body()?.get("lesson_id")?.asString
                    ?: response.body()?.get("id")?.asString
                    ?: throw IllegalStateException("Сервер не вернул lesson_id")
                DebugLog.add("INFO", "Создана серверная учебная сессия $lessonId")
                startActivity(Intent(this@LessonCatalogActivity, LessonActivity::class.java).putExtra("server_lesson_id", lessonId))
            } catch (e: Exception) {
                DebugLog.add("ERROR", "Start lesson API: ${e.stackTraceToString()}")
                android.widget.Toast.makeText(this@LessonCatalogActivity, "Ошибка запуска: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
