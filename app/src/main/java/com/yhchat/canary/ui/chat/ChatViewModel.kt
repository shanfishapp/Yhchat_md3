package com.yhchat.canary.ui.chat

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.ChatMessage
import com.yhchat.canary.data.repository.MessageRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private var currentChatId: String = ""
    private var currentChatType: Int = 1
    private var currentUserId: String = ""
    private var hasMoreMessages: Boolean = true
    private var oldestMsgSeq: Long = 0

    private val tag = "ChatViewModel"

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    /**
     * 初始化聊天
     */
    fun initChat(chatId: String, chatType: Int, userId: String) {
        currentChatId = chatId
        currentChatType = chatType
        currentUserId = userId
        
        Log.d(tag, "Initializing chat: chatId=$chatId, chatType=$chatType, userId=$userId")
        
        // 清空之前的消息
        _messages.clear()
        hasMoreMessages = true
        oldestMsgSeq = 0
        
        // 加载初始消息
        loadMessages()
    }

    /**
     * 加载消息列表
     */
    fun loadMessages(refresh: Boolean = false) {
        if (currentChatId.isEmpty()) {
            Log.w(tag, "Chat not initialized")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = !refresh,
                    isRefreshing = refresh,
                    error = null
                )

                val result = if (oldestMsgSeq > 0 && !refresh) {
                    // 加载更多历史消息
                    messageRepository.getMessagesBySeq(
                        chatId = currentChatId,
                        chatType = currentChatType,
                        msgSeq = oldestMsgSeq
                    )
                } else {
                    // 加载最新消息
                    messageRepository.getMessages(
                        chatId = currentChatId,
                        chatType = currentChatType,
                        msgCount = 20
                    )
                }

                result.fold(
                    onSuccess = { newMessages ->
                        Log.d(tag, "Loaded ${newMessages.size} messages")
                        
                        if (refresh) {
                            // 刷新时替换所有消息
                            _messages.clear()
                            _messages.addAll(newMessages.sortedBy { it.sendTime })
                        } else {
                            // 加载更多时添加到现有消息前面
                            val sortedNewMessages = newMessages.sortedBy { it.sendTime }
                            _messages.addAll(0, sortedNewMessages)
                        }

                        // 更新最旧消息的序列号
                        if (newMessages.isNotEmpty()) {
                            oldestMsgSeq = newMessages.minOfOrNull { it.msgSeq ?: 0L } ?: oldestMsgSeq
                        }

                        // 检查是否还有更多消息
                        hasMoreMessages = newMessages.size >= 20

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to load messages", exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = exception.message ?: "加载消息失败"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Error loading messages", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "加载消息失败"
                    )
                }
        }
    }
    
    /**
     * 加载更多历史消息
     */
    fun loadMoreMessages() {
        if (!hasMoreMessages || _uiState.value.isLoading) {
            Log.d(tag, "No more messages to load or already loading")
            return
        }
        
        Log.d(tag, "Loading more messages from seq: $oldestMsgSeq")
        loadMessages(refresh = false)
    }

    /**
     * 发送文本消息
     */
    fun sendTextMessage(text: String, quoteMsgId: String? = null) {
        if (text.isBlank() || currentChatId.isEmpty()) {
            Log.w(tag, "Cannot send empty message or chat not initialized")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d(tag, "Sending message: $text")
                
                val result = messageRepository.sendMessage(
                    chatId = currentChatId,
                    chatType = currentChatType,
                    text = text,
                    contentType = 1, // 文本消息
                    quoteMsgId = quoteMsgId
                )

                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            Log.d(tag, "Message sent successfully")
                            // 发送成功后刷新消息列表以获取最新消息
                            loadMessages(refresh = true)
                        }
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to send message", exception)
                        _uiState.value = _uiState.value.copy(
                            error = exception.message ?: "发送消息失败"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Error sending message", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "发送消息失败"
                )
            }
        }
    }
    
    /**
     * 添加新收到的消息（通过WebSocket）
     */
    fun addNewMessage(message: ChatMessage) {
        Log.d(tag, "Adding new message: ${message.msgId}")
        
        // 检查消息是否已存在
        val existingIndex = _messages.indexOfFirst { it.msgId == message.msgId }
        if (existingIndex != -1) {
            // 消息已存在，更新它
            _messages[existingIndex] = message
            Log.d(tag, "Updated existing message: ${message.msgId}")
        } else {
            // 新消息，按时间排序插入
            val insertIndex = _messages.indexOfLast { it.sendTime <= message.sendTime } + 1
            _messages.add(insertIndex, message)
            Log.d(tag, "Inserted new message at index: $insertIndex")
        }
    }
    
    /**
     * 更新消息（编辑后）
     */
    fun updateMessage(message: ChatMessage) {
        val index = _messages.indexOfFirst { it.msgId == message.msgId }
        if (index != -1) {
            _messages[index] = message
            Log.d(tag, "Updated message: ${message.msgId}")
        }
    }
    
    /**
     * 删除消息（撤回）
     */
    fun removeMessage(msgId: String) {
        val index = _messages.indexOfFirst { it.msgId == msgId }
        if (index != -1) {
            _messages.removeAt(index)
            Log.d(tag, "Removed message: $msgId")
        }
    }
    
    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 刷新消息
     */
    fun refreshMessages() {
        Log.d(tag, "Refreshing messages")
        oldestMsgSeq = 0
        hasMoreMessages = true
        loadMessages(refresh = true)
    }

    /**
     * 获取当前用户ID
     */
    fun getCurrentUserId(): String = currentUserId

    /**
     * 检查消息是否来自当前用户
     * 使用direction字段判断：right=自己发送，left=对方发送
     */
    fun isMyMessage(message: ChatMessage): Boolean {
        return message.direction == "right"
    }
}