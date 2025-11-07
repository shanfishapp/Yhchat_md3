package com.yhchat.canary.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
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
import com.yhchat.canary.ui.components.ScrollBehavior
import com.yhchat.canary.ui.components.HandleScrollBehavior

/**
 * 我的界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    userRepository: UserRepository? = null,
    tokenRepository: com.yhchat.canary.data.repository.TokenRepository? = null,
    navigationRepository: NavigationRepository? = null,
    scrollBehavior: ScrollBehavior? = null
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
    val changeNicknameState by viewModel.changeNicknameState.collectAsStateWithLifecycle()
    val changeAvatarState by viewModel.changeAvatarState.collectAsStateWithLifecycle()
    val betaState by viewModel.betaState.collectAsStateWithLifecycle()
    
    // 修改邀请码弹窗状态
    var showChangeInviteCodeDialog by remember { mutableStateOf(false) }
    // 修改用户名称弹窗状态
    var showChangeNicknameDialog by remember { mutableStateOf(false) }
    // 图片选择器状态
    var showImagePicker by remember { mutableStateOf(false) }
    
    // 图片选择器
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { 
            viewModel.changeAvatar(context, it)
        }
    }
    
    // 在界面显示时加载用户资料
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }
    
    // 监听头像修改状态
    LaunchedEffect(changeAvatarState.isSuccess) {
        if (changeAvatarState.isSuccess) {
            android.widget.Toast.makeText(context, "头像修改成功", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetChangeAvatarState()
        }
    }
    
    LaunchedEffect(changeAvatarState.error) {
        changeAvatarState.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetChangeAvatarState()
        }
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
        
        // 连接滚动行为到底部导航栏的显示/隐藏
        scrollBehavior?.let { behavior ->
            scrollState.HandleScrollBehavior(scrollBehavior = behavior)
        }
        
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
                        onShowChangeNicknameDialog = { showChangeNicknameDialog = true },
                        imagePickerLauncher = imagePickerLauncher,
                        changeAvatarState = changeAvatarState,
                        betaState = betaState,
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
    
    // 修改用户名称弹窗
    if (showChangeNicknameDialog) {
        ChangeNicknameDialog(
            currentNickname = uiState.userProfile?.nickname ?: "",
            changeNicknameState = changeNicknameState,
            onConfirm = { newNickname ->
                viewModel.changeNickname(newNickname)
            },
            onDismiss = {
                showChangeNicknameDialog = false
                viewModel.resetChangeNicknameState()
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
    onShowChangeNicknameDialog: () -> Unit = {},
    imagePickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    changeAvatarState: ChangeAvatarState,
    betaState: BetaState,
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
                // 头像（可点击修改）
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { 
                            imagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
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
                    
                    // 修改头像指示器
                    Surface(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "修改头像",
                            modifier = Modifier
                                .size(16.dp)
                                .padding(4.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    // 上传中指示器
                    if (changeAvatarState.isLoading) {
                        Surface(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 用户名（带编辑按钮）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = userProfile.nickname,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onShowChangeNicknameDialog,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "修改用户名",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 用户ID
                Text(
                    text = "ID: ${userProfile.userId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // VIP 标识和内测标识
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    // VIP 标识
                    if (userProfile.isVip == 1) {
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
                    
                    // 内测标识
                    if (betaState.betaInfo?.isBetaUser == true) {
                        var showBetaInfo by remember { mutableStateOf(false) }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { showBetaInfo = true }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "内测用户",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "内测",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // 内测信息弹窗
                        if (showBetaInfo) {
                            BetaInfoDialog(
                                betaInfo = betaState.betaInfo!!,
                                onDismiss = { showBetaInfo = false }
                            )
                        }
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

                // 云湖币（可点击）
                if (userProfile.coin != null) {
                    var showCoinMenu by remember { mutableStateOf(false) }
                    
                    ProfileInfoItemClickable(
                        icon = Icons.Default.AccountCircle,
                        label = "金币",
                        value = "%.2f".format(userProfile.coin),
                        onClick = {
                            showCoinMenu = true
                        }
                    )
                    
                    if (showCoinMenu) {
                        CoinMenuBottomSheet(
                            onDismiss = { showCoinMenu = false }
                        )
                    }
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

/**
 * 金币菜单底部弹窗（MD3风格）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoinMenuBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "金币功能",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            // 金币商城
            CoinMenuItem(
                icon = Icons.Default.Star,
                label = "金币商城",
                onClick = {
                    val intent = android.content.Intent(context, com.yhchat.canary.ui.coin.CoinShopActivity::class.java)
                    context.startActivity(intent)
                    onDismiss()
                }
            )
            
            // 任务中心
            CoinMenuItem(
                icon = Icons.Default.AccountCircle,
                label = "任务中心",
                onClick = {
                    val intent = android.content.Intent(context, com.yhchat.canary.ui.coin.CoinDetailActivity::class.java)
                    context.startActivity(intent)
                    onDismiss()
                }
            )
            
            // 金币明细
            CoinMenuItem(
                icon = Icons.Default.ArrowForward,
                label = "金币明细",
                onClick = {
                    val intent = android.content.Intent(context, com.yhchat.canary.ui.coin.CoinRecordActivity::class.java)
                    context.startActivity(intent)
                    onDismiss()
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CoinMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 可点击的资料项（带右箭头）
 */
@Composable
private fun ProfileInfoItemClickable(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "查看详情",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 32.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
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

/**
 * 修改用户名称弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeNicknameDialog(
    currentNickname: String,
    changeNicknameState: ChangeNicknameState,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newNickname by remember { mutableStateOf(currentNickname) }
    
    // 成功后自动关闭弹窗
    LaunchedEffect(changeNicknameState.isSuccess) {
        if (changeNicknameState.isSuccess) {
            kotlinx.coroutines.delay(1500) // 显示成功提示1.5秒
            onDismiss()
        }
    }
    
    AlertDialog(
        onDismissRequest = { if (!changeNicknameState.isLoading) onDismiss() },
        title = {
            Text(
                text = "修改用户名",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "请输入新的用户名：",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = newNickname,
                    onValueChange = { newNickname = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !changeNicknameState.isLoading,
                    singleLine = true,
                    isError = changeNicknameState.error != null
                )
                
                // 错误提示
                changeNicknameState.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // 成功提示
                if (changeNicknameState.isSuccess) {
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
                onClick = { onConfirm(newNickname) },
                enabled = !changeNicknameState.isLoading && newNickname.isNotBlank() && !changeNicknameState.isSuccess
            ) {
                if (changeNicknameState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = when {
                        changeNicknameState.isLoading -> "修改中..."
                        changeNicknameState.isSuccess -> "已修改"
                        else -> "确定"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !changeNicknameState.isLoading
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 内测信息弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BetaInfoDialog(
    betaInfo: com.yhchat.canary.data.model.BetaInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "内测",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "内测功能",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "恭喜您成为云湖内测用户！",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = betaInfo.info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("我知道了")
            }
        }
    )
}
