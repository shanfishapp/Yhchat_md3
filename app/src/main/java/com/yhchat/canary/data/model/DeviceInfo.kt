package com.yhchat.canary.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 设备信息数据模型
 */
data class DeviceInfo(
    val userId: String,
    val platform: String,
    val deviceId: String,
    val loginTimestamp: Long
) {
    /**
     * 获取平台显示名称
     */
    fun getPlatformDisplayName(): String {
        return when (platform.lowercase()) {
            "android" -> "Android"
            "windows" -> "Windows"
            "web" -> "网页版"
            "ios" -> "iOS"
            "mac" -> "macOS"
            "linux" -> "Linux"
            else -> platform
        }
    }
    
    /**
     * 获取平台图标资源
     */
    fun getPlatformIcon(): ImageVector {
        return when (platform.lowercase()) {
            "android" -> Icons.Filled.Phone
            "windows" -> Icons.Filled.Computer
            "web" -> Icons.Filled.Language
            "ios" -> Icons.Filled.Phone
            "mac" -> Icons.Filled.Computer
            "linux" -> Icons.Filled.Computer
            else -> Icons.Filled.DeviceHub
        }
    }
    
    /**
     * 格式化登录时间
     */
    fun getFormattedLoginTime(): String {
        return try {
            val date = java.util.Date(loginTimestamp * 1000) // 转换为毫秒
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            formatter.format(date)
        } catch (e: Exception) {
            "未知时间"
        }
    }
}