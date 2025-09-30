package com.yhchat.canary.data.websocket

import android.util.Log
import com.yhchat.canary.data.model.ChatMessage
import com.yhchat.canary.data.repository.ConversationRepository
import com.yhchat.canary.data.repository.MessageRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket管理器 - 协调WebSocket服务和数据存储
 * 负责处理收到的消息并更新本地数据
 */
@Singleton
class WebSocketManager @Inject constructor(
    private val webSocketService: WebSocketService,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) {
    private val tag = "WebSocketManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 是否已经初始化监听
    private var isListening = false
    
    /**
     * 开始监听WebSocket消息并处理数据更新
     */
    fun startListening() {
        if (isListening) {
            Log.d(tag, "Already listening to WebSocket messages")
            return
        }
        
        isListening = true
        Log.d(tag, "Starting to listen to WebSocket messages")
        
        // 监听消息事件并处理
        scope.launch {
            webSocketService.messageEvents.collect { event ->
                handleMessageEvent(event)
            }
        }
        
        // 监听会话更新事件
        scope.launch {
            webSocketService.conversationUpdates.collect { update ->
                handleConversationUpdate(update)
            }
        }
    }
    
    /**
     * 停止监听
     */
    fun stopListening() {
        isListening = false
        scope.cancel()
    }
    
    /**
     * 连接WebSocket
     */
    suspend fun connect(userId: String, platform: String = "android") {
        startListening() // 确保开始监听
        webSocketService.connect(userId, platform)
    }
    
    /**
     * 断开WebSocket连接
     */
    fun disconnect() {
        webSocketService.disconnect()
    }
    
    /**
     * 发送草稿同步
     */
    fun sendDraftInput(chatId: String, input: String) {
        webSocketService.sendDraftInput(chatId, input)
    }
    
    /**
     * 获取连接状态流
     */
    fun getConnectionState(): SharedFlow<ConnectionState> {
        return webSocketService.connectionState
    }
    
    /**
     * 获取消息事件流 - 供UI监听
     */
    fun getMessageEvents(): SharedFlow<MessageEvent> {
        return webSocketService.messageEvents
    }
    
    /**
     * 处理消息事件
     */
    private suspend fun handleMessageEvent(event: MessageEvent) {
        try {
            when (event) {
                is MessageEvent.NewMessage -> {
                    Log.d(tag, "Handling new message: ${event.message.msgId}")
                    
                    // 将新消息插入到本地数据库/缓存
                    insertMessageToLocal(event.message)
                }
                
                is MessageEvent.MessageEdited -> {
                    Log.d(tag, "Handling edited message: ${event.message.msgId}")
                    
                    // 更新本地消息
                    updateLocalMessage(event.message)
                }
                
                is MessageEvent.MessageDeleted -> {
                    Log.d(tag, "Handling deleted message: ${event.msgId}")
                    
                    // 删除本地消息
                    deleteLocalMessage(event.msgId)
                }
                
                is MessageEvent.DraftUpdated -> {
                    Log.d(tag, "Handling draft update for chat: ${event.chatId}")
                    
                    // 可以在这里处理草稿同步，比如更新UI状态
                    // 暂时只记录日志
                }
                
                is MessageEvent.BotBoardMessage -> {
                    Log.d(tag, "Handling bot board message from: ${event.botId}")
                    
                    // 处理机器人公告消息
                    // 可以显示为系统消息或特殊通知
                }
                
                is MessageEvent.StreamMessage -> {
                    Log.d(tag, "Handling stream message update: msgId=${event.msgId}, chatId=${event.chatId}")
                    // 流式消息由前台实时渲染，无需写入本地数据库
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error handling message event", e)
        }
    }
    
    /**
     * 处理会话更新
     */
    private suspend fun handleConversationUpdate(update: ConversationUpdate) {
        try {
            when (update) {
                is ConversationUpdate.NewMessage -> {
                    Log.d(tag, "Updating conversation for new message from: ${update.message.sender.chatId}")
                    
                    // 更新会话的最后一条消息和时间
                    updateConversationLastMessage(update.message)
                }
                
                is ConversationUpdate.MessageEdited -> {
                    Log.d(tag, "Updating conversation for edited message: ${update.message.msgId}")
                    
                    // 如果编辑的是最后一条消息，需要更新会话显示
                    updateConversationIfLastMessage(update.message)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error handling conversation update", e)
        }
    }
    
    /**
     * 将新消息插入到本地存储
     */
    private suspend fun insertMessageToLocal(message: ChatMessage) {
        try {
            // 检查消息是否已存在，避免重复插入
            val existingMessage = messageRepository.getMessageById(message.msgId)
            if (existingMessage != null) {
                Log.d(tag, "Message ${message.msgId} already exists, skipping insert")
                return
            }
            
            // 插入新消息到本地数据库
            messageRepository.insertMessage(message)
            
            Log.d(tag, "Inserted new message: ${message.msgId}")
            
                } catch (e: Exception) {
            Log.e(tag, "Error inserting message to local storage", e)
        }
    }
    
    /**
     * 更新本地消息
     */
    private suspend fun updateLocalMessage(message: ChatMessage) {
        try {
            messageRepository.updateMessage(message)
            Log.d(tag, "Updated local message: ${message.msgId}")
        } catch (e: Exception) {
            Log.e(tag, "Error updating local message", e)
        }
    }
    
    /**
     * 删除本地消息
     */
    private suspend fun deleteLocalMessage(msgId: String) {
        try {
            messageRepository.deleteMessage(msgId)
            Log.d(tag, "Deleted local message: $msgId")
        } catch (e: Exception) {
            Log.e(tag, "Error deleting local message", e)
        }
    }
    
    /**
     * 更新会话的最后一条消息
     */
    private suspend fun updateConversationLastMessage(message: ChatMessage) {
        try {
            // 根据文档说明，只有当recvId和chatId相等时，才创建/更新与发送者的私聊会话
            // 否则，应该更新chatId对应的群聊会话
            val isPrivateChat = message.chatId == message.recvId
            
            if (isPrivateChat) {
                // 私聊消息或机器人对话：创建/更新与发送者的私聊会话
                val targetChatId = message.sender.chatId
                val targetChatType = message.sender.chatType
                
                Log.d(tag, "Updating private conversation:")
                Log.d(tag, "  - Private chat with: ${message.sender.chatId} (${message.sender.name})")
                Log.d(tag, "  - Message preview: ${getMessagePreview(message)}")
                
                conversationRepository.updateLastMessage(
                    chatId = targetChatId,
                    chatType = targetChatType,
                    lastMessage = getMessagePreview(message),
                    lastMessageTime = message.sendTime,
                    unreadCount = 1
                )
                
                Log.d(tag, "Successfully updated private conversation: $targetChatId")
                
            } else {
                // 群聊消息：更新群聊会话，不创建私聊会话
                val targetChatId = message.chatId!!
                val targetChatType = message.chatType!!
                
                Log.d(tag, "Updating group conversation:")
                Log.d(tag, "  - Group: $targetChatId (type: $targetChatType)")
                Log.d(tag, "  - Sender: ${message.sender.chatId} (${message.sender.name})")
                Log.d(tag, "  - Message preview: ${getMessagePreview(message)}")
                
                conversationRepository.updateLastMessage(
                    chatId = targetChatId,
                    chatType = targetChatType,
                    lastMessage = getMessagePreview(message),
                    lastMessageTime = message.sendTime,
                    unreadCount = 1
                )
                
                Log.d(tag, "Successfully updated group conversation: $targetChatId")
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error updating conversation last message", e)
        }
    }
    
    /**
     * 如果是最后一条消息则更新会话
     */
    private suspend fun updateConversationIfLastMessage(message: ChatMessage) {
        try {
            // 使用正确的chatId和chatType
            val targetChatId = message.chatId ?: message.sender.chatId
            val targetChatType = message.chatType ?: message.sender.chatType
            
            // 检查是否是会话的最后一条消息
            val lastMessage = messageRepository.getLastMessage(
                targetChatId, 
                targetChatType
            )
            
            if (lastMessage?.msgId == message.msgId) {
                // 是最后一条消息，更新会话显示
                conversationRepository.updateLastMessage(
                    chatId = targetChatId,
                    chatType = targetChatType,
                    lastMessage = getMessagePreview(message),
                    lastMessageTime = message.sendTime,
                    unreadCount = null // 不改变未读数
                )
                
                Log.d(tag, "Updated conversation for edited last message: ${message.msgId}")
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error checking and updating conversation for edited message", e)
        }
    }
    
    /**
     * 获取消息预览文本
     */
    private fun getMessagePreview(message: ChatMessage): String {
        // 获取消息内容预览
        val contentPreview = when {
            !message.content.text.isNullOrEmpty() -> message.content.text
            !message.content.imageUrl.isNullOrEmpty() -> "[图片]"
            !message.content.fileUrl.isNullOrEmpty() -> {
                if (!message.content.fileName.isNullOrEmpty()) {
                    "[文件]${message.content.fileName}"
                } else {
                    "[文件]"
                }
            }
            !message.content.audioUrl.isNullOrEmpty() -> "[语音]"
            !message.content.videoUrl.isNullOrEmpty() -> "[视频]"
            !message.content.stickerUrl.isNullOrEmpty() -> "[表情]"
            !message.content.postTitle.isNullOrEmpty() -> "[文章]${message.content.postTitle}"
            else -> "[消息]"
        }
        
        // 确定目标会话类型
        val targetChatType = message.chatType ?: message.sender.chatType
        
        // 根据会话类型决定显示格式
        return when (targetChatType) {
            2, 3 -> {
                // 群聊(2)或机器人会话(3)：显示"发送者：内容"
                "${message.sender.name}：$contentPreview"
            }
            else -> {
                // 私聊(1)或其他：只显示内容
                contentPreview
            }
        }
    }
    
    /**
     * 销毁管理器
     */
    fun destroy() {
        stopListening()
        webSocketService.destroy()
    }
}
