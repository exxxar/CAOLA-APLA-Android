package ru.caola.apla.ui

import android.os.Bundle
import ru.caola.apla.data.DemoRepository
import ru.caola.apla.data.DemoState

class HistoryActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title("История занятий")
        val state = DemoState(this)
        text("Завершено занятий: ${state.completedLessons}")
        text("Правильных ответов: ${state.correctAnswers} из ${state.totalAnswers}")
        text("Последний результат: ${state.lastLessonResult}")
        val history = DemoRepository(this).section("progress").optJSONArray("history")
        if (history != null) for (i in 0 until history.length()) {
            val item = history.getJSONObject(i)
            text("${item.optString("date")} · ${item.optString("title")}", 17f)
            text("${item.optString("result")} · ${item.optInt("minutes")} мин.")
        }
    }
}
