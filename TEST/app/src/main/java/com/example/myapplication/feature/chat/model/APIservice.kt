package com.example.myapplication.feature.chat.model

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface DashScopeApiService {

    @POST("api/v1/services/aigc/text-generation/generation")
    fun generate(
        @Header("Authorization") authorization: String,
        @Body request: GenerationRequest
    ): Call<GenerationResponse>

    // 如果需要异步调用，可以添加这个
    @POST("api/v1/services/aigc/text-generation/generation")
    suspend fun generateAsync(
        @Header("Authorization") authorization: String,
        @Body request: GenerationRequest
    ): GenerationResponse

    @Streaming
    @POST("compatible-mode/v1/chat/completions")
    suspend fun generateStream(
        @Header("Authorization") authorization: String,
        @Body request: GenerationRequest
    ): ResponseBody
}