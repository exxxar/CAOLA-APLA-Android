package ru.caola.apla.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.RadioButton
import android.widget.RadioGroup
import ru.caola.apla.data.DemoRepository
import ru.caola.apla.data.DebugLog
import java.util.Locale

class ListeningActivity : BaseActivity() {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val item = DemoRepository(this).section("listening")
        title(item.optString("title"))
        text(item.optString("instruction"))
        val phrase = item.optString("text")
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.UK)
                DebugLog.add("INFO", "TTS инициализирован: $result")
            } else DebugLog.add("ERROR", "Не удалось инициализировать TTS")
        }
        primary("▶ Прослушать фразу") {
            tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "caola-listening")
        }
        text(item.optString("question"), 18f)
        val options = item.optJSONArray("options") ?: return
        val group = RadioGroup(this)
        for (i in 0 until options.length()) group.addView(RadioButton(this).apply {
            text = options.getString(i); setTextColor(white); id = i
        })
        root.addView(group)
        primary("Проверить ответ") {
            val correct = group.checkedRadioButtonId == item.optInt("answer")
            text(if (correct) "Верно!" else "Попробуйте повторить фразу.")
            text(item.optString("explanation"), 13f)
            DebugLog.add("INFO", "Аудирование: ${if (correct) "верно" else "ошибка"}")
        }
    }
    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); super.onDestroy()
    }
}
