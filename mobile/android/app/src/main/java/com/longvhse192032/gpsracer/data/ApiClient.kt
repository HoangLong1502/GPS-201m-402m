package com.longvhse192032.gpsracer.data

import com.longvhse192032.gpsracer.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    @Volatile
    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder().apply {
            authToken?.let { header("Authorization", "Bearer $it") }
        }.build()
        chain.proceed(request)
    }

    private val httpClient = OkHttpClient.Builder()
        // Render free tier can take 30–60s to wake from sleep on first request.
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(ensureTrailingSlash(BuildConfig.API_BASE_URL))
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    val baseUrl: String get() = BuildConfig.API_BASE_URL.trimEnd('/')

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
