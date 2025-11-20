package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName

data class StreamUsage(
    @SerializedName("output_tokens")
    val outputTokens: Int,
    @SerializedName("input_tokens")
    val inputTokens: Int
)
