package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity // 推荐改为 ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. 定义简单的消息数据模型
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: String
)

// 2. 将 AppCompatActivity 改为 ComponentActivity (Compose 标准)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 使用 Material3 主题容器
            MaterialTheme {
                // 调用我们的主聊天界面
                ChatScreen()
            }
        }
    }
}

@Composable
fun ChatScreen() {
    // 3. 准备一些写死的静态数据
    val messages = remember {
        listOf(
            ChatMessage("1", "你好，我是你的 AI 助手。", false, "10:00"),
        )
    }

    // Scaffold 提供了标准的布局结构（顶部栏、底部栏、内容区域）
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(), // imePadding 避免键盘遮挡
        bottomBar = { ChatInputBar() } // 底部输入区域
    ) { innerPadding ->
        // 消息列表区域
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)) // 浅灰背景
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f), // 占据剩余空间
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // 气泡间距
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
            }
        }
    }
}

// 4. 单个消息气泡组件
@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser

    //这一行决定了气泡靠左还是靠右
    val alignment = if (isUser) Alignment.End else Alignment.Start

    // 气泡颜色
    val bubbleColor = if (isUser) Color(0xFF007AFF) else Color.White
    val textColor = if (isUser) Color.White else Color.Black

    // 气泡形状 (让它看起来像对话框)
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 300.dp) // 限制气泡最大宽度
        ) {
            Text(
                text = message.text,
                color = textColor,
                modifier = Modifier.padding(12.dp),
                fontSize = 16.sp
            )
        }
    }
}

// 5. 底部输入框组件
@Composable
fun ChatInputBar() {
    var textState by remember { mutableStateOf(TextFieldValue("")) }

    Surface(
        shadowElevation = 8.dp, // 顶部阴影
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 输入框
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                placeholder = { Text("输入消息...") },
                modifier = Modifier
                    .weight(1f) // 占据大部分宽度
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(24.dp), // 圆角输入框
                maxLines = 3
            )

            // 发送按钮
            IconButton(
                onClick = { /* 这里处理发送逻辑 */ },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF007AFF), shape = RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

// 预览功能，方便在 Android Studio 中直接查看
@Preview(showBackground = true)
@Composable
fun ChatPreview() {
    MaterialTheme {
        ChatScreen()
    }
}