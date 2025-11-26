package com.example.myapplication.feature.chat.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.myapplication.feature.chat.model.ChatMessage
import com.example.myapplication.feature.chat.model.ConversationEntity
import com.example.myapplication.feature.chat.model.ConversationWithMessages

@Dao
interface ConversationDao {

    @Transaction
    @Query("SELECT * FROM conversations ORDER BY creationTimestamp DESC")
    suspend fun getConversationsWithMessages(): List<ConversationWithMessages>

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationWithMessagesById(conversationId: String): ConversationWithMessages?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)
}
