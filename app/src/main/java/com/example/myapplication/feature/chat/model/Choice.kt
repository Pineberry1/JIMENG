package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName

data class Choice(
    @SerializedName("message")
    val message: StreamDelta,
    @SerializedName("index")
    val index: Int,
    @SerializedName("finish_reason")
    val finishReason: String?
)
