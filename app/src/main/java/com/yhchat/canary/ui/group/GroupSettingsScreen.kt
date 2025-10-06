package com.yhchat.canary.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.yhchat.canary.ui.components.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreenRoot(
    groupId: String,
    groupName: String,
    viewModel: GroupSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(groupId) {
        viewModel.loadGroupInfo(groupId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("群聊设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(
                            onClick = { viewModel.saveEditing() },
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "保存"
                                )
                            }
                        }
                    } else {
                        if (viewModel.isAdminOrOwner()) {
                            IconButton(onClick = { viewModel.startEditing() }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "编辑"
                                )
                            }
                        }
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
                uiState.groupInfo != null -> {
                    GroupSettingsContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupSettingsContent(
    uiState: GroupSettingsUiState,
    viewModel: GroupSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val groupInfo = uiState.groupInfo!!
    val isAdminOrOwner = viewModel.isAdminOrOwner()
    
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 群聊基本信息
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
                            url = if (uiState.isEditing) uiState.editedAvatarUrl else groupInfo.avatarUrl
                        ),
                        contentDescription = "群头像",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (uiState.isEditing) {
                        OutlinedTextField(
                            value = uiState.editedName,
                            onValueChange = viewModel::updateEditedName,
                            label = { Text("群聊名称") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isAdminOrOwner
                        )
                    } else {
                        Text(
                            text = groupInfo.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (uiState.isEditing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.editedIntroduction,
                            onValueChange = viewModel::updateEditedIntroduction,
                            label = { Text("群聊简介") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isAdminOrOwner,
                            maxLines = 3
                        )
                    } else if (groupInfo.introduction.isNotEmpty()) {
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
        
        // 群聊设置选项
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "群聊设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 进群免审核
                    SettingSwitchItem(
                        title = "进群免审核",
                        subtitle = "允许用户直接加入群聊，无需审核",
                        checked = if (uiState.isEditing) uiState.editedDirectJoin else groupInfo.directJoin,
                        onCheckedChange = viewModel::updateEditedDirectJoin,
                        enabled = uiState.isEditing && isAdminOrOwner
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 历史消息
                    SettingSwitchItem(
                        title = "查看历史消息",
                        subtitle = "允许新成员查看群聊历史消息",
                        checked = if (uiState.isEditing) uiState.editedHistoryMsg else groupInfo.historyMsgEnabled,
                        onCheckedChange = viewModel::updateEditedHistoryMsg,
                        enabled = uiState.isEditing && isAdminOrOwner
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 私有群聊
                    SettingSwitchItem(
                        title = "私有群聊",
                        subtitle = "设置为私有群聊",
                        checked = if (uiState.isEditing) uiState.editedPrivate else groupInfo.isPrivate,
                        onCheckedChange = viewModel::updateEditedPrivate,
                        enabled = uiState.isEditing && isAdminOrOwner
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 群聊分类
                    if (uiState.isEditing) {
                        OutlinedTextField(
                            value = uiState.editedCategoryName,
                            onValueChange = viewModel::updateEditedCategoryName,
                            label = { Text("群聊分类") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isAdminOrOwner
                        )
                    } else {
                        SettingTextItem(
                            title = "群聊分类",
                            value = groupInfo.categoryName
                        )
                    }
                }
            }
        }
        
        // 群聊详细信息（只读）
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "群聊信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    SettingTextItem("群ID", groupInfo.groupId)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingTextItem("成员数量", "${groupInfo.memberCount} 人")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingTextItem("创建者", groupInfo.createBy)
                    if (groupInfo.communityName.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SettingTextItem("所属社区", groupInfo.communityName)
                    }
                }
            }
        }
        
        // 错误提示
        uiState.saveError?.let { error ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        
        // 成功提示
        if (uiState.isSaveSuccess) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "保存成功",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingTextItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
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
