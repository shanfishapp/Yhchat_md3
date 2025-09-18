package com.yhchat.canary.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * WebSocket管理器
 */
class WebSocketManager private constructor() {
    
    companion object {
        private const val TAG = "WebSocketManager"
        private const val WS_URL = "wss://chat-ws-go.jwzhd.com/ws"
        private const val HEARTBEAT_INTERVAL = 30L // 30秒心跳
        
        @Volatile
        private var INSTANCE: WebSocketManager? = null
        
        fun getInstance(): WebSocketManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebSocketManager().also { INSTANCE = it }
            }
        }
    }
    
    private val client = OkHttpClient.Builder()
        .pingInterval(HEARTBEAT_INTERVAL, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var heartbeatJob: Job? = null
    
    // 消息流
        private val _messages = MutableSharedFlow<com.yhchat.canary.data.model.WebSocketMessage>()
        val messages: SharedFlow<com.yhchat.canary.data.model.WebSocketMessage> = _messages.asSharedFlow()
    
    // 会话更新流
    private val _conversations = MutableSharedFlow<List<com.yhchat.canary.data.model.Conversation>>()
    val conversations: SharedFlow<List<com.yhchat.canary.data.model.Conversation>> = _conversations.asSharedFlow()
    
    /**
     * 连接WebSocket
     */
    fun connect(token: String, userId: String, platform: String = "android", deviceId: String = UUID.randomUUID().toString()) {
        if (isConnected) {
            Log.d(TAG, "WebSocket already connected")
            return
        }
        
        val request = Request.Builder()
            .url(WS_URL)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                isConnected = true
                
                // 发送登录消息
                sendLoginMessage(token, userId, platform, deviceId)
                
                // 启动心跳
                startHeartbeat()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received text message: $text")
                handleTextMessage(text)
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                Log.d(TAG, "Received binary message: ${bytes.size} bytes")
                handleBinaryMessage(bytes.toByteArray())
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                isConnected = false
                stopHeartbeat()
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                isConnected = false
                stopHeartbeat()
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failed", t)
                isConnected = false
                stopHeartbeat()
                
                // 自动重连
                CoroutineScope(Dispatchers.IO).launch {
                    delay(5000) // 5秒后重连
                    if (!isConnected) {
                        connect(token, userId, platform, deviceId)
                    }
                }
            }
        })
    }
    
    /**
     * 发送登录消息
     */
    private fun sendLoginMessage(token: String, userId: String, platform: String, deviceId: String) {
        val loginMessage = JsonObject().apply {
            addProperty("seq", UUID.randomUUID().toString())
            addProperty("cmd", "login")
            add("data", JsonObject().apply {
                addProperty("userId", userId)
                addProperty("token", token)
                addProperty("platform", platform)
                addProperty("deviceId", deviceId)
            })
        }
        
        webSocket?.send(Gson().toJson(loginMessage))
    }
    
    /**
     * 启动心跳
     */
    private fun startHeartbeat() {
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isConnected) {
                try {
                    val heartbeatMessage = JsonObject().apply {
                        addProperty("seq", UUID.randomUUID().toString())
                        addProperty("cmd", "heartbeat")
                        add("data", JsonObject())
                    }
                    
                    webSocket?.send(Gson().toJson(heartbeatMessage))
                    Log.d(TAG, "Sent heartbeat")
                    
                    delay(HEARTBEAT_INTERVAL * 1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat failed", e)
                    break
                }
            }
        }
    }
    
    /**
     * 停止心跳
     */
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
    
    /**
     * 处理文本消息
     */
    private fun handleTextMessage(text: String) {
        try {
            val gson = Gson()
            val message = gson.fromJson(text, JsonObject::class.java)
            val cmd = message.get("cmd")?.asString
            
            when (cmd) {
                "heartbeat_ack" -> {
                    Log.d(TAG, "Received heartbeat ack")
                }
                else -> {
                    Log.d(TAG, "Unknown command: $cmd")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse text message", e)
        }
    }
    
    /**
     * 处理二进制消息
     */
    private fun handleBinaryMessage(bytes: ByteArray) {
        try {
            val message = WebSocketMessageParser.parseWebSocketMessage(bytes)
            if (message != null) {
                Log.d(TAG, "Parsed WebSocket message: ${message.cmd}")
                _messages.tryEmit(message)
                
                    // 如果是推送消息，更新会话列表
                    if (message.cmd == "push_message" && message.data != null) {
                        val msg = message.data["message"] as? com.yhchat.canary.data.model.Message
                        if (msg != null) {
                            // 这里可以更新会话列表的最后一条消息
                            Log.d(TAG, "New message in chat ${msg.sender.chatId}: ${msg.content.text}")
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse binary message", e)
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        isConnected = false
        stopHeartbeat()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }
    
    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = isConnected
}

/**
 * WebSocket消息数据类
 */
data class WebSocketMessage(
    val cmd: String,
    val data: Any? = null,
    val timestamp: Long = System.currentTimeMillis()
)