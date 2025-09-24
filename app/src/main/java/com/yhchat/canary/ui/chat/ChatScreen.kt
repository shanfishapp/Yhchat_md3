package com.yhchat.canary.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.yhchat.canary.ui.components.MarkdownText
import com.yhchat.canary.ui.components.HtmlWebView
import com.yhchat.canary.ui.components.ChatInputBar
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.ui.components.ImageViewer
import com.yhchat.canary.ui.components.LinkText
import com.yhchat.canary.ui.components.LinkDetector
import com.yhchat.canary.data.model.ChatMessage
import com.yhchat.canary.data.model.MessageContent
import com.yhchat.canary.service.AudioPlayerService
import com.yhchat.canary.service.FileDownloadService
import com.yhchat.canary.utils.PermissionUtils
import android.app.Activity
import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

/**
 * 聊天界面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ChatScreen(
    chatId: String,
    chatType: Int,
    chatName: String,
    userId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    onAvatarClick: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // 图片预览状态
    var showImageViewer by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf("") }
    
    // 初始化聊天
    LaunchedEffect(chatId, chatType, userId) {
        viewModel.initChat(chatId, chatType, userId)
    }

    // 处理系统返回键/手势返回
    BackHandler {
        onBackClick()
    }

    // 下拉刷新状态
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.loadMoreMessages() }
    )
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = chatName,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        // 错误信息
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text("关闭")
                    }
                }
            }
        }

        // 消息列表（占据中间可用空间）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pullRefresh(pullRefreshState)
        ) {
            if (uiState.isLoading && messages.isEmpty()) {
                // 初始加载状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true // 最新消息在底部
                ) {
                    items(
                        items = messages.reversed(), // 反转显示顺序
                        key = { it.msgId }
                    ) { message ->
                        MessageItem(
                            message = message,
                            isMyMessage = viewModel.isMyMessage(message),
                            modifier = Modifier.fillMaxWidth(),
                            onImageClick = { imageUrl ->
                                currentImageUrl = imageUrl
                                showImageViewer = true
                            },
                            onAvatarClick = onAvatarClick
                        )
                    }

                    // 加载更多指示器
                    if (messages.isNotEmpty()) {
                        item {
                            LaunchedEffect(Unit) {
                                viewModel.loadMoreMessages()
                            }

                            if (uiState.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 空状态
                    if (messages.isEmpty() && !uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无消息\n开始对话吧",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 下拉刷新指示器
            PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            // 下拉刷新指示器
            PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // 底部输入栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSendMessage = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendTextMessage(inputText.trim())
                        inputText = ""
                    }
                },
                onImageClick = {
                    // TODO: 实现图片选择功能
                },
                onFileClick = {
                    // TODO: 实现文件选择功能
                },
                onCameraClick = {
                    // TODO: 实现相机拍照功能
                },
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 16.dp // 增加底部间距，避免粘在最底部
                )
            )
        }
    }
    
    // 图片预览器
    if (showImageViewer && currentImageUrl.isNotEmpty()) {
        ImageViewer(
            imageUrl = currentImageUrl,
            onDismiss = {
                showImageViewer = false
                currentImageUrl = ""
            }
        )
    }
}

/**
 * 消息项
 */
@Composable
private fun MessageItem(
    message: ChatMessage,
    isMyMessage: Boolean,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit = {},
    onAvatarClick: (String, String) -> Unit = { _, _ -> }
) {
    // 检查是否为撤回消息
    if (message.msgDeleteTime != null) {
        // 撤回消息显示
        RecallMessageItem(
            message = message,
            modifier = modifier
        )
        return
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = if (isMyMessage) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        if (!isMyMessage) {
            // 发送者头像
            AsyncImage(
                model = ImageUtils.createAvatarImageRequest(
                    context = LocalContext.current,
                    url = message.sender.avatarUrl
                ),
                contentDescription = message.sender.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        onAvatarClick(message.sender.chatId, message.sender.name)
                    },
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isMyMessage) {
                Alignment.End
            } else {
                Alignment.Start
            }
        ) {
            // 发送者姓名（非自己的消息）
            if (!isMyMessage) {
                Text(
                    text = message.sender.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            // 消息气泡
            Surface(
                modifier = Modifier.clip(
                    RoundedCornerShape(
                        topStart = if (isMyMessage) 16.dp else 4.dp,
                        topEnd = if (isMyMessage) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                ),
                color = if (isMyMessage) {
                        MaterialTheme.colorScheme.primary 
                } else {
                        MaterialTheme.colorScheme.surface
                },
                tonalElevation = if (isMyMessage) {
                    0.dp  // 自己的消息使用纯色
                } else {
                    2.dp  // 对方的消息使用浅色高程
                }
            ) {
                MessageContentView(
                    content = message.content,
                    contentType = message.contentType,
                    isMyMessage = isMyMessage,
                    modifier = Modifier.padding(12.dp),
                    onImageClick = onImageClick
                )
            }

            // 时间戳
            Text(
                text = formatTimestamp(message.sendTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        if (isMyMessage) {
            Spacer(modifier = Modifier.width(8.dp))

            // 自己的头像
            AsyncImage(
                model = ImageUtils.createAvatarImageRequest(
                    context = LocalContext.current,
                    url = message.sender.avatarUrl
                ),
                contentDescription = "我",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        onAvatarClick(message.sender.chatId, message.sender.name)
                    },
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * 撤回消息项
 */
@Composable
private fun RecallMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .widthIn(max = 280.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 1.dp
        ) {
            Text(
                text = "${message.sender.name} 在 ${formatRecallTime(message.msgDeleteTime!!)} 撤回了一条消息",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * 消息内容视图
 */
@Composable
private fun MessageContentView(
    content: MessageContent,
    contentType: Int,
    isMyMessage: Boolean,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit = {}
) {
    val textColor = if (isMyMessage) {
                                MaterialTheme.colorScheme.onPrimary 
    } else {
                                MaterialTheme.colorScheme.onSurface
    }
    val context = LocalContext.current

    Column(modifier = modifier) {
        when (contentType) {
            1 -> {
                // 文本消息
                content.text?.let { text ->
                    if (LinkDetector.containsLink(text)) {
                        // 包含链接的文本
                        LinkText(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                            linkColor = if (isMyMessage) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    } else {
                        // 普通文本
                        Text(
                            text = text,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            2 -> {
                // 图片消息
                content.imageUrl?.let { imageUrl ->
                        AsyncImage(
                        model = ImageUtils.createImageRequest(
                            context = LocalContext.current,
                            url = imageUrl
                        ),
                            contentDescription = "图片",
                            modifier = Modifier
                                .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(imageUrl) },
                            contentScale = ContentScale.Crop
                        )
                    }
                content.text?.let { text ->
                    if (text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = text,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            4 -> {
                // 文件消息
                content.fileName?.let { fileName ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                content.fileUrl?.let { fileUrl ->
                                    handleFileDownload(
                                        context = context,
                                        fileUrl = fileUrl,
                                        fileName = fileName,
                                        fileSize = content.fileSize ?: 0L
                                    )
                                }
                            },
                        color = textColor.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send, // 用作文件图标的临时替代
                                contentDescription = "文件",
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fileName,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                content.fileSize?.let { size ->
                                    Text(
                                        text = formatFileSize(size),
                                        color = textColor.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "下载",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            11 -> {
                // 语音消息
                content.audioUrl?.let { audioUrl ->
                    AudioMessageView(
                        audioUrl = audioUrl,
                        duration = content.audioTime ?: 0,
                        textColor = textColor,
                        senderName = "语音消息"
                    )
                }
            }
            3 -> {
                // Markdown消息
                content.text?.let { markdownText ->
                    MarkdownText(
                        markdown = markdownText,
                        textColor = if (isMyMessage) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            8 -> {
                // HTML消息
                content.text?.let { htmlContent ->
                    HtmlWebView(
                        htmlContent = htmlContent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            7 -> {
                // 表情消息 (包括表情包和个人收藏表情)
                // 根据示例，contentType: 7 统一处理表情消息，直接使用 imageUrl
                content.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = ImageUtils.createStickerImageRequest(
                            context = LocalContext.current,
                            url = imageUrl
                        ),
                        contentDescription = when {
                            content.expressionId != null && content.expressionId != "0" -> "个人收藏表情"
                            content.stickerPackId != null -> "表情包"
                            else -> "表情"
                        },
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                } ?: run {
                    // 如果没有 imageUrl，尝试使用 stickerUrl 拼接完整URL
                    content.stickerUrl?.let { stickerUrl ->
                        val fullUrl = if (stickerUrl.startsWith("http")) {
                            stickerUrl
                        } else {
                            "https://chat-img.jwznb.com/$stickerUrl"
                        }
                        
                        AsyncImage(
                            model = ImageUtils.createStickerImageRequest(
                                context = LocalContext.current,
                                url = fullUrl
                            ),
                            contentDescription = "表情",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            25 -> {
                // 兼容旧的表情消息类型 (如果还有使用的话)
                content.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = ImageUtils.createStickerImageRequest(
                            context = LocalContext.current,
                            url = imageUrl
                        ),
                        contentDescription = "表情",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            26 -> {
                // 兼容旧的表情包消息类型 (如果还有使用的话)
                content.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = ImageUtils.createStickerImageRequest(
                            context = LocalContext.current,
                            url = imageUrl
                        ),
                        contentDescription = "表情包",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            19 -> {
                // 视频消息 - 已移除视频播放功能，显示提示文本
                content.videoUrl?.let { videoPath ->
                    Text(
                        text = "📹 视频消息 (暂不支持播放)",
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
                    else -> {
                // 其他类型消息，显示文本内容
                content.text?.let { text ->
                    if (LinkDetector.containsLink(text)) {
                        // 包含链接的文本
                        LinkText(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                            linkColor = if (isMyMessage) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    } else {
                        // 普通文本
                        Text(
                            text = text,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 引用消息
        content.quoteMsgText?.let { quoteText ->
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                color = textColor.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 引用消息的图片（如果有）
                    content.quoteImageUrl?.let { imageUrl ->
                        AsyncImage(
                            model = ImageUtils.createImageRequest(
                                context = LocalContext.current,
                                url = imageUrl
                            ),
                            contentDescription = "引用图片",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onImageClick(imageUrl) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    // 引用消息文本
                    Text(
                        text = quoteText,
                        color = textColor.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val calendar = Calendar.getInstance()
    
    val todayCalendar = Calendar.getInstance().apply {
        time = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    return when {
        date.after(todayCalendar.time) -> {
            // 今天 - 只显示时间
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        date.after(Date(todayCalendar.timeInMillis - 86400000)) -> {
            // 昨天
            "昨天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        else -> {
            // 更早 - 显示日期
            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date)
        }
    }
}

/**
 * 格式化撤回时间（只显示时:分）
 */
private fun formatRecallTime(timestamp: Long): String {
    val date = Date(timestamp)
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
        else -> "${bytes / (1024 * 1024 * 1024)}GB"
    }
}

/**
 * 格式化音频时长
 */
private fun formatAudioDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes}:${remainingSeconds.toString().padStart(2, '0')}"
}

private fun handleFileDownload(
    context: Context,
    fileUrl: String,
    fileName: String,
    fileSize: Long
) {
    if (!PermissionUtils.hasAllDownloadPermissions(context)) {
        if (context is Activity) {
            PermissionUtils.requestAllDownloadPermissions(context)
            Toast.makeText(context, "请先授予下载所需权限", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "当前上下文无法申请权限", Toast.LENGTH_SHORT).show()
        }
        return
    }

    Toast.makeText(context, "开始下载：$fileName", Toast.LENGTH_SHORT).show()
    FileDownloadService.startDownload(
        context = context,
        fileUrl = fileUrl,
        fileName = fileName,
        fileSize = fileSize
    )
}

/**
 * 语音消息视图
 */
@Composable
private fun AudioMessageView(
    audioUrl: String,
    duration: Long,
    textColor: Color,
    senderName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCurrentlyPlaying by remember { mutableStateOf(false) }
    
    // 检查当前是否正在播放这个音频
    LaunchedEffect(audioUrl) {
        // 这里可以添加检查当前播放状态的逻辑
        // 暂时简化处理
    }
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                // 点击播放语音
                AudioPlayerService.startPlayAudio(
                    context = context,
                    audioUrl = audioUrl,
                    title = "$senderName 的语音"
                )
                isCurrentlyPlaying = !isCurrentlyPlaying
            },
        color = textColor.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 播放/暂停图标
            Icon(
                imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isCurrentlyPlaying) "暂停" else "播放",
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 音频波形效果 (简化版本)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    val height = if (isCurrentlyPlaying) {
                        // 简单的动画效果
                        (8 + (index * 2)).dp
                    } else {
                        6.dp
                    }
                    
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(height)
                            .background(
                                textColor.copy(alpha = 0.6f),
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 时长显示
            Text(
                text = formatAudioDuration(duration),
                color = textColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}