package com.yhchat.canary.ui.bot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yhchat.canary.data.api.ApiClient
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.BotEventEditRequest
import com.yhchat.canary.data.model.BotIdRequest
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import kotlinx.coroutines.launch
import android.widget.Toast

class BotSettingsActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_BOT_ID = "extra_bot_id"
        private const val EXTRA_BOT_NAME = "extra_bot_name"
        private const val EXTRA_BOT_TOKEN = "extra_bot_token"

        fun start(context: Context, botId: String, botName: String, botToken: String = "") {
            val intent = Intent(context, BotSettingsActivity::class.java).apply {
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
        val initialBotToken = intent.getStringExtra(EXTRA_BOT_TOKEN) ?: ""
        if (botId.isEmpty()) { finish(); return }

        setContent {
            YhchatCanaryTheme {
                BotSettingsScreen(
                    botId = botId,
                    botName = botName,
                    initialBotToken = initialBotToken,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BotSettingsScreen(
    botId: String,
    botName: String,
    initialBotToken: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient.apiService }
    val webApi = remember { ApiClient.webApiService }
    val tokenRepo = remember { RepositoryFactory.getTokenRepository(context) }

    var token by remember { mutableStateOf(initialBotToken) }
    var isLoading by remember { mutableStateOf(false) }
    var isResettingLink by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // 事件订阅开关
    var messageReceiveNormal by remember { mutableStateOf(false) }
    var messageReceiveInstruction by remember { mutableStateOf(false) }
    var botFollowed by remember { mutableStateOf(false) }
    var botUnfollowed by remember { mutableStateOf(false) }
    var groupJoin by remember { mutableStateOf(false) }
    var groupLeave by remember { mutableStateOf(false) }
    var botSetting by remember { mutableStateOf(false) }

    // 辅助函数：提交事件订阅设置
    suspend fun submitEventEdit(typ: String, enabled: Boolean) {
        val userToken = tokenRepo.getTokenSync() ?: return
        val value = if (enabled) 1 else 0
        val req = when (typ) {
            "messageReceiveNormal" -> BotEventEditRequest(botId, messageReceiveNormal = value, typ = typ)
            "messageReceiveInstruction" -> BotEventEditRequest(botId, messageReceiveInstruction = value, typ = typ)
            "botFollowed" -> BotEventEditRequest(botId, botFollowed = value, typ = typ)
            "botUnfollowed" -> BotEventEditRequest(botId, botUnfollowed = value, typ = typ)
            "groupJoin" -> BotEventEditRequest(botId, groupJoin = value, typ = typ)
            "groupLeave" -> BotEventEditRequest(botId, groupLeave = value, typ = typ)
            "botSetting" -> BotEventEditRequest(botId, botSetting = value, typ = typ)
            else -> BotEventEditRequest(botId, typ = typ)
        }
        runCatching { api.editBotEventSettings(userToken, req) }
    }

    LaunchedEffect(botId) {
        val userToken = tokenRepo.getTokenSync() ?: return@LaunchedEffect
        // 1 如果没有传递 token，从我的机器人列表中获取
        if (token.isBlank()) {
            val botRepo = RepositoryFactory.getBotRepository(context)
            botRepo.getMyBotList().fold(
                onSuccess = { bots ->
                    val found = bots.firstOrNull { it.botId == botId }
                    token = found?.token ?: ""
                },
                onFailure = { /* 忽略错误，保持空token */ }
            )
        }
        // 2 拉取事件订阅设置（初次进入）
        isLoading = true
        error = null
        runCatching {
            api.getBotEventSettings(userToken, BotIdRequest(botId))
        }.onSuccess { resp ->
            isLoading = false
            val ok = resp.body()?.code == 1
            if (!ok) {
                error = resp.body()?.msg ?: "加载失败"
            } else {
                // 解析并设置当前开关状态
                resp.body()?.data?.list?.let { settings ->
                    messageReceiveNormal = settings.messageReceiveNormal == 1
                    messageReceiveInstruction = settings.messageReceiveInstruction == 1
                    botFollowed = settings.botFollowed == 1
                    botUnfollowed = settings.botUnfollowed == 1
                    groupJoin = settings.groupJoin == 1
                    groupLeave = settings.groupLeave == 1
                    botSetting = settings.botSetting == 1
                }
            }
        }.onFailure { e ->
            isLoading = false
            error = e.message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$botName 设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            }

            // Token 区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "机器人 Token",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("请输入机器人 Token") },
                            singleLine = true,
                            enabled = !isLoading
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val userToken = tokenRepo.getTokenSync() ?: return@launch
                                    isLoading = true
                                    error = null
                                    runCatching { api.resetBotToken(userToken, BotIdRequest(botId)) }
                                        .onSuccess { resp ->
                                            isLoading = false
                                            if (resp.body()?.code == 1) {
                                                token = resp.body()?.data?.token ?: token
                                            } else {
                                                error = resp.body()?.msg ?: "重置失败"
                                            }
                                        }
                                        .onFailure { e ->
                                            isLoading = false
                                            error = e.message
                                        }
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重置")
                        }
                    }
                }
            }
            
            // 恢复订阅链接按钮
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "订阅链接管理",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "如果机器人订阅链接失效，可以使用此功能恢复",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Button(
                        onClick = {
                            scope.launch {
                                val userToken = tokenRepo.getTokenSync() ?: return@launch
                                isResettingLink = true
                                error = null
                                runCatching {
                                    webApi.resetBotLink(
                                        token = userToken,
                                        request = mapOf("botId" to botId)
                                    )
                                }.onSuccess { resp ->
                                    isResettingLink = false
                                    if (resp.body()?.code == 1) {
                                        Toast.makeText(context, "订阅链接已恢复", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val errorMsg = resp.body()?.message ?: "恢复失败"
                                        error = errorMsg
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }.onFailure { e ->
                                    isResettingLink = false
                                    error = e.message
                                    Toast.makeText(context, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isResettingLink && !isLoading
                    ) {
                        if (isResettingLink) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("恢复中...")
                        } else {
                            Text("恢复订阅链接")
                        }
                    }
                }
            }

            // 订阅设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "事件订阅", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    SubscriptionSwitch("普通消息事件", messageReceiveNormal) { checked ->
                        messageReceiveNormal = checked
                        scope.launch { submitEventEdit("messageReceiveNormal", checked) }
                    }
                    SubscriptionSwitch("指令消息事件", messageReceiveInstruction) { checked ->
                        messageReceiveInstruction = checked
                        scope.launch { submitEventEdit("messageReceiveInstruction", checked) }
                    }
                    SubscriptionSwitch("关注机器人事件", botFollowed) { checked ->
                        botFollowed = checked
                        scope.launch { submitEventEdit("botFollowed", checked) }
                    }
                    SubscriptionSwitch("取关机器人事件", botUnfollowed) { checked ->
                        botUnfollowed = checked
                        scope.launch { submitEventEdit("botUnfollowed", checked) }
                    }
                    SubscriptionSwitch("加入群事件", groupJoin) { checked ->
                        groupJoin = checked
                        scope.launch { submitEventEdit("groupJoin", checked) }
                    }
                    SubscriptionSwitch("退出群事件", groupLeave) { checked ->
                        groupLeave = checked
                        scope.launch { submitEventEdit("groupLeave", checked) }
                    }
                    SubscriptionSwitch("机器人设置消息事件", botSetting) { checked ->
                        botSetting = checked
                        scope.launch { submitEventEdit("botSetting", checked) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
