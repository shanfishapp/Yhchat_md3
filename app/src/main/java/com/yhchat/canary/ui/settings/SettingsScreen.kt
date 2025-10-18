package com.yhchat.canary.ui.settings

import android.content.Context
import androidx.compose.foundation.background
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
            
            // 显示设置
            item {
                DisplaySettingsCard(context = context)
            }
            
            // 个性化设置
            item {
                SettingsCard(
                    title = "个性化",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Wallpaper,
                            title = "聊天背景",
                            subtitle = "设置全局聊天背景",
                            onClick = {
                                com.yhchat.canary.ui.background.ChatBackgroundActivity.start(
                                    context,
                                    "all",  // 设置全局背景
                                    "全局"
                                )
                            }
                        )
                    )
                )
            }
            
            // 关于应用
            item {
                SettingsCard(
                    title = "关于",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "应用详情",
                            subtitle = "查看应用版本和开发者信息",
                            onClick = {
                                AppInfoActivity.start(context)
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
 * 显示设置卡片
 */
@Composable
private fun DisplaySettingsCard(
    context: Context,
    modifier: Modifier = Modifier
) {
    val prefs = remember { 
        context.getSharedPreferences("display_settings", Context.MODE_PRIVATE) 
    }
    
    var showStickyConversations by remember { 
        mutableStateOf(prefs.getBoolean("show_sticky_conversations", true)) 
    }
    
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
                text = "显示设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // 置顶会话显示开关
            Card(
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
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "置顶会话",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "显示置顶会话",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "在会话列表中显示置顶的会话",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Switch(
                        checked = showStickyConversations,
                        onCheckedChange = { checked ->
                            showStickyConversations = checked
                            prefs.edit().putBoolean("show_sticky_conversations", checked).apply()
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 字体大小调节
            FontSizeSettingItem(context = context)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 全局组件大小（无极调节）
            GlobalScaleSettingItem(context = context)

            Spacer(modifier = Modifier.height(8.dp))

            // 省流量模式
            DataSaverSettingItem(context = context)

            Spacer(modifier = Modifier.height(8.dp))

            // WebSocket 连接开关
            WebSocketSettingItem(context = context)
        }
    }
}

/**
 * WebSocket 设置项
 */
@Composable
private fun WebSocketSettingItem(
    context: Context,
    modifier: Modifier = Modifier
) {
    val prefs = remember { 
        context.getSharedPreferences("display_settings", Context.MODE_PRIVATE) 
    }
    
    var disableWebSocket by remember { 
        mutableStateOf(prefs.getBoolean("disable_websocket", false)) 
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
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
                imageVector = Icons.Default.CloudOff,
                contentDescription = "禁用 WebSocket",
                modifier = Modifier.size(24.dp),
                tint = if (disableWebSocket) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "禁用 WebSocket",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (disableWebSocket) "已禁用实时消息推送，需重启应用生效" else "启用实时消息推送",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (disableWebSocket) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Switch(
                checked = disableWebSocket,
                onCheckedChange = { checked ->
                    disableWebSocket = checked
                    prefs.edit().putBoolean("disable_websocket", checked).apply()
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

/**
 * 字体大小设置项
 */
@Composable
private fun FontSizeSettingItem(
    context: Context,
    modifier: Modifier = Modifier
) {
    val prefs = remember { 
        context.getSharedPreferences("display_settings", Context.MODE_PRIVATE) 
    }
    
    var fontScale by remember { 
        mutableStateOf(prefs.getFloat("font_scale", 100f)) 
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FormatSize,
                    contentDescription = "字体大小",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "字体大小",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${fontScale.toInt()}% (重启应用生效)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 滑动条
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Slider(
                    value = fontScale,
                    onValueChange = { newValue ->
                        fontScale = newValue
                        prefs.edit().putFloat("font_scale", newValue).apply()
                    },
                    valueRange = 1f..100f,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 全局组件大小无极调节
 */
@Composable
private fun GlobalScaleSettingItem(
    context: Context,
    modifier: Modifier = Modifier
) {
    val prefs = remember {
        context.getSharedPreferences("display_settings", Context.MODE_PRIVATE)
    }

    var scale by remember {
        mutableStateOf(prefs.getFloat("global_scale", 1.0f))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "组件大小",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "组件大小",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format("%.0f%% (部分界面需重启生效)", scale * 100f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "50%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                Slider(
                    value = scale,
                    onValueChange = { newValue ->
                        val clamped = newValue.coerceIn(0.5f, 1.5f)
                        scale = clamped
                        prefs.edit().putFloat("global_scale", clamped).apply()
                    },
                    valueRange = 0.5f..1.5f,
                    steps = 0,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "150%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 省流量模式（不加载全局任何图片）
 */
@Composable
private fun DataSaverSettingItem(
    context: Context,
    modifier: Modifier = Modifier
) {
    val prefs = remember {
        context.getSharedPreferences("display_settings", Context.MODE_PRIVATE)
    }

    var dataSaver by remember {
        mutableStateOf(prefs.getBoolean("data_saver", false))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
                imageVector = Icons.Default.DataSaverOn,
                contentDescription = "省流量模式",
                modifier = Modifier.size(24.dp),
                tint = if (dataSaver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "省流量模式",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (dataSaver) "不加载全局图片（头像/消息图片/背景等）" else "默认关闭",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = dataSaver,
                onCheckedChange = { checked ->
                    dataSaver = checked
                    prefs.edit().putBoolean("data_saver", checked).apply()
                }
            )
        }
    }
}
