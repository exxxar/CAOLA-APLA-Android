package ru.caola.apla.data

import android.content.Context
import ru.caola.apla.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object ApiClient {
    fun create(context: Context, session: SessionStore = SessionStore(context)): ApiService {
        // Validate only the server origin. The API prefix (for example /api/v1)
        // is a legitimate Retrofit base path and must not be rejected by the
        // host/scheme security policy.
        val serverBase = session.apiBaseUrl.trim().trimEnd('/')
        require(NetworkEndpointPolicy.isAllowed(serverBase, BuildConfig.DEBUG)) {
            if (BuildConfig.DEBUG) {
                "Для тестирования по HTTP используйте localhost, 10.0.2.2 или локальный IP компьютера (10.x, 172.16–31.x, 192.168.x). Публичные адреса должны использовать HTTPS."
            } else {
                "Для релизной версии требуется HTTPS-адрес API"
            }
        }
        val root = session.apiRoot().trimEnd('/')
        val baseUrl = "$root/"
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("Accept", "application/json")
                .header("X-Request-ID", UUID.randomUUID().toString())
                .header("X-Client-Platform", "android")
                .header("X-App-Version", BuildConfig.VERSION_NAME)
            val token = session.accessToken
            val path = original.url.encodedPath
            val isPublicAuthCall = path.endsWith("/auth/login") || path.endsWith("/auth/register") || path.endsWith("/auth/refresh")
            if (!isPublicAuthCall && token.isNotBlank()) builder.header("Authorization", "Bearer $token")
            chain.proceed(builder.build())
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(TokenAuthenticator(session))
            .connectTimeout(session.connectTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(session.readTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(session.readTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .callTimeout((session.readTimeoutSeconds + session.connectTimeoutSeconds).toLong(), TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
