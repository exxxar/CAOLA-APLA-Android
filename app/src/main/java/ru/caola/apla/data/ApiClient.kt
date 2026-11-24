package ru.caola.apla.data

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object ApiClient {
    fun create(context: Context, session: SessionStore = SessionStore(context)): ApiService {
        val root = session.apiRoot().trimEnd('/')
        require(root.startsWith("https://")) { "Для Production требуется HTTPS-адрес API" }
        val baseUrl = "$root/"
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("Accept", "application/json")
                .header("X-Request-ID", UUID.randomUUID().toString())
                .header("X-Client-Platform", "android")
                .header("X-App-Version", "0.1.0")
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
