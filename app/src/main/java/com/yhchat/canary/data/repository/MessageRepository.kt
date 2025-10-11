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
                            convertProtoToMessage(protoMsg, chatId, chatType)
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
                            convertProtoToMessage(protoMsg, chatId, chatType)
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
        quoteMsgText: String? = null
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
            
            // 添加引用消息文本
            if (!quoteMsgText.isNullOrEmpty()) {
                contentBuilder.setQuoteMsgText(quoteMsgText)
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

            Log.d(tag, "Sending message to chat: $chatId, type: $chatType, text: $text")
            
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
     * 撤回消息
     * 使用protobuf的recall_msg_send
     */
    suspend fun recallMessage(
        chatId: String,
        chatType: Int,
        msgId: String
    ): Result<Boolean> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "❌ Token为空")
                return Result.failure(Exception("用户未登录"))
            }
            
            Log.d(tag, "📤 ========== 撤回消息 ==========")
            Log.d(tag, "📤 msgId: $msgId")
            Log.d(tag, "📤 chatId: $chatId")
            Log.d(tag, "📤 chatType: $chatType")
            
            // 构建protobuf请求
            val request = recall_msg_send.newBuilder()
                .setMsgId(msgId)
                .setChatId(chatId)
                .setChatType(chatType.toLong())
                .build()
            
            val requestBody = request.toByteArray().toRequestBody("application/x-protobuf".toMediaType())
            
            Log.d(tag, "📤 发送撤回请求...")
            
            val response = apiService.recallMessage(token, requestBody)
            
            Log.d(tag, "📥 服务器响应码: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val bytes = responseBody.bytes()
                    val recallResponse = recall_msg.parseFrom(bytes)
                    
                    Log.d(tag, "📥 响应状态码: ${recallResponse.status.code}")
                    Log.d(tag, "📥 响应消息: ${recallResponse.status.msg}")
                    
                    if (recallResponse.status.code == 1) {
                        Log.d(tag, "✅ ========== 消息撤回成功！ ==========")
                        Result.success(true)
                    } else {
                        Log.e(tag, "❌ 撤回失败: ${recallResponse.status.msg}")
                        Result.failure(Exception(recallResponse.status.msg))
                    }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(tag, "❌ HTTP错误: ${response.code()}, 错误详情: $errorBody")
                Result.failure(Exception("撤回失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ 撤回消息异常", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 编辑消息
     * 使用protobuf的edit_message_send
     */
    suspend fun editMessage(
        chatId: String,
        chatType: Int,
        msgId: String,
        content: String,
        contentType: Int
    ): Result<Boolean> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "❌ Token为空")
                return Result.failure(Exception("用户未登录"))
            }
            
            Log.d(tag, "📤 ========== 编辑消息 ==========")
            Log.d(tag, "📤 msgId: $msgId")
            Log.d(tag, "📤 chatId: $chatId")
            Log.d(tag, "📤 chatType: $chatType")
            Log.d(tag, "📤 contentType: $contentType")
            Log.d(tag, "📤 content: ${content.take(100)}...")
            
            // 构建protobuf请求
            val contentBuilder = edit_message_send.Content.newBuilder()
                .setText(content)
            
            val request = edit_message_send.newBuilder()
                .setMsgId(msgId)
                .setChatId(chatId)
                .setChatType(chatType)
                .setContent(contentBuilder.build())
                .setContentType(contentType.toLong())
                .build()
            
            val requestBody = request.toByteArray().toRequestBody("application/x-protobuf".toMediaType())
            
            Log.d(tag, "📤 发送编辑请求...")
            
            val response = apiService.editMessage(token, requestBody)
            
            Log.d(tag, "📥 服务器响应码: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val bytes = responseBody.bytes()
                    val editResponse = edit_message.parseFrom(bytes)
                    
                    Log.d(tag, "📥 响应状态码: ${editResponse.status.code}")
                    Log.d(tag, "📥 响应消息: ${editResponse.status.msg}")
                    
                    if (editResponse.status.code == 1) {
                        Log.d(tag, "✅ ========== 消息编辑成功！ ==========")
                        Result.success(true)
                    } else {
                        Log.e(tag, "❌ 编辑失败: ${editResponse.status.msg}")
                        Result.failure(Exception(editResponse.status.msg))
                    }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(tag, "❌ HTTP错误: ${response.code()}, 错误详情: $errorBody")
                Result.failure(Exception("编辑失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ 编辑消息异常", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 发送图片消息
     * 参考Python实现：只需在content.image设置图片key即可
     */
    suspend fun sendImageMessage(
        chatId: String,
        chatType: Int,
        imageKey: String,
        width: Int,
        height: Int,
        fileSize: Long,
        quoteMsgId: String? = null,
        quoteMsgText: String? = null
    ): Result<Boolean> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "❌ Token为空")
                return Result.failure(Exception("用户未登录"))
            }

            val msgId = UUID.randomUUID().toString().replace("-", "")
            
            Log.d(tag, "📤 ========== 发送图片消息 ==========")
            Log.d(tag, "📤 msgId: $msgId")
            Log.d(tag, "📤 chatId: $chatId")
            Log.d(tag, "📤 chatType: $chatType")
            Log.d(tag, "📤 imageKey: $imageKey")
            Log.d(tag, "📤 图片尺寸: ${width}x${height}")
            Log.d(tag, "📤 文件大小: $fileSize bytes")
            
            // 构建protobuf请求 - 根据Python实现，只需设置content.image
            val contentBuilder = send_message_send.Content.newBuilder()
                .setImage(imageKey)  // 设置图片key，例如：f812a79eca05dfa884c9e89d54b2bca5.jpg
            
            // 添加引用消息文本
            if (!quoteMsgText.isNullOrEmpty()) {
                contentBuilder.setQuoteMsgText(quoteMsgText)
                Log.d(tag, "📤 引用消息: $quoteMsgText")
            }
            
            val requestBuilder = send_message_send.newBuilder()
                .setMsgId(msgId)
                .setChatId(chatId)
                .setChatType(chatType.toLong())
                .setContent(contentBuilder.build())
                .setContentType(2) // 图片消息类型为2
            
            if (!quoteMsgId.isNullOrEmpty()) {
                requestBuilder.setQuoteMsgId(quoteMsgId)
                Log.d(tag, "📤 引用消息ID: $quoteMsgId")
            }
            
            // 添加media信息 - 包含图片的详细元数据
            val mediaBuilder = send_message_send.Media.newBuilder()
                .setFileKey(imageKey)
                .setFileKey2(imageKey) // 据说不写会报错
                .setFileType("image/jpeg")
                .setImageWidth(width.toLong())
                .setImageHeight(height.toLong())
                .setFileSize(fileSize)
                .setFileSuffix(imageKey.substringAfterLast("."))
            
            requestBuilder.setMedia(mediaBuilder.build())
            
            val request = requestBuilder.build()
            val requestBytes = request.toByteArray()
            val requestBody = requestBytes.toRequestBody("application/x-protobuf".toMediaType())

            Log.d(tag, "📤 Protobuf请求大小: ${requestBytes.size} bytes")
            
            val response = apiService.sendMessage(token, requestBody)
            
            Log.d(tag, "📥 服务器响应码: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val bytes = responseBody.bytes()
                    val sendResponse = send_message.parseFrom(bytes)
                    
                    Log.d(tag, "📥 响应状态码: ${sendResponse.status.code}")
                    Log.d(tag, "📥 响应消息: ${sendResponse.status.msg}")
                    
                    if (sendResponse.status.code == 1) {
                        Log.d(tag, "✅ ========== 图片消息发送成功！ ==========")
                        Result.success(true)
                    } else {
                        Log.e(tag, "❌ 发送失败: ${sendResponse.status.msg}")
                        Result.failure(Exception(sendResponse.status.msg))
                    }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(tag, "❌ HTTP错误: ${response.code()}, 错误详情: $errorBody")
                Result.failure(Exception("发送失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ 发送图片消息异常", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 发送表情消息 (contentType=7)
     * @param expression 表情对象，包含id、url等信息
     */
    suspend fun sendExpressionMessage(
        chatId: String,
        chatType: Int,
        expression: com.yhchat.canary.data.model.Expression,
        quoteMsgId: String? = null,
        quoteMsgText: String? = null
    ): Result<Boolean> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "❌ Token为空")
                return Result.failure(Exception("用户未登录"))
            }

            val msgId = UUID.randomUUID().toString().replace("-", "")
            
            Log.d(tag, "📤 ========== 发送表情消息 ==========")
            Log.d(tag, "📤 msgId: $msgId")
            Log.d(tag, "📤 chatId: $chatId")
            Log.d(tag, "📤 chatType: $chatType")
            Log.d(tag, "📤 表情ID: ${expression.id}")
            Log.d(tag, "📤 表情URL: ${expression.url}")
            
            // 构建protobuf请求
            // 注意：expression.url已经是不带域名的路径，如 "expression/xxx.jpg"
            val contentBuilder = send_message_send.Content.newBuilder()
                .setExpressionId(expression.id.toString())  // 个人收藏表情ID
                .setImage(expression.url)  // 图片路径（不带域名）
            
            // 添加引用消息文本
            if (!quoteMsgText.isNullOrEmpty()) {
                contentBuilder.setQuoteMsgText(quoteMsgText)
                Log.d(tag, "📤 引用消息: $quoteMsgText")
            }
            
            val requestBuilder = send_message_send.newBuilder()
                .setMsgId(msgId)
                .setChatId(chatId)
                .setChatType(chatType.toLong())
                .setContent(contentBuilder.build())
                .setContentType(7) // 表情消息类型为7
            
            if (!quoteMsgId.isNullOrEmpty()) {
                requestBuilder.setQuoteMsgId(quoteMsgId)
                Log.d(tag, "📤 引用消息ID: $quoteMsgId")
            }
            
            val request = requestBuilder.build()
            val requestBytes = request.toByteArray()
            val requestBody = requestBytes.toRequestBody("application/x-protobuf".toMediaType())

            Log.d(tag, "📤 Protobuf请求大小: ${requestBytes.size} bytes")
            
            val response = apiService.sendMessage(token, requestBody)
            
            Log.d(tag, "📥 服务器响应码: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val bytes = responseBody.bytes()
                    val sendResponse = send_message.parseFrom(bytes)
                    
                    Log.d(tag, "📥 响应状态码: ${sendResponse.status.code}")
                    Log.d(tag, "📥 响应消息: ${sendResponse.status.msg}")
                    
                    if (sendResponse.status.code == 1) {
                        Log.d(tag, "✅ ========== 表情消息发送成功！ ==========")
                        Result.success(true)
                    } else {
                        Log.e(tag, "❌ 发送失败: ${sendResponse.status.msg}")
                        Result.failure(Exception(sendResponse.status.msg))
                    }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(tag, "❌ HTTP错误: ${response.code()}, 错误详情: $errorBody")
                Result.failure(Exception("发送失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ 发送表情消息异常", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 将Proto消息转换为应用内消息模型
     */
    private fun convertProtoToMessage(protoMsg: Msg, chatId: String, chatType: Int): ChatMessage {
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
            editTime = if (protoMsg.editTime > 0) protoMsg.editTime else null,
            chatId = chatId,  // 设置会话ID
            chatType = chatType  // 设置会话类型
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
    
    /**
     * 上报按钮点击事件
     */
    suspend fun reportButtonClick(
        chatId: String,
        chatType: Int,
        msgId: String,
        userId: String,
        buttonValue: String
    ): Result<Unit> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "Token is null or empty")
                return Result.failure(Exception("用户未登录"))
            }

            // 构建protobuf请求
            val request = button_report_send.newBuilder()
                .setMsgId(msgId)
                .setChatType(chatType.toLong())
                .setChatId(chatId)
                .setUserId(userId)
                .setButtonValue(buttonValue)
                .build()
            
            val requestBody = request.toByteArray().toRequestBody("application/x-protobuf".toMediaType())

            val chatTypeText = when (chatType) {
                1 -> "私聊用户"
                2 -> "群聊"
                3 -> "机器人"
                else -> "未知类型($chatType)"
            }
            Log.d(tag, "Reporting button click: chatType=$chatTypeText, chatId=$chatId, msgId=$msgId, value=$buttonValue")
            
            val response = apiService.buttonReport(token, requestBody)
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val bytes = responseBody.bytes()
                    val buttonResponse = recall_msg.parseFrom(bytes) // 使用 recall_msg 解析（只有status）
                    
                    if (buttonResponse.status.code == 1) {
                        Log.d(tag, "Button click reported successfully")
                        Result.success(Unit)
                    } else {
                        Log.e(tag, "API error: ${buttonResponse.status.msg}")
                        Result.failure(Exception(buttonResponse.status.msg))
                    }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Log.e(tag, "HTTP error: ${response.code()}")
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error reporting button click", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取消息编辑历史记录
     */
    /**
     * 添加表情到个人收藏
     */
    suspend fun addExpressionToFavorites(expressionId: String): Result<Unit> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "Token is null or empty")
                return Result.failure(Exception("用户未登录"))
            }
            
            // 将String转换为Long
            val expressionIdLong = expressionId.toLongOrNull() ?: return Result.failure(Exception("无效的表情ID"))
            val request = AddExpressionRequest(id = expressionIdLong)
            
            Log.d(tag, "Adding expression to favorites: $expressionId")
            
            val response = apiService.addExpression(token, request)
            
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val message = body.message
                    if (body.code == 1) {
                        Log.d(tag, "Successfully added expression: $expressionId")
                        Result.success(Unit)
                    } else {
                        Log.e(tag, "Failed to add expression: $message")
                        Result.failure(Exception(message))
                    }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Log.e(tag, "Failed to add expression: ${response.message()}")
                Result.failure(Exception("添加表情失败: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error adding expression", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMessageEditHistory(msgId: String, size: Int = 10, page: Int = 1): Result<List<MessageEditRecord>> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "Token is null or empty")
                return Result.failure(Exception("用户未登录"))
            }
            
            val request = com.yhchat.canary.data.api.ListMessageEditRecordRequest(
                msgId = msgId,
                size = size,
                page = page
            )
            
            Log.d(tag, "Getting edit history for message: $msgId")
            
            val response = apiService.listMessageEditRecord(token, request)
            
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val code = body["code"] as? Double
                    if (code == 1.0) {
                        val data = body["data"] as? Map<*, *>
                        val list = data?.get("list") as? List<*>
                        
                        val records = list?.mapNotNull { item ->
                            val record = item as? Map<*, *> ?: return@mapNotNull null
                            MessageEditRecord(
                                id = (record["id"] as? Double)?.toLong() ?: 0L,
                                msgId = record["msgId"] as? String ?: "",
                                contentType = (record["contentType"] as? Double)?.toInt() ?: 1,
                                contentOld = record["contentOld"] as? String ?: "",
                                createTime = (record["createTime"] as? Double)?.toLong() ?: 0L,
                                msgTime = (record["msgTime"] as? Double)?.toLong() ?: 0L
                            )
                        } ?: emptyList()
                        
                        Log.d(tag, "Successfully got ${records.size} edit records")
                        Result.success(records)
                    } else {
                        val msg = body["msg"] as? String ?: "获取编辑历史失败"
                        Log.e(tag, "API error: $msg")
                        Result.failure(Exception(msg))
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
}