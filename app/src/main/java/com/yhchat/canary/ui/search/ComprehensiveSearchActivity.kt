package com.yhchat.canary.ui.search

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.*
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import kotlinx.coroutines.launch

class ComprehensiveSearchActivity : ComponentActivity() {
    
    private val viewModel: ComprehensiveSearchViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            YhchatCanaryTheme {
                ComprehensiveSearchScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComprehensiveSearchScreen(
    viewModel: ComprehensiveSearchViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }
    
    val tabs = listOf("群聊", "用户", "机器人")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // 顶部应用栏和搜索框
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("输入ID搜索...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchText.isNotBlank()) {
                                when (pagerState.currentPage) {
                                    0 -> viewModel.searchGroup(searchText.trim())
                                    1 -> viewModel.searchUser(searchText.trim())
                                    2 -> viewModel.searchBot(searchText.trim())
                                }
                            }
                        }
                    ),
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = {
                                searchText = ""
                                viewModel.clearResults()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清除"
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent
                    )
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
                IconButton(onClick = {
                    if (searchText.isNotBlank()) {
                        when (pagerState.currentPage) {
                            0 -> viewModel.searchGroup(searchText.trim())
                            1 -> viewModel.searchUser(searchText.trim())
                            2 -> viewModel.searchBot(searchText.trim())
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                }
            }
        )
        
        // 标签栏
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }
        
        // 错误信息
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // 内容区域
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> GroupSearchPage(
                    groupResult = uiState.groupResult,
                    isLoading = uiState.isLoading,
                    onGroupClick = { group ->
                        viewModel.showGroupDialog(group)
                    }
                )
                1 -> UserSearchPage(
                    userResult = uiState.userResult,
                    isLoading = uiState.isLoading,
                    onUserClick = { user ->
                        viewModel.showUserDialog(user)
                    }
                )
                2 -> BotSearchPage(
                    botResult = uiState.botResult,
                    isLoading = uiState.isLoading,
                    onBotClick = { bot ->
                        viewModel.showBotDialog(bot)
                    }
                )
            }
        }
    }
    }
    
    // 显示详情弹窗
    if (uiState.showGroupDialog) {
        GroupInfoDialog(
            group = uiState.groupResult,
            onDismiss = { viewModel.hideGroupDialog() },
            onAdd = { group ->
                viewModel.addGroup(group.groupId ?: "")
            },
            isAdding = uiState.isAdding
        )
    }
    
    if (uiState.showUserDialog) {
        UserInfoDialog(
            user = uiState.userResult,
            onDismiss = { viewModel.hideUserDialog() },
            onAdd = { user ->
                viewModel.addUser(user.userId ?: "")
            },
            isAdding = uiState.isAdding
        )
    }
    
    if (uiState.showBotDialog) {
        BotInfoDialog(
            bot = uiState.botResult,
            onDismiss = { viewModel.hideBotDialog() },
            onAdd = { bot ->
                viewModel.addBot(bot.botId ?: "")
            },
            isAdding = uiState.isAdding
        )
    }
}

@Composable
private fun GroupSearchPage(
    groupResult: GroupInfo?,
    isLoading: Boolean,
    onGroupClick: (GroupInfo) -> Unit
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
        groupResult != null -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    GroupResultCard(
                        group = groupResult,
                        onClick = { onGroupClick(groupResult) }
                    )
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "输入群聊ID进行搜索",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun UserSearchPage(
    userResult: UserInfo?,
    isLoading: Boolean,
    onUserClick: (UserInfo) -> Unit
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
        userResult != null -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    UserResultCard(
                        user = userResult,
                        onClick = { onUserClick(userResult) }
                    )
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "输入用户ID进行搜索",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BotSearchPage(
    botResult: BotInfo?,
    isLoading: Boolean,
    onBotClick: (BotInfo) -> Unit
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
        botResult != null -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    BotResultCard(
                        bot = botResult,
                        onClick = { onBotClick(botResult) }
                    )
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "输入机器人ID进行搜索",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
