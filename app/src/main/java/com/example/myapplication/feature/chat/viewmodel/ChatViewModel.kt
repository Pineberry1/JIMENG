package com.example.myapplication.feature.chat.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Debug
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.BuildConfig
import com.example.myapplication.feature.chat.model.*
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
import com.example.myapplication.feature.chat.persistence.ImagePersistenceManager
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.concurrent.TimeUnit

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

    private val localUriToOssUrlMap = mutableMapOf<String, String>()


    init {
        loadConversations()
        if (BuildConfig.DEBUG && Debug.isDebuggerConnected()) {
            processIntent(ChatIntent.StartDebugSequence(DebugCases.case3, DebugCases.modelName))
        }
        viewModelScope.launch(Dispatchers.IO) {
            // 计算48小时前的毫秒时间戳
            val expirationTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(48)

            // 1. 从数据库删除过期的索引
            conversationRepository.deleteExpiredIndexes(expirationTime)

            // 2. 加载所有未过期的索到内存的Map中
            val validIndexes = conversationRepository.getAllImageIndexes()
            validIndexes.forEach { index ->
                localUriToOssUrlMap[index.localUri] = index.ossUrl
            }
            Log.d("ChatViewModel", "Loaded ${validIndexes.size} valid image indexes from database.")
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
            is ChatIntent.SelectImage -> uploadImage(intent.uri)
            is ChatIntent.ClearSelectedImage -> clearSelectedImage(intent.uri)
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
        _state.update { it.copy(inputText = "", selectedImageUri = emptyList(), uploadedImageUrl = emptyList()) }
    }

    private fun clearSelectedImage(uri: Uri?) {
        _state.update { currentState ->
            if (uri == null) {
                // 如果 uri 为 null，则清除所有图片（保留旧逻辑）
                currentState.copy(selectedImageUri = emptyList())
            } else {
                // 如果 uri 不为 null，则从列表中移除指定的 uri
                val updatedUris = currentState.selectedImageUri.filter { it != uri }
                currentState.copy(selectedImageUri = updatedUris)
            }
        }
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
        val imageUriToSend = _state.value.selectedImageUri
        if (text.isBlank() && imageUriToSend.isEmpty()) return

        clearInput() // 清除UI输入

        viewModelScope.launch {
            if (isNewConversation || _state.value.activeConversationId == null) {
                selectModel(modelName)
            }
            // 将本地的 Uri 转换成 String 列表传递下去
            val localImageUris = imageUriToSend.map { it.toString() }
            executeConversationTurn(text, localImageUris)
        }
    }

    private fun startDebugSequence(prompts: List<String>, modelName: String = "qwen-plus") {
        viewModelScope.launch {
            Log.d("ChatViewModel", "start debug")
            selectModel(modelName, "Debug Test")
            for (prompt in prompts) {
                executeConversationTurn(prompt, emptyList()) // Pass null for the image URL
                delay(2000)
            }
        }
    }

    private suspend fun executeConversationTurn(text: String, imageUrl: List<String>) { // Accept imageUrl
        val activeConversationId = _state.value.activeConversationId ?: return
        // Use the passed-in imageUrl
        val userMessage = ChatMessage(id = tempMessageIdCounter--, conversationId = activeConversationId, text = text, isUser = true, timestamp = getCurrentTimestamp(), imageUrl = imageUrl)
        updateConversationMessages(activeConversationId, _state.value.getActiveConversation()!!.messages + userMessage)

        val aiMessage = ChatMessage(id = tempMessageIdCounter--, conversationId = activeConversationId, text = "", isUser = false, timestamp = getCurrentTimestamp())
        updateConversationMessages(activeConversationId, _state.value.getActiveConversation()!!.messages + aiMessage)

        _state.update { it.copy(isLoading = true) } // Don't clear image URI/URL here anymore
        try {
            val currentConversation = _state.value.getActiveConversation()!!

            when (currentConversation.modelName) {
                "qwen-image-plus" -> executeImageGeneration(currentConversation)
                "qwen-vl-plus" -> executeTextGeneration(currentConversation)
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
        // 1. 我们不再需要预处理 conversation.messages。直接使用它。

        // 2. 初始化一个图片计数器，但在 history 构建循环的外部。
        val imageLimit = 3

        // --- 1. 倒序收集最近的、数量正确的图片URL ---
        val recentImageUrlsToInclude = mutableListOf<String>()
        for (msg in conversation.messages.asReversed()) {
            if (recentImageUrlsToInclude.size >= imageLimit) {
                break // 如果已经收集够了，立即停止遍历更早的消息
            }
            if (!msg.imageUrl.isNullOrEmpty()) {
                val needed = imageLimit - recentImageUrlsToInclude.size
                // 从当前消息中，从后往前取图片，直到名额用完
                val imagesFromThisMsg = msg.imageUrl.takeLast(needed)
                recentImageUrlsToInclude.addAll(imagesFromThisMsg)
            }
        }
        // 将收集到的URL列表转换为Set，以便快速查找
        val imageSetToInclude = recentImageUrlsToInclude.toSet()
        // 3. 在构建 history 时，从后往前检查是否要包含图片
        val history = conversation.messages.mapNotNull { msg ->
            if (msg.text.isBlank() && msg.imageUrl.isNullOrEmpty()) return@mapNotNull null

            val contentParts = mutableListOf<ContentPart>()
            if (msg.text.isNotBlank()) {
                contentParts.add(TextPart(text = msg.text))
            }

            if (!msg.imageUrl.isNullOrEmpty()) {
                // 遍历当前消息的所有图片URL
                msg.imageUrl.forEach { url ->
                    // 只有当这个URL在我们收集的Set中时，才处理它
                    if (imageSetToInclude.contains(url)) {
                        Log.d("ChatViewModel", "Including selected image in history: $url")
                        try {
                            val uploadedUrl = localUriToOssUrlMap[url] ?: run {
                                val newOssUrl = UploadImage.uploadFileAndGetUrlfromLocaluri(application, "qwen-vl-plus", Uri.parse(url))
                                // 更新内存和数据库索引
                                localUriToOssUrlMap[url] = newOssUrl
                                val newIndex = uploadImageIndex(localUri = url, ossUrl = newOssUrl)
                                conversationRepository.insertImageIndex(newIndex)
                                newOssUrl
                            }
                            contentParts.add(ImageUrlPart(imageUrl = ImageUrl(url = uploadedUrl)))
                        } catch (e: Exception) {
                            Log.e("ChatViewModel", "Image upload failed in history generation", e)
                        }
                    }
                }
            }
            if (contentParts.isEmpty()) return@mapNotNull null

            val content: Any = if (contentParts.size == 1 && contentParts.first() is TextPart) {
                (contentParts.first() as TextPart).text
            } else {
                contentParts
            }

            Message(role = if (msg.isUser) "user" else "assistant", content = content)
        }

        val streamAction = if (conversation.modelName == "qwen-vl-plus") {
            chatWithHttp.generateStreamWithImage(
                conversation.modelName,
                history,
                0.8,
                0.8,
                _state.value.enableThinking
            )
        } else {
            chatWithHttp.generateStream(
                conversation.modelName,
                history,
                0.8,
                0.8,
                _state.value.enableSearch,
                _state.value.enableThinking
            )
        }

        streamAction.collect { newChunk ->
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
            updateLastMessage(conversation.id) { it.copy(imageUrl = listOf(imageUrl)) }
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

    private fun uploadImage(tmpuri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingImage = true) }
            try {
                // 使用 Uri.toString() 作为唯一的 key
                val internalFilePath = ImagePersistenceManager.getOrCreateInternalCopy(application,
                    tmpuri) ?: ""
                val internalFileUri = Uri.fromFile(File(internalFilePath))
                _state.update { it.copy(selectedImageUri = it.selectedImageUri + internalFileUri) }
                val localinterUriString = internalFileUri.toString()
                // 如果图片还未上传过，则上传
                if (!localUriToOssUrlMap.containsKey(localinterUriString)) {
                    val ossUrl = UploadImage.uploadFileAndGetUrl(application, "qwen-vl-plus", internalFileUri)
                    localUriToOssUrlMap[localinterUriString] = ossUrl
                    val newIndex = uploadImageIndex(localUri = localinterUriString, ossUrl = ossUrl)
                    conversationRepository.insertImageIndex(newIndex)
                    _state.update { it.copy(isUploadingImage = false) }
                    Log.d("ChatViewModel", "Image uploaded and indexed: $localinterUriString -> $ossUrl")
                } else {
                    _state.update { it.copy(isUploadingImage = false) }
                    Log.d("ChatViewModel", "Image already indexed. Skipping upload.")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Image upload for indexing failed", e)
                _sideEffect.emit(ChatSideEffect.ShowError("图片上传失败: ${e.message}"))
                // 上传失败，清空选择，防止用户发送一个没有上传成功的图片
                _state.update { it.copy(selectedImageUri = emptyList()) }
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
