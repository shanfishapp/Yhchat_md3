package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * WebSocket消息基类
 */
data class WebSocketMessage(
    @SerializedName("seq")
    val seq: String,
    
    @SerializedName("cmd")
    val cmd: String,
    
    @SerializedName("data")
    val data: Map<String, Any>? = null
)

/**
 * 登录消息
 */
data class LoginMessage(
    @SerializedName("seq")
    val seq: String,
    
    @SerializedName("cmd")
    val cmd: String = "login",
    
    @SerializedName("data")
    val data: WebSocketLoginData
)

/**
 * WebSocket登录数据
 */
data class WebSocketLoginData(
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("token")
    val token: String,
    
    @SerializedName("platform")
    val platform: String = "android",
    
    @SerializedName("deviceId")
    val deviceId: String
)

/**
 * 心跳消息
 */
data class HeartbeatMessage(
    @SerializedName("seq")
    val seq: String,
    
    @SerializedName("cmd")
    val cmd: String = "heartbeat",
    
    @SerializedName("data")
    val data: Map<String, Any> = emptyMap()
)

/**
 * 心跳确认消息
 */
data class HeartbeatAckMessage(
    @SerializedName("seq")
    val seq: String,
    
    @SerializedName("cmd")
    val cmd: String = "heartbeat_ack",
    
    @SerializedName("data")
    val data: Map<String, Any>? = null
)

/**
 * 推送消息
 */
data class PushMessage(
    @SerializedName("seq")
    val seq: String,
    
    @SerializedName("cmd")
    val cmd: String = "push_message",
    
    @SerializedName("data")
    val data: PushMessageData
)

/**
 * 推送消息数据
 */
data class PushMessageData(
    @SerializedName("msg")
    val message: Message? = null,
    
    @SerializedName("conversation")
    val conversation: Conversation? = null
)

/**
 * 编辑消息
 */
data class EditMessage(
    @SerializedName("seq")
    val seq: String,
    
    @SerializedName("cmd")
    val cmd: String = "edit_message",
    
    @SerializedName("data")
    val data: EditMessageData
)

/**
 * 编辑消息数据
 */
data class EditMessageData(
    @SerializedName("msg_id")
    val msgId: String,
    
    @SerializedName("content")
    val content: MessageContent,
    
    @SerializedName("edit_time")
    val editTime: Long
)

/**
 * 草稿输入
 */
data class DraftInput(
    @SerializedName("seq")
    val seq: String,
    
    @SerializedName("cmd")
    val cmd: String = "draft_input",
    
    @SerializedName("data")
    val data: DraftInputData
)

/**
 * 草稿输入数据
 */
data class DraftInputData(
    @SerializedName("chat_id")
    val chatId: String,
    
    @SerializedName("chat_type")
    val chatType: Int,
    
    @SerializedName("text")
    val text: String
)

/**
 * 机器人面板消息
 */
data class BotBoardMessage(
    @SerializedName("seq")
    val seq: String,
    
    @SerializedName("cmd")
    val cmd: String = "bot_board_message",
    
    @SerializedName("data")
    val data: BotBoardMessageData
)

/**
 * 机器人面板消息数据
 */
data class BotBoardMessageData(
    @SerializedName("bot_id")
    val botId: String,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("type")
    val type: Int
)
