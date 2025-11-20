package com.example.myapplication.feature.chat.viewmodel

import android.util.Log
import com.example.myapplication.feature.chat.model.ChatCompletionChunk
import com.example.myapplication.feature.chat.model.GenerationRequest
import com.example.myapplication.feature.chat.model.GenerationResponse
import com.example.myapplication.feature.chat.model.Message
import com.example.myapplication.feature.chat.model.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ChatWithHttp(private val apiKey: String) {
    private val apiService = RetrofitClient.dashScopeService
    private val gson = Gson()

    suspend fun generateStream(modelName: String, conversationHistory: List<Message>, topP: Double?, temperature: Double?): Flow<String> = flow {
        val request = createGenerationRequest(modelName, conversationHistory, true, topP, temperature)

        val requestJson = gson.toJson(request)
        Log.d("ChatWithHttp", "Request Body: $requestJson")

        val responseBody = apiService.generateStream("Bearer $apiKey", request)

        responseBody.byteStream().bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                Log.d("ChatWithHttp", "Stream Raw Line: $line")
                if (line!!.startsWith("data:")) {
                    val json = line!!.substring(5).trim()
                    if (json == "[DONE]") {
                        break
                    }
                    try {
                        val chunk = gson.fromJson(json, ChatCompletionChunk::class.java)
                        chunk.choices.firstOrNull()?.delta?.content?.let {
                            emit(it)
                        }
                    } catch (e: Exception) {
                        Log.e("ChatWithHttp", "Error parsing JSON chunk: $json", e)
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO) // This ensures the flow runs on a background thread, and collection happens on the caller's thread (Main).

    private fun createGenerationRequest(
        model: String,
        messages: List<Message>,
        stream: Boolean,
        topP: Double?,
        temperature: Double?
    ): GenerationRequest {
        return GenerationRequest(
            model = model,
            messages = messages,
            stream = stream,
            topP = topP,
            temperature = temperature
        )
    }
}
