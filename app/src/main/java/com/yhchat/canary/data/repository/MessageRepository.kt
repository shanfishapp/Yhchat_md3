package com.yhchat.canary.data.repository

import com.yhchat.canary.data.api.*
import com.yhchat.canary.data.model.*
import java.util.UUID

/**
 * 消息数据仓库
 */
class MessageRepository {
    
    private val apiService = ApiClient.apiService
    private var tokenRepository: TokenRepository? = null
    
    fun setTokenRepository(tokenRepository: TokenRepository) {
        this.tokenRepository = tokenRepository
    }
    
    private suspend fun getToken(): String? {
        return tokenRepository?.getTokenSync()
    }
    
    /**
     * 发送消息
     */
    suspend fun sendMessage(
        token: String,
        chatId: String,
        chatType: Int,
        contentType: Int,
        content: SendMessageData,
        quoteMsgId: String? = null
    ): Result<Boolean> {
        return try {
            val request = SendMessageRequest(
                msgId = java.util.UUID.randomUUID().toString().replace("-", ""),
                chatId = chatId,
                chatType = chatType,
                contentType = contentType,
                data = content,
                quoteMsgId = quoteMsgId
            )
            val response = apiService.sendMessage(token, request)
            if (response.isSuccessful) {
                val status = response.body()
                if (status?.code == 1) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(status?.message ?: "发送消息失败"))
                }
            } else {
                Result.failure(Exception("发送消息失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 发送文本消息
     */
    suspend fun sendTextMessage(
        token: String,
        chatId: String,
        chatType: Int,
        text: String,
        quoteMsgId: String? = null
    ): Result<Boolean> {
        val content = SendMessageData(text = text)
        return sendMessage(token, chatId, chatType, MessageType.TEXT.value, content, quoteMsgId)
    }
    
    /**
     * 发送图片消息
     */
    suspend fun sendImageMessage(
        token: String,
        chatId: String,
        chatType: Int,
        imageKey: String,
        width: Long? = null,
        height: Long? = null,
        quoteMsgId: String? = null
    ): Result<Boolean> {
        val content = SendMessageData(
            image = imageKey,
            fileSize = 0L
        )
        return sendMessage(token, chatId, chatType, MessageType.IMAGE.value, content, quoteMsgId)
    }
    
    /**
     * 发送文件消息
     */
    suspend fun sendFileMessage(
        token: String,
        chatId: String,
        chatType: Int,
        fileKey: String,
        fileName: String,
        fileSize: Long,
        quoteMsgId: String? = null
    ): Result<Boolean> {
        val content = SendMessageData(
            fileKey = fileKey,
            fileName = fileName,
            fileSize = fileSize
        )
        return sendMessage(token, chatId, chatType, MessageType.FILE.value, content, quoteMsgId)
    }
    
    /**
     * 发送语音消息
     */
    suspend fun sendAudioMessage(
        token: String,
        chatId: String,
        chatType: Int,
        audioKey: String,
        audioTime: Long,
        quoteMsgId: String? = null
    ): Result<Boolean> {
        val content = SendMessageData(
            audio = audioKey,
            audioTime = audioTime
        )
        return sendMessage(token, chatId, chatType, MessageType.AUDIO.value, content, quoteMsgId)
    }
    
    /**
     * 获取消息列表
     */
    suspend fun getMessages(
        token: String,
        chatId: String,
        chatType: Int,
        msgCount: Int = 20,
        msgId: String = ""
    ): Result<List<Message>> {
        return try {
            val request = ListMessageRequest(
                msgCount = msgCount,
                msgId = msgId,
                chatType = chatType,
                chatId = chatId
            )
            val response = apiService.listMessages(token, request)
            if (response.isSuccessful) {
                val messageResponse = response.body()
                if (messageResponse?.status?.code == 1) {
                    Result.success(messageResponse.messages ?: emptyList())
                } else {
                    Result.failure(Exception(messageResponse?.status?.message ?: "获取消息失败"))
                }
            } else {
                Result.failure(Exception("获取消息失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 编辑消息
     */
    suspend fun editMessage(
        token: String,
        msgId: String,
        chatId: String,
        chatType: Int,
        contentType: Int,
        content: MessageContent,
        quoteMsgId: String? = null
    ): Result<Boolean> {
        return try {
            val request = EditMessageRequest(
                msgId = msgId,
                chatId = chatId,
                chatType = chatType,
                contentType = contentType,
                content = content,
                quoteMsgId = quoteMsgId
            )
            val response = apiService.editMessage(token, request)
            if (response.isSuccessful) {
                val status = response.body()
                if (status?.code == 1) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(status?.message ?: "编辑消息失败"))
                }
            } else {
                Result.failure(Exception("编辑消息失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 撤回消息
     */
    suspend fun recallMessage(
        token: String,
        msgId: String,
        chatId: String,
        chatType: Int
    ): Result<Boolean> {
        return try {
            val request = RecallMessageRequest(
                msgId = msgId,
                chatId = chatId,
                chatType = chatType
            )
            val response = apiService.recallMessage(token, request)
            if (response.isSuccessful) {
                val status = response.body()
                if (status?.code == 1) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(status?.message ?: "撤回消息失败"))
                }
            } else {
                Result.failure(Exception("撤回消息失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 批量撤回消息
     */
    suspend fun recallMessagesBatch(
        token: String,
        msgIds: List<String>,
        chatId: String,
        chatType: Int
    ): Result<Boolean> {
        return try {
            val request = RecallMessagesBatchRequest(
                msgIds = msgIds,
                chatId = chatId,
                chatType = chatType
            )
            val response = apiService.recallMessagesBatch(token, request)
            if (response.isSuccessful) {
                val status = response.body()
                if (status?.code == 1) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(status?.message ?: "批量撤回消息失败"))
                }
            } else {
                Result.failure(Exception("批量撤回消息失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 举报消息
     */
    suspend fun reportMessage(
        token: String,
        msgId: String,
        chatId: String,
        chatType: Int,
        userId: String,
        buttonValue: String
    ): Result<Boolean> {
        return try {
            val request = ButtonReportRequest(
                msgId = msgId,
                chatId = chatId,
                chatType = chatType,
                userId = userId,
                buttonValue = buttonValue
            )
            val response = apiService.buttonReport(token, request)
            if (response.isSuccessful) {
                val status = response.body()
                if (status?.code == 1) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(status?.message ?: "举报消息失败"))
                }
            } else {
                Result.failure(Exception("举报消息失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}