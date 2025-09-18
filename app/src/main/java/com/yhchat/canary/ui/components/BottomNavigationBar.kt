package com.yhchat.canary.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航栏组件
 */
@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onScreenChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Chat,
                    contentDescription = "聊天"
                )
            },
            label = {
                Text("聊天")
            },
            selected = currentScreen == "conversation",
            onClick = {
                onScreenChange("conversation")
            }
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Filled.People,
                    contentDescription = "社区"
                )
            },
            label = {
                Text("社区")
            },
            selected = currentScreen == "community",
            onClick = {
                onScreenChange("community")
            }
        )
    }
}

/**
 * 底部导航栏项数据类
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)
