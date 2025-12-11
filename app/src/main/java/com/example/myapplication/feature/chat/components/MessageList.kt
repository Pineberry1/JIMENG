package com.example.myapplication.feature.chat.components

import android.util.Log
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    onSaveImageClicked: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val lastMessage = messages.lastOrNull()
    // --- 状态定义 ---
    // 1. 核心状态：列表是否应该自动滚动到底部。
    var shouldFollowBottom by remember { mutableStateOf(true) }

    // 2. 派生状态：判断用户是否正在用手指拖动列表。
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()

    // 3. 派生状态：通过 listState 计算列表是否物理上滚动到底部。
    val isPhysicallyAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0 || visibleItemsInfo.isEmpty()) {
                true
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                val isLastItemVisible = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
                val itemBottom = lastVisibleItem.offset + lastVisibleItem.size
                val viewportBottom = layoutInfo.viewportEndOffset
                // 增加一个小小的容差值，避免因为浮点数精度问题导致判断失败
                isLastItemVisible && itemBottom <= viewportBottom + 5
            }
        }
    }

    // --- 滚动与锁定逻辑 (最终、精确修复版) ---

    // 1. 当用户向上滚动时，解锁“跟随底部”模式
    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .filter { isUserDragging && !isPhysicallyAtBottom } // 用户拖动且不在底部时解锁
            .collect {
                if (shouldFollowBottom) {
                    Log.d("MessageList", "User is scrolling up. Unlocking follow mode.")
                    shouldFollowBottom = false
                }
            }
    }

    // 2. 当用户滚动回底部时，重新锁定
    LaunchedEffect(isPhysicallyAtBottom) {
        if (isPhysicallyAtBottom && !shouldFollowBottom) {
            Log.d("MessageList", "User scrolled back to the bottom. Re-locking follow mode.")
            shouldFollowBottom = true
        }
    }

    // 3. 当有新消息时，如果需要，则执行【精确】的瞬时滚动到底部
    LaunchedEffect(lastMessage?.text, lastMessage?.reason_text) {
        val lastIndex = messages.size - 1
        if (lastIndex < 0) return@LaunchedEffect

        // 条件：需要跟随，且用户没有正在拖动或fling
        if (shouldFollowBottom && !listState.isScrollInProgress) {
            Log.d("MessageList", "message update")

            // 使用瞬时滚动，避免所有动画竞态条件
            listState.scrollToItem(lastIndex, scrollOffset = Int.MAX_VALUE)
        }
    }
    LaunchedEffect(lastMessage?.timestamp){
        if (lastMessage != null) {
            shouldFollowBottom = true
            Log.d("MessageList", "New message. lock and Performing precise scroll to bottom.")
            listState.animateScrollToItem(messages.size - 2)
            //if(shouldFollowBottom)listState.scrollToItem(messages.size - 1, scrollOffset = Int.MAX_VALUE)
        }
    }
    // --- UI 渲染 ---
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            if (message.imageUrl != null) {
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
