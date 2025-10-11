package com.yhchat.canary.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yhchat.canary.data.model.Medal
import com.yhchat.canary.data.model.UserProfile
import com.yhchat.canary.data.model.UserHomepageInfo
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * 用户资料页面Activity - 完全重写版本
 * 基于 yhapi/web/v1/user.md 和 yhapi/v1/friend.md
 */
@AndroidEntryPoint
class UserProfileActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"
        private const val EXTRA_USER_NAME = "extra_user_name"
        private const val EXTRA_GROUP_ID = "extra_group_id"
        private const val EXTRA_IS_GROUP_ADMIN = "extra_is_group_admin"

        /**
         * 启动用户资料Activity
         */
        fun start(
            context: Context, 
            userId: String, 
            userName: String? = null,
            groupId: String? = null,
            isGroupAdmin: Boolean = false
        ) {
            val intent = Intent(context, UserProfileActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
                groupId?.let { putExtra(EXTRA_GROUP_ID, it) }
                putExtra(EXTRA_IS_GROUP_ADMIN, isGroupAdmin)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: ""
        val initialUserName = intent.getStringExtra(EXTRA_USER_NAME)
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID)
        val isGroupAdmin = intent.getBooleanExtra(EXTRA_IS_GROUP_ADMIN, false)

        if (userId.isEmpty()) {
            finish()
            return
        }

        setContent {
            YhchatCanaryTheme {
                UserProfileScreen(
                    userId = userId,
                    initialUserName = initialUserName,
                    groupId = groupId,
                    isGroupAdmin = isGroupAdmin,
                    onBackClick = { finish() },
                    onShowToast = { message ->
                        Toast.makeText(this@UserProfileActivity, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

/**
 * 用户资料界面 - 完全重写版本
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    initialUserName: String? = null,
    groupId: String? = null,
    isGroupAdmin: Boolean = false,
    onBackClick: () -> Unit,
    onShowToast: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel = androidx.
    hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    var showMemberMenu by remember { mutableStateOf(false) }
    var showGagMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    // 初始化时加载用户资料
    LaunchedEffect(userId) {
        println("UserProfileScreen: 开始加载用户资料, userId = $userId, initialUserName = $initialUserName")
        viewModel.loadUserProfile(userId)
    }
    
    // 如果在群聊环境，加载群信息和目标用户信息
    LaunchedEffect(groupId, userId) {
        groupId?.let { gId ->
            viewModel.loadGroupInfoAndMemberInfo(gId, userId)
        }
    }
    
    // 获取目标用户的权限等级
    val targetUserPermission = uiState.targetUserPermission
    
    // 群聊管理操作处理
    val handleRemoveMember = {
        if (groupId != null) {
            viewModel.removeMemberFromGroup(groupId, userId)
        }
    }
    
    val handleGagMember = { gagTime: Int ->
        if (groupId != null) {
            viewModel.gagMemberInGroup(groupId, userId, gagTime)
        }
    }
    
    val handleSetMemberRole = { userLevel: Int ->
        if (groupId != null) {
            viewModel.setMemberRole(groupId, userId, userLevel)
        }
    }

    // 监听添加好友成功状态
    LaunchedEffect(uiState.addFriendSuccess) {
        if (uiState.addFriendSuccess) {
            onShowToast("好友申请已发送")
            viewModel.clearAddFriendSuccess()
        }
    }

    // 监听错误状态
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            onShowToast(error)
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
                    text = initialUserName ?: "用户资料",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            actions = {
                // 举报按钮
                IconButton(onClick = { showReportDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Report,
                        contentDescription = "举报用户"
                    )
                }
                
                // 聊天背景按钮
                IconButton(onClick = {
                    com.yhchat.canary.ui.background.ChatBackgroundActivity.start(context, userId, initialUserName ?: "用户")
                }) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = "聊天背景"
                    )
                }
                
                // 群聊管理菜单（在群聊环境下都显示，权限由后端控制）
                if (groupId != null) {
                    Box {
                        IconButton(onClick = { showMemberMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多操作"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMemberMenu,
                            onDismissRequest = { showMemberMenu = false }
                        ) {
                            // 根据目标用户权限显示上任/卸任管理员
                            if (targetUserPermission == 2) {
                                // 目标是管理员，显示卸任选项
                                DropdownMenuItem(
                                    text = { Text("卸任管理员") },
                                    onClick = {
                                        showMemberMenu = false
                                        handleSetMemberRole(0)
                                    },
                                    enabled = !uiState.isProcessingMemberAction
                                )
                            } else if (targetUserPermission == 0) {
                                // 目标是普通成员，显示上任选项
                                DropdownMenuItem(
                                    text = { Text("设为管理员") },
                                    onClick = {
                                        showMemberMenu = false
                                        handleSetMemberRole(2)
                                    },
                                    enabled = !uiState.isProcessingMemberAction
                                )
                            }
                            
                            // 踢出和禁言（都显示，由后端控制权限）
                            DropdownMenuItem(
                                text = { Text("踢出群聊") },
                                onClick = {
                                    showMemberMenu = false
                                    handleRemoveMember()
                                },
                                enabled = !uiState.isProcessingMemberAction
                            )
                            DropdownMenuItem(
                                text = { Text("禁言") },
                                onClick = {
                                    showMemberMenu = false
                                    showGagMenu = true
                                },
                                enabled = !uiState.isProcessingMemberAction
                            )
                        }
                    }
                }
            }
        )

        // 内容区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when {
                uiState.isLoading -> {
                    // 加载状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                userProfile != null -> {
                    // 用户资料内容
                    val currentUserProfile = userProfile!! // 解决委托属性智能转换问题，确保非空
                    UserHomepageContent(
                        userProfile = currentUserProfile,
                        onAddFriendClick = { profile ->
                            viewModel.showAddFriendDialog(profile.userId, profile.nickname)
                        }
                    )
                }

                else -> {
                    // 空状态或错误状态
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "暂无数据",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadUserProfile(userId) }
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
        }

        // 添加好友对话框
        uiState.showAddFriendDialog?.let { dialogData ->
            AddFriendDialog(
                userName = dialogData.userName,
                remark = dialogData.remark,
                isLoading = uiState.isAddingFriend,
                onRemarkChange = { viewModel.updateFriendRemark(it) },
                onConfirm = { viewModel.confirmAddFriend() },
                onDismiss = { viewModel.dismissAddFriendDialog() }
            )
        }
        
        // 禁言对话框
        if (showGagMenu) {
            GagMemberDialog(
                userName = initialUserName ?: "该用户",
                isLoading = uiState.isProcessingMemberAction,
                onConfirm = { gagTime ->
                    handleGagMember(gagTime)
                    showGagMenu = false
                },
                onDismiss = { showGagMenu = false }
            )
        }
        
        // 举报对话框
        if (showReportDialog) {
            com.yhchat.canary.ui.components.ReportDialog(
                chatId = userId,
                chatType = 1,  // 用户
                chatName = initialUserName ?: "该用户",
                onDismiss = { showReportDialog = false },
                onSuccess = {
                    onShowToast("举报已提交")
                }
            )
        }
        }
    }
}

/**
 * 用户主页内容
 */
@Composable
private fun UserHomepageContent(
    userProfile: UserHomepageInfo,
    onAddFriendClick: (UserHomepageInfo) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // 头像和基本信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 头像
                if (!userProfile.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(userProfile.avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "用户头像",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "默认头像",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 用户名
                Text(
                    text = userProfile.nickname,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // 用户ID
                Text(
                    text = "ID: ${userProfile.userId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // VIP 标识
                if (userProfile.isVip == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "VIP",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VIP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 统计信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "统计信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatisticItem(
                        title = "在线天数",
                        value = userProfile.onLineDay.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatisticItem(
                        title = "连续在线",
                        value = userProfile.continuousOnLineDay.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatisticItem(
                        title = "注册时间",
                        value = userProfile.registerTimeText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 勋章展示
        if (userProfile.medals.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "获得勋章",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(userProfile.medals.sortedBy { it.sort }) { medal ->
                            MedalItem(medal = medal)
                        }
                    }
                }
            }
        }

        // 添加好友按钮
        Button(
            onClick = { onAddFriendClick(userProfile) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "添加好友",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
    }
}

/**
 * 统计项
 */
@Composable
private fun RowScope.StatisticItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 勋章项
 */
@Composable
private fun MedalItem(
    medal: Medal,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!medal.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(medal.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = medal.name,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "🏅",
                    fontSize = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = medal.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * 添加好友对话框
 */
@Composable
private fun AddFriendDialog(
    userName: String,
    remark: String,
    isLoading: Boolean,
    onRemarkChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("添加好友")
        },
        text = {
            Column {
                Text("确定要添加 $userName 为好友吗？")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = remark,
                    onValueChange = onRemarkChange,
                    label = { Text("申请备注（可选）") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("添加")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 禁言成员对话框
 */
@Composable
private fun GagMemberDialog(
    userName: String,
    isLoading: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val gagOptions = listOf(
        0 to "取消禁言",
        600 to "禁言10分钟",
        3600 to "禁言1小时",
        21600 to "禁言6小时",
        43200 to "禁言12小时",
        1 to "永久禁言"
    )
    
    AlertDialog(
        onDismissRequest = if (!isLoading) onDismiss else { {} },
        title = { Text("禁言 $userName") },
        text = {
            Column {
                Text("选择禁言时长：", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                gagOptions.forEach { (gagTime, label) ->
                    TextButton(
                        onClick = {
                            onConfirm(gagTime)
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("取消")
            }
        }
    )
}