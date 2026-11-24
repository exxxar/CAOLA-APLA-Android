package ru.caola.apla.data

import android.content.Context

class SessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("caola_session", Context.MODE_PRIVATE)
    private val secureTokenStore = SecureTokenStore(appContext)

    var mode: AppMode
        get() = if (prefs.getString("mode", "DEMO") == "PRODUCTION") AppMode.PRODUCTION else AppMode.DEMO
        set(value) { prefs.edit().putString("mode", value.name).apply() }

    var apiBaseUrl: String
        get() = prefs.getString("api_base_url", "") ?: ""
        set(value) { prefs.edit().putString("api_base_url", value.trim()).apply() }

    var apiPathPrefix: String
        get() = prefs.getString("api_path_prefix", "/api/v1") ?: "/api/v1"
        set(value) { prefs.edit().putString("api_path_prefix", value.trim()).apply() }

    var connectTimeoutSeconds: Int
        get() = prefs.getInt("connect_timeout_seconds", 15)
        set(value) { prefs.edit().putInt("connect_timeout_seconds", value.coerceIn(3, 120)).apply() }

    var readTimeoutSeconds: Int
        get() = prefs.getInt("read_timeout_seconds", 30)
        set(value) { prefs.edit().putInt("read_timeout_seconds", value.coerceIn(3, 300)).apply() }

    var accessToken: String
        get() = secureTokenStore.read()
        set(value) { secureTokenStore.write(value.trim()) }

    var refreshToken: String
        get() = secureTokenStore.readRefresh()
        set(value) { secureTokenStore.writeRefresh(value.trim()) }

    fun apiRoot(): String {
        val base = apiBaseUrl.trim().trimEnd('/')
        val prefix = "/" + apiPathPrefix.trim().trim('/')
        return if (prefix == "/") base else base + prefix
    }
}
