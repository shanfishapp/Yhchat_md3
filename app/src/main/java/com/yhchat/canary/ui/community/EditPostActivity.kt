package com.yhchat.canary.ui.community

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

/**
 * 编辑文章Activity
 */
class EditPostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val postId = intent.getIntExtra("post_id", 0)
        val token = intent.getStringExtra("token") ?: ""
        val originalTitle = intent.getStringExtra("original_title") ?: ""
        val originalContent = intent.getStringExtra("original_content") ?: ""
        val contentType = intent.getIntExtra("content_type", 1)
        
        setContent {
            YhchatCanaryTheme {
                val viewModel: EditPostViewModel = viewModel {
                    EditPostViewModel(
                        communityRepository = RepositoryFactory.getCommunityRepository(this@EditPostActivity),
                        tokenRepository = RepositoryFactory.getTokenRepository(this@EditPostActivity)
                    )
                }
                
                EditPostScreen(
                    postId = postId,
                    token = token,
                    originalTitle = originalTitle,
                    originalContent = originalContent,
                    originalContentType = contentType,
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onPostUpdated = { finish() }
                )
            }
        }
    }
}

/**
 * 编辑文章界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    postId: Int,
    token: String,
    originalTitle: String,
    originalContent: String,
    originalContentType: Int,
    viewModel: EditPostViewModel,
    onBackClick: () -> Unit,
    onPostUpdated: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(originalTitle) }
    var content by remember { mutableStateOf(originalContent) }
    var isMarkdownMode by remember { mutableStateOf(originalContentType == 2) }
    
    val editPostState by viewModel.editPostState.collectAsState()
    
    // 控制退出确认对话框
    var showExitDialog by remember { mutableStateOf(false) }
    
    // 监听编辑结果
    LaunchedEffect(editPostState.isSuccess) {
        if (editPostState.isSuccess) {
            onPostUpdated()
        }
    }
    
    // 处理返回按键
    BackHandler {
        if (title != originalTitle || content != originalContent || 
            (if (isMarkdownMode) 2 else 1) != originalContentType) {
            showExitDialog = true
        } else {
            onBackClick()
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
                    Text(
                        text = "编辑文章",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (title != originalTitle || content != originalContent || 
                            (if (isMarkdownMode) 2 else 1) != originalContentType) {
                            showExitDialog = true
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 保存按钮
                    IconButton(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                viewModel.editPost(
                                    token = token,
                                    postId = postId,
                                    title = title.trim(),
                                    content = content.trim(),
                                    contentType = if (isMarkdownMode) 2 else 1
                                )
                            }
                        },
                        enabled = title.isNotBlank() && content.isNotBlank() && !editPostState.isLoading
                    ) {
                        if (editPostState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "保存"
                            )
                        }
                    }
                }
            )
            
            // 错误提示
            editPostState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题输入
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("文章标题") },
                    placeholder = { Text("请输入文章标题...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Markdown模式切换
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Markdown模式",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isMarkdownMode,
                        onCheckedChange = { isMarkdownMode = it }
                    )
                }
                
                // 内容输入
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { 
                        Text(if (isMarkdownMode) "文章内容 (Markdown)" else "文章内容") 
                    },
                    placeholder = { 
                        Text(
                            if (isMarkdownMode) 
                                "支持Markdown语法，如：\n# 标题\n**粗体**\n*斜体*\n- 列表项" 
                            else 
                                "请输入文章内容..."
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    minLines = 10
                )
                
                // 提示信息
                if (isMarkdownMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Markdown语法提示：",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• # 一级标题  ## 二级标题\n• **粗体文本**  *斜体文本*\n• - 无序列表  1. 有序列表\n• `代码`  ```代码块```\n• [链接](URL)  ![图片](URL)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // 保存按钮
                Button(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            viewModel.editPost(
                                token = token,
                                postId = postId,
                                title = title.trim(),
                                content = content.trim(),
                                contentType = if (isMarkdownMode) 2 else 1
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank() && content.isNotBlank() && !editPostState.isLoading
                ) {
                    if (editPostState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (editPostState.isLoading) "保存中..." else "保存修改")
                }
            }
        }
        
        // 退出确认对话框
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = {
                    Text("放弃修改")
                },
                text = {
                    Text("您有未保存的修改，确定要放弃吗？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onBackClick()
                        }
                    ) {
                        Text("放弃")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showExitDialog = false }
                    ) {
                        Text("继续编辑")
                    }
                }
            )
        }
    }
}
