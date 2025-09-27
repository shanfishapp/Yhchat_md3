package com.yhchat.canary.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.yhchat.canary.data.repository.UserRepository
import com.yhchat.canary.data.repository.NavigationRepository
import com.yhchat.canary.ui.settings.SettingsActivity
import com.yhchat.canary.ui.settings.NavigationSettingsActivity
import android.text.TextUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * 我的界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    userRepository: UserRepository? = null,
    tokenRepository: com.yhchat.canary.data.repository.TokenRepository? = null,
    navigationRepository: NavigationRepository? = null
) {
    val context = LocalContext.current
    val viewModel = remember {
        val repo = userRepository ?: com.yhchat.canary.data.repository.UserRepository(com.yhchat.canary.data.api.ApiClient.apiService, null)
        if (tokenRepository != null) {
            repo.setTokenRepository(tokenRepository)
        }
        ProfileViewModel(repo)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val changeInviteCodeState by viewModel.changeInviteCodeState.collectAsStateWithLifecycle()
    
    // 修改邀请码弹窗状态
    var showChangeInviteCodeDialog by remember { mutableStateOf(false) }
    
    // 在界面显示时加载用户资料
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = "我的",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(
                    onClick = {
                        SettingsActivity.start(context, navigationRepository, tokenRepository)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置"
                    )
                }
            }
        )
        
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
            
            uiState.error != null -> {
                // 错误状态
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "加载失败",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error ?: "未知错误",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
        Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadUserProfile() }
                    ) {
                        Text("重试")
                    }
                }
            }
            
            uiState.userProfile != null -> {
                // 成功状态 - 显示用户信息
                uiState.userProfile?.let { userProfile ->
                    UserProfileContent(
                        userProfile = userProfile,
                        navigationRepository = navigationRepository,
                        tokenRepository = tokenRepository,
                        viewModel = viewModel,
                        onShowChangeInviteCodeDialog = { showChangeInviteCodeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            else -> {
                // 空状态
                Text(
                    text = "暂无用户信息",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    }
    
    // 修改邀请码弹窗
    if (showChangeInviteCodeDialog) {
        ChangeInviteCodeDialog(
            currentInviteCode = uiState.userProfile?.invitationCode ?: "",
            changeInviteCodeState = changeInviteCodeState,
            onConfirm = { newCode ->
                viewModel.changeInviteCode(newCode)
            },
            onDismiss = {
                showChangeInviteCodeDialog = false
                viewModel.resetChangeInviteCodeState()
            }
        )
    }
}
@Composable
private fun UserProfileContent(
    userProfile: com.yhchat.canary.data.model.UserProfile,
    navigationRepository: NavigationRepository? = null,
    tokenRepository: com.yhchat.canary.data.repository.TokenRepository? = null,
    viewModel: ProfileViewModel? = null,
    onShowChangeInviteCodeDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 头像和姓名部分
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                        model = userProfile.avatarUrl,
                        contentDescription = "用户头像",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "默认头像",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
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
                                MaterialTheme.colorScheme.tertiary,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "VIP",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VIP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // 详细信息部分
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
        Text(
                    text = "详细信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // 手机号
                if (!TextUtils.isEmpty(userProfile.phone)) {
                    ProfileInfoItem(
                        icon = Icons.Default.Phone,
                        label = "手机号",
                        value = userProfile.phone!!
                    )
                }

                // 邮箱
                if (!TextUtils.isEmpty(userProfile.
                    email)) {
                    ProfileInfoItem(
                        icon = Icons.Default.Email,
                        label = "邮箱",
                        value = userProfile.email!!
                    )
                }

                // 云湖币
                if (userProfile.coin != null && userProfile.coin > 0) {
                    ProfileInfoItem(
                        icon = Icons.Default.AccountCircle,
                        label = "金币",
                        value = "%.2f".format(userProfile.coin)
                    )
                }

                // VIP到期时间
                if (userProfile.isVip == 1 && userProfile.vipExpiredTime != null && userProfile.vipExpiredTime > 0) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val expiredDate = Date(userProfile.vipExpiredTime * 1000) // 假设是秒级时间戳
                    ProfileInfoItem(
                        icon = Icons.Default.Star,
                        label = "VIP到期时间",
                        value = dateFormat.format(expiredDate)
                    )
                }

                // 邀请码
                if (!TextUtils.isEmpty(userProfile.invitationCode)) {
                    ProfileInfoItemWithButton(
                        icon = Icons.Default.Person,
                        label = "邀请码",
                        value = userProfile.invitationCode!!,
                        buttonText = "修改邀请码",
                        onButtonClick = onShowChangeInviteCodeDialog
                    )
                }
            }
        }
        
        // 添加底部间距，确保内容不会贴底
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProfileSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "前往",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileInfoItemWithButton(
    icon: ImageVector,
    label: String,
    value: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        OutlinedButton(
            onClick = onButtonClick,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = buttonText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * 修改邀请码弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeInviteCodeDialog(
    currentInviteCode: String,
    changeInviteCodeState: ChangeInviteCodeState,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newInviteCode by remember { mutableStateOf(currentInviteCode) }
    
    // 成功后自动关闭弹窗
    LaunchedEffect(changeInviteCodeState.isSuccess) {
        if (changeInviteCodeState.isSuccess) {
            kotlinx.coroutines.delay(1500) // 显示成功提示1.5秒
            onDismiss()
        }
    }
    
    AlertDialog(
        onDismissRequest = { if (!changeInviteCodeState.isLoading) onDismiss() },
        title = {
            Text(
                text = "修改邀请码",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "请输入新的邀请码：",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = newInviteCode,
                    onValueChange = { newInviteCode = it },
                    label = { Text("邀请码") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !changeInviteCodeState.isLoading,
                    singleLine = true,
                    isError = changeInviteCodeState.error != null
                )
                
                // 错误提示
                changeInviteCodeState.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // 成功提示
                if (changeInviteCodeState.isSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "修改成功！",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newInviteCode) },
                enabled = !changeInviteCodeState.isLoading && newInviteCode.isNotBlank() && !changeInviteCodeState.isSuccess
            ) {
                if (changeInviteCodeState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = when {
                        changeInviteCodeState.isLoading -> "修改中..."
                        changeInviteCodeState.isSuccess -> "已修改"
                        else -> "确定"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !changeInviteCodeState.isLoading
            ) {
                Text("取消")
            }
        }
    )
}
