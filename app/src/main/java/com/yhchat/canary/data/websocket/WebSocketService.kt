package com.yhchat.canary.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.yhchat.canary.data.model.ChatMessage
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.proto.chat_ws_go.heartbeat_ack
import com.yhchat.canary.proto.chat_ws_go.push_message
import com.yhchat.canary.proto.chat_ws_go.edit_message
import com.yhchat.canary.proto.chat_ws_go.draft_input
import com.yhchat.canary.proto.chat_ws_go.file_send_message
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
 * WebSocket服务，用于实时接收聊天消息
 */
@Singleton
class WebSocketService @Inject constructor(
    private val tokenRepository: TokenRepository
) {
    private val tag = "WebSocketService"
    private val gson = Gson()
    
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var isConnected = false
    private var shouldReconnect = true
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 消息事件流
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

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    /**
     * 连接WebSocket
     */
    suspend fun connect(userId: String, platform: String = "android") {
        if (isConnected) {
            Log.d(tag, "Already connected")
            return
        }

        val token = tokenRepository.getToken()
        if (token == null) {
            Log.e(tag, "No token available")
            _connectionState.emit(ConnectionState.Error("未登录"))
            return
        }
        
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
                    
                    // 发送登录请求
                    val loginData = mapOf(
                        "seq" to UUID.randomUUID().toString().replace("-", ""),
                        "cmd" to "login",
                        "data" to mapOf(
                            "userId" to userId,
                            "token" to token,
                            "platform" to platform,
                            "deviceId" to UUID.randomUUID().toString().replace("-", "")
                        )
                    )
                    
                    val loginJson = gson.toJson(loginData)
                    Log.d(tag, "Sending login: $loginJson")
                    webSocket.send(loginJson)
                    
                    scope.launch {
                        _connectionState.emit(ConnectionState.Connected)
                    }
                    
                    // 启动心跳
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
                        handleTextMessage(text)
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
                            delay(5000) // 等待5秒后重连
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
                            delay(5000) // 等待5秒后重连
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
     * 启动心跳
     */
    private fun startHeartbeat(webSocket: WebSocket) {
        heartbeatJob = scope.launch {
            while (isConnected && currentCoroutineContext().isActive) {
                try {
                    delay(30000) // 30秒心跳间隔
                    
                    val heartbeatData = mapOf(
                        "seq" to UUID.randomUUID().toString().replace("-", ""),
                        "cmd" to "heartbeat",
                        "data" to emptyMap<String, Any>()
                    )
                    
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
     * 处理文本消息
     */
    private fun handleTextMessage(text: String) {
        // 通常不会收到文本消息，主要是protobuf二进制消息
        Log.d(tag, "Received text message: $text")
    }

    /**
     * 处理二进制消息
     * 参考yh_user_sdk/core/ws.py的decode方法
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
                            _messageEvents.emit(MessageEvent.NewMessage(chatMessage))
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
                        }
                        
                        Log.d(tag, "Received edited message: ${chatMessage.msgId}")
                    }
                }
                
                "draft_input" -> {
                    // 草稿同步，暂不处理
                    Log.d(tag, "Received draft input")
                }
                
                "file_send_message" -> {
                    // 文件分享，暂不处理
                    Log.d(tag, "Received file send message")
                }
                
                "bot_board_message" -> {
                    // 机器人公告，暂不处理
                    Log.d(tag, "Received bot board message")
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
        // 这里重用MessageRepository中的转换逻辑
        // 为了避免循环依赖，我们在这里直接实现转换
        
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
     * 清理资源
     */
    private fun cleanup() {
        isConnected = false
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
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
}