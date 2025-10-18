package com.yhchat.canary.ui.discover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
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
import coil.compose.AsyncImage
import com.yhchat.canary.data.api.ApiClient
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.AddFriendRequest
import com.yhchat.canary.data.model.RecommendGroup
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DiscoverGroupsActivity : ComponentActivity() {
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DiscoverGroupsActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YhchatCanaryTheme {
                DiscoverGroupsScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverGroupsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val discoverRepo = remember { RepositoryFactory.getDiscoverRepository(context) }

    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingCategories by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // 搜索状态
    var isSearching by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<RecommendGroup>>(emptyList()) }
    var isSearchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // 加载分类列表
    LaunchedEffect(Unit) {
        isLoadingCategories = true
        error = null
        discoverRepo.getGroupCategories().fold(
            onSuccess = { cats ->
                // 添加"最新"到第一个
                categories = listOf("最新") + cats
                isLoadingCategories = false
            },
            onFailure = { e ->
                error = e.message
                isLoadingCategories = false
                categories = listOf("最新") // 至少显示最新
            }
        )
    }

    val pagerState = rememberPagerState(pageCount = { categories.size })

    // 搜索群聊函数
    fun searchGroups(keyword: String) {
        if (keyword.isBlank()) {
            searchResults = emptyList()
            return
        }
        
        scope.launch {
            isSearchLoading = true
            searchError = null
            
            val apiService = com.yhchat.canary.data.api.ApiClient.apiService
            val token = RepositoryFactory.getTokenRepository(context).getTokenSync()
            
            if (token != null) {
                runCatching {
                    apiService.searchRecommendGroups(
                        token = token,
                        request = com.yhchat.canary.data.model.SearchRecommendGroupRequest(
                            keyword = keyword,
                            categoryId = null
                        )
                    )
                }.onSuccess { response ->
                    if (response.isSuccessful && response.body()?.code == 1) {
                        searchResults = response.body()?.data?.groups?.map { group ->
                            RecommendGroup(
                                chatId = group.groupId,
                                banId = group.banId,
                                nickname = group.name,
                                introduction = group.introduction,
                                avatarUrl = group.avatarUrl,
                                headcount = group.headcount,
                                createTime = group.createTime
                            )
                        } ?: emptyList()
                    } else {
                        searchError = response.body()?.msg ?: "搜索失败"
                    }
                    isSearchLoading = false
                }.onFailure { e ->
                    searchError = e.message ?: "网络错误"
                    isSearchLoading = false
                }
            } else {
                searchError = "未登录"
                isSearchLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchKeyword,
                            onValueChange = { searchKeyword = it },
                            placeholder = { Text("搜索群聊...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("发现群聊", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) {
                            isSearching = false
                            searchKeyword = ""
                            searchResults = emptyList()
                            searchError = null
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = if (isSearching) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isSearching) "关闭搜索" else "返回"
                        )
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    } else {
                        IconButton(onClick = { searchGroups(searchKeyword) }) {
                            Icon(Icons.Default.Search, contentDescription = "执行搜索")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 如果正在搜索，显示搜索结果
            if (isSearching) {
                when {
                    isSearchLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    searchError != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = searchError ?: "搜索失败",
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { searchGroups(searchKeyword) }) {
                                    Text("重试")
                                }
                            }
                        }
                    }
                    searchResults.isEmpty() && searchKeyword.isNotBlank() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("未找到相关群聊")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "搜索: \"$searchKeyword\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    searchResults.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResults) { group ->
                                var selectedGroup by remember { mutableStateOf<RecommendGroup?>(null) }
                                GroupCard(
                                    group = group,
                                    onClick = { selectedGroup = group }
                                )
                                
                                // 群聊详情弹窗
                                selectedGroup?.let { sg ->
                                    GroupDetailDialog(
                                        group = sg,
                                        onDismiss = { selectedGroup = null },
                                        onJoin = {
                                            scope.launch {
                                                val api = com.yhchat.canary.data.api.ApiClient.apiService
                                                val token = RepositoryFactory.getTokenRepository(context).getTokenSync()
                                                if (token != null) {
                                                    runCatching {
                                                        api.addFriend(
                                                            token,
                                                            AddFriendRequest(
                                                                chatId = sg.chatId,
                                                                chatType = 2,
                                                                remark = ""
                                                            )
                                                        )
                                                    }.onSuccess { response ->
                                                        if (response.body()?.code == 1) {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "已发送加入申请",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                            selectedGroup = null
                                                        } else {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                response.body()?.message ?: "加入失败",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }.onFailure {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "网络错误",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "输入关键词搜索群聊",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (isLoadingCategories) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 分类选项卡
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 16.dp
                ) {
                    categories.forEachIndexed { index, category ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(category) }
                        )
                    }
                }

                // 群聊列表页面
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val category = if (page == 0) "" else categories[page]
                    GroupListPage(
                        category = category,
                        discoverRepo = discoverRepo
                    )
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun GroupListPage(
    category: String,
    discoverRepo: com.yhchat.canary.data.repository.DiscoverRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf<List<RecommendGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedGroup by remember { mutableStateOf<RecommendGroup?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    var hasMoreData by remember { mutableStateOf(true) }

    // 加载更多数据的函数
    fun loadMoreGroups() {
        if (isLoadingMore || !hasMoreData) return
        
        scope.launch {
            isLoadingMore = true
            discoverRepo.getRecommendGroups(
                category = category, 
                size = 20, 
                page = currentPage + 1
            ).fold(
                onSuccess = { newGroups ->
                    if (newGroups.isEmpty()) {
                        hasMoreData = false
                    } else {
                        groups = groups + newGroups
                        currentPage += 1
                    }
                    isLoadingMore = false
                },
                onFailure = { 
                    isLoadingMore = false
                }
            )
        }
    }

    // 初始加载
    LaunchedEffect(category) {
        isLoading = true
        error = null
        currentPage = 1
        hasMoreData = true
        groups = emptyList()
        
        discoverRepo.getRecommendGroups(category = category, size = 20, page = 1).fold(
            onSuccess = { groupList ->
                groups = groupList
                isLoading = false
                hasMoreData = groupList.size >= 20 // 如果返回的数据少于20条，说明没有更多数据了
            },
            onFailure = { e ->
                error = e.message
                isLoading = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    Text(
                        text = error ?: "加载失败",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            groups.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无群聊")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups) { group ->
                        GroupCard(
                            group = group,
                            onClick = { selectedGroup = group }
                        )
                    }
                    
                    // 加载更多指示器
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    } else if (hasMoreData) {
                        // 触发加载更多的项目
                        item {
                            LaunchedEffect(Unit) {
                                loadMoreGroups()
                            }
                            // 占位符，用户看不到
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    } else {
                        // 没有更多数据的提示
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "没有更多群聊了",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 群聊详情弹窗
    selectedGroup?.let { group ->
        GroupDetailDialog(
            group = group,
            onDismiss = { selectedGroup = null },
            onJoin = {
                scope.launch {
                    val api = ApiClient.apiService
                    val token = RepositoryFactory.getTokenRepository(context).getTokenSync()
                    if (token != null) {
                        runCatching {
                            api.addFriend(
                                token,
                                AddFriendRequest(
                                    chatId = group.chatId,
                                    chatType = 2,
                                    remark = ""
                                )
                            )
                        }.onSuccess { response ->
                            if (response.body()?.code == 1) {
                                android.widget.Toast.makeText(
                                    context,
                                    "已发送加入申请",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                selectedGroup = null
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    response.body()?.message ?: "加入失败",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }.onFailure {
                            android.widget.Toast.makeText(
                                context,
                                "网络错误",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun GroupCard(
    group: RecommendGroup,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 群聊头像
            if (!group.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageUtils.createAvatarImageRequest(LocalContext.current, group.avatarUrl),
                    contentDescription = "群聊头像",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "默认头像",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 群聊信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!group.introduction.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = group.introduction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${group.headcount}人",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun GroupDetailDialog(
    group: RecommendGroup,
    onDismiss: () -> Unit,
    onJoin: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val createDate = remember(group.createTime) {
        dateFormat.format(Date(group.createTime * 1000))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("群聊详情") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 群聊头像
                if (!group.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageUtils.createAvatarImageRequest(LocalContext.current, group.avatarUrl),
                        contentDescription = "群聊头像",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "默认头像",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = group.nickname,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ID: ${group.chatId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!group.introduction.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = group.introduction,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${group.headcount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "成员",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = createDate,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "创建时间",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onJoin) {
                Text("加入群聊")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

