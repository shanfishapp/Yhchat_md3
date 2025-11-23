package com.yhchat.canary.ui.components

import android.content.Context

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
                            columns = GridCells.Fixed(10), // 修改为一行显示10个表情
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp), // 减小间距
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                            .size(40.dp) // 减小表情大小
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
                                            .size(40.dp) // 减小占位符大小
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
        // 加载 assets/emojis 目录下的表情
        val localExpressions = mutableListOf<LocalExpression>()
        try {
            // 从assets目录获取所有表情文件名
            val emojiFileNames = context.assets.list("emojis") 
            if (emojiFileNames != null && emojiFileNames.isNotEmpty()) {
                // 添加所有表情文件
                emojiFileNames.forEach { fileName ->
                    localExpressions.add(LocalExpression(fileName, fileName))
                }
                android.util.Log.d("ExpressionPicker", "成功加载 ${emojiFileNames.size} 个表情文件")
            } else {
                android.util.Log.w("ExpressionPicker", "assets/emojis 目录为空或不存在")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExpressionPicker", "加载本地表情失败: ${e.message}", e)
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