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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.CommunityPost
import com.yhchat.canary.data.model.CommunityComment
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import com.yhchat.canary.ui.profile.UserProfileActivity
import com.yhchat.canary.util.YunhuLinkHandler
import com.yhchat.canary.utils.UnifiedLinkHandler
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import android.widget.TextView
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.yhchat.canary.ui.components.ImageViewer
import com.yhchat.canary.ui.components.MarkdownText
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.data.model.CommunityBoard
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import com.yhchat.canary.utils.ChatAddLinkHandler
import java.util.regex.Pattern
import android.net.Uri

/**
 * 文章详情Activity
 */
class PostDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 首先尝试处理深度链接
        var postId = intent.getIntExtra("post_id", 0)
        var postTitle = intent.getStringExtra("post_title") ?: "文章详情"
        var token = intent.getStringExtra("token") ?: ""
        
        // 处理深度链接
        if (postId == 0 && intent.data != null) {
            val deepLinkPostId = YunhuLinkHandler.extractPostIdFromLink(intent.data.toString())
            if (deepLinkPostId != null) {
                postId = deepLinkPostId
                postTitle = "文章详情"
                // 可能需要从其他地方获取token，这里先留空
            }
        }
        
        setContent {
            YhchatCanaryTheme {
                val viewModel: PostDetailViewModel = viewModel {
                    PostDetailViewModel(
                        communityRepository = RepositoryFactory.getCommunityRepository(this@PostDetailActivity),
                        tokenRepository = RepositoryFactory.getTokenRepository(this@PostDetailActivity)
                    )
                }
                
                PostDetailScreen(
                    postId = postId,
                    postTitle = postTitle,
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

/**
 * 文章内容卡片
 */
@Composable
fun PostContentCard(
    post: CommunityPost,
    board: CommunityBoard? = null,
    token: String = "",
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onCommentClick: () -> Unit = {},
    onRewardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showImageViewer by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 作者信息
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.senderAvatar,
                contentDescription = post.senderNickname,
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        UserProfileActivity.start(context, post.senderId, post.senderNickname)
                    },
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 文章内容 - 支持Markdown和HTML
        if (post.contentType == 2) {
            // Markdown 内容 - 使用统一的MarkdownText组件
            MarkdownText(
                markdown = post.content,
                onImageClick = { imageUrl ->
                    currentImageUrl = imageUrl
                    showImageViewer = true
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // 普通文本内容 - 支持链接点击
            ArticleLinkText(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 群聊来源信息
        post.group?.let { group ->
            if (!group.groupId.isNullOrEmpty() && group.groupId != "0") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // 使用统一链接处理器跳转到群聊添加页面
                            val groupLink = "yunhu://chat-add?id=${group.groupId}&type=group"
                            UnifiedLinkHandler.handleLink(context, groupLink)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = group.avatarUrl,
                            contentDescription = group.name,
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "来自 ${group.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        // 分区信息卡片
        board?.let { boardInfo ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // 跳转到分区详情页
                        val intent = Intent(context, BoardDetailActivity::class.java).apply {
                            putExtra("board_id", boardInfo.id)
                            putExtra("board_name", boardInfo.name)
                            putExtra("token", token)
                        }
                        context.startActivity(intent)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageUtils.createBoardImageRequest(
                            context = context,
                            url = boardInfo.avatar
                        ),
                        contentDescription = boardInfo.name,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = boardInfo.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${boardInfo.memberNum} 成员 · ${boardInfo.postNum} 文章",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "进入分区",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 操作按钮 - 使用MD3图标
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = if (post.isLiked == "1") Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
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
                    icon = if (post.isCollected == 1) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    text = "收藏",
                    count = post.collectNum,
                    isActive = post.isCollected == 1,
                    onClick = onCollectClick
                )
                
                ActionButton(
                    icon = if (post.isReward == 1) Icons.Filled.MonetizationOn else Icons.Outlined.MonetizationOn,
                    text = "打赏",
                    count = post.amountNum.toInt(),
                    isActive = post.isReward == 1,
                    onClick = onRewardClick
                )
            }
        }
    }
    
    // 图片预览器
    if (showImageViewer) {
        ImageViewer(
            imageUrl = currentImageUrl,
            onDismiss = { showImageViewer = false }
        )
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
    onLikeClick: (Int) -> Unit = {},
    onReplyClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = comment.senderAvatar,
                    contentDescription = comment.senderNickname,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            UserProfileActivity.start(context, comment.senderId, comment.senderNickname)
                        },
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
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 评论操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 点赞按钮
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLikeClick(comment.id) }
                    ) {
                        Icon(
                            imageVector = if (comment.isLiked == "1") Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "点赞",
                            tint = if (comment.isLiked == "1") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        if (comment.likeNum > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = comment.likeNum.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (comment.isLiked == "1") 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 回复按钮
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onReplyClick(comment.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = "回复",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        if (comment.repliesNum > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = comment.repliesNum.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 显示回复
            if (!comment.replies.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.padding(start = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    comment.replies.forEach { reply ->
                        CommentReplyItem(reply = reply)
                    }
                }
            }
        }
    }
}

/**
 * 评论回复项
 */
@Composable
fun CommentReplyItem(
    reply: CommunityComment,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = reply.senderAvatar,
            contentDescription = reply.senderNickname,
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    UserProfileActivity.start(context, reply.senderId, reply.senderNickname)
                },
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reply.senderNickname,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                if (reply.isVip == 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "VIP",
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = reply.createTimeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = reply.content,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Markwon Markdown渲染组件
 */
@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                val markwon = Markwon.builder(ctx)
                    .usePlugin(SoftBreakAddsNewLinePlugin.create())  // 支持软换行（单个回车换行）
                    .usePlugin(HtmlPlugin.create())
                    .usePlugin(CoilImagesPlugin.create(ctx))
                    .usePlugin(object : AbstractMarkwonPlugin() {
                        override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                            builder.linkResolver { view, link ->
                                // 处理链接点击
                                if (UnifiedLinkHandler.isHandleableLink(link)) {
                                    UnifiedLinkHandler.handleLink(ctx, link)
                                } else {
                                    // 使用默认浏览器打开其他链接
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                                        ctx.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(ctx, "无法打开链接", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    })
                    .usePlugin(LinkifyPlugin.create())
                    .usePlugin(StrikethroughPlugin.create())
                    .usePlugin(TablePlugin.create(ctx))
                    .build()
                
                textSize = 16f
                setPadding(0, 0, 0, 0)
                setTextColor(textColor)
                
                // 设置链接可点击
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
                
                markwon.setMarkdown(this, markdown)
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            val markwon = Markwon.builder(context)
                .usePlugin(SoftBreakAddsNewLinePlugin.create())  // 支持软换行（单个回车换行）
                .usePlugin(HtmlPlugin.create())
                .usePlugin(CoilImagesPlugin.create(context))
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                        builder.linkResolver { view, link ->
                            // 处理链接点击
                            if (UnifiedLinkHandler.isHandleableLink(link)) {
                                UnifiedLinkHandler.handleLink(context, link)
                            } else {
                                // 使用默认浏览器打开其他链接
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                })
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(context))
                .build()
            
            markwon.setMarkdown(textView, markdown)
        }
    )
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
                
                OutlinedTextField(
                    value = rewardAmount,
                    onValueChange = { 
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

/**
 * 分享对话框
 */
@Composable
fun ShareDialog(
    postId: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webLink = "www.yhchat.com/c/p/$postId"
    val deepLink = "yunhu://post-detail?id=$postId"
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "分享文章",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "选择分享方式：",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "网页链接",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = webLink,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("文章链接", webLink)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "网页链接已复制", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "复制",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("复制链接")
                        }
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "应用内链接",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = deepLink,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("文章链接", deepLink)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "应用内链接已复制", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "复制",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("复制链接")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 文章详情界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    postTitle: String,
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
    
    // 分享对话框状态
    var showShareDialog by remember { mutableStateOf(false) }
    
    // Token状态
    var currentToken by remember { mutableStateOf("") }
    
    // 加载数据
    LaunchedEffect(postId) {
        if (postId > 0) {
            viewModel.loadPostDetailWithToken(postId)
            // 获取token
            currentToken = viewModel.getTokenAsync()
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
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
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
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享"
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
                    val swipeRefreshState = rememberSwipeRefreshState(postDetailState.isRefreshing)
                    SwipeRefresh(
                        state = swipeRefreshState,
                        onRefresh = { viewModel.refreshPostDetailWithToken(postId) }
                    ) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 文章内容
                            item {
                                PostContentCard(
                                    post = post,
                                    board = postDetailState.board,
                                    token = currentToken,
                                    onLikeClick = {
                                        viewModel.likePostWithToken(postId)
                                    },
                                    onCollectClick = {
                                        viewModel.collectPostWithToken(postId)
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
                                CommentItem(
                                    comment = comment,
                                    onLikeClick = { commentId ->
                                        viewModel.likeCommentWithToken(postId, commentId)
                                    },
                                    onReplyClick = { commentId ->
                                        // TODO: 实现回复功能
                                        showCommentInput = true
                                    }
                                )
                            }
                            
                            // 加载更多评论
                            if (commentListState.hasMore) {
                                item {
                                    Button(
                                        onClick = { viewModel.loadMoreCommentsWithToken(postId) },
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
                                    minLines = 1,
                                    maxLines = 5,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Send,
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (commentText.isNotBlank()) {
                                                // 处理换行符：保持原始换行符
                                                val processedContent = commentText.trim()
                                                android.util.Log.d("PostDetail", "发送评论: postId=$postId, content=$processedContent")
                                                viewModel.commentPostWithToken(postId, processedContent)
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
                                            // 处理换行符：保持原始换行符
                                            val processedContent = commentText.trim()
                                            viewModel.commentPostWithToken(postId, processedContent)
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
                        viewModel.rewardPostWithToken(postId, amount)
                        showRewardDialog = false
                    }
                )
            }
            
            // 分享对话框
            if (showShareDialog) {
                ShareDialog(
                    postId = postId,
                    onDismiss = { showShareDialog = false }
                )
            }
        }
    }
}

/**
 * 文章链接文本组件 - 支持 HTTP/HTTPS 链接点击，遇到中文停止识别
 */
@Composable
fun ArticleLinkText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // HTTP/HTTPS 链接正则表达式 - 遇到中文字符停止
    val urlPattern = Pattern.compile(
        "https?://[^\\s\\u4e00-\\u9fff]+",
        Pattern.CASE_INSENSITIVE
    )
    
    val annotatedString = buildAnnotatedString {
        val matcher = urlPattern.matcher(text)
        var lastEnd = 0
        
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val url = matcher.group()
            
            // 添加链接前的普通文本
            if (start > lastEnd) {
                append(text.substring(lastEnd, start))
            }
            
            // 添加链接文本 - 醒目的样式
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(url)
            }
            pop()
            
            lastEnd = end
        }
        
        // 添加剩余的普通文本
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
    
    ClickableText(
        text = annotatedString,
        style = style,
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val url = annotation.item
                    
                    // 优先使用应用内链接处理器
                    when {
                        YunhuLinkHandler.containsYunhuLink(url) -> {
                            YunhuLinkHandler.handleYunhuLink(context, url)
                        }
                        ChatAddLinkHandler.isChatAddLink(url) -> {
                            ChatAddLinkHandler.handleLink(context, url)
                        }
                        UnifiedLinkHandler.isHandleableLink(url) -> {
                            UnifiedLinkHandler.handleLink(context, url)
                        }
                        else -> {
                            // 使用系统浏览器打开其他链接
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
        }
    )
}