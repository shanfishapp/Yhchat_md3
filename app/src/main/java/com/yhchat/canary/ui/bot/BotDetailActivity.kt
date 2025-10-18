package com.yhchat.canary.ui.bot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import yh_bot.Bot
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.ui.components.MarkdownText
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 机器人详细信息 Activity
 */
class BotDetailActivity : ComponentActivity() {
    
    private lateinit var viewModel: BotDetailViewModel
    
    companion object {
        const val EXTRA_BOT_ID = "extra_bot_id"
        const val EXTRA_BOT_NAME = "extra_bot_name"
        const val EXTRA_CHAT_TYPE = "extra_chat_type"
        
        fun start(context: Context, botId: String, botName: String = "机器人", chatType: Int = 3) {
            val intent = Intent(context, BotDetailActivity::class.java).apply {
                putExtra(EXTRA_BOT_ID, botId)
                putExtra(EXTRA_BOT_NAME, botName)
                putExtra(EXTRA_CHAT_TYPE, chatType)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val botId = intent.getStringExtra(EXTRA_BOT_ID)
        val botName = intent.getStringExtra(EXTRA_BOT_NAME) ?: "机器人"
        val chatType = intent.getIntExtra(EXTRA_CHAT_TYPE, 3)
        
        if (botId.isNullOrEmpty()) {
            finish()
            return
        }
        
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(BotDetailViewModel::class.java)
        
        setContent {
            YhchatCanaryTheme {
                BotDetailScreen(
                    botId = botId,
                    botName = botName,
                    chatType = chatType,
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
        
        // 加载数据
        viewModel.loadBotDetail(botId)
        viewModel.loadBoardInfo(botId, chatType)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BotDetailScreen(
    botId: String,
    botName: String,
    chatType: Int,
    viewModel: BotDetailViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showImageViewer by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf("") }
    
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "机器人详细信息",
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
                },
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        com.yhchat.canary.ui.background.ChatBackgroundActivity.start(
                            context, 
                            botId, 
                            botName
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = "聊天背景"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadBotDetail(botId) }
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
            uiState.botInfo != null -> {
                BotDetailContent(
                    botInfo = uiState.botInfo!!,
                    boardInfo = uiState.boardInfo,
                    isBoardLoading = uiState.isBoardLoading,
                    modifier = Modifier.padding(paddingValues),
                    onAvatarClick = { url ->
                        currentImageUrl = url
                        showImageViewer = true
                    }
                )
            }
        }
    }
    
    // 图片预览器
    if (showImageViewer) {
        com.yhchat.canary.ui.components.ImageViewer(
            imageUrl = currentImageUrl,
            onDismiss = { showImageViewer = false }
        )
    }
    }
}

@Composable
private fun BotDetailContent(
    botInfo: Bot.bot_info,
    boardInfo: Bot.board?,
    isBoardLoading: Boolean,
    modifier: Modifier = Modifier,
    onAvatarClick: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 头像和基本信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 头像
                AsyncImage(
                    model = if (botInfo.data.avatarUrl.isNotBlank()) {
                        ImageUtils.createBotImageRequest(
                            context = LocalContext.current,
                            url = botInfo.data.avatarUrl
                        )
                    } else {
                        null
                    },
                    contentDescription = "机器人头像",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (botInfo.data.avatarUrl.isNotBlank()) {
                                onAvatarClick(botInfo.data.avatarUrl)
                            }
                        },
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 机器人名称
                Text(
                    text = botInfo.data.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 机器人ID
                Text(
                    text = "ID: ${botInfo.data.botId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 机器人简介
        if (botInfo.data.introduction.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "机器人简介",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = botInfo.data.introduction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // 统计信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "统计信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatisticItem(
                        icon = Icons.Default.People,
                        label = "使用人数",
                        value = botInfo.data.headcount.toString()
                    )
                    StatisticItem(
                        icon = Icons.Default.CalendarToday,
                        label = "创建时间",
                        value = formatDate(botInfo.data.createTime)
                    )
                }
            }
        }
        
        // 详细信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "详细信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                DetailItem(
                    icon = Icons.Default.Person,
                    label = "创建者ID",
                    value = botInfo.data.createBy
                )
                
                DetailItem(
                    icon = if (botInfo.data.private == 1) Icons.Default.Lock else Icons.Default.Public,
                    label = "可见性",
                    value = if (botInfo.data.private == 1) "私有" else "公开"
                )
                
                DetailItem(
                    icon = if (botInfo.data.isStop == 0) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    label = "运行状态",
                    value = if (botInfo.data.isStop == 0) "正常运行" else "已停用"
                )
                
                DetailItem(
                    icon = if (botInfo.data.alwaysAgree == 1) Icons.Default.Check else Icons.Default.Close,
                    label = "自动加群",
                    value = if (botInfo.data.alwaysAgree == 1) "是" else "否"
                )
                
                DetailItem(
                    icon = if (botInfo.data.doNotDisturb == 1) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                    label = "免打扰",
                    value = if (botInfo.data.doNotDisturb == 1) "是" else "否"
                )
                
                DetailItem(
                    icon = if (botInfo.data.top == 1) Icons.Default.PushPin else Icons.Default.Remove,
                    label = "置顶",
                    value = if (botInfo.data.top == 1) "是" else "否"
                )
                
                DetailItem(
                    icon = if (botInfo.data.groupLimit == 1) Icons.Default.Block else Icons.Default.GroupAdd,
                    label = "限制进群",
                    value = if (botInfo.data.groupLimit == 1) "是" else "否"
                )
            }
        }
        
        // 看板信息
        if (boardInfo != null && boardInfo.board.content.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "看板信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    when (boardInfo.board.contentType) {
                        1 -> { // 文本
                            Text(
                                text = boardInfo.board.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        3 -> { // Markdown
                            MarkdownText(
                                markdown = boardInfo.board.content,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        8 -> { // HTML
                            com.yhchat.canary.ui.components.HtmlWebView(
                                htmlContent = boardInfo.board.content,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp, max = 400.dp)
                            )
                        }
                        else -> {
                            Text(
                                text = boardInfo.board.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else if (isBoardLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDate(timestamp: Long): String {
    return try {
        val date = Date(timestamp * 1000L)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        "-"
    }
}
