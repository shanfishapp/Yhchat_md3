package com.yhchat.canary.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    onBack: (() -> Unit)? = null,
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
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
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
                            icon = Icons.Default.Devices,
                            title = "在线设备",
                            subtitle = "查看当前登录的设备",
                            onClick = {
                                tokenRepository?.let { tokenRepo ->
                                    OnlineDevicesActivity.start(context, tokenRepo)
                                }
                            }
                        ),
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
                PersonalizationSettingsCard(context = context)
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

            Spacer(modifier = Modifier.height(8.dp))

            // 机器人看板显示开关
            BotBoardSettingItem(context = context)

            Spacer(modifier = Modifier.height(8.dp))

            // 菜单按钮栏显示开关
            MenuButtonsSettingItem(context = context)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // WebP压缩质量设置
            WebPQualitySettingItem(context = context)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // HTML消息显示原文开关
            HtmlRawTextSettingItem(context = context)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Markdown消息显示原文开关
            MarkdownRawTextSettingItem(context = context)
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

/**
 * 机器人看板显示设置项
 */
@Composable
private fun BotBoardSettingItem(
    context: Context,
    modifier: Modifier = Modifier
) {
    val prefs = remember { 
        context.getSharedPreferences("chat_settings", Context.MODE_PRIVATE) 
    }
    
    var showBotBoard by remember { 
        mutableStateOf(prefs.getBoolean("show_bot_board", true)) 
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
                imageVector = Icons.Default.Dashboard,
                contentDescription = "机器人看板",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "显示机器人看板",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (showBotBoard) "在聊天界面显示群聊机器人看板和单机器人看板" else "隐藏所有机器人看板",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Switch(
                checked = showBotBoard,
                onCheckedChange = { checked ->
                    showBotBoard = checked
                    prefs.edit().putBoolean("show_bot_board", checked).apply()
                }
            )
        }
    }
}

/**
 * 菜单按钮栏显示设置项
 */
@Composable
private fun MenuButtonsSettingItem(
    context: Context,
    modifier: Modifier = Modifier
) {
    val prefs = remember { 
        context.getSharedPreferences("chat_settings", Context.MODE_PRIVATE) 
    }
    
    var showMenuButtons by remember { 
        mutableStateOf(prefs.getBoolean("show_menu_buttons", true)) 
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
                imageVector = Icons.Default.Apps,
                contentDescription = "菜单按钮栏",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "显示菜单按钮栏",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (showMenuButtons) "在聊天输入框上方显示菜单按钮栏" else "隐藏菜单按钮栏",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Switch(
                checked = showMenuButtons,
                onCheckedChange = { checked ->
                    showMenuButtons = checked
                    prefs.edit().putBoolean("show_menu_buttons", checked).apply()
                }
            )
        }
    }
}

/**
 * 个性化设置卡片
 */
@Composable
private fun PersonalizationSettingsCard(
    context: Context,
    modifier: Modifier = Modifier
) {
    var showColorPickerDialog by remember { mutableStateOf(false) }
    
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
                text = "个性化",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // 聊天背景设置
            Card(
                onClick = {
                    com.yhchat.canary.ui.background.ChatBackgroundActivity.start(
                        context,
                        "all",
                        "全局"
                    )
                },
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
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = "聊天背景",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "聊天背景",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "设置全局聊天背景",
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 主题颜色设置
            ThemeColorSettingItem(
                context = context,
                onColorPickerClick = { showColorPickerDialog = true }
            )
        }
    }
    
    // 颜色选择对话框
    if (showColorPickerDialog) {
        ColorPickerDialog(
            context = context,
            onDismiss = { showColorPickerDialog = false }
        )
    }
}

/**
 * 主题颜色设置项
 */
@Composable
private fun ThemeColorSettingItem(
    context: Context,
    onColorPickerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = remember { 
        context.getSharedPreferences("theme_settings", Context.MODE_PRIVATE) 
    }
    
    val currentColorInt = prefs.getInt("custom_primary_color", 0xFF6200EE.toInt())
    val currentColor = Color(currentColorInt)
    val isCustomColor = currentColorInt != 0xFF6200EE.toInt()
    
    Card(
        onClick = onColorPickerClick,
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
                imageVector = Icons.Default.Palette,
                contentDescription = "主题颜色",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "主题颜色",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isCustomColor) {
                        "已自定义（会覆盖莫奈取色，需重启生效）"
                    } else {
                        "使用默认配色或莫奈取色（Android 12+）"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 当前颜色预览
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "编辑",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 颜色选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val prefs = remember { 
        context.getSharedPreferences("theme_settings", Context.MODE_PRIVATE) 
    }
    
    val currentColorInt = prefs.getInt("custom_primary_color", 0xFF6200EE.toInt())
    var selectedColor by remember { mutableStateOf(Color(currentColorInt)) }
    var colorInput by remember { 
        mutableStateOf(String.format("#%06X", currentColorInt and 0xFFFFFF)) 
    }
    var errorMessage by remember { mutableStateOf("") }
    
    // 预设颜色
    val presetColors = listOf(
        Color(0xFF6200EE), // 默认紫色
        Color(0xFFFF5722), // 橙红色
        Color(0xFFF44336), // 红色
        Color(0xFFE91E63), // 粉色
        Color(0xFF9C27B0), // 紫色
        Color(0xFF673AB7), // 深紫色
        Color(0xFF3F51B5), // 靛蓝色
        Color(0xFF2196F3), // 蓝色
        Color(0xFF03A9F4), // 浅蓝色
        Color(0xFF00BCD4), // 青色
        Color(0xFF009688), // 蓝绿色
        Color(0xFF4CAF50), // 绿色
        Color(0xFF8BC34A), // 浅绿色
        Color(0xFFCDDC39), // 黄绿色
        Color(0xFFFFEB3B), // 黄色
        Color(0xFFFFC107), // 琥珀色
        Color(0xFFFF9800), // 橙色
        Color(0xFF795548), // 棕色
        Color(0xFF607D8B), // 蓝灰色
        Color(0xFF000000)  // 黑色
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "自定义主题颜色",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 颜色预览
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(3.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "当前颜色",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = colorInput,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 颜色输入框
                OutlinedTextField(
                    value = colorInput,
                    onValueChange = { input ->
                        colorInput = input.uppercase()
                        errorMessage = ""
                        
                        // 验证并解析颜色
                        if (input.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                            try {
                                val colorInt = android.graphics.Color.parseColor(input)
                                selectedColor = Color(colorInt)
                            } catch (e: Exception) {
                                errorMessage = "无效的颜色值"
                            }
                        } else if (input.isNotEmpty() && !input.startsWith("#")) {
                            errorMessage = "颜色值必须以 # 开头"
                        } else if (input.length > 7) {
                            errorMessage = "颜色值格式: #RRGGBB (6位十六进制)"
                        }
                    },
                    label = { Text("颜色值") },
                    placeholder = { Text("#6200EE") },
                    supportingText = {
                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("格式: #RRGGBB (例如 #6200EE)")
                        }
                    },
                    isError = errorMessage.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = "颜色值"
                        )
                    }
                )
                
                // 预设颜色选择器
                Text(
                    text = "快速选择",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // 颜色网格
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.chunked(5).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (color == selectedColor) 3.dp else 1.dp,
                                            color = if (color == selectedColor) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedColor = color
                                            colorInput = String.format("#%06X", color.toArgb() and 0xFFFFFF)
                                            errorMessage = ""
                                        }
                                )
                            }
                        }
                    }
                }
                
                // 重置按钮
                TextButton(
                    onClick = {
                        selectedColor = Color(0xFF6200EE)
                        colorInput = "#6200EE"
                        errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重置",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重置为默认颜色")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 保存颜色
                    if (colorInput.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                        val colorInt = android.graphics.Color.parseColor(colorInput)
                        prefs.edit().putInt("custom_primary_color", colorInt).apply()
                        android.widget.Toast.makeText(
                            context,
                            "主题颜色已保存，重启应用后生效",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        onDismiss()
                    } else {
                        errorMessage = "请输入有效的颜色值"
                    }
                },
                enabled = colorInput.matches(Regex("^#[0-9A-Fa-f]{6}$"))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * WebP压缩质量设置项
 */
@Composable
private fun WebPQualitySettingItem(
    context: Context,
    modifier: Modifier = Modifier
) {
    val prefs = remember { 
        context.getSharedPreferences("image_settings", Context.MODE_PRIVATE) 
    }
    
    var webpQuality by remember { 
        mutableFloatStateOf(prefs.getInt("webp_quality", 95).toFloat()) 
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
                    imageVector = Icons.Default.Image,
                    contentDescription = "图片压缩",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WebP压缩质量",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "当前: ${webpQuality.toInt()}% (数值越高质量越好，文件越大)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 滑动条
            Slider(
                value = webpQuality,
                onValueChange = { newValue ->
                    webpQuality = newValue
                },
                onValueChangeFinished = {
                    // 保存设置
                    prefs.edit()
                        .putInt("webp_quality", webpQuality.toInt())
                        .apply()
                },
                valueRange = 10f..100f,
                steps = 17, // 10-100，每5一个步长，共18个值，所以17个步长
                modifier = Modifier.fillMaxWidth()
            )
            
            // 质量说明
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "低质量 (10%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "高质量 (100%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * HTML消息显示原文设置项
 */
@Composable
private fun HtmlRawTextSettingItem(
    context: Context,
    modifier: Modifier = Modifier
) {
    val prefs = remember { 
        context.getSharedPreferences("message_settings", Context.MODE_PRIVATE) 
    }
    
    var showHtmlRawText by remember { 
        mutableStateOf(prefs.getBoolean("show_html_raw_text", false)) 
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
                imageVector = Icons.Default.Code,
                contentDescription = "HTML原文显示",
                modifier = Modifier.size(24.dp),
                tint = if (showHtmlRawText) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "HTML消息显示原文",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (showHtmlRawText) "显示HTML源代码而不是渲染后的网页" else "正常渲染HTML消息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Switch(
                checked = showHtmlRawText,
                onCheckedChange = { checked ->
                    showHtmlRawText = checked
                    prefs.edit().putBoolean("show_html_raw_text", checked).apply()
                }
            )
        }
    }
}

/**
 * Markdown消息显示原文设置项
 */
@Composable
private fun MarkdownRawTextSettingItem(
    context: Context,
    modifier: Modifier = Modifier
) {
    val prefs = remember { 
        context.getSharedPreferences("message_settings", Context.MODE_PRIVATE) 
    }
    
    var showMarkdownRawText by remember { 
        mutableStateOf(prefs.getBoolean("show_markdown_raw_text", false)) 
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
                imageVector = Icons.Default.TextFields,
                contentDescription = "Markdown原文显示",
                modifier = Modifier.size(24.dp),
                tint = if (showMarkdownRawText) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Markdown消息显示原文",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (showMarkdownRawText) "显示Markdown源代码而不是渲染后的格式" else "正常渲染Markdown消息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Switch(
                checked = showMarkdownRawText,
                onCheckedChange = { checked ->
                    showMarkdownRawText = checked
                    prefs.edit().putBoolean("show_markdown_raw_text", checked).apply()
                }
            )
        }
    }
}
