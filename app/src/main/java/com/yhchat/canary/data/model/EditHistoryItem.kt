package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 编辑历史项数据模型
 */
data class EditHistoryItem(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("msgId")
    val msgId: String,
    
    @SerializedName("contentOld")
    val contentOld: String,
    
    @SerializedName("contentNew")
    val contentNew: String,
    
    @SerializedName("createTime")
    val createTime: Long,
    
    @SerializedName("updateTime")
    val updateTime: Long
)

/**
 * 编辑历史请求数据模型
 */
data class ListEditRequest(
    @SerializedName("msgId")
    val msgId: String,
    
    @SerializedName("size")
    val size: Int = 10,
    
    @SerializedName("page")
    val page: Int = 1
)

/**
 * 编辑历史响应数据模型
 */
data class ListEditMessage(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("msg")
    val msg: String,
    
    @SerializedName("data")
    val data: ListEditData
)

/**
 * 编辑历史数据
 */
data class ListEditData(
    @SerializedName("list")
    val list: List<EditHistoryItem>,
    
    @SerializedName("total")
    val total: Int
)