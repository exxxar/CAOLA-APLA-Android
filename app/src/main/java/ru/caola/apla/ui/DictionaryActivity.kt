package ru.caola.apla.ui

import android.os.Bundle
import ru.caola.apla.data.DemoRepository

class DictionaryActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title("Словарь и материалы")
        val words = DemoRepository(this).data.optJSONArray("vocabulary")
        if (words == null) { text("Материалы отсутствуют"); return }
        for (i in 0 until words.length()) {
            val item = words.getJSONObject(i)
            text("${item.optString("word")} — ${item.optString("translation")}", 18f)
            text(item.optString("example"), 14f)
            text("${item.optString("topic")} · ${item.optString("status")}", 12f)
            text("────────────────────────")
        }
    }
}
