package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用户信息数据模型
 */
data class User(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    
    @SerializedName("avatar_id")
    val avatarId: Long? = null,
    
    @SerializedName("chat_id")
    val chatId: String,
    
    @SerializedName("chat_type")
    val chatType: Int, // 1-用户，2-群聊，3-机器人
    
    @SerializedName("tag")
    val tags: List<Tag>? = null,
    
    @SerializedName("certification_level")
    val certificationLevel: Int? = null // 1-官方，2-地区
)

/**
 * 用户标签
 */
data class Tag(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("text")
    val text: String,
    
    @SerializedName("color")
    val color: String
)

/**
 * 登录请求
 */
data class LoginRequest(
    @SerializedName("mobile")
    val mobile: String? = null,
    
    @SerializedName("captcha")
    val captcha: String? = null,
    
    @SerializedName("email")
    val email: String? = null,
    
    @SerializedName("password")
    val password: String? = null,
    
    @SerializedName("deviceId")
    val deviceId: String,
    
    @SerializedName("platform")
    val platform: String = "android"
)

/**
 * 登录响应
 */
data class LoginResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("msg")
    val message: String,
    
    @SerializedName("data")
    val data: LoginData? = null
)

/**
 * 登录数据
 */
data class LoginData(
    @SerializedName("token")
    val token: String
)

/**
 * 验证码响应
 */
data class CaptchaResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("msg")
    val message: String,
    
    @SerializedName("data")
    val data: CaptchaData? = null
)

/**
 * 验证码数据
 */
data class CaptchaData(
    @SerializedName("b64s")
    val b64s: String,

    @SerializedName("id")
    val id: String
)

/**
 * 获取短信验证码请求
 */
data class SmsCaptchaRequest(
    @SerializedName("mobile")
    val mobile: String,

    @SerializedName("code")
    val code: String,

    @SerializedName("id")
    val id: String
)

/**
 * 搜索请求
 */
data class SearchRequest(
    @SerializedName("word")
    val word: String
)

/**
 * 搜索响应
 */
data class SearchResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: SearchData,
    
    @SerializedName("msg")
    val msg: String
)

/**
 * 搜索数据
 */
data class SearchData(
    @SerializedName("list")
    val list: List<SearchCategory>
)

/**
 * 搜索类别
 */
data class SearchCategory(
    @SerializedName("title")
    val title: String,

    @SerializedName("list")
    val list: List<SearchItem>? = null
)

/**
 * 搜索项
 */
data class SearchItem(
    @SerializedName("friendId")
    val friendId: String,

    @SerializedName("friendType")
    val friendType: Int,

    @SerializedName("nickname")
    val nickname: String,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("avatarUrl")
    val avatarUrl: String,

    @SerializedName("hit")
    val hit: Int
)

/**
 * 置顶会话响应
 */
data class StickyResponse(
    @SerializedName("code")
    val code: Int,

    @SerializedName("msg")
    val message: String,

    @SerializedName("data")
    val data: StickyData? = null
)

/**
 * 置顶会话数据
 */
data class StickyData(
    @SerializedName("sticky")
    val sticky: List<StickyItem>
)

/**
 * 置顶会话项
 */
data class StickyItem(
    @SerializedName("id")
    val id: Int,

    @SerializedName("chatType")
    val chatType: Int,

    @SerializedName("chatId")
    val chatId: String,

    @SerializedName("chatName")
    val chatName: String,

    @SerializedName("sort")
    val sort: Long,

    @SerializedName("avatarUrl")
    val avatarUrl: String,

    @SerializedName("createTime")
    val createTime: Long,

    @SerializedName("delFlag")
    val delFlag: Int,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("certificationLevel")
    val certificationLevel: Int
)

/**
 * 置顶会话操作请求
 */
data class StickyOperationRequest(
    @SerializedName("chatId")
    val chatId: String,

    @SerializedName("chatType")
    val chatType: Int
)

/**
 * 置顶会话置顶请求
 */
data class StickyTopRequest(
    @SerializedName("id")
    val id: Int
)

// ========== 聊天消息相关数据模型 ==========

/**
 * 消息列表响应
 */
data class MessageListResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val message: String,
    @SerializedName("data")
    val data: MessageListData? = null
)

/**
 * 消息列表数据
 */
data class MessageListData(
    @SerializedName("status")
    val status: MessageStatus? = null,
    @SerializedName("msg")
    val messages: List<ChatMessage>? = null,
    @SerializedName("total")
    val total: Int? = null
)

/**
 * 消息状态
 */
data class MessageStatus(
    @SerializedName("number")
    val number: Long? = null,
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val message: String
)

/**
 * 聊天消息
 */
data class ChatMessage(
    @SerializedName("msg_id")
    val msgId: String,
    @SerializedName("sender")
    val sender: MessageSender,
    @SerializedName("direction")
    val direction: String, // left/right
    @SerializedName("content_type")
    val contentType: Int,
    @SerializedName("content")
    val content: MessageContent,
    @SerializedName("send_time")
    val sendTime: Long, // 毫秒时间戳
    @SerializedName("cmd")
    val cmd: MessageCmd? = null,
    @SerializedName("msg_delete_time")
    val msgDeleteTime: Long? = null,
    @SerializedName("quote_msg_id")
    val quoteMsgId: String? = null,
    @SerializedName("msg_seq")
    val msgSeq: Long? = null,
    @SerializedName("edit_time")
    val editTime: Long? = null
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
    val avatarUrl: String,
    @SerializedName("tag_old")
    val tagOld: List<String>? = null,
    @SerializedName("tag")
    val tag: List<MessageTag>? = null
)

/**
 * 消息标签
 */
data class MessageTag(
    @SerializedName("id")
    val id: Long,
    @SerializedName("text")
    val text: String,
    @SerializedName("color")
    val color: String
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
    @SerializedName("quote_image_url")
    val quoteImageUrl: String? = null,
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
 * 消息命令
 */
data class MessageCmd(
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: Int
)

/**
 * 获取消息列表请求（通过序列）
 */
data class ListMessageBySeqRequest(
    @SerializedName("msg_seq")
    val msgSeq: Long,
    @SerializedName("chat_type") 
    val chatType: Int,
    @SerializedName("chat_id")
    val chatId: String
)

/**
 * 获取消息列表请求
 */
data class ListMessageRequest(
    @SerializedName("msg_count")
    val msgCount: Int,
    @SerializedName("msg_id")
    val msgId: String? = null,
    @SerializedName("chat_type")
    val chatType: Int,
    @SerializedName("chat_id")
    val chatId: String
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
    @SerializedName("content")
    val content: SendMessageContent,
    @SerializedName("content_type")
    val contentType: Int,
    @SerializedName("command_id")
    val commandId: Long? = null,
    @SerializedName("quote_msg_id")
    val quoteMsgId: String? = null,
    @SerializedName("media")
    val media: MessageMedia? = null
)

/**
 * 发送消息内容
 */
data class SendMessageContent(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("buttons")
    val buttons: String? = null,
    @SerializedName("file_name")
    val fileName: String? = null,
    @SerializedName("file")
    val file: String? = null,
    @SerializedName("mentioned_id")
    val mentionedId: List<String>? = null,
    @SerializedName("form")
    val form: String? = null,
    @SerializedName("quote_msg_text")
    val quoteMsgText: String? = null,
    @SerializedName("image")
    val image: String? = null,
    @SerializedName("msg_text1")
    val msgText1: String? = null,
    @SerializedName("msg_text2")
    val msgText2: String? = null,
    @SerializedName("expression_id")
    val expressionId: String? = null,
    @SerializedName("file_size")
    val fileSize: Long? = null,
    @SerializedName("video")
    val video: String? = null,
    @SerializedName("audio")
    val audio: String? = null,
    @SerializedName("audio_time")
    val audioTime: Long? = null,
    @SerializedName("sticker_item_id")
    val stickerItemId: Long? = null,
    @SerializedName("sticker_pack_id")
    val stickerPackId: Long? = null,
    @SerializedName("room_name")
    val roomName: String? = null
)

/**
 * 消息媒体
 */
data class MessageMedia(
    @SerializedName("file_key")
    val fileKey: String,
    @SerializedName("file_hash")
    val fileHash: String,
    @SerializedName("file_type")
    val fileType: String,
    @SerializedName("image_height")
    val imageHeight: Long? = null,
    @SerializedName("image_width")
    val imageWidth: Long? = null,
    @SerializedName("file_size")
    val fileSize: Long,
    @SerializedName("file_key2")
    val fileKey2: String,
    @SerializedName("file_suffix")
    val fileSuffix: String
)

/**
 * API 通用状态响应
 */
data class ApiStatus(
    @SerializedName("number")
    val number: Long? = null,
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val message: String
)

/**
 * 撤回消息请求
 */
data class RecallMessageRequest(
    @SerializedName("msg_id")
    val msgId: String,
    @SerializedName("chat_id")
    val chatId: String,
    @SerializedName("chat_type")
    val chatType: Int
)

/**
 * 批量撤回消息请求
 */
data class RecallMessageBatchRequest(
    @SerializedName("msg_id")
    val msgIds: List<String>,
    @SerializedName("chat_id")
    val chatId: String,
    @SerializedName("chat_type")
    val chatType: Int
)

/**
 * 聊天类型枚举
 */
enum class ChatType(val value: Int) {
    USER(1),
    GROUP(2),
    BOT(3)
}

/**
 * 当前用户个人信息
 */
data class CurrentUserProfile(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    
    @SerializedName("avatar_id")
    val avatarId: Long? = null,
    
    @SerializedName("phone")
    val phone: String? = null,
    
    @SerializedName("email")
    val email: String? = null,
    
    @SerializedName("coin")
    val coin: Double? = null,
    
    @SerializedName("is_vip")
    val isVip: Int? = null,
    
    @SerializedName("vip_expired_time")
    val vipExpiredTime: Long? = null,
    
    @SerializedName("invitation_code")
    val invitationCode: String? = null
)

/**
 * 用户信息API响应
 */
data class UserInfoResponse(
    @SerializedName("number")
    val number: Long? = null,
    
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("msg")
    val message: String,
    
    @SerializedName("data")
    val data: UserProfile? = null
)

/**
 * 更改密码请求
 */
data class ChangePasswordRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("captcha")
    val captcha: String,
    
    @SerializedName("password")
    val password: String
)

/**
 * 邮箱验证码请求
 */
data class EmailVerificationRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("code")
    val code: String,
    
    @SerializedName("id")
    val id: String
)

/**
 * 机器人信息
 * 基于 yhapi/v1/bot.md 的 bot-info 接口
 */
data class BotInfo(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("bot_id")
    val botId: String? = null,
    @SerializedName("name") // bot.md中使用name字段



    val nickname: String? = null,
    @SerializedName("name_id") // bot.md中使用name_id字段
    val nicknameId: Int? = null,
    @SerializedName("avatar_id")
    val avatarId: Int? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("link")
    val link: String? = null,
    @SerializedName("introduction")
    val introduction: String? = null,
    @SerializedName("create_by")
    val createBy: String? = null,
    @SerializedName("create_time")
    val createTime: Long? = null,
    @SerializedName("headcount")
    val headcount: Int? = null,
    @SerializedName("private")
    val private: Int? = null,
    @SerializedName("is_stop")
    val isStop: Int? = null, // 是否停用，0-启用，1-停用
    @SerializedName("always_agree")
    val alwaysAgree: Int? = null, // 自动进群，0-不自动进群，1-自动进群
    @SerializedName("do_not_disturb")
    val doNotDisturb: Int? = null, // 免打扰，0-不免打扰，1-免打扰
    @SerializedName("top")
    val top: Int? = null, // 置顶，0-未置顶，1-已置顶
    @SerializedName("group_limit")
    val groupLimit: Int? = null // 限制进群，0-允许进群，1-限制进群
)

/**
 * 机器人信息响应
 */
data class BotInfoResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: BotInfoData,
    @SerializedName("msg")
    val msg: String
)

/**
 * 机器人信息数据
 */
data class BotInfoData(
    @SerializedName("bot")
    val bot: BotInfo
)

/**
 * Web用户信息响应
 */
data class WebUserInfoResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: WebUserInfoData,
    @SerializedName("msg")
    val msg: String
)

/**
 * Web用户信息数据
 */
data class WebUserInfoData(
    @SerializedName("user")
    val user: WebUserDetailInfo
)

/**
 * Web用户详细信息
 */
data class WebUserDetailInfo(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("avatar_url")
    val avatarUrl: String,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("create_time")
    val createTime: Long,
    @SerializedName("last_login_time")
    val lastLoginTime: Long? = null,
    @SerializedName("status")
    val status: Int
)

/**
 * 群聊信息
 */
data class GroupInfo(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("introduction")
    val introduction: String? = null,
    @SerializedName("create_by")
    val createBy: String? = null,
    @SerializedName("create_time")
    val createTime: Long? = null,
    @SerializedName("avatar_id")
    val avatarId: Int? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("headcount")
    val headcount: Int? = null,
    @SerializedName("read_history")
    val readHistory: Int? = null,
    @SerializedName("category")
    val category: String? = null
)

/**
 * 群聊信息响应
 */
data class GroupInfoResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: GroupInfoData,
    @SerializedName("msg")
    val msg: String
)

/**
 * 群聊信息数据
 */
data class GroupInfoData(
    @SerializedName("group")
    val group: GroupInfo
)

/**
 * 用户信息 (用于搜索)
 */
data class UserInfo(
    @SerializedName("user_id")
    val userId: String? = null,
    @SerializedName("nickname")
    val nickname: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("register_time")
    val registerTime: Long? = null,
    @SerializedName("register_time_text")
    val registerTimeText: String? = null,
    @SerializedName("on_line_day")
    val onLineDay: Int? = null,
    @SerializedName("continuous_on_line_day")
    val continuousOnLineDay: Int? = null,
    @SerializedName("is_vip")
    val isVip: Int? = null
)

