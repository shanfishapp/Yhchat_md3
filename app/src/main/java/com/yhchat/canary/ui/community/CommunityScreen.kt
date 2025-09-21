package com.yhchat.canary.ui.community

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 社区界面
 */
@Composable
fun CommunityScreen(
    token: String,
    modifier: Modifier = Modifier,
    viewModel: CommunityViewModel = viewModel()
) {
    // 显示社区标签页界面
    CommunityTabScreen(
        token = token,
        viewModel = viewModel,
        modifier = modifier
    )
}

