package com.yhchat.canary.ui.chat

import android.os.Bundle
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

@AndroidEntryPoint
class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chatId = intent.getStringExtra("chatId") ?: ""
        val chatType = intent.getIntExtra("chatType", 1)
        val chatName = intent.getStringExtra("chatName") ?: ""
        val userId = "" // 可根据实际需求传递
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
                        userId = userId,
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
}
