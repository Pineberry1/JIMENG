package com.example.myapplication.feature.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.feature.chat.model.ChatMessage
import com.example.myapplication.feature.chat.model.ChatState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _sideEffect = MutableSharedFlow<ChatSideEffect>()
    val sideEffect: SharedFlow<ChatSideEffect> = _sideEffect.asSharedFlow()

    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInputText -> updateInputText(intent.text)
            is ChatIntent.SendMessage -> sendMessage(intent.text)
            ChatIntent.ClearInput -> clearInput()
        }
    }

    private fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    private fun clearInput() {
        _state.update { it.copy(inputText = "") }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                // 添加用户消息
                val userMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = text,
                    isUser = true,
                    timestamp = "刚刚"
                )

                _state.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + userMessage,
                        inputText = "",
                        isLoading = true
                    )
                }

                // 模拟 AI 回复
                simulateAIResponse()

            } catch (e: Exception) {
                _sideEffect.emit(ChatSideEffect.ShowError("发送失败: ${e.message}"))
                _state.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages.dropLast(1),
                        inputText = text,
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun simulateAIResponse() {
        kotlinx.coroutines.delay(1000)

        val aiMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = "这是 AI 的回复",
            isUser = false,
            timestamp = "刚刚"
        )

        _state.update { currentState ->
            currentState.copy(
                messages = currentState.messages + aiMessage,
                isLoading = false
            )
        }
    }
}