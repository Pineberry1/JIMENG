package com.example.myapplication.feature.chat.viewmodel

sealed class ChatIntent {
    data class SendMessage(val text: String) : ChatIntent()
    data class UpdateInputText(val text: String) : ChatIntent()
    object ClearInput : ChatIntent()
}