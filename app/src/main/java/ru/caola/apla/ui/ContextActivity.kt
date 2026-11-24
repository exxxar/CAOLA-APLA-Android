package ru.caola.apla.ui

import android.os.Bundle
import android.widget.CheckBox
import ru.caola.apla.data.DemoRepository
import ru.caola.apla.data.DebugLog

class ContextActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title("Контекст пользователя")
        val ctx = DemoRepository(this).section("context")
        text("Расписание: ${ctx.optString("schedule")}")
        text("Активность: ${ctx.optString("activity")}")
        text("Период тишины: ${ctx.optString("quiet_hours")}")
        text("Источники контекста", 20f)
        val sources = ctx.optJSONArray("sources") ?: return
        for (i in 0 until sources.length()) {
            val source = sources.getJSONObject(i)
            root.addView(CheckBox(this).apply {
                text = source.optString("name") + "\n" + source.optString("description")
                setTextColor(white)
                isChecked = source.optBoolean("enabled")
                setOnCheckedChangeListener { _, checked ->
                    DebugLog.add("INFO", "Демо-источник '${source.optString("name")}': $checked")
                }
            })
        }
        text("Это демонстрационные признаки. Реальные датчики и разрешения Android не используются.", 13f)
    }
}
