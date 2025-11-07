package com.yhchat.canary.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.foundation.layout.PaddingValues
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
    onExpressionClick: ((com.yhchat.canary.data.model.Expression) -> Unit)? = null,  // 表情点击回调
    onStickerClick: ((com.yhchat.canary.data.model.StickerItem) -> Unit)? = null,  // 表情包贴纸点击回调
    onInstructionClick: ((com.yhchat.canary.data.model.Instruction) -> Unit)? = null,  // 指令点击回调
    groupId: String? = null,  // 群聊ID，用于加载指令
    selectedInstruction: com.yhchat.canary.data.model.Instruction? = null, // 选中的指令
    onClearInstruction: (() -> Unit)? = null // 清除指令
) {
    var showAttachMenu by remember { mutableStateOf(false) }
    var showExpressionPicker by remember { mutableStateOf(false) }
    var showInstructionPicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column {
        // 主输入栏容器
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            // 引用消息显示框
            if (quotedMessageText != null) {
                QuotedMessageBar(
                    quotedText = quotedMessageText,
                    onClearQuote = { onClearQuote?.invoke() }
                )
            }
            
            // 指令显示框
            if (selectedInstruction != null) {
                InstructionBar(
                    instruction = selectedInstruction,
                    onClearInstruction = { onClearInstruction?.invoke() }
                )
            }
            
            // 整体输入框背景 - 圆角矩形，包含所有按钮和输入框
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),  // 大圆角
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),  // 半透明背景
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 加号按钮
                IconButton(
                    onClick = { 
                        showAttachMenu = !showAttachMenu
                            showExpressionPicker = false
                            showInstructionPicker = false
                        if (showAttachMenu) {
                            keyboardController?.hide()
                        }
                    },
                        modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "附件",
                            tint = if (showAttachMenu) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // 输入框 - 无边框，融入背景
                    BasicTextField(
                    value = text,
                        onValueChange = { newText: String ->
                        onTextChange(newText)
                        onDraftChange?.invoke(newText)
                    },
                    modifier = Modifier
                        .weight(1f)
                            .heightIn(min = 36.dp, max = 90.dp)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 3,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (text.isEmpty()) {
                                    val effectivePlaceholder = when {
                                        selectedInstruction != null && selectedInstruction.hintText.isNotEmpty() -> 
                                            selectedInstruction.hintText
                                        else -> placeholder
                                    }
                        Text(
                                        text = effectivePlaceholder,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    
                    // 指令按钮 - 只在群聊中显示
                    if (groupId != null && onInstructionClick != null) {
                        IconButton(
                            onClick = { 
                                showInstructionPicker = !showInstructionPicker
                                showAttachMenu = false
                                showExpressionPicker = false
                                if (showInstructionPicker) {
                                    keyboardController?.hide()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Code,
                                contentDescription = "指令",
                                tint = if (showInstructionPicker) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // 表情按钮
                    IconButton(
                        onClick = { 
                            showExpressionPicker = !showExpressionPicker
                            showAttachMenu = false
                            showInstructionPicker = false
                            if (showExpressionPicker) {
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEmotions,
                            contentDescription = "表情",
                            tint = if (showExpressionPicker) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // 发送按钮 - 圆形背景
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSendMessage()
                        }
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier
                            .size(32.dp)
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
                            modifier = Modifier.size(18.dp)
                    )
                    }
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
        }  // 关闭主输入栏容器Column
        
        // 表情选择器
        if (showExpressionPicker && onExpressionClick != null) {
            ExpressionPicker(
                onExpressionClick = { expression ->
                    onExpressionClick?.invoke(expression)
                    showExpressionPicker = false
                },
                onStickerClick = { stickerItem ->
                    onStickerClick?.invoke(stickerItem)
                    showExpressionPicker = false
                },
                onDismiss = { showExpressionPicker = false }
            )
        }
        
        // 指令选择器（在Surface外面）
        if (showInstructionPicker && onInstructionClick != null && groupId != null) {
            InstructionPicker(
                groupId = groupId,
                onInstructionClick = { instruction ->
                    onInstructionClick?.invoke(instruction)
                    showInstructionPicker = false
                },
                onDismiss = { showInstructionPicker = false }
            )
        }
    }
}

/**
 * 指令显示条
 */
@Composable
fun InstructionBar(
    instruction: com.yhchat.canary.data.model.Instruction,
    onClearInstruction: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                    text = "/${instruction.name}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (instruction.desc.isNotEmpty()) {
                    Text(
                        text = instruction.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            IconButton(
                onClick = onClearInstruction,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "清除指令",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
