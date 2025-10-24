package com.yhchat.canary.ui.group

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.local.AppDatabase
import com.yhchat.canary.data.repository.BotRepository
import com.yhchat.canary.data.repository.GroupRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.UserRepository
import com.yhchat.canary.proto.group.Bot_data
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.ui.bot.BotInfoActivity
import com.yhchat.canary.ui.contacts.Contact
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 群聊机器人管理Activity
 */
class GroupBotManagementActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_GROUP_ID = "group_id"
        private const val EXTRA_GROUP_NAME = "group_name"
        
        fun start(context: Context, groupId: String, groupName: String) {
            val intent = Intent(context, GroupBotManagementActivity::class.java).apply {
                putExtra(EXTRA_GROUP_ID, groupId)
                putExtra(EXTRA_GROUP_NAME, groupName)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID) ?: run {
            finish()
            return
        }
        
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: "群聊机器人"
        
        setContent {
            YhchatCanaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GroupBotManagementScreen(
                        groupId = groupId,
                        groupName = groupName,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupBotManagementScreen(
    groupId: String,
    groupName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { GroupBotManagementViewModel() }
    
    LaunchedEffect(Unit) {
        viewModel.init(context)
        viewModel.loadGroupBots(groupId)
        viewModel.loadMyBots(context)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    var showInviteBotDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("机器人管理") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showInviteBotDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "邀请机器人")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadGroupBots(groupId) }) {
                            Text("重试")
                        }
                    }
                }
                uiState.bots.isEmpty() -> {
                    Text(
                        text = "暂无机器人",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.bots) { bot ->
                            BotCard(
                                bot = bot,
                                onRemoveClick = {
                                    viewModel.removeBot(bot.botId, groupId)
                                },
                                onBotClick = {
                                    // 跳转到机器人详情页
                                  BotInfoActivity.start(context, bot.botId, bot.name)
                                },
                                canRemove = true // TODO: 根据用户权限设置，普通成员设为 false
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 显示操作结果
    LaunchedEffect(uiState.operationSuccess) {
        if (uiState.operationSuccess) {
            Toast.makeText(context, "操作成功", Toast.LENGTH_SHORT).show()
            viewModel.resetOperationState()
            viewModel.loadGroupBots(groupId)
        }
    }
    
    LaunchedEffect(uiState.operationError) {
        uiState.operationError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.resetOperationState()
        }
    }
    
    // 邀请机器人对话框
    if (showInviteBotDialog) {
        InviteBotDialog(
            myBots = uiState.myBots,
            onDismiss = { showInviteBotDialog = false },
            onInvite = { botId ->
                viewModel.inviteBot(botId, groupId)
                showInviteBotDialog = false
            }
        )
    }
}

@Composable
fun BotCard(
    bot: Bot_data,
    onRemoveClick: () -> Unit,
    onBotClick: () -> Unit,
    canRemove: Boolean = true  // 是否可以删除机器人
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBotClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 机器人头像
            AsyncImage(
                model = ImageUtils.createBotImageRequest(context, bot.avatarUrl),
                contentDescription = bot.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 机器人信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = bot.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (bot.introduction.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bot.introduction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "使用人数: ${bot.headcount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 删除按钮（只有有权限的用户才显示）
            if (canRemove) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "移除机器人",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("移除机器人") },
            text = { Text("确定要从群聊中移除「${bot.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onRemoveClick()
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteBotDialog(
    myBots: List<Contact>,
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredBots = remember(searchQuery, myBots) {
        if (searchQuery.isEmpty()) {
            myBots
        } else {
            myBots.filter { bot ->
                bot.name.contains(searchQuery, ignoreCase = true) ||
                bot.chatId.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("邀请机器人") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索机器人") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 机器人列表
                if (filteredBots.isEmpty()) {
                    Text(
                        text = if (searchQuery.isEmpty()) "暂无机器人" else "未找到匹配的机器人",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredBots) { bot ->
                            InviteBotItem(
                                bot = bot,
                                onInvite = { onInvite(bot.chatId) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun InviteBotItem(
    bot: Contact,
    onInvite: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onInvite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 机器人头像
            AsyncImage(
                model = ImageUtils.createBotImageRequest(context, bot.avatarUrl),
                contentDescription = bot.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 机器人信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = bot.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "ID: ${bot.chatId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 邀请按钮
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "邀请",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

class GroupBotManagementViewModel : ViewModel() {
    private lateinit var groupRepository: GroupRepository
    private lateinit var botRepository: BotRepository
    
    private val _uiState = MutableStateFlow(GroupBotManagementUiState())
    val uiState: StateFlow<GroupBotManagementUiState> = _uiState.asStateFlow()
    
    fun init(context: Context) {
        groupRepository = RepositoryFactory.getGroupRepository(context)
        botRepository = RepositoryFactory.getBotRepository(context)
    }
    
    fun loadGroupBots(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            groupRepository.getGroupBots(groupId).fold(
                onSuccess = { bots ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        bots = bots
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun removeBot(botId: String, groupId: String) {
        viewModelScope.launch {
            botRepository.removeGroupBot(botId, groupId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(operationSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(operationError = error.message)
                }
            )
        }
    }
    
    fun resetOperationState() {
        _uiState.value = _uiState.value.copy(
            operationSuccess = false,
            operationError = null
        )
    }
    
    fun loadMyBots(context: Context) {
        viewModelScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val tokenRepository = TokenRepository(db.userTokenDao(), context)
                val friendRepository = com.yhchat.canary.data.repository.FriendRepository(
                    com.yhchat.canary.data.api.ApiClient.apiService,
                    tokenRepository
                )
                
                friendRepository.getAddressBookList().fold(
                    onSuccess = { addressBookList ->
                        // 获取机器人列表
                        val botsList = mutableListOf<Contact>()
                        addressBookList.dataList.forEach { data ->
                            if (data.listName == "机器人") {
                                data.dataList.forEach { botData ->
                                    botsList.add(
                                        Contact(
                                            chatId = botData.chatId,
                                            name = botData.name,
                                            avatarUrl = botData.avatarUrl,
                                            permissionLevel = botData.permissonLevel
                                        )
                                    )
                                }
                            }
                        }
                        _uiState.value = _uiState.value.copy(myBots = botsList)
                    },
                    onFailure = { error ->
                        android.util.Log.e("GroupBotManagement", "加载我的机器人失败", error)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("GroupBotManagement", "加载我的机器人异常", e)
            }
        }
    }
    
    fun inviteBot(botId: String, groupId: String) {
        viewModelScope.launch {
            groupRepository.inviteToGroup(botId, 3, groupId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(operationSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(operationError = error.message ?: "邀请失败")
                }
            )
        }
    }
}

data class GroupBotManagementUiState(
    val isLoading: Boolean = false,
    val bots: List<Bot_data> = emptyList(),
    val myBots: List<Contact> = emptyList(),
    val error: String? = null,
    val operationSuccess: Boolean = false,
    val operationError: String? = null
)

