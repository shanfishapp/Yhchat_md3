package com.yhchat.canary.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yhchat.canary.data.repository.NavigationRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.UserRepository
import com.yhchat.canary.data.model.UserProfile

/**
 * 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigationRepository: NavigationRepository? = null,
    tokenRepository: TokenRepository? = null,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 获取用户信息
    var userEmail by remember { mutableStateOf("") }
    
    LaunchedEffect(tokenRepository) {
        tokenRepository?.let { tokenRepo: TokenRepository ->
            val userRepo: UserRepository = com.yhchat.canary.data.di.RepositoryFactory.getUserRepository(context)
            userRepo.setTokenRepository(tokenRepo)
            userRepo.getUserProfile().onSuccess { profile: UserProfile ->
                userEmail = profile.email ?: ""
            }
        }
    }
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        )
        
        // 设置项列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 底部导航栏设置
            item {
                SettingsCard(
                    title = "界面设置",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Menu,
                            title = "底部导航栏设置",
                            subtitle = "自定义导航栏显示和排序",
                            onClick = {
                                navigationRepository?.let {
                                    NavigationSettingsActivity.start(context, it)
                                }
                            }
                        )
                    )
                )
            }
            
            // 账户设置
            item {
                SettingsCard(
                    title = "账户设置",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Lock,
                            title = "修改密码",
                            subtitle = "更改账户登录密码",
                            onClick = {
                                // 启动修改密码Activity，传递用户邮箱
                                val intent = ChangePasswordActivity.createIntent(context, userEmail)
                                context.startActivity(intent)
                            }
                        )
                    )
                )
            }
            
            // 内容设置
            item {
                SettingsCard(
                    title = "内容设置",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Web,
                            title = "HTML设置",
                            subtitle = "网页内容显示设置",
                            onClick = {
                                HtmlSettingsActivity.start(context)
                            }
                        )
                    )
                )
            }
            
            // 账户设置
            item {
                SettingsCard(
                    title = "账户管理",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.ExitToApp,
                            title = "退出登录",
                            subtitle = "安全退出当前账户",
                            onClick = {
                                showLogoutDialog = true
                            },
                            isDestructive = true
                        )
                    )
                )
            }
        }
        
        // 退出登录确认对话框
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text("确认退出登录")
                },
                text = {
                    Text("退出登录后需要重新输入账号密码，确定要退出吗？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        }
                    ) {
                        Text("退出", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLogoutDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 设置项数据类
 */
data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false
)

/**
 * 设置卡片组件
 */
@Composable
private fun SettingsCard(
    title: String,
    items: List<SettingsItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            items.forEachIndexed { index, item ->
                SettingsItemRow(
                    item = item,
                    showDivider = index < items.size - 1
                )
            }
        }
    }
}

/**
 * 设置项行组件
 */
@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Card(
            onClick = item.onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp),
                    tint = if (item.isDestructive) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (item.isDestructive) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "前往",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (showDivider) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
