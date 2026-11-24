package ru.caola.apla.ui

import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.caola.apla.data.ApiClient
import ru.caola.apla.data.AppMode
import ru.caola.apla.data.DebugLog
import ru.caola.apla.data.NetworkEndpointPolicy
import ru.caola.apla.BuildConfig
import java.io.IOException

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title("Настройки", "Режим работы, API и разрешения контекста")

        section("Режим приложения")
        val modeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Демонстрационный режим", "Работа с сервером"))
            setSelection(if (session.mode == AppMode.PRODUCTION) 1 else 0)
        }
        root.addView(modeSpinner, LinearLayout.LayoutParams(-1, dp(52)))
        text("Демо-режим работает на встроенных данных. Серверный режим использует REST API и требует доступный backend.", 12f)

        section("Адрес сервера")
        text("Базовый URL (домен без /api/v1)", 12f)
        val defaultBaseUrl = if (BuildConfig.DEBUG) "http://10.0.2.2:8000" else "https://api.example.com"
        // Persist a safe default when the user opens Settings and immediately proceeds to Login.
        // This prevents ApiClient from being initialized with an empty endpoint.
        if (session.apiBaseUrl.isBlank()) session.apiBaseUrl = defaultBaseUrl
        val baseUrl = field(defaultBaseUrl, session.apiBaseUrl.ifBlank { defaultBaseUrl }).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        text("Префикс API", 12f)
        val apiPrefix = field("/api/v1", session.apiPathPrefix)
        text("Итоговый адрес: ${session.apiRoot().ifBlank { "не задан" }}", 11f)
        if (BuildConfig.DEBUG) {
            text("Локальная разработка: эмулятор Android → http://10.0.2.2:8000; физический телефон → http://IP_КОМПЬЮТЕРА:8000 (например, 192.168.1.50). Компьютер и телефон должны быть в одной Wi-Fi сети. HTTP разрешён только для локальных адресов в debug-сборке.", 11f)
        } else {
            text("Для публичного сервера используйте HTTPS.", 11f)
        }

        section("Авторизация")
        text("Access token", 12f)
        val token = field("Bearer access token", session.accessToken).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        text("Refresh token", 12f)
        val refreshToken = field("Refresh token", session.refreshToken).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        text("Оба токена шифруются отдельными ключами Android Keystore. Пароль пользователя и ключи LLM не хранятся в приложении.", 11f)

        section("Тайм-ауты")
        text("Соединение, секунд", 12f)
        val connectTimeout = field("15", session.connectTimeoutSeconds.toString()).apply { inputType = InputType.TYPE_CLASS_NUMBER }
        text("Ответ сервера, секунд", 12f)
        val readTimeout = field("30", session.readTimeoutSeconds.toString()).apply { inputType = InputType.TYPE_CLASS_NUMBER }

        val status = TextView(this).apply {
            text = "Статус соединения: не проверялось"
            textSize = 12f
            setTextColor(muted)
            setPadding(0, dp(10), 0, dp(10))
        }
        root.addView(status)

        fun validateAndStore(): Boolean {
            val url = baseUrl.text.toString().trim().trimEnd('/')
            val prefix = apiPrefix.text.toString().trim().ifBlank { "/api/v1" }
            val connect = connectTimeout.text.toString().toIntOrNull()
            val read = readTimeout.text.toString().toIntOrNull()
            val selectedMode = if (modeSpinner.selectedItemPosition == 1) AppMode.PRODUCTION else AppMode.DEMO
            if (selectedMode == AppMode.PRODUCTION && !NetworkEndpointPolicy.isAllowed(url, BuildConfig.DEBUG)) {
                val message = if (BuildConfig.DEBUG) {
                    "Укажите HTTPS или локальный HTTP-адрес: 10.0.2.2, localhost либо IP компьютера в локальной сети"
                } else {
                    "Для релизной версии укажите корректный HTTPS-адрес"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show(); return false
            }
            if (!isValidPrefix(prefix)) {
                Toast.makeText(this, "Префикс должен начинаться с / и не содержать пробелов", Toast.LENGTH_LONG).show(); return false
            }
            if (connect == null || connect !in 3..120 || read == null || read !in 3..300) {
                Toast.makeText(this, "Тайм-ауты: соединение 3–120 с, ответ 3–300 с", Toast.LENGTH_LONG).show(); return false
            }
            session.apiBaseUrl = url
            session.apiPathPrefix = prefix
            session.accessToken = token.text.toString()
            session.refreshToken = refreshToken.text.toString()
            session.connectTimeoutSeconds = connect
            session.readTimeoutSeconds = read
            session.mode = selectedMode
            return true
        }

        primary("Сохранить настройки") {
            if (!validateAndStore()) return@primary
            DebugLog.add("INFO", "Настройки API сохранены; режим=${session.mode.name}; endpoint=${session.apiRoot()}")
            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
        }

        val testButton = Button(this).apply {
            text = "Проверить соединение с сервером"
            isAllCaps = false
            textSize = 14f
            setTextColor(white)
            background = rounded(panel2, 14)
            setOnClickListener {
                if (!validateAndStore()) return@setOnClickListener
                if (session.mode != AppMode.PRODUCTION) {
                    Toast.makeText(this@SettingsActivity, "Для проверки выберите режим работы с сервером", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                isEnabled = false
                text = "Проверяем…"
                status.text = "Статус соединения: выполняется запрос GET ${session.apiRoot()}/onboarding"
                lifecycleScope.launch {
                    try {
                        val response = ApiClient.create(this@SettingsActivity, session).getOnboarding()
                        val message = when {
                            response.isSuccessful -> "Сервер доступен. API отвечает HTTP ${response.code()}"
                            response.code() == 401 || response.code() == 403 -> "Сервер доступен, но требуется корректная авторизация (HTTP ${response.code()})"
                            response.code() == 404 -> "Сервер доступен, но маршрут /onboarding не найден (HTTP 404). Сверьте версию API"
                            else -> "Сервер ответил HTTP ${response.code()}: ${response.message()}"
                        }
                        status.text = "Статус соединения: $message"
                        status.setTextColor(if (response.isSuccessful || response.code() in 401..403) mint else muted)
                        DebugLog.add("INFO", "Проверка API: $message")
                        Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        val message = when (e) {
                            is IOException -> "Не удалось подключиться: ${e.localizedMessage ?: "проверьте сеть и адрес"}"
                            else -> "Ошибка запроса: ${e.localizedMessage ?: e.javaClass.simpleName}"
                        }
                        status.text = "Статус соединения: $message"
                        status.setTextColor(0xFFFF8A80.toInt())
                        DebugLog.add("ERROR", "Проверка API: ${e.stackTraceToString()}")
                        Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
                    } finally {
                        isEnabled = true
                        text = "Проверить соединение с сервером"
                    }
                }
            }
        }
        root.addView(testButton, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(8) })

        section("Источники контекста CAOLA")
        listOf("Расписание пользователя", "История занятий", "Физическая активность", "Календарь").forEach { label ->
            root.addView(CheckBox(this).apply {
                text = label; setTextColor(white)
                isChecked = label == "Расписание пользователя" || label == "История занятий"
                setOnCheckedChangeListener { _, checked -> DebugLog.add("INFO", "Источник '$label': ${if (checked) "включён" else "отключён"}") }
            })
        }
        text("Источники включаются независимо. Передача контекстных событий на сервер выполняется только после согласия пользователя.", 12f)
        action("Войти в аккаунт", LoginActivity::class.java)
        action("Открыть отладку", DebugActivity::class.java)
    }

    private fun isValidPrefix(value: String): Boolean = value.startsWith("/") && !value.contains(" ") && !value.contains("?") && !value.contains("#")
}
