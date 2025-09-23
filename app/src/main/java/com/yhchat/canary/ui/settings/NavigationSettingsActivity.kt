package com.yhchat.canary.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
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
                NavigationSettingsScreen(
                    navigationRepository = navigationRepository,
                    onBackClick = { finish() }
                )
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
    val navigationConfig by navigationRepository.navigationConfig.collectAsStateWithLifecycle()
    val allAvailableItems = NavigationConfig.getAllAvailableItems()
    
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
                    text = "• 开启/关闭导航项的显示\n• 未来版本将支持拖拽排序功能\n• 需要重新启动应用才能更新底部导航栏",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }





        }
        
        // 导航项列表
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            items(allAvailableItems, key = { it.id }) { availableItem ->
                val currentItem = navigationConfig.items.find { it.id == availableItem.id }
                    ?: availableItem
                
                NavigationItemCard(
                    item = currentItem,
                    isDragging = false,
                    onVisibilityChange = { isVisible ->
                        navigationRepository.updateItemVisibility(currentItem.id, isVisible)
                    }
                )
            }
            
            // 添加一些底部空间
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 导航项卡片
 */
@Composable
private fun NavigationItemCard(
    item: NavigationItem,
    isDragging: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 导航项图标
            Icon(
                imageVector = item.getIcon(),
                contentDescription = item.title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 导航项信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ID: ${item.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 可见性开关
            Switch(
                checked = item.isVisible,
                onCheckedChange = onVisibilityChange
            )
        }
    }
}
