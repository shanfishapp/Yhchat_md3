package com.yhchat.canary.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import yh_bot.Bot
import com.yhchat.canary.proto.group.Bot_data

/**
 * 机器人看板内容组件
 * @param boardContent 看板内容数据
 * @param onImageClick 点击图片时的回调
 */
@Composable
fun BotBoardContent(
    boardContent: Bot.board.Board_data,
    onImageClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 看板标题 (从机器人名称获取)
            boardContent.botName?.let { botName ->
                if (botName.isNotBlank()) {
                    Text(
                        text = botName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            // 看板内容
            boardContent.content?.let { content ->
                if (content.isNotBlank()) {
                    MarkdownText(
                        markdown = content,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 群聊中的机器人看板列表组件
 * @param groupBots 群组中的机器人列表
 * @param groupBotBoards 群组中机器人的看板数据
 * @param onImageClick 点击图片时的回调
 */
@Composable
fun GroupBotBoardsSection(
    groupBots: List<Bot_data>,
    groupBotBoards: Map<String, Bot.board.Board_data>,
    onImageClick: (String) -> Unit
) {
    // 追踪当前展开的机器人ID
    var expandedBotId by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 机器人按钮行（一行显示所有机器人按钮）
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groupBots.filter { groupBotBoards.containsKey(it.botId) }) { bot ->
                val botBoardData = groupBotBoards[bot.botId]
                if (botBoardData != null && botBoardData.content.isNotBlank()) {
                    Button(
                        onClick = {
                            // 如果点击的是当前展开的机器人，则收起；否则展开新的机器人
                            expandedBotId = if (expandedBotId == bot.botId) null else bot.botId
                        },
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (expandedBotId == bot.botId) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = bot.name ?: "未知机器人",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (expandedBotId == bot.botId) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        
        // 当前展开的机器人看板内容
        if (expandedBotId != null) {
            val expandedBot = groupBots.find { it.botId == expandedBotId }
            val expandedBoardData = expandedBot?.let { groupBotBoards[it.botId] }
            
            expandedBoardData?.let { boardData ->
                if (boardData.content.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        BotBoardContent(
                            boardContent = boardData,
                            onImageClick = onImageClick
                        )
                    }
                }
            }
        }
    }
}