package ru.caola.apla.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.TextView
import android.widget.Toast

class RegisterActivity : AuthFlowActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title("Создание аккаунта", "Зарегистрируйтесь, чтобы начать обучение")
        card {
            cardText(this, "НОВЫЙ ПРОФИЛЬ", 11f, mint, true)
            cardText(this, "Заполните данные один раз. После регистрации откроется ваш личный кабинет.", 12f, muted)
        }
        section("Ваши данные")
        val displayName = field("Как к вам обращаться").apply { setSingleLine(true) }
        val email = field("Email").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine(true)
        }
        val password = field("Пароль (не менее 8 символов)").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        val confirm = field("Повторите пароль").apply {
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
        primary("Зарегистрироваться") {
            val name = displayName.text.toString().trim()
            val mail = email.text.toString().trim()
            val pass = password.text.toString()
            val repeated = confirm.text.toString()
            when {
                name.isBlank() || mail.isBlank() || pass.isBlank() || repeated.isBlank() ->
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                pass.length < 8 ->
                    Toast.makeText(this, "Пароль должен содержать не менее 8 символов", Toast.LENGTH_SHORT).show()
                pass != repeated ->
                    Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                else -> submitAuth(status, true, mail, pass, name)
            }
        }
        text("Уже зарегистрированы?", 12f)
        secondary("Перейти ко входу") {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
