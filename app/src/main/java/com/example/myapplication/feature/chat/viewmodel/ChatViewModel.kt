package com.example.myapplication.feature.chat.viewmodel

import android.app.Application
import android.os.Debug
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.BuildConfig
import com.example.myapplication.feature.chat.model.ChatMessage
import com.example.myapplication.feature.chat.model.ChatState
import com.example.myapplication.feature.chat.model.Conversation
import com.example.myapplication.feature.chat.model.ConversationEntity
import com.example.myapplication.feature.chat.model.ConversationWithMessages
import com.example.myapplication.feature.chat.model.ImageGenParams
import com.example.myapplication.feature.chat.model.Message
import com.example.myapplication.feature.chat.model.StreamDelta
import com.example.myapplication.feature.chat.persistence.ConversationRepository
import com.example.myapplication.feature.chat.util.ImageSaver
import com.example.myapplication.feature.settings.model.ModelSettingsRepository
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel(
    private val application: Application,
    private val chatWithHttp: ChatWithHttp,
    private val settingsRepository: ModelSettingsRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    private val gson = Gson()
    private val _sideEffect = MutableSharedFlow<ChatSideEffect>()
    val sideEffect: SharedFlow<ChatSideEffect> = _sideEffect.asSharedFlow()
    private var tempMessageIdCounter = -1L

    init {
        loadConversations()
        if (BuildConfig.DEBUG && Debug.isDebuggerConnected()) {
            processIntent(ChatIntent.StartDebugSequence(DebugCases.case3, DebugCases.modelName))
        }
    }

    private fun loadConversations() {
        viewModelScope.launch {
            val conversationsFromDb = conversationRepository.getConversationsWithMessages()
            val conversations = conversationsFromDb.map { it.toConversation() }
            _state.update {
                it.copy(
                    conversations = conversations,
                    activeConversationId = conversations.firstOrNull()?.id
                )
            }
        }
    }

    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInputText -> updateInputText(intent.text)
            is ChatIntent.SendMessage -> sendMessage(intent.text, intent.modelName, intent.isNewConversation)
            is ChatIntent.SelectModel -> viewModelScope.launch { selectModel(intent.modelName) }
            is ChatIntent.SwitchConversation -> switchConversation(intent.conversationId)
            is ChatIntent.UpdateImageGenParams -> updateImageGenParams(intent.params)
            is ChatIntent.SaveImageToGallery -> saveImageToGallery(intent.imageUrl)
            is ChatIntent.SetEnableSearch -> _state.update { it.copy(enableSearch = intent.enabled) }
            is ChatIntent.SetEnableThinking -> _state.update { it.copy(enableThinking = intent.enabled) }
            is ChatIntent.StartDebugSequence -> startDebugSequence(DebugCases.case2, DebugCases.modelName)
            ChatIntent.ClearInput -> clearInput()
            is ChatIntent.DeleteConversation -> {
                viewModelScope.launch {
                    conversationRepository.deleteConversation(intent.conversationId)
                    // Also remove from UI state
                    _state.update {
                        val conversations = it.conversations.filter { conv -> conv.id != intent.conversationId }
                        it.copy(
                            conversations = conversations,
                            activeConversationId = if(it.activeConversationId == intent.conversationId) conversations.firstOrNull()?.id else it.activeConversationId
                        )
                    }
                }
            }
        }
    }

    private fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    private fun clearInput() {
        _state.update { it.copy(inputText = "") }
    }

    private suspend fun selectModel(modelName: String, title: String = "New Chat") {
        val newConversationEntity = ConversationEntity(modelName = modelName, title = title)
        conversationRepository.insertConversation(newConversationEntity)
        val newConversation = newConversationEntity.toConversation()
        _state.update {
            it.copy(
                conversations = listOf(newConversation) + it.conversations,
                activeConversationId = newConversation.id
            )
        }
    }

    private fun switchConversation(conversationId: String) {
        _state.update { it.copy(activeConversationId = conversationId) }
    }

    private fun updateImageGenParams(params: ImageGenParams) {
        _state.update { it.copy(imageGenParams = params) }
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun sendMessage(text: String, modelName: String, isNewConversation: Boolean) {
        if (text.isBlank()) return
        clearInput()

        viewModelScope.launch {
            if (isNewConversation || _state.value.activeConversationId == null) {
                selectModel(modelName)
            }
            executeConversationTurn(text)
        }
    }

    private fun startDebugSequence(prompts: List<String>, modelName: String = "qwen-plus") {
        viewModelScope.launch {
            Log.d("ChatViewModel", "start debug")
            selectModel(modelName, "Debug Test")
            for (prompt in prompts) {
                executeConversationTurn(prompt)
                delay(2000)
            }
        }
    }

    private suspend fun executeConversationTurn(text: String) {

        val activeConversationId = _state.value.activeConversationId ?: return
        val userMessage = ChatMessage(id = tempMessageIdCounter--, conversationId = activeConversationId, text = text, isUser = true, timestamp = getCurrentTimestamp())
        updateConversationMessages(activeConversationId, _state.value.getActiveConversation()!!.messages + userMessage)

        val aiMessage = ChatMessage(id = tempMessageIdCounter--, conversationId = activeConversationId, text = "", isUser = false, timestamp = getCurrentTimestamp())
        updateConversationMessages(activeConversationId, _state.value.getActiveConversation()!!.messages + aiMessage)

        _state.update { it.copy(isLoading = true) }
        try {
            val currentConversation = _state.value.getActiveConversation()!!

            when (currentConversation.modelName) {
                "qwen-image-plus" -> executeImageGeneration(currentConversation)
                else -> executeTextGeneration(currentConversation)
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Text generation failed", e)
            _sideEffect.emit(ChatSideEffect.ShowError("请求失败: ${e.message}"))
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun executeTextGeneration(conversation: Conversation) {
        val history = conversation.messages
            .filter { it.text.isNotBlank() }
            .map { Message(role = if (it.isUser) "user" else "assistant", content = it.text) }

        Log.d("ChatViewModel", "prepare text generation request...")
        Log.d("ChatViewModel", "modelName: ${conversation.modelName}")
        Log.d("ChatViewModel", "history: ${Gson().toJson(history)}")


        Log.d("ChatViewModel", "Starting to collect from generateStream")
        chatWithHttp.generateStream(
            conversation.modelName,
            history,
            0.8,
            0.8,
            _state.value.enableSearch,
            _state.value.enableThinking
        ).collect { newChunk ->
            Log.d("ChatViewModel", "Received new chunk")
            val streamDelta = gson.fromJson(newChunk, StreamDelta::class.java)
            val newText = streamDelta.content?.takeIf { it != "null" } ?: ""
            val newReason = streamDelta.reasoning_content?.takeIf { it != "null" } ?: ""
            Log.d("ChatViewModel", "Collected new text chunk: '$newReason'")

            updateLastMessage(conversation.id) {
                it.copy(
                    text = it.text + newText,
                    reason_text = it.reason_text + newReason
                )
            }
        }
        Log.d("ChatViewModel", "Finished collecting from generateStream")

        // Insert messages after stream is complete
        val finalConversation = _state.value.getActiveConversation()
        if (finalConversation != null && finalConversation.messages.size >= 2) {
            val userMessage = finalConversation.messages[finalConversation.messages.size - 2]
            val aiMessage = finalConversation.messages.last()
            viewModelScope.launch {
                conversationRepository.insertMessage(userMessage.copy(id = 0))
                conversationRepository.insertMessage(aiMessage.copy(id = 0))
            }
        }
    }

    private suspend fun executeImageGeneration(conversation: Conversation) {
        val userMessage = conversation.messages.lastOrNull { it.isUser } ?: return
        val responseJson = chatWithHttp.generateImage(_state.value.imageGenParams, userMessage.text)
        val response = Gson().fromJson(responseJson, com.example.myapplication.feature.chat.model.QwenImageResponse::class.java)
        val imageUrl = response.output.choices.firstOrNull()?.message?.content?.firstOrNull()?.image

        if (imageUrl != null) {
            updateLastMessage(conversation.id) { it.copy(imageUrl = imageUrl) }
        } else {
            updateLastMessage(conversation.id) { it.copy(text = "图片生成失败，请重试。") }
        }

        // Insert messages after image generation is complete
        val finalConversation = _state.value.getActiveConversation()
        if (finalConversation != null && finalConversation.messages.size >= 2) {
            val userMessageToSave = finalConversation.messages[finalConversation.messages.size - 2]
            val aiMessageToSave = finalConversation.messages.last()
            viewModelScope.launch {
                conversationRepository.insertMessage(userMessageToSave.copy(id = 0))
                conversationRepository.insertMessage(aiMessageToSave.copy(id = 0))
            }
        }
    }

    private fun updateConversationMessages(conversationId: String, messages: List<ChatMessage>) {
        _state.update { currentState ->
            val updatedConversations = currentState.conversations.map {
                if (it.id == conversationId) it.copy(messages = messages) else it
            }
            currentState.copy(conversations = updatedConversations)
        }
    }

    private fun updateLastMessage(conversationId: String, update: (ChatMessage) -> ChatMessage) {
        _state.update { currentState ->
            val updatedConversations = currentState.conversations.map {
                if (it.id == conversationId) {
                    if (it.messages.isEmpty()) {
                        it
                    } else {
                        val updatedMessages = it.messages.dropLast(1) + update(it.messages.last())
                        it.copy(messages = updatedMessages)
                    }
                } else {
                    it
                }
            }
            currentState.copy(conversations = updatedConversations)
        }
    }

    private fun saveImageToGallery(imageUrl: String) {
        ImageSaver.save(application, imageUrl) { success, message ->
            viewModelScope.launch {
                if (success) {
                    _sideEffect.emit(ChatSideEffect.ShowSuccess(message))
                } else {
                    _sideEffect.emit(ChatSideEffect.ShowError(message))
                }
            }
        }
    }

    private fun ConversationWithMessages.toConversation(): Conversation {
        return Conversation(
            id = this.conversation.id,
            modelName = this.conversation.modelName,
            title = this.conversation.title,
            messages = this.messages,
            creationTimestamp = this.conversation.creationTimestamp
        )
    }

    private fun ConversationEntity.toConversation(): Conversation {
        return Conversation(
            id = this.id,
            modelName = this.modelName,
            title = this.title,
            messages = emptyList(),
            creationTimestamp = this.creationTimestamp
        )
    }
}
