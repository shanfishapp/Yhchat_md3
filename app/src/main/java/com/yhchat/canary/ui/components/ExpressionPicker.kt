package com.yhchat.canary.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.Expression
import com.yhchat.canary.data.model.StickerPack
import com.yhchat.canary.data.model.StickerItem
import com.yhchat.canary.data.repository.ExpressionRepository
import com.yhchat.canary.data.repository.StickerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 表情选择器（模仿Telegram风格）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressionPicker(
    onExpressionClick: (Expression) -> Unit,  // 点击表情后的回调（传递完整的Expression对象）
    onStickerClick: (StickerItem) -> Unit = {},  // 点击表情包贴纸的回调
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { ExpressionPickerViewModel() }
    
    LaunchedEffect(Unit) {
        viewModel.init(context)
        viewModel.loadExpressions()
        viewModel.loadStickerPacks()
    }
    
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedStickerPackIndex by remember { mutableIntStateOf(0) }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab 切换（我的表情 / 表情包）
            if (uiState.stickerPacks.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 8.dp
                ) {
                    // 我的表情 tab
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("我收藏的") }
                    )
                    
                    // 表情包 tabs
                    uiState.stickerPacks.forEachIndexed { index, stickerPack ->
                        val tabIndex = index + 1
                        Tab(
                            selected = selectedTab == tabIndex,
                            onClick = { 
                                selectedTab = tabIndex
                                selectedStickerPackIndex = index
                            }
                        ) {
                            // 使用表情包第一个贴纸作为图标
                            val firstSticker = stickerPack.stickerItems.firstOrNull()
                            if (firstSticker != null) {
                                AsyncImage(
                                    model = ImageUtils.createStickerImageRequest(
                                        context = context,
                                        url = firstSticker.getFullUrl()
                                    ),
                                    contentDescription = stickerPack.name,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("📦")
                            }
                        }
                    }
                }
            } else {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("我收藏的") }
                    )
                }
            }
            
            // 内容区域
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = uiState.error ?: "加载失败",
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = { 
                                viewModel.loadExpressions()
                                viewModel.loadStickerPacks()
                            }) {
                                Text("重试")
                            }
                        }
                    }
                }
                
                selectedTab == 0 -> {
                    // 我的表情
                    if (uiState.expressions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无收藏的表情\n长按聊天中的图片添加到表情收藏",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.expressions) { expression ->
                                AsyncImage(
                                    model = expression.getFullUrl(),
                                    contentDescription = "表情",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clickable {
                                            onExpressionClick(expression)  // 传递完整的Expression对象
                                            onDismiss()
                                        },
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
                
                selectedTab > 0 -> {
                    // 表情包内容
                    val selectedStickerPack = uiState.stickerPacks.getOrNull(selectedStickerPackIndex)
                    if (selectedStickerPack != null) {
                        if (selectedStickerPack.stickerItems.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "该表情包暂无内容",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(selectedStickerPack.stickerItems) { stickerItem ->
                                    AsyncImage(
                                        model = ImageUtils.createStickerImageRequest(
                                            context = context,
                                            url = stickerItem.getFullUrl()
                                        ),
                                        contentDescription = stickerItem.name,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clickable {
                                                onStickerClick(stickerItem)
                                                onDismiss()
                                            },
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "表情包加载失败",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 表情选择器ViewModel
 */
class ExpressionPickerViewModel : ViewModel() {
    private lateinit var expressionRepository: ExpressionRepository
    private lateinit var stickerRepository: StickerRepository
    
    private val _uiState = MutableStateFlow(ExpressionPickerUiState())
    val uiState: StateFlow<ExpressionPickerUiState> = _uiState.asStateFlow()
    
    fun init(context: Context) {
        expressionRepository = RepositoryFactory.getExpressionRepository(context)
        stickerRepository = RepositoryFactory.getStickerRepository(context)
    }
    
    fun loadExpressions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            expressionRepository.getExpressionList().fold(
                onSuccess = { expressions ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        expressions = expressions
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
    
    fun loadStickerPacks() {
        viewModelScope.launch {
            stickerRepository.getStickerPackList().fold(
                onSuccess = { stickerPacks ->
                    _uiState.value = _uiState.value.copy(
                        stickerPacks = stickerPacks
                    )
                },
                onFailure = { error ->
                    // 表情包加载失败不影响个人表情的显示
                    android.util.Log.e("ExpressionPicker", "加载表情包失败: ${error.message}")
                }
            )
        }
    }
}

data class ExpressionPickerUiState(
    val isLoading: Boolean = false,
    val expressions: List<Expression> = emptyList(),
    val stickerPacks: List<StickerPack> = emptyList(),
    val error: String? = null
)

