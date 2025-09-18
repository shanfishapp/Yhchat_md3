package com.yhchat.canary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yhchat.canary.ui.login.LoginScreen
import com.yhchat.canary.ui.conversation.ConversationScreen
import com.yhchat.canary.ui.chat.ChatScreen
import com.yhchat.canary.ui.community.CommunityScreen
import com.yhchat.canary.ui.components.BottomNavigationBar
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import com.yhchat.canary.data.local.AppDatabase
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.UserRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.Coil
import coil.request.CachePolicy
import okhttp3.OkHttpClient

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
                var isLoggedIn by remember { mutableStateOf(false) }
                var token by remember { mutableStateOf("") }
                var userId by remember { mutableStateOf("") }
                var currentScreen by remember { mutableStateOf("conversation") }
                var currentChatId by remember { mutableStateOf("") }
                var currentChatType by remember { mutableStateOf(0) }
                var currentChatName by remember { mutableStateOf("") }
                var tokenRepository by remember { mutableStateOf<TokenRepository?>(null) }
                var userRepository by remember { mutableStateOf<UserRepository?>(null) }
                var pendingLoginToken by remember { mutableStateOf<String?>(null) }
                
                // 获取上下文
                val context = LocalContext.current
                
                // 初始化数据库
                LaunchedEffect(Unit) {
                    val database = AppDatabase.getDatabase(context)
                    tokenRepository = TokenRepository(database.userTokenDao())
                    userRepository = UserRepository().apply { 
                        setTokenRepository(tokenRepository!!) 
                    }
                    
                    // 检查是否已登录
                    val savedToken = tokenRepository?.getTokenSync()
                    if (savedToken != null) {
                        token = savedToken
                        // 获取用户信息
                        userRepository?.getUserInfo()?.onSuccess { user ->
                            userId = user.id
                        }?.onFailure {
                            // 如果获取用户信息失败，使用token的后8位作为userId
                            userId = "user_${savedToken.takeLast(8)}"
                        }
                        isLoggedIn = true
                    }
                }
                
                // 处理登录后的用户信息获取
                LaunchedEffect(pendingLoginToken) {
                    pendingLoginToken?.let { loginToken ->
                        userRepository?.getUserInfo()?.onSuccess { user ->
                            userId = user.id
                        }?.onFailure {
                            // 如果获取用户信息失败，保持当前的userId
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
                                isLoggedIn = true
                            },
                            tokenRepository = tokenRepository
                        )
                    }
                    currentScreen == "chat" -> {
                        // 显示聊天界面
                        ChatScreen(
                            token = token,
                            chatId = currentChatId,
                            chatType = currentChatType,
                            chatName = currentChatName,
                            onBackClick = {
                                currentScreen = "conversation"
                            },
                            onMenuClick = {
                                // 显示聊天菜单
                            },
                            tokenRepository = tokenRepository
                        )
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
                                        onConversationClick = { chatId, chatType ->
                                            currentChatId = chatId
                                            currentChatType = chatType
                                            currentChatName = "聊天" // 这里应该从会话数据中获取名称
                                            currentScreen = "chat"
                                        },
                                        onSearchClick = {
                                            // 跳转到搜索界面
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