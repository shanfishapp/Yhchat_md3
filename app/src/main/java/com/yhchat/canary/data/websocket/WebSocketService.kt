package com.yhchat.canary.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.yhchat.canary.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.*

/**
 * WebSocket服务
 */
class WebSocketService {
    
    companion object {
        private const val TAG = "WebSocketService"
        private const val WS_URL = "wss://chat-ws-go.jwzhd.com/ws"
        private const val HEARTBEAT_INTERVAL = 30000L // 30秒
    }
    
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var heartbeatJob: Job? = null
    private val gson = Gson()
    private val okHttpClient = OkHttpClient()
    
    // 消息流
    private val _messageFlow = MutableSharedFlow<WebSocketMessage>()
    val messageFlow: SharedFlow<WebSocketMessage> = _messageFlow.asSharedFlow()
    
    // 连接状态流
    private val _connectionStateFlow = MutableSharedFlow<Boolean>()
    val connectionStateFlow: SharedFlow<Boolean> = _connectionStateFlow.asSharedFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 连接WebSocket
     */
    fun connect(token: String, userId: String, deviceId: String = UUID.randomUUID().toString()) {
        if (isConnected) {
            Log.w(TAG, "WebSocket already connected")
            return
        }
        
        try {
            val request = Request.Builder()
                .url(WS_URL)
                .build()
            
            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected")
                    isConnected = true
                    coroutineScope.launch {
                        _connectionStateFlow.emit(true)
                    }
                    
                    // 发送登录消息
                    sendLoginMessage(token, userId, deviceId)
                    
                    // 启动心跳
                    startHeartbeat()
                }
                
                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "Received message: $text")
                    handleMessage(text)
                }
                
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closing: $code, $reason")
                    isConnected = false
                    coroutineScope.launch {
                        _connectionStateFlow.emit(false)
                    }
                    stopHeartbeat()
                }
                
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $code, $reason")
                    isConnected = false
                    coroutineScope.launch {
                        _connectionStateFlow.emit(false)
                    }
                    stopHeartbeat()
                }
                
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket error", t)
                    isConnected = false
                    coroutineScope.launch {
                        _connectionStateFlow.emit(false)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect WebSocket", e)
            isConnected = false
            coroutineScope.launch {
                _connectionStateFlow.emit(false)
            }
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        stopHeartbeat()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        isConnected = false
        coroutineScope.launch {
            _connectionStateFlow.emit(false)
        }
    }
    
    /**
     * 发送登录消息
     */
    private fun sendLoginMessage(token: String, userId: String, deviceId: String) {
        val loginMessage = LoginMessage(
            seq = generateSeq(),
            data = WebSocketLoginData(
                userId = userId,
                token = token,
                platform = "android",
                deviceId = deviceId
            )
        )
        sendMessage(loginMessage)
    }
    
    /**
     * 启动心跳
     */
    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = coroutineScope.launch {
            while (isActive && isConnected) {
                delay(HEARTBEAT_INTERVAL)
                if (isConnected) {
                    val heartbeatMessage = HeartbeatMessage(seq = generateSeq())
                    sendMessage(heartbeatMessage)
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
     * 发送消息
     */
    private fun sendMessage(message: Any) {
        try {
            val json = gson.toJson(message)
            Log.d(TAG, "Sending message: $json")
            webSocket?.send(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private fun handleMessage(message: String) {
        try {
            val webSocketMessage = gson.fromJson(message, WebSocketMessage::class.java)
            coroutineScope.launch {
                _messageFlow.emit(webSocketMessage)
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse message: $message", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
        }
    }
    
    /**
     * 生成序列号
     */
    private fun generateSeq(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        disconnect()
        coroutineScope.cancel()
    }
}
