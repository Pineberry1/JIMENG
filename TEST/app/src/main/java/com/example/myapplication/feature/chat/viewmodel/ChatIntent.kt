package com.example.myapplication.feature.chat.viewmodel

sealed class ChatIntent {
    data class SendMessage(val text: String) : ChatIntent()
    data class UpdateInputText(val text: String) : ChatIntent()
    data class SelectModel(val modelName: String) : ChatIntent()
    data class StartDebugSequence(val prompts: List<String>) : ChatIntent() // Now it carries the case
    object ClearInput : ChatIntent()
}
