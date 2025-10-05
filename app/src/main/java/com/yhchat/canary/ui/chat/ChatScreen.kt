package com.yhchat.canary.ui.chat

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.yhchat.canary.ui.bot.BotInfoActivity
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import com.yhchat.canary.ui.community.PostDetailActivity
import androidx.compose.foundation.border
import org.json.JSONArray
import org.json.JSONObject
// pointerInput 相关扩展函数无需单独 import，consume 已废弃

/**
 * 聊天界面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    chatId: String,
    chatType: Int,
    chatName: String,
    userId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    onAvatarClick: (String, String, Int) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages
    var inputText by remember { mutableStateOf("") }
    var selectedMessageType by remember { mutableStateOf(1) } // 1-文本, 3-Markdown, 8-HTML
    val listState = rememberLazyListState()
    
    // 图片预览状态
    var showImageViewer by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf("") }
    
    // 滚动到底部按钮状态
    var showScrollToBottomButton by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // 初始化聊天
    LaunchedEffect(chatId, chatType, userId) {
        viewModel.initChat(chatId, chatType, userId)
    }
    
    // 监听滚动状态，当不在底部时显示"回到最新消息"按钮
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        // 当用户滚动查看历史消息时（不在最新消息位置），显示回到底部按钮
        // 因为是 reverseLayout，第一个可见项目的索引大于0表示不在最新消息位置
        showScrollToBottomButton = listState.firstVisibleItemIndex > 0 || 
                                   (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset > 100)
    }
    
    // WebSocket新消息不再自动滚动，用户可以自己滑动查看
    LaunchedEffect(uiState.newMessageReceived) {
        if (uiState.newMessageReceived) {
            // 重置新消息标记，但不进行任何滚动操作
            viewModel.resetNewMessageFlag()
        }
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
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
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
            actions = {
                // 群聊信息菜单（只在群聊时显示）
                if (chatType == 2) {
                    IconButton(onClick = {
                        android.util.Log.d("ChatScreen", "Opening group info: chatId=$chatId, chatName=$chatName")
                        val intent = Intent(context, com.yhchat.canary.ui.group.GroupInfoActivity::class.java)
                        intent.putExtra(com.yhchat.canary.ui.group.GroupInfoActivity.EXTRA_GROUP_ID, chatId)
                        intent.putExtra(com.yhchat.canary.ui.group.GroupInfoActivity.EXTRA_GROUP_NAME, chatName)
                        android.util.Log.d("ChatScreen", "Intent extras: groupId=${intent.getStringExtra(com.yhchat.canary.ui.group.GroupInfoActivity.EXTRA_GROUP_ID)}, groupName=${intent.getStringExtra(com.yhchat.canary.ui.group.GroupInfoActivity.EXTRA_GROUP_NAME)}")
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "群聊信息"
                        )
                    }
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
                    val reversedMessages = messages.reversed()
                    items(
                        count = reversedMessages.size,
                        key = { index -> 
                            // 使用索引和msgId组合作为key，确保唯一性
                            "${reversedMessages[index].msgId}_$index"
                        }
                    ) { index ->
                        val message = reversedMessages[index]
                        MessageItem(
                            message = message,
                            isMyMessage = viewModel.isMyMessage(message),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(),
                            onImageClick = { imageUrl ->
                                currentImageUrl = imageUrl
                                showImageViewer = true
                            },
                            onAvatarClick = { chatId, name, chatType ->
                                // 处理头像点击事件
                                if (chatType == 3) { // 机器人
                                    val intent = Intent(context, BotInfoActivity::class.java).apply {
                                        putExtra(BotInfoActivity.EXTRA_BOT_ID, chatId)
                                        putExtra(BotInfoActivity.EXTRA_BOT_NAME, name)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    // 用户头像点击，传递给外部处理（UserProfileActivity）
                                    onAvatarClick(chatId, name, chatType)
                                }
                            },
            onAddExpression = viewModel::addExpressionToFavorites
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
            
            // "回到最新消息"浮动按钮
            androidx.compose.animation.AnimatedVisibility(
                visible = showScrollToBottomButton,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            // 滚动到最新消息（索引0，因为是 reverseLayout）
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "回到最新消息"
                    )
                }
            }
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
                        // 根据选择的消息类型发送消息
                        viewModel.sendMessage(inputText.trim(), selectedMessageType)
                        inputText = ""
                        // 发送后重置为文本类型
                        selectedMessageType = 1
                        // 发送消息后自动滚动到最新消息
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                },
                onImageClick = {
                    // TODO: 实现图片选择功能
                },
                onFileClick = {
                    // TODO: 实现文件选择功能
                },
                onDraftChange = { draftText ->
                    viewModel.sendDraftInput(draftText)
                },
                onCameraClick = {
                    // TODO: 实现相机拍照功能
                },
                selectedMessageType = selectedMessageType,
                onMessageTypeChange = { newType ->
                    // 只能选择一个类型，点击已选中的类型则取消（回到文本）
                    selectedMessageType = if (selectedMessageType == newType) 1 else newType
                },
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp // 增加底部间距，避免粘在最底部
                )
            )
        }
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageItem(
    message: ChatMessage,
    isMyMessage: Boolean,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit = {},
    onAvatarClick: (String, String, Int) -> Unit = { _, _, _ -> },
    onAddExpression: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    var showContextMenu by remember { mutableStateOf(false) }
    
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
        modifier = modifier
            .combinedClickable(
                onClick = {}, // 单击不做任何事
                onDoubleClick = {
                    // 双击复制消息文本
                    val textToCopy = message.content.text ?: ""
                    if (textToCopy.isNotEmpty()) {
                        val clip = android.content.ClipData.newPlainText("message", textToCopy)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = {
                    // 长按显示菜单
                    showContextMenu = true
                }
            ),
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
                        onAvatarClick(message.sender.chatId, message.sender.name, message.sender.chatType)
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
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = message.sender.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 机器人标签
                    if (message.sender.chatType == 3) {
                        Surface(
                            modifier = Modifier,
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "机器人",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
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
                    message = message,
                    content = message.content,
                    contentType = message.contentType,
                    isMyMessage = isMyMessage,
                    modifier = Modifier.padding(12.dp),
                    onImageClick = onImageClick
                )
            }

            // 时间戳和编辑状态
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
            Text(
                text = formatTimestamp(message.sendTime),
                style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 如果消息被编辑过，显示"已编辑"标记
                if (message.editTime != null && message.editTime > 0) {
                    var showEditHistory by remember { mutableStateOf(false) }
                    
                    Row(
                        modifier = Modifier.clickable { showEditHistory = true },
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已编辑",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "查看编辑历史",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    
                    // 编辑历史弹窗
                    if (showEditHistory) {
                        EditHistoryDialog(
                            msgId = message.msgId,
                            onDismiss = { showEditHistory = false }
                        )
                    }
                }
            }
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
                        onAvatarClick(message.sender.chatId, message.sender.name, message.sender.chatType)
                    },
                contentScale = ContentScale.Crop
            )
        }
    }
    
    // 长按菜单
    if (showContextMenu) {
        MessageContextMenu(
            message = message,
            onDismiss = { showContextMenu = false },
            onCopyAll = {
                val textToCopy = message.content.text ?: ""
                if (textToCopy.isNotEmpty()) {
                    val clip = android.content.ClipData.newPlainText("message", textToCopy)
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(context, "已复制全部", Toast.LENGTH_SHORT).show()
                }
                showContextMenu = false
            },
            onQuote = {
                // TODO: 实现引用功能
                Toast.makeText(context, "引用功能开发中", Toast.LENGTH_SHORT).show()
                showContextMenu = false
            },
            onRecall = {
                // TODO: 实现撤回功能
                Toast.makeText(context, "撤回功能开发中", Toast.LENGTH_SHORT).show()
                showContextMenu = false
            },
            onAddExpression = if (message.contentType == 7) {
                {
                    // 添加表情到个人收藏
                    val expressionId = message.content.expressionId
                    if (!expressionId.isNullOrEmpty()) {
                        onAddExpression(expressionId)
                        Toast.makeText(context, "已添加表情", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "无法获取表情ID", Toast.LENGTH_SHORT).show()
                    }
                    showContextMenu = false
                }
            } else null
        )
    }
}

/**
 * 消息上下文菜单
 */
@Composable
private fun MessageContextMenu(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onCopyAll: () -> Unit,
    onQuote: () -> Unit,
    onRecall: () -> Unit,
    onAddExpression: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "消息操作",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 复制全部
                TextButton(
                    onClick = onCopyAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制全部",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("复制全部")
                    }
                }
                
                // 引用
                TextButton(
                    onClick = onQuote,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "引用",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("引用")
                    }
                }
                
                // 添加表情（仅对消息类型7显示）
                if (onAddExpression != null && message.contentType == 7) {
                    TextButton(
                        onClick = onAddExpression,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "添加表情",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("添加表情")
                        }
                    }
                }
                
                // 撤回
                TextButton(
                    onClick = onRecall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "撤回",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "撤回",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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
    message: ChatMessage,
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
            8 -> {
                // HTML消息
                content.text?.let { htmlContent ->
                    // 使用Box包裹，添加占位符以减少初始渲染压力
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 400.dp)
                    ) {
                    HtmlWebView(
                        htmlContent = htmlContent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { pointerInputChange ->
                                            // 兼容旧Compose：手动判断down
                                            if (!pointerInputChange.previousPressed && pointerInputChange.pressed) {
                                                pointerInputChange.consume()
                                            }
                                        }
                                    }
                                }
                            }
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
                        backgroundColor = Color.Transparent, // 使用透明背景，继承消息气泡背景
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            6 -> {
                // 文章消息
                PostMessageView(
                    postId = content.postId,
                    postTitle = content.postTitle,
                    postContent = content.postContent,
                    postContentType = content.postContentType,
                    textColor = textColor,
                    isMyMessage = isMyMessage
                )
            }
            7 -> {
                // 表情消息 (包括表情包和个人收藏表情)
                val context = LocalContext.current
                val stickerPackId = content.stickerPackId
                
                content.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = ImageUtils.createStickerImageRequest(
                            context = context,
                            url = imageUrl
                        ),
                        contentDescription = when {
                            content.expressionId != null && content.expressionId != "0" -> "个人收藏表情"
                            stickerPackId != null -> "表情包"
                            else -> "表情"
                        },
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                // 如果是表情包，点击跳转到详情页面
                                if (stickerPackId != null && stickerPackId != 0L) {
                                    com.yhchat.canary.ui.sticker.StickerPackDetailActivity.start(
                                        context,
                                        stickerPackId.toString()
                                    )
                                }
                            },
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
                                context = context,
                                url = fullUrl
                            ),
                            contentDescription = "表情",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    // 如果是表情包，点击跳转到详情页面
                                    if (stickerPackId != null && stickerPackId != 0L) {
                                        com.yhchat.canary.ui.sticker.StickerPackDetailActivity.start(
                                            context,
                                            stickerPackId.toString()
                                        )
                                    }
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            10 -> {
                // 视频消息 (contentType 10)
                content.videoUrl?.let { videoUrl ->
                    VideoMessageView(
                        videoUrl = videoUrl,
                        textColor = textColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            5 -> {
                // 表单消息（带按钮）
                content.text?.let { text ->
                    Text(
                        text = text,
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
        content.quoteMsgText?.let { quoteText: String ->
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
                    content.quoteImageUrl?.let { imageUrl: String ->
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
        
        // 按钮（用于表单消息等）
        content.buttons?.let { buttonsJson ->
            if (buttonsJson.isNotBlank() && buttonsJson != "null") {
                MessageButtons(
                    buttonsJson = buttonsJson,
                    message = message,
                    textColor = textColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 文章消息视图
 */
@Composable
private fun PostMessageView(
    postId: String?,
    postTitle: String?,
    postContent: String?,
    postContentType: String?,
    textColor: Color,
    isMyMessage: Boolean
) {
    val context = LocalContext.current
    
    if (postId.isNullOrEmpty()) {
        Text(
            text = "文章消息",
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    
    // 文章卡片
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 点击跳转到文章详情
                val intent = Intent(context, PostDetailActivity::class.java).apply {
                    putExtra("post_id", postId.toIntOrNull() ?: 0)
                    putExtra("post_title", postTitle ?: "文章详情")
                }
                context.startActivity(intent)
            }
            .border(
                width = 1.dp,
                color = if (isMyMessage) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(8.dp)
            ),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 文章图标和标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📄",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = postTitle ?: "文章",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 文章内容预览
            if (!postContent.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                when (postContentType) {
                    "2" -> {
                        // Markdown内容预览
                        Text(
                            text = postContent.take(100) + if (postContent.length > 100) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    else -> {
                        // 普通文本内容预览
                        Text(
                            text = postContent.take(100) + if (postContent.length > 100) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // 查看详情提示
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击查看文章详情 →",
                style = MaterialTheme.typography.labelSmall,
                color = if (isMyMessage) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 编辑历史弹窗
 */
@Composable
private fun EditHistoryDialog(
    msgId: String,
    onDismiss: () -> Unit
) {
    val viewModel: ChatViewModel = viewModel()
    var editRecords by remember { mutableStateOf<List<com.yhchat.canary.data.model.MessageEditRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(msgId) {
        isLoading = true
        val result = viewModel.getMessageEditHistory(msgId)
        result.fold(
            onSuccess = { records ->
                editRecords = records
                isLoading = false
            },
            onFailure = { error ->
                errorMessage = error.message
                isLoading = false
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "编辑历史",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage ?: "加载失败",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    editRecords.isEmpty() -> {
                        Text(
                            text = "暂无编辑历史",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(editRecords) { record ->
                                EditRecordItem(record)
                            }
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
 * 编辑记录项
 */
@Composable
private fun EditRecordItem(record: com.yhchat.canary.data.model.MessageEditRecord) {
    val parsedText = remember(record) {
        runCatching {
            val json = org.json.JSONObject(record.contentOld)
            json.optString("text").takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 编辑时间
            Text(
                text = "编辑于 ${formatTimestamp(record.msgTime)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            // 旧内容
            val displayText = parsedText ?: record.contentOld
            if (displayText.isNotEmpty()) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
 * 视频消息视图
 */
@Composable
private fun VideoMessageView(
    videoUrl: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.NotStarted) }
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (downloadState == DownloadState.NotStarted) {
                    downloadState = DownloadState.Downloading
                    // 提取文件名
                    val fileName = videoUrl.substringAfterLast("/").ifEmpty { "video_${System.currentTimeMillis()}.mp4" }
                    
                    // 启动下载，下载完成后自动打开
                    FileDownloadService.startDownload(
                        context = context,
                        fileUrl = videoUrl,
                        fileName = fileName,
                        fileSize = 0L,
                        autoOpen = true
                    )
                    
                    Toast.makeText(context, "开始下载视频：$fileName", Toast.LENGTH_SHORT).show()
                }
            },
        color = textColor.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 视频图标
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(textColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "视频",
                    tint = textColor,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 视频信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "视频消息",
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (downloadState) {
                        DownloadState.NotStarted -> "点击下载，使用外部播放器播放"
                        DownloadState.Downloading -> "正在下载..."
                        DownloadState.Completed -> "已下载"
                    },
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // 下载图标
            Icon(
                imageVector = when (downloadState) {
                    DownloadState.NotStarted -> Icons.Default.PlayArrow
                    DownloadState.Downloading -> Icons.Default.Add // 用作loading的临时替代
                    DownloadState.Completed -> Icons.Default.Check
                },
                contentDescription = when (downloadState) {
                    DownloadState.NotStarted -> "下载"
                    DownloadState.Downloading -> "下载中"
                    DownloadState.Completed -> "完成"
                },
                tint = textColor.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 下载状态
 */
private enum class DownloadState {
    NotStarted,
    Downloading,
    Completed
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

/**
 * 消息按钮组件
 */
@Composable
private fun MessageButtons(
    buttonsJson: String,
    message: ChatMessage,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ChatViewModel = viewModel()
    
    // 在Composable外部解析JSON
    val buttonData = remember(buttonsJson) {
        try {
            val buttonRows = JSONArray(buttonsJson)
            val rows = mutableListOf<List<ButtonData>>()
            
            for (i in 0 until buttonRows.length()) {
                val buttonRow = buttonRows.getJSONArray(i)
                val buttons = mutableListOf<ButtonData>()
                
                for (j in 0 until buttonRow.length()) {
                    val button = buttonRow.getJSONObject(j)
                    buttons.add(
                        ButtonData(
                            text = button.optString("text", "按钮"),
                            actionType = button.optInt("actionType", 0),
                            url = button.optString("url", ""),
                            value = button.optString("value", "")
                        )
                    )
                }
                rows.add(buttons)
            }
            rows
        } catch (e: Exception) {
            android.util.Log.e("MessageButtons", "Failed to parse buttons JSON", e)
            emptyList()
        }
    }
    
    if (buttonData.isNotEmpty()) {
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // 遍历每一行按钮
            buttonData.forEach { buttonRow ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 遍历每一行的按钮
                    buttonRow.forEach { btnData ->
                        Button(
                            onClick = {
                                handleButtonClick(
                                    context = context,
                                    viewModel = viewModel,
                                    message = message,
                                    actionType = btnData.actionType,
                                    url = btnData.url,
                                    value = btnData.value,
                                    buttonText = btnData.text
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = textColor.copy(alpha = 0.15f),
                                contentColor = textColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = btnData.text,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 按钮数据类
 */
private data class ButtonData(
    val text: String,
    val actionType: Int,
    val url: String,
    val value: String
)

/**
 * 处理按钮点击事件
 */
private fun handleButtonClick(
    context: Context,
    viewModel: ChatViewModel,
    message: ChatMessage,
    actionType: Int,
    url: String,
    value: String,
    buttonText: String
) {
    when (actionType) {
        1 -> {
            // 打开URL
            if (url.isNotBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                }
            }
        }
        2 -> {
            // 复制文本
            if (value.isNotBlank()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("button_value", value)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
            }
        }
        3 -> {
            // 按钮事件上报（button_report）
            val chatId = message.chatId ?: ""
            val chatType = message.chatType ?: 1
            
            viewModel.reportButtonClick(
                chatId = chatId,
                chatType = chatType,
                msgId = message.msgId,
                buttonValue = value
            )
            
            val chatTypeText = when (chatType) {
                1 -> "私聊"
                2 -> "群聊"
                3 -> "机器人"
                else -> "未知"
            }
            android.util.Log.d("ButtonClick", "点击按钮: 类型=$chatTypeText, chatId=$chatId, 按钮值=$value")
            Toast.makeText(context, "已点击：$buttonText", Toast.LENGTH_SHORT).show()
        }
        else -> {
            Toast.makeText(context, "未知按钮类型", Toast.LENGTH_SHORT).show()
        }
    }
}