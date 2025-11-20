package com.example.myapplication.feature.chat.viewmodel

import android.app.Application
import android.os.Debug
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.BuildConfig
import com.example.myapplication.feature.chat.model.ChatMessage
import com.example.myapplication.feature.chat.model.ChatState
import com.example.myapplication.feature.chat.model.Message
import com.example.myapplication.feature.settings.model.ModelSettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatViewModel(
    private val application: Application,
    private val chatWithHttp: ChatWithHttp,
    private val settingsRepository: ModelSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _sideEffect = MutableSharedFlow<ChatSideEffect>()
    val sideEffect: SharedFlow<ChatSideEffect> = _sideEffect.asSharedFlow()

    init {
        if (BuildConfig.DEBUG && Debug.isDebuggerConnected()) {
            // --- Easy Switch for Debugging ---
            processIntent(ChatIntent.StartDebugSequence(DebugCases.case2))
            // -----------------------------------
        }
    }

    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInputText -> updateInputText(intent.text)
            is ChatIntent.SendMessage -> sendMessage(intent.text)
            is ChatIntent.SelectModel -> selectModel(intent.modelName)
            is ChatIntent.StartDebugSequence -> startDebugSequence(intent.prompts)
            ChatIntent.ClearInput -> clearInput()
        }
    }

    private fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    private fun clearInput() {
        _state.update { it.copy(inputText = "") }
    }

    private fun selectModel(modelName: String) {
        _state.update { it.copy(currentModelName = modelName) }
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return
        clearInput()
        viewModelScope.launch {
            executeConversationTurn(text)
        }
    }

    private fun startDebugSequence(prompts: List<String>) {
        viewModelScope.launch {
            for (prompt in prompts) {
                executeConversationTurn(prompt)
                delay(2000) // Increase delay to see conversations clearly
            }
        }
    }

    private suspend fun executeConversationTurn(text: String) {
        val currentModelName = _state.value.currentModelName
        val userMessage = ChatMessage(UUID.randomUUID().toString(), text, true, getCurrentTimestamp())
        val aiMessage = ChatMessage(UUID.randomUUID().toString(), "", false, getCurrentTimestamp())

        val conversationHistory = (_state.value.messages + userMessage).map {
            Message(role = if (it.isUser) "user" else "assistant", content = it.text)
        }.filter { it.content.isNotBlank() }

        Log.d("ChatViewModel", "Conversation History Sent:\n${conversationHistory.joinToString("\n") { " - ${it.role}: ${it.content}" }}")

        _state.update { it.copy(messages = it.messages + userMessage + aiMessage, isLoading = true) }

        val modelConfig = settingsRepository.getConfig(currentModelName)

        try {
            chatWithHttp.generateStream(currentModelName, conversationHistory, modelConfig?.topP, modelConfig?.temperature)
                .onCompletion { _state.update { it.copy(isLoading = false) } }
                .collect { newText ->
                    // --- PERFORMANCE OPTIMIZATION ---
                    // Instead of mapping the whole list, directly update the last message.
                    _state.update { currentState ->
                        val lastMessage = currentState.messages.last()
                        val updatedMessages = currentState.messages.dropLast(1) + lastMessage.copy(text = lastMessage.text + newText)
                        currentState.copy(messages = updatedMessages)
                    }
                    // --------------------------------
                }
        } catch (e: Exception) {
            _sideEffect.emit(ChatSideEffect.ShowError("流式响应失败: ${e.message}"))
            _state.update { it.copy(isLoading = false) }
        }
    }
}
