package com.example.myapplication.feature.chat.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val modelName: String,
    val title: String,
    val creationTimestamp: Long = System.currentTimeMillis()
)
