package com.yhchat.canary

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yhchat.canary.ui.login.LoginScreen
import com.yhchat.canary.ui.conversation.ConversationScreen
import com.yhchat.canary.ui.chat.ChatScreen
import com.yhchat.canary.ui.community.CommunityScreen
import com.yhchat.canary.ui.contacts.ContactsScreen
import com.yhchat.canary.ui.discover.DiscoverScreen
import com.yhchat.canary.ui.profile.ProfileScreen
import com.yhchat.canary.ui.search.SearchScreen
import com.yhchat.canary.ui.components.BottomNavigationBar
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import coil.ImageLoader
import coil.Coil
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 配置Coil ImageLoader，为chat-img.jwznb.com添加Referer
        val imageLoader = ImageLoader.Builder(this)
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
                        }?.onFailure {
                            // 如果获取用户信息失败，使用token的后8位作为userId
                            userId = "user_${loginToken.takeLast(8)}"
                            mainViewModel.onLoginSuccess(loginToken, userId)
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
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    else -> {
                        // 主界面，包含底部导航栏
                        Scaffold(
                            bottomBar = {
                                BottomNavigationBar(
                                    currentScreen = currentScreen,
                                    onScreenChange = { screen ->
                                        currentScreen = screen
                                    }
                                )
                            }
                        ) { paddingValues ->
                            when (currentScreen) {
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
                                        onMenuClick = {
                                            // 显示菜单
                                        },
                                        tokenRepository = tokenRepository,
                                        modifier = Modifier.padding(paddingValues)
                                    )
                                }
                                "community" -> {
                                    CommunityScreen(
                                        token = token,
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
                                        tokenRepository = tokenRepository
                                    )
                                }
                                "search" -> {
                                    if (isInitialized && tokenRepository != null) {
                                        SearchScreen(
                                            onBackClick = {
                                                currentScreen = "conversation"
                                            },
                                            onItemClick = { searchItem ->
                                                // 处理搜索项点击，可以跳转到聊天界面
                                                currentChatId = searchItem.friendId
                                                currentChatType = searchItem.friendType
                                                currentChatName = searchItem.nickname
                                                currentScreen = "chat"
                                            },
                                            tokenRepository = tokenRepository,
                                            modifier = Modifier.padding(paddingValues)
                                        )
                                    } else {
                                        // 数据库还未初始化，显示加载状态
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                                "chat" -> {
                                    if (currentChatId.isNotEmpty()) {
                                        ChatScreen(
                                            chatId = currentChatId,
                                            chatType = currentChatType,
                                            chatName = currentChatName,
                                            userId = userId,
                                            onBackClick = {
                                                currentScreen = "conversation"
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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