package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName

data class ChatResponseBody(
    @SerializedName("choices")
    val choices: List<Choice>,
    @SerializedName("usage")
    val usage: StreamUsage?
)
