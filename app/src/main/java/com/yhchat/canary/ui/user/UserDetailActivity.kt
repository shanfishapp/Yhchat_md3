package com.yhchat.canary.ui.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yhchat.canary.data.api.ApiClient
import com.yhchat.canary.data.local.AppDatabase
import com.yhchat.canary.data.model.Medal
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.UserRepository
import com.yhchat.canary.ui.chat.ChatActivity
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import yh_user.User

/**
 * 用户详情Activity
 */
class UserDetailActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_USER_ID = "user_id"
        private const val EXTRA_USER_NAME = "user_name"
        
        fun start(context: Context, userId: String, userName: String = "") {
            val intent = Intent(context, UserDetailActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: ""
        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: ""
        
        setContent {
            YhchatCanaryTheme {
                UserDetailScreen(
                    userId = userId,
                    userName = userName,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

/**
 * 用户详情数据状态
 */
data class UserDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userId: String = "",
    val userName: String = "",
    val avatarUrl: String = "",
    val avatarId: Long = 0L,
    val nameId: Long = 0L,
    val medals: List<Medal> = emptyList(),
    val registerTime: String = "",
    val banTime: Long = 0L,
    val onlineDay: Int = 0,
    val continuousOnlineDay: Int = 0,
    val isVip: Int = 0,
    val vipExpiredTime: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    userId: String,
    userName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf(UserDetailUiState(userId = userId, userName = userName)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // 加载用户详情
    LaunchedEffect(userId) {
        uiState = uiState.copy(isLoading = true, error = null)
        
        try {
            val db = AppDatabase.getDatabase(context)
            val tokenRepository = TokenRepository(db.userTokenDao(), context)
            val userRepository = UserRepository(ApiClient.apiService, tokenRepository)
            
            val token = tokenRepository.getTokenSync() ?: ""
            
            // 构建ProtoBuf请求
            val requestProto = User.get_user_send.newBuilder()
                .setId(userId)
                .build()
            
            val requestBody = requestProto.toByteArray()
                .toRequestBody("application/x-protobuf".toMediaTypeOrNull())
            
            val response = userRepository.getUserDetail(token, requestBody)
            
            response.fold(
                onSuccess = { responseBytes ->
                    // 解析ProtoBuf响应
                    val getUserResponse = User.get_user.parseFrom(responseBytes)
                    
                    if (getUserResponse.status.code == 1) {
                        val data = getUserResponse.data
                        
                        uiState = uiState.copy(
                            isLoading = false,
                            userId = data.id,
                            userName = data.name,
                            avatarUrl = data.avatarUrl,
                            avatarId = data.avatarId,
                            nameId = data.nameId,
                            medals = data.medalList.map { medalInfo ->
                                Medal(
                                    id = medalInfo.id.toInt(),
                                    name = medalInfo.name,
                                    desc = "",
                                    imageUrl = "",
                                    sort = medalInfo.sort.toInt()
                                )
                            },
                            registerTime = data.registerTime,
                            banTime = data.banTime,
                            onlineDay = data.onlineDay,
                            continuousOnlineDay = data.continuousOnlineDay,
                            isVip = data.isVip,
                            vipExpiredTime = data.vipExpiredTime
                        )
                    } else {
                        uiState = uiState.copy(
                            isLoading = false,
                            error = getUserResponse.status.msg
                        )
                    }
                },
                onFailure = { throwable ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = throwable.message ?: "加载失败"
                    )
                }
            )
        } catch (e: Exception) {
            uiState = uiState.copy(
                isLoading = false,
                error = e.message ?: "加载失败"
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户详情") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 发消息按钮
                    IconButton(
                        onClick = {
                            val intent = Intent(context, ChatActivity::class.java).apply {
                                putExtra("chatId", userId)
                                putExtra("chatType", 1)
                                putExtra("chatName", uiState.userName)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.Chat, "发消息")
                    }
                    
                    // 删除好友按钮
                    IconButton(
                        onClick = {
                            showDeleteDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Delete, "删除好友")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            // 重新加载
                            uiState = uiState.copy(isLoading = true, error = null)
                        }) {
                            Text("重试")
                        }
                    }
                }
                else -> {
                    UserDetailContent(uiState = uiState)
                }
            }
        }
        
        // 删除好友确认对话框
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除好友") },
                text = { Text("确定要删除好友 ${uiState.userName} 吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            // 执行删除操作
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val db = AppDatabase.getDatabase(context)
                                    val tokenRepository = TokenRepository(db.userTokenDao(), context)
                                    val userRepository = UserRepository(ApiClient.apiService, tokenRepository)
                                    
                                    userRepository.deleteFriend(userId, 1).fold(
                                        onSuccess = {
                                            Toast.makeText(context, "已删除好友", Toast.LENGTH_SHORT).show()
                                            // 返回上一页
                                            (context as? ComponentActivity)?.finish()
                                        },
                                        onFailure = { throwable ->
                                            Toast.makeText(
                                                context,
                                                "删除失败: ${throwable.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "删除失败: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Text("确定", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun UserDetailContent(uiState: UserDetailUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 用户头像和基本信息
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 头像
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uiState.avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "用户头像",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 用户名
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.userName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (uiState.isVip == 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "VIP",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 用户ID
                    Text(
                        text = "ID: ${uiState.userId}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // 勋章
        if (uiState.medals.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "勋章",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        uiState.medals.forEach { medal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = medal.name,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 账号信息
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "账号信息",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    InfoRow("注册时间", uiState.registerTime)
                    InfoRow("在线天数", "${uiState.onlineDay} 天")
                    InfoRow("连续在线", "${uiState.continuousOnlineDay} 天")
                    
                    if (uiState.isVip == 1 && uiState.vipExpiredTime > 0) {
                        val expireDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date(uiState.vipExpiredTime * 1000))
                        InfoRow("VIP到期时间", expireDate)
                    }
                    
                    if (uiState.banTime > 0) {
                        val banDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date(uiState.banTime * 1000))
                        InfoRow("封禁结束时间", banDate, MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor
        )
    }
}

