package ru.caola.apla.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.TextView
import android.widget.Toast

class LoginActivity : AuthFlowActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title("С возвращением", "Войдите в свой аккаунт CAOLA / APLA")
        card {
            cardText(this, "CAOLA  /  APLA", 13f, mint, true)
            cardText(this, "Ваш персональный маршрут изучения языка продолжится с того места, где вы остановились.", 12f, muted)
        }
        section("Вход в аккаунт")
        val email = field("Email").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine(true)
        }
        val password = field("Пароль").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        val status = TextView(this).apply {
            text = ""
            textSize = 12f
            setTextColor(muted)
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(status)
        primary("Войти") {
            val mail = email.text.toString().trim()
            val pass = password.text.toString()
            if (mail.isBlank() || pass.isBlank()) {
                Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show()
                return@primary
            }
            submitAuth(status, false, mail, pass)
        }
        text("Впервые в CAOLA / APLA? Создайте аккаунт отдельно.", 12f)
        secondary("Создать аккаунт") {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        action("Настройки подключения", SettingsActivity::class.java)
    }
}
