package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用户统计数据
 */
data class UserStatsResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: UserStatsData,
    @SerializedName("msg")
    val msg: String
)

data class UserStatsData(
    @SerializedName("currentNumberDay")
    val currentNumberDay: Int, // 今日注册用户数量
    @SerializedName("currentNumberTotal")
    val currentNumberTotal: Int, // 目前总用户数量
    @SerializedName("targetNumberDay")
    val targetNumberDay: Int, // 单日最高注册用户数
    @SerializedName("targetNumberTotal")
    val targetNumberTotal: Int // 注册用户总目标
)
