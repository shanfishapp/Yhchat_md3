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
    
    // 图片选择器
    private val imagePickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            android.util.Log.d("ChatActivity", "图片已选择: $selectedUri")
            // 通过Intent传递给ChatScreen处理
            imageUriToSend = selectedUri
        }
    }
    
    private var imageUriToSend by mutableStateOf<android.net.Uri?>(null)
    
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
                        onAvatarClick = { userId, userName, chatType, currentUserPermission ->
                            if (chatType != 3) {
                                // 如果是群聊环境，传递群聊信息和当前用户权限
                                val isGroupAdmin = currentUserPermission >= 2
                                com.yhchat.canary.ui.profile.UserProfileActivity.start(
                                    context = this@ChatActivity, 
                                    userId = userId, 
                                    userName = userName,
                                    groupId = if (this@ChatActivity.chatType == 2) this@ChatActivity.chatId else null,
                                    isGroupAdmin = isGroupAdmin
                                )
                            }
                        },
                        onImagePickerClick = {
                            // 启动图片选择器
                            imagePickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        imageUriToSend = imageUriToSend,
                        onImageSent = {
                            // 图片发送后清空
                            imageUriToSend = null
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
