package com.yhchat.canary.ui.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.yhchat.canary.data.model.GroupMemberInfo
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreenRoot(
    groupId: String,
    groupName: String,
    viewModel: GroupInfoViewModel,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showShareDialog by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }
    
    LaunchedEffect(groupId) {
        viewModel.loadGroupInfo(groupId)
    }
    
    // 搜索时自动触发
    LaunchedEffect(searchKeyword) {
        if (isSearching) {
            viewModel.searchMembers(groupId, searchKeyword)
        }
    }
    
    YhchatCanaryTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { 
                            if (!isSearching) {
                                Text(groupName, fontWeight = FontWeight.Bold)
                            } else {
                                TextField(
                                    value = searchKeyword,
                                    onValueChange = { searchKeyword = it },
                                    placeholder = { Text("搜索群成员...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (isSearching) {
                                    isSearching = false
                                    searchKeyword = ""
                                    viewModel.clearSearch(groupId)
                                } else {
                                    onBackClick()
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSearching) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (isSearching) "取消搜索" else "返回"
                                )
                            }
                        },
                        actions = {
                            if (!isSearching) {
                                IconButton(onClick = { isSearching = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "搜索"
                                    )
                                }
                                IconButton(onClick = { showShareDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "分享"
                                    )
                                }
                                IconButton(onClick = onSettingsClick) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "群聊设置"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
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
                            Button(onClick = { viewModel.loadGroupInfo(groupId) }) {
                                Text("重试")
                            }
                        }
                    }
                    uiState.groupInfo != null -> {
                        GroupInfoContent(
                            groupId = groupId,
                            groupInfo = uiState.groupInfo!!,
                            members = uiState.members,
                            isLoadingMembers = uiState.isLoadingMembers,
                            isLoadingMoreMembers = uiState.isLoadingMoreMembers,
                            hasMoreMembers = uiState.hasMoreMembers,
                            currentUserPermission = uiState.groupInfo!!.permissionLevel.toInt(),
                            onLoadMore = { viewModel.loadMoreMembers(groupId) },
                            onRemoveMember = { userId -> viewModel.removeMember(groupId, userId) },
                            onGagMember = { userId, gagTime -> viewModel.gagMember(groupId, userId, gagTime) },
                            onSetMemberRole = { userId, userLevel -> viewModel.setMemberRole(groupId, userId, userLevel) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        
        // 分享对话框
        if (showShareDialog) {
            com.yhchat.canary.ui.components.ShareDialog(
                chatId = groupId,
                chatType = 2,  // 群聊
                chatName = groupName,
                onDismiss = { showShareDialog = false }
            )
        }
    }
}

@Composable
private fun GroupInfoContent(
    groupId: String,
    groupInfo: com.yhchat.canary.data.model.GroupDetail,
    members: List<GroupMemberInfo>,
    isLoadingMembers: Boolean,
    isLoadingMoreMembers: Boolean,
    hasMoreMembers: Boolean,
    currentUserPermission: Int = 0,
    onLoadMore: () -> Unit,
    onRemoveMember: (String) -> Unit = {},
    onGagMember: (String, Int) -> Unit = { _, _ -> },
    onSetMemberRole: (String, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // 检测滚动到底部并加载更多
    LaunchedEffect(listState, isLoadingMembers, isLoadingMoreMembers, hasMoreMembers) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val lastVisibleItem = visibleItems.lastOrNull()
            val totalItemsCount = layoutInfo.totalItemsCount
            
            // 判断是否滚动到了最后一个item
            lastVisibleItem != null && lastVisibleItem.index == totalItemsCount - 1
        }.collect { isAtBottom ->
            // 当滚动到底部且没在加载时，触发加载更多
            if (isAtBottom && !isLoadingMembers && !isLoadingMoreMembers && hasMoreMembers) {
                android.util.Log.d("GroupInfoScreen", "检测到滚动到底部，触发加载更多")
                onLoadMore()
            }
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 群聊头像和基本信息
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = ImageUtils.createImageRequest(
                            context = LocalContext.current,
                            url = groupInfo.avatarUrl
                        ),
                        contentDescription = "群头像",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = groupInfo.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (groupInfo.introduction.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = groupInfo.introduction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // 群聊详细信息
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("群ID", groupInfo.groupId)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("成员数量", "${groupInfo.memberCount} 人")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("群分类", groupInfo.categoryName)
                    if (groupInfo.communityName.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        InfoRow("所属社区", groupInfo.communityName)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("免审核进群", if (groupInfo.directJoin) "是" else "否")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("查看历史消息", if (groupInfo.historyMsgEnabled) "是" else "否")
                    if (groupInfo.isPrivate) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        InfoRow("群聊类型", "私有群聊")
                    }
                }
            }
        }
        
        // 成员列表
        item {
            Text(
                text = "成员列表 (${groupInfo.memberCount})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        if (isLoadingMembers && members.isEmpty()) {
            // 首次加载显示加载指示器
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // 显示成员列表
            items(members) { member ->
                MemberItem(
                    member = member,
                    currentUserPermission = currentUserPermission,
                    groupId = groupId,
                    onRemoveMember = onRemoveMember,
                    onGagMember = onGagMember,
                    onSetMemberRole = onSetMemberRole
                )
            }
            
            // 加载更多指示器
            if (isLoadingMoreMembers) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "加载更多成员...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (!hasMoreMembers && members.isNotEmpty()) {
                // 没有更多数据时显示提示
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "已加载全部成员",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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

@Composable
private fun MemberItem(
    member: GroupMemberInfo,
    currentUserPermission: Int = 0,
    groupId: String = "",
    onRemoveMember: ((String) -> Unit)? = null,
    onGagMember: ((String, Int) -> Unit)? = null,
    onSetMemberRole: ((String, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showGagDialog by remember { mutableStateOf(false) }
    
    // 判断是否显示管理菜单：除了群主外的所有成员都显示
    val showAdminMenu = member.permissionLevel < 100
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 点击成员卡片跳转到用户详情页
                com.yhchat.canary.ui.profile.UserProfileActivity.start(
                    context = context,
                    userId = member.userId,
                    userName = member.name
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageUtils.createAvatarImageRequest(
                    context = LocalContext.current,
                    url = member.avatarUrl
                ),
                contentDescription = member.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // 群主/管理员标签
                    when (member.permissionLevel) {
                        100 -> {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "群主",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        2 -> {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Text(
                                    text = "管理员",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    // VIP标签
                    if (member.isVip) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "VIP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = "ID: ${member.userId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 禁言状态
                if (member.isGag) {
                    Text(
                        text = "🔇 被禁言",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // 管理菜单（除了群主外的所有成员都显示）
            if (showAdminMenu) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "管理"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // 根据成员权限显示上任/卸任管理员
                        if (member.permissionLevel == 2) {
                            // 管理员，显示卸任选项
                            DropdownMenuItem(
                                text = { Text("卸任管理员") },
                                onClick = {
                                    showMenu = false
                                    onSetMemberRole?.invoke(member.userId, 0)
                                }
                            )
                        } else if (member.permissionLevel == 0) {
                            // 普通成员，显示上任选项
                            DropdownMenuItem(
                                text = { Text("设为管理员") },
                                onClick = {
                                    showMenu = false
                                    onSetMemberRole?.invoke(member.userId, 2)
                                }
                            )
                        }
                        
                        DropdownMenuItem(
                            text = { Text("踢出群聊") },
                            onClick = {
                                showMenu = false
                                onRemoveMember?.invoke(member.userId)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("禁言") },
                            onClick = {
                                showMenu = false
                                showGagDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // 禁言对话框
    if (showGagDialog) {
        GroupMemberGagDialog(
            memberName = member.name,
            onConfirm = { gagTime ->
                onGagMember?.invoke(member.userId, gagTime)
                showGagDialog = false
            },
            onDismiss = { showGagDialog = false }
        )
    }
}

@Composable
fun EditCategoryDialog(
    categoryName: String,
    onCategoryNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "编辑群分类")
        },
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = onCategoryNameChange,
                label = { Text("分类名称") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                onSave()
                onDismiss()
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 群成员禁言对话框
 */
@Composable
private fun GroupMemberGagDialog(
    memberName: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val gagOptions = listOf(
        0 to "取消禁言",
        600 to "禁言10分钟",
        3600 to "禁言1小时",
        21600 to "禁言6小时",
        43200 to "禁言12小时",
        1 to "永久禁言"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("禁言 $memberName") },
        text = {
            Column {
                Text("选择禁言时长：", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                gagOptions.forEach { (gagTime, label) ->
                    TextButton(
                        onClick = { onConfirm(gagTime) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth())
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

