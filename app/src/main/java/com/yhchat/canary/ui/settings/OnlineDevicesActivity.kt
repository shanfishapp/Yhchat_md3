package com.yhchat.canary.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yhchat.canary.ui.base.BaseActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.DeviceInfo
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 在线设备Activity
 */
@AndroidEntryPoint
class OnlineDevicesActivity : BaseActivity() {
    
    companion object {
        private const val EXTRA_HAS_TOKEN_REPO = "has_token_repo"
        
        /**
         * 启动在线设备Activity
         */
        fun start(context: Context, tokenRepository: TokenRepository) {
            val intent = Intent(context, OnlineDevicesActivity::class.java).apply {
                putExtra(EXTRA_HAS_TOKEN_REPO, true)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val hasTokenRepo = intent.getBooleanExtra(EXTRA_HAS_TOKEN_REPO, false)
        val tokenRepository = if (hasTokenRepo) {
            RepositoryFactory.getTokenRepository(this)
        } else null
        
        setContent {
            YhchatCanaryTheme {
                val deviceViewModel: DeviceViewModel = viewModel()
                
                OnlineDevicesScreen(
                    deviceViewModel = deviceViewModel,
                    tokenRepository = tokenRepository,
                    onBack = { finish() }
                )
            }
        }
    }
}

/**
 * 在线设备界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineDevicesScreen(
    deviceViewModel: DeviceViewModel,
    tokenRepository: TokenRepository?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceState by deviceViewModel.deviceState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // 获取 token
    var token by remember { mutableStateOf("") }
    
    LaunchedEffect(tokenRepository) {
        tokenRepository?.let { tokenRepo ->
            deviceViewModel.setTokenRepository(tokenRepo)
            deviceViewModel.loadOnlineDevices()
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "在线设备",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            deviceViewModel.loadOnlineDevices()
                        },
                        enabled = !deviceState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新"
                        )
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
            // 错误提示
            deviceState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // 设备列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deviceState.devices) { device ->
                    DeviceItem(device = device)
                }
                
                // 空状态
                if (deviceState.devices.isEmpty() && !deviceState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeviceHub,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "暂无在线设备",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // 加载状态
                if (deviceState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
    
    // 错误对话框
    deviceState.error?.let { error ->
        AlertDialog(
            onDismissRequest = {
                deviceViewModel.clearError()
            },
            title = { Text("加载失败") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = {
                    deviceViewModel.clearError()
                }) {
                    Text("确定")
                }
            }
        )
    }
}

/**
 * 设备列表项
 */
@Composable
fun DeviceItem(
    device: DeviceInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 平台图标
            Icon(
                imageVector = device.getPlatformIcon(),
                contentDescription = device.platform,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // 平台名称
                Text(
                    text = device.getPlatformDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 设备ID
                Text(
                    text = "设备ID: ${device.deviceId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 登录时间
                Text(
                    text = "登录时间: ${device.getFormattedLoginTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}