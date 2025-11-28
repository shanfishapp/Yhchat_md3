package com.yhchat.canary.ui.group

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.yhchat.canary.data.model.GroupMemberInfo
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    onSearchChatClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showShareDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showExitGroupDialog by remember { mutableStateOf(false) }
    var showEditNicknameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        viewModel.loadGroupInfo(groupId)
    }

    YhchatCanaryTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = groupName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "群聊详情",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                            // 直接进入群聊设置
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "群聊设置"
                                )
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
                            groupName = groupName,
                            context = context,
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
                            onShareClick = { showShareDialog = true },
                            onReportClick = { showReportDialog = true },
                            onSearchChatClick = onSearchChatClick,
                            onInviteClick = { showInviteDialog = true },
                            onExitClick = { showExitGroupDialog = true },
                            onEditNicknameClick = { showEditNicknameDialog = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        
    }
    
    // 显示成功消息
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessMessage()
        }
    }
    
    // 显示错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    // 分享弹窗
    if (showShareDialog) {
        com.yhchat.canary.ui.components.ShareDialog(
            chatId = groupId,
            chatType = 2, // 群聊
            chatName = groupName,
            onDismiss = { showShareDialog = false }
        )
    }
    
    // 举报弹窗
    if (showReportDialog) {
        com.yhchat.canary.ui.components.ReportDialog(
            chatId = groupId,
            chatType = 2, // 群聊
            chatName = groupName,
            onDismiss = { showReportDialog = false },
            onSuccess = {
                android.widget.Toast.makeText(context, "举报已提交", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // 邀请好友弹窗
    if (showInviteDialog) {
        com.yhchat.canary.ui.components.InviteToGroupDialog(
            groupId = groupId,
            groupName = groupName,
            onDismiss = { showInviteDialog = false },
            onSuccess = {
                android.widget.Toast.makeText(context, "邀请已发送", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // 退出群聊弹窗
    if (showExitGroupDialog) {
        ExitGroupDialog(
            groupName = groupName,
            onConfirm = {
                // 实现退出群聊逻辑
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    val userRepository = com.yhchat.canary.data.di.RepositoryFactory.getUserRepository(context)
                    userRepository.deleteFriend(groupId, 2).fold(
                        onSuccess = { _: Boolean ->
                            android.widget.Toast.makeText(context, "已退出群聊", android.widget.Toast.LENGTH_SHORT).show()
                            onBackClick()
                        },
                        onFailure = { error: Throwable ->
                            android.widget.Toast.makeText(context, "退出失败：${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                showExitGroupDialog = false
            },
            onDismiss = { showExitGroupDialog = false }
        )
    }
    
    // 编辑群昵称对话框
    if (showEditNicknameDialog) {
        val currentUserNickname = viewModel.getCurrentUserNicknameInGroup()
        
        EditGroupNicknameDialog(
            currentNickname = currentUserNickname,
            onConfirm = { newNickname: String ->
                viewModel.editMyGroupNickname(groupId, newNickname)
                showEditNicknameDialog = false
            },
            onDismiss = { showEditNicknameDialog = false }
        )
    }
}

@Composable
private fun GroupInfoContent(
    groupId: String,
    groupName: String,
    context: android.content.Context,
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
    onShareClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onSearchChatClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onExitClick: () -> Unit = {},
    onEditNicknameClick: () -> Unit = {},
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
    
    // 捕获变量以在 LazyColumn 作用域中使用
    val currentContext = context
    val currentGroupId = groupId
    val currentGroupName = groupName
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 群聊基本信息卡片 - 紧凑版
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 群头像
                    AsyncImage(
                        model = ImageUtils.createImageRequest(
                            context = LocalContext.current,
                            url = groupInfo.avatarUrl
                        ),
                        contentDescription = "群头像",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 群信息
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = groupInfo.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ID: ${groupInfo.groupId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${groupInfo.memberCount} 名成员",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 分享按钮
                    IconButton(
                        onClick = onShareClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享群聊",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        // 群聊简介卡片
        if (groupInfo.introduction.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "群聊简介",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
        
        // 成员按钮
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            GroupMembersActivity.start(
                                context = currentContext,
                                groupId = currentGroupId,
                                groupName = currentGroupName
                            )
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "群成员",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "群成员",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${groupInfo.memberCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "查看",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // 功能选项
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // 搜索聊天记录
                    FunctionMenuItem(
                        icon = Icons.Default.Search,
                        text = "搜索聊天记录",
                        onClick = onSearchChatClick
                    )
                    
                    // 举报群聊
                    FunctionMenuItem(
                        icon = Icons.Default.Report,
                        text = "举报群聊",
                        onClick = onReportClick
                    )
                    
                    // 设置聊天背景
                    FunctionMenuItem(
                        icon = Icons.Default.Wallpaper,
                        text = "设置聊天背景",
                        onClick = {
                            com.yhchat.canary.ui.background.ChatBackgroundActivity.start(
                                currentContext,
                                currentGroupId,
                                currentGroupName
                            )
                        }
                    )
                    
                    // 群网盘
                    FunctionMenuItem(
                        icon = Icons.Default.Folder,
                        text = "群网盘",
                        onClick = {
                            com.yhchat.canary.ui.disk.GroupDiskActivity.start(currentContext, currentGroupId, currentGroupName)
                        }
                    )
                    
                    // 邀请好友
                    FunctionMenuItem(
                        icon = Icons.Default.PersonAdd,
                        text = "邀请好友",
                        onClick = onInviteClick
                    )
                    
                    // 群聊设置
                    FunctionMenuItem(
                        icon = Icons.Default.Settings,
                        text = "群聊设置",
                        onClick = {
                            val intent = android.content.Intent(currentContext, com.yhchat.canary.ui.group.GroupSettingsActivity::class.java)
                            intent.putExtra(com.yhchat.canary.ui.group.GroupSettingsActivity.EXTRA_GROUP_ID, currentGroupId)
                            intent.putExtra(com.yhchat.canary.ui.group.GroupSettingsActivity.EXTRA_GROUP_NAME, currentGroupName)
                            currentContext.startActivity(intent)
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 退出群聊 (危险操作)
                    FunctionMenuItem(
                        icon = Icons.Default.ExitToApp,
                        text = "退出群聊",
                        onClick = onExitClick,
                        isDangerous = true
                    )
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
private fun InfoRowModern(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(56.dp)
                .shadow(4.dp, CircleShape)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@Composable
private fun FunctionMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isDangerous: Boolean = false
) {
    val iconColor = if (isDangerous) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val textColor = if (isDangerous) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "进入",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 退出群聊确认对话框
 */
@Composable
fun ExitGroupDialog(
    groupName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "退出群聊",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("确定要退出群聊「$groupName」吗？")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("退出", color = MaterialTheme.colorScheme.onError)
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
 * 编辑群昵称对话框
 */
@Composable
fun EditGroupNicknameDialog(
    currentNickname: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    var nickname by remember { mutableStateOf(currentNickname) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "编辑群昵称",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "设置您在群聊中的昵称",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("群昵称") },
                    placeholder = { Text("请输入群昵称") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    maxLines = 1,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nickname) },
                enabled = !isLoading && nickname.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("设置中...")
                } else {
                    Text("确定")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("取消")
            }
        }
    )
}
