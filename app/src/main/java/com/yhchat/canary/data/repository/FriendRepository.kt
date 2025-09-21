package com.yhchat.canary.data.repository

import com.yhchat.canary.data.api.AddFriendRequest
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.*
import javax.inject.Inject

/**
 * 好友数据仓库
 */
class FriendRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    /**
     * 申请添加好友/群聊/机器人
     */
    suspend fun applyFriend(
        token: String,
        chatId: String,
        chatType: Int, // 1-用户，2-群聊，3-机器人
        remark: String
    ): ApiStatus {
        return try {
            val request = AddFriendRequest(
                chatId = chatId,
                chatType = chatType,
                remark = remark
            )
            val response = apiService.addFriend(token, request)
            if (response.isSuccessful) {
                response.body() ?: ApiStatus(code = 0, message = "响应为空")
            } else {
                ApiStatus(code = 0, message = "网络请求失败: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiStatus(code = 0, message = "请求失败: ${e.message}")
        }
    }
}
