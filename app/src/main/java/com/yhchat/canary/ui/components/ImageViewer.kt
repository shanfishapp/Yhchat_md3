package com.yhchat.canary.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.math.abs

/**
 * MD3风格的图片预览器
 * 支持双指缩放、拖拽、旋转等手势操作
 * 完全使用Compose实现，无需第三方库
 */
@Composable
fun ImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 缩放和位移状态 - 支持无限缩放
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false, // 禁用点击外部关闭，避免手势冲突
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim) // 使用MD3黑色蒙版
        ) {
            // 图片显示区域
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .apply {
                        // 只对 jwznb.com 域名的图片添加 Referer
                        if (imageUrl.contains(".jwznb.com")) {
                            setHeader("Referer", "https://myapp.jwznb.com")
                            setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                        }
                    }
                    .crossfade(true)
                    .build(),
                contentDescription = "预览图片",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                        rotationZ = rotation
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures(
                            onGesture = { _, pan, zoom, rotate ->
                                // 支持无限缩放，从0.1到20倍
                                scale = (scale * zoom).coerceIn(0.1f, 100f)
                                rotation += rotate
                                
                                // 跟手拖动 - 根据缩放比例动态调整拖动范围
                                    offsetX += pan.x
                                    offsetY += pan.y
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        // 双击缩放
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                    rotation = 0f
                                } else {
                                    scale = 2f
                                }
                            },
                            onTap = {
                                if (scale <= 1f) {
                                    onDismiss()
                                }
                            }
                        )
                    },
                contentScale = ContentScale.Fit
            )
            
            // 顶部工具栏 - 使用MD3颜色
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 关闭按钮
                FilledIconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
                
                // 右侧操作按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 下载按钮
                    FilledIconButton(
                        onClick = {
                            // 直接保存图片，不检查权限
                            downloadImageToGallery(context, imageUrl)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "保存"
                        )
                    }
                    
                    // 分享按钮
                    FilledIconButton(
                        onClick = {
                            // 实现分享功能
                            shareImage(context, imageUrl)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享"
                        )
                    }
                }
            }
            
            // 底部信息栏 - 显示缩放比例
            if (scale != 1f || offsetX != 0f || offsetY != 0f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "缩放: ${String.format("%.1f", scale)}x",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 轻量级图片预览组件
 * 不支持手势操作，但加载速度更快
 * 适用于简单的图片预览场景
 */
@Composable
fun SimpleImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .clickable { onDismiss() }
        ) {
            // 使用 Coil 的 AsyncImagePainter
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .apply {
                        // 只对 jwznb.com 域名的图片添加 Referer
                        if (imageUrl.contains(".jwznb.com")) {
                            setHeader("Referer", "https://myapp.jwznb.com")
                            setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                        }
                    }
                    .crossfade(true)
                    .build()
            )
            
            when (painter.state) {
                is AsyncImagePainter.State.Loading -> {
                    // 加载中
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                is AsyncImagePainter.State.Error -> {
                    // 加载失败
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "加载失败",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "图片加载失败",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                else -> {
                    // 显示图片
                    androidx.compose.foundation.Image(
                        painter = painter,
                        contentDescription = "预览图片",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onDismiss() },
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // 关闭按钮
            FilledIconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭"
                )
            }
        }
    }
}

/**
 * 高级图片预览器
 * 支持更多手势操作和高级功能
 */
@Composable
fun AdvancedImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // 手势状态
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    // UI状态
    var showControls by remember { mutableStateOf(true) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
        ) {
            // 图片显示
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .apply {
                        // 只对 jwznb.com 域名的图片添加 Referer
                        if (imageUrl.contains(".jwznb.com")) {
                            setHeader("Referer", "https://myapp.jwznb.com")
                            setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                        }
                    }
                    .crossfade(true)
                    .build(),
                contentDescription = "预览图片",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                        rotationZ = rotation
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures(
                            onGesture = { centroid, pan, zoom, rotate ->
                                // 更精细的缩放控制
                                val newScale = (scale * zoom).coerceIn(0.3f, 5f)
                                scale = newScale
                                
                                // 旋转控制
                                rotation += rotate * 57.3f // 弧度转角度
                                if (abs(rotation) > 360f) {
                                    rotation %= 360f
                                }
                                
                                // 智能拖拽边界检测
                                val maxTranslationX = size.width * (newScale - 1) / 2
                                val maxTranslationY = size.height * (newScale - 1) / 2
                                
                                offsetX = (offsetX + pan.x).coerceIn(-maxTranslationX, maxTranslationX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxTranslationY, maxTranslationY)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                            },
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    // 重置所有变换
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                    rotation = 0f
                                } else {
                                    // 智能缩放到点击位置
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentScale = ContentScale.Fit
            )
            
            // 控制栏 - 只在需要时显示
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 关闭按钮
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // 中间信息
                        Text(
                            text = "${String.format("%.1f", scale)}x",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // 右侧操作
                        Row {
                            IconButton(
                                onClick = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                    rotation = 0f
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download, // 用作重置图标
                                    contentDescription = "重置",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    // TODO: 实现分享功能
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "分享",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            
            // 底部手势提示
            if (scale == 1f && offsetX == 0f && offsetY == 0f) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showControls,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "双击缩放 • 拖拽移动 • 旋转手势",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}


/**
 * 下载图片到相册
 */
private fun downloadImageToGallery(context: Context, imageUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 创建目标目录
                val picturesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "云湖"
                )
                if (!picturesDir.exists()) {
                    picturesDir.mkdirs()
                }
                
                // 获取文件名
                var fileName = imageUrl.substringAfterLast("/", "image_${System.currentTimeMillis()}.jpg")
                .substringBefore("?")
                
                // 确保文件名有合适的扩展名
                if (!fileName.contains(".")) {
                    fileName += ".jpg"
                }
                
                // 如果文件已存在，添加序号
                var targetFile = File(picturesDir, fileName)
                var counter = 1
                val baseName = fileName.substringBeforeLast(".")
                val extension = fileName.substringAfterLast(".", "jpg")
                while (targetFile.exists()) {
                    targetFile = File(picturesDir, "${baseName}_$counter.$extension")
                    counter++
                }
                
            // 下载图片（只对 jwznb.com 域名添加Referer头）
                val url = URL(imageUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            if (imageUrl.contains(".jwznb.com")) {
                connection.setRequestProperty("Referer", "https://myapp.jwznb.com")
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
            }
            connection.connect()
            
            connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // 通知系统媒体扫描
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(targetFile)
                context.sendBroadcast(mediaScanIntent)
                
                // 在主线程显示成功提示
                withContext(Dispatchers.Main) {
                Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * 分享图片
 */
private fun shareImage(context: Context, imageUrl: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 下载图片到缓存
            val cacheDir = context.cacheDir
            val fileName = "share_${System.currentTimeMillis()}.jpg"
            val tempFile = File(cacheDir, fileName)
            
            val url = URL(imageUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            if (imageUrl.contains(".jwznb.com")) {
                connection.setRequestProperty("Referer", "https://myapp.jwznb.com")
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
            }
            connection.connect()
            
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // 使用FileProvider获取URI
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            
            // 创建分享Intent
            withContext(Dispatchers.Main) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // 使用Intent选择器
                val chooserIntent = Intent.createChooser(shareIntent, "分享图片")
                context.startActivity(chooserIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}