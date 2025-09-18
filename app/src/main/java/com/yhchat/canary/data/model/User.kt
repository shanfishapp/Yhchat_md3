package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用户信息数据模型
 */
data class User(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    
    @SerializedName("avatar_id")
    val avatarId: Long? = null,
    
    @SerializedName("chat_id")
    val chatId: String,
    
    @SerializedName("chat_type")
    val chatType: Int, // 1-用户，2-群聊，3-机器人
    
    @SerializedName("tag")
    val tags: List<Tag>? = null,
    
    @SerializedName("certification_level")
    val certificationLevel: Int? = null // 1-官方，2-地区
)

/**
 * 用户标签
 */
data class Tag(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("text")
    val text: String,
    
    @SerializedName("color")
    val color: String
)

/**
 * 登录请求
 */
data class LoginRequest(
    @SerializedName("mobile")
    val mobile: String? = null,
    
    @SerializedName("captcha")
    val captcha: String? = null,
    
    @SerializedName("email")
    val email: String? = null,
    
    @SerializedName("password")
    val password: String? = null,
    
    @SerializedName("deviceId")
    val deviceId: String,
    
    @SerializedName("platform")
    val platform: String = "android"
)

/**
 * 登录响应
 */
data class LoginResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("msg")
    val message: String,
    
    @SerializedName("data")
    val data: LoginData? = null
)

/**
 * 登录数据
 */
data class LoginData(
    @SerializedName("token")
    val token: String
)

/**
 * 验证码响应
 */
data class CaptchaResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("msg")
    val message: String,
    
    @SerializedName("data")
    val data: CaptchaData? = null
)

/**
 * 验证码数据
 */
data class CaptchaData(
    @SerializedName("captcha_key")
    val captchaKey: String,
    
    @SerializedName("captcha_image")
    val captchaImage: String
)
