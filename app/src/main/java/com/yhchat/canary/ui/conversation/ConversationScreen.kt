package com.yhchat.canary.ui.conversation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.animation.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.yhchat.canary.data.model.Conversation
import com.yhchat.canary.data.model.StickyData
import com.yhchat.canary.data.model.StickyItem
import com.yhchat.canary.data.model.ChatType
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.ui.components.ScrollBehavior
import com.yhchat.canary.ui.components.HandleScrollBehavior
import com.yhchat.canary.ui.search.ComprehensiveSearchActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.*
import androidx.compose.ui.input.pointer.pointerInput
import java.text.SimpleDateFormat
import java.util.*
import com.yhchat.canary.ui.components.ConversationMenuDialog

/**
 * 会话列表界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    token: String,
    userId: String,
    onConversationClick: (String, Int, String) -> Unit, // chatId, chatType, chatName
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    tokenRepository: TokenRepository? = null,
    viewModel: ConversationViewModel = viewModel(),
    scrollBehavior: ScrollBehavior? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val stickyData by viewModel.stickyData.collectAsState()
    val stickyLoading by viewModel.stickyLoading.collectAsState()
    
    // 读取显示置顶会话的设置
    val context = LocalContext.current
    val prefs = remember { 
        context.getSharedPreferences("display_settings", Context.MODE_PRIVATE) 
    }
    var showStickyConversations by remember { 
        mutableStateOf(prefs.getBoolean("show_sticky_conversations", true)) 
    }
    
    // 监听设置变化
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "show_sticky_conversations") {
                showStickyConversations = prefs.getBoolean("show_sticky_conversations", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // 列表状态
    val listState = rememberLazyListState()

    // 置顶栏显示状态 - 使用key保持状态
    var showStickyBar by remember(key1 = "sticky_bar") { mutableStateOf(false) }

    // 刷新状态 - 使用key保持状态
    var refreshing by remember(key1 = "refreshing") { mutableStateOf(false) }

    // 下拉刷新状态
    val swipeRefreshState =
        rememberSwipeRefreshState(isRefreshing = refreshing)
    
    // 协程作用域
    val coroutineScope = rememberCoroutineScope()
    
    // 长按菜单状态
    var showConversationMenu by remember { mutableStateOf(false) }
    var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
    var isSelectedConversationSticky by remember { mutableStateOf(false) }
    
    // 添加菜单 BottomSheet 状态
    var showAddMenuBottomSheet by remember { mutableStateOf(false) }

    // 监听列表滚动位置，控制置顶栏显示
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        // 只有当滚动到顶部附近（前几个项目且滚动偏移较小）时才显示置顶栏
        // 注意：这里判断是否在顶部，不考虑置顶会话
        showStickyBar = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100
    }
    
    // 连接滚动行为到底部导航栏的显示/隐藏
    scrollBehavior?.let { behavior ->
        listState.HandleScrollBehavior(scrollBehavior = behavior)
    }
    
    // 移除自动滚动逻辑，让用户自己控制滚动位置

    // 允许返回后重新刷新（移除禁止刷新逻辑）
    
    // 设置tokenRepository（只在第一次或tokenRepository变化时执行）
    LaunchedEffect(tokenRepository) {
        tokenRepository?.let { viewModel.setTokenRepository(it) }
    }
    
    // 启动WebSocket连接（只在第一次或token/userId变化时执行）
    // 检查是否禁用了 WebSocket
    val isWebSocketDisabled = remember { 
        prefs.getBoolean("disable_websocket", false) 
    }
    
    LaunchedEffect(token, userId, isWebSocketDisabled) {
        if (token.isNotEmpty() && userId.isNotEmpty() && !isWebSocketDisabled) {
            viewModel.startWebSocket(userId)
        }
    }
    
    // 每次进入页面都拉取一次
    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            viewModel.loadConversations(token)
            // 加载置顶会话（独立加载，不影响普通会话）
            viewModel.loadStickyConversations()
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部应用栏



        TopAppBar(
            title = {
                Text(
                    text = "云湖",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = {
                    showAddMenuBottomSheet = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加"
                    )
                }
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                }
            }
        )
        
        // 错误信息
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // 置顶会话（根据滚动状态和设置显示/隐藏，带动画效果）
        AnimatedVisibility(
            visible = showStickyBar && showStickyConversations && !stickyData?.sticky.isNullOrEmpty(),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            IntegratedStickyConversations(
                stickyData = stickyData,
                onConversationClick = onConversationClick
            )
        }

        // 会话列表（支持下拉刷新）
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                // 只有用户主动下拉刷新时才重新加载数据
                refreshing = true
                viewModel.loadConversations(token)
                // 延迟一下再关闭刷新状态，让用户感知到刷新动作
                coroutineScope.launch {
                    kotlinx.coroutines.delay(500)
                    refreshing = false
                }
            }
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val pagedConversations by viewModel.pagedConversations.collectAsState()
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = pagedConversations,
                        key = { conversation -> "${conversation.chatId}_${conversation.timestampMs}" }
                    ) { conversation ->
                        // 使用remember确保点击时获取最新的conversation数据
                        val chatId = conversation.chatId
                        val chatType = conversation.chatType
                        val chatName = conversation.name
                        
                        ConversationItem(
                            conversation = conversation,
                            onClick = {
                                // 标记会话为已读
                                viewModel.markConversationAsRead(chatId, chatType)
                                
                                // 跳转到聊天界面（使用最新的会话数据）
                                val intent = Intent(context, com.yhchat.canary.ui.chat.ChatActivity::class.java)
                                intent.putExtra("chatId", chatId)
                                intent.putExtra("chatType", chatType)
                                intent.putExtra("chatName", chatName)
                                // 使用 FLAG_ACTIVITY_CLEAR_TOP 确保清除栈顶到目标Activity之间的所有Activity
                                // 配合 FLAG_ACTIVITY_SINGLE_TOP 确保如果已存在则重用并调用 onNewIntent
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                context.startActivity(intent)
                            },
                            onLongClick = {
                                selectedConversation = conversation
                                coroutineScope.launch {
                                    isSelectedConversationSticky = viewModel.isConversationSticky(conversation.chatId)
                                    showConversationMenu = true
                                }
                            }
                        )
                    }
                    if (pagedConversations.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无会话",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // 加载更多提示
                    if (uiState.isLoading && pagedConversations.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
                // 触底自动加载更多
                LaunchedEffect(pagedConversations, listState) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastIndex ->
                            if (lastIndex == pagedConversations.lastIndex && !uiState.isLoading) {
                                viewModel.loadMoreConversations()
                            }
                        }
                }
            }
        }
    }
    
    // 长按菜单弹窗
    if (showConversationMenu && selectedConversation != null) {
        ConversationMenuDialog(
            conversation = selectedConversation!!,
            isSticky = isSelectedConversationSticky,
            onDismiss = { 
                showConversationMenu = false
                selectedConversation = null
            },
            onToggleSticky = {
                selectedConversation?.let { conversation ->
                    viewModel.toggleStickyConversation(conversation)
                }
            },
            onDelete = {
                selectedConversation?.let { conversation ->
                    viewModel.deleteConversation(conversation.chatId)
                }
            }
        )
    }
    
    // 添加菜单 BottomSheet
    if (showAddMenuBottomSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showAddMenuBottomSheet = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState()
        ) {
            AddMenuBottomSheetContent(
                onAddUserGroupBot = {
                    showAddMenuBottomSheet = false
                    val intent = Intent(context, ComprehensiveSearchActivity::class.java)
                    context.startActivity(intent)
                },
                onCreateGroupBot = {
                    showAddMenuBottomSheet = false
                    // TODO: 创建群聊/机器人功能
                    android.widget.Toast.makeText(context, "创建群聊/机器人功能待实现", android.widget.Toast.LENGTH_SHORT).show()
                },
                onScan = {
                    showAddMenuBottomSheet = false
                    // TODO: 扫一扫功能
                    android.widget.Toast.makeText(context, "扫一扫功能待实现", android.widget.Toast.LENGTH_SHORT).show()
                },
                onSendFile = {
                    showAddMenuBottomSheet = false
                    // TODO: 传文件功能
                    android.widget.Toast.makeText(context, "传文件功能待实现", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

/**
 * 添加菜单 BottomSheet 内容
 */
@Composable
private fun AddMenuBottomSheetContent(
    onAddUserGroupBot: () -> Unit,
    onCreateGroupBot: () -> Unit,
    onScan: () -> Unit,
    onSendFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "添加",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        
        HorizontalDivider()
        
        // 添加用户/群聊/机器人
        androidx.compose.material3.ListItem(
            headlineContent = { Text("添加用户/群聊/机器人") },
            supportingContent = { Text("通过ID搜索并添加好友、群聊或机器人", style = MaterialTheme.typography.bodySmall) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "添加",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            modifier = Modifier.clickable(onClick = onAddUserGroupBot)
        )
        
        // 创建群聊/机器人
        androidx.compose.material3.ListItem(
            headlineContent = { Text("创建群聊/机器人") },
            supportingContent = { Text("创建新的群聊或机器人", style = MaterialTheme.typography.bodySmall) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = "创建",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            modifier = Modifier.clickable(onClick = onCreateGroupBot)
        )
        
        // 扫一扫
        androidx.compose.material3.ListItem(
            headlineContent = { Text("扫一扫") },
            supportingContent = { Text("扫描二维码添加好友或加入群聊", style = MaterialTheme.typography.bodySmall) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "扫一扫",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            modifier = Modifier.clickable(onClick = onScan)
        )
        
        // 传文件
        androidx.compose.material3.ListItem(
            headlineContent = { Text("传文件") },
            supportingContent = { Text("快速发送文件到指定联系人", style = MaterialTheme.typography.bodySmall) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "传文件",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            modifier = Modifier.clickable(onClick = onSendFile)
        )
    }
}

/**
 * 会话项
 */
@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
    val now = System.currentTimeMillis()
    val timeText = if (now - conversation.timestampMs < 24 * 60 * 60 * 1000) {
        timeFormat.format(Date(conversation.timestampMs))
    } else {
        dateFormat.format(Date(conversation.timestampMs))
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box {
                AsyncImage(
                    model = if (conversation.avatarUrl != null) {
                        ImageRequest.Builder(LocalContext.current)
                            .data(conversation.avatarUrl)
                            .addHeader("Referer", "https://myapp.jwznb.com")
                            .crossfade(true)
                            .build()
                    } else {
                        ImageRequest.Builder(LocalContext.current)
                            .data("https://chat-img.jwznb.com/default-avatar.png")
                            .addHeader("Referer", "https://myapp.jwznb.com")
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = com.yhchat.canary.R.drawable.ic_person)
                )
                
                // 未读消息标识 - 开启免打扰时不显示红点
                if (conversation.unreadMessage > 0 && conversation.doNotDisturb != 1) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadMessage.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 会话信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conversation.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // 认证标识
                        if (conversation.certificationLevel != null && conversation.certificationLevel > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        when (conversation.certificationLevel) {
                                            1 -> Color(0xFF4CAF50) // 官方 - 绿色
                                            2 -> Color(0xFF2196F3) // 地区 - 蓝色
                                            else -> Color.Gray
                                        },
                                        CircleShape
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (conversation.certificationLevel) {
                                        1 -> "官方"
                                        2 -> "地区"
                                        else -> "认证"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        
                        // 免打扰图标
                        if (conversation.doNotDisturb == 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = "免打扰",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 直接显示消息内容，不再显示"发送者："前缀
                    val displayContent = if (conversation.at > 0) {
                        "@${conversation.chatContent}"
                    } else {
                        conversation.chatContent
                    }
                    
                    Text(
                        text = displayContent,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (conversation.at > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // @标识
                    if (conversation.at > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 会话类型图标
 */
@Composable
fun ChatTypeIcon(chatType: Int) {
    val icon = when (chatType) {
        ChatType.USER.value -> "👤"
        ChatType.GROUP.value -> "👥"
        ChatType.BOT.value -> "🤖"
        else -> "💬"
    }
    
    Text(
        text = icon,
        fontSize = 20.sp
    )
}

/**
 * 集成的置顶会话组件
 */
@Composable
fun IntegratedStickyConversations(
    stickyData: com.yhchat.canary.data.model.StickyData?,
    onConversationClick: (String, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 如果没有置顶会话，不显示组件
    if (stickyData?.sticky.isNullOrEmpty()) {
        return
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            // 置顶会话标题
            Text(
                text = "置顶会话",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            // 置顶会话横向列表
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                stickyData.sticky?.let { stickyList ->
                    items(stickyList) { stickyItem ->
                        IntegratedStickyItem(
                            stickyItem = stickyItem,
                            onClick = {
                                onConversationClick(
                                    stickyItem.chatId,
                                    stickyItem.chatType,
                                    stickyItem.chatName
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 集成的置顶会话项
 */
@Composable
fun IntegratedStickyItem(
    stickyItem: com.yhchat.canary.data.model.StickyItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(64.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 头像
        Box {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(stickyItem.avatarUrl)
                    .addHeader("Referer", "https://myapp.jwznb.com")
                    .crossfade(true)
                    .build(),
                contentDescription = "头像",
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.res.painterResource(id = com.yhchat.canary.R.drawable.ic_person)
            )

            // 认证标识
            if (stickyItem.certificationLevel > 0) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            when (stickyItem.certificationLevel) {
                                1 -> Color(0xFF4CAF50) // 官方 - 绿色
                                2 -> Color(0xFF2196F3) // 地区 - 蓝色
                                else -> Color.Gray
                            },
                            CircleShape
                        )
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (stickyItem.certificationLevel) {
                            1 -> "官"
                            2 -> "地"
                            else -> "认"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 8.sp
                    )
                }
            }
        }

        // 会话名称
        Text(
            text = stickyItem.chatName,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(58.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
