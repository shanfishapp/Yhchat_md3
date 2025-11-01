package com.yhchat.canary.ui.bot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yhchat.canary.data.api.ApiClient
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.CreatedBot
import com.yhchat.canary.data.model.MyBotListResponse
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import android.widget.Toast

/**
 * 机器人管理Activity
 * 用于管理我创建的机器人
 */
class BotManagementActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_BOT_ID = "botId"
        const val EXTRA_BOT_NAME = "botName"
        const val EXTRA_BOT_TOKEN = "botToken"
        
        fun start(context: Context, botId: String, botName: String, botToken: String = "") {
            val intent = Intent(context, BotManagementActivity::class.java).apply {
                putExtra(EXTRA_BOT_ID, botId)
                putExtra(EXTRA_BOT_NAME, botName)
                putExtra(EXTRA_BOT_TOKEN, botToken)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val botId = intent.getStringExtra(EXTRA_BOT_ID) ?: ""
        val botName = intent.getStringExtra(EXTRA_BOT_NAME) ?: "机器人"
        val botToken = intent.getStringExtra(EXTRA_BOT_TOKEN) ?: ""
        
        if (botId.isEmpty()) {
            finish()
            return
        }
        
        setContent {
            YhchatCanaryTheme {
                BotManagementScreen(
                    botId = botId,
                    botName = botName,
                    botToken = botToken,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BotManagementScreen(
    botId: String,
    botName: String,
    botToken: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 缓存机器人列表数据
    var botList by remember { mutableStateOf<List<CreatedBot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isStoppingBot by remember { mutableStateOf(false) }
    var isDeletingBot by remember { mutableStateOf(false) }
    var botIsStop by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // 获取当前机器人的token
    val currentBotToken = remember(botList, botId) {
        val foundBot = botList.find { it.botId == botId }
        val finalToken = foundBot?.token ?: botToken
        android.util.Log.d("BotManagement", "Current bot: $botId, found token: ${foundBot?.token}, final token: $finalToken")
        finalToken
    }
    
    // 加载机器人列表和详细信息
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val botRepo = RepositoryFactory.getBotRepository(context)
                
                // 加载机器人列表
                botRepo.getMyBotList().fold(
                    onSuccess = { bots ->
                        botList = bots
                        // 调试日志
                        android.util.Log.d("BotManagement", "Loaded ${bots.size} bots")
                        bots.forEach { bot ->
                            android.util.Log.d("BotManagement", "Bot: ${bot.botId}, token: ${bot.token}")
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("BotManagement", "Failed to load bots: ${error.message}")
                    }
                )
                
                // 获取当前机器人的详细信息（包括停用状态）
                botRepo.getBotInfo(botId).fold(
                    onSuccess = { botInfo ->
                        botIsStop = botInfo.data.isStop == 1
                    },
                    onFailure = { /* 忽略错误 */ }
                )
                
            } catch (e: Exception) {
                // 网络错误，保持空列表
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "管理机器人",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 错误提示
            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // 机器人基本信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = botName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ID: $botId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 管理选项卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "管理选项",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // 查看详情
                    ManagementOption(
                        icon = Icons.Default.Info,
                        title = "查看详情",
                        subtitle = "查看机器人详细信息",
                        onClick = {
                            BotDetailActivity.start(context, botId, botName, 3)
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 进入聊天
                    ManagementOption(
                        icon = Icons.Default.Chat,
                        title = "进入聊天",
                        subtitle = "与机器人对话",
                        onClick = {
                            val intent = Intent(context, com.yhchat.canary.ui.chat.ChatActivity::class.java).apply {
                                putExtra("chatId", botId)
                                putExtra("chatType", 3)
                                putExtra("chatName", botName)
                            }
                            context.startActivity(intent)
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 编辑机器人
                    ManagementOption(
                        icon = Icons.Default.Edit,
                        title = "编辑机器人",
                        subtitle = "修改机器人信息",
                        onClick = {
                            BotEditActivity.start(context, botId, botName)
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 机器人设置
                    ManagementOption(
                        icon = Icons.Default.Settings,
                        title = "机器人设置",
                        subtitle = "配置机器人参数",
                        onClick = {
                            BotSettingsActivity.start(context, botId, botName, currentBotToken)
                        }
                    )
                }
            }
            
            // 机器人控制卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "机器人控制",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 停用/启用机器人按钮
                        Button(
                            onClick = {
                                scope.launch {
                                    val tokenRepo = RepositoryFactory.getTokenRepository(context)
                                    val userToken = tokenRepo.getTokenSync() ?: return@launch
                                    isStoppingBot = true
                                    error = null
                                    
                                    // 构建 protobuf 请求
                                    val operation = if (botIsStop) 0 else 1 // 当前停用则启用(0)，当前启用则停用(1)
                                    val request = yh_bot.Bot.bot_stop_send.newBuilder()
                                        .setBotId(botId)
                                        .setOperation(operation)
                                        .build()
                                    
                                    val requestBody = request.toByteArray()
                                        .toRequestBody("application/x-protobuf".toMediaType())
                                    
                                    runCatching {
                                        ApiClient.apiService.stopBot(userToken, requestBody)
                                    }.onSuccess { resp ->
                                        if (resp.isSuccessful) {
                                            val responseBody = resp.body()?.bytes()
                                            if (responseBody != null) {
                                                val status = yh_bot.Bot.Status.parseFrom(responseBody)
                                                if (status.code == 1) {
                                                    val action = if (operation == 1) "停用" else "启用"
                                                    Toast.makeText(context, "机器人${action}成功", Toast.LENGTH_SHORT).show()
                                                    
                                                    // 重新查询机器人状态
                                                    val botRepo = RepositoryFactory.getBotRepository(context)
                                                    botRepo.getBotInfo(botId).fold(
                                                        onSuccess = { botInfo ->
                                                            botIsStop = botInfo.data.isStop == 1
                                                            isStoppingBot = false
                                                        },
                                                        onFailure = { 
                                                            // 如果查询失败，使用预期状态
                                                            botIsStop = !botIsStop
                                                            isStoppingBot = false
                                                        }
                                                    )
                                                } else {
                                                    error = status.msg
                                                    isStoppingBot = false
                                                }
                                            } else {
                                                error = "响应数据为空"
                                                isStoppingBot = false
                                            }
                                        } else {
                                            error = "请求失败: ${resp.code()}"
                                            isStoppingBot = false
                                        }
                                    }.onFailure { e ->
                                        isStoppingBot = false
                                        error = e.message
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isStoppingBot && !isLoading && !isDeletingBot,
                            colors = if (botIsStop) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            }
                        ) {
                            if (isStoppingBot) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("处理中...")
                            } else {
                                Text(if (botIsStop) "启用机器人" else "停用机器人")
                            }
                        }
                        
                        // 删除机器人按钮
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val tokenRepo = RepositoryFactory.getTokenRepository(context)
                                    val userToken = tokenRepo.getTokenSync() ?: return@launch
                                    isDeletingBot = true
                                    error = null
                                    
                                    val request = com.yhchat.canary.data.model.DeleteFriendRequest(
                                        chatId = botId,
                                        chatType = 3 // 机器人
                                    )
                                    
                                    runCatching {
                                        ApiClient.apiService.deleteFriend(userToken, request)
                                    }.onSuccess { resp ->
                                        isDeletingBot = false
                                        if (resp.body()?.code == 1) {
                                            Toast.makeText(context, "机器人删除成功", Toast.LENGTH_SHORT).show()
                                            // 删除成功后返回上一页
                                            onBackClick()
                                        } else {
                                            error = resp.body()?.message ?: "删除失败"
                                        }
                                    }.onFailure { e ->
                                        isDeletingBot = false
                                        error = e.message
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isDeletingBot && !isLoading && !isStoppingBot,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            if (isDeletingBot) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("删除中...")
                            } else {
                                Text("删除机器人")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

