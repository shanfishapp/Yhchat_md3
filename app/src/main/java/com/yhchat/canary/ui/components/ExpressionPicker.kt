package com.yhchat.canary.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yhchat.canary.R
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
import java.io.File

data class LocalExpression(
    val name: String,
    val path: String
)

/**
 * 表情选择器（模仿Telegram风格）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressionPicker(
    onExpressionClick: (Expression) -> Unit,  // 点击表情后的回调（传递完整的Expression对象）
    onStickerClick: (StickerItem) -> Unit = {},  // 点击表情包贴纸的回调
    onLocalExpressionClick: (String) -> Unit = {}, // 点击本地表情的回调，传递格式化的表情名称
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { ExpressionPickerViewModel() }
    
    LaunchedEffect(Unit) {
        viewModel.init(context)
        viewModel.loadLocalExpressions(context)
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
            // Tab 切换（默认表情 / 我的收藏 / 表情包）
            if (uiState.stickerPacks.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 8.dp
                ) {
                    // 默认表情 tab
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("默认表情") },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) }
                    )
                    
                    // 我的收藏 tab
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("我的收藏") },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) }
                    )
                    
                    // 表情包 tabs
                    uiState.stickerPacks.forEachIndexed { index, stickerPack ->
                        val tabIndex = index + 2 // 默认表情和我的收藏各占一个tab
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
                    // 默认表情 tab
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("默认表情") },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) }
                    )
                    
                    // 我的收藏 tab
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("我的收藏") },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) }
                    )
                }
            }
            
            // 内容区域
            when {
                uiState.isLoading && selectedTab == 1 -> { // 只在"我的收藏"tab显示加载状态
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
                    // 默认表情
                    if (uiState.localExpressions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无默认表情",
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
                            items(uiState.localExpressions) { localExpression ->
                                // 从 assets/emojis/ 目录加载图片
                                val assetPath = "emojis/${localExpression.name}"
                                
                                // 使用 remember 来管理 Bitmap，避免重复加载
                                val bitmap = remember(localExpression.name) {
                                    try {
                                        val inputStream = context.assets.open(assetPath)
                                        BitmapFactory.decodeStream(inputStream)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    }
                                }

                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = localExpression.name,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clickable {
                                                // 传递格式化的表情名称，只使用文件名（不含扩展名）
                                                val fileNameWithoutExtension = localExpression.name.substringBeforeLast(".")
                                                onLocalExpressionClick("[.$fileNameWithoutExtension]")
                                                onDismiss()
                                            },
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    // 加载失败时显示占位符
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(Color.LightGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("加载失败")
                                    }
                                }
                            }
                        }
                    }
                }
                
                selectedTab == 1 -> {
                    // 我的收藏
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
                
                selectedTab >= 2 -> {
                    // 表情包内容
                    val stickerPackIndex = selectedTab - 2 // 调整索引，因为默认表情和我的收藏各占一个tab
                    val selectedStickerPack = uiState.stickerPacks.getOrNull(stickerPackIndex)
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
    
    fun loadLocalExpressions(context: Context) {
        // 加载 res/emojis 目录下的表情
        val localExpressions = mutableListOf<LocalExpression>()
        try {
            // 确定表情资源的文件名列表（直接列出已知的资源文件名）
            val emojiFileNames = listOf("1f345", "1f346", "1f347", "1f349", "1f34a", "1f34b", "1f34c", "1f34d", "1f34e", "1f34f", "1f350", "1f351", "1f352", "1f353", "1f354", "1f355", "1f35a", "1f35c", "1f36d", "1f378", "1f37b", "1f37c", "1f37f", "1f381", "1f382", "1f389", "1f40c", "1f420", "1f422", "1f423", "1f424", "1f425", "1f431", "1f437", "1f438", "1f439", "1f43a", "1f43b", "1f43b-200d-2744-fe0f", "1f43c", "1f44a", "1f44c", "1f44d", "1f44f", "1f602", "1f605", "1f60a", "1f60b", "1f60c", "1f60d", "1f60e", "1f60f", "1f611", "1f618", "1f61a", "1f61b", "1f61c", "1f61d", "1f61e", "1f61f", "1f620", "1f625", "1f62a", "1f62b", "1f62c", "1f62d", "1f62e", "1f62e-200d-1f4a8", "1f62f", "1f63a", "1f63b", "1f63c", "1f63d", "1f63e", "1f63f", "1f644", "1f64b-200d-2640-fe0f", "1f64b-200d-2642-fe0f", "1f64f", "1f913", "1f914", "1f91d", "1f923", "1f925", "1f926-200d-2640-fe0f", "1f926-200d-2642-fe0f", "1f927", "1f928", "1f929", "1f92a", "1f92b", "1f92c", "1f92d", "1f92e", "1f937-200d-2640-fe0f", "1f937-200d-2642-fe0f", "1f970", "1f973", "1f97a", "1f996", "1fae0", "1fae2", "1fae3", "1fae4", "1faf0", "270c", "27a1", "2b05", "2b06", "2b07", "31-20e3", "32-20e3", "33-20e3", "34-20e3", "35-20e3", "36-20e3", "37-20e3", "38-20e3", "39-20e3", "eeee", "tuosai")
            val pngFileNames = listOf("eeee", "tuosai")
            
            // 添加SVG格式的表情
            emojiFileNames.filter { !pngFileNames.contains(it) }.forEach { fileName ->
                localExpressions.add(LocalExpression("${fileName}.svg", fileName))
            }
            
            // 添加PNG格式的表情
            pngFileNames.forEach { fileName ->
                localExpressions.add(LocalExpression("${fileName}.png", fileName))
            }
        } catch (e: Exception) {
            android.util.Log.e("ExpressionPicker", "加载本地表情失败: ${e.message}")
        }
        
        _uiState.value = _uiState.value.copy(
            localExpressions = localExpressions
        )
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
    val localExpressions: List<LocalExpression> = emptyList(),
    val stickerPacks: List<StickerPack> = emptyList(),
    val error: String? = null
)