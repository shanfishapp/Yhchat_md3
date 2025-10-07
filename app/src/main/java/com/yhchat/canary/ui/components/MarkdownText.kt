package com.yhchat.canary.ui.components

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.yhchat.canary.utils.UnifiedLinkHandler
import io.noties.markwon.Markwon
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.SoftBreakAddsNewLinePlugin


/**
 * Markdown文本渲染组件
 * 支持Material Design 3主题，背景透明以适配消息气泡
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = Color.Transparent,
    onImageClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val textColorInt = textColor.toArgb()
    val backgroundColorInt = backgroundColor.toArgb()
    
    val markwon = remember(context, isDarkTheme, textColorInt, onImageClick) {
        Markwon.builder(context)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())  // 支持软换行（单个回车换行）
            .usePlugin(HtmlPlugin.create())
            .usePlugin(CoilImagesPlugin.create(context))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver { view, link ->
                        // 检查是否是图片链接
                        if (onImageClick != null && (link.endsWith(".jpg") || link.endsWith(".jpeg") || 
                            link.endsWith(".png") || link.endsWith(".gif") || link.endsWith(".webp"))) {
                            onImageClick(link)
                        } else if (UnifiedLinkHandler.isHandleableLink(link)) {
                            // 使用统一的链接处理器
                            UnifiedLinkHandler.handleLink(context, link)
                        } else {
                            // 使用默认浏览器打开其他链接
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "无法打开链接", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .build()
    }
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                // 设置链接可点击
                movementMethod = LinkMovementMethod.getInstance()
                setTextColor(textColorInt)
                setBackgroundColor(backgroundColorInt)
                textSize = 14f
                setPadding(0, 0, 0, 0)
                linksClickable = true
            }
        },
        update = { textView ->
            textView.setTextColor(textColorInt)
            textView.setBackgroundColor(backgroundColorInt)
            val spanned = markwon.toMarkdown(markdown)
            markwon.setParsedMarkdown(textView, spanned)
            
            // 为图片添加点击事件
            if (onImageClick != null) {
                val text = textView.text
                if (text is Spanned) {
                    val spannable: Spannable = if (text is Spannable) {
                        text
                    } else {
                        SpannableStringBuilder(text)
                    }

                    if (textView.text !== spannable) {
                        textView.text = spannable
                    }

                    val imageSpans = spannable.getSpans(0, spannable.length, ImageSpan::class.java)

                    imageSpans.forEach { span ->
                        val start = spannable.getSpanStart(span)
                        val end = spannable.getSpanEnd(span)
                        val imageUrl = span.source

                        if (!imageUrl.isNullOrEmpty()) {
                            spannable.getSpans(start, end, ClickableSpan::class.java).forEach { existingSpan ->
                                spannable.removeSpan(existingSpan)
                            }

                            val clickableSpan = object : ClickableSpan() {
                                override fun onClick(widget: View) {
                                    onImageClick(imageUrl)
                                }

                                override fun updateDrawState(ds: TextPaint) {
                                    super.updateDrawState(ds)
                                    ds.isUnderlineText = false
                                }
                            }

                            spannable.setSpan(
                                clickableSpan,
                                start,
                                end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                    textView.movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }
    )
}
