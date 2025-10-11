package com.yhchat.canary.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.ShareData
import com.yhchat.canary.data.repository.ShareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 分享对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareDialog(
    chatId: String,
    chatType: Int,  // 1-用户，2-群聊，3-机器人
    chatName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { ShareViewModel() }
    
    LaunchedEffect(Unit) {
        viewModel.init(context)
        viewModel.createShareLink(chatId, chatType, chatName)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("分享${when(chatType) {
                1 -> "用户"
                2 -> "群聊"
                3 -> "机器人"
                else -> "对象"
            }}")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator()
                        Text("生成分享链接中...")
                    }
                    
                    uiState.error != null -> {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
            uiState.shareLink != null -> {
                val shareData = uiState.shareLink
                        
                        // 云湖内链
                        val chatTypeText = when(chatType) {
                            1 -> "user"
                            2 -> "group"
                            3 -> "bot"
                            else -> "unknown"
                        }
                        val yunhuLink = "yunhu://chat-add?id=$chatId&type=$chatTypeText"
                        
                        // 分享文本（带群ID）
                        val shareText = "访问链接加入云湖${when(chatType) {
                            1 -> "用户"
                            2 -> "群聊"
                            3 -> "机器人"
                            else -> "对象"
                        }}【$chatName】\n${shareData?.shareUrl}share?key=${shareData?.key}&ts=${shareData?.ts}\n${when(chatType) {
                            1 -> "用户ID"
                            2 -> "群ID"
                            3 -> "机器人ID"
                            else -> "ID"
                        }}: $chatId"
                        
                        // 显示分享图片
                        AsyncImage(
                            model = shareData?.getShareImageUrl(),
                            contentDescription = "分享图片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                        
                        // 分享链接（外部链接）
                        OutlinedTextField(
                            value = shareText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("分享链接（外部）") },
                            minLines = 3,
                            trailingIcon = {
                                IconButton(onClick = {
                                    copyToClipboard(context, shareText)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "复制外部链接"
                                    )
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 云湖内链
                        OutlinedTextField(
                            value = yunhuLink,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("云湖内链") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    copyToClipboard(context, yunhuLink)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "复制云湖内链"
                                    )
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 分享按钮
                        Button(
                            onClick = {
                                shareToSystem(context, shareText, chatName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "分享"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("分享到其他应用")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 复制到剪贴板
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("分享链接", text)
    clipboardManager.setPrimaryClip(clip)
    android.widget.Toast.makeText(context, "链接已复制", android.widget.Toast.LENGTH_SHORT).show()
}

/**
 * 分享到系统
 */
private fun shareToSystem(context: Context, shareUrl: String, chatName: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "加入我的$chatName：$shareUrl")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "分享到")
    context.startActivity(shareIntent)
}

/**
 * 分享ViewModel
 */
class ShareViewModel : ViewModel() {
    private lateinit var shareRepository: ShareRepository
    
    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()
    
    fun init(context: Context) {
        shareRepository = RepositoryFactory.getShareRepository(context)
    }
    
    fun createShareLink(chatId: String, chatType: Int, chatName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            shareRepository.createShareLink(chatId, chatType, chatName).fold(
                onSuccess = { shareResponse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        shareLink = shareResponse
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
}

data class ShareUiState(
    val isLoading: Boolean = false,
    val shareLink: com.yhchat.canary.data.model.ShareData? = null,
    val error: String? = null
)

