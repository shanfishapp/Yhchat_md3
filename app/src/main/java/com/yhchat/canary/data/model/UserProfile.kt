package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用户勋章
 */
data class UserMedal(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("desc")
    val desc: String,
    @SerializedName("imageUrl")
    val imageUrl: String,
    @SerializedName("sort")
    val sort: Int
)

/**
 * 用户资料信息
 */
data class UserProfile(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,
    @SerializedName("registerTime")
    val registerTime: Long,
    @SerializedName("registerTimeText")
    val registerTimeText: String,
    @SerializedName("onLineDay")
    val onLineDay: Int,
    @SerializedName("continuousOnLineDay")
    val continuousOnLineDay: Int,
    @SerializedName("medals")
    val medals: List<UserMedal> = emptyList(),
    @SerializedName("isVip")
    val isVip: Int,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("coin")
    val coin: Double? = null,
    @SerializedName("vipExpiredTime")
    val vipExpiredTime: Long? = null,
    @SerializedName("invitationCode")
    val invitationCode: String? = null
)

/**
 * 用户主页API响应
 */
data class UserHomepageResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: UserHomepageData,
    @SerializedName("msg")
    val msg: String
)

/**
 * 用户主页数据
 */
data class UserHomepageData(
    @SerializedName("user")
    val user: UserProfile
)
