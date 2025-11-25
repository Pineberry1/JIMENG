package com.example.myapplication.feature.chat.model

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface DashScopeApiService {

    @Streaming
    @POST("compatible-mode/v1/chat/completions")
    suspend fun generateTxt(
        @Header("Authorization") authorization: String,
        @Body request: TextGenerationRequest
    ): ResponseBody

    @POST("api/v1/services/aigc/multimodal-generation/generation")
    suspend fun generateImage(
        @Header("Authorization") authorization: String,
        @Body request: QwenImageRequest
    ): ResponseBody
}