package com.example.myapplication.feature.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.feature.chat.model.Conversation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationListScreen(
    conversations: List<Conversation>,
    availableModels: List<String>,
    onConversationClicked: (String) -> Unit,
    onNewConversationClicked: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    var showModelMenu by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

    if (conversationToDelete != null) {
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this conversation?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        conversationToDelete?.id?.let(onDeleteConversation)
                        conversationToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { conversationToDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversations") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showModelMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New Conversation")
                }
                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false }
                ) {
                    availableModels.forEach { modelName ->
                        DropdownMenuItem(
                            text = { Text(modelName) },
                            onClick = {
                                onNewConversationClicked(modelName)
                                showModelMenu = false
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        // 在 LazyColumn 中对传入的列表进行排序
        // sortedByDescending 会返回一个新的、已排序的列表，而不会改变原始的 conversations 列表
        val sortedConversations = conversations.sortedByDescending { it.timestamp }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(sortedConversations, key = { it.id }) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    onClick = { onConversationClicked(conversation.id) },
                    onLongClick = { conversationToDelete = conversation }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = conversation.modelName, fontWeight = FontWeight.Bold)
                Text(text = conversation.messages.lastOrNull()?.text ?: "New Conversation", maxLines = 1)
            }
            Text(
                text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(conversation.timestamp)),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
