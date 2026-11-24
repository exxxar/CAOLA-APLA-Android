package ru.caola.apla.data

import com.google.gson.JsonParser
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

/** Rotates the refresh token after a 401 and retries the original request once. */
class TokenAuthenticator(private val session: SessionStore) : Authenticator {
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(session.connectTimeoutSeconds.toLong(), TimeUnit.SECONDS)
        .readTimeout(session.readTimeoutSeconds.toLong(), TimeUnit.SECONDS)
        .writeTimeout(session.readTimeoutSeconds.toLong(), TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? = synchronized(refreshLock) {
        authenticateLocked(response)
    }

    private fun authenticateLocked(response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val failedPath = response.request.url.encodedPath
        if (failedPath.endsWith("/auth/login") || failedPath.endsWith("/auth/register") || failedPath.endsWith("/auth/refresh")) return null
        val previousAuthorization = response.request.header("Authorization")
        val currentAccess = session.accessToken
        if (currentAccess.isNotBlank() && previousAuthorization != "Bearer $currentAccess") {
            return response.request.newBuilder().header("Authorization", "Bearer $currentAccess").build()
        }
        val refreshToken = session.refreshToken
        if (refreshToken.isBlank()) return null
        val url = session.apiRoot().trimEnd('/') + "/auth/refresh"
        val payload = "{\"refresh_token\":\"${escapeJson(refreshToken)}\"}"
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "application/json")
            .build()
        return try {
            refreshClient.newCall(request).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) return null
                val raw = refreshResponse.body?.string().orEmpty()
                val json = JsonParser.parseString(raw).asJsonObject
                val access = json.get("access_token")?.asString.orEmpty()
                val rotatedRefresh = json.get("refresh_token")?.asString.orEmpty()
                if (access.isBlank()) return null
                session.accessToken = access
                if (rotatedRefresh.isNotBlank()) session.refreshToken = rotatedRefresh
                response.request.newBuilder().header("Authorization", "Bearer $access").build()
            }
        } catch (_: Exception) { null }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }

    private fun escapeJson(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    companion object { private val refreshLock = Any() }
}
