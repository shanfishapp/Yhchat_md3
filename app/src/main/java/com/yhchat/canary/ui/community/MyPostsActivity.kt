@file:OptIn(ExperimentalMaterial3Api::class)
package com.yhchat.canary.ui.community

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.ui.base.BaseActivity
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

/**
 * 我的文章Activity
 */
class MyPostsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val token = intent.getStringExtra("token") ?: ""
        
        setContent {
            YhchatCanaryTheme {
                val viewModel: CommunityViewModel = viewModel {
                    CommunityViewModel(
                        communityRepository = RepositoryFactory.getCommunityRepository(this@MyPostsActivity)
                    )
                }
                
                MyPostsScreen(
                    token = token,
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    token: String,
    viewModel: CommunityViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val myPostListState by viewModel.myPostListState.collectAsState()
    
    // 搜索状态
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 本地过滤文章列表
    val filteredPosts = remember(myPostListState.posts, searchQuery) {
        if (searchQuery.isBlank()) {
            myPostListState.posts
        } else {
            myPostListState.posts.filter { post ->
                post.title.contains(searchQuery, ignoreCase = true) ||
                post.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    // 加载数据
    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            viewModel.loadMyPostList(token)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                if (isSearching) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索我的文章...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Text(
                        text = "我的文章",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            actions = {
                // 搜索按钮
                if (isSearching) {
                    IconButton(onClick = {
                        isSearching = false
                        searchQuery = ""
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "取消搜索"
                        )
                    }
                } else {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索我的文章"
                        )
                    }
                }
                
                // 屏蔽用户按钮
                IconButton(onClick = {
                    val intent = android.content.Intent(context, BlockedUsersActivity::class.java).apply {
                        putExtra("token", token)
                    }
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "屏蔽用户"
                    )
                }
            }
        )
        
        // 内容区域
        val swipeRefreshState = rememberSwipeRefreshState(myPostListState.isRefreshing)
        
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshMyPostList(token) }
        ) {
            MyPostListContent(
                posts = filteredPosts,
                isLoading = myPostListState.isLoading,
                error = myPostListState.error,
                searchQuery = searchQuery,
                onPostClick = { post ->
                    val intent = android.content.Intent(context, PostDetailActivity::class.java).apply {
                        putExtra("post_id", post.id)
                        putExtra("token", token)
                    }
                    context.startActivity(intent)
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
