package com.example.myapplication.feature.chat.model

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://dashscope.aliyuncs.com/"

    // Create a logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // IMPORTANT: Use HEADERS for streaming, BODY will buffer the whole response and break streaming.
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    // Configure OkHttpClient
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // Add the logger
        .connectTimeout(30, TimeUnit.SECONDS) // Standard connect timeout
        .readTimeout(300, TimeUnit.SECONDS) // Long read timeout for streaming
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Create Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient) // Use the custom OkHttpClient
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val dashScopeService: DashScopeApiService = retrofit.create(DashScopeApiService::class.java)
}
