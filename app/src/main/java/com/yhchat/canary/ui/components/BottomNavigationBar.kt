package com.yhchat.canary.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import com.yhchat.canary.data.model.NavigationItem

/**
 * 底部导航栏组件 - 固定显示
 */
@Composable
fun BottomNavigationBar(
    currentScreen: String,
    visibleItems: List<NavigationItem>,
    onScreenChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .padding(horizontal = 0.dp) // 减小水平间距
    ) {
        visibleItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.getIcon(),
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(item.title)
                },
                selected = currentScreen == item.id,
                onClick = {
                    onScreenChange(item.id)
                }
            )
        }
    }
}
