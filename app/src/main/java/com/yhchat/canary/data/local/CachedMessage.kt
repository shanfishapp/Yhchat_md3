package com.yhchat.canary.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 缓存的消息实体
 */
@Entity(tableName = "cached_messages")
data class CachedMessage(
    @PrimaryKey
    val msgId: String,
    val chatId: String,
    val chatType: Int,
    val senderChatId: String,
    val senderName: String,
    val senderAvatarUrl: String,
    val direction: String,
    val contentType: Int,
    val contentText: String? = null,
    val contentImageUrl: String? = null,
    val contentFileName: String? = null,
    val contentFileUrl: String? = null,
    val quoteMsgText: String? = null,
    val quoteImageUrl: String? = null,
    val sendTime: Long,
    val msgSeq: Long? = null,
    val editTime: Long? = null,
    val msgDeleteTime: Long? = null,
    val quoteMsgId: String? = null,
    val cmdName: String? = null,  // 指令名称
    val cmdType: Int? = null,  // 指令类型
    val localInsertTime: Long = System.currentTimeMillis()
)
