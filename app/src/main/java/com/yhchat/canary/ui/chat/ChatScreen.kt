package com.yhchat.canary.ui.chat

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import com.yhchat.canary.ui.components.MessageContextMenu
import com.yhchat.canary.ui.components.EditMessageDialog
import com.yhchat.canary.ui.components.RecallMessageDialog
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
import androidx.compose.material3.*
import androidx.compose.foundation.border
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import com.yhchat.canary.data.model.ShareRequest
import com.yhchat.canary.data.model.ShareResponse
import com.yhchat.canary.data.repository.TokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// 添加缺失的导入语句
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalConfiguration
import com.yhchat.canary.ui.community.PostDetailActivity
import androidx.compose.foundation.combinedClickable
import com.yhchat.canary.ui.conversation.EmptyScreen

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
    onAvatarClick: (String, String, Int) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // 控制是否显示EmptyScreen的状态
    var showEmptyScreen by remember { mutableStateOf(false) }
    
    // 图片预览状态
    var showImageViewer by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf("") }
    
    // 滚动到底部按钮状态
    var showScrollToBottomButton by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // 引用消息状态
    var quoteMessage by remember { mutableStateOf<ChatMessage?>(null) }
    
    // 消息上下文菜单状态
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var contextMenuPosition by remember { mutableStateOf(androidx.compose.ui.unit.IntOffset.Zero) }
    
    // 编辑消息对话框状态
    var showEditMessageDialog by remember { mutableStateOf(false) }
    
    // 撤回消息确认对话框状态
    var showRecallMessageDialog by remember { mutableStateOf(false) }
    
    // 分享状态
    var isSharing by remember { mutableStateOf(false) }
    
    // 艾特用户状态
    var mentionText by remember { mutableStateOf("") }
    var mentionedUserIds by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // 获取当前聊天的消息类型
    val contentType by viewModel.getContentTypeForChat(chatId).collectAsStateWithLifecycle()
    
    // 屏幕尺寸
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp.value
    val screenHeight = configuration.screenHeightDp.dp.value
    
    // 艾特用户函数
    val onMentionUser = { userName: String, userId: String ->
        // 只在群聊中允许艾特用户，且不能艾特自己
        if (chatType == 2 && userName != viewModel.getCurrentUserNickname()) {  // 2表示群聊
            // 更新输入框文本，添加艾特用户名
            mentionText = "@$userName "
            inputText = if (inputText.isBlank()) {
                mentionText
            } else {
                "$inputText $mentionText"
            }
            
            // 添加被@用户的ID到列表中
            if (!mentionedUserIds.contains(userId)) {
                mentionedUserIds = mentionedUserIds + userId
            }
        }
    }
    
    // 初始化聊天
    LaunchedEffect(chatId, chatType, userId) {
        viewModel.initChat(chatId, chatType, userId)
        // TODO: 设置当前用户昵称
        // viewModel.setCurrentUserNickname("当前用户昵称")
    }
    
    // 监听滚动状态，当不在底部时显示"回到最新消息"按钮
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        // 当用户滚动查看历史消息时（不在最新消息位置），显示回到底部按钮
        // 因为是 reverseLayout，第一个可见项目的索引大于0表示不在最新消息位置
        showScrollToBottomButton = listState.firstVisibleItemIndex > 0 || 
                                   (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset > 100)
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
    
    // 根据showEmptyScreen状态决定显示哪个界面
    if (showEmptyScreen) {
        EmptyScreen(
            title = "功能完善中",
            onBackClick = { showEmptyScreen = false }
        )
    } else {
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
                } else if (chatType == 1) {
                    // 实现用户信息跳转
                    IconButton(onClick = {
                        android.util.Log.d("ChatScreen", "Opening user profile: chatId=$chatId, chatName=$chatName")
                        val intent = Intent(context, com.yhchat.canary.ui.info.UserProfileActivity::class.java)
                        intent.putExtra(com.yhchat.canary.ui.info.UserProfileActivity.EXTRA_USER_ID, chatId)
                        intent.putExtra(com.yhchat.canary.ui.info.UserProfileActivity.EXTRA_USER_NAME, chatName)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "用户信息"
                        )
                    }
                } else {
                    // 实现机器人跳转
                    IconButton(onClick = {
                        android.util.Log.d("ChatScreen", "Opening bot profile: chatId=$chatId, chatName=$chatName")
                        val intent = Intent(context, com.yhchat.canary.ui.info.BotProfileActivity::class.java)
                        intent.putExtra(com.yhchat.canary.ui.info.BotProfileActivity.EXTRA_BOT_ID, chatId)
                        intent.putExtra(com.yhchat.canary.ui.info.BotProfileActivity.EXTRA_BOT_NAME, chatName)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "机器人信息"
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
                            onMessageClick = { clickedMessage ->
                                // 双击消息跳转到引用消息位置
                                clickedMessage.quoteMsgId?.let { quoteMsgId ->
                                    val index = messages.indexOfFirst { it.msgId == quoteMsgId }
                                    if (index != -1) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(messages.size - 1 - index)
                                        }
                                    }
                                }
                            },
                            onMessageLongClick = { longClickedMessage, messagePosition ->
                                // 显示上下文菜单
                                selectedMessage = longClickedMessage
                                showContextMenu = true
                                // 使用消息位置作为菜单位置
                                contextMenuPosition = IntOffset(
                                    x = (messagePosition.x + 100).toInt(), // 偏移一些像素使其不完全贴在消息上
                                    y = (messagePosition.y + 50).toInt()
                                )
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
                            onMentionUser = { userName, userId ->
                    onMentionUser(userName, userId)
                },
                            onEditHistoryClick = { msgId ->
                                // 跳转到编辑历史界面
                                val intent = Intent(context, EditHistoryActivity::class.java).apply {
                                    putExtra("msg_id", msgId)
                                }
                                context.startActivity(intent)
                            }
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
                        // 发送消息时包含引用消息信息和被@用户ID
                        val quoteMsgId = quoteMessage?.msgId
                        viewModel.sendTextMessage(inputText.trim(), quoteMsgId, mentionedUserIds, contentType)
                        inputText = ""
                        quoteMessage = null // 清除引用消息
                        mentionedUserIds = emptyList() // 清除被@用户ID
                        // 不再重置消息类型，保持当前类型
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
                quoteMessage = quoteMessage,
                onClearQuote = { quoteMessage = null },
                contentType = contentType,
                onContentTypeChange = { type -> viewModel.setContentTypeForChat(chatId, type) },
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
    
    // 消息上下文菜单
    if (showContextMenu && selectedMessage != null) {
        MessageContextMenu(
            onReply = {
                quoteMessage = selectedMessage
                showContextMenu = false
                selectedMessage = null
            },
            onCopy = {
                // 实现复制消息功能
                selectedMessage?.content?.text?.let { text ->
                    if (text.isNotBlank()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("消息内容", text)
                        clipboard.setPrimaryClip(clip)
                        // 显示一个简单的提示（可以使用Toast或者Snackbar）
                        Toast.makeText(context, "消息已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }
                }
                showContextMenu = false
                selectedMessage = null
            },
            onForward = {
                // TODO: 实现转发消息功能
                showContextMenu = false
                selectedMessage = null
            },
            onEdit = {
                // 显示编辑消息对话框，保持selectedMessage不变
                showEditMessageDialog = true
                showContextMenu = false
                // 不要在这里重置selectedMessage，因为它需要传递给编辑对话框
            },
            onDelete = {
                // 显示撤回消息确认对话框
                showRecallMessageDialog = true
                showContextMenu = false
                // 不要在这里重置selectedMessage，因为它需要传递给确认对话框
            },
            onDismiss = {
                showContextMenu = false
                // 不要在这里重置selectedMessage
            },
            position = contextMenuPosition,
            isMyMessage = viewModel.isMyMessage(selectedMessage!!) // 传递是否为自己发送的消息
        )
    }
    
    // 编辑消息对话框
    if (showEditMessageDialog && selectedMessage != null) {
        EditMessageDialog(
            initialText = selectedMessage!!.content.text ?: "",
            initialContentType = selectedMessage!!.contentType,
            onConfirm = { text, contentType ->
                // 调用ViewModel编辑消息
                viewModel.editTextMessage(
                    msgId = selectedMessage!!.msgId,
                    text = text,
                    contentType = contentType
                )
                showEditMessageDialog = false
                selectedMessage = null // 在确认后重置selectedMessage
            },
            onDismiss = {
                showEditMessageDialog = false
                selectedMessage = null // 在取消后重置selectedMessage
            }
        )
    }
    
    // 撤回消息确认对话框
    if (showRecallMessageDialog && selectedMessage != null) {
        RecallMessageDialog(
            onConfirm = {
                // 调用ViewModel撤回消息
                selectedMessage?.msgId?.let { msgId ->
                    viewModel.recallMessage(msgId)
                }
                selectedMessage = null // 在确认后重置selectedMessage
            },
            onDismiss = {
                showRecallMessageDialog = false
                // 只有在取消时才重置selectedMessage
                if (!showRecallMessageDialog) {
                    selectedMessage = null
                }
            }
        )
    }
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
    onMessageClick: (ChatMessage) -> Unit = {},
    onMessageLongClick: (ChatMessage, androidx.compose.ui.geometry.Offset) -> Unit = { _, _ -> },
    onAvatarClick: (String, String, Int) -> Unit = { _, _, _ -> },
    onMentionUser: (String, String) -> Unit = { _, _ -> },
    onEditHistoryClick: (String) -> Unit = {}
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
    
    var messagePosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    Row(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                messagePosition = coordinates.positionInRoot()
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onDoubleClick = { onMessageClick(message) },
                onLongClick = { 
                    // 触发长按事件，传递消息位置
                    onMessageLongClick(message, messagePosition) 
                },
                onClick = { }
            ),
        horizontalArrangement = if (isMyMessage) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        if (!isMyMessage) {
            // 发送者头像
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
                    .combinedClickable(
                        onClick = {
                            onAvatarClick(message.sender.chatId, message.sender.name, message.sender.chatType)
                        },
                        onLongClick = {
                            // 长按头像实现艾特功能
                            // 只有在群聊中才允许艾特用户，且不能艾特自己
                            onMentionUser(message.sender.name, message.sender.chatId)
                        }
                    ),
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
                    content = message.content,
                    contentType = message.contentType,
                    isMyMessage = isMyMessage,
                    modifier = Modifier.padding(12.dp),
                    onImageClick = onImageClick
                )
            }

            // 时间戳和编辑历史按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(message.sendTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 如果消息有编辑时间，显示编辑历史按钮
                if (message.editTime != null && message.editTime > 0) {
                    TextButton(
                        onClick = { onEditHistoryClick(message.msgId) },
                        modifier = Modifier.height(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "编辑历史",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
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
 * 提示消息显示
 */
@Composable
private fun TipMessageItem(
    message: String,
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
                text = message,
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
            8 -> {
                // HTML消息
                content.text?.let { htmlContent ->
                    HtmlWebView(
                        htmlContent = htmlContent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 400.dp)
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
            9 -> {
                content.text?.let { text -> 
                    TipMessageItem(
                        message = text,
                        modifier = Modifier
                    )
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
            text = "📄 文章消息",
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