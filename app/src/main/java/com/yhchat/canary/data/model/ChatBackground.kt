package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 聊天背景设置
 */
data class ChatBackground(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("chatId")
    val chatId: String,
    
    @SerializedName("imgUrl")
    val imgUrl: String,
    
    @SerializedName("createTime")
    val createTime: Long,
    
    @SerializedName("delFlag")
    val delFlag: Int,
    
    @SerializedName("updateTime")
    val updateTime: Long
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

/**
 * 聊天背景列表数据
 */
data class ChatBackgroundListData(
    @SerializedName("list")
    val list: List<ChatBackground>
)