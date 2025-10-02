package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 分享请求数据类
 */
data class ShareRequest(
    @SerializedName("chatId")
    val chatId: String,
    
    @SerializedName("chatType")
    val chatType: Int,
    
    @SerializedName("chatName")
    val chatName: String
)

/**
 * 分享响应数据类
 */
data class ShareResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: ShareData?,
    
    @SerializedName("msg")
    val msg: String
)

/**
 * 分享数据
 */
data class ShareData(
    @SerializedName("imageKey")
    val imageKey: String,
    
    @SerializedName("key")
    val key: String,
    
    @SerializedName("shareUrl")
    val shareUrl: String,
    
    @SerializedName("ts")
    val ts: Long
)