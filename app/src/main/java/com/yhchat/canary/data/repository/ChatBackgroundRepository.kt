package com.yhchat.canary.data.repository

import android.util.Log
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.ChatBackground
import com.yhchat.canary.data.model.SetChatBackgroundRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天背景仓库
 */
@Singleton
class ChatBackgroundRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenRepository: TokenRepository
) {
    companion object {
        private const val TAG = "ChatBackgroundRepository"
    }

    /**
     * 设置聊天背景
     * @param chatId 聊天ID，如果设置全部背景则填"all"
     * @param backgroundUrl 背景图片URL（文件名+扩展名）
     */
    suspend fun setChatBackground(
        chatId: String,
        backgroundUrl: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = tokenRepository.getTokenSync()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("未登录"))
            }
            
            // 获取当前用户ID
            val userId = tokenRepository.getUserIdSync() ?: ""

            val request = SetChatBackgroundRequest(
                userId = userId,
                chatId = chatId,
                url = backgroundUrl
            )
            
            val response = apiService.setChatBackground(token, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && (body.code == 1 || body.code == 200)) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("设置聊天背景失败: ${body?.message ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("设置聊天背景失败: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取背景设置列表
     */
    suspend fun getChatBackgroundList(): Result<List<ChatBackground>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = tokenRepository.getTokenSync()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("未登录"))
            }
            
            val response = apiService.getChatBackgroundList(token)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && (body.code == 1 || body.code == 200)) {
                    Result.success(body.data.list)
                } else {
                    Result.failure(Exception("获取背景列表失败: ${body?.msg ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("获取背景列表失败: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 根据chatId获取对应的背景
     */
    suspend fun getBackgroundForChat(chatId: String): Result<String?> = withContext(Dispatchers.IO) {
        return@withContext try {
            getChatBackgroundList().fold(
                onSuccess = { backgrounds ->
                    // 优先查找特定聊天的背景
                    val specificBg = backgrounds.find { it.chatId == chatId }
                    if (specificBg != null) {
                        Result.success(specificBg.imgUrl)
                    } else {
                        // 查找全局背景
                        val globalBg = backgrounds.find { it.chatId == "all" }
                        Result.success(globalBg?.imgUrl)
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

