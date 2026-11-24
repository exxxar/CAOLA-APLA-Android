package ru.caola.apla.ui

import android.os.Bundle

class OnboardingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title("Настройка обучения")
        text("Укажите параметры персональной траектории.")
        field("Изучаемый язык", "Английский")
        field("Родной язык", "Русский")
        field("Уровень", "A2")
        field("Цель обучения", "Разговорная практика")
        field("Желаемая длительность занятия, минут", "15")
        primary("Сохранить настройки") {
            ru.caola.apla.data.DebugLog.add("INFO", "Параметры обучения сохранены локально (демо-заготовка)")
            finish()
        }
    }
}
