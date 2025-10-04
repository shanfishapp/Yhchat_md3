package com.yhchat.canary.data.repository

import android.util.Log
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.*
import com.yhchat.canary.proto.*
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenRepository: TokenRepository,
    private val cacheRepository: CacheRepository
) {
    private val tag = "MessageRepository"
    
    /**
     * 获取消息列表
     */
    suspend fun getMessages(
        chatId: String,
        chatType: Int,
        msgCount: Int = 20,
        msgId: String? = null
    ): Result<List<ChatMessage>> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "Token is null or empty")
                return Result.failure(Exception("用户未登录"))
            }

            // 构建protobuf请求
            val requestBuilder = list_message_send.newBuilder()
                .setChatId(chatId)
                .setChatType(chatType.toLong())
                .setMsgCount(msgCount.toLong())
            
            if (!msgId.isNullOrEmpty()) {
                requestBuilder.setMsgId(msgId)
            }
            
            val request = requestBuilder.build()
            val requestBody = request.toByteArray().toRequestBody("application/x-protobuf".toMediaType())

            Log.d(tag, "Getting messages for chat: $chatId, type: $chatType, count: $msgCount")
            
            val response = apiService.listMessage(token, requestBody)
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val bytes = responseBody.bytes()
                    val messageResponse = list_message.parseFrom(bytes)
                    
                    if (messageResponse.status.code == 1) {
                        val messages = messageResponse.msgList.map { protoMsg ->
                            convertProtoToMessage(protoMsg)
                        }
                        Log.d(tag, "Successfully got ${messages.size} messages")
                        Result.success(messages)
                } else {
                        Log.e(tag, "API error: ${messageResponse.status.msg}")
                        Result.failure(Exception(messageResponse.status.msg))
                }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Log.e(tag, "HTTP error: ${response.code()}")
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting messages", e)
            Result.failure(e)
        }
    }
    
    /**
     * 通过序列获取消息列表
     */
    suspend fun getMessagesBySeq(
        chatId: String,
        chatType: Int,
        msgSeq: Long = 0
    ): Result<List<ChatMessage>> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "Token is null or empty")
                return Result.failure(Exception("用户未登录"))
            }

            // 构建protobuf请求
            val request = list_message_by_seq_send.newBuilder()
                .setChatId(chatId)
                .setChatType(chatType.toLong())
                .setMsgSeq(msgSeq)
                .build()
            
            val requestBody = request.toByteArray().toRequestBody("application/x-protobuf".toMediaType())

            Log.d(tag, "Getting messages by seq for chat: $chatId, type: $chatType, seq: $msgSeq")
            
            val response = apiService.listMessageBySeq(token, requestBody)
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val bytes = responseBody.bytes()
                    val messageResponse = list_message_by_seq.parseFrom(bytes)
                    
                    if (messageResponse.status.code == 1) {
                        val messages = messageResponse.msgList.map { protoMsg ->
                            convertProtoToMessage(protoMsg)
                        }
                        Log.d(tag, "Successfully got ${messages.size} messages by seq")
                        Result.success(messages)
                } else {
                        Log.e(tag, "API error: ${messageResponse.status.msg}")
                        Result.failure(Exception(messageResponse.status.msg))
                }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Log.e(tag, "HTTP error: ${response.code()}")
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting messages by seq", e)
            Result.failure(e)
        }
    }
    
    /**
     * 发送消息
     */
    suspend fun sendMessage(
        chatId: String,
        chatType: Int,
        text: String,
        contentType: Int = 1, // 1-文本
        quoteMsgId: String? = null,
        mentionedUserIds: List<String> = emptyList()
    ): Result<Boolean> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "Token is null or empty")
                return Result.failure(Exception("用户未登录"))
            }

            val msgId = UUID.randomUUID().toString().replace("-", "")
            
            // 构建protobuf请求
            val contentBuilder = send_message_send.Content.newBuilder()
                .setText(text)
            
            // 添加被@用户的ID
            mentionedUserIds.forEach { userId ->
                contentBuilder.addMentionedId(userId)
            }
            
            // 如果有引用消息，添加引用消息文本
            if (!quoteMsgId.isNullOrEmpty()) {
                // 获取引用消息的文本内容
                val quoteMessage = getMessageById(quoteMsgId)
                quoteMessage?.let { msg ->
                    val quoteText = "${msg.sender.name}: ${msg.content.text}"
                    contentBuilder.setQuoteMsgText(quoteText)
                }
            }
            
            val requestBuilder = send_message_send.newBuilder()
                .setMsgId(msgId)
                .setChatId(chatId)
                .setChatType(chatType.toLong())
                .setContent(contentBuilder.build())
                .setContentType(contentType.toLong())
            
            if (!quoteMsgId.isNullOrEmpty()) {
                requestBuilder.setQuoteMsgId(quoteMsgId)
            }
            
            val request = requestBuilder.build()
            val requestBody = request.toByteArray().toRequestBody("application/x-protobuf".toMediaType())

            Log.d(tag, "Sending message to chat: $chatId, type: $chatType, text: $text, mentionedUserIds: $mentionedUserIds")
            
            val response = apiService.sendMessage(token, requestBody)
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val bytes = responseBody.bytes()
                    val sendResponse = send_message.parseFrom(bytes)
                    
                    if (sendResponse.status.code == 1) {
                        Log.d(tag, "Message sent successfully")
                    Result.success(true)
                } else {
                        Log.e(tag, "Send message error: ${sendResponse.status.msg}")
                        Result.failure(Exception(sendResponse.status.msg))
                }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Log.e(tag, "HTTP error: ${response.code()}")
                Result.failure(Exception("发送失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error sending message", e)
            Result.failure(e)
        }
    }
    
    /**
     * 编辑消息
     */
    suspend fun editMessage(
        msgId: String,
        chatId: String,
        chatType: Int,
        text: String,
        contentType: Int = 1, // 1-文本
        quoteMsgId: String? = null,
        mentionedUserIds: List<String> = emptyList()
    ): Result<Boolean> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "Token is null or empty")
                return Result.failure(Exception("用户未登录"))
            }
            
            // 构建protobuf请求
            val contentBuilder = send_message_send.Content.newBuilder()
                .setText(text)
            
            // 添加被@用户的ID
            mentionedUserIds.forEach { userId ->
                contentBuilder.addMentionedId(userId)
            }
            
            // 如果有引用消息，添加引用消息文本
            if (!quoteMsgId.isNullOrEmpty()) {
                // 获取引用消息的文本内容
                val quoteMessage = getMessageById(quoteMsgId)
                quoteMessage?.let { msg ->
                    val quoteText = "${msg.sender.name}: ${msg.content.text}"
                    contentBuilder.setQuoteMsgText(quoteText)
                }
            }
            
            val requestBuilder = send_message_send.newBuilder()
                .setMsgId(msgId) // 使用实际的消息ID而不是生成新的UUID
                .setChatId(chatId)
                .setChatType(chatType.toLong())
                .setContent(contentBuilder.build())
                .setContentType(contentType.toLong())
            
            if (!quoteMsgId.isNullOrEmpty()) {
                requestBuilder.setQuoteMsgId(quoteMsgId)
            }
            
            val request = requestBuilder.build()
            val requestBody = request.toByteArray().toRequestBody("application/x-protobuf".toMediaType())

            Log.d(tag, "Editing message: $msgId, chat: $chatId, type: $chatType, text: $text, mentionedUserIds: $mentionedUserIds")
            
            val response = apiService.editMessage(token, requestBody)
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val bytes = responseBody.bytes()
                    val sendResponse = send_message.parseFrom(bytes)
                    
                    if (sendResponse.status.code == 1) {
                        Log.d(tag, "Message edited successfully")
                    Result.success(true)
                } else {
                        Log.e(tag, "Edit message error: ${sendResponse.status.msg}")
                        Result.failure(Exception(sendResponse.status.msg))
                }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Log.e(tag, "HTTP error: ${response.code()}")
                Result.failure(Exception("编辑失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error editing message", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取编辑历史
     */
    suspend fun getEditHistory(
        msgId: String,
        size: Int = 10,
        page: Int = 1
    ): Result<List<EditHistoryItem>> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "Token is null or empty")
                return Result.failure(Exception("用户未登录"))
            }

            val request = ListEditRequest(
                msgId = msgId,
                size = size,
                page = page
            )

            Log.d(tag, "Getting edit history for message: $msgId")
            
            val response = apiService.getEditList(token, request)
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    if (responseBody.code == 1) {
                        Log.d(tag, "Successfully got ${responseBody.data.list.size} edit history items")
                        Result.success(responseBody.data.list)
                    } else {
                        Log.e(tag, "API error: ${responseBody.msg}")
                        Result.failure(Exception(responseBody.msg))
                    }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Log.e(tag, "HTTP error: ${response.code()}")
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting edit history", e)
            Result.failure(e)
        }
    }
    
    /**
     * 将Proto消息转换为应用内消息模型
     */
    private fun convertProtoToMessage(protoMsg: Msg): ChatMessage {
        val sender = MessageSender(
            chatId = protoMsg.sender.chatId,
            chatType = protoMsg.sender.chatType,
            name = protoMsg.sender.name,
            avatarUrl = protoMsg.sender.avatarUrl,
            tagOld = protoMsg.sender.tagOldList,
            tag = protoMsg.sender.tagList.map { tag ->
                MessageTag(
                    id = tag.id,
                    text = tag.text,
                    color = tag.color
                )
            }
        )

        val content = MessageContent(
            text = if (protoMsg.content.text.isNotEmpty()) protoMsg.content.text else null,
            buttons = if (protoMsg.content.buttons.isNotEmpty()) protoMsg.content.buttons else null,
            imageUrl = if (protoMsg.content.imageUrl.isNotEmpty()) protoMsg.content.imageUrl else null,
            fileName = if (protoMsg.content.fileName.isNotEmpty()) protoMsg.content.fileName else null,
            fileUrl = if (protoMsg.content.fileUrl.isNotEmpty()) protoMsg.content.fileUrl else null,
            form = if (protoMsg.content.form.isNotEmpty()) protoMsg.content.form else null,
            quoteMsgText = if (protoMsg.content.quoteMsgText.isNotEmpty()) protoMsg.content.quoteMsgText else null,
            quoteImageUrl = null, // Proto中可能没有这个字段，暂时设为null
            stickerUrl = if (protoMsg.content.stickerUrl.isNotEmpty()) protoMsg.content.stickerUrl else null,
            postId = if (protoMsg.content.postId.isNotEmpty()) protoMsg.content.postId else null,
            postTitle = if (protoMsg.content.postTitle.isNotEmpty()) protoMsg.content.postTitle else null,
            postContent = if (protoMsg.content.postContent.isNotEmpty()) protoMsg.content.postContent else null,
            postContentType = if (protoMsg.content.postContentType.isNotEmpty()) protoMsg.content.postContentType else null,
            expressionId = if (protoMsg.content.expressionId.isNotEmpty()) protoMsg.content.expressionId else null,
            fileSize = if (protoMsg.content.fileSize > 0) protoMsg.content.fileSize else null,
            videoUrl = if (protoMsg.content.videoUrl.isNotEmpty()) protoMsg.content.videoUrl else null,
            audioUrl = if (protoMsg.content.audioUrl.isNotEmpty()) protoMsg.content.audioUrl else null,
            audioTime = if (protoMsg.content.audioTime > 0) protoMsg.content.audioTime else null,
            stickerItemId = if (protoMsg.content.stickerItemId > 0) protoMsg.content.stickerItemId else null,
            stickerPackId = if (protoMsg.content.stickerPackId > 0) protoMsg.content.stickerPackId else null,
            callText = if (protoMsg.content.callText.isNotEmpty()) protoMsg.content.callText else null,
            callStatusText = if (protoMsg.content.callStatusText.isNotEmpty()) protoMsg.content.callStatusText else null,
            width = if (protoMsg.content.width > 0) protoMsg.content.width else null,
            height = if (protoMsg.content.height > 0) protoMsg.content.height else null
        )

        val cmd = if (protoMsg.hasCmd()) {
            MessageCmd(
                name = protoMsg.cmd.name,
                type = protoMsg.cmd.type
            )
        } else null

        return ChatMessage(
            msgId = protoMsg.msgId,
            sender = sender,
            direction = protoMsg.direction,
            contentType = protoMsg.contentType,
            content = content,
            sendTime = protoMsg.sendTime,
            cmd = cmd,
            msgDeleteTime = if (protoMsg.msgDeleteTime > 0) protoMsg.msgDeleteTime else null,
            quoteMsgId = if (protoMsg.quoteMsgId.isNotEmpty()) protoMsg.quoteMsgId else null,
            msgSeq = protoMsg.msgSeq,
            editTime = if (protoMsg.editTime > 0) protoMsg.editTime else null
        )
    }
    
    // ========== WebSocket相关的本地存储方法 ==========
    
    /**
     * 通过消息ID获取消息
     */
    suspend fun getMessageById(msgId: String): ChatMessage? {
        return try {
            cacheRepository.getMessageById(msgId)
        } catch (e: Exception) {
            Log.e(tag, "Error getting message by id: $msgId", e)
            null
        }
    }
    
    /**
     * 插入新消息到本地缓存
     */
    suspend fun insertMessage(message: ChatMessage) {
        try {
            cacheRepository.cacheMessages(listOf(message))
            Log.d(tag, "Inserted message: ${message.msgId}")
        } catch (e: Exception) {
            Log.e(tag, "Error inserting message: ${message.msgId}", e)
        }
    }
    
    /**
     * 更新本地消息
     */
    suspend fun updateMessage(message: ChatMessage) {
        try {
            // 先删除旧消息，再插入新消息
            cacheRepository.deleteMessage(message.msgId)
            cacheRepository.cacheMessages(listOf(message))
            Log.d(tag, "Updated message: ${message.msgId}")
        } catch (e: Exception) {
            Log.e(tag, "Error updating message: ${message.msgId}", e)
        }
    }
    
    /**
     * 删除本地消息
     */
    suspend fun deleteMessage(msgId: String) {
        try {
            cacheRepository.deleteMessage(msgId)
            Log.d(tag, "Deleted message: $msgId")
        } catch (e: Exception) {
            Log.e(tag, "Error deleting message: $msgId", e)
        }
    }
    
    /**
     * 撤回消息
     */
    suspend fun recallMessage(
        msgId: String,
        chatId: String,
        chatType: Int
    ): Result<Boolean> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "Token is null or empty")
                return Result.failure(Exception("用户未登录"))
            }

        
            val requestBuilder = send_message_send.newBuilder()
                .setMsgId(msgId) // 使用实际的消息ID而不是生成新的UUID
                .setChatId(chatId)
                .setChatType(chatType.toLong())
            Log.d(tag, "Recalling message: $msgId, chat: $chatId, type: $chatType")
            
            val request = requestBuilder.build()
            val requestBody = request.toByteArray().toRequestBody("application/x-protobuf".toMediaType())

            val response = apiService.recallMessage(token, requestBody)
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val bytes = responseBody.bytes()
                    val sendResponse = recall_msg.parseFrom(bytes)
                    
                    if (sendResponse.status.code == 1) {
                        Log.d(tag, "Message edited successfully")
                    Result.success(true)
                } else {
                        Log.e(tag, "Edit message error: ${sendResponse.status.msg}")
                        Result.failure(Exception(sendResponse.status.msg))
                }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Log.e(tag, "HTTP error: ${response.code()}")
                Result.failure(Exception("撤回失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error recalling message", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取会话的最后一条消息
     */
    suspend fun getLastMessage(chatId: String, chatType: Int): ChatMessage? {
        return try {
            cacheRepository.getLastMessage(chatId, chatType)
        } catch (e: Exception) {
            Log.e(tag, "Error getting last message for chat: $chatId", e)
            null
        }
    }
}