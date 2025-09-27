package com.yhchat.canary.ui.community

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import com.yhchat.canary.data.repository.DraftRepository
import com.yhchat.canary.data.model.Draft
import java.text.SimpleDateFormat
import java.util.*

/**
 * 草稿箱Activity
 */
class DraftBoxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val token = intent.getStringExtra("token") ?: ""
        
        setContent {
            YhchatCanaryTheme {
                val view = LocalView.current
                val darkTheme = isSystemInDarkTheme()
                
                SideEffect {
                    val window = (view.context as ComponentActivity).window
                    window.statusBarColor = Color.Transparent.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                    WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
                }
                DraftBoxScreen(
                    token = token,
                    onBackClick = { finish() },
                    onDraftClick = { draft ->
                        // 返回到CreatePostActivity并加载草稿
                        val intent = Intent(this@DraftBoxActivity, CreatePostActivity::class.java).apply {
                            putExtra("board_id", draft.boardId)
                            putExtra("board_name", draft.boardName)
                            putExtra("token", token)
                            putExtra("draft_id", draft.id)
                            putExtra("draft_title", draft.title)
                            putExtra("draft_content", draft.content)
                            putExtra("draft_markdown_mode", draft.isMarkdownMode)
                        }
                        startActivity(intent)
                        finish()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                )
            }
        }
    }
}

/**
 * 草稿箱界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftBoxScreen(
    token: String,
    onBackClick: () -> Unit,
    onDraftClick: (Draft) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val draftRepository = remember { DraftRepository(context) }
    
    // 草稿数据状态
    var drafts by remember { mutableStateOf(listOf<Draft>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // 加载草稿数据
    LaunchedEffect(Unit) {
        drafts = draftRepository.getDrafts()
        isLoading = false
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
                    text = "草稿箱",
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
        
        // 草稿列表
        if (isLoading) {
            // 加载状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (drafts.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "暂无草稿",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "在编写文章时保存的草稿会出现在这里",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(drafts) { draft ->
                    DraftItem(
                        draft = draft,
                        onClick = { onDraftClick(draft) },
                        onDelete = { 
                            draftRepository.deleteDraft(draft.id)
                            drafts = drafts.filter { it.id != draft.id }
                        }
                    )
                }
            }
        }
    }
}
}

/**
 * 草稿项
 */
@Composable
fun DraftItem(
    draft: Draft,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 标题
                Text(
                    text = if (draft.title.isNotBlank()) draft.title else "无标题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (draft.title.isNotBlank()) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 内容预览
                if (draft.content.isNotBlank()) {
                    Text(
                        text = draft.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // 分区和时间信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = draft.boardName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    if (draft.isMarkdownMode) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "MD",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    Text(
                        text = formatTime(draft.updateTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 删除按钮
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除草稿",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 格式化时间
 */
private fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diffInMillis = now.time - date.time
    val diffInHours = diffInMillis / (1000 * 60 * 60)
    
    return when {
        diffInHours < 1 -> "刚刚"
        diffInHours < 24 -> "${diffInHours}小时前"
        diffInHours < 24 * 7 -> "${diffInHours / 24}天前"
        else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(date)
    }
}

