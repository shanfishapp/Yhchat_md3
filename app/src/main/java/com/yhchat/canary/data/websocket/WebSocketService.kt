package com.yhchat.canary.data.websocket

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yhchat.canary.MainActivity
import com.yhchat.canary.R
import com.yhchat.canary.data.model.ChatMessage
import com.yhchat.canary.data.model.Conversation
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.proto.chat_ws_go.heartbeat_ack
import com.yhchat.canary.proto.chat_ws_go.push_message
import com.yhchat.canary.proto.chat_ws_go.edit_message
import com.yhchat.canary.proto.chat_ws_go.draft_input
import com.yhchat.canary.proto.chat_ws_go.bot_board_message
import com.yhchat.canary.proto.chat_ws_go.WsMsg
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket服务，用于实时接收聊天消息和发送草稿同步
 */
@Singleton
class WebSocketService @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val context: Context
) {
    private val tag = "WebSocketService"
    private val gson = Gson()
    
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var isConnected = false
    private var shouldReconnect = true
    private var currentUserId: String? = null
    private var currentDeviceId: String = UUID.randomUUID().toString().replace("-", "")
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 消息事件流 - 用于UI更新
    private val _messageEvents = MutableSharedFlow<MessageEvent>(
        replay = 0,
        extraBufferCapacity = 100
    )
    val messageEvents: SharedFlow<MessageEvent> = _messageEvents.asSharedFlow()
    
    // 连接状态流
    private val _connectionState = MutableSharedFlow<ConnectionState>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()

    // 会话更新流 - 当收到新消息时更新会话列表
    private val _conversationUpdates = MutableSharedFlow<ConversationUpdate>(
        replay = 0,
        extraBufferCapacity = 50
    )
    val conversationUpdates: SharedFlow<ConversationUpdate> = _conversationUpdates.asSharedFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "chat_messages"
        private const val NOTIFICATION_ID_BASE = 1000
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * 连接WebSocket
     */
    suspend fun connect(userId: String, platform: String = "windows") {
        if (isConnected) {
            Log.d(tag, "Already connected")
            return
        }

        val token = tokenRepository.getTokenSync()
        if (token == null) {
            Log.e(tag, "No token available")
            _connectionState.emit(ConnectionState.Error("未登录"))
            return
        }
        
        currentUserId = userId
        
        try {
            Log.d(tag, "Connecting to WebSocket...")
            _connectionState.emit(ConnectionState.Connecting)
            
            val request = Request.Builder()
                .url("wss://chat-ws-go.jwzhd.com/ws")
                .build()
            
            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(tag, "WebSocket opened")
                    isConnected = true
                    
                    // 发送登录请求 - 参考yh_user_sdk/core/ws.py
                    val loginData = JsonObject().apply {
                        addProperty("seq", UUID.randomUUID().toString().replace("-", ""))
                        addProperty("cmd", "login")
                        add("data", JsonObject().apply {
                            addProperty("userId", userId)
                            addProperty("token", token)
                            addProperty("platform", platform)
                            addProperty("deviceId", currentDeviceId)
                        })
                    }
                    
                    val loginJson = gson.toJson(loginData)
                    Log.d(tag, "Sending login: $loginJson")
                    webSocket.send(loginJson)
                    
                    scope.launch {
                        _connectionState.emit(ConnectionState.Connected)
                    }
                    
                    // 启动心跳 - 30秒间隔
                    startHeartbeat(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                    try {
                        Log.d(tag, "Received binary message: ${bytes.hex()}")
                        handleBinaryMessage(bytes.toByteArray())
                    } catch (e: Exception) {
                        Log.e(tag, "Error handling binary message", e)
                    }
                }
                
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        Log.d(tag, "Received text message: $text")
                        // 文本消息通常是登录响应等，暂不处理
                    } catch (e: Exception) {
                        Log.e(tag, "Error handling text message", e)
                    }
                }
                
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(tag, "WebSocket closing: $code $reason")
                    cleanup()
                }
                
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(tag, "WebSocket closed: $code $reason")
                    cleanup()
                    
                    scope.launch {
                        _connectionState.emit(ConnectionState.Disconnected)
                        
                        // 自动重连
                        if (shouldReconnect) {
                            delay(5000)
                            connect(userId, platform)
                        }
                    }
                }
                
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(tag, "WebSocket failure", t)
                    cleanup()
                    
                    scope.launch {
                        _connectionState.emit(ConnectionState.Error(t.message ?: "连接失败"))
                        
                        // 自动重连
                        if (shouldReconnect) {
                            delay(5000)
                            connect(userId, platform)
                        }
                    }
                }
            })
            
        } catch (e: Exception) {
            Log.e(tag, "Error connecting to WebSocket", e)
            _connectionState.emit(ConnectionState.Error(e.message ?: "连接失败"))
        }
    }
    
    /**
     * 发送草稿同步消息
     */
    fun sendDraftInput(chatId: String, input: String) {
        if (!isConnected || webSocket == null) {
            Log.w(tag, "WebSocket not connected, cannot send draft")
            return
        }
        
        try {
            // 参考用户提供的草稿同步格式
            val dataObject = JsonObject().apply {
                addProperty("chatId", chatId)
                addProperty("input", input)
                addProperty("deviceId", currentDeviceId)
            }
            val draftData = JsonObject().apply {
                addProperty("seq", UUID.randomUUID().toString().replace("-", ""))
                addProperty("cmd", "inputInfo")
                add("data", dataObject)
            }
            
            val draftJson = gson.toJson(draftData)
            Log.d(tag, "Sending draft input: $draftJson")
            webSocket?.send(draftJson)
            
        } catch (e: Exception) {
            Log.e(tag, "Error sending draft input", e)
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        Log.d(tag, "Disconnecting WebSocket")
        shouldReconnect = false
        cleanup()
        webSocket?.close(1000, "User disconnect")
        webSocket = null
    }
    
    /**
     * 启动心跳 - 参考yh_user_sdk/core/ws.py
     */
    private fun startHeartbeat(webSocket: WebSocket) {
        heartbeatJob = scope.launch {
            while (isConnected && currentCoroutineContext().isActive) {
                try {
                    delay(30000) // 30秒心跳间隔
                    
                    val heartbeatData = JsonObject().apply {
                        addProperty("seq", UUID.randomUUID().toString().replace("-", ""))
                        addProperty("cmd", "heartbeat")
                        add("data", JsonObject())
                    }
                    
                    val heartbeatJson = gson.toJson(heartbeatData)
                    Log.d(tag, "Sending heartbeat")
                    webSocket.send(heartbeatJson)
                    
                } catch (e: Exception) {
                    Log.e(tag, "Error sending heartbeat", e)
                    break
                }
            }
        }
    }

    /**
     * 处理二进制消息 - 参考yh_user_sdk/core/ws.py的decode方法
     */
    private fun handleBinaryMessage(bytes: ByteArray) {
        try {
            // 按照yh_user_sdk的方式，首先尝试解析为heartbeat_ack获取命令类型
            val tempMsg = heartbeat_ack.parseFrom(bytes)
            val cmd = tempMsg.info.cmd
            val seq = tempMsg.info.seq
            
            Log.d(tag, "Message command: $cmd, seq: $seq")
            
            when (cmd) {
                "heartbeat_ack" -> {
                    Log.d(tag, "Received heartbeat ack")
                    // 心跳响应，不需要特殊处理
                }
                
                "push_message" -> {
                    // 新消息推送
                    val pushMessage = push_message.parseFrom(bytes)
                    if (pushMessage.hasData() && pushMessage.data.hasMsg()) {
                        val protoMsg = pushMessage.data.msg
                        val chatMessage = convertWsMsgToMessage(protoMsg)
                        
                        scope.launch {
                            // 发送消息事件供UI更新
                            _messageEvents.emit(MessageEvent.NewMessage(chatMessage))
                            
                            // 更新会话列表
                            _conversationUpdates.emit(ConversationUpdate.NewMessage(chatMessage))
                            
                            // 显示通知
                            showMessageNotification(chatMessage)
                        }
                        
                        Log.d(tag, "Received new message: ${chatMessage.msgId}")
                    }
                }
                
                "edit_message" -> {
                    // 消息编辑
                    val editMessage = edit_message.parseFrom(bytes)
                    if (editMessage.hasData() && editMessage.data.hasMsg()) {
                        val protoMsg = editMessage.data.msg
                        val chatMessage = convertWsMsgToMessage(protoMsg)
                        
                        scope.launch {
                            _messageEvents.emit(MessageEvent.MessageEdited(chatMessage))
                            _conversationUpdates.emit(ConversationUpdate.MessageEdited(chatMessage))
                        }
                        
                        Log.d(tag, "Received edited message: ${chatMessage.msgId}")
                    }
                }
                
                "draft_input" -> {
                    // 草稿同步 - 暂时只记录日志
                    val draftInput = draft_input.parseFrom(bytes)
                    if (draftInput.hasData() && draftInput.data.hasDraft()) {
                        val draft = draftInput.data.draft
                        Log.d(tag, "Received draft input for chat ${draft.chatId}: ${draft.input}")
                        
                        scope.launch {
                            _messageEvents.emit(MessageEvent.DraftUpdated(draft.chatId, draft.input))
                        }
                    }
                }
                
                "bot_board_message" -> {
                    // 机器人公告
                    val botBoardMessage = bot_board_message.parseFrom(bytes)
                    if (botBoardMessage.hasData() && botBoardMessage.data.hasBoard()) {
                        val board = botBoardMessage.data.board
                        Log.d(tag, "Received bot board message from ${board.botId}: ${board.content}")
                        
                        scope.launch {
                            _messageEvents.emit(MessageEvent.BotBoardMessage(
                                board.botId, 
                                board.chatId, 
                                board.content
                            ))
                        }
                    }
                }
                
                else -> {
                    Log.d(tag, "Unhandled message command: $cmd")
                }
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error parsing binary message", e)
        }
    }
    
    /**
     * 将WebSocket Proto消息转换为应用内消息模型
     */
    private fun convertWsMsgToMessage(protoMsg: WsMsg): ChatMessage {
        val sender = com.yhchat.canary.data.model.MessageSender(
            chatId = protoMsg.sender.chatId,
            chatType = protoMsg.sender.chatType,
            name = protoMsg.sender.name,
            avatarUrl = protoMsg.sender.avatarUrl,
            tagOld = protoMsg.sender.tagOldList,
            tag = protoMsg.sender.tagList.map { tag ->
                com.yhchat.canary.data.model.MessageTag(
                    id = tag.id,
                    text = tag.text,
                    color = tag.color
                )
            }
        )

        val content = com.yhchat.canary.data.model.MessageContent(
            text = if (protoMsg.content.text.isNotEmpty()) protoMsg.content.text else null,
            buttons = if (protoMsg.content.buttons.isNotEmpty()) protoMsg.content.buttons else null,
            imageUrl = if (protoMsg.content.imageUrl.isNotEmpty()) protoMsg.content.imageUrl else null,
            fileName = if (protoMsg.content.fileName.isNotEmpty()) protoMsg.content.fileName else null,
            fileUrl = if (protoMsg.content.fileUrl.isNotEmpty()) protoMsg.content.fileUrl else null,
            form = if (protoMsg.content.form.isNotEmpty()) protoMsg.content.form else null,
            quoteMsgText = if (protoMsg.content.quoteMsgText.isNotEmpty()) protoMsg.content.quoteMsgText else null,
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
            com.yhchat.canary.data.model.MessageCmd(
                name = protoMsg.cmd.name,
                type = protoMsg.cmd.id.toInt()
            )
        } else null

        return ChatMessage(
            msgId = protoMsg.msgId,
            sender = sender,
            direction = "left", // WebSocket消息通常是接收的消息
            contentType = protoMsg.contentType,
            content = content,
            sendTime = protoMsg.timestamp,
            cmd = cmd,
            msgDeleteTime = if (protoMsg.deleteTime > 0) protoMsg.deleteTime else null,
            quoteMsgId = if (protoMsg.quoteMsgId.isNotEmpty()) protoMsg.quoteMsgId else null,
            msgSeq = protoMsg.msgSeq,
            editTime = if (protoMsg.editTime > 0) protoMsg.editTime else null
        )
    }
    
    /**
     * 显示消息通知
     */
    private fun showMessageNotification(message: ChatMessage) {
        try {
            // 不为自己的消息显示通知
            if (message.sender.chatId == currentUserId) {
                return
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // 构建通知内容
            val senderName = message.sender.name.ifEmpty { "未知用户" }
            val messageContent = when {
                !message.content.text.isNullOrEmpty() -> message.content.text
                !message.content.imageUrl.isNullOrEmpty() -> "[图片]"
                !message.content.fileUrl.isNullOrEmpty() -> "[文件]"
                !message.content.audioUrl.isNullOrEmpty() -> "[语音]"
                !message.content.videoUrl.isNullOrEmpty() -> "[视频]"
                !message.content.stickerUrl.isNullOrEmpty() -> "[表情]"
                else -> "[消息]"
            }
            
            // 确定会话名称
            val conversationTitle = when (message.sender.chatType) {
                1 -> senderName // 私聊直接用发送者名称
                2 -> "群聊" // 群聊
                3 -> "机器人" // 机器人
                else -> "会话"
            }
            
            // 点击通知的Intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("chat_id", message.sender.chatId)
                putExtra("chat_type", message.sender.chatType)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 
                message.sender.chatId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // 构建通知
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(conversationTitle)
                .setContentText("$senderName：$messageContent")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            
            // 显示通知，使用chatId的哈希作为通知ID
            notificationManager.notify(
                NOTIFICATION_ID_BASE + message.sender.chatId.hashCode(),
                notification
            )
            
            Log.d(tag, "Shown notification for message from $senderName")
            
        } catch (e: Exception) {
            Log.e(tag, "Error showing notification", e)
        }
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "聊天消息",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "接收新的聊天消息通知"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 清理资源
     */
    private fun cleanup() {
        isConnected = false
        heartbeatJob?.cancel()
    }

    /**
     * 销毁服务
     */
    fun destroy() {
        shouldReconnect = false
        disconnect()
        scope.cancel()
    }
}

/**
 * WebSocket连接状态
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * 消息事件
 */
sealed class MessageEvent {
    data class NewMessage(val message: ChatMessage) : MessageEvent()
    data class MessageEdited(val message: ChatMessage) : MessageEvent()
    data class MessageDeleted(val msgId: String) : MessageEvent()
    data class DraftUpdated(val chatId: String, val input: String) : MessageEvent()
    data class BotBoardMessage(val botId: String, val chatId: String, val content: String) : MessageEvent()
}

/**
 * 会话更新事件
 */
sealed class ConversationUpdate {
    data class NewMessage(val message: ChatMessage) : ConversationUpdate()
    data class MessageEdited(val message: ChatMessage) : ConversationUpdate()
}