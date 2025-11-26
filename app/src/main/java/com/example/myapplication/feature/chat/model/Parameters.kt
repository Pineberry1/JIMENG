package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName

data class Parameters(
    @SerializedName("enable_thinking") val enableThinking: Boolean = false,
    @SerializedName("enable_search") val enableSearch: Boolean = false
)
