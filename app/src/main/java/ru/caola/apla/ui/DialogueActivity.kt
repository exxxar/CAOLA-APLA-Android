package ru.caola.apla.ui

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import ru.caola.apla.data.DemoRepository
import ru.caola.apla.data.DebugLog

class DialogueActivity : BaseActivity() {
    private var step = 0
    private var score = 0
    private lateinit var turns: org.json.JSONArray
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = DemoRepository(this).section("dialogue")
        title(data.optString("title"))
        turns = data.optJSONArray("turns") ?: org.json.JSONArray()
        showTurn()
    }
    private fun showTurn() {
        if (step >= turns.length()) {
            text("Диалог завершён: $score из ${turns.length()}")
            DebugLog.add("INFO", "Диалог завершён: $score/${turns.length()}")
            primary("Завершить") { finish() }
            return
        }
        val turn = turns.getJSONObject(step)
        text("Реплика ${step + 1} из ${turns.length()} · ${turn.optString("speaker")}")
        text(turn.optString("text"), 20f)
        text(turn.optString("translation"), 14f)
        val opts = turn.optJSONArray("options") ?: return
        val group = RadioGroup(this)
        for (i in 0 until opts.length()) group.addView(RadioButton(this).apply {
            text = opts.getString(i); setTextColor(white); id = i
        })
        root.addView(group)
        primary("Ответить") {
            val correct = group.checkedRadioButtonId == turn.optInt("answer")
            if (correct) score++
            text(if (correct) "Подходит по смыслу." else "Попробуйте выбрать более естественную реплику.")
            step++
            showTurn()
        }
    }
}
