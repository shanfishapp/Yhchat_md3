package com.yhchat.canary.ui.community

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.CommunityPost
import com.yhchat.canary.data.model.CommunityComment
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

/**
 * 文章详情Activity
 */
class PostDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val postId = intent.getIntExtra("post_id", 0)
        val postTitle = intent.getStringExtra("post_title") ?: "文章详情"
        val token = intent.getStringExtra("token") ?: ""
        
        setContent {
            YhchatCanaryTheme {
                val viewModel: PostDetailViewModel = viewModel {
                    PostDetailViewModel(
                        communityRepository = RepositoryFactory.getCommunityRepository(this@PostDetailActivity),
                        tokenRepository = RepositoryFactory.
                        getTokenRepository(this@PostDetailActivity)
                    )
                }
                
                PostDetailScreen(
                    postId = postId,
                    postTitle = postTitle,
                    token = token,
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

/**
 * 文章详情界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    postTitle: String,
    token: String,
    viewModel: PostDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 获取状态
    val postDetailState by viewModel.postDetailState.collectAsState()
    val commentListState by viewModel.commentListState.collectAsState()
    
    // 评论输入状态
    var commentText by remember { mutableStateOf("") }
    var showCommentInput by remember { mutableStateOf(false) }
    
    // 打赏对话框状态
    var showRewardDialog by remember { mutableStateOf(false) }
    
    // 加载数据
    LaunchedEffect(postId, token) {
        if (token.isNotEmpty() && postId > 0) {
            viewModel.loadPostDetail(token, postId)
        }
    }
    
    // 错误处理
    LaunchedEffect(postDetailState.error, commentListState.error) {
        postDetailState.error?.let { error ->
            // 可以在这里显示Snackbar或其他错误提示
            viewModel.clearError()
        }
        commentListState.error?.let { error ->
            // 可以在这里显示Snackbar或其他错误提示
            viewModel.clearError()
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = postDetailState.post?.title ?: postTitle,
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
            }
        )
        
        // 错误提示
        postDetailState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
        
        // 加载状态
        if (postDetailState.isLoading && postDetailState.post == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            postDetailState.post?.let { post ->
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 文章内容
                    item {
                        PostContentCard(
                            post = post,
                            onLikeClick = {
                                viewModel.likePost(token, postId)
                            },
                            onCollectClick = {
                                viewModel.collectPost(token, postId)
                            },
                            onCommentClick = {
                                showCommentInput = !showCommentInput
                            },
                            onRewardClick = {
                                showRewardDialog = true
                            }
                        )
                    }
                    
                    // 评论标题
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "评论 (${commentListState.comments.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            TextButton(
                                onClick = { showCommentInput = !showCommentInput }
                            ) {
                                Text("写评论")
                            }
                        }
                    }
                    
                    // 评论列表
                    items(commentListState.comments) { comment ->
                        CommentItem(comment = comment)
                    }
                    
                    // 加载更多评论
                    if (commentListState.hasMore) {
                        item {
                            Button(
                                onClick = { viewModel.loadMoreComments(token, postId) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !commentListState.isLoading
                            ) {
                                if (commentListState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (commentListState.isLoading) "加载中..." else "加载更多评论")
                            }
                        }
                    }
                }
                
                // 评论输入框
                if (showCommentInput) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("写下你的评论...") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (commentText.isNotBlank()) {
                                            viewModel.commentPost(token, postId, commentText.trim())
                                            commentText = ""
                                            showCommentInput = false
                                        }
                                    }
                                )
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        viewModel.commentPost(token, postId, commentText.trim())
                                        commentText = ""
                                        showCommentInput = false
                                    }
                                },
                                enabled = commentText.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "发送评论"
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 打赏对话框
        if (showRewardDialog) {
            RewardDialog(
                onDismiss = { showRewardDialog = false },
                onReward = { amount ->
                    viewModel.rewardPost(token, postId, amount)
                    showRewardDialog = false
                }
            )
        }
    }
}

/**
 * 文章内容卡片
 */
@Composable
fun PostContentCard(
    post: CommunityPost,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onCommentClick: () -> Unit = {},
    onRewardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 作者信息
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.senderAvatar,
                    contentDescription = post.senderNickname,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.senderNickname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (post.isVip == 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "VIP",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 文章标题
            Text(
                text = post.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 文章内容
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = if (post.isLiked == "1") Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    text = "点赞",
                    count = post.likeNum,
                    isActive = post.isLiked == "1",
                    onClick = onLikeClick
                )
                
                ActionButton(
                    icon = Icons.Default.Comment,
                    text = "评论",
                    count = post.commentNum,
                    isActive = false,
                    onClick = onCommentClick
                )
                
                ActionButton(
                    icon = if (post.isCollected == 1) Icons.Default.Star else Icons.Default.StarBorder,
                    text = "收藏",
                    count = post.collectNum,
                    isActive = post.isCollected == 1,
                    onClick = onCollectClick
                )
                
                ActionButton(
                    icon = Icons.Default.MonetizationOn,
                    text = "打赏",
                    count = post.amountNum.toInt(),
                    isActive = post.isReward == 1,
                    onClick = onRewardClick
                )
            }
        }
    }
}

/**
 * 操作按钮
 */
@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    count: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isActive) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
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

/**
 * 评论项
 */
@Composable
fun CommentItem(
    comment: CommunityComment,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = comment.senderAvatar,
                contentDescription = comment.senderNickname,
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.senderNickname,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (comment.isVip == 1) {
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
                    text = comment.createTimeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (comment.likeNum > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "👍 ${comment.likeNum}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 打赏对话框
 */
@Composable
fun RewardDialog(
    onDismiss: () -> Unit,
    onReward: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var rewardAmount by remember { mutableStateOf("") }
    val predefinedAmounts = listOf(0.1, 0.5, 1.0, 2.0, 5.0, 10.0)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "打赏文章",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "感谢作者的精彩内容，给予一些支持吧！",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // 预设金额按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    predefinedAmounts.take(3).forEach { amount ->
                        FilterChip(
                            onClick = { rewardAmount = amount.toString() },
                            label = { Text("${amount}币") },
                            selected = rewardAmount == amount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    predefinedAmounts.drop(3).forEach { amount ->
                        FilterChip(
                            onClick = { rewardAmount = amount.toString() },
                            label = { Text("${amount}币") },
                            selected = rewardAmount == amount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // 自定义金额输入
                OutlinedTextField(
                    value = rewardAmount,
                    onValueChange = { 
                        // 只允许输入数字和小数点
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            rewardAmount = it
                        }
                    },
                    label = { Text("自定义金额") },
                    placeholder = { Text("输入打赏金额") },
                    suffix = { Text("币") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = rewardAmount.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onReward(amount)
                    }
                },
                enabled = rewardAmount.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("确认打赏")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
