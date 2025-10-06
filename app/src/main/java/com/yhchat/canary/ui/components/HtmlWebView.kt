package com.yhchat.canary.ui.components

import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.viewinterop.AndroidView
import com.yhchat.canary.utils.UnifiedLinkHandler

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
                // 使用软件渲染层以避免GPU硬件加速导致的崩溃
                // 这会稍微降低性能，但能显著提高稳定性
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        url?.let {
                            // 处理 yunhu:// 和 yhfx 分享链接
                            if (UnifiedLinkHandler.isHandleableLink(it)) {
                                val handled = UnifiedLinkHandler.handleLink(ctx, it)
                                if (handled) {
                                    return true
                                }
                                // 如果是分享链接，需要特殊处理（触发回调）
                                // 这里返回 true 阻止默认行为，实际处理在外部进行
                                return true
                            }
                        }
                        return false
                    }
                    
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                        
                        // 为 https://chat-img.jwznb.com 的图片添加 Referer
                        if (url.startsWith("https://chat-img.jwznb.com")) {
                            try {
                                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                                connection.setRequestProperty("Referer", "https://myapp.jwznb.com")
                                connection.connect()
                                
                                val contentType = connection.contentType
                                val encoding = connection.contentEncoding ?: "UTF-8"
                                val inputStream = connection.inputStream
                                
                                return WebResourceResponse(contentType, encoding, inputStream)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        return super.shouldInterceptRequest(view, request)
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // 页面加载完成，可以进行一些清理
                    }
                }
                settings.apply {
                    javaScriptEnabled = true // 保留JS功能
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    setSupportZoom(true)
                    builtInZoomControls = false
                    displayZoomControls = false
                    
                    // 性能优化设置
                    cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                    loadsImagesAutomatically = true
                    blockNetworkImage = false
                    
                    // 减少内存占用
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    }
                    
                    // 禁用预加载以减少内存占用
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        offscreenPreRaster = false
                    }
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
    
    // 使用DisposableEffect管理WebView生命周期
    DisposableEffect(htmlContent) {
        onDispose {
            // WebView会由AndroidView自动管理销毁
        }
    }
}
