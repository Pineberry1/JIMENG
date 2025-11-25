package com.example.myapplication.feature.chat.model

data class ChatState(
    val conversations: List<Conversation> = emptyList(),
    val activeConversationId: String? = null,
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val imageGenParams: ImageGenParams = ImageGenParams(),
    val availableModels: List<String> = listOf("qwen-plus", "qwen-image-plus")
) {
    fun getActiveConversation(): Conversation? {
        return conversations.find { it.id == activeConversationId }
    }
}
