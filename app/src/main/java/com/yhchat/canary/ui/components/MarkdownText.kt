package com.yhchat.canary.ui.components

import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
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
import com.yhchat.canary.utils.ChatAddLinkHandler
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * 自定义链接点击处理
 */
class CustomLinkMovementMethod(private val context: android.content.Context) : LinkMovementMethod() {
    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            var x = event.x.toInt()
            var y = event.y.toInt()
            
            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop
            
            x += widget.scrollX
            y += widget.scrollY
            
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())
            
            val link = buffer.getSpans(off, off, URLSpan::class.java)
            
            if (link.isNotEmpty()) {
                if (action == MotionEvent.ACTION_UP) {
                    val url = link[0].url
                    if (url.startsWith("yunhu://")) {
                        ChatAddLinkHandler.handleLink(context, url)
                        return true
                    }
                    // 让系统处理其他链接
                    link[0].onClick(widget)
                }
                return true
            }
        }
        return super.onTouchEvent(widget, buffer, event)
    }
}

/**
 * Markdown文本渲染组件
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val textColorInt = textColor.toArgb()
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    
    val markwon = remember(context, isDarkTheme, textColorInt) {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(CoilImagesPlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .build()
    }
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                // 使用自定义链接处理
                movementMethod = CustomLinkMovementMethod(ctx)
                setTextColor(textColorInt)
                setBackgroundColor(backgroundColor)
                textSize = 14f
                setPadding(0, 0, 0, 0)
            }
        },
        update = { textView ->
            textView.setTextColor(textColorInt)
            val spanned = markwon.toMarkdown(markdown)
            markwon.setParsedMarkdown(textView, spanned)
        }
    )
}
