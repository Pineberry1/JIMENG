package com.example.myapplication.feature.chat

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                 ChatInputBar(
                    inputText = state.inputText,
                    isLoading = state.isLoading,
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
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                val activeConversation = state.getActiveConversation()
                val message = activeConversation?.messages ?: emptyList()
                if (message.isEmpty()) {
                    WelcomeScreen(
                        availableModels = state.availableModels,
                        selectedModel = selectedModel,
                        onModelSelected = { selectedModel = it }
                    )
                } else {
                    MessageList(
                        messages = activeConversation?.messages ?: emptyList(),
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
