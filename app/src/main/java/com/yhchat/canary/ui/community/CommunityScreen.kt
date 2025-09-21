package com.yhchat.canary.ui.community

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhchat.canary.ui.components.IconShowcase
import com.yhchat.canary.data.model.CommunityBoard
import com.yhchat.canary.data.model.CommunityPost

/**
 * 社区界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    token: String,
    modifier: Modifier = Modifier,
    viewModel: CommunityViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // 获取状态
    val boardListState by viewModel.boardListState.collectAsState()
    
    // 加载分区列表
    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            viewModel.loadBoardList(token)
        }
    }
    
    // 显示分区列表
    BoardListScreen(
        boards = boardListState.boards,
        isLoading = boardListState.isLoading,
        error = boardListState.error,
        onBoardClick = { board ->
            // 启动分区详情Activity
            val intent = Intent(context, BoardDetailActivity::class.java).apply {
                putExtra("board_id", board.id)
                putExtra("board_name", board.name)
                putExtra("token", token)
            }
            context.startActivity(intent)
        },
        onSearchClick = {
            // 搜索功能
        },
        modifier = modifier
    )
}

