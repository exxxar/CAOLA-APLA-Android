package ru.caola.apla.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** REST contract from the CAOLA/APLA technical specification, API v1. */
interface ApiService {
    // Authentication
    @POST("auth/register") suspend fun register(@Body body: JsonObject): Response<JsonObject>
    @POST("auth/login") suspend fun login(@Body body: JsonObject): Response<JsonObject>
    @POST("auth/refresh") suspend fun refresh(@Body body: JsonObject): Response<JsonObject>
    @POST("auth/logout") suspend fun logout(): Response<JsonObject>

    // Profile and onboarding
    @GET("profile") suspend fun getProfile(): Response<JsonObject>
    @PATCH("profile") suspend fun updateProfile(@Body body: JsonObject): Response<JsonObject>
    @GET("onboarding") suspend fun getOnboarding(): Response<JsonObject>
    @POST("onboarding/complete") suspend fun completeOnboarding(@Body body: JsonObject): Response<JsonObject>

    // Learning dashboard and CAOLA recommendations
    @GET("learning/dashboard") suspend fun getDashboard(): Response<JsonObject>
    @GET("learning/trajectory") suspend fun getTrajectory(): Response<JsonObject>
    @GET("learning/recommendation") suspend fun getRecommendation(): Response<JsonObject>
    @POST("learning/recommendation/{id}/decision")
    suspend fun sendRecommendationDecision(@Path("id") id: String, @Body body: JsonObject): Response<JsonObject>

    // Context permissions and aggregated events
    @POST("context/events") suspend fun sendContextEvents(@Header("Idempotency-Key") idempotencyKey: String, @Body events: JsonArray): Response<JsonObject>
    @GET("context/settings") suspend fun getContextSettings(): Response<JsonObject>
    @PATCH("context/settings") suspend fun updateContextSettings(@Body body: JsonObject): Response<JsonObject>

    // Lessons, answers and results
    @POST("lessons/start") suspend fun startLesson(@Header("Idempotency-Key") idempotencyKey: String, @Body body: JsonObject): Response<JsonObject>
    @GET("lessons/{id}") suspend fun getLesson(@Path("id") id: String): Response<JsonObject>
    @POST("lessons/{id}/answers")
    suspend fun submitAnswer(@Path("id") lessonId: String, @Header("Idempotency-Key") idempotencyKey: String, @Body body: JsonObject): Response<JsonObject>
    @POST("lessons/{id}/complete")
    suspend fun completeLesson(@Path("id") lessonId: String, @Header("Idempotency-Key") idempotencyKey: String, @Body body: JsonObject): Response<JsonObject>

    // Progress and spaced repetition
    @GET("progress/summary") suspend fun getProgressSummary(): Response<JsonObject>
    @GET("progress/history")
    suspend fun getProgressHistory(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<JsonObject>
    @GET("review/queue") suspend fun getReviewQueue(): Response<JsonObject>

    // Offline event synchronization. Idempotency-Key prevents duplicate processing.
    @POST("sync")
    suspend fun synchronize(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: JsonObject
    ): Response<JsonObject>
}
