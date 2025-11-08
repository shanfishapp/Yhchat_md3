package com.yhchat.canary.ui.contacts

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.yhchat.canary.ui.chat.ChatActivity
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.ui.search.SearchActivity
import androidx.compose.foundation.lazy.rememberLazyListState

/**
 * 通讯录界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通讯录") },
                actions = {
                    // 搜索按钮
                    IconButton(onClick = {
                        val intent = Intent(context, SearchActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
        modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    // 加载中
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    // 错误提示
                    Column(
                        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
                            text = uiState.error ?: "加载失败",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadContacts() }) {
                            Text("重试")
                        }
                    }
                }
                else -> {
                    // 显示通讯录列表
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // 好友分组
                        item {
                            ContactGroupHeader(
                                title = "好友",
                                count = uiState.friends.size,
                                isExpanded = uiState.friendsExpanded,
                                onToggle = { viewModel.toggleFriendsExpanded() }
                            )
                        }
                        
                        if (uiState.friendsExpanded) {
                            items(uiState.friends) { contact ->
                                ContactItem(
                                    contact = contact,
                                    onClick = {
                                        // 打开聊天界面（用户类型为1）
                                        val intent = Intent(context, ChatActivity::class.java).apply {
                                            putExtra("chatId", contact.chatId)
                                            putExtra("chatType", 1)
                                            putExtra("chatName", contact.name)
                                        }
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                        
                        // 群聊分组
                        item {
                            ContactGroupHeader(
                                title = "我加入的群聊",
                                count = uiState.groups.size,
                                isExpanded = uiState.groupsExpanded,
                                onToggle = { viewModel.toggleGroupsExpanded() }
                            )
                        }
                        
                        if (uiState.groupsExpanded) {
                            items(uiState.groups) { contact ->
                                ContactItem(
                                    contact = contact,
                                    onClick = {
                                        // 打开聊天界面（群聊类型为2）
                                        val intent = Intent(context, ChatActivity::class.java).apply {
                                            putExtra("chatId", contact.chatId)
                                            putExtra("chatType", 2)
                                            putExtra("chatName", contact.name)
                                        }
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                        
                        // 机器人分组
                        item {
                            ContactGroupHeader(
                                title = "机器人",
                                count = uiState.bots.size,
                                isExpanded = uiState.botsExpanded,
                                onToggle = { viewModel.toggleBotsExpanded() }
                            )
                        }
                        
                        if (uiState.botsExpanded) {
                            items(uiState.bots) { contact ->
                                ContactItem(
                                    contact = contact,
                                    onClick = {
                                        // 打开聊天界面（机器人类型为3）
                                        val intent = Intent(context, ChatActivity::class.java).apply {
                                            putExtra("chatId", contact.chatId)
                                            putExtra("chatType", 3)
                                            putExtra("chatName", contact.name)
                                        }
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                        
                        // 我创建的机器人分组
                        item {
                            ContactGroupHeader(
                                title = "我创建的机器人",
                                count = uiState.myBots.size,
                                isExpanded = uiState.myBotsExpanded,
                                onToggle = { viewModel.toggleMyBotsExpanded() }
                            )
                        }
                        
                        if (uiState.myBotsExpanded) {
                            items(uiState.myBots) { contact ->
                                ContactItem(
                                    contact = contact,
                                    onClick = {
                                        // 打开机器人管理界面
                                        val intent = Intent(context, com.yhchat.canary.ui.bot.BotManagementActivity::class.java).apply {
                                            putExtra("botId", contact.chatId)
                                            putExtra("botName", contact.name)
                                        }
                                        context.startActivity(intent)
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

/**
 * 联系人分组头部
 */
@Composable
private fun ContactGroupHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 联系人项
 */
@Composable
private fun ContactItem(
    contact: Contact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            AsyncImage(
                model = ImageUtils.createAvatarImageRequest(
                    context = context,
                    url = contact.avatarUrl
                ),
                contentDescription = contact.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 名称和ID
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "ID: ${contact.chatId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 权限标识（仅群聊）
            if (contact.permissionLevel == 100) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "群主",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else if (contact.permissionLevel == 2) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "管理员",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
    
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
