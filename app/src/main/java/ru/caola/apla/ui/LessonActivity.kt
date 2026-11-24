package ru.caola.apla.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import ru.caola.apla.data.ApiRepository
import ru.caola.apla.data.AppMode
import java.util.UUID
import ru.caola.apla.data.DemoRepository
import ru.caola.apla.data.DemoState
import ru.caola.apla.data.DebugLog
import org.json.JSONArray
import org.json.JSONObject

class LessonActivity : BaseActivity() {
    private lateinit var lessons: JSONArray
    private var lessonIndex = 0
    private var exerciseIndex = 0
    private var score = 0
    private var total = 0
    private var answerGroup: RadioGroup? = null
    private var answerInput: EditText? = null
    private var serverLessonId: String? = null
    private var serverLesson: JSONObject? = null
    private var serverExerciseIndex = 0
    private var serverCorrect = 0
    private var serverAnswered = 0
    private var serverStartedAt = 0L
    private var serverOptionIds = emptyList<String>()
    private var serverAnswerSubmitted = false
    private var serverExerciseStartedAt = 0L
    private var pendingCompleteBody: JsonObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serverLessonId = intent.getStringExtra("server_lesson_id")
        if (session.mode == AppMode.PRODUCTION && !serverLessonId.isNullOrBlank()) {
            loadServerLesson(serverLessonId!!)
            return
        }
        val all = DemoRepository(this).data.optJSONArray("lessons") ?: JSONArray()
        val selectedId = intent.getStringExtra("lesson_id")
        lessonIndex = (0 until all.length()).firstOrNull {
            all.getJSONObject(it).optString("id") == selectedId
        } ?: 0
        lessons = all
        showExercise()
    }

    private fun loadServerLesson(id: String) {
        title("Загрузка урока", "Получаем содержание с сервера")
        val status = TextView(this).apply { text = "GET /lessons/$id"; setTextColor(muted) }
        root.addView(status)
        lifecycleScope.launch {
            try {
                val response = ApiRepository(this@LessonActivity).lesson(id)
                if (!response.isSuccessful) { status.text = "Не удалось загрузить урок: HTTP ${response.code()}"; return@launch }
                val payload = response.body() ?: throw IllegalStateException("Пустой ответ урока")
                val lessonJson = if (payload.has("lesson") && payload.get("lesson").isJsonObject) payload.getAsJsonObject("lesson") else payload
                serverLesson = JSONObject(lessonJson.toString())
                serverStartedAt = System.currentTimeMillis()
                serverExerciseIndex = 0; serverCorrect = 0; serverAnswered = 0
                showServerExercise()
            } catch (e: Exception) {
                status.text = "Ошибка загрузки урока: ${e.localizedMessage ?: e.javaClass.simpleName}"
                DebugLog.add("ERROR", "Server lesson load: ${e.stackTraceToString()}")
            }
        }
    }

    private fun showServerExercise() {
        val lesson = serverLesson ?: return
        val exercises = lesson.optJSONArray("exercises") ?: JSONArray()
        if (serverExerciseIndex >= exercises.length()) { completeServerLesson(); return }
        root.removeAllViews()
        title(lesson.optString("title", "Учебный блок"), lesson.optString("description"))
        val exercise = exercises.getJSONObject(serverExerciseIndex)
        serverExerciseStartedAt = System.currentTimeMillis()
        serverAnswerSubmitted = false
        text("Задание ${serverExerciseIndex + 1} из ${exercises.length()} · ${exercise.optString("type")}", 12f)
        text(exercise.optString("prompt", exercise.optString("question")), 19f)
        answerGroup = null; answerInput = null
        val options = exercise.optJSONArray("options")
        serverOptionIds = emptyList()
        if (options != null && options.length() > 0) {
            val ids = mutableListOf<String>()
            val group = RadioGroup(this)
            for (i in 0 until options.length()) {
                val option = options.opt(i)
                val optionId: String
                val optionText: String
                if (option is JSONObject) { optionId = option.optString("id", i.toString()); optionText = option.optString("text", optionId) }
                else { optionId = i.toString(); optionText = option.toString() }
                ids.add(optionId)
                group.addView(RadioButton(this).apply { text = optionText; setTextColor(white); id = i })
            }
            root.addView(group); answerGroup = group; serverOptionIds = ids
        } else {
            answerInput = field("Введите ответ")
        }
        primary("Отправить ответ") {
            if (serverAnswerSubmitted) return@primary
            val answer = JsonObject()
            if (options != null && options.length() > 0) {
                val selected = answerGroup?.checkedRadioButtonId ?: -1
                if (selected !in serverOptionIds.indices) { android.widget.Toast.makeText(this, "Выберите вариант ответа", android.widget.Toast.LENGTH_SHORT).show(); return@primary }
                answer.addProperty("selected_option_id", serverOptionIds[selected])
            } else {
                val value = answerInput?.text?.toString()?.trim().orEmpty()
                if (value.isBlank()) { android.widget.Toast.makeText(this, "Введите ответ", android.widget.Toast.LENGTH_SHORT).show(); return@primary }
                answer.addProperty("text", value)
            }
            serverAnswerSubmitted = true
            val exerciseId = exercise.optString("id")
            val body = JsonObject().apply {
                addProperty("event_id", UUID.randomUUID().toString())
                addProperty("exercise_id", exerciseId)
                add("answer", answer)
                addProperty("elapsed_ms", (System.currentTimeMillis() - serverExerciseStartedAt).toInt())
            }
            lifecycleScope.launch {
                try {
                    val response = ApiRepository(this@LessonActivity).submitAnswer(serverLessonId!!, body)
                    if (!response.isSuccessful) { serverAnswerSubmitted = false; text("Ответ не сохранён: HTTP ${response.code()}", 13f); DebugLog.add("WARN", "Answer API HTTP ${response.code()}"); return@launch }
                    val result = response.body() ?: JsonObject()
                    val correct = result.get("correct")?.asBoolean ?: false
                    if (correct) serverCorrect++
                    serverAnswered++
                    text(if (correct) "Верно!" else "Ответ требует повторения.", 16f)
                    text(result.get("explanation")?.asString ?: result.get("feedback")?.asString ?: "Результат проверен сервером.", 13f)
                    DebugLog.add(if (correct) "INFO" else "WARN", "Server answer $exerciseId correct=$correct")
                    primary("Следующее задание") { serverExerciseIndex++; showServerExercise() }
                } catch (e: Exception) { serverAnswerSubmitted = false; text("Ошибка отправки ответа: ${e.localizedMessage}", 13f); DebugLog.add("ERROR", "Answer API: ${e.stackTraceToString()}") }
            }
        }
    }

    private fun completeServerLesson() {
        val id = serverLessonId ?: return
        root.removeAllViews()
        title("Завершение урока", "Сохраняем результат на сервере")
        val status = TextView(this).apply { text = "POST /lessons/$id/complete"; setTextColor(muted) }
        root.addView(status)
        lifecycleScope.launch {
            try {
                val body = pendingCompleteBody ?: JsonObject().apply {
                    addProperty("event_id", UUID.randomUUID().toString())
                    addProperty("elapsed_seconds", ((System.currentTimeMillis() - serverStartedAt) / 1000).toInt())
                    addProperty("answered_count", serverAnswered)
                }.also { pendingCompleteBody = it }
                val response = ApiRepository(this@LessonActivity).completeLesson(id, body)
                if (!response.isSuccessful) {
                    status.text = "Не удалось завершить урок: HTTP ${response.code()}"
                    primary("Повторить сохранение результата") { completeServerLesson() }
                    return@launch
                }
                pendingCompleteBody = null
                val result = response.body()
                root.removeAllViews(); title("Урок завершён", "Результат сохранён сервером")
                text("Правильных ответов: $serverCorrect из $serverAnswered", 18f)
                text("Итоговый балл: ${result?.get("score")?.asString ?: "рассчитывается сервером"}", 14f)
                text("APLA обновила модель знаний и очередь повторения.", 13f)
                DebugLog.add("INFO", "Server lesson $id completed: $serverCorrect/$serverAnswered")
                primary("Вернуться к обучению") { finish() }
            } catch (e: Exception) {
                status.text = "Ошибка завершения: ${e.localizedMessage}"
                primary("Повторить сохранение результата") { completeServerLesson() }
                DebugLog.add("ERROR", "Complete lesson API: ${e.stackTraceToString()}")
            }
        }
    }

    private fun currentLesson(): JSONObject = lessons.getJSONObject(lessonIndex)

    private fun showExercise() {
        root.removeAllViews()
        val lesson = currentLesson()
        if (exerciseIndex == 0) {
            title(lesson.optString("title"))
            text(lesson.optString("description"))
        } else title(lesson.optString("title"))
        val exercises = lesson.optJSONArray("exercises") ?: JSONArray()
        if (exerciseIndex >= exercises.length()) {
            DemoState(this).recordLesson(score, total, lesson.optString("title"))
            text("Занятие завершено!")
            text("Результат: $score из $total")
            text("APLA: результат сохранён; приоритеты повторения будут пересчитаны.")
            DebugLog.add("INFO", "Урок '${lesson.optString("title")}' завершён: $score/$total")
            primary("Вернуться на главную") { finish() }
            return
        }
        val item = exercises.getJSONObject(exerciseIndex)
        text("Задание ${exerciseIndex + 1} из ${exercises.length()} · ${item.optString("type")}")
        text(item.optString("question"), 19f)
        answerGroup = null
        answerInput = null
        val options = item.optJSONArray("options")
        if (options != null) {
            val group = RadioGroup(this)
            for (i in 0 until options.length()) group.addView(RadioButton(this).apply {
                text = options.getString(i); setTextColor(white); id = i
            })
            root.addView(group)
            answerGroup = group
        } else {
            answerInput = field("Введите ответ")
        }
        primary("Проверить ответ") {
            val correct = if (options != null) {
                answerGroup?.checkedRadioButtonId == item.optInt("answer") &&
                    (answerGroup?.checkedRadioButtonId ?: -1) >= 0
            } else {
                answerInput?.text?.toString()?.trim()?.equals(item.optString("answer_text"), true) == true
            }
            total++
            if (correct) score++
            text(if (correct) "Верно!" else "Ответ требует повторения.")
            text(item.optString("explanation"), 14f)
            DebugLog.add(if (correct) "INFO" else "WARN",
                "Задание '${item.optString("item")}': ${if (correct) "верно" else "ошибка"}")
            primary("Следующее задание") {
                exerciseIndex++
                showExercise()
            }
        }
    }
}
