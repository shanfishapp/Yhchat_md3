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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(groupId) {
        viewModel.loadGroupInfo(groupId)
    }
    
    YhchatCanaryTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(groupName, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
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
                    uiState.isEditingCategory -> {
                        EditCategoryDialog(
                            categoryName = uiState.newCategoryName,
                            onCategoryNameChange = { viewModel.updateNewCategoryName(it) },
                            onSave = { viewModel.saveCategoryName(groupId) },
                            onDismiss = { viewModel.hideEditCategoryDialog() }
                        )
                    }
                    uiState.groupInfo != null -> {
                        GroupInfoContent(
                            groupId = groupId,
                            groupInfo = uiState.groupInfo!!,
                            members = uiState.members,
                            isLoadingMembers = uiState.isLoadingMembers,
                            isLoadingMoreMembers = uiState.isLoadingMoreMembers,
                            hasMoreMembers = uiState.hasMoreMembers,
                            showMemberList = uiState.showMemberList,
                            isEditingCategory = uiState.isEditingCategory,
                            newCategoryName = uiState.newCategoryName,
                            onLoadMore = { viewModel.loadMoreMembers(groupId) },
                            onToggleMemberList = { viewModel.toggleMemberList() },
                            onShowEditCategoryDialog = { viewModel.showEditCategoryDialog() },
                            onHideEditCategoryDialog = { viewModel.hideEditCategoryDialog() },
                            onUpdateNewCategoryName = { viewModel.updateNewCategoryName(it) },
                            onSaveCategoryName = { viewModel.saveCategoryName(groupId) },
                            onUpdateDirectJoin = { viewModel.updateDirectJoin(groupId, it) },
                            onUpdateHistoryMsg = { viewModel.updateHistoryMsg(groupId, it) },
                            onUpdatePrivateSetting = { viewModel.updatePrivateSetting(groupId, it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
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
    showMemberList: Boolean,
    isEditingCategory: Boolean,
    newCategoryName: String,
    onLoadMore: () -> Unit,
    onToggleMemberList: () -> Unit,
    onShowEditCategoryDialog: () -> Unit,
    onHideEditCategoryDialog: () -> Unit,
    onUpdateNewCategoryName: (String) -> Unit,
    onSaveCategoryName: (String) -> Unit,
    onUpdateDirectJoin: (Boolean) -> Unit,
    onUpdateHistoryMsg: (Boolean) -> Unit,
    onUpdatePrivateSetting: (Boolean) -> Unit,
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
                    CategoryRow("群分类", groupInfo.categoryName, onShowEditCategoryDialog)
                    if (groupInfo.communityName.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        InfoRow("所属社区", groupInfo.communityName)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SwitchRow("免审核进群", groupInfo.directJoin, onUpdateDirectJoin)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SwitchRow("查看历史消息", groupInfo.historyMsgEnabled, onUpdateHistoryMsg)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SwitchRow("群聊私有", groupInfo.isPrivate, onUpdatePrivateSetting)
                }
            }
        }
        
        // 成员列表
        item {
            Text(
                text = "成员列表 (${groupInfo.memberCount})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable { onToggleMemberList() }
            )
        }
        
        if (showMemberList && (isLoadingMembers && members.isEmpty())) {
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
        } else if (showMemberList) {
            // 显示成员列表
            items(members) { member ->
                MemberItem(member = member)
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
private fun MemberItem(member: GroupMemberInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        text = "🔇 已禁言",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
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

