package com.example.myapplication.feature.chat.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.myapplication.feature.chat.model.ChatMessage
import com.example.myapplication.feature.chat.model.ConversationEntity
import com.example.myapplication.feature.chat.model.ConversationWithMessages
import com.example.myapplication.feature.chat.model.uploadImageIndex
import kotlinx.coroutines.flow.Flow
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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageIndex(index: uploadImageIndex)

    @Query("SELECT * FROM image_upload_index WHERE localUri = :localUri")
    suspend fun getImageIndexByLocalUri(localUri: String): uploadImageIndex?

    @Query("SELECT * FROM image_upload_index")
    fun getAllImageIndexes(): Flow<List<uploadImageIndex>> // 或者 suspend fun ...(): List<...>

    @Query("DELETE FROM image_upload_index WHERE creationTimestamp < :expirationTime")
    suspend fun deleteExpiredIndexes(expirationTime: Long)
}
