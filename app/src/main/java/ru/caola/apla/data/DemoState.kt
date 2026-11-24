package ru.caola.apla.data

import android.content.Context

class DemoState(context: Context) {
    private val prefs = context.getSharedPreferences("caola_demo_state", Context.MODE_PRIVATE)
    var completedLessons: Int
        get() = prefs.getInt("completed_lessons", 4)
        set(value) { prefs.edit().putInt("completed_lessons", value).apply() }
    var totalAnswers: Int
        get() = prefs.getInt("total_answers", 37)
        set(value) { prefs.edit().putInt("total_answers", value).apply() }
    var correctAnswers: Int
        get() = prefs.getInt("correct_answers", 29)
        set(value) { prefs.edit().putInt("correct_answers", value).apply() }
    var lastLessonResult: String
        get() = prefs.getString("last_lesson_result", "Пока нет новых результатов") ?: ""
        set(value) { prefs.edit().putString("last_lesson_result", value).apply() }
    fun recordLesson(correct: Int, total: Int, title: String) {
        completedLessons += 1
        totalAnswers += total
        correctAnswers += correct
        lastLessonResult = "$title: $correct из $total"
    }
}
