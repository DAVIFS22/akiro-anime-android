package com.akiro.anime.network

import com.akiro.anime.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AkiroApiClient {
    val isConfigured: Boolean get() = BuildConfig.AKIRO_API_BASE_URL.isNotBlank()

    private val authInterceptor = Interceptor { chain ->
        val token = AkiroSession.token
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
        }.build()
        chain.proceed(request)
    }

    val api: AkiroApi by lazy {
        require(isConfigured) { "AKIRO_API_BASE_URL não configurada" }
        val base = BuildConfig.AKIRO_API_BASE_URL.trimEnd('/') + "/"
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(authInterceptor).addInterceptor(logging).build()
        Retrofit.Builder().baseUrl(base).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(AkiroApi::class.java)
    }
}

object AkiroSession {
    @Volatile var token: String? = null
}
