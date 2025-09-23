package com.yhchat.canary.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.annotations.SerializedName

/**
 * 底部导航栏项配置
 */
data class NavigationItem(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("iconName")
    val iconName: String,
    
    @SerializedName("isVisible")
    val isVisible: Boolean = true,
    
    @SerializedName("order")
    val order: Int = 0
) {
    /**
     * 根据iconName获取对应的Icon
     */
    fun getIcon(): ImageVector = when (iconName) {
        "Chat" -> Icons.Filled.Chat
        "People" -> Icons.Filled.People
        "Contacts" -> Icons.Filled.Contacts
        "Search" -> Icons.Filled.Search
        "Person" -> Icons.Filled.Person
        "Explore" -> Icons.Filled.Explore
        "Notifications" -> Icons.Filled.Notifications
        "Settings" -> Icons.Filled.Settings
        else -> Icons.Filled.Home
    }
}

/**
 * 底部导航栏配置
 */
data class NavigationConfig(
    @SerializedName("items")
    val items: List<NavigationItem>
) {
    companion object {
        /**
         * 默认导航配置
         */
        fun getDefault(): NavigationConfig {
            return NavigationConfig(
                items = listOf(
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
            )
        }
        
        /**
         * 获取所有可用的导航项（包括隐藏的）
         */
        fun getAllAvailableItems(): List<NavigationItem> {
            return listOf(
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
        }
    }
    
    /**
     * 获取可见的导航项，按order排序
     */
    fun getVisibleItems(): List<NavigationItem> {
        return items.filter { it.isVisible }.sortedBy { it.order }
    }
}
