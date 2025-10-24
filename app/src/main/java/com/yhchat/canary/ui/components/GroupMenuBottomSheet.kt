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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true  // 完全展开，不显示部分展开状态
    )
    var showReportDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showExitGroupDialog by remember { mutableStateOf(false) }
    
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
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 退出群聊
            GroupMenuItem(
                icon = Icons.Default.ExitToApp,
                text = "退出群聊",
                onClick = {
                    showExitGroupDialog = true
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
    
    // 退出群聊确认对话框
    if (showExitGroupDialog) {
        ExitGroupDialog(
            groupName = groupName,
            onConfirm = {
                showExitGroupDialog = false
                onDismiss()
                // 调用退出群聊API
                val db = com.yhchat.canary.data.local.AppDatabase.getDatabase(context)
                val tokenRepository = com.yhchat.canary.data.repository.TokenRepository(db.userTokenDao(), context)
                val userRepository = com.yhchat.canary.data.repository.UserRepository(
                    com.yhchat.canary.data.api.ApiClient.apiService,
                    tokenRepository
                )
                CoroutineScope(Dispatchers.Main).launch {
                    userRepository.deleteFriend(groupId, 2).fold(
                        onSuccess = {
                            Toast.makeText(context, "已退出群聊", Toast.LENGTH_SHORT).show()
                            // 返回到主界面
                            if (context is android.app.Activity) {
                                context.finish()
                            }
                        },
                        onFailure = { exception: Throwable ->
                            Toast.makeText(context, "退出群聊失败: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            onDismiss = { showExitGroupDialog = false }
        )
    }
}

/**
 * 退出群聊确认对话框
 */
@Composable
fun ExitGroupDialog(
    groupName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "退出群聊",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("确定要退出群聊「$groupName」吗？")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("退出", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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

