package com.yhchat.canary.ui.group

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yhchat.canary.data.api.GroupTag
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupTagManagementActivity : ComponentActivity() {
    
    private val viewModel: GroupTagManagementViewModel by viewModels()
    
    companion object {
        private const val EXTRA_GROUP_ID = "group_id"
        private const val EXTRA_GROUP_NAME = "group_name"
        
        fun start(context: Context, groupId: String, groupName: String) {
            val intent = Intent(context, GroupTagManagementActivity::class.java).apply {
                putExtra(EXTRA_GROUP_ID, groupId)
                putExtra(EXTRA_GROUP_NAME, groupName)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID) ?: run {
            finish()
            return
        }
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: "群聊"
        
        setContent {
            YhchatCanaryTheme {
                GroupTagManagementScreen(
                    groupId = groupId,
                    groupName = groupName,
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onTagClick = { tag ->
                        // 跳转到标签详情页面
                        GroupTagDetailActivity.start(this, groupId, tag.id, tag.tag)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTagManagementScreen(
    groupId: String,
    groupName: String,
    viewModel: GroupTagManagementViewModel,
    onBackClick: () -> Unit,
    onTagClick: (GroupTag) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(groupId) {
        viewModel.loadTags(groupId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("标签管理", fontWeight = FontWeight.Bold) },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "创建标签")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.tags.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null && uiState.tags.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadTags(groupId) }) {
                            Text("重试")
                        }
                    }
                }
                uiState.tags.isEmpty() -> {
                    Text(
                        text = "暂无标签\n点击右下角按钮创建标签",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.tags) { tag ->
                            TagCard(
                                tag = tag,
                                onClick = { onTagClick(tag) },
                                onEditClick = { viewModel.showEditDialog(tag) },
                                onDeleteClick = { viewModel.deleteTag(tag.id, groupId) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 创建/编辑标签对话框
    if (uiState.showCreateDialog) {
        TagEditDialog(
            tag = uiState.editingTag,
            tagName = uiState.editingTagName,
            tagColor = uiState.editingTagColor,
            tagDesc = uiState.editingTagDesc,
            isSaving = uiState.isSaving,
            error = uiState.saveError,
            onTagNameChange = viewModel::updateTagName,
            onTagColorChange = viewModel::updateTagColor,
            onTagDescChange = viewModel::updateTagDesc,
            onConfirm = { viewModel.saveTag(groupId) },
            onDismiss = viewModel::dismissDialog
        )
    }
}

@Composable
fun TagCard(
    tag: GroupTag,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 标签颜色指示器
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = try {
                            Color(tag.color.toColorInt())
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        },
                        shape = CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 标签信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tag.tag,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (tag.desc.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tag.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 操作按钮
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除标签") },
            text = { Text("确定要删除标签 ${tag.tag} 吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun TagEditDialog(
    tag: GroupTag?,
    tagName: String,
    tagColor: String,
    tagDesc: String,
    isSaving: Boolean,
    error: String?,
    onTagNameChange: (String) -> Unit,
    onTagColorChange: (String) -> Unit,
    onTagDescChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val predefinedColors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFC107", "#FF9800", "#FF5722", "#795548"
    )
    
    AlertDialog(
        onDismissRequest = if (!isSaving) onDismiss else { {} },
        title = { Text(if (tag != null) "编辑标签" else "创建标签") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = onTagNameChange,
                    label = { Text("标签名称") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = tagDesc,
                    onValueChange = onTagDescChange,
                    label = { Text("标签描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    maxLines = 3
                )
                
                // 颜色选择
                Column {
                    Text(
                        text = "标签颜色",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 颜色网格
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        predefinedColors.chunked(8).forEach { rowColors ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowColors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                color = Color(color.toColorInt()),
                                                shape = CircleShape
                                            )
                                            .clickable(enabled = !isSaving) {
                                                onTagColorChange(color)
                                            }
                                            .then(
                                                if (color == tagColor) {
                                                    Modifier.padding(4.dp)
                                                } else Modifier
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 错误提示
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving && tagName.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (tag != null) "保存" else "创建")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("取消")
            }
        }
    )
}

