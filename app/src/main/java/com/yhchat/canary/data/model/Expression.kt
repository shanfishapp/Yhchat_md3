package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 表情收藏响应
 */
data class ExpressionListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: ExpressionListData,
    
    @SerializedName("msg")
    val msg: String
)

data class ExpressionListData(
    @SerializedName("expression")
    val expression: List<Expression>
)

/**
 * 表情项
 */
data class Expression(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("url")
    val url: String,  // 需要前面加上 https://chat-img.jwznb.com/
    
    @SerializedName("urlOriginal")
    val urlOriginal: String,  // 来源URL
    
    @SerializedName("delFlag")
    val delFlag: Int = 0,
    
    @SerializedName("createTime")
    val createTime: Long,  // 创建时间戳
    
    @SerializedName("createBy")
    val createBy: String  // 创建者ID
) {
    // 获取完整URL
    fun getFullUrl(): String {
        return if (url.startsWith("http")) url else "https://chat-img.jwznb.com/$url"
    }
}

/**
 * 添加/删除/置顶表情请求
 */
data class ExpressionActionRequest(
    @SerializedName("id")
    val id: Long? = null,  // 表情ID（删除、置顶时使用）
    
    @SerializedName("url")
    val url: String? = null  // 图片URL（添加时使用）
)

