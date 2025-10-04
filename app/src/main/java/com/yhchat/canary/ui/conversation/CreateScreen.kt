package com.yhchat.canary.ui.conversation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 创建界面（机器人/群聊）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    onBack: () -> Unit,
    onBotCreated: (String) -> Unit,
    viewModel: CreateScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // 返回时重置表单
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetForm()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "创建",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Tab选择器
            CreateTabSelector(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.setSelectedTab(it) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 根据选中的Tab显示不同的内容
            when (uiState.selectedTab) {
                CreateTab.BOT -> {
                    BotCreationForm(
                        uiState = uiState,
                        onNameChange = { viewModel.updateBotName(it) },
                        onDescriptionChange = { viewModel.updateBotDescription(it) },
                        onAvatarUrlChange = { viewModel.updateBotAvatarUrl(it) },
                        onPrivacyToggle = { viewModel.toggleBotPrivacy() },
                        onCreateBot = { 
                            viewModel.createBot(
                                onSuccess = { botId ->
                                    Toast.makeText(context, "机器人创建成功", Toast.LENGTH_SHORT).show()
                                    onBotCreated(botId)
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        isLoading = uiState.isLoading
                    )
                }
                CreateTab.GROUP -> {
                    GroupCreationForm(
                        uiState = uiState,
                        onNameChange = { viewModel.updateGroupName(it) },
                        onDescriptionChange = { viewModel.updateGroupDescription(it) },
                        onAvatarUrlChange = { viewModel.updateGroupAvatarUrl(it) },
                        onCreateGroup = { 
                            viewModel.createGroup(
                                onSuccess = { groupId ->
                                    Toast.makeText(context, "群聊创建成功", Toast.LENGTH_SHORT).show()
                                    onBotCreated(groupId)
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        isLoading = uiState.isLoading
                    )
                }
            }
        }
    }
}

/**
 * Tab选择器
 */
@Composable
fun CreateTabSelector(
    selectedTab: CreateTab,
    onTabSelected: (CreateTab) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 机器人Tab
            Box(modifier = Modifier.weight(1f)) {
                TabItem(
                    selected = selectedTab == CreateTab.BOT,
                    icon = Icons.Default.Android,
                    text = "机器人",
                    onClick = { onTabSelected(CreateTab.BOT) }
                )
            }
            
            // 群聊Tab
            Box(modifier = Modifier.weight(1f)) {
                TabItem(
                    selected = selectedTab == CreateTab.GROUP,
                    icon = Icons.Default.Group,
                    text = "群聊",
                    onClick = { onTabSelected(CreateTab.GROUP) }
                )
            }
        }
    }
}

/**
 * Tab项
 */
@Composable
fun TabItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 选中指示器
        if (selected) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
            ) {}
        }
    }
}

/**
 * 机器人创建表单
 */
@Composable
fun BotCreationForm(
    uiState: CreateUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAvatarUrlChange: (String) -> Unit,
    onPrivacyToggle: () -> Unit,
    onCreateBot: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 机器人名称
        OutlinedTextField(
            value = uiState.botName,
            onValueChange = onNameChange,
            label = { Text("机器人名称*") },
            placeholder = { Text("请输入机器人名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            }
        )
        
        // 机器人描述
        OutlinedTextField(
            value = uiState.botDescription,
            onValueChange = onDescriptionChange,
            label = { Text("机器人描述") },
            placeholder = { Text("请输入机器人描述") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        
        // 头像URL
        OutlinedTextField(
            value = uiState.botAvatarUrl,
            onValueChange = onAvatarUrlChange,
            label = { Text("头像URL") },
            placeholder = { Text("请输入头像URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // 隐私设置
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "设为私有",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = uiState.isBotPrivate,
                onCheckedChange = { onPrivacyToggle() }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 创建按钮
        Button(
            onClick = onCreateBot,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "创建机器人",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * 群聊创建表单
 */
@Composable
fun GroupCreationForm(
    uiState: CreateUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAvatarUrlChange: (String) -> Unit,
    onCreateGroup: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 群聊名称
        OutlinedTextField(
            value = uiState.groupName,
            onValueChange = onNameChange,
            label = { Text("群聊名称*") },
            placeholder = { Text("请输入群聊名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            }
        )
        
        // 群聊描述
        OutlinedTextField(
            value = uiState.groupDescription,
            onValueChange = onDescriptionChange,
            label = { Text("群聊描述") },
            placeholder = { Text("请输入群聊描述") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        
        // 头像URL
        OutlinedTextField(
            value = uiState.groupAvatarUrl,
            onValueChange = onAvatarUrlChange,
            label = { Text("头像URL") },
            placeholder = { Text("请输入头像URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 创建按钮
        Button(
            onClick = onCreateGroup,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "创建群聊",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}