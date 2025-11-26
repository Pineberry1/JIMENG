package com.example.myapplication.feature.chat.viewmodel

import com.example.myapplication.feature.chat.model.ImageGenParams

sealed class ChatIntent {
    data class UpdateInputText(val text: String) : ChatIntent()
    data class SendMessage(val text: String, val modelName: String, val isNewConversation: Boolean) : ChatIntent()
    data class SelectModel(val modelName: String) : ChatIntent()
    data class SwitchConversation(val conversationId: String) : ChatIntent()
    data class UpdateImageGenParams(val params: ImageGenParams) : ChatIntent()
    data class SaveImageToGallery(val imageUrl: String) : ChatIntent()
    data class StartDebugSequence(val prompts: List<String>, val modelName: String) : ChatIntent()
    data class SetEnableSearch(val enabled: Boolean) : ChatIntent()
    data class SetEnableThinking(val enabled: Boolean) : ChatIntent()
    data class DeleteConversation(val conversationId: String) : ChatIntent()
    object ClearInput : ChatIntent()
}
