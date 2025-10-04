package com.yhchat.canary.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import com.yhchat.canary.data.model.ChatMessage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * 聊天输入栏组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "输入消息...",
    onImageClick: (() -> Unit)? = null,
    onFileClick: (() -> Unit)? = null,
    onCameraClick: (() -> Unit)? = null,
    onDraftChange: ((String) -> Unit)? = null,
    quoteMessage: ChatMessage? = null,
    onClearQuote: (() -> Unit)? = null,
    contentType: Int = 1,
    onContentTypeChange: ((Int) -> Unit)? = null
) {
    var showAttachMenu by remember { mutableStateOf(false) }
    var showMessageTypeDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // 引用消息显示
            if (quoteMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "引用 ${quoteMessage.sender.name}: ${quoteMessage.content.text ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        
                        IconButton(
                            onClick = { onClearQuote?.invoke() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "取消引用",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // 附件菜单
            AnimatedVisibility(
                visible = showAttachMenu,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                AttachmentMenu(
                    currentContentType = contentType,
                    onImageClick = {
                        onImageClick?.invoke()
                        showAttachMenu = false
                    },
                    onFileClick = {
                        onFileClick?.invoke()
                        showAttachMenu = false
                    },
                    onCameraClick = {
                        onCameraClick?.invoke()
                        showAttachMenu = false
                    },
                    onMessageTypeClick = {
                        showMessageTypeDialog = true
                        showAttachMenu = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // 消息类型选择对话框
            if (showMessageTypeDialog) {
                MessageTypeDialog(
                    currentContentType = contentType,
                    onContentTypeSelected = { type ->
                        onContentTypeChange?.invoke(type)
                        showMessageTypeDialog = false
                    },
                    onDismiss = { showMessageTypeDialog = false }
                )
            }
            
            // 主输入栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 加号按钮
                IconButton(
                    onClick = { 
                        showAttachMenu = !showAttachMenu
                        if (showAttachMenu) {
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = getContentTypeIcon(contentType),
                        contentDescription = getContentTypeDescription(contentType),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 输入框
                OutlinedTextField(
                    value = text,
                    onValueChange = { newText ->
                        onTextChange(newText)
                        onDraftChange?.invoke(newText)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp, max = 120.dp),
                    placeholder = { 
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                // 发送按钮
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSendMessage()
                        }
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (text.isNotBlank()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "发送",
                        tint = if (text.isNotBlank()) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 获取消息类型对应的图标
 */
@Composable
private fun getContentTypeIcon(contentType: Int): ImageVector {
    return when (contentType) {
        1 -> Icons.Default.TextFields  // 文本
        3 -> Icons.Default.Description  // Markdown
        8 -> Icons.Default.Html  // HTML
        else -> Icons.Default.Add
    }
}

/**
 * 获取消息类型对应的描述
 */
private fun getContentTypeDescription(contentType: Int): String {
    return when (contentType) {
        1 -> "文本"
        3 -> "Markdown"
        8 -> "HTML"
        else -> "附件"
    }
}

/**
 * 附件菜单
 */
@Composable
private fun AttachmentMenu(
    currentContentType: Int,
    onImageClick: () -> Unit,
    onFileClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMessageTypeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentMenuItem(
                    icon = Icons.Default.Image,
                    label = "图片",
                    onClick = onImageClick
                )
                
                AttachmentMenuItem(
                    icon = Icons.Default.CameraAlt,
                    label = "拍照",
                    onClick = onCameraClick
                )
                
                AttachmentMenuItem(
                    icon = Icons.Default.AttachFile,
                    label = "文件",
                    onClick = onFileClick
                )
            }
            
            // 分割线
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // 消息类型选择
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMessageTypeClick() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getContentTypeIcon(currentContentType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "消息类型: ${getContentTypeDescription(currentContentType)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "更改",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 附件菜单项
 */
@Composable
private fun AttachmentMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 消息类型选择对话框
 */
@Composable
private fun MessageTypeDialog(
    currentContentType: Int,
    onContentTypeSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "选择消息类型",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 文本消息
                RadioButtonRow(
                    icon = Icons.Default.TextFields,
                    text = "文本消息",
                    selected = currentContentType == 1,
                    onClick = { onContentTypeSelected(1) }
                )
                
                // Markdown消息
                RadioButtonRow(
                    icon = Icons.Default.Description,
                    text = "Markdown消息",
                    selected = currentContentType == 3,
                    onClick = { onContentTypeSelected(3) }
                )
                
                // HTML消息
                RadioButtonRow(
                    icon = Icons.Default.Html,
                    text = "HTML消息",
                    selected = currentContentType == 8,
                    onClick = { onContentTypeSelected(8) }
                )
            }
        }
    }
}

/**
 * 单选按钮行
 */
@Composable
private fun RadioButtonRow(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    }
}
