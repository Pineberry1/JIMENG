package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName

// Correct data models based on the user-provided JSON sample

// This is the message structure the API expects
data class Message(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: Any // Can be String or List<ContentPart>
)

sealed class ContentPart

data class TextPart(
    @SerializedName("type")
    val type: String = "text",
    @SerializedName("text")
    val text: String
) : ContentPart()

data class ImageUrlPart(
    @SerializedName("type")
    val type: String = "image_url",
    @SerializedName("image_url")
    val imageUrl: ImageUrl
) : ContentPart()

data class ImageUrl(
    @SerializedName("url")
    val url: String
)

// This is the correct top-level request object
data class GenerationRequest(
    @SerializedName("model")
    val model: String,

    @SerializedName("messages")
    val messages: List<Message>, // messages is a top-level property

    @SerializedName("stream")
    val stream: Boolean = false,

    // Adding customizable parameters
    @SerializedName("top_p")
    val topP: Double? = null,

    @SerializedName("temperature")
    val temperature: Double? = null
)


// --- Response Models (These are likely unchanged) ---

data class GenerationResponse(
    @SerializedName("output")
    val output: Output?,

    @SerializedName("usage")
    val usage: Usage?,

    @SerializedName("request_id")
    val requestId: String?
)

data class Output(
    @SerializedName("text")
    val text: String?
)

data class Usage(
    @SerializedName("total_tokens")
    val totalTokens: Int?,
    @SerializedName("output_tokens")
    val outputTokens: Int?,
    @SerializedName("input_tokens")
    val inputTokens: Int?
)
