package com.example.myapplication.feature.chat.model

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentModelName: String = "qwen-plus",
    val availableModels: List<String> = listOf("qwen-plus", "qwen-image-plus")
)
