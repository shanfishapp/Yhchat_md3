package com.yhchat.canary.ui.conversation

import android.content.Intent
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
import androidx.compose.material.icons.filled.FileUpload
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
    onMenuClick: (String) -> Unit, // 添加参数用于区分不同的菜单项
    tokenRepository: TokenRepository? = null,
    viewModel: ConversationViewModel = viewModel(),
    scrollBehavior: ScrollBehavior? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val stickyData by viewModel.stickyData.collectAsState()
    val stickyLoading by viewModel.stickyLoading.collectAsState()

    // 列表状态
    val listState = rememberLazyListState()

    // 置顶栏显示状态 - 使用key保持状态
    var showStickyBar by remember(key1 = "sticky_bar") { mutableStateOf(false) }

    // 刷新状态 - 使用key保持状态
    var refreshing by remember(key1 = "refreshing") { mutableStateOf(false) }

    // 下拉刷新状态
    val swipeRefreshState =
        rememberSwipeRefreshState(isRefreshing = refreshing)
    val context = LocalContext.current
    
    // 协程作用域
    val coroutineScope = rememberCoroutineScope()
    
    // 长按菜单状态
    var showConversationMenu by remember { mutableStateOf(false) }
    var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
    var isSelectedConversationSticky by remember { mutableStateOf(false) }

    // 监听列表滚动位置，控制置顶栏显示
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        // 只有当滚动到顶部附近（前几个项目且滚动偏移较小）时才显示置顶栏
        showStickyBar = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100
    }

    // 允许返回后重新刷新（移除禁止刷新逻辑）
    
    // 设置tokenRepository（只在第一次或tokenRepository变化时执行）
    LaunchedEffect(tokenRepository) {
        tokenRepository?.let { viewModel.setTokenRepository(it) }
    }
    
    // 启动WebSocket连接（只在第一次或token/userId变化时执行）
    LaunchedEffect(token, userId) {
        if (token.isNotEmpty() && userId.isNotEmpty()) {
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



        // 顶部应用栏
        var expanded by remember { mutableStateOf(false) }
        
        TopAppBar(
            title = {
                Text(
                    text = "云湖聊天",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加"
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("添加...")
                                }
                            },
                            onClick = {
                                // 跳转到综合搜索页面（原有跳转逻辑）
                                val intent = Intent(context, ComprehensiveSearchActivity::class.java)
                                context.startActivity(intent)
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("创建...")
                                }
                            },
                            onClick = {
                                // 创建...（暂时跳转到空屏幕）
                                // TODO: 实现创建功能
                                onMenuClick("create")
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("扫一扫")
                                }
                            },
                            onClick = {
                                // 扫一扫（暂时跳转到空屏幕）
                                // TODO: 实现扫描功能
                                onMenuClick("scan")
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("传文件")
                                }
                            },
                            onClick = {
                                // 传文件（暂时跳转到空屏幕）
                                // TODO: 实现文件传输功能
                                onMenuClick("transfer")
                                expanded = false
                            }
                        )
                    }
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
        
        // 置顶会话（根据滚动状态显示/隐藏，带动画效果）
        AnimatedVisibility(
            visible = showStickyBar && !stickyData?.sticky.isNullOrEmpty(),
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
                    items(pagedConversations) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            onClick = {
                                // 跳转到聊天界面（无动画，交给系统默认）
                                val intent = Intent(context, com.yhchat.canary.ui.chat.ChatActivity::class.java)
                                intent.putExtra("chatId", conversation.chatId)
                                intent.putExtra("chatType", conversation.chatType)
                                intent.putExtra("chatName", conversation.name)
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
                    Text(
                        text = if (conversation.at > 0) "@${conversation.chatContent}" else conversation.chatContent,
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
