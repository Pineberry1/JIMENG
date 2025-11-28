package com.example.myapplication.feature.chat

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.feature.chat.components.*
import com.example.myapplication.feature.chat.viewmodel.ChatIntent
import com.example.myapplication.feature.chat.viewmodel.ChatSideEffect
import com.example.myapplication.feature.chat.viewmodel.ChatViewModel
import com.example.myapplication.feature.chat.viewmodel.ChatViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(LocalContext.current.applicationContext as Application)),
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    var showImageParamsSheet by remember { mutableStateOf(false) }
    var selectedModel by remember(state.availableModels) {
        mutableStateOf(state.availableModels.firstOrNull() ?: "")
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is ChatSideEffect.ShowError -> {
                    Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
                }
                is ChatSideEffect.ShowSuccess -> {
                    Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ConversationListScreen(
                    conversations = state.conversations,
                    availableModels = state.availableModels,
                    onConversationClicked = { conversationId ->
                        viewModel.processIntent(ChatIntent.SwitchConversation(conversationId))
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNewConversationClicked = { modelName ->
                        viewModel.processIntent(ChatIntent.SelectModel(modelName))
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(state.getActiveConversation()?.modelName ?: "New Chat") },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Conversations")
                        }
                    },
                    actions = {
                        if (state.getActiveConversation()?.modelName == "qwen-image-plus") {
                            IconButton(onClick = { showImageParamsSheet = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Image Parameters")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    val isTextModel = state.getActiveConversation()?.modelName?.startsWith("qwen") ?: true &&
                            state.getActiveConversation()?.modelName != "qwen-image-plus"

                    if (!state.selectedImageUri.isNullOrEmpty()) {
                        ImagePreview(
                            uri = state.selectedImageUri!!,
                            isUploading = state.isUploadingImage,
                            onClear = { uriToRemove ->
                                viewModel.processIntent(ChatIntent.ClearSelectedImage(uriToRemove))
                            })
                    }

                    if (isTextModel) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("联网搜索")
                            Switch(
                                checked = state.enableSearch,
                                onCheckedChange = { viewModel.processIntent(ChatIntent.SetEnableSearch(it)) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("深度思考")
                            Switch(
                                checked = state.enableThinking,
                                onCheckedChange = { viewModel.processIntent(ChatIntent.SetEnableThinking(it)) }
                            )
                        }
                    }

                    val canSendMessage = (state.inputText.isNotBlank() || state.uploadedImageUrl != null) && !state.isLoading && !state.isUploadingImage

                    ChatInputBar(
                        inputText = state.inputText,
                        canSendMessage = canSendMessage,
                        onInputTextChanged = { text -> viewModel.processIntent(ChatIntent.UpdateInputText(text)) },
                        onSendMessage = { text ->
                            val activeConversation = state.getActiveConversation()
                            viewModel.processIntent(
                                ChatIntent.SendMessage(
                                    text = text,
                                    modelName = activeConversation?.modelName ?: selectedModel,
                                    isNewConversation = activeConversation == null
                                )
                            )
                        },
                        onImageSelected = { uri ->
                            viewModel.processIntent(ChatIntent.SelectImage(uri))
                        },
                        isImageModel = state.getActiveConversation()?.modelName == "qwen-vl-plus"
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                val activeConversation = state.getActiveConversation()
                val message = activeConversation?.messages ?: emptyList()
                if (message.isEmpty()) {
                    WelcomeScreen(
                        availableModels = state.availableModels,
                        selectedModel = selectedModel,
                        onModelSelected = { newModel ->
                            selectedModel = newModel
                            viewModel.processIntent(ChatIntent.SelectModel(newModel))
                        }
                    )
                } else {
                    MessageList(
                        messages = message,
                        onSaveImageClicked = { imageUrl ->
                            viewModel.processIntent(ChatIntent.SaveImageToGallery(imageUrl))
                        }
                    )
                }
            }
        }
    }

    if (showImageParamsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImageParamsSheet = false },
            sheetState = sheetState
        ) {
            ImageParamsPanel(
                params = state.imageGenParams,
                onParamsChanged = { newParams ->
                    viewModel.processIntent(ChatIntent.UpdateImageGenParams(newParams))
                }
            )
        }
    }
}
