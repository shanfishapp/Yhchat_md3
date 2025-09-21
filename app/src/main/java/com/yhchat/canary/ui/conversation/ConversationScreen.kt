package com.yhchat.canary.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.yhchat.canary.data.model.ChatType
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.ui.sticky.StickyConversations
import java.text.SimpleDateFormat
import java.util.*

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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()

    // 列表状态
    val listState = rememberLazyListState()

    // 置顶栏显示状态
    var showStickyBar by remember { mutableStateOf(false) }

    // 刷新状态
    var refreshing by remember { mutableStateOf(false) }

    // 下拉刷新状态
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = refreshing)

    // 监听列表滚动位置，控制置顶栏显示
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        // 只有当滚动到顶部附近（前几个项目且滚动偏移较小）时才显示置顶栏
        showStickyBar = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100
    }

    // 设置tokenRepository
    LaunchedEffect(tokenRepository) {
        tokenRepository?.let { viewModel.setTokenRepository(it) }
    }
    
    // 启动WebSocket连接
    LaunchedEffect(token, userId) {
        if (token.isNotEmpty() && userId.isNotEmpty()) {
            viewModel.startWebSocket(token, userId)
        }
    }
    
    // 加载会话列表
    LaunchedEffect(token) {
        viewModel.loadConversations(token)
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部应用栏



        TopAppBar(
            title = {
                Text(
                    text = "云湖聊天",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                }
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "菜单"
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
            visible = showStickyBar,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            StickyConversations(
                onConversationClick = onConversationClick,
                tokenRepository = tokenRepository
            )
        }

        // 会话列表（支持下拉刷新）
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                refreshing = true
                viewModel.loadConversations(token)
                refreshing = false
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(conversations) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            onClick = {
                                onConversationClick(conversation.chatId, conversation.chatType, conversation.name)
                            },
                            onLongClick = {
                                // 长按处理
                            }
                        )
                    }

                    if (conversations.isEmpty()) {
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
                }
            }
        }
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
            .clickable { onClick() },
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
                
                // 未读消息标识
                if (conversation.unreadMessage > 0) {
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
