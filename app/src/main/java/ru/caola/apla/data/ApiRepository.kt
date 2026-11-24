package ru.caola.apla.data

import android.content.Context
import com.google.gson.JsonObject
import retrofit2.Response
import java.util.UUID
import com.google.gson.JsonArray

/** Thin repository used by screens/ViewModels; server remains authoritative for CAOLA/APLA. */
class ApiRepository(context: Context) {
    private val session = SessionStore(context)
    private val api = ApiClient.create(context, session)

    suspend fun register(email: String, password: String, displayName: String): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("email", email); addProperty("password", password); addProperty("display_name", displayName)
        }
        val response = api.register(body)
        if (response.isSuccessful) {
            val data = response.body()
            data?.get("access_token")?.takeIf { it.isJsonPrimitive }?.asString?.let { session.accessToken = it }
            data?.get("refresh_token")?.takeIf { it.isJsonPrimitive }?.asString?.let { session.refreshToken = it }
        }
        return response
    }

    suspend fun login(email: String, password: String): Response<JsonObject> {
        val body = JsonObject().apply { addProperty("email", email); addProperty("password", password) }
        val response = api.login(body)
        if (response.isSuccessful) {
            val data = response.body()
            data?.get("access_token")?.takeIf { it.isJsonPrimitive }?.asString?.let { session.accessToken = it }
            data?.get("refresh_token")?.takeIf { it.isJsonPrimitive }?.asString?.let { session.refreshToken = it }
        }
        return response
    }

    suspend fun refreshSession(): Response<JsonObject> {
        val refresh = session.refreshToken
        require(refresh.isNotBlank()) { "Refresh-токен не задан" }
        val response = api.refresh(JsonObject().apply { addProperty("refresh_token", refresh) })
        if (response.isSuccessful) {
            val data = response.body()
            data?.get("access_token")?.takeIf { it.isJsonPrimitive }?.asString?.let { session.accessToken = it }
            data?.get("refresh_token")?.takeIf { it.isJsonPrimitive }?.asString?.let { session.refreshToken = it }
        }
        return response
    }

    suspend fun logout(): Response<JsonObject> {
        val response = api.logout()
        if (response.isSuccessful) {
            session.accessToken = ""
            session.refreshToken = ""
        }
        return response
    }

    suspend fun dashboard() = api.getDashboard()
    suspend fun profile() = api.getProfile()
    suspend fun updateProfile(body: JsonObject) = api.updateProfile(body)
    suspend fun onboarding() = api.getOnboarding()
    suspend fun completeOnboarding(body: JsonObject) = api.completeOnboarding(body)
    suspend fun trajectory() = api.getTrajectory()
    suspend fun recommendation() = api.getRecommendation()
    suspend fun decideRecommendation(id: String, decision: String, postponeUntil: String? = null): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("decision", decision)
            if (postponeUntil != null) addProperty("postpone_until", postponeUntil)
        }
        return api.sendRecommendationDecision(id, body)
    }
    suspend fun contextSettings() = api.getContextSettings()
    suspend fun updateContextSettings(body: JsonObject) = api.updateContextSettings(body)
    suspend fun sendContextEvents(events: JsonArray) = api.sendContextEvents(UUID.randomUUID().toString(), events)
    suspend fun startLesson(body: JsonObject) = api.startLesson(body.get("request_id")?.asString ?: UUID.randomUUID().toString(), body)
    suspend fun lesson(id: String) = api.getLesson(id)
    suspend fun submitAnswer(lessonId: String, body: JsonObject) = api.submitAnswer(lessonId, body.get("event_id")?.asString ?: UUID.randomUUID().toString(), body)
    suspend fun completeLesson(lessonId: String, body: JsonObject) = api.completeLesson(lessonId, body.get("event_id")?.asString ?: UUID.randomUUID().toString(), body)
    suspend fun progressSummary() = api.getProgressSummary()
    suspend fun progressHistory(page: Int = 1, pageSize: Int = 20) = api.getProgressHistory(page, pageSize)
    suspend fun reviewQueue() = api.getReviewQueue()
    suspend fun synchronize(batchId: String, clientId: String, events: com.google.gson.JsonArray) = api.synchronize(
        batchId,
        JsonObject().apply { addProperty("batch_id", batchId); addProperty("client_id", clientId); add("events", events) }
    )
}
