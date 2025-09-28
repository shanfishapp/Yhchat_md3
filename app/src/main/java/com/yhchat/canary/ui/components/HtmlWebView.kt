package com.yhchat.canary.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.viewinterop.AndroidView
import com.yhchat.canary.utils.ChatAddLinkHandler

/**
 * HTML WebView渲染组件
 */
@Composable
fun HtmlWebView(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val codeBackgroundColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceVariant.toArgb()
    } else {
        MaterialTheme.colorScheme.surfaceVariant.toArgb()
    }
    
    val styledHtml = remember(htmlContent, backgroundColor, textColor, linkColor, codeBackgroundColor, isDarkTheme) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    margin: 0;
                    padding: 8px;
                    background-color: ${String.format("#%06X", backgroundColor and 0xFFFFFF)};
                    color: ${String.format("#%06X", textColor and 0xFFFFFF)} !important;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    font-size: 14px;
                    line-height: 1.4;
                }
                * {
                    color: ${String.format("#%06X", textColor and 0xFFFFFF)} !important;
                }
                p, div, span, h1, h2, h3, h4, h5, h6 {
                    color: ${String.format("#%06X", textColor and 0xFFFFFF)} !important;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                a {
                    color: ${String.format("#%06X", linkColor and 0xFFFFFF)} !important;
                    text-decoration: none;
                }
                a:hover {
                    text-decoration: underline;
                }
                pre, code {
                    background-color: ${String.format("#%06X", codeBackgroundColor and 0xFFFFFF)} !important;
                    color: ${String.format("#%06X", textColor and 0xFFFFFF)} !important;
                    padding: 4px 8px;
                    border-radius: 4px;
                    font-family: 'Courier New', monospace;
                }
                blockquote {
                    border-left: 4px solid ${String.format("#%06X", linkColor and 0xFFFFFF)};
                    margin: 8px 0;
                    padding-left: 12px;
                    color: ${String.format("#%06X", textColor and 0xFFFFFF)} !important;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                }
                th, td {
                    border: 1px solid rgba(128, 128, 128, 0.3);
                    padding: 8px;
                    text-align: left;
                }
                th {
                    background-color: ${String.format("#%06X", codeBackgroundColor and 0xFFFFFF)} !important;
                    color: ${String.format("#%06X", textColor and 0xFFFFFF)} !important;
                }
                td {
                    color: ${String.format("#%06X", textColor and 0xFFFFFF)} !important;
                }
            </style>
        </head>
        <body>
            $htmlContent
        </body>
        </html>
        """.trimIndent()
    }
    
    // 自动高度：根据内容动态调整 WebView 高度
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx: android.content.Context ->
            WebView(ctx).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        url?.let {
                            if (it.startsWith("yunhu://")) {
                                ChatAddLinkHandler.handleLink(ctx, it)
                                return true
                            }
                        }
                        return false
                    }
                }
                settings.apply {
                    javaScriptEnabled = true // HTML消息可能需要JS
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    setSupportZoom(true)
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                setBackgroundColor(backgroundColor)
            }
        },
        update = { webView: WebView ->
            webView.loadDataWithBaseURL(
                null,
                styledHtml,
                "text/html",
                "UTF-8",
                null
            )
            // 自动高度适配：注入JS获取内容高度并设置WebView高度
            webView.post {
                webView.evaluateJavascript(
                    """
                    (function() {
                        var body = document.body,
                            html = document.documentElement;
                        var height = Math.max(body.scrollHeight, body.offsetHeight,
                            html.clientHeight, html.scrollHeight, html.offsetHeight);
                        window.AndroidInterface && window.AndroidInterface.setHeight && window.AndroidInterface.setHeight(height);
                        return height;
                    })();
                    """.trimIndent(),
                    null
                )
            }
        }
    )
}
