package com.yhchat.canary.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * HTML WebView渲染组件
 */
@Composable
fun HtmlWebView(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    
    val styledHtml = remember(htmlContent, backgroundColor, textColor) {
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
                    color: #000000 !important;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    font-size: 14px;
                    line-height: 1.4;
                }
                * {
                    color: #000000 !important;
                }
                p, div, span, h1, h2, h3, h4, h5, h6 {
                    color: #000000 !important;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                a {
                    color: #2196F3;
                    text-decoration: none;
                }
                a:hover {
                    text-decoration: underline;
                }
                pre, code {
                    background-color: rgba(128, 128, 128, 0.2);
                    padding: 4px 8px;
                    border-radius: 4px;
                    font-family: 'Courier New', monospace;
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
                    background-color: rgba(128, 128, 128, 0.1);
                }
            </style>
        </head>
        <body>
            $htmlContent
        </body>
        </html>
        """.trimIndent()
    }
    
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp), // 默认高度，可以根据需要调整
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = false // 出于安全考虑，禁用JavaScript
                    domStorageEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                setBackgroundColor(backgroundColor)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                null,
                styledHtml,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}
