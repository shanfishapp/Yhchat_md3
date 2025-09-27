package com.yhchat.canary.ui.community

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.data.model.CommunityBoard
import com.yhchat.canary.data.model.CommunityPost
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

/**
 * 分区详情Activity - 显示某个分区的文章列表
 */
class BoardDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val boardId = intent.getIntExtra("board_id", 0)
        val boardName = intent.getStringExtra("board_name") ?: "分区详情"
        val token = intent.getStringExtra("token") ?: ""
        
        setContent {
            YhchatCanaryTheme {
                val viewModel: BoardDetailViewModel = viewModel {
                    BoardDetailViewModel(
                        communityRepository = RepositoryFactory.getCommunityRepository(this@BoardDetailActivity),
                        tokenRepository = RepositoryFactory.getTokenRepository(this@BoardDetailActivity)
                    )
                }
                
                BoardDetailScreen(
                    boardId = boardId,
                    boardName = boardName,
                    token = token,
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

/**
 * 分区详情界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDetailScreen(
    boardId: Int,
    boardName: String,
    token: String,
    viewModel: BoardDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 获取状态
    val boardDetailState by viewModel.boardDetailState.collectAsState()
    val postListState by viewModel.postListState.collectAsState()
    val followState by viewModel.followState.collectAsState()
    
    // 加载数据
    LaunchedEffect(boardId, token) {
        if (token.isNotEmpty() && boardId > 0) {
            viewModel.loadBoardDetail(token, boardId)
            viewModel.loadPostList(token, boardId)
        }
    }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = boardDetailState.board?.name ?: boardName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        
        // 分区信息卡片
        boardDetailState.board?.let { board ->
            BoardInfoCard(
                board = board,
                onGroupListClick = {
                    // 跳转到群聊列表Activity
                    val intent = Intent(context, GroupListActivity::class.java).apply {
                        putExtra("board_id", boardId)
                        putExtra("board_name", board.name)
                        putExtra("token", token)
                    }
                    context.startActivity(intent)
                },
                onFollowClick = {
                    viewModel.followBoard(token, boardId)
                },
                followState = followState,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        // 错误提示
        postListState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // 文章列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(postListState.posts) { post ->
                PostListItem(
                    post = post,
                    onClick = {
                        // 跳转到文章详情
                        val intent = Intent(context, PostDetailActivity::class.java).apply {
                            putExtra("post_id", post.id)
                            putExtra("post_title", post.title)
                            putExtra("token", token)
                        }
                        context.startActivity(intent)
                    }
                )
            }
            
            // 加载更多按钮
            if (postListState.posts.isNotEmpty() && postListState.hasMore) {
                item {
                    Button(
                        onClick = { viewModel.loadMorePosts(token, boardId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        enabled = !postListState.isLoading
                    ) {
                        if (postListState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (postListState.isLoading) "加载中..." else "加载更多")
                    }
                }
            }
            
            // 空状态
            if (postListState.posts.isEmpty() && !postListState.isLoading) {
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
            if (postListState.isLoading && postListState.posts.isEmpty()) {
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
        
        // 浮空发文章按钮 - 位置稍微上移
        FloatingActionButton(
            onClick = {
                // 启动发文章Activity
                val intent = Intent(context, CreatePostActivity::class.java).apply {
                    putExtra("board_id", boardId)
                    putExtra("board_name", boardDetailState.board?.name ?: boardName)
                    putExtra("token", token)
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "发布文章"
            )
        }
    }
    }
}

/**
 * 分区信息卡片
 */
@Composable
fun BoardInfoCard(
    board: CommunityBoard,
    onGroupListClick: () -> Unit,
    onFollowClick: () -> Unit,
    followState: FollowState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageUtils.createBoardImageRequest(
                    context = LocalContext.current,
                    url = board.avatar
                ),
                contentDescription = board.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = board.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoItem(label = "成员", value = board.memberNum.toString())
                    InfoItem(label = "文章", value = board.postNum.toString())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onGroupListClick() }
                    ) {
                    InfoItem(label = "群聊", value = board.groupNum.toString())
                        if (board.groupNum > 0) {
                            Text(
                                text = " >",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "创建于 ${board.createTimeText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 关注状态按钮
            Button(
                onClick = onFollowClick,
                enabled = !followState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (board.isFollowed == "1") 
                        MaterialTheme.colorScheme.surface 
                    else 
                        MaterialTheme.colorScheme.primary,
                    contentColor = if (board.isFollowed == "1") 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                if (followState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = if (board.isFollowed == "1") "已关注" else "未关注",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * 信息项
 */
@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 文章列表项
 */
@Composable
fun PostListItem(
    post: CommunityPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 作者信息
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageUtils.createBoardImageRequest(
                        context = LocalContext.current,
                        url = post.senderAvatar
                    ),
                    contentDescription = post.senderNickname,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.senderNickname,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (post.isVip == 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "VIP",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Text(
                        text = post.createTimeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 内容类型标识
                Surface(
                    color = if (post.contentType == 2) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (post.contentType == 2) "MD" else "文本",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (post.contentType == 2) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 文章标题
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 文章内容预览
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 统计信息
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatItem(
                    icon = if (post.isLiked == "1") Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    count = post.likeNum,
                    isActive = post.isLiked == "1"
                )
                StatItem(
                    icon = Icons.Default.Comment,
                    count = post.commentNum,
                    isActive = false
                )
                StatItem(
                    icon = Icons.Default.Star,
                    count = post.collectNum,
                    isActive = post.isCollected == 1
                )
                if (post.amountNum > 0) {
                    StatItem(
                        icon = Icons.Default.MonetizationOn,
                        count = post.amountNum.toInt(),
                        isActive = post.isReward == 1
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
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
