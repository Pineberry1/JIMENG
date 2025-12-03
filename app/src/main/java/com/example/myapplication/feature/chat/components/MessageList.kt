package com.example.myapplication.feature.chat.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.feature.chat.model.ChatMessage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter

// import kotlinx.coroutines.flow.pairwise // <- 不再需要此导入

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    onSaveImageClicked: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val lastMessage = messages.lastOrNull()
    // 1. 引入“锁定到底部”的状态，默认为 true。
    var isLockedToBottom by remember { mutableStateOf(true) }

    // 2. 判断列表是否物理上滚动到底部的派生状态。
    val isPhysicallyAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0 || visibleItemsInfo.isEmpty()) {
                true
            } else {
                // 1. 最后一个可见项的索引必须是整个列表的最后一项。
                // 2. 该项的底部边缘必须在视口之内或底部。
                val lastVisibleItem = visibleItemsInfo.last()
                val isLastItemVisible = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
                val itemBottom = lastVisibleItem.offset + lastVisibleItem.size
                val viewportBottom = layoutInfo.viewportEndOffset

                isLastItemVisible && itemBottom <= viewportBottom
            }
        }
    }
    suspend fun scrollToBottom(){
        listState.animateScrollToItem(index = messages.size - 1, scrollOffset = Int.MAX_VALUE)
    }
    // 3. 滚动逻辑：当处于锁定状态且最后一条消息内容变化时，滚动到底部。
    LaunchedEffect(lastMessage?.text?.length, lastMessage?.reason_text?.length) {
        if (isLockedToBottom && messages.isNotEmpty()) {
            scrollToBottom()
        }
    }
    //有新消息直接到最底层
    LaunchedEffect(lastMessage?.timestamp) {
        if (!isLockedToBottom && messages.isNotEmpty()) {
            scrollToBottom()
            isLockedToBottom = true
            Log.d("MessageList", "List is physically at bottom. Re-locking.")
        }
    }

    // 4. 重新锁定的逻辑 - 当用户滚动回底部时，自动重新锁定。
    LaunchedEffect(isPhysicallyAtBottom) {
        if (isPhysicallyAtBottom) {
            if (!isLockedToBottom) {
                Log.d("MessageList", "List is physically at bottom. Re-locking.")
                isLockedToBottom = true
            }
        }
    }

    // 5. 最终解锁逻辑：修复了锁定后立即解锁的冲突
    LaunchedEffect(listState) {
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .filter { currentOffset ->
                val isScrollingUp = currentOffset > previousOffset
                previousOffset = currentOffset // 更新上一次的偏移量

                // 核心修复：只有在“向上滚动”且“列表当前是锁定的”且“列表不在物理底部”时，才允许解锁
                isScrollingUp && isLockedToBottom && !isPhysicallyAtBottom
            }
            .collect {
                Log.d("MessageList", "Unlocking list due to user scrolling UP.")
                isLockedToBottom = false
            }
    }


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            if (message.imageUrl != null && !message.isUser) {
                ImageMessageItem(
                    imageUrls = message.imageUrl,
                    onSaveClicked = onSaveImageClicked,
                    isUserMessage = message.isUser
                )
            }
            MessageBubble(message)
        }
    }
}
