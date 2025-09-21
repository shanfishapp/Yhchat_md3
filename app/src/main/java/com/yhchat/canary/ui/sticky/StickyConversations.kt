package com.yhchat.canary.ui.sticky

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yhchat.canary.data.model.StickyItem
import com.yhchat.canary.R

/**
 * 置顶会话组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickyConversations(
    onConversationClick: (String, Int, String) -> Unit, // chatId, chatType, chatName
    tokenRepository: com.yhchat.canary.data.repository.TokenRepository? = null,
    viewModel: StickyViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val stickyData by viewModel.stickyData.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // 设置tokenRepository
    LaunchedEffect(tokenRepository) {
        if (tokenRepository != null) {
            viewModel.setTokenRepository(tokenRepository)
        }
    }

    // 加载置顶会话数据
    LaunchedEffect(Unit) {
        if (tokenRepository != null) {
            viewModel.loadStickyList()
        }
    }

    // 如果没有置顶会话，不显示组件
    if (stickyData?.sticky.isNullOrEmpty()) {
        return
    }

    Column(modifier = modifier) {
        // 置顶会话标题
        Text(
            text = "置顶会话",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 置顶会话横向列表
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            stickyData?.sticky?.let { stickyList ->
                items(stickyList) { stickyItem ->
                    StickyConversationItem(
                        stickyItem = stickyItem,
                        onClick = {
                            onConversationClick(
                                stickyItem.chatId,
                                stickyItem.chatType,
                                stickyItem.chatName
                            )
                        }
                    )
                }
            }
        }

        // 分隔线
        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

/**
 * 置顶会话项
 */
@Composable
fun StickyConversationItem(
    stickyItem: StickyItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(80.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 头像
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(stickyItem.avatarUrl)
                    .addHeader("Referer", "https://myapp.jwznb.com")
                    .crossfade(true)
                    .build(),
                contentDescription = "头像",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person)
            )

            // 认证标识
            if (stickyItem.certificationLevel > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            when (stickyItem.certificationLevel) {
                                1 -> Color(0xFF4CAF50) // 官方 - 绿色
                                2 -> Color(0xFF2196F3) // 地区 - 蓝色
                                else -> Color.Gray
                            },
                            CircleShape
                        )
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (stickyItem.certificationLevel) {
                            1 -> "官"
                            2 -> "地"
                            else -> "认"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 8.sp
                    )
                }
            }
        }

        // 会话名称
        Text(
            text = stickyItem.chatName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(70.dp)
        )

        // 会话类型标识
        val typeText = when (stickyItem.chatType) {
            1 -> "好友"
            2 -> "群聊"
            3 -> "机器人"
            else -> ""
        }

        if (typeText.isNotEmpty()) {
            Text(
                text = typeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.width(70.dp)
            )
        }
    }
}
