package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 消息数据模型
 */
data class Message(
    @SerializedName("msg_id")
    val msgId: String,
    
    @SerializedName("sender")
    val sender: MessageSender,
    
    @SerializedName("direction")
    val direction: String, // "left" 或 "right"
    
    @SerializedName("content_type")
    val contentType: Int, // 1-文本，2-图片，3-markdown，4-文件，5-表单，6-文章，7-表情，8-html，11-语音，13-语音通话
    
    @SerializedName("content")
    val content: MessageContent,
    
    @SerializedName("send_time")
    val sendTime: Long, // 毫秒时间戳
    
    @SerializedName("cmd")
    val cmd: MessageCmd? = null,
    
    @SerializedName("msg_delete_time")
    val msgDeleteTime: Long? = null, // 消息撤回时间
    
    @SerializedName("quote_msg_id")
    val quoteMsgId: String? = null, // 引用消息ID
    
    @SerializedName("msg_seq")
    val msgSeq: Long,
    
    @SerializedName("edit_time")
    val editTime: Long? = null // 最后编辑时间
)

/**
 * 消息发送者
 */
data class MessageSender(
    @SerializedName("chat_id")
    val chatId: String,
    
    @SerializedName("chat_type")
    val chatType: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    
    @SerializedName("tag_old")
    val tagOld: List<String>? = null,
    
    @SerializedName("tag")
    val tags: List<Tag>? = null
)

/**
 * 消息内容
 */
data class MessageContent(
    @SerializedName("text")
    val text: String? = null,
    
    @SerializedName("buttons")
    val buttons: String? = null,
    
    @SerializedName("image_url")
    val imageUrl: String? = null,
    
    @SerializedName("file_name")
    val fileName: String? = null,
    
    @SerializedName("file_url")
    val fileUrl: String? = null,
    
    @SerializedName("form")
    val form: String? = null,
    
    @SerializedName("quote_msg_text")
    val quoteMsgText: String? = null,
    
    @SerializedName("sticker_url")
    val stickerUrl: String? = null,
    
    @SerializedName("post_id")
    val postId: String? = null,
    
    @SerializedName("post_title")
    val postTitle: String? = null,
    
    @SerializedName("post_content")
    val postContent: String? = null,
    
    @SerializedName("post_content_type")
    val postContentType: String? = null,
    
    @SerializedName("expression_id")
    val expressionId: String? = null,
    
    @SerializedName("file_size")
    val fileSize: Long? = null,
    
    @SerializedName("video_url")
    val videoUrl: String? = null,
    
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    
    @SerializedName("audio_time")
    val audioTime: Long? = null,
    
    @SerializedName("sticker_item_id")
    val stickerItemId: Long? = null,
    
    @SerializedName("sticker_pack_id")
    val stickerPackId: Long? = null,
    
    @SerializedName("call_text")
    val callText: String? = null,
    
    @SerializedName("call_status_text")
    val callStatusText: String? = null,
    
    @SerializedName("width")
    val width: Long? = null,
    
    @SerializedName("height")
    val height: Long? = null
)

/**
 * 消息指令
 */
data class MessageCmd(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("type")
    val type: Int
)

/**
 * 发送消息请求
 */
data class SendMessageRequest(
    @SerializedName("msg_id")
    val msgId: String,
    
    @SerializedName("chat_id")
    val chatId: String,
    
    @SerializedName("chat_type")
    val chatType: Int,
    
    @SerializedName("content_type")
    val contentType: Int,
    
    @SerializedName("data")
    val data: SendMessageData,
    
    @SerializedName("quote_msg_id")
    val quoteMsgId: String? = null
)

/**
 * 发送消息数据
 */
data class SendMessageData(
    @SerializedName("text")
    val text: String? = null,
    
    @SerializedName("buttons")
    val buttons: String? = null,
    
    @SerializedName("file_name")
    val fileName: String? = null,
    
    @SerializedName("file_key")
    val fileKey: String? = null,
    
    @SerializedName("mentioned_id")
    val mentionedId: List<String>? = null,
    
    @SerializedName("form")
    val form: String? = null,
    
    @SerializedName("quote_msg_text")
    val quoteMsgText: String? = null,
    
    @SerializedName("image")
    val image: String? = null,
    
    @SerializedName("post_id")
    val postId: String? = null,
    
    @SerializedName("post_title")
    val postTitle: String? = null,
    
    @SerializedName("post_content")
    val postContent: String? = null,
    
    @SerializedName("post_type")
    val postType: String? = null,
    
    @SerializedName("temp_text2")
    val tempText2: String? = null,
    
    @SerializedName("temp_text3")
    val tempText3: String? = null,
    
    @SerializedName("file_size")
    val fileSize: Long? = null,
    
    @SerializedName("video")
    val video: String? = null,
    
    @SerializedName("audio")
    val audio: String? = null,
    
    @SerializedName("audio_time")
    val audioTime: Long? = null,
    
    @SerializedName("temp_text4")
    val tempText4: String? = null,
    
    @SerializedName("temp_code1")
    val tempCode1: Long? = null,
    
    @SerializedName("sticker_item_id")
    val stickerItemId: Long? = null,
    
    @SerializedName("sticker_pack_id")
    val stickerPackId: Long? = null,
    
    @SerializedName("room_name")
    val roomName: String? = null
)

/**
 * 消息列表响应
 */
data class MessageListResponse(
    @SerializedName("status")
    val status: ApiStatus,
    
    @SerializedName("msg")
    val messages: List<Message>? = null,
    
    @SerializedName("total")
    val total: Int? = null
)

/**
 * API状态
 */
data class ApiStatus(
    @SerializedName("number")
    val number: Long,
    
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("msg")
    val message: String
)

/**
 * 消息类型枚举
 */
enum class MessageType(val value: Int) {
    TEXT(1),
    IMAGE(2),
    MARKDOWN(3),
    FILE(4),
    FORM(5),
    ARTICLE(6),
    STICKER(7),
    HTML(8),
    AUDIO(11),
    VOICE_CALL(13)
}

/**
 * 聊天类型枚举
 */
enum class ChatType(val value: Int) {
    USER(1),
    GROUP(2),
    BOT(3)
}
