package com.example.myapplication.feature.chat.model

import java.util.UUID

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val modelName: String,
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val creationTimestamp: Long = System.currentTimeMillis()
)
