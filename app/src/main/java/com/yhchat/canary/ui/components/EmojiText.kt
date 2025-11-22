package com.yhchat.canary.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import java.util.regex.Pattern

/**
 * 表情文本渲染组件
 * 支持渲染 [.{表情名称}] 格式的文本为对应的表情图片
 */
@Composable
fun EmojiText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val context = LocalContext.current
    val emojiPattern = Pattern.compile("""\[\.([^\]]+)\]""")
    val matcher = emojiPattern.matcher(text)
    
    val parts = mutableListOf<RenderPart>()
    var lastIndex = 0
    
    // 分析文本，提取表情和普通文本部分
    while (matcher.find()) {
        // 添加表情前的普通文本
        if (matcher.start() > lastIndex) {
            val normalText = text.substring(lastIndex, matcher.start())
            if (normalText.isNotEmpty()) {
                parts.add(RenderPart.NormalText(normalText))
            }
        }
        
        // 添加表情部分
        val emojiName = matcher.group(1) ?: ""
        parts.add(RenderPart.Emoji(emojiName))
        
        lastIndex = matcher.end()
    }
    
    // 添加最后的普通文本
    if (lastIndex < text.length) {
        val remainingText = text.substring(lastIndex)
        if (remainingText.isNotEmpty()) {
            parts.add(RenderPart.NormalText(remainingText))
        }
    }
    
    // 渲染文本和表情
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        parts.forEach { part ->
            when (part) {
                is RenderPart.NormalText -> {
                    Text(
                        text = part.text,
                        style = style,
                        color = color,
                        maxLines = maxLines,
                        overflow = overflow
                    )
                }
                is RenderPart.Emoji -> {
                    val resourceId = context.resources.getIdentifier(
                        part.name, // 表情文件名（不含扩展名）
                        "drawable",
                        context.packageName
                    )
                    
                    if (resourceId != 0) {
                        // 如果找到了对应的资源，则显示图片
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(resourceId)
                                    .build()
                            ),
                            contentDescription = "[${part.name}]",
                            modifier = Modifier
                                .size(24.dp) // 设置表情大小，可根据需要调整
                        )
                    } else {
                        // 如果找不到对应资源，则显示原始格式文本
                        Text(
                            text = "[.${part.name}]",
                            style = style,
                            color = color,
                            maxLines = maxLines,
                            overflow = overflow
                        )
                    }
                }
            }
        }
    }
}

/**
 * 表情文本渲染部分
 */
sealed class RenderPart {
    data class NormalText(val text: String) : RenderPart()
    data class Emoji(val name: String) : RenderPart()
}