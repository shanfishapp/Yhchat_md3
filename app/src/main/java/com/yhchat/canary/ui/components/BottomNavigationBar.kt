package com.yhchat.canary.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.yhchat.canary.data.model.NavigationItem

/**
 * 底部导航栏组件
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

/**
 * 兼容性方法 - 保持向后兼容
 */
@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onScreenChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用默认的导航项
    val defaultItems = listOf(
        NavigationItem(
            id = "conversation",
            title = "聊天",
            iconName = "Chat",
            isVisible = true,
            order = 0
        ),
        NavigationItem(
            id = "community",
            title = "社区",
            iconName = "People",
            isVisible = true,
            order = 1
        ),
        NavigationItem(
            id = "contacts",
            title = "通讯录",
            iconName = "Contacts",
            isVisible = true,
            order = 2
        ),
        NavigationItem(
            id = "discover",
            title = "发现",
            iconName = "Search",
            isVisible = true,
            order = 3
        ),
        NavigationItem(
            id = "profile",
            title = "我的",
            iconName = "Person",
            isVisible = true,
            order = 4
        )
    )
    
    BottomNavigationBar(
        currentScreen = currentScreen,
        visibleItems = defaultItems,
        onScreenChange = onScreenChange,
        modifier = modifier
    )
}

/**
 * 底部导航栏项数据类
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)
