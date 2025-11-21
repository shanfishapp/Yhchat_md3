package com.yhchat.canary.ui.bot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.yhchat.canary.data.api.ApiClient
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.BotInstruction
import com.yhchat.canary.data.model.BotInstructionRequest
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class InstructionManagementActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_BOT_ID = "bot_id"
        private const val EXTRA_BOT_NAME = "bot_name"
        
        fun start(context: Context, botId: String, botName: String) {
            val intent = Intent(context, InstructionManagementActivity::class.java).apply {
                putExtra(EXTRA_BOT_ID, botId)
                putExtra(EXTRA_BOT_NAME, botName)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val botId = intent.getStringExtra(EXTRA_BOT_ID) ?: run {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val botName = intent.getStringExtra(EXTRA_BOT_NAME) ?: "机器人"
        
        setContent {
            YhchatCanaryTheme {
                InstructionManagementScreen(
                    botId = botId,
                    botName = botName,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionManagementScreen(
    botId: String,
    botName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var instructions by remember { mutableStateOf<List<BotInstruction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // 加载指令列表
    fun loadInstructions() {
        scope.launch {
            isLoading = true
            error = null
            
            val api = ApiClient.apiService
            val token = RepositoryFactory.getTokenRepository(context).getTokenSync()
            
            if (token != null) {
                runCatching {
                    api.getBotInstructionList(token, BotInstructionRequest(botId))
                }.onSuccess { response ->
                    if (response.body()?.code == 1) {
                        instructions = response.body()?.data?.list ?: emptyList()
                    } else {
                        error = response.body()?.msg ?: "加载失败"
                    }
                    isLoading = false
                }.onFailure { e ->
                    error = e.message ?: "未知错误"
                    isLoading = false
                }
            } else {
                error = "未登录"
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        loadInstructions()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$botName - 指令管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        InstructionEditActivity.start(context, botId, botName, null)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加指令"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { loadInstructions() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                
                instructions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "暂无指令",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { 
                                InstructionEditActivity.start(context, botId, botName, null)
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("创建第一个指令")
                            }
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(instructions) { instruction ->
                            InstructionListItem(
                                instruction = instruction,
                                onEdit = { 
                                    InstructionEditActivity.start(context, botId, botName, instruction.id)
                                },
                                onPreview = {
                                    InstructionPreviewActivity.start(context, instruction)
                                },
                                onDelete = {
                                    scope.launch {
                                        val api = ApiClient.apiService
                                        val token = RepositoryFactory.getTokenRepository(context).getTokenSync()
                                        
                                        if (token != null) {
                                            runCatching {
                                                api.editBotInstruction(
                                                    token,
                                                    com.yhchat.canary.data.model.EditInstructionRequest(
                                                        id = instruction.id,
                                                        botId = botId,
                                                        name = instruction.name,
                                                        desc = instruction.desc,
                                                        type = instruction.instructionType,
                                                        hintText = instruction.hintText,
                                                        defaultText = instruction.defaultText,
                                                        customJson = instruction.customJson,
                                                        delFlag = 1
                                                    )
                                                )
                                            }.onSuccess { response ->
                                                if (response.body()?.code == 1) {
                                                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                                                    loadInstructions()
                                                } else {
                                                    Toast.makeText(context, response.body()?.message ?: "删除失败", Toast.LENGTH_SHORT).show()
                                                }
                                            }.onFailure { e ->
                                                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstructionListItem(
    instruction: BotInstruction,
    onEdit: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = instruction.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = when (instruction.instructionType) {
                                1 -> "普通指令"
                                2 -> "直发指令"
                                5 -> "自定义输入"
                                else -> "未知类型"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                if (!instruction.desc.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = instruction.desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 显示提示文字和默认文字
                if (instruction.instructionType == 1 || instruction.instructionType == 5) {
                    if (!instruction.hintText.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "提示: ${instruction.hintText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    if (!instruction.defaultText.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "默认: ${instruction.defaultText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                if (instruction.instructionType == 5 && !instruction.customJson.isNullOrBlank()) {
                    val customCount = remember(instruction.customJson) {
                        parseCustomFields(instruction.customJson ?: "").size
                    }
                    if (customCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "自定义字段：$customCount 个",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 预览按钮
            IconButton(onClick = onPreview) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "预览",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            
            // 编辑按钮
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // 删除按钮
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


