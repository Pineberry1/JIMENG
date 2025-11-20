package com.example.myapplication.feature.chat.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.feature.settings.model.ModelSettingsRepository

class ChatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val repository = ModelSettingsRepository(application)
            val chatWithHttp = ChatWithHttp("sk-b2f10e1cfec74edebbac23137ffa9815")
            // Pass the application context to the ViewModel
            return ChatViewModel(application, chatWithHttp, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
