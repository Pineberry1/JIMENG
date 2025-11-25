package com.example.myapplication.feature.chat.model

data class
ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: String,
    val imageUrl: String? = null
)
