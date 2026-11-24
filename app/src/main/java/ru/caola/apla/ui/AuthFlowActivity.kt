package ru.caola.apla.ui

import android.content.Intent
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Response
import com.google.gson.JsonObject
import ru.caola.apla.data.ApiRepository
import ru.caola.apla.data.AppMode
import ru.caola.apla.data.DebugLog

/** Shared network flow; the login and registration forms remain separate screens. */
abstract class AuthFlowActivity : BaseActivity() {
    protected fun submitAuth(
        status: TextView,
        register: Boolean,
        email: String,
        password: String,
        displayName: String = ""
    ) {
        status.text = "Выполняется запрос к серверу…"
        lifecycleScope.launch {
            try {
                val repository = ApiRepository(this@AuthFlowActivity)
                val response = if (register) {
                    repository.register(email, password, displayName)
                } else {
                    repository.login(email, password)
                }
                if (response.isSuccessful) {
                    session.mode = AppMode.PRODUCTION
                    status.text = "Успешно"
                    DebugLog.add("INFO", if (register) "Пользователь зарегистрирован" else "Вход выполнен")
                    Toast.makeText(this@AuthFlowActivity, "Успешно. Загружаем кабинет.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@AuthFlowActivity, DashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                    finish()
                } else {
                    val requestUrl = response.raw().request.url.toString()
                    val errorBody = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("").take(700)
                    val serverDetail = extractDetail(errorBody).ifBlank { response.message() }
                    val message = when (response.code()) {
                        401 -> "Сервер не принял email или пароль. Проверьте учётные данные и адрес Backend."
                        403 -> serverDetail.ifBlank { "Учётная запись заблокирована или доступ запрещён." }
                        409 -> serverDetail.ifBlank { "Пользователь с таким email уже существует." }
                        else -> "Сервер отклонил запрос: HTTP ${response.code()}. $serverDetail"
                    }
                    status.text = message
                    DebugLog.add("WARN", "Auth API HTTP ${response.code()}; url=$requestUrl; response=$errorBody")
                    Toast.makeText(this@AuthFlowActivity, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                status.text = "Ошибка соединения: ${e.localizedMessage ?: e.javaClass.simpleName}"
                DebugLog.add("ERROR", "Auth API: ${e.stackTraceToString()}")
            }
        }
    }

    private fun extractDetail(raw: String): String = runCatching {
        val json = com.google.gson.JsonParser.parseString(raw).asJsonObject
        json.get("detail")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
    }.getOrDefault("")
}
