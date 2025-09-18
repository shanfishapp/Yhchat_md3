package com.yhchat.canary.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.Conversation
import com.yhchat.canary.data.repository.ConversationRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.websocket.WebSocketManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 会话列表ViewModel
 */
class ConversationViewModel : ViewModel() {
    
    private val conversationRepository = ConversationRepository()
    private val webSocketManager = WebSocketManager.getInstance()
    
    fun setTokenRepository(tokenRepository: TokenRepository) {
        conversationRepository.setTokenRepository(tokenRepository)
    }
    
    // UI状态
    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()
    
    // 会话列表
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()
    
    init {
        observeWebSocketConversations()
        observeWebSocketMessages()
    }
    
    /**
     * 加载会话列表
     */
    fun loadConversations(token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            conversationRepository.getConversations()
                .onSuccess { conversationList ->
                    _conversations.value = conversationList
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
     * 设置会话为已读
     */
    fun markAsRead(token: String, chatId: String) {
        viewModelScope.launch {
            conversationRepository.dismissNotification(chatId)
                .onSuccess {
                    // 更新本地会话状态
                    _conversations.value = _conversations.value.map { conversation ->
                        if (conversation.chatId == chatId) {
                            conversation.copy(unreadMessage = 0, at = 0)
                        } else {
                            conversation
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }
    
    /**
     * 观察WebSocket会话更新
     */
    private fun observeWebSocketConversations() {
        viewModelScope.launch {
            webSocketManager.conversations.collect { updatedConversations ->
                if (updatedConversations.isNotEmpty()) {
                    _conversations.value = updatedConversations
                }
            }
        }
    }
    
    /**
     * 观察WebSocket消息
     */
    private fun observeWebSocketMessages() {
        viewModelScope.launch {
            webSocketManager.messages.collect { message ->
                when (message.cmd) {
                    "push_message" -> {
                        // 新消息到达，更新会话列表
                        val msg = message.data?.get("message") as? com.yhchat.canary.data.model.Message
                        if (msg != null) {
                            updateConversationWithNewMessage(msg)
                        }
                    }
                    "edit_message" -> {
                        // 消息被编辑，更新会话列表
                        val msg = message.data?.get("message") as? com.yhchat.canary.data.model.Message
                        if (msg != null) {
                            updateConversationWithEditedMessage(msg)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 用新消息更新会话列表
     */
    private fun updateConversationWithNewMessage(message: com.yhchat.canary.data.model.Message) {
        val currentConversations = _conversations.value.toMutableList()
        val conversationIndex = currentConversations.indexOfFirst { it.chatId == message.sender.chatId }
        
        if (conversationIndex >= 0) {
            // 更新现有会话
            val conversation = currentConversations[conversationIndex]
            val updatedConversation = conversation.copy(
                chatContent = message.content.text ?: "",
                timestampMs = message.sendTime,
                unreadMessage = conversation.unreadMessage + 1
            )
            currentConversations[conversationIndex] = updatedConversation
        } else {
            // 创建新会话
            val newConversation = Conversation(
                chatId = message.sender.chatId,
                chatType = message.sender.chatType,
                name = message.sender.name,
                chatContent = message.content.text ?: "",
                timestampMs = message.sendTime,
                unreadMessage = 1,
                at = 0,
                avatarUrl = message.sender.avatarUrl,
                timestamp = message.sendTime / 1000
            )
            currentConversations.add(0, newConversation)
        }
        
        // 按时间排序
        currentConversations.sortByDescending { it.timestampMs }
        _conversations.value = currentConversations
    }
    
    /**
     * 用编辑的消息更新会话列表
     */
    private fun updateConversationWithEditedMessage(message: com.yhchat.canary.data.model.Message) {
        val currentConversations = _conversations.value.toMutableList()
        val conversationIndex = currentConversations.indexOfFirst { it.chatId == message.sender.chatId }
        
        if (conversationIndex >= 0) {
            val conversation = currentConversations[conversationIndex]
            val updatedConversation = conversation.copy(
                chatContent = message.content.text ?: "",
                timestampMs = message.sendTime
            )
            currentConversations[conversationIndex] = updatedConversation
            _conversations.value = currentConversations
        }
    }
    
    /**
     * 启动WebSocket连接
     */
    fun startWebSocket(token: String, userId: String) {
        webSocketManager.connect(token, userId)
    }
    
    /**
     * 停止WebSocket连接
     */
    fun stopWebSocket() {
        webSocketManager.disconnect()
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 刷新会话列表
     */
    fun refreshConversations(token: String) {
        loadConversations(token)
    }
}

/**
 * 会话UI状态
 */
data class ConversationUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
