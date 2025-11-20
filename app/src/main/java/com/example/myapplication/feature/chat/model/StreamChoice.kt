package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName

data class StreamChoice(
    @SerializedName("delta")
    val delta: StreamDelta,
    @SerializedName("index")
    val index: Int,
    @SerializedName("finish_reason")
    val finishReason: String?
)
