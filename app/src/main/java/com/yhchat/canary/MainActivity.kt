package com.yhchat.canary

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yhchat.canary.ui.login.LoginScreen
import com.yhchat.canary.ui.conversation.ConversationScreen
import com.yhchat.canary.ui.chat.ChatScreen
import com.yhchat.canary.ui.community.CommunityScreen
import com.yhchat.canary.ui.contacts.ContactsScreen
import com.yhchat.canary.ui.discover.DiscoverScreen
import com.yhchat.canary.ui.profile.ProfileScreen
import com.yhchat.canary.ui.search.SearchScreen
import com.yhchat.canary.ui.components.BottomNavigationBar
import com.yhchat.canary.ui.components.ScrollBehavior
import com.yhchat.canary.ui.components.rememberScrollBehavior
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import coil.ImageLoader
import coil.Coil
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhchat.canary.ui.conversation.ConversationViewModel
import com.yhchat.canary.ui.profile.UserProfileActivity
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.NavigationItem
import com.yhchat.canary.ui.chat.ChatAddActivity
import com.yhchat.canary.utils.ChatAddLinkHandler
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 应用字体大小设置
        applyFontScale()
        
        // 处理 Deep Link
        handleDeepLink()
        
        // 配置Coil ImageLoader，为chat-img.jwznb.com添加Referer，支持GIF和WebP
        val imageLoader = ImageLoader.Builder(this)
            .components {
                // 添加GIF支持
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(coil.decode.ImageDecoderDecoder.Factory())
                } else {
                    add(coil.decode.GifDecoder.Factory())
                }
                // WebP支持已经内置在Coil中
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request()
                        val url = request.url.toString()
                        
                        // 为chat-img.jwznb.com的请求添加Referer
                        if (url.contains("chat-img.jwznb.com")) {
                            val newRequest = request.newBuilder()
                                .addHeader("Referer", "https://myapp.jwznb.com")
                                .build()
                            chain.proceed(newRequest)
                        } else {
                            chain.proceed(request)
                        }
                    }
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        
        Coil.setImageLoader(imageLoader)
        setContent {
            YhchatCanaryTheme {
                // 使用MainViewModel
                val mainViewModel: MainViewModel = viewModel()
                val isInitialized by mainViewModel.isInitialized.collectAsStateWithLifecycle()
                val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()
                val savedToken by mainViewModel.savedToken.collectAsStateWithLifecycle()
                val savedUserId by mainViewModel.userId.collectAsStateWithLifecycle()
                val tokenRepository by mainViewModel.tokenRepositoryState.collectAsStateWithLifecycle()
                val userRepository by mainViewModel.userRepositoryState.collectAsStateWithLifecycle()

                // 本地状态
                var token by remember { mutableStateOf(savedToken ?: "") }
                var userId by remember { mutableStateOf(savedUserId ?: "") }
                var currentScreen by remember { mutableStateOf("conversation") }
                var currentChatId by remember { mutableStateOf("") }
                var currentChatType by remember { mutableStateOf(0) }
                var currentChatName by remember { mutableStateOf("") }
                var pendingLoginToken by remember { mutableStateOf<String?>(null) }
                
                // 保持ConversationScreen的ViewModel状态，避免重新创建
                val conversationViewModel: ConversationViewModel = viewModel()
                
                // 导航配置
                val navigationRepository = remember { RepositoryFactory.getNavigationRepository(this@MainActivity) }
                val navigationConfig by navigationRepository.navigationConfig.collectAsStateWithLifecycle()
                val visibleNavItems = navigationConfig.getVisibleItems()

                // 同步ViewModel状态到本地状态
                LaunchedEffect(savedToken) {
                    savedToken?.let { token = it }
                }
                LaunchedEffect(savedUserId) {
                    savedUserId?.let { userId = it }
                }

                // 处理登录后的用户信息获取
                LaunchedEffect(pendingLoginToken) {
                    pendingLoginToken?.let { loginToken ->
                        userRepository?.getUserInfo()?.onSuccess { user ->
                            userId = user.id
                            mainViewModel.onLoginSuccess(loginToken, user.id)
                        }?.onFailure { error ->
                            // 获取用户信息失败，显示错误并保持登录状态无效
                            println("获取用户信息失败: ${error.message}")
                            // 不设置userId，保持登录失败状态
                        }
                        pendingLoginToken = null
                    }
                }
                
                when {
                    !isLoggedIn -> {
                        // 未登录，显示登录界面
                        LoginScreen(
                            onLoginSuccess = { loginToken, loginUserId ->
                                token = loginToken
                                pendingLoginToken = loginToken
                                userId = loginUserId
                                mainViewModel.onLoginSuccess(loginToken, loginUserId)
                            },
                            tokenRepository = tokenRepository
                        )
                    }
                    currentScreen == "chat" -> {
                        // 显示聊天界面
                        if (currentChatId.isNotEmpty()) {
                            ChatScreen(
                                chatId = currentChatId,
                                chatType = currentChatType,
                                chatName = currentChatName,
                                userId = userId,
                                onBackClick = {
                                    currentScreen = "conversation"
                                },
                                onAvatarClick =
                                    { userId, userName, chatType, currentUserPermission ->
                                    val isGroupAdmin = currentUserPermission >= 2
                                    val groupId = if (chatType == 2) currentChatId else null
                                    UserProfileActivity.start(
                                        this@MainActivity,
                                        userId,
                                        userName,
                                        groupId,
                                        isGroupAdmin
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    else -> {
                        // 主界面，包含底部导航栏和HorizontalPager
                        val coroutineScope = rememberCoroutineScope()
                        val pagerState = rememberPagerState { visibleNavItems.size }
                        val scrollBehavior = rememberScrollBehavior()
                        
                        // 获取当前页面对应的导航项ID
                        val currentPageItem = if (visibleNavItems.isNotEmpty() && pagerState.currentPage < visibleNavItems.size) {
                            visibleNavItems[pagerState.currentPage].id
                        } else {
                            currentScreen
                        }
                        
                        // 优化后的页面同步逻辑
                        LaunchedEffect(currentScreen, visibleNavItems) {
                            val targetIndex = visibleNavItems.indexOfFirst { it.id == currentScreen }
                            if (targetIndex >= 0 && targetIndex != pagerState.currentPage && !pagerState.isScrollInProgress) {
                                pagerState.scrollToPage(targetIndex)
                            }
                        }
                        
                        LaunchedEffect(pagerState.currentPage, visibleNavItems) {
                            if (visibleNavItems.isNotEmpty() && pagerState.currentPage < visibleNavItems.size) {
                                val newScreen = visibleNavItems[pagerState.currentPage].id
                                if (newScreen != currentScreen && !pagerState.isScrollInProgress) {
                                    currentScreen = newScreen
                                }
                            }
                        }
                        
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // 主内容区域（全屏显示，不使用paddingValues）
                            val paddingValues = PaddingValues(0.dp)
                            if (visibleNavItems.isNotEmpty()) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    val navItem = visibleNavItems[page]
                                    when (navItem.id) {
                                        "conversation" -> {
                                            ConversationScreen(
                                                token = token,
                                                userId = userId,
                                                onConversationClick = { chatId, chatType, chatName ->
                                                    currentChatId = chatId
                                                    currentChatType = chatType
                                                    currentChatName = chatName
                                                    currentScreen = "chat"
                                                },
                                                onSearchClick = {
                                                    if (isInitialized) {
                                                        currentScreen = "search"
                                                    }
                                                },
                                                onMenuClick = { },
                                                tokenRepository = tokenRepository,
                                                viewModel = conversationViewModel,
                                                scrollBehavior = scrollBehavior,
                                                modifier = Modifier.padding(paddingValues)
                                            )
                                        }
                                        "community" -> {
                                            CommunityScreen(
                                                token = token,
                                                scrollBehavior = scrollBehavior,
                                                modifier = Modifier.padding(paddingValues)
                                            )
                                        }
                                        "contacts" -> {
                                            ContactsScreen(
                                                modifier = Modifier.padding(paddingValues)
                                            )
                                        }
                                        "discover" -> {
                                            DiscoverScreen(
                                                modifier = Modifier.padding(paddingValues)
                                            )
                                        }
                                        "profile" -> {
                                            ProfileScreen(
                                                modifier = Modifier.padding(paddingValues),
                                                userRepository = userRepository,
                                                tokenRepository = tokenRepository,
                                                navigationRepository = navigationRepository
                                            )
                                        }
                                    }
                                }
                            } else {
                                // 没有可见的导航项时显示空状态
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("666全关了是吗，自己清数据解决。")
                                }
                            }
                            
                            // 处理特殊页面（如chat和search）
                            if (currentScreen == "chat" && currentChatId.isNotEmpty()) {
                                ChatScreen(
                                    chatId = currentChatId,
                                    chatType = currentChatType,
                                    chatName = currentChatName,
                                    userId = userId,
                                    onBackClick = {
                                        currentScreen = "conversation"
                                    },
                                    onAvatarClick = { userId, userName, chatType, currentUserPermission ->
                                        val isGroupAdmin = currentUserPermission >= 2
                                        val groupId = if (chatType == 2) currentChatId else null
                                        UserProfileActivity.start(
                                            this@MainActivity,
                                            userId,
                                            userName,
                                            groupId,
                                            isGroupAdmin
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (currentScreen == "search") {
                                if (isInitialized && tokenRepository != null) {
                                    SearchScreen(
                                        onBackClick = {
                                            currentScreen = "conversation"
                                        },
                                        onItemClick = { chatId, chatType, chatName ->
                                            // 处理搜索项点击，跳转到聊天界面
                                            currentChatId = chatId
                                            currentChatType = chatType
                                            currentChatName = chatName
                                            currentScreen = "chat"
                                        },
                                        tokenRepository = tokenRepository,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            
                            // 底部导航栏（浮动在内容上方）
                            BottomNavigationBar(
                                currentScreen = currentPageItem,
                                visibleItems = visibleNavItems,
                                onScreenChange = { screen ->
                                    val targetIndex = visibleNavItems.indexOfFirst { it.id == screen }
                                    if (targetIndex >= 0) {
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(targetIndex)
                                        }
                                    }
                                    currentScreen = screen
                                },
                                isVisible = scrollBehavior.isVisible.value,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 处理 Deep Link
     */
    private fun handleDeepLink() {
        intent?.data?.toString()?.let { uriString ->
            if (ChatAddLinkHandler.isChatAddLink(uriString)) {
                // 延迟执行，确保 Activity 初始化完成
                window.decorView.post {
                    ChatAddActivity.start(this, uriString)
                }
            }
        }
    }
    
    /**
     * 应用字体大小设置
     */
    private fun applyFontScale() {
        val prefs = getSharedPreferences("display_settings", MODE_PRIVATE)
        val fontScale = prefs.getFloat("font_scale", 100f)
        
        // 将百分比转换为系统字体缩放因子 (1-100% -> 0.01-1.0)
        val scaleFactor = fontScale / 100f
        
        // 应用字体缩放
        val configuration = resources.configuration
        configuration.fontScale = scaleFactor
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}


@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Text(
        text = "欢迎使用云湖聊天！",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    YhchatCanaryTheme {
        MainScreen()
    }
}