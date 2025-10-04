package com.yhchat.canary.ui.info

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.CircleShape
import com.yhchat.canary.ui.components.ImageUtils
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yhchat.canary.data.model.GroupMemberInfo
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import android.widget.Toast
import android.app.LocalActivityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreenRoot(
    userId: String,
    userName: String,
    viewModel: UserProfileViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    UserProfileScreen(
        userId = userId,
        userName = userName,
        uiState = uiState,
        onBackClick = onBackClick,
        onDeleteFriend = { 
            viewModel.delFriend(it)
            if (uiState.deleteSuccess == true) {
                Toast.makeText(context, "删除好友成功!", Toast.LENGTH_SHORT).show()
                onBackClick()
            } else {
                Toast.makeText(context, "删除好友失败!", Toast.LENGTH_SHORT).show()
                onBackClick()
            }
        },
        modifier = modifier
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    userName: String,
    uiState: UserProfileUiState,
    onBackClick: () -> Unit,
    onDeleteFriend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户信息") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加载失败: ${uiState.error}")
                    }
                }
                else -> {
                    UserProfileContent(
                        userId = userId,
                        userName = userName,
                        userInfo = uiState.userInfo,
                        isDeleting = uiState.isDeleting,
                        onDeleteFriend = onDeleteFriend,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun UserProfileContent(
    userId: String,
    userName: String,
    userInfo: com.yhchat.canary.data.model.UserInfo?,
    isDeleting: Boolean = false,
    onDeleteFriend: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头像
        val avatarUrl = userInfo?.avatarUrl
        AsyncImage(
            model = if (!avatarUrl.isNullOrBlank()) {
                ImageUtils.createBotImageRequest(
                    context = LocalContext.current,
                    url = avatarUrl
                )
            } else {
                null
            },
            contentDescription = "用户头像",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // 名称
        Text(
            text = userInfo?.nickname ?: userName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // ID
        Text(
            text = "ID: ${userInfo?.userId ?: userId}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 注册时间
        if (userInfo?.registerTimeText != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "注册时间",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = userInfo.registerTimeText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 在线天数
        if (userInfo?.onLineDay != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "在线天数",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${userInfo.onLineDay}天",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 删除好友按钮
        Button(
            onClick = { onDeleteFriend(userId) },
            enabled = !isDeleting,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isDeleting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除好友")
                }
            }
        }
    }
}