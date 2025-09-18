package com.yhchat.canary.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户Token实体
 */
@Entity(tableName = "user_tokens")
data class UserToken(
    @PrimaryKey
    val id: Int = 1, // 只存储一个token
    val token: String,
    val loginTime: Long = System.currentTimeMillis()
)
