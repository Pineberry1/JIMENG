package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName

data class ChatCompletionChunk(
    @SerializedName("choices")
    val choices: List<StreamChoice>,
    @SerializedName("usage")
    val usage: StreamUsage?
)
