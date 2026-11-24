package ru.caola.apla.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.caola.apla.data.ApiRepository
import ru.caola.apla.data.AppMode
import ru.caola.apla.data.DebugLog

class AuthActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title("Вход в CAOLA / APLA", "Авторизация на сервере")
        text("Укажите учётные данные, зарегистрированные в backend. Пароль не сохраняется на устройстве.", 12f)
        section("Учётная запись")
        val displayName = field("Имя для регистрации")
        val email = field("Email").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val password = field("Пароль").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val status = field("Статус авторизации").apply { isFocusable = false; isClickable = false }

        primary("Войти") {
            val mail = email.text.toString().trim()
            val pass = password.text.toString()
            if (mail.isBlank() || pass.isBlank()) { Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show(); return@primary }
            authenticate(status, false, mail, pass, displayName.text.toString().trim())
        }
        secondary("Создать аккаунт") {
            val mail = email.text.toString().trim()
            val pass = password.text.toString()
            val name = displayName.text.toString().trim()
            if (mail.isBlank() || pass.isBlank() || name.isBlank()) { Toast.makeText(this, "Для регистрации заполните имя, email и пароль", Toast.LENGTH_LONG).show(); return@secondary }
            authenticate(status, true, mail, pass, name)
        }
        action("Настройки API", SettingsActivity::class.java)
    }

    private fun authenticate(status: android.widget.EditText, register: Boolean, email: String, password: String, name: String) {
        status.setText("Выполняется запрос к серверу…")
        lifecycleScope.launch {
            try {
                val repository = ApiRepository(this@AuthActivity)
                val response = if (register) repository.register(email, password, name) else repository.login(email, password)
                if (response.isSuccessful) {
                    session.mode = AppMode.PRODUCTION
                    status.setText("Авторизация успешна")
                    DebugLog.add("INFO", if (register) "Пользователь зарегистрирован" else "Вход выполнен")
                    Toast.makeText(this@AuthActivity, "Успешно. Загружаем кабинет.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@AuthActivity, DashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                } else {
                    val message = "Сервер отклонил запрос: HTTP ${response.code()} ${response.message()}"
                    status.setText(message)
                    DebugLog.add("WARN", "Auth API HTTP ${response.code()}")
                    Toast.makeText(this@AuthActivity, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                status.setText("Ошибка соединения: ${e.localizedMessage ?: e.javaClass.simpleName}")
                DebugLog.add("ERROR", "Auth API: ${e.stackTraceToString()}")
            }
        }
    }
}
