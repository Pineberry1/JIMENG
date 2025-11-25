package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName

data class stream_option(
    @SerializedName("include_usage")
    val include_usage: Boolean
)
// This should be the final, correct structure, mirroring the working image generation call.
data class TextGenerationRequest(
    @SerializedName("model")
    val model: String,
/*    val input : Input,*/
    @SerializedName("messages")
    val messages: List<Message>,
    @SerializedName("stream")
    val stream: Boolean,
    @SerializedName("stream_options")
    val stream_option: stream_option,
/*    @SerializedName("parameters")
    val parameters: Parameters*/
)
