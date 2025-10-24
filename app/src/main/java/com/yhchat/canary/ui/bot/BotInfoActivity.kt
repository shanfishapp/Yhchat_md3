package com.yhchat.canary.ui.bot

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Wallpaper
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.yhchat.canary.data.model.BotInfo
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BotInfoActivity : ComponentActivity() {
    
    private lateinit var viewModel: BotInfoViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val botId = intent.getStringExtra(EXTRA_BOT_ID)
        val botName = intent.getStringExtra(EXTRA_BOT_NAME) ?: "机器人"
        
        if (botId.isNullOrEmpty()) {
            Toast.makeText(this, "机器人ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(BotInfoViewModel::class.java)
        
        setContent {
            YhchatCanaryTheme {
                BotInfoScreen(
                    botId = botId,
                    botName = botName,
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
        
        viewModel.loadBotInfo(botId)
    }
    
    companion object {
        const val EXTRA_BOT_ID = "extra_bot_id"
        const val EXTRA_BOT_NAME = "extra_bot_name"
        
        fun start(context: android.content.Context, botId: String, botName: String) {
            val intent = android.content.Intent(context, BotInfoActivity::class.java).apply {
                putExtra(EXTRA_BOT_ID, botId)
                putExtra(EXTRA_BOT_NAME, botName)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BotInfoScreen(
    botId: String,
    botName: String,
    viewModel: BotInfoViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showImageViewer by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = botName,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    if (uiState.botInfo != null) {
                        IconButton(
                            onClick = { showReportDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Report,
                                contentDescription = "举报机器人"
                            )
                        }
                        IconButton(
                            onClick = {
                                com.yhchat.canary.ui.background.ChatBackgroundActivity.start(context, botId, botName)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = "聊天背景"
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.addBot(botId)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加机器人"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val botInfo = uiState.botInfo

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
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadBotInfo(botId) }
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
            botInfo != null -> {
                BotInfoContent(
                    botInfo = botInfo,
                    modifier = Modifier.padding(paddingValues),
                    isAdding = uiState.isAdding,
                    onAddBot = { viewModel.addBot(botId) },
                    onAvatarClick = { url ->
                        currentImageUrl = url
                        showImageViewer = true
                    }
                )
            }
        }
    }
    
    // 处理添加结果
    LaunchedEffect(uiState.addResult) {
        uiState.addResult?.let { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "添加机器人成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "添加机器人失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
            viewModel.clearAddResult()
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
    
    // 举报对话框
    if (showReportDialog) {
        com.yhchat.canary.ui.components.ReportDialog(
            chatId = botId,
            chatType = 3,  // 机器人
            chatName = botName,
            onDismiss = { showReportDialog = false },
            onSuccess = {
                Toast.makeText(context, "举报已提交", Toast.LENGTH_SHORT).show()
            }
        )
    }
    }
}

@Composable
private fun BotInfoContent(
    botInfo: BotInfo,
    modifier: Modifier = Modifier,
    isAdding: Boolean = false,
    onAddBot: () -> Unit = {},
    onAvatarClick: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头像和基本信息卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 头像
                    val avatarUrl = botInfo.avatarUrl
                    AsyncImage(
                        model = if (!avatarUrl.isNullOrBlank()) {
                            ImageUtils.createBotImageRequest(
                                context = LocalContext.current,
                                url = avatarUrl
                            )
                        } else {
                            null
                        },
                        contentDescription = "机器人头像",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (!avatarUrl.isNullOrBlank()) {
                                    onAvatarClick(avatarUrl)
                                }
                            },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 机器人名称
                    Text(
                        text = botInfo.nickname ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // 机器人ID
                    Text(
                        text = "机器人ID: ${botInfo.botId ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 机器人标识
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🤖",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "机器人",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 机器人简介卡片
            val introduction = botInfo.introduction
            if (!introduction.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "机器人简介",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = introduction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        
            // 统计信息卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "统计信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatisticItem(
                            title = "使用人数",
                            value = botInfo.headcount?.toString() ?: "0",
                            modifier = Modifier.weight(1f)
                        )
                        StatisticItem(
                            title = "创建时间",
                            value = botInfo.createTime?.let { formatTimestamp(it) } ?: "-",
                            modifier = Modifier.weight(1f)
                        )
                        StatisticItem(
                            title = "可见性",
                            value = when (botInfo.isPrivate) {
                                1 -> "私有"
                                0 -> "公开"
                                else -> "未知"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 详细信息卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "详细信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    InfoRow(
                        label = "创建者ID",
                        value = botInfo.createBy ?: "-"
                    )
                    
                    InfoRow(
                        label = "状态",
                        value = "正常运行" // BotInfo数据类中没有isStop字段
                    )
                    
                    InfoRow(
                        label = "自动加群",
                        value = "未知" // BotInfo数据类中没有alwaysAgree字段
                    )
                }
            }
        
            // 添加机器人按钮
            Button(
                onClick = onAddBot,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isAdding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "添加中...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "添加机器人",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun RowScope.StatisticItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp * 1000L) // 转换为毫秒
    val formatter = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
    return formatter.format(date)
}
