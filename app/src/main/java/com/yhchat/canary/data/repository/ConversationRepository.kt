package com.yhchat.canary.data.repository

import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.*
import com.yhchat.canary.data.protobuf.ConversationProtoParser
import javax.inject.Inject

/**
 * 会话数据仓库
 */
class ConversationRepository @Inject constructor(
    private val apiService: ApiService
) {

    private var tokenRepository: TokenRepository? = null

    fun setTokenRepository(tokenRepository: TokenRepository) {
        this.tokenRepository = tokenRepository
    }
    
    private suspend fun getToken(): String? {
        return tokenRepository?.getTokenSync()
    }
    
    /**
     * 获取会话列表
     */
    suspend fun getConversations(): Result<List<Conversation>> {
        return try {
            val token = getToken() ?: return Result.failure(Exception("未登录"))
            val response = apiService.listConversations(token)
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    val protoBytes = responseBody.bytes()
                    ConversationProtoParser.parseConversationList(protoBytes)
                } else {
                    Result.failure(Exception("响应体为空"))
                }
            } else {
                Result.failure(Exception("获取会话列表失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 设置会话为已读
     */
    suspend fun dismissNotification(chatId: String): Result<Boolean> {
        return try {
            val token = getToken() ?: return Result.failure(Exception("未登录"))
            val request = DismissNotificationRequest(chatId = chatId)
            val response = apiService.dismissNotification(token, request)
            if (response.isSuccessful) {
                val dismissResponse = response.body()
                if (dismissResponse?.code == 1) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(dismissResponse?.message ?: "设置已读失败"))
                }
            } else {
                Result.failure(Exception("设置已读失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
