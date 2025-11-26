package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName

data class StreamDelta(
    @SerializedName("content")
    val content: String?,
    @SerializedName("reasoning_content")
    val reasoning_content: String?,
    @SerializedName("role")
    val role: String?
)
