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
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

@AndroidEntryPoint
class ChatActivity : ComponentActivity() {
    
    // 使用状态管理会话参数，以便在 onNewIntent 中更新
    private var chatId by mutableStateOf("")
    private var chatType by mutableStateOf(1)
    private var chatName by mutableStateOf("")
    
    // 图片选择器 - 使用与 ChatBackgroundActivity 相同的 API
    private val imagePickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            android.util.Log.d("ChatActivity", "图片已选择: $selectedUri")
            imageUriToSend = selectedUri
        }
    }
    
    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            android.util.Log.d("ChatActivity", "📁 文件已选择: $selectedUri")
            fileUriToSend = selectedUri
        }
    }
    
    // 相机拍照
    private var cameraImageUri by mutableStateOf<android.net.Uri?>(null)
    private val cameraLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                android.util.Log.d("ChatActivity", "拍照成功: $uri")
                imageUriToSend = uri
            }
        }
    }
    
    private var imageUriToSend by mutableStateOf<android.net.Uri?>(null)
    private var fileUriToSend by mutableStateOf<android.net.Uri?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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
                            // 启动图片选择器 - 使用与 ChatBackgroundActivity 相同的 API
                            imagePickerLauncher.launch("image/*")
                        },
                        onCameraClick = {
                            // 启动相机拍照
                            val photoFile = java.io.File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                            cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
                                this@ChatActivity,
                                "${packageName}.fileprovider",
                                photoFile
                            )
                            cameraLauncher.launch(cameraImageUri)
                        },
                        onFilePickerClick = {
                            // 启动文件选择器 - 选择所有类型文件
                            android.util.Log.d("ChatActivity", "📁 启动文件选择器")
                            filePickerLauncher.launch("*/*")
                        },
                        imageUriToSend = imageUriToSend,
                        fileUriToSend = fileUriToSend,
                        onImageSent = {
                            // 图片发送后清空
                            imageUriToSend = null
                            cameraImageUri = null
                        },
                        onFileSent = {
                            // 文件发送后清空
                            android.util.Log.d("ChatActivity", "📁 文件发送完成，清空URI")
                            fileUriToSend = null
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
