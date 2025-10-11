package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 创建分享链接请求
 */
data class CreateShareRequest(
    @SerializedName("chatId")
    val chatId: String,
    
    @SerializedName("chatType")
    val chatType: Int,  // 1-用户，2-群聊，3-机器人
    
    @SerializedName("chatName")
    val chatName: String
)

/**
 * 创建分享链接响应
 */
data class CreateShareResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: ShareData,
    
    @SerializedName("msg")
    val msg: String
)

data class ShareData(
    @SerializedName("imageKey")
    val imageKey: String,  // 图片key
    
    @SerializedName("key")
    val key: String,  // 分享链接的key
    
    @SerializedName("shareUrl")
    val shareUrl: String,  // 分享开头的url
    
    @SerializedName("ts")
    val ts: Long  // 分享链接创建时间戳
) {
    // 获取完整分享链接
    fun getFullShareUrl(): String {
        return "$shareUrl?key=$key&ts=$ts"
    }
    
    // 获取分享图片URL
    fun getShareImageUrl(): String {
        return "https://chat-img.jwznb.com/$imageKey"
    }
}

