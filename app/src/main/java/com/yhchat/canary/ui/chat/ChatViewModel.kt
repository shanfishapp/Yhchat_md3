package com.yhchat.canary.ui.chat

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.ChatMessage
import com.yhchat.canary.data.model.GroupMemberInfo
import com.yhchat.canary.data.repository.GroupRepository
import com.yhchat.canary.data.repository.MessageRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.websocket.WebSocketManager
import com.yhchat.canary.data.websocket.MessageEvent
import com.yhchat.canary.data.local.ReadPositionStore
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
    val isRefreshing: Boolean = false,
    val newMessageReceived: Boolean = false,  // 标记是否收到新消息
    val groupMembers: Map<String, GroupMemberInfo> = emptyMap()  // 群成员信息：chatId -> GroupMemberInfo
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val tokenRepository: TokenRepository,
    private val webSocketManager: WebSocketManager,
    private val groupRepository: GroupRepository,
    private val readPositionStore: ReadPositionStore
) : ViewModel() {

    private var currentChatId: String = ""
    private var currentChatType: Int = 1
    private var currentUserId: String = ""
    private var hasMoreMessages: Boolean = true
    private var oldestMsgSeq: Long = 0
    private var oldestMsgId: String? = null

    private val tag = "ChatViewModel"

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages
    
    // 流式消息缓存：msgId -> 累积的content
    private val streamingMessages = mutableMapOf<String, String>()

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
        oldestMsgId = null
        
        // 如果是群聊，加载群成员信息
        if (chatType == 2) {
            loadGroupMembers(chatId)
        }
        
        // 开始监听WebSocket消息
        startListeningToWebSocketMessages()
        
        // 始终加载最新消息（移除跳转到上次读取位置功能）
        Log.d(tag, "Loading latest messages")
        loadMessages()
    }
    
    /**
     * 加载群成员信息
     */
    private fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            Log.d(tag, "Loading group members for: $groupId")
            groupRepository.setTokenRepository(tokenRepository)
            
            // 加载群信息
            groupRepository.getGroupInfo(groupId).fold(
                onSuccess = { groupInfo ->
                    Log.d(tag, "Group info loaded, fetching members...")
                    
                    // 加载所有成员（分页获取）
                    val allMembers = mutableListOf<GroupMemberInfo>()
                    var currentPage = 1
                    var hasMore = true
                    
                    while (hasMore) {
                        groupRepository.getGroupMembers(groupId, page = currentPage, size = 50).fold(
                            onSuccess = { members ->
                                allMembers.addAll(members)
                                hasMore = members.size >= 50
                                currentPage++
                                Log.d(tag, "Loaded ${members.size} members, total: ${allMembers.size}")
                            },
                            onFailure = { error ->
                                Log.e(tag, "Failed to load group members page $currentPage", error)
                                hasMore = false
                            }
                        )
                    }
                    
                    // 转换为 Map: userId -> GroupMemberInfo
                    val membersMap: Map<String, GroupMemberInfo> = allMembers.associateBy { member ->
                        member.userId
                    }
                    _uiState.value = _uiState.value.copy(groupMembers = membersMap)
                    Log.d(tag, "Group members loaded: ${membersMap.size} members")
                },
                onFailure = { error ->
                    Log.e(tag, "Failed to load group info", error)
                }
            )
        }
    }
    
    /**
     * 开始监听WebSocket实时消息
     */
    private fun startListeningToWebSocketMessages() {
        viewModelScope.launch {
            webSocketManager.getMessageEvents().collect { event ->
                when (event) {
                    is MessageEvent.NewMessage -> {
                        handleNewMessage(event.message)
                    }
                    is MessageEvent.MessageEdited -> {
                        handleEditedMessage(event.message)
                    }
                    is MessageEvent.MessageDeleted -> {
                        handleDeletedMessage(event.msgId)
                    }
                    is MessageEvent.StreamMessage -> {
                        handleStreamMessage(event)
                    }
                    else -> {
                        // 忽略其他事件类型
                    }
                }
            }
        }
    }
    
    /**
     * 处理新消息
     */
    private fun handleNewMessage(message: ChatMessage) {
        // 判断消息是否属于当前聊天
        val isPrivateChat = message.chatId == message.recvId
        val targetChatId = if (isPrivateChat) message.sender.chatId else message.chatId
        
        if (targetChatId == currentChatId) {
            // 检查消息是否已存在，避免重复添加
            val existingIndex = _messages.indexOfFirst { it.msgId == message.msgId }
            if (existingIndex == -1) {
                // 按时间排序插入新消息
                val insertIndex = _messages.indexOfLast { it.sendTime <= message.sendTime } + 1
                _messages.add(insertIndex, message)
                Log.d(tag, "Inserted new real-time message at index $insertIndex: ${message.msgId}")
                
                // 标记收到新消息，触发UI更新
                _uiState.value = _uiState.value.copy(newMessageReceived = true)
                
                // 初始化流式消息缓存（如果是机器人消息，准备接收stream_message）
                if (message.sender.chatType == 3) {
                    streamingMessages[message.msgId] = message.content.text ?: ""
                    Log.d(tag, "Initialized streaming cache for bot message: ${message.msgId}")
                }
            }
        }
    }
    
    /**
     * 重置新消息标记
     */
    fun resetNewMessageFlag() {
        _uiState.value = _uiState.value.copy(newMessageReceived = false)
    }
    
    /**
     * 获取消息编辑历史
     */
    suspend fun getMessageEditHistory(msgId: String): Result<List<com.yhchat.canary.data.model.MessageEditRecord>> {
        return messageRepository.getMessageEditHistory(msgId)
    }
    
    /**
     * 添加表情到个人收藏
     */
    fun addExpressionToFavorites(expressionId: String) {
        viewModelScope.launch {
            try {
                val result = messageRepository.addExpressionToFavorites(expressionId)
                result.fold(
                    onSuccess = {
                        Log.d(tag, "添加表情成功: $expressionId")
                    },
                    onFailure = { error ->
                        Log.e(tag, "添加表情失败: ${error.message}")
                        _uiState.value = _uiState.value.copy(error = "添加表情失败: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "添加表情异常", e)
                _uiState.value = _uiState.value.copy(error = "添加表情异常: ${e.message}")
            }
        }
    }
    
    /**
     * 处理编辑的消息
     */
    private fun handleEditedMessage(message: ChatMessage) {
        val isPrivateChat = message.chatId == message.recvId
        val targetChatId = if (isPrivateChat) message.sender.chatId else message.chatId
        
        if (targetChatId == currentChatId) {
            val existingIndex = _messages.indexOfFirst { it.msgId == message.msgId }
            if (existingIndex != -1) {
                _messages[existingIndex] = message
                Log.d(tag, "Updated edited message: ${message.msgId}")
            }
        }
    }
    
    /**
     * 处理删除的消息
     */
    private fun handleDeletedMessage(messageId: String) {
        val existingIndex = _messages.indexOfFirst { it.msgId == messageId }
        if (existingIndex != -1) {
            _messages.removeAt(existingIndex)
            Log.d(tag, "Removed deleted message: $messageId")
        }
    }
    
    /**
     * 处理流式消息
     */
    private fun handleStreamMessage(event: MessageEvent.StreamMessage) {
        // 判断消息是否属于当前聊天
        if (event.chatId != currentChatId) {
            return
        }
        
        Log.d(tag, "Handling stream message: msgId=${event.msgId}, content=${event.content}")
        
        // 查找是否已有此消息
        val existingIndex = _messages.indexOfFirst { it.msgId == event.msgId }
        
        if (existingIndex == -1) {
            // 消息不存在，先创建基础消息（push_message的作用）
            val baseMessage = ChatMessage(
                msgId = event.msgId,
                sender = com.yhchat.canary.data.model.MessageSender(
                    chatId = event.chatId,
                    chatType = 3,
                    name = "机器人",
                    avatarUrl = "",
                    tagOld = emptyList(),
                    tag = emptyList()
                ),
                direction = "left",
                contentType = 1,
                content = com.yhchat.canary.data.model.MessageContent(
                    text = event.content
                ),
                sendTime = System.currentTimeMillis(),
                cmd = null,
                msgDeleteTime = null,
                quoteMsgId = null,
                msgSeq = null,
                editTime = null,
                chatId = event.chatId,
                chatType = 3,
                recvId = event.recvId
            )
            
            val insertIndex = _messages.indexOfLast { it.sendTime <= baseMessage.sendTime } + 1
            _messages.add(insertIndex, baseMessage)
            streamingMessages[event.msgId] = event.content
            Log.d(tag, "Created base message for stream at index $insertIndex")
        } else {
            // 消息已存在，追加内容
            val accumulatedContent = streamingMessages.getOrDefault(event.msgId, "") + event.content
            streamingMessages[event.msgId] = accumulatedContent
            
            val existingMessage = _messages[existingIndex]
            val updatedMessage = existingMessage.copy(
                content = existingMessage.content.copy(text = accumulatedContent)
            )
            _messages[existingIndex] = updatedMessage
            Log.d(tag, "Updated stream message at index $existingIndex")
        }
    }

    /**
     * 从指定位置加载消息（用于恢复上次阅读位置）
     */
    private fun loadMessagesFromPosition(msgId: String) {
        if (currentChatId.isEmpty()) {
            Log.w(tag, "Chat not initialized")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // 加载指定消息及前后20条消息
                val result = messageRepository.getMessages(
                    chatId = currentChatId,
                    chatType = currentChatType,
                    msgCount = 40,  // 加载更多消息以提供上下文
                    msgId = msgId
                )

                result.fold(
                    onSuccess = { newMessages ->
                        Log.d(tag, "Loaded ${newMessages.size} messages from position $msgId")
                        
                        _messages.clear()
                        _messages.addAll(newMessages.sortedBy { it.sendTime })

                        // 更新最旧消息的序列号和ID
                        if (newMessages.isNotEmpty()) {
                            oldestMsgSeq = newMessages.minOfOrNull { it.msgSeq ?: 0L } ?: 0L
                            val oldestMessage = newMessages.minByOrNull { it.sendTime }
                            if (oldestMessage != null) {
                                oldestMsgId = oldestMessage.msgId
                            }
                        }

                        hasMoreMessages = newMessages.size >= 40

                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to load messages from position", exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "加载消息失败: ${exception.message}"
                        )
                        // 失败则加载最新消息
                        loadMessages()
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Exception loading messages from position", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载消息异常: ${e.message}"
                )
                // 异常则加载最新消息
                loadMessages()
            }
        }
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

                val result = if (!oldestMsgId.isNullOrEmpty() && !refresh) {
                    // 加载更多历史消息 - 使用最老的消息ID
                    messageRepository.getMessages(
                        chatId = currentChatId,
                        chatType = currentChatType,
                        msgCount = 20,
                        msgId = oldestMsgId
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

                        // 更新最旧消息的序列号和ID
                        if (newMessages.isNotEmpty()) {
                            oldestMsgSeq = newMessages.minOfOrNull { it.msgSeq ?: 0L } ?: oldestMsgSeq
                            // 找到发送时间最早的消息ID作为oldestMsgId
                            val oldestMessage = newMessages.minByOrNull { it.sendTime }
                            if (oldestMessage != null) {
                                oldestMsgId = oldestMessage.msgId
                                Log.d(tag, "Updated oldestMsgId to: $oldestMsgId, sendTime: ${oldestMessage.sendTime}")
                            }
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
        
        Log.d(tag, "Loading more messages from msgId: $oldestMsgId, seq: $oldestMsgSeq")
        loadMessages(refresh = false)
    }

    /**
     * 发送文本消息
     */
    fun sendTextMessage(text: String, quoteMsgId: String? = null) {
        sendMessage(text, 1, quoteMsgId)
    }
    
    /**
     * 发送消息（支持不同类型）
     * @param text 消息文本
     * @param contentType 消息类型：1-文本，3-Markdown，8-HTML
     * @param quoteMsgId 引用消息ID
     * @param quoteMsgText 引用消息文本
     */
    fun sendMessage(
        text: String, 
        contentType: Int = 1, 
        quoteMsgId: String? = null,
        quoteMsgText: String? = null
    ) {
        if (text.isBlank() || currentChatId.isEmpty()) {
            Log.w(tag, "Cannot send empty message or chat not initialized")
            return
        }
        
        viewModelScope.launch {
            try {
                val typeText = when (contentType) {
                    3 -> "Markdown"
                    8 -> "HTML"
                    else -> "文本"
                }
                Log.d(tag, "Sending $typeText message: $text${if (!quoteMsgId.isNullOrEmpty()) " (引用消息)" else ""}")
                
                val result = messageRepository.sendMessage(
                    chatId = currentChatId,
                    chatType = currentChatType,
                    text = text,
                    contentType = contentType,
                    quoteMsgId = quoteMsgId,
                    quoteMsgText = quoteMsgText
                )

                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            Log.d(tag, "$typeText message sent successfully")
                            // 发送成功后刷新消息列表以获取最新消息
                            loadMessages(refresh = true)
                        }
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to send $typeText message", exception)
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
     * 发送草稿输入（输入框内容变化时调用）
     */
    fun sendDraftInput(inputText: String) {
        if (currentChatId.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    webSocketManager.sendDraftInput(currentChatId, inputText)
                    Log.d(tag, "Sent draft input for chat: $currentChatId")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to send draft input", e)
                }
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
     * 保存当前读取位置（退出聊天时调用）
     */
    fun saveCurrentReadPosition() {
        if (currentChatId.isEmpty() || _messages.isEmpty()) {
            return
        }
        
        // 取最新的消息作为读取位置
        val latestMessage = _messages.maxByOrNull { it.sendTime }
        if (latestMessage != null && latestMessage.msgSeq != null) {
            readPositionStore.saveReadPosition(
                chatId = currentChatId,
                chatType = currentChatType,
                msgId = latestMessage.msgId,
                msgSeq = latestMessage.msgSeq!!
            )
            Log.d(tag, "Saved read position: msgId=${latestMessage.msgId}, msgSeq=${latestMessage.msgSeq}")
        }
    }
    
    /**
     * 获取当前会话的未读消息数
     * @return 未读消息数，如果无法计算则返回null
     */
    fun getUnreadCount(): Int? {
        if (currentChatId.isEmpty()) {
            return null
        }
        
        val readPosition = readPositionStore.getReadPosition(currentChatId, currentChatType)
        if (readPosition == null || _messages.isEmpty()) {
            return null
        }
        
        // 取最新消息的 msgSeq
        val latestMsgSeq = _messages.maxOfOrNull { it.msgSeq ?: 0L } ?: return null
        val lastReadMsgSeq = readPosition.second
        
        // 计算未读数
        val unreadCount = (latestMsgSeq - lastReadMsgSeq).toInt()
        return if (unreadCount > 0) unreadCount else 0
    }
    
    /**
     * 刷新消息
     */
    fun refreshMessages() {
        Log.d(tag, "Refreshing messages")
        oldestMsgSeq = 0
        oldestMsgId = null
        hasMoreMessages = true
        loadMessages(refresh = true)
    }
    
    /**
     * 刷新最新消息（下拉刷新使用）
     * 调用 list-msg API 获取最新的消息
     */
    fun refreshLatestMessages() {
        if (currentChatId.isEmpty()) {
            Log.w(tag, "Chat not initialized")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

                // 不带 msgId 参数，获取最新的20条消息
                val result = messageRepository.getMessages(
                    chatId = currentChatId,
                    chatType = currentChatType,
                    msgCount = 20
                )

                result.fold(
                    onSuccess = { newMessages ->
                        Log.d(tag, "Refreshed ${newMessages.size} latest messages")
                        
                        if (newMessages.isNotEmpty()) {
                            // 合并新消息到现有消息列表
                            val existingMsgIds = _messages.map { it.msgId }.toSet()
                            val uniqueNewMessages = newMessages.filter { it.msgId !in existingMsgIds }
                            
                            if (uniqueNewMessages.isNotEmpty()) {
                                // 添加不重复的新消息
                                _messages.addAll(uniqueNewMessages.sortedBy { it.sendTime })
                                Log.d(tag, "Added ${uniqueNewMessages.size} new messages")
                            }
                            
                            // 按发送时间重新排序所有消息
                            val sortedMessages = _messages.sortedBy { it.sendTime }
                            _messages.clear()
                            _messages.addAll(sortedMessages)
                        }

                        _uiState.value = _uiState.value.copy(isRefreshing = false, error = null)
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to refresh latest messages", exception)
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            error = "刷新失败: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Exception refreshing latest messages", e)
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "刷新异常: ${e.message}"
                )
            }
        }
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
    
    /**
     * 检查消息是否正在流式接收中
     */
    fun isMessageStreaming(msgId: String): Boolean {
        return streamingMessages.containsKey(msgId)
    }
    
    /**
     * 清除流式消息缓存（当流式消息完成时调用）
     */
    fun clearStreamingMessage(msgId: String) {
        streamingMessages.remove(msgId)
        Log.d(tag, "Cleared streaming message: $msgId")
    }
    
    /**
     * 上报按钮点击事件
     */
    fun reportButtonClick(
        chatId: String,
        chatType: Int,
        msgId: String,
        buttonValue: String
    ) {
        viewModelScope.launch {
            try {
                val userId = tokenRepository.getUserId() ?: ""
                messageRepository.reportButtonClick(
                    chatId = chatId,
                    chatType = chatType,
                    msgId = msgId,
                    userId = userId,
                    buttonValue = buttonValue
                )
                Log.d(tag, "Button click reported successfully: msgId=$msgId, value=$buttonValue")
            } catch (e: Exception) {
                Log.e(tag, "Failed to report button click", e)
            }
        }
    }
    
    /**
     * ViewModel被清理时保存读取位置
     */
    override fun onCleared() {
        super.onCleared()
        // 保存当前读取位置，防止用户从后台直接结束应用
        saveCurrentReadPosition()
        Log.d(tag, "ChatViewModel cleared, read position saved")
    }
}