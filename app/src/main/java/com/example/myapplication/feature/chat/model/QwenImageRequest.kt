package com.example.myapplication.feature.chat.model

// Main request body for qwen-image-plus
data class QwenImageRequest(
    val model: String,
    val input: QwenInput,
    val parameters: QwenParameters
)

// Contains the message list
data class QwenInput(
    val messages: List<QwenMessage>
)

// Represents a single message in the list
data class QwenMessage(
    val role: String,
    val content: List<QwenContent>
)

// The actual content of the message
data class QwenContent(
    val text: String
)

// Parameters specific to the image generation model
data class QwenParameters(
    val negative_prompt: String,
    val prompt_extend: Boolean,
    val watermark: Boolean,
    val size: String // e.g., "1328*1328"
)
