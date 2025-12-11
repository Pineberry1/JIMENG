package com.example.myapplication.feature.chat.viewmodel

import android.util.Log
import androidx.compose.ui.semantics.text
import com.example.myapplication.feature.chat.model.ChatCompletionChunk
import com.example.myapplication.feature.chat.model.ImageGenParams
import com.example.myapplication.feature.chat.model.Message
import com.example.myapplication.feature.chat.model.QwenImageRequest
import com.example.myapplication.feature.chat.model.QwenInput
import com.example.myapplication.feature.chat.model.QwenMessage
import com.example.myapplication.feature.chat.model.QwenContent
import com.example.myapplication.feature.chat.model.QwenParameters
import com.example.myapplication.feature.chat.model.RetrofitClient
import com.example.myapplication.feature.chat.model.StreamChoice
import com.example.myapplication.feature.chat.model.TextGenerationRequest
import com.example.myapplication.feature.chat.model.stream_option
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import com.example.myapplication.feature.chat.model.StreamDelta
private val UPDATE_INTERVAL_MS = 50L
class ChatWithHttp(private val apiKey: String) {
    private val apiService = RetrofitClient.dashScopeService
    private val gson = Gson()

    suspend fun generateStream(
        modelName: String,
        conversationHistory: List<Message>,
        topP: Double?,
        temperature: Double?,
        enable_search: Boolean,
        enable_thinking: Boolean
    ): Flow<String> {
        // 1. 准备请求参数
        val request = createGenerationRequest(
            modelName,
            conversationHistory,
            true,
            enable_search,
            enable_thinking
        )

        // 2. 调用通用函数
        return executeStreamCall(request) {
            // 传入具体的 API 调用逻辑
            apiService.generateTxt("Bearer $apiKey", request)
        }
    }
    private fun <T> executeStreamCall(
        requestBody: T,
        apiCall: suspend () -> ResponseBody
    ): Flow<String> = flow {
        // 1. 统一打印请求日志
        val requestJson = gson.toJson(requestBody)
        Log.d("ChatWithHttp", "Request Body: $requestJson")
        // 如果 T 的 toString() 有意义，也可以打印，但在 JSON 存在的情况下通常不需要
        // Log.d("ChatWithHttp", "API Request object: $requestBody")
        val contentBuffer = StringBuilder()
        val reasonBuffer = StringBuilder()
        var lastEmitTime = System.currentTimeMillis()
        // 2. 执行传入的 API 调用
        val responseBody = apiCall()

        // 3. 统一的流处理逻辑 (原封不动移过来)
        responseBody.byteStream().bufferedReader().useLines { lines ->
            lines.forEach { line ->
                Log.d("ChatWithHttp", "Stream Raw Line: $line")
                val trimmedLine = line.trim()
                if (trimmedLine.startsWith("data:")) {
                    val json = trimmedLine.substring(5).trim()
                    if (json != "[DONE]") {
                        try {
                            // 2. 解析原始 Chunk
                            val chunk = gson.fromJson(json, ChatCompletionChunk::class.java)
                            val delta = chunk.choices.firstOrNull()?.delta

                            if (delta != null) {
                                // 3. 累加到缓冲区 (注意处理 null)
                                if (!delta.content.isNullOrEmpty()) {
                                    contentBuffer.append(delta.content)
                                }
                                if (!delta.reasoning_content.isNullOrEmpty()) {
                                    reasonBuffer.append(delta.reasoning_content)
                                }

                                // 4. 检查时间：如果缓冲区有货 且 距离上次发射超过 50ms
                                val currentTime = System.currentTimeMillis()
                                val hasData = contentBuffer.isNotEmpty() || reasonBuffer.isNotEmpty()

                                if (hasData && (currentTime - lastEmitTime >= UPDATE_INTERVAL_MS)) {
                                    val accumulatedDelta = StreamDelta(
                                        content = contentBuffer.toString(),
                                        reasoning_content = reasonBuffer.toString(),
                                        role = "assistant"
                                    )

                                    // 序列化成 JSON 字符串并发射
                                    emit(gson.toJson(accumulatedDelta))
                                    // --- 核心改动结束 ---

                                    // 5. 清空缓冲区 & 重置计时器
                                    contentBuffer.setLength(0) // 高效清空
                                    reasonBuffer.setLength(0)
                                    lastEmitTime = currentTime
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ChatWithHttp", "Error parsing chunk", e)
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun generateStreamWithImage(
        modelName: String,
        conversationHistory: List<Message>,
        topP: Double?,
        temperature: Double?,
        enable_thinking: Boolean
    ): Flow<String> {
        // 1. 准备请求参数
        val request = createGenerationRequestForImage(
            modelName,
            conversationHistory,
            true,
            enable_thinking
        )

        // 2. 调用通用函数
        return executeStreamCall(request) {
            // 传入具体的带图片的 API 调用逻辑
            apiService.generateTxtWithImage("Bearer $apiKey", request = request)
        }
    }

    suspend fun generateSummary(previousSummary: String, recentMessages: List<Message>): String {
        // 1. 定义总结任务的Prompt模板
        val promptTemplate = """
# Role
你是一个专业的对话记录员。你的任务是将一段“最近发生的对话”转化为精炼的“日志段落”。

# Goal
生成一段纯文本摘要，概括用户(User)和助手(Assistant)在这几轮对话中的交互重点。这段摘要将被追加到长期的历史记录文件中，因此必须简洁、客观，且不包含重复的客套话。

# Rules
1. **格式**：直接输出段落文本，不要使用列表、不要Markdown标题、不要JSON。
2. **视角**：使用第三人称（例如：“用户询问了...”，“助手建议...”）。
3. **内容**：
    - 捕捉核心意图：用户想要解决什么新问题？
    - 记录关键细节：代码变量名、特定参数、具体的报错信息或达成的一致结论。
    - 忽略闲聊：删除问候语（如“你好”、“谢谢”）。
4. **长度**：根据内容来定150字到300 字。
5. **语境衔接**：假设这段文字是接在之前的记录后面的，不需要写“在本次对话中”这样的开场白，直接叙述发生的事件。

# Input Conversation (summary+Recent 5 Turns)
{{之前的摘要}}
{{最近五轮对话内容}}

# Output
(请直接开始生成关于这五轮的摘要段落)
    """.trimIndent()

        // 2. [已修复] 将最近对话格式化为纯文本字符串，安全地处理 content 字段
        val recentConversationText = recentMessages.joinToString("\n") { message ->
            // 从 Any 类型的 content 中安全地提取文本
            val textContent = when (val content = message.content) {
                is String -> content
                is List<*> -> content.filterIsInstance<com.example.myapplication.feature.chat.model.TextPart>()
                    .joinToString(separator = " ") { it.text }
                else -> ""
            }
            // 使用 message.role 来判断角色，更符合 Message 类的结构
            "${if (message.role == "user") "User" else "Assistant"}: $textContent"
        }

        // 3. 组合成最终的输入内容
        val finalInputText = promptTemplate
            .replace("{{之前的摘要}}", if (previousSummary.isNotBlank()) previousSummary else "无")
            .replace("{{最近五轮对话内容}}", recentConversationText)

        // 4. 将整个Prompt作为一条 "user" 消息发送给模型
        val summaryRequestMessages = listOf(
            Message(role = "user", content = finalInputText)
        )

        // 5. [已优化] 复用 createGenerationRequest 函数来构建请求
        val request = createGenerationRequest(
            model = "qwen-plus", // 使用指定的模型
            messages = summaryRequestMessages,
            stream = false,      // 摘要生成是非流式的
            enable_search = false, // 摘要不需要搜索
            enable_thinking = false
        )

        val requestJson = gson.toJson(request)
        Log.d("ChatWithHttp", "Summary Request Body: $requestJson")

        try {
            // 6. 调用一个非流式的API端点来获取一次性响应
            val response = apiService.generateTxtOnce("Bearer $apiKey", request)

            val responseBodyString = response.body()?.string()
            Log.d("ChatWithHttp", "Summary Response Body: $responseBodyString")

            if (response.isSuccessful && !responseBodyString.isNullOrBlank()) {
                // 7. 解析JSON并提取文本
                // 确保你已经创建了 TextGenerationResponse 和 TextPart 数据类
                val summaryResponse = gson.fromJson(responseBodyString, com.example.myapplication.feature.chat.model.ChatResponseBody::class.java)
                // 返回大模型生成的摘要文本，并去除可能存在的前后多余空格
                return summaryResponse.choices.firstOrNull()?.message?.content?.trim() ?: ""
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatWithHttp", "Summary generation failed: ${response.code()} - $errorBody")
                return "" // 返回空字符串表示失败
            }
        } catch (e: Exception) {
            Log.e("ChatWithHttp", "Exception during summary generation", e)
            return "" // 异常情况下也返回空字符串
        }
    }
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
        enable_search: Boolean,
        enable_thinking: Boolean
    ): TextGenerationRequest {
        return TextGenerationRequest(
            model = model,
            messages = messages,
            stream = stream,
            stream_option = if(stream) stream_option(include_usage = true) else null,
            enable_thinking = enable_thinking,
            enable_search = enable_search
        )
    }

    private fun createGenerationRequestForImage(
        model: String,
        messages: List<Message>,
        stream: Boolean,
        enable_thinking: Boolean
    ): TextGenerationRequest {
        return TextGenerationRequest(
            model = model,
            messages = messages,
            stream = stream,
            stream_option = stream_option(include_usage = true),
            enable_thinking = enable_thinking
        )
    }
}
