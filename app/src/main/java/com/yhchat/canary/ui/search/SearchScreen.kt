package com.yhchat.canary.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhchat.canary.data.di.RepositoryFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.res.painterResource
import com.yhchat.canary.R
import com.yhchat.canary.data.repository.TokenRepository
import kotlinx.coroutines.delay

/**
 * 搜索界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onItemClick: (chatId: String, chatType: Int, chatName: String) -> Unit,
    tokenRepository: TokenRepository?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SearchViewModel = viewModel {
        SearchViewModel(
            apiService = RepositoryFactory.apiService,
            tokenRepository = tokenRepository,
            friendRepository = try { RepositoryFactory.getFriendRepository(context) } catch (e: Exception) { null },
            conversationRepository = try { RepositoryFactory.getConversationRepository(context) } catch (e: Exception) { null }
        )
    }

    val uiState by viewModel.uiState.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val joinRequestState by viewModel.joinRequestState.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedGroupInfo by remember { mutableStateOf<Pair<String, String>?>(null) } // (groupId, groupName)

    // 处理系统返回键/手势返回
    BackHandler {
        onBackClick()
    }
    
    // 监听申请状态变化
    LaunchedEffect(selectedGroupInfo) {
        if (selectedGroupInfo != null) {
            viewModel.joinRequestState.collect { state ->
                if (state.isInConversations) {
                    // 已在会话列表中，直接进入聊天
                    selectedGroupInfo?.let { (groupId, groupName) ->
                        onItemClick(groupId, 2, groupName)
                    }
                    selectedGroupInfo = null
                    showGroupDialog = false
                    viewModel.resetJoinRequestState()
                } else if (state.isSuccess) {
                    // 申请成功，延迟关闭弹窗
                    delay(2000)
                    selectedGroupInfo = null
                    showGroupDialog = false
                    viewModel.resetJoinRequestState()
                }
            }
        }
    }

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
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("搜索用户、群组和机器人") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.search(searchText)
                        }
                    ),
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = {
                                searchText = ""
                                viewModel.clearSearch()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清除"
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent
                    )
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.search(searchText) }) {
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

        // 搜索结果
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            searchResult?.let { result ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // 根据API文档，result.list包含不同类别的搜索结果
                    result.list.forEach { category ->
                        category.list?.let { items ->
                            if (items.isNotEmpty()) {
                                item {
                                    Text(
                                        text = category.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                
                                items(items) { searchItem ->
                                        Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .clickable { 
                                                when (searchItem.friendType) {
                                                    2 -> {
                                                        // 群组，显示弹窗
                                                        selectedGroupInfo = Pair(searchItem.friendId, searchItem.nickname)
                                                        showGroupDialog = true
                                                    }
                                                    else -> {
                                                        // 用户和机器人，直接进入聊天
                                                        onItemClick(
                                                            searchItem.friendId,
                                                            searchItem.friendType,
                                                            searchItem.nickname
                                                        )
                                                    }
                                                }
                                            },
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(searchItem.avatarUrl)
                                                    .addHeader("Referer", "https://myapp.jwznb.com")
                                                    .crossfade(true)
                                                    .placeholder(R.drawable.ic_launcher_foreground)
                                                    .error(R.drawable.ic_launcher_foreground)
                                                    .build(),
                                                contentDescription = searchItem.nickname,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = searchItem.nickname,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                
                                                // 显示对应的ID
                                                val idLabel = when (searchItem.friendType) {
                                                    1 -> "用户ID"
                                                    2 -> "群ID"
                                                    3 -> "机器人ID"
                                                    else -> "ID"
                                                }
                                                Text(
                                                    text = "$idLabel: ${searchItem.friendId}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                
                                                if (searchItem.name?.isNotEmpty() == true) {
                                                    Text(
                                                        text = searchItem.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            
                                            // 显示类型标签
                                            Surface(
                                                color = when (searchItem.friendType) {
                                                    1 -> MaterialTheme.colorScheme.primaryContainer
                                                    2 -> MaterialTheme.colorScheme.secondaryContainer
                                                    3 -> MaterialTheme.colorScheme.tertiaryContainer
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    text = when (searchItem.friendType) {
                                                        1 -> "用户"
                                                        2 -> "群组"
                                                        3 -> "机器人"
                                                        else -> "未知"
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = when (searchItem.friendType) {
                                                        1 -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        2 -> MaterialTheme.colorScheme.onSecondaryContainer
                                                        3 -> MaterialTheme.colorScheme.onTertiaryContainer
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
            } ?: run {
                // 初始状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "输入关键词开始搜索",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        }
    }
    
    // 群聊详情弹窗
    if (showGroupDialog && selectedGroupInfo != null) {
        val groupInfo = selectedGroupInfo!!
        val groupId: String = groupInfo.first
        val groupName: String = groupInfo.second
        
        // 获取token
        var token by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            tokenRepository?.getToken()?.collect { userToken ->
                token = userToken?.token ?: ""
            }
        }
        
        GroupJoinDialog(
            groupId = groupId,
            groupName = groupName,
            token = token,
            viewModel = viewModel,
            onDismiss = { 
                showGroupDialog = false
                selectedGroupInfo = null
                viewModel.resetJoinRequestState()
            },
            onJoinRequest = { groupIdParam ->
                viewModel.handleGroupJoin(token, groupIdParam)
            }
        )
    }
}

/**
 * 群聊加入弹窗
 */
@Composable
fun GroupJoinDialog(
    groupId: String,
    groupName: String,
    token: String,
    viewModel: SearchViewModel,
    onDismiss: () -> Unit,
    onJoinRequest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val joinRequestState by viewModel.joinRequestState.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = "群聊详情",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // 群聊基本信息
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 默认头像（由于搜索结果可能没有头像信息）
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = groupName.take(2),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "群ID: $groupId",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 错误提示
                joinRequestState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // 成功提示
                if (joinRequestState.isSuccess) {
                    Text(
                        text = "申请已发送，请等待管理员审核",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (joinRequestState.isInConversations) {
                    Text(
                        text = "即将进入群聊...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoinRequest(groupId) },
                enabled = !joinRequestState.isLoading && !joinRequestState.isSuccess && !joinRequestState.isChecking
            ) {
                if (joinRequestState.isLoading || joinRequestState.isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = when {
                        joinRequestState.isChecking -> "检查中..."
                        joinRequestState.isLoading -> "申请中..."
                        joinRequestState.isSuccess -> "已申请"
                        joinRequestState.isInConversations -> "进入聊天"
                        else -> "加入群聊"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}