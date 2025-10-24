package com.yhchat.canary.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yhchat.canary.R
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import com.yhchat.canary.utils.UnifiedLinkHandler
import com.yhchat.canary.ui.components.ImageViewer

/**
 * 应用详情 Activity
 */
class AppInfoActivity : ComponentActivity() {
    
    companion object {
        // 应用版本信息（开发者可以在这里修改）
        const val APP_VERSION = "Canary 19.6"
        const val APP_NAME = "Yhchat Canary"
        const val DEVELOPER_NAME_1 = "Kauid323"
        const val DEVELOPER_NAME_2 = "那狗吧"
        const val DEVELOPER_URL_1 = "https://github.com/Kauid323/"
        const val DEVELOPER_URL_2 = "yunhu://chat-add?id=8516939&type=user"
        const val GITHUB_REPO_URL = "https://github.com/Kauid323/Yhchat_md3"
        
        fun start(context: Context) {
            val intent = Intent(context, AppInfoActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YhchatCanaryTheme {
                AppInfoScreen(
                    onBack = { finish() },
                    onDeveloperClick = { url ->
                        handleDeveloperClick(url)
                    },
                    onGithubClick = {
                        openUrl(GITHUB_REPO_URL)
                    }
                )
            }
        }
    }
    
    /**
     * 处理开发者点击
     */
    private fun handleDeveloperClick(url: String) {
        if (url.startsWith("yunhu://")) {
            // 使用统一链接处理器处理内链
            UnifiedLinkHandler.handleLink(this, url)
        } else {
            // 打开外部链接
            openUrl(url)
        }
    }
    
    /**
     * 打开 URL
     */
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 应用详情界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppInfoScreen(
    onBack: () -> Unit,
    onDeveloperClick: (String) -> Unit,
    onGithubClick: () -> Unit
) {
    var showImageViewer by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf("") }
    
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "应用详情",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 应用图标
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(R.mipmap.ic_launcher)
                    .crossfade(true)
                    .build(),
                contentDescription = "应用图标",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable {
                        // 应用图标暂不支持预览（是mipmap资源）
                    },
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 应用名称
            Text(
                text = AppInfoActivity.APP_NAME,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 版本号
            Text(
                text = "版本 ${AppInfoActivity.APP_VERSION}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 分隔线
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 开发者信息
            AppInfoItem(
                icon = Icons.Default.Person,
                title = "开发者",
                content = {
                    DeveloperText(
                        developer1 = AppInfoActivity.DEVELOPER_NAME_1,
                        developer2 = AppInfoActivity.DEVELOPER_NAME_2,
                        onDeveloper1Click = { onDeveloperClick(AppInfoActivity.DEVELOPER_URL_1) },
                        onDeveloper2Click = { onDeveloperClick(AppInfoActivity.DEVELOPER_URL_2) }
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // GitHub 源代码
            AppInfoItem(
                icon = Icons.Default.Code,
                title = "GitHub 源代码",
                onClick = onGithubClick,
                showArrow = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 许可证
            AppInfoItem(
                icon = Icons.Default.Description,
                title = "许可证",
                content = {
                    Text(
                        text = "MIT License",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 底部说明文本
            Text(
                text = "一个云湖第三方客户端，由 AI 强力驱动",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
    }
}

/**
 * 应用信息项组件
 */
@Composable
private fun AppInfoItem(
    icon: ImageVector,
    title: String,
    content: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showArrow: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            if (content != null) {
                Spacer(modifier = Modifier.height(4.dp))
                content()
            }
        }
        
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 可点击的开发者文本组件
 */
@Composable
private fun DeveloperText(
    developer1: String,
    developer2: String,
    onDeveloper1Click: () -> Unit,
    onDeveloper2Click: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = developer1,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onDeveloper1Click)
        )
        
        Text(
            text = " / ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = developer2,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onDeveloper2Click)
        )
    }
}

