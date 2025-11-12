package com.yhchat.canary.ui.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yhchat.canary.ui.base.BaseActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import com.yhchat.canary.ui.settings.HtmlSettingsActivity

/**
 * WebView Activity - 用于显示网页链接
 */
class WebViewActivity : BaseActivity() {
    
    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"
        
        /**
         * 启动 WebView Activity
         */
        fun start(context: Context, url: String, title: String? = null) {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val url = intent.getStringExtra(EXTRA_URL) ?: ""
        val initialTitle = intent.getStringExtra(EXTRA_TITLE) ?: "网页"
        
        if (url.isEmpty()) {
            finish()
            return
        }
        
        setContent {
            YhchatCanaryTheme {
                WebViewScreen(
                    url = url,
                    initialTitle = initialTitle,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

/**
 * WebView 界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    initialTitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var currentTitle by remember { mutableStateOf(initialTitle) }
    var currentUrl by remember { mutableStateOf(url) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    
    // 添加超时处理
    LaunchedEffect(isLoading) {
        if (isLoading) {
            kotlinx.coroutines.delay(30000) // 30秒超时
            if (isLoading) {
                isLoading = false // 强制停止加载状态
            }
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = currentTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "菜单"
                        )
                    }
                    
                    // 下拉菜单
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // 刷新
                        DropdownMenuItem(
                            text = { Text("刷新") },
                            onClick = {
                                showMenu = false
                                webView?.reload()
                                isLoading = true // 手动设置加载状态
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新"
                                )
                            }
                        )
                        
                        // 强制停止加载
                        DropdownMenuItem(
                            text = { Text("停止加载") },
                            onClick = {
                                showMenu = false
                                webView?.stopLoading()
                                isLoading = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "停止加载"
                                )
                            }
                        )
                        
                        // 在浏览器中打开
                        DropdownMenuItem(
                            text = { Text("在浏览器中打开") },
                            onClick = {
                                showMenu = false
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // 处理没有浏览器的情况
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "在浏览器中打开"
                                )
                            }
                        )
                    }
                }
            }
        )
        
        // 加载进度条 - 始终显示一个占位空间，避免布局跳动
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // 现代化 WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    // 启用现代WebView功能
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        WebView.setWebContentsDebuggingEnabled(true)
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            url?.let { currentUrl = it }
                        }
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            url?.let { currentUrl = it }
                        }
                        
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            return false // 让 WebView 处理所有链接
                        }
                        
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            isLoading = false
                        }
                        
                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            isLoading = false
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { currentTitle = it }
                        }
                        
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            isLoading = newProgress < 100
                        }
                        
                        override fun onPermissionRequest(request: PermissionRequest?) {
                            // 处理权限请求（如摄像头、麦克风等）
                            request?.grant(request.resources)
                        }
                        
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            // 处理控制台消息
                            return true
                        }
                    }
                    
                    webView = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            update = { view ->
                // 每次重新组合时更新WebView设置
                val prefs = context.getSharedPreferences("html_settings", Context.MODE_PRIVATE)
                view.settings.apply {
                    // 基础设置
                    javaScriptEnabled = prefs.getBoolean("enable_javascript", true)
                    domStorageEnabled = true
                    loadWithOverviewMode = prefs.getBoolean("allow_zoom", true)
                    useWideViewPort = prefs.getBoolean("allow_zoom", true)
                    builtInZoomControls = prefs.getBoolean("allow_zoom", true)
                    displayZoomControls = true
                    setSupportZoom(prefs.getBoolean("allow_zoom", true))
                    
                    // 现代化WebView设置
                    javaScriptCanOpenWindowsAutomatically = true
                    allowFileAccess = true
                    allowContentAccess = true
                    allowUniversalAccessFromFileURLs = true
                    allowFileAccessFromFileURLs = true
                    loadsImagesAutomatically = prefs.getBoolean("load_images", true)
                    blockNetworkImage = false
                    blockNetworkLoads = false
                    
                    // 缓存设置
                    val cacheModeValue = prefs.getInt("cache_mode", 0)
                    cacheMode = when (cacheModeValue) {
                        1 -> WebSettings.LOAD_NO_CACHE
                        2 -> WebSettings.LOAD_CACHE_ONLY
                        3 -> WebSettings.LOAD_CACHE_ELSE_NETWORK
                        else -> WebSettings.LOAD_DEFAULT
                    }
                    databaseEnabled = true
                    
                    // 媒体设置
                    mediaPlaybackRequiresUserGesture = true
                    
                    // User Agent设置
                    val userAgentValue = prefs.getString("user_agent", "default") ?: "default"
                    userAgentString = when (userAgentValue) {
                        "desktop_chrome" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        "mobile_chrome" -> "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36"
                        "ios_safari" -> "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
                        else -> "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36"
                    }
                    
                    // 混合内容设置（允许HTTPS页面加载HTTP资源）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    }
                    
                    // 硬件加速在WebView中默认启用，无需手动设置
                }
                
                // 如果WebView还没有加载URL，现在加载
                if (view.url.isNullOrEmpty()) {
                    view.loadUrl(url)
                }
                
                webView = view
            }
        )
    }
}
