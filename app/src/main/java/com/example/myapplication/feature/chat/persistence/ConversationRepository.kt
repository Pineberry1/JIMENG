package com.example.myapplication.feature.chat.persistence

import com.example.myapplication.feature.chat.model.ChatMessage
import com.example.myapplication.feature.chat.model.ConversationEntity
import com.example.myapplication.feature.chat.model.ConversationWithMessages

class ConversationRepository(private val conversationDao: ConversationDao) {

    suspend fun getConversationsWithMessages(): List<ConversationWithMessages> {
        return conversationDao.getConversationsWithMessages()
    }

    suspend fun getConversationWithMessagesById(conversationId: String): ConversationWithMessages? {
        return conversationDao.getConversationWithMessagesById(conversationId)
    }

    suspend fun insertConversation(conversation: ConversationEntity) {
        conversationDao.insertConversation(conversation)
    }

    suspend fun insertMessage(message: ChatMessage) {
        conversationDao.insertMessage(message)
    }

    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteConversation(conversationId)
    }

    suspend fun createConversation(modelName: String, firstMessage: ChatMessage) {
        val title = firstMessage.text.take(10)
        val conversation = ConversationEntity(
            modelName = modelName,
            title = title
        )
        conversationDao.insertConversation(conversation)
        conversationDao.insertMessage(firstMessage.copy(conversationId = conversation.id))
    }
}
