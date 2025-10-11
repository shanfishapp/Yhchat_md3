package com.yhchat.canary.ui.coin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.MyTaskInfo
import com.yhchat.canary.data.repository.CoinRepository
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 金币明细Activity
 */
class CoinDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            YhchatCanaryTheme {
                CoinDetailScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { CoinDetailViewModel() }
    
    LaunchedEffect(Unit) {
        viewModel.init(context)
        viewModel.loadTaskInfo()
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("金币明细", fontWeight = FontWeight.Bold) },
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
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadTaskInfo() }) {
                            Text("重试")
                        }
                    }
                }
            }
            
            uiState.taskInfo != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "任务完成情况",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    item {
                        TaskInfoCard(taskInfo = uiState.taskInfo!!)
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "获取金币的方式",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    item {
                        TaskTipCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskInfoCard(
    taskInfo: MyTaskInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TaskInfoItem(
                label = "观看广告次数",
                value = "${taskInfo.adsWatchNumber} 次"
            )
            
            HorizontalDivider()
            
            TaskInfoItem(
                label = "修改头像",
                value = if (taskInfo.avatarEditNumber > 0) "✓ 已完成" else "✗ 未完成",
                isCompleted = taskInfo.avatarEditNumber > 0
            )
            
            HorizontalDivider()
            
            TaskInfoItem(
                label = "修改昵称",
                value = if (taskInfo.nicknameEditNumber > 0) "✓ 已完成" else "✗ 未完成",
                isCompleted = taskInfo.nicknameEditNumber > 0
            )
            
            HorizontalDivider()
            
            TaskInfoItem(
                label = "抽奖次数",
                value = "${taskInfo.raffleTimes} 次"
            )
        }
    }
}

@Composable
private fun TaskInfoItem(
    label: String,
    value: String,
    isCompleted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isCompleted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun TaskTipCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💡 获取金币的方式：",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• 观看广告",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• 修改个人头像",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• 修改个人昵称",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• 参与抽奖活动",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * 金币明细ViewModel
 */
class CoinDetailViewModel : ViewModel() {
    private lateinit var coinRepository: CoinRepository
    
    private val _uiState = MutableStateFlow(CoinDetailUiState())
    val uiState: StateFlow<CoinDetailUiState> = _uiState.asStateFlow()
    
    fun init(context: android.content.Context) {
        coinRepository = RepositoryFactory.getCoinRepository(context)
    }
    
    fun loadTaskInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            coinRepository.getMyTaskInfo().fold(
                onSuccess = { taskInfo ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        taskInfo = taskInfo
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
}

data class CoinDetailUiState(
    val isLoading: Boolean = false,
    val taskInfo: MyTaskInfo? = null,
    val error: String? = null
)

