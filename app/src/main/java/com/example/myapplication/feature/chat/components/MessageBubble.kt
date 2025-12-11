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
import androidx.compose.runtime.key
private val LIST_ITEM_REGEX = Regex("^(\\d+\\.|-|\\*)\\s.*")

fun smartSplitMarkdown(fullText: String): List<String> {
    val lines = fullText.lines()
    val result = mutableListOf<String>()
    val currentChunk = StringBuilder()

    var inCodeBlock = false

    for (i in lines.indices) {
        val line = lines[i]
        val trimmedLine = line.trim()

        // 1. 检查代码块标记 (```)
        // 注意：通常代码块独占一行，但也可能前面有空格
        if (trimmedLine.startsWith("```")) {
            inCodeBlock = !inCodeBlock
        }

        // 2. 判断是否应该“切分” (Flush)
        // 触发切分的条件：当前是空行 且 不在代码块中 且 不是列表的内部空行
        val isEmptyLine = line.isBlank()

        if (isEmptyLine && !inCodeBlock) {
            // 进阶检查：这是否是一个“松散列表” (Loose List) 的中间空行？
            // 规则：如果 上一行是列表项 且 下一行也是列表项，则认为是列表的一部分，不切分
            val prevLine = lines.getOrNull(i - 1)?.trim() ?: ""
            val nextLine = lines.getOrNull(i + 1)?.trim() ?: ""

            val isListContinuation = prevLine.matches(LIST_ITEM_REGEX) &&
                    nextLine.matches(LIST_ITEM_REGEX)

            if (isListContinuation) {
                // 是列表中间的空行 -> 保留，不切分
                currentChunk.append(line).append("\n")
            } else {
                // 是真正的段落分隔符 -> 切分！
                if (currentChunk.isNotEmpty()) {
                    result.add(currentChunk.toString().trim())
                    currentChunk.clear()
                }
            }
        } else {
            // 普通内容 -> 累加
            currentChunk.append(line).append("\n")
        }
    }

    // 处理剩余的缓冲
    if (currentChunk.isNotEmpty()) {
        result.add(currentChunk.toString().trim())
    }

    return result
}
@Composable
private fun MarkdownParagraph(
    text: String,
    parser: CommonmarkAstNodeParser
) {
    // 这里的 RichText Scope 可以根据库的要求调整，通常 RichText 内部包含 Text 样式
    RichText {
        // 因为 text 变了，AST 才需要重新解析。
        // 对于已经结束的段落，text 不变，astNode 直接复用缓存。
        val astNode = remember(parser, text) {
            parser.parse(text)
        }
        BasicMarkdown(astNode)
    }
}
@Composable
fun SplitMarkdownView(
    fullText: String,
    textColor: Color
) {
    // 1. 只有当 fullText 变化时才重新拆分
    // 注意：split 操作在主线程做虽然有成本，但比全量 Markdown 解析低得多。
    // 如果追求极致，可以在 ViewModel 里拆好传 List<String> 进来。
    val paragraphs = remember(fullText) {
        smartSplitMarkdown(fullText)
    }

    // 复用同一个 parser 实例
    val parser = remember { CommonmarkAstNodeParser() }

    CompositionLocalProvider(LocalContentColor provides textColor) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // 段落间距
            paragraphs.forEachIndexed { index, paragraph ->
                // 2. 🔥 核心优化：使用 key 包裹
                // 只要 index 对应的 paragraph 内容没变，Compose 就会直接跳过这个 Item 的重组
                key(index) {
                    // 过滤空段落，防止渲染空白占位
                    if (paragraph.isNotBlank()) {
                        MarkdownParagraph(text = paragraph, parser = parser)
                    }
                }
            }
        }
    }
}
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
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // ... 头像部分代码保持不变 ...
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

                // --- 思考过程 (Reasoning) ---
                message.reason_text?.let { reasonText ->
                    if (reasonText.isNotBlank()) {
                        Surface(
                            color = reasonBubbleColor,
                            shape = shape,
                            shadowElevation = 1.dp,
                            modifier = Modifier.widthIn(max = screenWidth * 0.8f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // ✅ 使用拆分渲染组件
                                SplitMarkdownView(
                                    fullText = reasonText,
                                    textColor = textColor.copy(alpha = 0.8f) // 思考过程颜色稍淡
                                )
                            }
                        }
                    }
                }

                // --- 正文内容 (Content) ---
                if (message.text.isNotBlank()) {
                    Surface(
                        color = bubbleColor,
                        shape = shape,
                        shadowElevation = 2.dp,
                        modifier = Modifier.widthIn(max = screenWidth * 0.8f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // ✅ 使用拆分渲染组件
                            SplitMarkdownView(
                                fullText = message.text,
                                textColor = textColor
                            )

                            // 时间戳
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