package com.yhchat.canary.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.yhchat.canary.proto.group.Bot_data
import com.yhchat.canary.ui.components.HtmlWebView
import com.yhchat.canary.ui.components.ImageUtils
import com.yhchat.canary.ui.components.MarkdownText
import yh_bot.Bot

/**
 * 机器人看板内容组件（可复用）
 */
@Composable
fun BotBoardContent(
    boardData: Bot.board.Board_data,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            when (boardData.contentType) {
                1 -> { // 文本
                    Text(
                        text = boardData.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                2 -> { // Markdown
                    MarkdownText(
                        markdown = boardData.content,
                        modifier = Modifier.fillMaxWidth(),
                        onImageClick = onImageClick
                    )
                }
                3 -> { // HTML
                    HtmlWebView(
                        htmlContent = boardData.content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        onImageClick = onImageClick
                    )
                }
                else -> { // 默认按文本处理
                    Text(
                        text = boardData.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 群聊机器人看板区域
 */
@Composable
fun GroupBotBoardsSection(
    groupBots: List<Bot_data>,
    groupBotBoards: Map<String, Bot.board.Board_data>,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 过滤出有看板内容的机器人
    val botsWithBoards = groupBots.filter { bot ->
        groupBotBoards[bot.botId]?.content?.isNotBlank() == true
    }

    if (botsWithBoards.isEmpty()) {
        return
    }

    var selectedBotId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        // 机器人选择按钮列表
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(botsWithBoards) { bot ->
                val isSelected = selectedBotId == bot.botId

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedBotId = if (isSelected) null else bot.botId
                    },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 机器人头像
                            AsyncImage(
                                model = ImageUtils.createBotImageRequest(
                                    context = LocalContext.current,
                                    url = bot.avatarUrl
                                ),
                                contentDescription = bot.name,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Text(
                                text = bot.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选中",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }

        // 显示选中机器人的看板内容
        AnimatedVisibility(
            visible = selectedBotId != null,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = shrinkVertically() + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            selectedBotId?.let { botId ->
                groupBotBoards[botId]?.let { boardData ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)  // 限制最大高度
                    ) {
                        // 看板标题
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${boardData.botName}的看板",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 关闭按钮
                            IconButton(
                                onClick = { selectedBotId = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "收起",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 看板内容 - 添加滚动支持
                        BotBoardContentScrollable(
                            boardData = boardData,
                            onImageClick = onImageClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * 可滚动的机器人看板内容组件
 */
@Composable
private fun BotBoardContentScrollable(
    boardData: Bot.board.Board_data,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Card已经包含在了BotBoardContent中，这里直接调用
    Box(modifier = modifier
        .fillMaxWidth()
        .heightIn(max = 400.dp) // 限制最大高度
        .verticalScroll(scrollState) // 添加垂直滚动
    ) {
        BotBoardContent(boardData = boardData, onImageClick = onImageClick)
    }
}
