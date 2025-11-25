package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName

// Root of the response
data class QwenImageResponse(
    val output: QwenOutput,
    @SerializedName("request_id")
    val requestId: String
)

// Contains the list of choices
data class QwenOutput(
    val choices: List<QwenChoice>
)

// A single choice, containing the message
data class QwenChoice(
    @SerializedName("finish_reason")
    val finishReason: String,
    val message: QwenResponseMessage
)

// The message from the assistant
data class QwenResponseMessage(
    val role: String,
    val content: List<QwenResponseContent>
)

// The actual content, which is the image URL
data class QwenResponseContent(
    val image: String
)
