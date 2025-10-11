package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 设置聊天背景请求
 */
data class SetChatBackgroundRequest(
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("chatId")
    val chatId: String,  // 如果设置全部背景则填"all"
    
    @SerializedName("url")
    val url: String  // 要设置背景的文件名+扩展名
)

/**
 * 聊天背景列表响应
 */
data class ChatBackgroundListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: ChatBackgroundListData,
    
    @SerializedName("msg")
    val msg: String
)

data class ChatBackgroundListData(
    @SerializedName("list")
    val list: List<ChatBackground>
)

/**
 * 聊天背景项
 */
data class ChatBackground(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("chatId")
    val chatId: String,  // 单个群聊或"all"
    
    @SerializedName("imgUrl")
    val imgUrl: String,  // 背景图片URL
    
    @SerializedName("createTime")
    val createTime: Long,
    
    @SerializedName("delFlag")
    val delFlag: Int = 0,
    
    @SerializedName("updateTime")
    val updateTime: Long
)

