package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 内测信息数据模型
 */
data class BetaInfo(
    @SerializedName("beta")
    val beta: String, // "allow" 或 "noapply"
    
    @SerializedName("info")
    val info: String // 内测功能介绍
) {
    /**
     * 是否为内测用户
     */
    val isBetaUser: Boolean
        get() = beta == "allow"
}

/**
 * 内测信息响应
 */
data class BetaInfoResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: BetaInfo?,
    
    @SerializedName("msg")
    val msg: String
)
