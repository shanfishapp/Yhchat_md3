package com.yhchat.canary.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.github.chrisbanes.photoview.PhotoView

/**
 * 图片预览组件 - 支持 SDK 21
 * 使用 PhotoView 实现图片缩放和拖拽功能
 */
@Composable
fun ImageViewer(
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
                .background(Color.Black)
        ) {
            // 图片显示区域
            AndroidView<PhotoView>(
                factory = { ctx ->
                    PhotoView(ctx).apply {
                        // 设置缩放参数
                        minimumScale = 1.0f
                        maximumScale = 5.0f
                        setOnClickListener {
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { photoView ->
                // 使用 Coil 加载图片到 PhotoView
                val imageRequest = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .setHeader("Referer", "https://myapp.jwznb.com")
                    .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                    .crossfade(true)
                    .target(
                        onStart = { placeholder ->
                            photoView.setImageDrawable(placeholder)
                        },
                        onSuccess = { result ->
                            photoView.setImageDrawable(result)
                        },
                        onError = { error ->
                            photoView.setImageDrawable(error)
                        }
                    )
                    .build()
                
                // 执行图片加载
                val imageLoader = coil.ImageLoader(context)
                imageLoader.enqueue(imageRequest)
            }
            
            // 顶部工具栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 关闭按钮
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
                
                // 分享按钮 (可选功能)
                IconButton(
                    onClick = {
                        // TODO: 实现分享功能
                    },
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "分享",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 轻量级图片预览组件
 * 使用 Compose 原生组件，无需第三方库
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
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            // 使用 Coil 的 AsyncImagePainter
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .setHeader("Referer", "https://myapp.jwznb.com")
                    .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
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
                            color = Color.White,
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
                        Text(
                            text = "图片加载失败",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    // 显示图片
                    androidx.compose.foundation.Image(
                        painter = painter,
                        contentDescription = "预览图片",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onDismiss() }
                    )
                }
            }
            
            // 关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        }
    }
}