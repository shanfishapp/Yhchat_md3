package com.yhchat.canary.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yhchat.canary.ui.disk.GroupDiskActivity

/**
 * 群聊菜单BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMenuBottomSheet(
    groupId: String,
    groupName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var showReportDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            // 标题
            Text(
                text = "群聊选项",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 举报群聊
            GroupMenuItem(
                icon = Icons.Default.Report,
                text = "举报群聊",
                onClick = {
                    showReportDialog = true
                }
            )
            
            // 设置聊天背景
            GroupMenuItem(
                icon = Icons.Default.Wallpaper,
                text = "设置聊天背景",
                onClick = {
                    com.yhchat.canary.ui.background.ChatBackgroundActivity.start(
                        context,
                        groupId,
                        groupName
                    )
                    onDismiss()
                }
            )
            
            // 群网盘
            GroupMenuItem(
                icon = Icons.Default.Folder,
                text = "群网盘",
                onClick = {
                    GroupDiskActivity.start(context, groupId, groupName)
                    onDismiss()
                }
            )
            
            // 邀请好友
            GroupMenuItem(
                icon = Icons.Default.PersonAdd,
                text = "邀请好友",
                onClick = {
                    showInviteDialog = true
                }
            )
            
            // 分享群聊
            GroupMenuItem(
                icon = Icons.Default.Share,
                text = "分享群聊",
                onClick = {
                    showShareDialog = true
                }
            )
            
            // 群聊详情
            GroupMenuItem(
                icon = Icons.Default.Info,
                text = "群聊详情",
                onClick = {
                    onDismiss()
                    val intent = Intent(context, com.yhchat.canary.ui.group.GroupInfoActivity::class.java)
                    intent.putExtra(com.yhchat.canary.ui.group.GroupInfoActivity.EXTRA_GROUP_ID, groupId)
                    intent.putExtra(com.yhchat.canary.ui.group.GroupInfoActivity.EXTRA_GROUP_NAME, groupName)
                    context.startActivity(intent)
                }
            )
            
            // 群聊设置
            GroupMenuItem(
                icon = Icons.Default.Settings,
                text = "群聊设置",
                onClick = {
                    onDismiss()
                    val intent = Intent(context, com.yhchat.canary.ui.group.GroupSettingsActivity::class.java)
                    intent.putExtra(com.yhchat.canary.ui.group.GroupSettingsActivity.EXTRA_GROUP_ID, groupId)
                    intent.putExtra(com.yhchat.canary.ui.group.GroupSettingsActivity.EXTRA_GROUP_NAME, groupName)
                    context.startActivity(intent)
                }
            )
            
            // 底部间距
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // 举报对话框
    if (showReportDialog) {
        ReportDialog(
            chatId = groupId,
            chatType = 2,  // 群聊
            chatName = groupName,
            onDismiss = { showReportDialog = false },
            onSuccess = {
                showReportDialog = false
                onDismiss()
                Toast.makeText(context, "举报已提交", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // 分享对话框
    if (showShareDialog) {
        ShareDialog(
            chatId = groupId,
            chatType = 2,  // 群聊
            chatName = groupName,
            onDismiss = { showShareDialog = false }
        )
    }
    
    // 邀请好友对话框
    if (showInviteDialog) {
        InviteToGroupDialog(
            groupId = groupId,
            groupName = groupName,
            onDismiss = { showInviteDialog = false },
            onSuccess = {
                showInviteDialog = false
                Toast.makeText(context, "邀请成功", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * 群聊菜单项
 */
@Composable
fun GroupMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

