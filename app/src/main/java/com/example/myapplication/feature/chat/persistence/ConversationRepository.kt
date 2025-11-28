package com.example.myapplication.feature.chat.persistence

import com.example.myapplication.feature.chat.model.ChatMessage
import com.example.myapplication.feature.chat.model.ConversationEntity
import com.example.myapplication.feature.chat.model.ConversationWithMessages
import com.example.myapplication.feature.chat.model.uploadImageIndex
import kotlinx.coroutines.flow.first
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
    suspend fun insertImageIndex(index: uploadImageIndex) {
        conversationDao.insertImageIndex(index)
    }

    suspend fun getAllImageIndexes(): List<uploadImageIndex> {
        // 使用 runBlocking 或其他方式，这里假设一个 suspend fun 的 DAO
        // 如果 DAO 返回 Flow，则需要用 .first()
        // 假设DAO是 suspend fun getAllImageIndexes(): List<ImageUploadIndex>
        return conversationDao.getAllImageIndexes().first() // 假设从Flow获取一次性列表
    }

    suspend fun deleteExpiredIndexes(expirationTime: Long) {
        conversationDao.deleteExpiredIndexes(expirationTime)
    }
}
