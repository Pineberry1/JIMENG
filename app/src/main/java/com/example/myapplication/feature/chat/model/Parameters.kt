package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName

data class Parameters(
    @SerializedName("top_p") val topP: Double?,
    @SerializedName("temperature")val temperature: Double?,

    @SerializedName("result_format") val resultFormat: String = "text",
    @SerializedName("enable_thinking") val enableThinking: Boolean = false,
    @SerializedName("enable_search") val enableSearch: Boolean = false
)
