package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 会话数据模型
 */
data class Conversation(
    @SerializedName("chat_id")
    val chatId: String,
    
    @SerializedName("chat_type")
    val chatType: Int, // 1-用户，2-群聊，3-机器人
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("chat_content")
    val chatContent: String,
    
    @SerializedName("timestamp_ms")
    val timestampMs: Long, // 毫秒时间戳
    
    @SerializedName("unread_message")
    val unreadMessage: Int, // 1表示有未读消息
    
    @SerializedName("at")
    val at: Int, // 是否被@，1表示被@
    
    @SerializedName("avatar_id")
    val avatarId: Long? = null,
    
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    
    @SerializedName("do_not_disturb")
    val doNotDisturb: Int? = null, // 免打扰，1表示开启
    
    @SerializedName("timestamp")
    val timestamp: Long, // 秒级时间戳
    
    @SerializedName("at_data")
    val atData: AtData? = null,
    
    @SerializedName("certification_level")
    val certificationLevel: Int? = null // 认证，1是官方，2是地区
)

/**
 * @数据
 */
data class AtData(
    @SerializedName("unknown")
    val unknown: Long? = null,
    
    @SerializedName("mentioned_id")
    val mentionedId: String? = null,
    
    @SerializedName("mentioned_name")
    val mentionedName: String? = null,
    
    @SerializedName("mentioned_in")
    val mentionedIn: String? = null,
    
    @SerializedName("mentioner_id")
    val mentionerId: String? = null,
    
    @SerializedName("mentioner_name")
    val mentionerName: String? = null,
    
    @SerializedName("msg_seq")
    val msgSeq: Long? = null
)

/**
 * 会话列表响应
 */
data class ConversationListResponse(
    @SerializedName("status")
    val status: ApiStatus,
    
    @SerializedName("data")
    val data: List<Conversation>? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("request_id")
    val requestId: String? = null
)

/**
 * 设置已读请求
 */
data class DismissNotificationRequest(
    @SerializedName("chatId")
    val chatId: String
)

/**
 * 设置已读响应
 */
data class DismissNotificationResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("msg")
    val message: String
)
