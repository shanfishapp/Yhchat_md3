package com.yhchat.canary.ui.chat

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    chatId: String,
    chatType: Int,
    chatName: String,
    viewModel: ChatInfoViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 复制分享内容到剪贴板
    val copyToClipboard = { shareUrl: String ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val shareContent = "访问链接加入云湖群聊【$chatName】\n$shareUrl\n群ID: $chatId"
        val clip = android.content.ClipData.newPlainText("分享链接", shareContent)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已复制分享内容到剪贴板", Toast.LENGTH_SHORT).show()
    }
    
    YhchatCanaryTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("聊天信息", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        // 分享按钮 - 所有类型都显示
                        IconButton(onClick = {
                            if (uiState.shareUrl != null) {
                                // 如果已有分享链接，直接复制
                                copyToClipboard(uiState.shareUrl!!)
                            } else {
                                // 创建分享链接
                                viewModel.createShare(chatId, chatType, chatName)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "分享"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.error != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = uiState.error ?: "加载失败",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.createShare(chatId, chatType, chatName) }) {
                                Text("重试")
                            }
                        }
                    }
                    else -> {
                        ChatInfoContent(
                            chatId = chatId,
                            chatType = chatType,
                            chatName = chatName,
                            shareUrl = uiState.shareUrl,
                            copyToClipboard = copyToClipboard
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInfoContent(
    chatId: String,
    chatType: Int,
    chatName: String,
    shareUrl: String?,
    copyToClipboard: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 显示聊天名称
        Text(
            text = chatName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 显示聊天类型
        val typeText = when (chatType) {
            1 -> "用户"
            2 -> "群聊"
            3 -> "机器人"
            else -> "未知"
        }
        
        Text(
            text = "类型: $typeText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        when (chatType) {
            2 -> {
                // 群聊 - 显示群聊信息占位符
                GroupInfoPlaceholder(chatId)
            }
            else -> {
                // 其他类型 - 显示占位屏
                OtherInfoPlaceholder()
            }
        }
        
        // 如果已有分享链接，显示复制按钮
        if (shareUrl != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { copyToClipboard(shareUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("复制分享链接")
            }
        }
    }
}

@Composable
private fun GroupInfoPlaceholder(chatId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "群聊信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            InfoRow("群ID", chatId)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("群成员", "0 人")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("群公告", "暂无公告")
        }
    }
}

@Composable
private fun OtherInfoPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "信息页面",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Text(
                text = "此页面用于显示用户或机器人的详细信息",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "功能正在开发中...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}