package com.example.myapplication.feature.chat.viewmodel

import android.util.Log
import com.example.myapplication.feature.chat.model.ChatCompletionChunk
import com.example.myapplication.feature.chat.model.ImageGenParams
import com.example.myapplication.feature.chat.model.Input
import com.example.myapplication.feature.chat.model.Message
import com.example.myapplication.feature.chat.model.Parameters
import com.example.myapplication.feature.chat.model.QwenContent
import com.example.myapplication.feature.chat.model.QwenImageRequest
import com.example.myapplication.feature.chat.model.QwenInput
import com.example.myapplication.feature.chat.model.QwenMessage
import com.example.myapplication.feature.chat.model.QwenParameters
import com.example.myapplication.feature.chat.model.RetrofitClient
import com.example.myapplication.feature.chat.model.TextGenerationRequest
import com.example.myapplication.feature.chat.model.stream_option
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
        Log.d("ChatWithHttp", "API Request object: $request")

        val responseBody = apiService.generateTxt("Bearer $apiKey", request)

        responseBody.byteStream().bufferedReader().useLines { lines ->
            lines.forEach { line ->
                Log.d("ChatWithHttp", "Stream Raw Line: $line")
                val trimmedLine = line.trim()
                if (trimmedLine.startsWith("data:")) {
                    val json = trimmedLine.substring(5).trim()
                    if (json != "[DONE]") {
                        try {
                            // The streaming format for this API is different, it sends the whole object
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
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateImage(params: ImageGenParams, text: String): String {
        val request = QwenImageRequest(
            model = "qwen-image-plus",
            input = QwenInput(
                messages = listOf(
                    QwenMessage(
                        role = "user",
                        content = listOf(QwenContent(text = text))
                    )
                )
            ),
            parameters = QwenParameters(
                negative_prompt = params.negativePrompt,
                prompt_extend = params.promptExtend,
                watermark = params.watermark,
                size = "${params.width}*${params.height}"
            )
        )

        val requestJson = gson.toJson(request)
        Log.d("ChatWithHttp", "Image Request Body: $requestJson")

        val responseBody = apiService.generateImage("Bearer $apiKey", request)
        return responseBody.string()
    }

    private fun createGenerationRequest(
        model: String,
        messages: List<Message>,
        stream: Boolean,
        topP: Double?,
        temperature: Double?
    ): TextGenerationRequest {
        return TextGenerationRequest(
            model = model,
            messages = messages,
            stream = stream,
            stream_option = stream_option(include_usage = true),
/*            parameters = Parameters(
                topP = topP,
                temperature = temperature,
                resultFormat = "text" // Crucial for streaming according to docs
            )*/
        )
    }
}
