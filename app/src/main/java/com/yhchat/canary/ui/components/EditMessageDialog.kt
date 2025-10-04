package com.yhchat.canary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
/**
 * 编辑消息对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMessageDialog(
    initialText: String,
    initialContentType: Int,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf(initialText) }
    var selectedContentType by remember { mutableStateOf(initialContentType) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 标题
                Text(
                    text = "编辑消息",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 消息类型选择
                Text(
                    text = "消息类型",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 文本消息
                RadioButtonRow(
                    icon = Icons.Default.TextFields,
                    text = "文本消息",
                    selected = selectedContentType == 1,
                    onClick = { selectedContentType = 1 }
                )
                
                // Markdown消息
                RadioButtonRow(
                    icon = Icons.Default.Description,
                    text = "Markdown消息",
                    selected = selectedContentType == 3,
                    onClick = { selectedContentType = 3 }
                )
                
                // HTML消息
                RadioButtonRow(
                    icon = Icons.Default.Html,
                    text = "HTML消息",
                    selected = selectedContentType == 8,
                    onClick = { selectedContentType = 8 }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 消息内容输入框
                Text(
                    text = "消息内容",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 200.dp),
                    placeholder = { 
                        Text(
                            text = "输入消息内容...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                onConfirm(messageText, selectedContentType)
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Text("确认")
                    }
                }
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
            .padding(vertical = 8.dp),
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