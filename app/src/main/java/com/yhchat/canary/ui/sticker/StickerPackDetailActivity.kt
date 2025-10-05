package com.yhchat.canary.ui.sticker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class StickerPackDetailActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
        
        fun start(context: Context, stickerPackId: String) {
            val intent = Intent(context, StickerPackDetailActivity::class.java)
            intent.putExtra(EXTRA_STICKER_PACK_ID, stickerPackId)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val stickerPackId = intent.getStringExtra(EXTRA_STICKER_PACK_ID) ?: ""
        
        setContent {
            YhchatCanaryTheme {
                StickerPackDetailScreen(
                    stickerPackId = stickerPackId,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPackDetailScreen(
    stickerPackId: String,
    onBackClick: () -> Unit
) {
    val viewModel: StickerPackDetailViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(stickerPackId) {
        viewModel.loadStickerPackDetail(stickerPackId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("表情包详情") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (uiState.stickerPack != null) {
                        IconButton(
                            onClick = { viewModel.addStickerPackToFavorites(stickerPackId) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加表情包"
                            )
                        }
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
                    Text(
                        text = uiState.error ?: "加载失败",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.stickerPack != null -> {
                    StickerPackDetailContent(
                        stickerPack = uiState.stickerPack!!
                    )
                }
            }
        }
    }
}

@Composable
fun StickerPackDetailContent(
    stickerPack: com.yhchat.canary.data.model.StickerPackDetail
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 表情包信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stickerPack.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 创建者信息
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = stickerPack.creator.avatarUrl,
                        contentDescription = "创建者头像",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stickerPack.creator.nickname,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "创建者",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 统计信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${stickerPack.stickerItems.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "表情数量",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column {
                        Text(
                            text = "${stickerPack.userCount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "使用人数",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        Text(
                            text = dateFormat.format(Date(stickerPack.createTime * 1000)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "创建时间",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 表情列表标题
        Text(
            text = "表情列表",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 表情网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stickerPack.stickerItems) { sticker ->
                StickerItemView(sticker = sticker)
            }
        }
    }
}

@Composable
fun StickerItemView(
    sticker: com.yhchat.canary.data.model.StickerItem
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable {
                // TODO: 可以添加预览或其他功能
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = "https://chat-img.jwznb.com/${sticker.url}",
                contentDescription = sticker.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

