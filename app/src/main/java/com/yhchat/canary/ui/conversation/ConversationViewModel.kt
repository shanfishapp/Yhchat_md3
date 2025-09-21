package com.yhchat.canary.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.Conversation
import com.yhchat.canary.data.repository.ConversationRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.CacheRepository
import com.yhchat.canary.data.websocket.WebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 会话列表ViewModel
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val cacheRepository: CacheRepository
) : ViewModel() {

    private val webSocketManager = WebSocketManager.getInstance()

    init {
        // 启动WebSocket观察
        observeWebSocketConversations()
        observeWebSocketMessages()
        
        // 立即加载缓存数据
        loadCachedConversations()
    }

    fun setTokenRepository(tokenRepository: TokenRepository) {
        conversationRepository.setTokenRepository(tokenRepository)
    }

    // UI状态
    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    // 会话列表
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()
    
    /**
     * 立即加载缓存数据
     */
    private fun loadCachedConversations() {
        viewModelScope.launch {
            cacheRepository.getCachedConversations().collect { cachedConversations ->
                if (cachedConversations.isNotEmpty()) {
                    _conversations.value = cachedConversations
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }
    
    /**
     * 加载会话列表（从网络，并缓存）
     */
    fun loadConversations(token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            conversationRepository.getConversations()
                .onSuccess { conversationList ->
                    _conversations.value = conversationList
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    
                    // 缓存到本地数据库
                    cacheRepository.cacheConversations(conversationList)
                }
                .onFailure { error ->
                    // 网络失败时，尝试使用缓存数据
                    val cachedConversations = cacheRepository.getCachedConversationsSync()
                    if (cachedConversations.isNotEmpty()) {
                        _conversations.value = cachedConversations
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
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
                        val msg = message.data?.get("message") as? com.yhchat.canary.data.model.ChatMessage
                        if (msg != null) {
                            updateConversationWithNewMessage(msg)
                        }
                    }
                    "edit_message" -> {
                        // 消息被编辑，更新会话列表
                        val msg = message.data?.get("message") as? com.yhchat.canary.data.model.ChatMessage
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
    private fun updateConversationWithNewMessage(message: com.yhchat.canary.data.model.ChatMessage) {
        viewModelScope.launch {
            // 缓存消息到数据库
            cacheRepository.cacheMessage(message)
            
            val currentConversations = _conversations.value.toMutableList()
            val conversationIndex = currentConversations.indexOfFirst { it.chatId == message.sender.chatId }
            
            if (conversationIndex >= 0) {
                // 更新现有会话
                val conversation = currentConversations[conversationIndex]
                val updatedConversation = conversation.copy(
                    chatContent = message.content.text ?: "",
                    timestampMs = message.sendTime,
                    // 如果开启免打扰，不增加未读计数
                    unreadMessage = if (conversation.doNotDisturb == 1) {
                        conversation.unreadMessage
                    } else {
                        conversation.unreadMessage + 1
                    }
                )
                currentConversations[conversationIndex] = updatedConversation
                
                // 更新缓存中的会话
                cacheRepository.updateConversationLastMessage(
                    message.sender.chatId, 
                    message.content.text ?: "", 
                    message.sendTime
                )
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
                
                // 缓存新会话
                cacheRepository.cacheConversations(listOf(newConversation))
            }
            
            // 按时间排序
            currentConversations.sortByDescending { it.timestampMs }
            _conversations.value = currentConversations
        }
    }
    
    /**
     * 用编辑的消息更新会话列表
     */
    private fun updateConversationWithEditedMessage(message: com.yhchat.canary.data.model.ChatMessage) {
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
