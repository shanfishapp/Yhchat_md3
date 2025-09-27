package com.yhchat.canary.data.repository

import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.Conversation
import com.yhchat.canary.data.model.DismissNotificationRequest
import com.yhchat.canary.data.model.RemoveConversationRequest
import com.yhchat.canary.data.protobuf.ConversationProtoParser
import android.util.Log
import javax.inject.Inject

/**
 * 会话数据仓库
 */
class ConversationRepository @Inject constructor(
    private val apiService: ApiService,
    private val cacheRepository: CacheRepository
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
    
    /**
     * 删除会话
     */
    suspend fun removeConversation(chatId: String): Result<Boolean> {
        return try {
            val token = getToken() ?: return Result.failure(Exception("未登录"))
            val request = RemoveConversationRequest(chatId = chatId)
            val response = apiService.removeConversation(token, request)
            if (response.isSuccessful) {
                val removeResponse = response.body()
                if (removeResponse?.code == 1) {
                    // 从缓存中删除会话
                    try {
                        cacheRepository.deleteConversationFromCache(chatId)
                    } catch (e: Exception) {
                        Log.w("ConversationRepository", "Failed to delete conversation from cache", e)
                    }
                    Result.success(true)
                } else {
                    Result.failure(Exception(removeResponse?.message ?: "删除会话失败"))
                }
            } else {
                Result.failure(Exception("删除会话失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新会话的最后消息信息
     */
    suspend fun updateLastMessage(
        chatId: String,
        chatType: Int,
        lastMessage: String,
        lastMessageTime: Long,
        unreadCount: Int?
    ) {
        try {
            cacheRepository.updateConversationLastMessage(chatId, lastMessage, lastMessageTime)
        } catch (e: Exception) {
            // 日志记录错误，但不抛出异常
            Log.e("ConversationRepository", "Error updating last message", e)
        }
    }
}
