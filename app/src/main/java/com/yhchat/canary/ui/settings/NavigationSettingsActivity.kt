package com.yhchat.canary.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yhchat.canary.data.repository.NavigationRepository
import com.yhchat.canary.data.model.NavigationItem
import com.yhchat.canary.data.model.NavigationConfig
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import kotlin.system.exitProcess

/**
 * 导航栏设置Activity
 */
class NavigationSettingsActivity : ComponentActivity() {
    
    companion object {
        fun start(context: Context, navigationRepository: NavigationRepository) {
            val intent = Intent(context, NavigationSettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 这里我们需要通过其他方式获取NavigationRepository，例如通过Hilt注入
        // 为了简化，我们创建一个新的实例
        val navigationRepository = com.yhchat.canary.data.di.RepositoryFactory.getNavigationRepository(this)
        
        setContent {
            YhchatCanaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationSettingsScreen(
                        navigationRepository = navigationRepository,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

/**
 * 导航栏设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationSettingsScreen(
    navigationRepository: NavigationRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navigationConfig by navigationRepository.navigationConfig.collectAsStateWithLifecycle()
    
    // 使用State管理可编辑的列表
    var items by remember { mutableStateOf(navigationConfig.items.sortedBy { it.order }) }
    var hasChanges by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    
    // 当配置更新时同步items
    LaunchedEffect(navigationConfig) {
        items = navigationConfig.items.sortedBy { it.order }
        hasChanges = false
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = "导航栏管理",
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
                TextButton(
                    onClick = {
                        navigationRepository.resetToDefault()
                    }
                ) {
                    Text("重置")
                }
            }
        )
        
        // 说明文字
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "自定义导航栏",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 点击开关显示/隐藏导航项\n• 使用上下箭头调整显示顺序\n• 修改后需要重启应用才能生效",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 应用更改按钮
        if (hasChanges) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "有未保存的更改",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Button(
                        onClick = {
                            // 保存更改
                            navigationRepository.updateItemsOrder(items)
                            showRestartDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("应用并重启")
                    }
                }
            }
        }
        
        // 导航项列表
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                NavigationItemCard(
                    item = item,
                    index = index,
                    totalItems = items.size,
                    onVisibilityChange = { isVisible ->
                        items = items.map {
                            if (it.id == item.id) it.copy(isVisible = isVisible) else it
                        }
                        hasChanges = true
                    },
                    onMoveUp = {
                        if (index > 0) {
                            items = items.toMutableList().apply {
                                val temp = this[index]
                                this[index] = this[index - 1]
                                this[index - 1] = temp
                            }
                            hasChanges = true
                        }
                    },
                    onMoveDown = {
                        if (index < items.size - 1) {
                            items = items.toMutableList().apply {
                                val temp = this[index]
                                this[index] = this[index + 1]
                                this[index + 1] = temp
                            }
                            hasChanges = true
                        }
                    }
                )
            }
            
            // 添加一些底部空间
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // 重启确认对话框
        if (showRestartDialog) {
            AlertDialog(
                onDismissRequest = { showRestartDialog = false },
                title = { Text("重启应用") },
                text = { Text("更改已保存。应用需要重启才能生效，是否立即重启？") },
                confirmButton = {
                    Button(
                        onClick = {
                            // 重启应用
                            val intent = context.packageManager
                                .getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            (context as? ComponentActivity)?.finish()
                            exitProcess(0)
                        }
                    ) {
                        Text("立即重启")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestartDialog = false }) {
                        Text("稍后")
                    }
                }
            )
        }
    }
}

/**
 * 导航项卡片
 */
@Composable
private fun NavigationItemCard(
    item: NavigationItem,
    index: Int,
    totalItems: Int,
    onVisibilityChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 导航项图标
            Icon(
                imageVector = item.getIcon(),
                contentDescription = item.title,
                modifier = Modifier.size(25.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 导航项信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "位置: 第 ${index + 1} 个",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 上下移动按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "上移",
                        tint = if (index > 0) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalItems - 1
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "下移",
                        tint = if (index < totalItems - 1) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 可见性开关
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (item.isVisible) "显示" else "隐藏",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = item.isVisible,
                    onCheckedChange = onVisibilityChange
                )
            }
        }
    }
}
