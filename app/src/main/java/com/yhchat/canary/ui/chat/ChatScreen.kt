package com.yhchat.canary.ui.chat

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.lazy.LazyItemScope
import com.yhchat.canary.ui.bot.BotInfoActivity
import com.yhchat.canary.ui.components.MarkdownText
import com.yhchat.canary.ui.components.EmojiText
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
import yh_bot.Bot
import com.yhchat.canary.proto.group.Bot_data
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
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 聊天界面
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun ChatScreen(
    chatId: String,
    chatType: Int,
    chatName: String,
    userId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    onAvatarClick: (String, String, Int, Int) -> Unit = { _, _, _, _ -> },  // 添加第4个参数：当前用户权限
    onImagePickerClick: () -> Unit = {},  // 图片选择器点击回调
    onCameraClick: () -> Unit = {},  // 相机点击回调
    onFilePickerClick: () -> Unit = {},  // 文件选择器点击回调
    imageUriToSend: android.net.Uri? = null,  // 待发送的图片URI
    fileUriToSend: android.net.Uri? = null,  // 待发送的文件URI
    onImageSent: () -> Unit = {},  // 图片发送完成回调
    onFileSent: () -> Unit = {}  // 文件发送完成回调
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages
    
    // 获取当前用户的权限等级
    val currentUserPermission = if (chatType == 2) {
        // 群聊时，从groupMembers中获取当前用户权限
        viewModel.getCurrentUserPermission()
    } else {
        0
    }
    var inputText by remember { mutableStateOf("") }
    var selectedMessageType by remember { mutableStateOf(1) } // 1-文本, 3-Markdown, 8-HTML
    var selectedInstruction by remember { mutableStateOf<com.yhchat.canary.data.model.Instruction?>(null) } // 选中的指令
    val listState = rememberLazyListState()
    
    // 图片预览状态
    var showImageViewer by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf<String?>(null) }
    
    // 滚动到底部按钮状态
    var showScrollToBottomButton by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // 引用消息状态
    var quotedMessageId by remember { mutableStateOf<String?>(null) }
    var quotedMessageText by remember { mutableStateOf<String?>(null) }
    
    // 编辑消息状态
    var showEditDialog by remember { mutableStateOf(false) }
    var messageToEdit by remember { mutableStateOf<ChatMessage?>(null) }
    
    // 输入框焦点请求器
    val inputFocusRequester = remember { FocusRequester() }
    
    // 键盘显示状态
    var shouldShowKeyboard by remember { mutableStateOf(false) }
    
    // 机器人看板展开状态
    var showBotBoard by remember { mutableStateOf(false) }
    
    // 初始化聊天
    LaunchedEffect(chatId, chatType, userId) {
        viewModel.initChat(chatId, chatType, userId)
    }
    
    // 如果是机器人聊天，加载机器人信息和看板
    LaunchedEffect(chatId, chatType) {
        if (chatType == 3) {
            viewModel.loadBotInfo(chatId)
            viewModel.loadBotBoard(chatId, chatType)
        }
    }
    
    // 加载聊天背景
    LaunchedEffect(chatId) {
        viewModel.loadChatBackground(context, chatId)
    }
    
    // 处理图片发送
    LaunchedEffect(imageUriToSend) {
        imageUriToSend?.let { uri ->
            android.util.Log.d("ChatScreen", "收到待发送的图片URI: $uri")
            viewModel.uploadAndSendImage(
                context = context,
                imageUri = uri,
                quoteMsgId = quotedMessageId,
                quoteMsgText = quotedMessageText
            )
            // 清除引用状态
            quotedMessageId = null
            quotedMessageText = null
            // 通知已发送
            onImageSent()
        }
    }
    
    // 监听待发送的文件
    LaunchedEffect(fileUriToSend) {
        fileUriToSend?.let { uri ->
            android.util.Log.d("ChatScreen", "📁 收到待发送的文件URI: $uri")
            viewModel.uploadAndSendFile(
                context = context,
                fileUri = uri,
                quoteMsgId = quotedMessageId,
                quoteMsgText = quotedMessageText
            )
            // 清除引用状态
            quotedMessageId = null
            quotedMessageText = null
            // 通知已发送
            onFileSent()
        }
    }
    
    // 退出时保存读取位置
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveCurrentReadPosition()
        }
    }
    
    // 监听滚动状态，当不在底部时显示"回到最新消息"按钮
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        // 当用户滚动查看历史消息时（不在最新消息位置），显示回到底部按钮
        // 因为是 reverseLayout，第一个可见项目的索引大于0表示不在最新消息位置
        showScrollToBottomButton = listState.firstVisibleItemIndex > 0 || 
                                   (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset > 100)
    }
    
    // WebSocket新消息处理：智能自动滚动
    LaunchedEffect(uiState.newMessageReceived) {
        if (uiState.newMessageReceived) {
            // 获取最新消息（reversedMessages的第一条就是最新的）
            val reversedMessages = messages.reversed()
            val latestMessage = reversedMessages.firstOrNull()
            
            // 判断条件1：用户是否在底部附近（允许一些偏移量）
            val isNearBottom = listState.firstVisibleItemIndex <= 4 && 
                              !listState.isScrollInProgress
            
            // 判断条件2：最新消息是否是当前用户发送的
            val isMyMessage = latestMessage?.sender?.chatId == userId
            
            // 判断条件3：最新消息时间戳是否很新（5秒内）
            val currentTime = System.currentTimeMillis()
            val isRecentMessage = latestMessage?.let { 
                currentTime - it.sendTime <= 500000 
            } ?: false
            
            // 自动滚动逻辑：
            // 1. 如果是自己发的消息，总是滚动到底部
            // 2. 如果用户在底部附近且消息是最近的，也自动滚动
            val shouldAutoScroll = isMyMessage || (isNearBottom && isRecentMessage)
            
            if (shouldAutoScroll) {
                // 平滑滚动到新消息
                listState.animateScrollToItem(0)
            }
            
            // 重置新消息标记
            viewModel.resetNewMessageFlag()
        }
    }

    // 处理系统返回键/手势返回
    BackHandler {
        onBackClick()
    }

    // 下拉刷新状态（刷新最新消息）
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refreshLatestMessages() }
    )
    
    // 应用聊天背景
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 背景图片
        if (uiState.chatBackgroundUrl != null) {
            coil.compose.AsyncImage(
                model = uiState.chatBackgroundUrl,
                contentDescription = "聊天背景",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alpha = 0.3f  // 半透明效果
            )
        }
    
    Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (uiState.chatBackgroundUrl != null) {
                MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.background
            }
    ) {
        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()  // 自动响应软键盘，推动内容上移
        ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Column {
                Text(
                    text = chatName,
                    fontWeight = FontWeight.Bold
                )
                    // 如果是群聊，显示群人数
                    if (chatType == 2 && uiState.groupMemberCount > 0) {
                        Text(
                            text = "${uiState.groupMemberCount} 人",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 如果是机器人，显示使用人数
                    if (chatType == 3) {
                        val botInfo = uiState.botInfo
                        if (botInfo != null) {
                            Text(
                                text = "${botInfo.data.headcount} 人使用",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
                // 用户详情按钮（只在单聊时显示）
                if (chatType == 1) {
                    IconButton(onClick = {
                        android.util.Log.d("ChatScreen", "Opening user detail: chatId=$chatId, chatName=$chatName")
                        com.yhchat.canary.ui.user.UserDetailActivity.start(
                            context = context,
                            userId = chatId,
                            userName = chatName
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "用户详情"
                        )
                    }
                }
                // 群聊信息菜单（只在群聊时显示）
                if (chatType == 2) {
                    IconButton(onClick = {
                        android.util.Log.d("ChatScreen", "Opening group info: chatId=$chatId, chatName=$chatName")
                        val intent = Intent(context, com.yhchat.canary.ui.group.GroupInfoActivity::class.java)
                        intent.putExtra(com.yhchat.canary.ui.group.GroupInfoActivity.EXTRA_GROUP_ID, chatId)
                        intent.putExtra(com.yhchat.canary.ui.group.GroupInfoActivity.EXTRA_GROUP_NAME, chatName)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "群聊详情"
                        )
                    }
                }
                // 机器人信息菜单（只在机器人聊天时显示）
                if (chatType == 3) {
                    IconButton(onClick = {
                        android.util.Log.d("ChatScreen", "Opening bot detail: chatId=$chatId, chatName=$chatName")
                        com.yhchat.canary.ui.bot.BotDetailActivity.start(
                            context = context,
                            botId = chatId,
                            botName = chatName,
                            chatType = chatType
                        )
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
        
        // 机器人看板按钮和内容
        // 单个机器人聊天时显示该机器人的看板（且设置允许）
        val botBoardEnabled = remember { 
            context.getSharedPreferences("chat_settings", android.content.Context.MODE_PRIVATE)
                .getBoolean("show_bot_board", true) 
        }
        if (chatType == 3 && botBoardEnabled) {
            val botBoard = uiState.botBoard
            if (botBoard != null && botBoard.boardCount > 0) {
                val boardData = botBoard.getBoardList().firstOrNull()
                if (boardData != null && boardData.content.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                // 展开/收起按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBotBoard = !showBotBoard }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "看板",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "机器人看板",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (showBotBoard) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (showBotBoard) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 看板内容（展开时显示）
                AnimatedVisibility(
                    visible = showBotBoard,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    uiState.botBoard?.let { board ->
                        if (board.boardCount > 0) {
                            val boardDataList = board.boardList
                            if (boardDataList.isNotEmpty()) {
                                val boardData = boardDataList[0]
                                BotBoardContent(
                                    boardData = boardData,
                                    onImageClick = { url ->
                                        currentImageUrl = url
                                        showImageViewer = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
                }
            }
        }
        
        // 群聊中的机器人看板列表（且设置允许）
        if (chatType == 2 && uiState.groupBots.isNotEmpty() && botBoardEnabled) {
            GroupBotBoardsSection(
                groupBots = uiState.groupBots,
                groupBotBoards = uiState.groupBotBoards,
                onImageClick = { url ->
                    currentImageUrl = url
                    showImageViewer = true
                }
            )
        }
        
        
        
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
                            // 使用多个字段组合确保key的唯一性，包括索引位置
                            val message = reversedMessages[index]
                            "${message.msgId}_${message.sendTime}_${message.sender.chatId}_${index}_${System.nanoTime()}"
                        }
                    ) { index ->
                        val message = reversedMessages[index]
                        // 获取发送者的权限等级（仅群聊）
                        val memberPermission = uiState.groupMembers[message.sender.chatId]?.permissionLevel
                        
                        AnimatedMessageItem(
                            message = message,
                            isMyMessage = viewModel.isMyMessage(message),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    fadeInSpec = tween(
                                        durationMillis = 300,
                                        easing = FastOutSlowInEasing
                                    ),
                                    fadeOutSpec = tween(
                                        durationMillis = 200,
                                        easing = FastOutSlowInEasing
                                    ),
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ),
                            onImageClick = { imageUrl ->
                                currentImageUrl = imageUrl
                                showImageViewer = true
                            },
                            onAvatarClick = { chatId, name, chatType , currentUserPermission ->
                                // 处理头像点击事件
                                if (chatType == 3) { // 机器人
                                    val intent = Intent(context, BotInfoActivity::class.java).apply {
                                        putExtra(BotInfoActivity.EXTRA_BOT_ID, chatId)
                                        putExtra(BotInfoActivity.EXTRA_BOT_NAME, name)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    // 用户头像点击，传递给外部处理（UserProfileActivity）
                                    onAvatarClick(chatId, name, chatType, currentUserPermission)
                                }
                            },
                            onAddExpression = viewModel::addExpressionToFavorites,
                            onQuote = { msgId, msgText ->
                                // 设置引用消息，格式：发送者名称 : 内容
                                val senderName = message.sender.name
                                val content = message.content.text ?: ""
                                val quotedText = "$senderName : $content"
                                quotedMessageId = msgId
                                quotedMessageText = quotedText
                                
                                // 自动聚焦输入框并显示键盘
                                coroutineScope.launch {
                                    inputFocusRequester.requestFocus()
                                    // 触发键盘显示
                                    shouldShowKeyboard = true
                                    // 延迟重置状态，避免重复触发
                                    delay(100)
                                    shouldShowKeyboard = false
                                }
                            },
                            onRecall = { msgId ->
                                // 撤回消息
                                viewModel.recallMessage(msgId)
                            },
                            onEdit = { message ->
                                // 编辑消息
                                messageToEdit = message
                                showEditDialog = true
                            },
                            memberPermission = memberPermission,
                            currentUserPermission = currentUserPermission
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
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSendMessage = {
                    if (inputText.isNotBlank()) {
                        val messageText = inputText.trim()
                        if (selectedInstruction != null) {
                            android.util.Log.d("ChatScreen", "📤 发送指令消息: /${selectedInstruction?.name}, commandId=${selectedInstruction?.id}, text=$messageText")
                        } else {
                            android.util.Log.d("ChatScreen", "📤 发送普通消息: $messageText")
                        }
                        
                        // 根据选择的消息类型发送消息，带上引用信息和指令ID
                        viewModel.sendMessage(
                            text = messageText,
                            contentType = selectedMessageType,
                            quoteMsgId = quotedMessageId,
                            quoteMsgText = quotedMessageText,
                            commandId = selectedInstruction?.id  // 传递指令ID
                        )
                        inputText = ""
                        // 发送后重置为文本类型
                        selectedMessageType = 1
                        // 清除引用状态
                        quotedMessageId = null
                        quotedMessageText = null
                        // 清除选中的指令
                        selectedInstruction = null
                        // 发送消息后自动滚动到最新消息
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                },
                onImageClick = {
                    // 调用图片选择器
                    onImagePickerClick()
                },
                onFileClick = {
                    // 调用文件选择器
                    onFilePickerClick()
                },
                onDraftChange = { draftText ->
                    viewModel.sendDraftInput(draftText)
                },
                onCameraClick = {
                    // 调用相机拍照
                    onCameraClick()
                },
                selectedMessageType = selectedMessageType,
                onMessageTypeChange = { newType ->
                    // 只能选择一个类型，点击已选中的类型则取消（回到文本）
                    selectedMessageType = if (selectedMessageType == newType) 1 else newType
                },
                quotedMessageText = quotedMessageText,
                onClearQuote = {
                    quotedMessageId = null
                    quotedMessageText = null
                },
                onExpressionClick = { expression ->
                    // 发送表情消息（contentType=7）
                    viewModel.sendExpressionMessage(
                        expression = expression,
                        quoteMsgId = quotedMessageId,
                        quoteMsgText = quotedMessageText
                    )
                    // 清除引用状态
                    quotedMessageId = null
                    quotedMessageText = null
                },
                onStickerClick = { stickerItem ->
                    // 发送表情包贴纸消息（contentType=7）
                    viewModel.sendStickerMessage(
                        stickerItem = stickerItem,
                        quoteMsgId = quotedMessageId,
                        quoteMsgText = quotedMessageText
                    )
                    // 清除引用状态
                    quotedMessageId = null
                    quotedMessageText = null
                },
                onLocalExpressionClick = { expressionText ->
                    // 将表情格式文本插入到输入框
                    inputText += expressionText
                },
                onInstructionClick = { instruction ->
                    android.util.Log.d("ChatScreen", "🎯 用户点击指令: /${instruction.name} (id=${instruction.id}, type=${instruction.type})")
                    
                    // 选中指令
                    selectedInstruction = instruction
                    
                    // 根据指令类型处理
                    when (instruction.type) {
                        1 -> {
                            android.util.Log.d("ChatScreen", "📝 普通指令，应用默认文本: ${instruction.defaultText}")
                            // 普通指令：应用默认文本（如果有）
                            if (instruction.defaultText.isNotEmpty()) {
                                inputText = instruction.defaultText
                            }
                        }
                        2 -> {
                            android.util.Log.d("ChatScreen", "⚡ 直发指令，立即发送消息")
                            // 直发指令：发送 "/{指令名称}"
                            val textToSend = "/${instruction.name}"
                            android.util.Log.d("ChatScreen", "📤 直发指令发送文本: '$textToSend'")
                            
                            // 立即发送消息
                            viewModel.sendMessage(
                                text = textToSend,
                                contentType = selectedMessageType,
                                quoteMsgId = quotedMessageId,
                                quoteMsgText = quotedMessageText,
                                commandId = instruction.id
                            )
                            inputText = ""
                            selectedInstruction = null
                            quotedMessageId = null
                            quotedMessageText = null
                        }
                        else -> {
                            android.util.Log.w("ChatScreen", "⚠️ 未知指令类型: ${instruction.type}")
                            // 其他类型指令暂不处理
                        }
                    }
                },
                groupId = if (chatType == 2) chatId else null,  // 只在群聊中传递groupId
                selectedInstruction = selectedInstruction,  // 传递选中的指令
                onClearInstruction = {
                    selectedInstruction = null
                    inputText = ""
                },
                focusRequester = inputFocusRequester,  // 传递焦点请求器
                shouldShowKeyboard = shouldShowKeyboard,  // 传递键盘显示状态
                modifier = Modifier
                    .navigationBarsPadding()  // 自适应导航栏
                    .padding(
                        start = 0.dp,  // 去掉左右padding让输入框占满宽度
                        end = 0.dp,
                        top = 1.dp,
                        bottom = 0.dp  // 导航栏padding已处理
                    )
            )
        }
        }
    }  // 闭合Box（聊天背景容器）
    
    // 图片预览器
    if (showImageViewer && !currentImageUrl.isNullOrEmpty()) {
        ImageViewer(
            imageUrl = currentImageUrl!!,
            onDismiss = { showImageViewer = false }
        )
    }
    
    // 编辑消息对话框
    if (showEditDialog && messageToEdit != null) {
        EditMessageDialog(
            message = messageToEdit!!,
            onConfirm = { newText ->
                // 更新消息
                viewModel.editMessage(messageToEdit!!.msgId, newText)
                showEditDialog = false
                messageToEdit = null
            },
            onDismiss = {
                showEditDialog = false
                messageToEdit = null
            }
        )
    }
}

/**
 * 消息项组件
 */
@Composable
fun LazyItemScope.AnimatedMessageItem(
    message: ChatMessage,
    isMyMessage: Boolean,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit = {},
    onAvatarClick: (String, String, Int, Int) -> Unit = { _, _, _, _ -> },
    onAddExpression: (String) -> Unit = {},
    onQuote: (String, String) -> Unit = { _, _ -> },
    onRecall: (String) -> Unit = {},
    onEdit: (ChatMessage) -> Unit = {},
    memberPermission: Int? = null,  // 群成员权限等级
    currentUserPermission: Int = 0  // 当前用户权限等级
) {
    val context = LocalContext.current
    val content = message.content
    val textColor = if (isMyMessage) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val backgroundColor = if (isMyMessage) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val alignment = if (isMyMessage) {
        Alignment.End
    } else {
        Alignment.Start
    }
    
    // 长按菜单状态
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }

    // 用于获取消息项位置的引用
    val messageItemCoordinates = remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    
    // 确定消息内容类型
    val contentType = message.content.contentType ?: 1
    
    // 为长按事件创建一个引用
    val messageText = content.text ?: ""
    val messageUrl = content.imageUrl ?: content.stickerUrl ?: content.fileUrl ?: content.videoUrl ?: content.audioUrl ?: ""
    
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                messageItemCoordinates.value = coordinates
            }
            .combinedClickable(
                onClick = { 
                    // 单击事件：根据内容类型执行不同的操作
                    when (contentType) {
                        2 -> {
                            // Markdown消息：什么都不做，内容已在界面上渲染
                        }
                        8 -> {
                            // HTML消息：什么都不做，内容已在界面上渲染
                        }
                        else -> {
                            // 其他类型消息：如果有URL则点击打开，否则无操作
                            if (messageUrl.isNotEmpty()) {
                                when {
                                    messageUrl.endsWith(".mp3") || messageUrl.endsWith(".wav") || messageUrl.endsWith(".m4a") -> {
                                        // 音频文件：播放音频
                                        AudioPlayerService.start(context, messageUrl, message.sender.name)
                                    }
                                    messageUrl.endsWith(".mp4") || messageUrl.endsWith(".mov") || messageUrl.endsWith(".avi") -> {
                                        // 视频文件：下载并播放视频
                                        FileDownloadService.startDownload(  
                                            context = context,  
                                            fileUrl = messageUrl,  
                                            fileName = "video_${System.currentTimeMillis()}.mp4",  
                                            fileSize = 0L,  // 如果不知道大小可以传 0  
                                            autoOpen = true  
                                        )  
                                    }
                                    messageUrl.endsWith(".pdf") || messageUrl.endsWith(".doc") || messageUrl.endsWith(".docx") || 
                                    messageUrl.endsWith(".xls") || messageUrl.endsWith(".xlsx") || messageUrl.endsWith(".ppt") || 
                                    messageUrl.endsWith(".pptx") -> {
                                        // 文档文件：下载并打开文档
                                        FileDownloadService.startDownload(  
                                            context = context,  
                                            fileUrl = messageUrl,  
                                            fileName = "file_${System.currentTimeMillis()}_${messageUrl.substringAfterLast("/")}",  
                                            fileSize = 0L,  
                                            autoOpen = true  
                                        )
                                    }
                                    else -> {
                                        // 图片文件：打开图片预览
                                        onImageClick(messageUrl)
                                    }
                                }
                            }
                        }
                    }
                },
                onLongClick = { 
                    messageItemCoordinates.value?.let { coordinates ->
                        contextMenuPosition = coordinates.positionInRoot()
                    } ?: run {
                        // 如果无法获取精确位置，使用默认位置
                        contextMenuPosition = Offset(0f, 0f)
                    }
                    showContextMenu = true
                }
            )
        // 消息容器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
        ) {
            // 头像（非我的消息才显示）
            if (!isMyMessage) {
                message.sender.avatarUrl?.let { avatarUrl ->
                    AsyncImage(
                        model = ImageUtils.createAvatarImageRequest(
                            context = context,
                            url = avatarUrl
                        ),
                        contentDescription = "头像",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { 
                                // 点击头像：进入用户详情
                                onAvatarClick(message.sender.chatId, message.sender.name, message.sender.chatType, currentUserPermission)
                            },
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    // 如果没有头像URL，使用默认头像
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { 
                                // 点击头像：进入用户详情
                                onAvatarClick(message.sender.chatId, message.sender.name, message.sender.chatType, currentUserPermission)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = message.sender.name.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // 消息内容气泡
            Surface(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .defaultMinSize(minWidth = 40.dp),
                shape = RoundedCornerShape(
                    topStart = if (isMyMessage) 16.dp else 4.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMyMessage) 4.dp else 16.dp,
                    bottomEnd = 16.dp
                ),
                color = backgroundColor
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .widthIn(max = 280.dp)  // 限制最大宽度
                ) {
                    // 发送者名称（只在非我的消息且非群主/管理员时显示）
                    if (!isMyMessage && memberPermission != null && memberPermission > 0) {
                        Text(
                            text = message.sender.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (memberPermission == 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = if (memberPermission == 2) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // 根据内容类型显示不同内容
                    when (contentType) {
                        2 -> {
                            // Markdown消息内容
                            if (message.content.text != null) {
                                MarkdownText(
                                    markdown = message.content.text!!,
                                    textColor = textColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        3 -> {
                            // Markdown消息内容
                            if (message.content.text != null) {
                                MarkdownText(
                                    markdown = message.content.text!!,
                                    textColor = textColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        8 -> {
                            // HTML消息内容
                            if (message.content.text != null) {
                                // 为HTML内容创建一个容器
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)  // 固定高度，可根据需要调整
                                ) {
                                    HtmlWebView(
                                        htmlContent = message.content.text!!,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        4 -> {
                            // 文件消息
                            message.content.fileName?.let { fileName ->
                                val fileSize = message.content.fileSize
                                val fileSizeText = if (fileSize != null) {
                                    " (${formatFileSize(fileSize)})"
                                } else {
                                    ""
                                }
                                
                                Column {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "文件",
                                        tint = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fileName + fileSizeText,
                                        color = textColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        6 -> {
                            // 文章消息
                            message.content.postTitle?.let { title ->
                                Column {
                                    Text(
                                        text = title,
                                        color = textColor,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    message.content.text?.let { summary ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = summary,
                                            color = textColor.copy(alpha = 0.8f),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "查看全文 →",
                                        color = if (isMyMessage) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                        7 -> {
                            // 表情消息 (包括表情包和个人收藏表情)
                            val stickerPackId = content.stickerPackId
                            val expressionId = content.expressionId
                            
                            // 判断是个人表情还是表情包
                            val isPersonalExpression = expressionId != null && expressionId != "0"
                            val isStickerPack = stickerPackId != null && stickerPackId != 0L
                            
                            content.imageUrl?.let { imageUrl ->
                                AsyncImage(
                                    model = ImageUtils.createStickerImageRequest(
                                        context = context,
                                        url = imageUrl
                                    ),
                                    contentDescription = when {
                                        isPersonalExpression -> "个人收藏表情"
                                        isStickerPack -> "表情包"
                                        else -> "表情"
                                    },
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = {
                                                if (isPersonalExpression) {
                                                    // 个人表情：打开图片预览
                                                    onImageClick(imageUrl)
                                                } else if (isStickerPack) {
                                                    // 表情包：跳转到表情包详情
                                                    com.yhchat.canary.ui.sticker.StickerPackDetailActivity.start(
                                                        context = context,
                                                        stickerPackId = stickerPackId?.toString() ?: ""
                                                    )
                                                } else {
                                                    // 默认：图片预览
                                                    onImageClick(imageUrl)
                                                }
                                            },
                                            onLongClick = { 
                                                showContextMenu = true
                                            }
                                        ),
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
                                            .combinedClickable(
                                                onClick = {
                                                    if (isPersonalExpression) {
                                                    onImageClick(fullUrl)
                                                    } else if (isStickerPack) {
                                                        com.yhchat.canary.ui.sticker.StickerPackDetailActivity.start(
                                                            context = context,
                                                            stickerPackId = stickerPackId?.toString() ?: ""
                                                        )
                                                    } else {
                                                        onImageClick(fullUrl)
                                                    }
                                                },
                                                onLongClick = { 
                                                    // 获取点击位置
                                                    val density = LocalDensity.current
                                                    onGloballyPositioned { coordinates ->
                                                        contextMenuPosition = coordinates.positionInRoot()
                                                    }
                                                    showContextMenu = true
                                                }
                                            ),
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
                                // 检查是否包含表情格式或链接
                                if (text.contains("[.")) {
                                    // 包含表情格式的文本，使用EmojiText组件
                                    EmojiText(
                                        text = text,
                                        color = textColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.combinedClickable(
                                            onClick = { },
                                            onLongClick = { 
                                                // 获取点击位置
                                                val density = LocalDensity.current
                                                onGloballyPositioned { coordinates ->
                                                    contextMenuPosition = coordinates.positionInRoot()
                                                }
                                                showContextMenu = true
                                            }
                                        )
                                    )
                                } else if (LinkDetector.containsLink(text)) {
                                    // 包含链接的文本
                                    LinkText(
                                        text = text,
                                        style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                                        linkColor = if (isMyMessage) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.combinedClickable(
                                            onClick = { },
                                            onLongClick = { 
                                                // 获取点击位置
                                                val density = LocalDensity.current
                                                onGloballyPositioned { coordinates ->
                                                    contextMenuPosition = coordinates.positionInRoot()
                                                }
                                                showContextMenu = true
                                            }
                                        )
                                    )
                                } else {
                                    // 普通文本（支持表情格式渲染）
                                    EmojiText(
                                        text = text,
                                        color = textColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.combinedClickable(
                                            onClick = { },
                                            onLongClick = { 
                                                // 获取点击位置
                                                val density = LocalDensity.current
                                                onGloballyPositioned { coordinates ->
                                                    contextMenuPosition = coordinates.positionInRoot()
                                                }
                                                showContextMenu = true
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 我的消息的头像
            if (isMyMessage) {
                Spacer(modifier = Modifier.width(8.dp))
                message.sender.avatarUrl?.let { avatarUrl ->
                    AsyncImage(
                        model = ImageUtils.createAvatarImageRequest(
                            context = context,
                            url = avatarUrl
                        ),
                        contentDescription = "头像",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { 
                                // 点击头像：进入用户详情
                                onAvatarClick(message.sender.chatId, message.sender.name, message.sender.chatType, currentUserPermission)
                            },
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    // 如果没有头像URL，使用默认头像
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { 
                                // 点击头像：进入用户详情
                                onAvatarClick(message.sender.chatId, message.sender.name, message.sender.chatType, currentUserPermission)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = message.sender.name.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
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
                try {
                    val buttonsArray = JSONArray(buttonsJson)
                    if (buttonsArray.length() > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 0 until buttonsArray.length()) {
                                val buttonObj = buttonsArray.getJSONObject(i)
                                val buttonText = buttonObj.optString("text", "")
                                val buttonValue = buttonObj.optString("value", "")
                                val buttonType = buttonObj.optString("type", "text") // 默认为text类型
                                
                                Button(
                                    onClick = {
                                        when (buttonType) {
                                            "url" -> {
                                                // URL类型按钮：打开链接
                                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(buttonValue))
                                                try {
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            "copy" -> {
                                                // 复制类型按钮：复制文本到剪贴板
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("button_value", buttonValue)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                            }
                                            else -> {
                                                // 文本类型按钮：执行相应操作
                                                Toast.makeText(context, "已点击：$buttonText", Toast.LENGTH_SHORT).show()
                                                // 这里可以添加其他按钮类型的处理逻辑
                                            }
                                        }
                                    },
                                    modifier = Modifier.height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = buttonText,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatScreen", "解析按钮JSON失败", e)
                }
            }
        }
    }
    
    // 长按菜单
    if (showContextMenu) {
        MessageContextMenu(
            message = message,
            isMyMessage = isMyMessage,
            position = contextMenuPosition,
            onCopy = { textToCopy ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("message", textToCopy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
            },
            onQuote = onQuote,
            onRecall = { 
                onRecall(message.msgId)
                showContextMenu = false
            },
            onEdit = {
                onEdit(message)
                showContextMenu = false
            },
            onAddExpression = { url ->
                onAddExpression(url)
                showContextMenu = false
            },
            onDismiss = { showContextMenu = false }
        )
    }
}

/**
 * 消息长按菜单
 */
@Composable
fun MessageContextMenu(
    message: ChatMessage,
    isMyMessage: Boolean,
    position: Offset,
    onCopy: (String) -> Unit,
    onQuote: (String, String) -> Unit,
    onRecall: () -> Unit,
    onEdit: () -> Unit,
    onAddExpression: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val currentMessage = message
    
    // 获取屏幕尺寸
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp * density.density
    val screenHeight = configuration.screenHeightDp.dp * density.density
    
    // 计算菜单位置，确保不超出屏幕边界
    val menuWidth = 160.dp
    val menuHeight = 48.dp * when {
        isMyMessage -> 4  // 编辑、撤回、引用、复制
        else -> 3  // 引用、复制、添加表情（如果包含图片）
    }
    
    val density = LocalDensity.current
    val x = with(density) {
        if (position.x + 160.dp.toPx() > screenWidth) {
            screenWidth - 160.dp.toPx() - 8.dp.toPx()  // 靠右但不超出屏幕
        } else {
            position.x
        }
    }
    
    val y = with(density) {
        if (position.y + menuHeight.toPx() > screenHeight) {
            position.y - menuHeight.toPx()  // 向上显示菜单
        } else {
            position.y
        }
    }
    
    Box(
        modifier = Modifier
            .offset { IntOffset(x.toInt(), y.toInt()) }
            .clickable { }  // 防止点击穿透
    ) {
        Card(
            modifier = Modifier.width(menuWidth),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column {
                // 复制
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val textToCopy = currentMessage.content.text ?: ""
                            if (textToCopy.isNotEmpty()) {
                                onCopy(textToCopy)
                                onDismiss()
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "复制",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // 引用
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val textToQuote = currentMessage.content.text ?: ""
                            if (textToQuote.isNotEmpty()) {
                                onQuote(currentMessage.msgId, textToQuote)
                                onDismiss()
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "引用",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // 添加到表情（如果是图片消息）
                if (currentMessage.content.imageUrl != null || 
                    currentMessage.content.stickerUrl != null) {
                    val imageUrl = currentMessage.content.imageUrl ?: currentMessage.content.stickerUrl
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!imageUrl.isNullOrEmpty()) {
                                    onAddExpression(imageUrl)
                                    onDismiss()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "添加表情",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // 编辑（仅自己的消息）
                if (isMyMessage && currentMessage.content.contentType == 1 && 
                    currentMessage.content.text != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onEdit()
                                onDismiss()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "编辑",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // 撤回（仅自己的消息，且在2分钟内）
                if (isMyMessage) {
                    val sendTime = currentMessage.sendTime
                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - sendTime
                    val canRecall = timeDiff <= 2 * 60 * 1000  // 2分钟内可以撤回
                    
                    if (canRecall) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRecall()
                                    onDismiss()
                                }
                                .padding(16.dp)
                                .background(if (canRecall) Color.Red else Color.Gray),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "撤回",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        
        // 点击菜单外部区域关闭菜单
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
        )
    }
}

/**
 * 编辑消息对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMessageDialog(
    message: ChatMessage,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editedText by remember { mutableStateOf(message.content.text ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑消息") },
        text = {
            TextField(
                value = editedText,
                onValueChange = { editedText = it },
                label = { Text("消息内容") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (editedText.isNotBlank()) {
                        onConfirm(editedText)
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 格式化文件大小
 */
fun formatFileSize(sizeInBytes: Long): String {
    val sizeInKB = sizeInBytes / 1024.0
    return when {
        sizeInKB < 1024 -> "%.1f KB".format(sizeInKB)
        else -> "%.1f MB".format(sizeInKB / 1024.0)
    }
}

/**
 * 视频消息视图
 */
@Composable
fun VideoMessageView(
    videoUrl: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "视频",
            tint = textColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "视频消息",
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "点击播放",
            color = if (textColor == MaterialTheme.colorScheme.onPrimary) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
            style = MaterialTheme.typography.labelMedium
        )
    }
}





@Composable  
fun MenuButtons(  
    onAddFriend: () -> Unit,  
    onGroupNotice: () -> Unit,  
    onGroupFile: () -> Unit,  
    onGroupMember: () -> Unit  
) {  
    Row(  
        modifier = Modifier  
            .fillMaxWidth()  
            .padding(vertical = 8.dp),  
        horizontalArrangement = Arrangement.SpaceEvenly  
    ) {  
        MenuButton(icon = Icons.Default.Add, label = "添加", onClick = onAddFriend)  
        MenuButton(icon = Icons.Default.FormatQuote, label = "公告", onClick = onGroupNotice)  
        MenuButton(icon = Icons.Default.Add, label = "文件", onClick = onGroupFile)  
        MenuButton(icon = Icons.Default.MoreVert, label = "成员", onClick = onGroupMember)  
    }  
}

/**
 * 菜单按钮
 */
@Composable
fun MenuButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 机器人看板内容
 */
@Composable
fun BotBoardContent(
    boardData: Bot.board.Board_data,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 看板标题
        if (boardData.bot_name.isNotBlank()) {
            Text(
                text = boardData.bot_name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 看板内容（使用Markdown渲染）
        if (boardData.content.isNotBlank()) {
            MarkdownText(
                markdown = boardData.content,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 群聊中的机器人看板部分
 */
@Composable
fun GroupBotBoardsSection(
    groupBots: List<Bot_data>,
    groupBotBoards: Map<String, Bot.board.Board_data>,
    onImageClick: (String) -> Unit
) {
    groupBots.forEach { bot ->
        val board = groupBotBoards[bot.botId]
        if (board != null && board.content.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "机器人看板",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${bot.name} 看板",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 看板内容
                        BotBoardContent(
                            boardData = board,
                            onImageClick = onImageClick
                        )
                    }
                }
        }
    }
}