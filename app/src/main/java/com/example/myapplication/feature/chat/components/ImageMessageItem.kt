// In D:/app/JIMENG/app/src/main/java/com/example/myapplication/feature/chat/components/ImageMessageItem.kt

package com.example.myapplication.feature.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ImageMessageItem(
    imageUrls: List<String>,
    onSaveClicked: (String) -> Unit,
    isUserMessage: Boolean,
    modifier: Modifier = Modifier
) {
    val displayImageUrls = imageUrls.take(3)

    // 1. 获取屏幕高度，并根据消息来源计算图片的目标高度
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val targetImageHeight = if (isUserMessage) {
        screenHeight / 5 // 用户图片: 1/6 屏幕高度
    } else {
        screenHeight / 3 // AI图片: 1/3 屏幕高度
    }

    // 2. 使用Box来控制整体对齐（靠左或靠右）
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        // 3. 关键修复：让Row的宽度自适应其内部内容
        Row(
            modifier = Modifier.wrapContentWidth(), // <--- 使用 wrapContentWidth
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 遍历URL列表，为每个URL创建一个图片
            displayImageUrls.forEach { url ->
                Box(
                    // 4. 为每个图片Box设置固定的高度和1:1的宽高比
                    modifier = Modifier
                        .height(targetImageHeight)
                        .aspectRatio(1f) // 宽度将根据高度自动调整
                ) {
                    // 卡片和下载按钮的内部逻辑完全不变
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Generated Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    IconButton(
                        onClick = { onSaveClicked(url) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save Image",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
