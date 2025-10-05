@file:OptIn(ExperimentalMaterial3Api::class)
package com.yhchat.canary.ui.community

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.yhchat.canary.data.model.CommunityBoard
import com.yhchat.canary.data.model.CommunityPost
import com.yhchat.canary.ui.components.ScrollBehavior
import com.yhchat.canary.ui.components.HandleScrollBehavior
import androidx.compose.foundation.lazy.rememberLazyListState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

/**
 * 社区标签页界面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CommunityTabScreen(
    token: String,
    modifier: Modifier = Modifier,
    scrollBehavior: ScrollBehavior? = null,
    viewModel: CommunityViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // 获取状态
    val boardListState by viewModel.boardListState.collectAsState()
    val followingBoardListState by viewModel.followingBoardListState.collectAsState()
    val myPostListState by viewModel.myPostListState.collectAsState()
    
    // 页面状态
    val pagerState = rememberPagerState { 3 }
    var selectedTab by remember { mutableStateOf(0) }
    
    // 标签页标题
    val tabTitles = listOf("分区列表", "关注分区", "我的文章")
    
    // 监听页面变化，使用snapshotFlow来获得更好的响应性
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedTab = page
        }
    }
    
    // 监听标签选择变化，使用协程作用域获得更好的控制
    val coroutineScope = rememberCoroutineScope()
    
    // 监听标签选择变化
    LaunchedEffect(selectedTab) {
        if (selectedTab != pagerState.currentPage) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = "社区",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = {
                    // 跳转到搜索Activity
                    val intent = Intent(context, SearchActivity::class.java).apply {
                        putExtra("token", token)
                    }
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                }
            }
        )
        
        // 标签选择栏
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { 
                        selectedTab = index
                        // 立即滚动到选中页面，不等待LaunchedEffect
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp),
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
        
        // 预加载数据
        LaunchedEffect(token) {
            if (token.isNotEmpty()) {
                viewModel.loadBoardList(token)
                viewModel.loadFollowingBoardList(token)
                viewModel.loadMyPostList(token)
            }
        }
        
        // 页面内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { 
            
            
            
            page ->
            when (page) {
                0 -> {
                    // 分区列表
                    val swipeRefreshState = rememberSwipeRefreshState(boardListState.isRefreshing)
                    SwipeRefresh(
                        state = swipeRefreshState,
                        onRefresh = { viewModel.refreshBoardList(token) }
                    ) {
                        BoardListContent(
                            boards = boardListState.boards,
                            isLoading = boardListState.isLoading,
                            error = boardListState.error,
                            scrollBehavior = scrollBehavior,
                            onBoardClick = { board ->
                                val intent = Intent(context, BoardDetailActivity::class.java).apply {
                                    putExtra("board_id", board.id)
                                    putExtra("board_name", board.name)
                                    putExtra("token", token)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
                1 -> {
                    // 关注分区
                    val swipeRefreshState = rememberSwipeRefreshState(followingBoardListState.isRefreshing)
                    SwipeRefresh(
                        state = swipeRefreshState,
                        onRefresh = { viewModel.refreshFollowingBoardList(token) }
                    ) {
                        BoardListContent(
                            boards = followingBoardListState.boards,
                            isLoading = followingBoardListState.isLoading,
                            error = followingBoardListState.error,
                            scrollBehavior = scrollBehavior,
                            onBoardClick = { board ->
                                val intent = Intent(context, BoardDetailActivity::class.java).apply {
                                    putExtra("board_id", board.id)
                                    putExtra("board_name", board.name)
                                    putExtra("token", token)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
                2 -> {
                    // 我的文章
                    val swipeRefreshState = rememberSwipeRefreshState(myPostListState.isRefreshing)
                    SwipeRefresh(
                        state = swipeRefreshState,
                        onRefresh = { viewModel.refreshMyPostList(token) }
                    ) {
                        MyPostListContent(
                            posts = myPostListState.posts,
                            isLoading = myPostListState.isLoading,
                            error = myPostListState.error,
                            hasMore = myPostListState.hasMore,
                            scrollBehavior = scrollBehavior,
                            onPostClick = { post ->
                                // 跳转到文章详情
                                val intent = Intent(context, PostDetailActivity::class.java).apply {
                                    putExtra("post_id", post.id)
                                    putExtra("post_title", post.title)
                                    putExtra("token", token)
                                }
                                context.startActivity(intent)
                            },
                            onLoadMore = {
                                viewModel.loadMoreMyPosts(token)
                            },
                            onDeletePost = { postId ->
                                viewModel.deletePost(token, postId)
                            },
                            context = context,
                            token = token
                        )
                    }
                }
            }
        }
    }
}

/**
 * 分区列表内容
 */
@Composable
fun BoardListContent(
    boards: List<CommunityBoard>,
    isLoading: Boolean,
    error: String?,
    onBoardClick: (CommunityBoard) -> Unit,
    scrollBehavior: ScrollBehavior? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // 连接滚动行为到底部导航栏的显示/隐藏
    scrollBehavior?.let { behavior ->
        listState.HandleScrollBehavior(scrollBehavior = behavior)
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 错误提示
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // 分区列表
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(boards) { board ->
                BoardItem(
                    board = board,
                    onClick = { onBoardClick(board) }
                )
            }
            
            // 空状态
            if (boards.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无分区",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 加载状态
            if (isLoading && boards.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

/**
 * 我的文章列表内容
 */
@Composable
fun MyPostListContent(
    posts: List<CommunityPost>,
    isLoading: Boolean,
    error: String?,
    hasMore: Boolean,
    onPostClick: (CommunityPost) -> Unit,
    onLoadMore: () -> Unit,
    onDeletePost: (Int) -> Unit,
    context: android.content.Context,
    token: String,
    scrollBehavior: ScrollBehavior? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // 连接滚动行为到底部导航栏的显示/隐藏
    scrollBehavior?.let { behavior ->
        listState.HandleScrollBehavior(scrollBehavior = behavior)
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 错误提示
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // 文章列表
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(posts) { post ->
                            MyPostItem(
                                post = post,
                                onClick = { onPostClick(post) },
                                onEdit = { 
                                    // 跳转到编辑文章Activity
                                    val intent = Intent(
                                        context, EditPostActivity::class.java).apply {
                                        putExtra("post_id", post.id)
                                        putExtra("token", token)
                                        putExtra("original_title", post.title)
                                        putExtra("original_content", post.content)
                                        putExtra("content_type", post.contentType)
                                    }
                                    context.startActivity(intent)
                                },
                                onDelete = {
                                    onDeletePost(post.id)
                                }
                            )
            }
            
            // 加载更多按钮
            if (posts.isNotEmpty() && hasMore) {
                item {
                    Button(
                        onClick = onLoadMore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isLoading) "加载中..." else "加载更多")
                    }
                }
            }
            
            // 空状态
            if (posts.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无文章",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 加载状态
            if (isLoading && posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

/**
 * 我的文章项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyPostItem(
    post: CommunityPost,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 文章标题
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 文章内容预览
            if (post.content.isNotEmpty()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 文章信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.createTimeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "点赞",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${post.likeNum}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = "评论",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${post.commentNum}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "收藏",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${post.collectNum}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // 上下文菜单
        if (showContextMenu) {
            PostContextMenu(
                onDismiss = { showContextMenu = false },
                onEdit = {
                    showContextMenu = false
                    onEdit()
                },
                onDelete = {
                    showContextMenu = false
                    showDeleteDialog = true
                }
            )
        }
        
        // 删除确认对话框
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除文章") },
                text = { Text("确定要删除这篇文章吗？删除后无法恢复。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete()
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
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
}

/**
 * 文章上下文菜单
 */
@Composable
fun PostContextMenu(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("操作选项")
        },
        text = {
            Column {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("编辑文章")
                    }
                }
                
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "删除文章",
                            color = MaterialTheme.colorScheme.error
                        )
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
