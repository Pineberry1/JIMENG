package com.example.myapplication.feature.chat.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.feature.settings.model.ModelSettingsRepository
import com.example.myapplication.BuildConfig
import com.example.myapplication.feature.chat.persistence.AppDatabase
import com.example.myapplication.feature.chat.persistence.ConversationRepository

class ChatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val settingsRepository = ModelSettingsRepository(application)
            val chatWithHttp = ChatWithHttp("sk-135ccd0346344aabaeefaa497b687340")
            
            // Create database and repository for conversations
            val database = AppDatabase.getDatabase(application)
            val conversationRepository = ConversationRepository(database.conversationDao())

            // Pass the application context and repositories to the ViewModel
            return ChatViewModel(application, chatWithHttp, settingsRepository, conversationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
