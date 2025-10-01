package com.yhchat.canary.ui.chat

import android.content.Intent
import android.os.Bundle
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

@AndroidEntryPoint
class ChatActivity : ComponentActivity() {
    
    // 使用状态管理会话参数，以便在 onNewIntent 中更新
    private var chatId by mutableStateOf("")
    private var chatType by mutableStateOf(1)
    private var chatName by mutableStateOf("")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 从Intent中读取参数
        updateChatParams(intent)
        
        setContent {
            YhchatCanaryTheme {
                val topBarColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
                val view = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                    window.statusBarColor = topBarColor
                }
                Surface(color = MaterialTheme.colorScheme.background) {
                    ChatScreen(
                        chatId = chatId,
                        chatType = chatType,
                        chatName = chatName,
                        userId = "",
                        onBackClick = { finish() },
                        onAvatarClick = { userId, userName, chatType ->
                            if (chatType != 3) {
                                com.yhchat.canary.ui.profile.UserProfileActivity.start(this@ChatActivity, userId, userName)
                            }
                        }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 当Activity被复用时，更新参数
        android.util.Log.d("ChatActivity", "onNewIntent called, updating chat params")
        updateChatParams(intent)
        // 更新当前Intent
        setIntent(intent)
    }
    
    private fun updateChatParams(intent: Intent) {
        val newChatId = intent.getStringExtra("chatId") ?: ""
        val newChatType = intent.getIntExtra("chatType", 1)
        val newChatName = intent.getStringExtra("chatName") ?: ""
        
        android.util.Log.d("ChatActivity", "Updating chat params: chatId=$newChatId, chatType=$newChatType, chatName=$newChatName")
        
        chatId = newChatId
        chatType = newChatType
        chatName = newChatName
    }
}
