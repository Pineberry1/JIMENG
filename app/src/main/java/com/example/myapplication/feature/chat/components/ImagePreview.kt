package com.example.myapplication.feature.chat.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.* // 导入所有 layout 组件
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun ImagePreview(
    uri: List<Uri>,
    isUploading: Boolean,
    onClear: (Uri) -> Unit // 1. 关键修改：让 onClear 接收一个 Uri
) {
    // 最外层的Box不再需要，我们将对齐逻辑移到Row中
    Row(
        // 设置一个固定的高度，让宽度自适应内容
        modifier = Modifier
            .height(80.dp)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), // 将 padding 应用到 Row
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 遍历Uri列表
        uri.forEach { url ->
            // 2. 用Box包裹每个Image，以便在图片上叠加关闭按钮
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
            ) {
                // 图片本身
                Image(
                    painter = rememberAsyncImagePainter(url),
                    contentDescription = "Selected image preview",
                    modifier = Modifier
                        .fillMaxSize() // 图片填满Box
                        .alpha(if (isUploading) 0.3f else 1.0f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                // 3. 为每张图片添加独立的关闭按钮
                IconButton(
                    onClick = { onClear(url) }, // 调用 onClear 并传入当前图片的 url
                    modifier = Modifier
                        .align(Alignment.TopEnd) // 定位到Box的右上角
                        .padding(2.dp) // 稍微加一点内边距，避免太靠边
                        .size(20.dp) // 明确按钮大小
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear selected image",
                        tint = Color.White,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                    )
                }

                // 4. 如果正在上传，为每张图片都显示一个加载指示器
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                    )
                }
            }
        }
    }
}
