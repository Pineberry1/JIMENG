package com.example.myapplication.feature.chat.model

import android.net.Uri

data class ChatState(
    val conversations: List<Conversation> = emptyList(),
    val activeConversationId: String? = null,
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val imageGenParams: ImageGenParams = ImageGenParams(),
    val availableModels: List<String> = listOf("qwen-plus", "qwen-vl-plus", "qwen-image-plus"),
    val enableSearch: Boolean = false,
    val enableThinking: Boolean = false,
    val selectedImageUri: List<Uri> = emptyList(),
    val uploadedImageUrl: List<String> = emptyList(),
    val isUploadingImage: Boolean = false
) {
    fun getActiveConversation(): Conversation? {
        return conversations.find { it.id == activeConversationId }
    }
}
