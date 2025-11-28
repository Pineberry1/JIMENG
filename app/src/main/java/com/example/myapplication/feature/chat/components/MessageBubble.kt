package com.example.myapplication.feature.chat.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.example.myapplication.feature.chat.model.ChatMessage
import com.halilibo.richtext.commonmark.CommonmarkAstNodeParser
import com.halilibo.richtext.markdown.BasicMarkdown
import com.halilibo.richtext.ui.material3.RichText

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) Color(0xFF007AFF) else Color.White
    val reasonBubbleColor = if (isUser) bubbleColor.copy(alpha = 0.8f) else Color(0xFFF0F0F0)
    val textColor = if (isUser) Color.White else Color.Black

    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            // 1. 如果是用户，将内容推到末尾 (右侧)
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.spacedBy(8.dp),
            // 2. 让 Row 占满整个宽度，这样 Arrangement.End 才能生效
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser && (message.text.isNotBlank() || message.reason_text.isNotBlank())) {
                Image(
                    painter = painterResource(id = R.drawable.jimeng),
                    contentDescription = "AI Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                message.reason_text?.let { reasonText ->
                    if (reasonText.isNotBlank()) {
                        Surface(
                            color = reasonBubbleColor,
                            shape = shape,
                            shadowElevation = 1.dp,
                            modifier = Modifier.widthIn(max = screenWidth * 0.8f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                CompositionLocalProvider(LocalContentColor provides textColor) {
                                    RichText {
                                        val parser = remember { CommonmarkAstNodeParser() }
                                        val astNode = remember(parser, reasonText) {
                                            parser.parse(reasonText)
                                        }
                                        BasicMarkdown(astNode)
                                    }
                                }
                            }
                        }
                    }
                }


                if (message.text.isNotBlank()) {
                    Surface(
                        color = bubbleColor,
                        shape = shape,
                        shadowElevation = 2.dp,
                        modifier = Modifier.widthIn(max = screenWidth * 0.8f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            CompositionLocalProvider(LocalContentColor provides textColor) {
                                RichText {
                                    val parser = remember { CommonmarkAstNodeParser() }
                                    val astNode = remember(parser, message.text) {
                                        parser.parse(message.text)
                                    }
                                    BasicMarkdown(astNode)
                                }
                            }
                            Text(
                                text = message.timestamp,
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
