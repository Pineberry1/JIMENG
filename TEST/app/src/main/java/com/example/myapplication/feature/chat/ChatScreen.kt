package com.example.myapplication.feature.chat

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.feature.chat.components.ChatInputBar
import com.example.myapplication.feature.chat.components.MessageList
import com.example.myapplication.feature.chat.components.ModelSelector
import com.example.myapplication.feature.chat.viewmodel.ChatIntent
import com.example.myapplication.feature.chat.viewmodel.ChatViewModel
import com.example.myapplication.feature.chat.viewmodel.ChatViewModelFactory
import com.example.myapplication.feature.chat.viewmodel.ChatSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(LocalContext.current.applicationContext as Application)),
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showSettingsMenu by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is ChatSideEffect.ShowError -> {
                    android.widget.Toast.makeText(context, sideEffect.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { ModelSelector(state.currentModelName, state.availableModels) { viewModel.processIntent(ChatIntent.SelectModel(it)) } },
                actions = {
                    IconButton(onClick = { showSettingsMenu = !showSettingsMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showSettingsMenu,
                        onDismissRequest = { showSettingsMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("Model Settings") }, onClick = onNavigateToSettings)
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = state.inputText,
                isLoading = state.isLoading,
                onInputTextChanged = { text -> viewModel.processIntent(ChatIntent.UpdateInputText(text)) },
                onSendMessage = { text -> viewModel.processIntent(ChatIntent.SendMessage(text)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            MessageList(messages = state.messages)
        }
    }
}
