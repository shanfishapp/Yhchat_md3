package com.yhchat.canary.ui.community

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

/**
 * 创建文章Activity
 */
class CreatePostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val boardId = intent.getIntExtra("board_id", 0)
        val boardName = intent.getStringExtra("board_name") ?: "发布文章"
        val token = intent.getStringExtra("token") ?: ""
        
        setContent {
            YhchatCanaryTheme {
                val viewModel: CreatePostViewModel = viewModel {
                    CreatePostViewModel(
                        communityRepository = RepositoryFactory.getCommunityRepository(this@CreatePostActivity),
                        tokenRepository = RepositoryFactory.getTokenRepository(this@CreatePostActivity)
                    )
                }
                
                CreatePostScreen(
                    boardId = boardId,
                    boardName = boardName,
                    token = token,
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onPostCreated = { finish() }
                )
            }
        }
    }
}

/**
 * 创建文章界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    boardId: Int,
    boardName: String,
    token: String,
    viewModel: CreatePostViewModel,
    onBackClick: () -> Unit,
    onPostCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isMarkdownMode by remember { mutableStateOf(false) }
    
    val createPostState by viewModel.createPostState.collectAsState()
    
    // 监听创建结果
    LaunchedEffect(createPostState.isSuccess) {
        if (createPostState.isSuccess) {
            onPostCreated()
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = "发布到: $boardName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
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
                IconButton(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            viewModel.createPost(
                                token = token,
                                boardId = boardId,
                                title = title.trim(),
                                content = content.trim(),
                                contentType = if (isMarkdownMode) 2 else 1
                            )
                        }
                    },
                    enabled = title.isNotBlank() && content.isNotBlank() && !createPostState.isLoading
                ) {
                    if (createPostState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "发布"
                        )
                    }
                }
            }
        )
        
        // 错误提示
        createPostState.error?.let { error ->
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
            
            // 发布按钮
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        viewModel.createPost(
                            token = token,
                            boardId = boardId,
                            title = title.trim(),
                            content = content.trim(),
                            contentType = if (isMarkdownMode) 2 else 1
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && content.isNotBlank() && !createPostState.isLoading
            ) {
                if (createPostState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (createPostState.isLoading) "发布中..." else "发布文章")
            }
        }
    }
}
