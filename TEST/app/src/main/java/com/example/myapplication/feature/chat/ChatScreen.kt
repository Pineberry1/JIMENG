package com.example.myapplication.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.feature.chat.components.ChatInputBar
import com.example.myapplication.feature.chat.components.MessageBubble
import com.example.myapplication.feature.chat.model.ChatMessage
import com.example.myapplication.feature.chat.viewmodel.ChatIntent
import com.example.myapplication.feature.chat.viewmodel.ChatSideEffect
import com.example.myapplication.feature.chat.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is ChatSideEffect.ShowError -> {
                    android.widget.Toast.makeText(
                        context,
                        sideEffect.message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                is ChatSideEffect.ShowSuccess -> {
                    android.widget.Toast.makeText(
                        context,
                        sideEffect.message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {}
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            ChatInputBar(
                inputText = state.inputText,
                isLoading = state.isLoading,
                onInputTextChanged = { text ->
                    viewModel.processIntent(ChatIntent.UpdateInputText(text))
                },
                onSendMessage = { text ->
                    viewModel.processIntent(ChatIntent.SendMessage(text))
                }
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

@Composable
private fun MessageList(messages: List<ChatMessage>) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier, // ✅ 正确的 weight 用法
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages) { message ->
            MessageBubble(message)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatPreview() {
    MaterialTheme {
        ChatScreen()
    }
}