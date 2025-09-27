package com.yhchat.canary.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.yhchat.canary.ui.webview.WebViewActivity
import com.yhchat.canary.utils.ChatAddLinkHandler
import java.util.regex.Pattern

/**
 * 链接文本组件 - 自动识别并处理 HTTP 链接
 */
@Composable
fun LinkText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    linkColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    
    // 扩展的链接正则表达式 - 支持http和yunhu协议
    val urlPattern = Pattern.compile(
        "(https?://[^\\s\\u4e00-\\u9fff]+|yunhu://[^\\s\\u4e00-\\u9fff]*)",
        Pattern.CASE_INSENSITIVE
    )
    
    val annotatedString = buildAnnotatedString {
        val matcher = urlPattern.matcher(text)
        var lastEnd = 0
        
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val url = matcher.group()
            
            // 添加链接前的普通文本
            if (start > lastEnd) {
                append(text.substring(lastEnd, start))
            }
            
            // 添加链接文本 - 醒目的样式
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            ) {
                append(url)
            }
            pop()
            
            lastEnd = end
        }
        
        // 添加剩余的普通文本
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
    
    ClickableText(
        text = annotatedString,
        style = style,
        onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val url = annotation.item
                    if (url.startsWith("yunhu://")) {
                        // 处理yunhu协议链接
                        ChatAddLinkHandler.handleLink(context, url)
                    } else {
                        // 启动 WebView Activity（HTTP链接）
                        WebViewActivity.start(context, url)
                    }
                }
        }
    )
}

/**
 * 简单的链接检测工具
 */
object LinkDetector {
    private val urlPattern = Pattern.compile(
        "(https?://[^\\s\\u4e00-\\u9fff]+|yunhu://[^\\s\\u4e00-\\u9fff]*)",
        Pattern.CASE_INSENSITIVE
    )
    
    /**
     * 检查文本是否包含链接
     */
    fun containsLink(text: String): Boolean {
        return urlPattern.matcher(text).find()
    }
    
    /**
     * 提取文本中的所有链接
     */
    fun extractLinks(text: String): List<String> {
        val links = mutableListOf<String>()
        val matcher = urlPattern.matcher(text)
        
        while (matcher.find()) {
            val url = matcher.group()
            links.add(url)
        }
        
        return links
    }
}
