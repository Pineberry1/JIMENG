package com.example.myapplication.feature.chat.model

import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Junction
data class ConversationWithMessages(
    @Embedded val conversation: ConversationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "conversationId" ,

    )
    val messages: List<ChatMessage>
)
