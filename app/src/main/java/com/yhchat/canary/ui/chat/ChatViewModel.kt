package com.yhchat.canary.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.*
import com.yhchat.canary.data.repository.MessageRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.websocket.WebSocketManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 聊天ViewModel
 */
class ChatViewModel : ViewModel() {
    
    private val messageRepository = MessageRepository()
    private val webSocketManager = WebSocketManager.getInstance()
    
    fun setTokenRepository(tokenRepository: TokenRepository) {
        messageRepository.setTokenRepository(tokenRepository)
    }
    
    // UI状态
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // 消息列表
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    // 当前聊天信息
    private val _chatInfo = MutableStateFlow<ChatInfo?>(null)
    val chatInfo: StateFlow<ChatInfo?> = _chatInfo.asStateFlow()
    
    init {
        observeWebSocketMessages()
    }
    
    /**
     * 初始化聊天
     */
    fun initChat(token: String, chatId: String, chatType: Int, chatName: String) {
        _chatInfo.value = ChatInfo(chatId, chatType, chatName)
        loadMessages(token, chatId, chatType)
    }
    
    /**
     * 加载消息
     */
    fun loadMessages(token: String, chatId: String, chatType: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            messageRepository.getMessages(token, chatId, chatType, 20)
                .onSuccess { messageList ->
                    _messages.value = messageList.reversed() // 反转以显示最新消息在底部
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * 发送文本消息
     */
    fun sendTextMessage(token: String, text: String, quoteMsgId: String? = null) {
        val chatInfo = _chatInfo.value ?: return
        if (text.isBlank()) return
        
        viewModelScope.launch {
            messageRepository.sendTextMessage(
                token = token,
                chatId = chatInfo.chatId,
                chatType = chatInfo.chatType,
                text = text,
                quoteMsgId = quoteMsgId
            ).onSuccess {
                // 发送成功，刷新消息列表
                loadMessages(token, chatInfo.chatId, chatInfo.chatType)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }
    
    /**
     * 发送图片消息
     */
    fun sendImageMessage(token: String, imageKey: String, width: Long? = null, height: Long? = null) {
        val chatInfo = _chatInfo.value ?: return
        
        viewModelScope.launch {
            messageRepository.sendImageMessage(
                token = token,
                chatId = chatInfo.chatId,
                chatType = chatInfo.chatType,
                imageKey = imageKey,
                width = width,
                height = height
            ).onSuccess {
                // 发送成功，刷新消息列表
                loadMessages(token, chatInfo.chatId, chatInfo.chatType)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }
    
    /**
     * 发送语音消息
     */
    fun sendAudioMessage(token: String, audioKey: String, audioTime: Long) {
        val chatInfo = _chatInfo.value ?: return
        
        viewModelScope.launch {
            messageRepository.sendAudioMessage(
                token = token,
                chatId = chatInfo.chatId,
                chatType = chatInfo.chatType,
                audioKey = audioKey,
                audioTime = audioTime
            ).onSuccess {
                // 发送成功，刷新消息列表
                loadMessages(token, chatInfo.chatId, chatInfo.chatType)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }
    
    /**
     * 编辑消息
     */
    fun editMessage(token: String, msgId: String, newContent: MessageContent) {
        val chatInfo = _chatInfo.value ?: return
        
        viewModelScope.launch {
            messageRepository.editMessage(
                token = token,
                msgId = msgId,
                chatId = chatInfo.chatId,
                chatType = chatInfo.chatType,
                contentType = MessageType.TEXT.value,
                content = newContent
            ).onSuccess {
                // 编辑成功，刷新消息列表
                loadMessages(token, chatInfo.chatId, chatInfo.chatType)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }
    
    /**
     * 撤回消息
     */
    fun recallMessage(token: String, msgId: String) {
        val chatInfo = _chatInfo.value ?: return
        
        viewModelScope.launch {
            messageRepository.recallMessage(
                token = token,
                msgId = msgId,
                chatId = chatInfo.chatId,
                chatType = chatInfo.chatType
            ).onSuccess {
                // 撤回成功，刷新消息列表
                loadMessages(token, chatInfo.chatId, chatInfo.chatType)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }
    
    /**
     * 观察WebSocket消息
     */
    private fun observeWebSocketMessages() {
        viewModelScope.launch {
            webSocketManager.messages.collect { message ->
                // 检查是否是当前聊天的消息
                val chatInfo = _chatInfo.value
                if (chatInfo != null && isMessageForCurrentChat(message, chatInfo)) {
                    // 添加到消息列表
                    val msg = message.data?.get("message") as? com.yhchat.canary.data.model.Message
                    if (msg != null) {
                        _messages.value = _messages.value + msg
                    }
                }
            }
        }
    }
    
    /**
     * 检查消息是否属于当前聊天
     */
    private fun isMessageForCurrentChat(message: com.yhchat.canary.data.model.WebSocketMessage, chatInfo: ChatInfo): Boolean {
        // 这里需要根据消息的发送者或聊天ID来判断
        // 简化处理，实际项目中需要更复杂的逻辑
        return true
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 加载更多消息
     */
    fun loadMoreMessages(token: String) {
        val chatInfo = _chatInfo.value ?: return
        val messages = _messages.value
        if (messages.isEmpty()) return
        
        val oldestMessage = messages.first()
        
        viewModelScope.launch {
            messageRepository.getMessages(
                token = token,
                chatId = chatInfo.chatId,
                chatType = chatInfo.chatType,
                msgCount = 20,
                msgId = oldestMessage.msgId
            ).onSuccess { newMessages ->
                _messages.value = (newMessages.reversed() + messages).distinctBy { it.msgId }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }
}

/**
 * 聊天UI状态
 */
data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSending: Boolean = false
)

/**
 * 聊天信息
 */
data class ChatInfo(
    val chatId: String,
    val chatType: Int,
    val chatName: String
)
