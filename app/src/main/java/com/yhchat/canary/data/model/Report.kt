package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 举报请求
 */
data class ReportRequest(
    @SerializedName("chatId")
    val chatId: String,  // 对象id
    
    @SerializedName("chatType")
    val chatType: Int,  // 对象类型，1-用户，2-群聊，3-机器人
    
    @SerializedName("chatName")
    val chatName: String,  // 对象名称
    
    @SerializedName("content")
    val content: String,  // 举报内容
    
    @SerializedName("url")
    val url: String = ""  // 举报提交的图片（可选）
)

