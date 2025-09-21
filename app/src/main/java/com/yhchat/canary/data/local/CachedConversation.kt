package com.yhchat.canary.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 缓存的会话实体
 */
@Entity(tableName = "cached_conversations")
data class CachedConversation(
    @PrimaryKey
    val chatId: String,
    val chatType: Int,
    val name: String,
    val chatContent: String,
    val timestampMs: Long,
    val timestamp: Long,
    val unreadMessage: Int,
    val at: Int,
    val avatarUrl: String? = null,
    val doNotDisturb: Int? = null,
    val certificationLevel: Int? = null,
    val lastUpdateTime: Long = System.currentTimeMillis()
)
