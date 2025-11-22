package com.yhchat.canary.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yhchat.canary.proto.chat_ws_go.ChatWsGoProto

/**
 * 机器人看板内容组件
 * @param boardData 看板数据
 * @param onImageClick 点击图片时的回调
 */
@Composable
fun BotBoardContent(
    boardData: ChatWsGoProto.BotBoardMessage.BoardData.BoardContent,
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
            boardData.botName?.let { botName ->
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
            boardData.content?.let { content ->
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
    groupBots: List<com.yhchat.canary.data.model.Bot>,
    groupBotBoards: Map<String, ChatWsGoProto.BotBoardMessage.BoardData>,
    onImageClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(groupBots) { bot ->
            val botBoardData = groupBotBoards[bot.botId]
            if (botBoardData != null) {
                val boardContent = botBoardData.board
                if (boardContent != null && boardContent.content.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // 机器人名称
                            Text(
                                text = bot.name ?: "未知机器人",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            // 看板内容
                            BotBoardContent(
                                boardData = boardContent,
                                onImageClick = onImageClick
                            )
                        }
                    }
                }
            }
        }
    }
}