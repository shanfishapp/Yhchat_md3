package com.yhchat.canary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * 自定义菜单项组件
 */
@Composable
fun MenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 消息上下文菜单（长按菜单）
 */
@Composable
fun MessageContextMenu(
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    position: IntOffset,
    isMyMessage: Boolean = false, // 添加参数标识是否为自己发送的消息
    modifier: Modifier = Modifier
) {
    Popup(
        offset = position, // 使用 IntOffset
        properties = PopupProperties(focusable = true),
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = modifier
                .width(180.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // 引用
                MenuItem(
                    text = "引用",
                    onClick = {
                        onReply()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = "引用",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                
                // 复制
                MenuItem(
                    text = "复制",
                    onClick = {
                        onCopy()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                
                // 转发
                MenuItem(
                    text = "转发",
                    onClick = {
                        onForward()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Forward,
                            contentDescription = "转发",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                
                // 编辑（仅自己发送的消息）
                if (isMyMessage) {
                    MenuItem(
                        text = "编辑",
                        onClick = {
                            onEdit()
                            // 不要在这里调用onDismiss()，让调用者决定何时关闭
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    
                    // 撤回（仅自己发送的消息且在撤回时间范围内）
                    MenuItem(
                        text = "撤回",
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "撤回",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}