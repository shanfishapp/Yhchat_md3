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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
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
    selectedMessageType: Int = 1, // 1-文本, 3-Markdown, 8-HTML
    onMessageTypeChange: ((Int) -> Unit)? = null,
    quotedMessageText: String? = null, // 引用的消息文本
    onClearQuote: (() -> Unit)? = null, // 清除引用
    onExpressionClick: ((com.yhchat.canary.data.model.Expression) -> Unit)? = null  // 表情点击回调
) {
    var showAttachMenu by remember { mutableStateOf(false) }
    var showExpressionPicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shadowElevation = 4.dp,
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
        Column {
            // 引用消息显示框
            if (quotedMessageText != null) {
                QuotedMessageBar(
                    quotedText = quotedMessageText,
                    onClearQuote = { onClearQuote?.invoke() }
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
                        imageVector = Icons.Default.Add,
                        contentDescription = "附件",
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
                
                // 表情按钮
                IconButton(
                    onClick = { 
                        showExpressionPicker = !showExpressionPicker
                        showAttachMenu = false  // 关闭附件菜单
                        if (showExpressionPicker) {
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "表情",
                        tint = if (showExpressionPicker) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
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
            
            // 附件菜单（在输入框下方弹出）
            AnimatedVisibility(
                visible = showAttachMenu,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                AttachmentMenu(
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
                    onHtmlClick = {
                        onMessageTypeChange?.invoke(8)
                        showAttachMenu = false
                    },
                    onMarkdownClick = {
                        onMessageTypeChange?.invoke(3)
                        showAttachMenu = false
                    },
                    selectedMessageType = selectedMessageType,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
        
        // 表情选择器（在Surface外面）
        if (showExpressionPicker && onExpressionClick != null) {
            ExpressionPicker(
                onExpressionClick = { expression ->
                    onExpressionClick?.invoke(expression)
                    showExpressionPicker = false
                },
                onDismiss = { showExpressionPicker = false }
            )
        }
    }
}
