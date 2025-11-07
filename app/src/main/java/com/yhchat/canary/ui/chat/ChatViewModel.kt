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
import com.yhchat.canary.proto.group.Bot_data
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job
import com.yhchat.canary.data.model.GroupDetail
import yh_bot.Bot

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val isRefreshing: Boolean = false,
    val newMessageReceived: Boolean = false,  // 标记是否收到新消息
    val groupInfo: GroupDetail? = null,
    val groupMembers: Map<String, GroupMemberInfo> = emptyMap(),  // 群成员信息：chatId -> GroupMemberInfo
    val groupMemberCount: Int = 0,  // 群成员总数
    val botInfo: Bot.bot_info? = null,  // 机器人信息
    val botBoard: Bot.board? = null,  // 机器人看板（单个机器人聊天）
    val groupBots: List<Bot_data> = emptyList(),  // 群聊中的机器人列表
    val groupBotBoards: Map<String, Bot.board.Board_data> = emptyMap(),  // 群聊机器人看板：botId -> board_data
    val chatBackgroundUrl: String? = null,  // 聊天背景图片URL
    val menuButtons: List<com.yhchat.canary.data.model.MenuButton> = emptyList()  // 群聊菜单按钮
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val tokenRepository: TokenRepository,
    private val webSocketManager: WebSocketManager,
    private val groupRepository: GroupRepository,
    private val readPositionStore: ReadPositionStore,
    private val apiService: com.yhchat.canary.data.api.ApiService,
    private val botRepository: com.yhchat.canary.data.repository.BotRepository
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
        
        // 如果是群聊，加载群成员信息、机器人列表和菜单按钮
        if (chatType == 2) { // 群聊
            loadGroupInfo(chatId)
            loadGroupMembers(chatId)
            loadGroupBotsAndBoards(chatId)
            loadGroupMenuButtons(chatId)
        } else if (chatType == 3) { // 机器人
            loadBotInfo(chatId)
            loadBotBoard(chatId, 3)
        }
        
        // 开始监听WebSocket消息
        startListeningToWebSocketMessages()
        
        // 始终加载最新消息（移除跳转到上次读取位置功能）
        Log.d(tag, "Loading latest messages")
        loadMessages()
    }
    
    /**
     * 加载群聊基本信息
     */
    private fun loadGroupInfo(groupId: String) {
        viewModelScope.launch {
            Log.d(tag, "Loading group info for: $groupId")
            groupRepository.setTokenRepository(tokenRepository)
            
            groupRepository.getGroupInfo(groupId).fold(
                onSuccess = { groupDetail ->
                    Log.d(tag, "✅ Group info loaded successfully")
                    _uiState.value = _uiState.value.copy(groupInfo = groupDetail)
                },
                onFailure = { error ->
                    Log.e(tag, "❌ Failed to load group info", error)
                    _uiState.value = _uiState.value.copy(error = "加载群信息失败: ${error.message}")
                }
            )
        }
    }

    /**
     * 加载群成员信息（加载群信息和部分成员以显示权限标签）
     */
    private fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            Log.d(tag, "Loading group info and members for: $groupId")
            groupRepository.setTokenRepository(tokenRepository)
            
            // 加载群信息以获取成员总数
            groupRepository.getGroupInfo(groupId).fold(
                onSuccess = { groupInfo ->
                    Log.d(tag, "Group info loaded, member count: ${groupInfo.memberCount}")
                    
                    // 只加载前2页成员（共100人）用于显示权限标签，避免加载所有成员
                    val membersToLoad = minOf(100, groupInfo.memberCount)
                    val pages = (membersToLoad + 49) / 50  // 向上取整
                    val allMembers = mutableListOf<GroupMemberInfo>()
                    
                    for (page in 1..pages) {
                        groupRepository.getGroupMembers(groupId, page = page, size = 50).fold(
                            onSuccess = { members ->
                                allMembers.addAll(members)
                                Log.d(tag, "Loaded page $page: ${members.size} members, total: ${allMembers.size}")
                            },
                            onFailure = { error ->
                                Log.e(tag, "Failed to load group members page $page", error)
                            }
                        )
                    }
                    
                    // 转换为 Map: userId -> GroupMemberInfo
                    val membersMap: Map<String, GroupMemberInfo> = allMembers.associateBy { it.userId }
                    
                    _uiState.value = _uiState.value.copy(
                        groupMembers = membersMap,
                        groupMemberCount = groupInfo.memberCount
                    )
                    Log.d(tag, "Group members loaded: ${membersMap.size} members (for permission display)")
                },
                onFailure = { error ->
                    Log.e(tag, "Failed to load group info", error)
                }
            )
        }
    }
    
    /**
     * 加载群聊中的机器人列表和他们的看板
     */
    private fun loadGroupBotsAndBoards(chatId: String) {
        viewModelScope.launch {
            groupRepository.getGroupBots(chatId).fold(
                onSuccess = { bots ->
                    _uiState.value = _uiState.value.copy(groupBots = bots)
                    if (bots.isNotEmpty()) {
                        // 只需要调用一次API，获取所有机器人的看板
                        loadGroupBotBoards(chatId)
                    }
                },
                onFailure = { error ->
                    Log.e(tag, "加载群聊机器人列表失败", error)
                }
            )
        }
    }

    /**
     * 加载群聊内所有机器人的看板
     * @param chatId 群聊ID
     */
    private fun loadGroupBotBoards(chatId: String) {
        viewModelScope.launch {
            Log.d(tag, "开始加载群聊机器人看板: groupId=$chatId")
            // 使用chatType=2获取群聊的看板
            botRepository.getBotBoard(chatId, 2).fold(
                onSuccess = { boardResponse ->
                    // boardResponse.boardList 现在是一个列表
                    val boardsDataList = boardResponse.boardList
                    Log.d(tag, "✅ 加载群聊看板成功: groupId=$chatId, 数量=${boardsDataList.size}")
                    if (boardsDataList.isNotEmpty()) {
                        // 使用机器人ID作为key，创建一个map
                        val boardsMap = boardsDataList.associateBy { it.botId }
                        _uiState.value = _uiState.value.copy(groupBotBoards = boardsMap)
                    }
                },
                onFailure = { error ->
                    Log.e(tag, "❌ 加载群聊机器人看板失败: groupId=$chatId", error)
                }
            )
        }
    }
    
    /**
     * 刷新单个机器人的看板（用于WebSocket更新）
     */
    fun refreshBotBoard(botId: String) {
        if (currentChatType == 2) {
            // 群聊场景
            loadGroupBotBoards(currentChatId)
        } else if (currentChatType == 3 && currentChatId == botId) {
            // 单个机器人聊天场景
            loadBotBoard(botId, 3)
        }
    }
    
    private fun loadGroupMenuButtons(groupId: String) {
        viewModelScope.launch {
            Log.d(tag, "Loading group menu buttons for: $groupId")
            groupRepository.setTokenRepository(tokenRepository)
            
            groupRepository.getGroupMenuButtons(groupId).fold(
                onSuccess = { menuButtons ->
                    Log.d(tag, "✅ 加载到 ${menuButtons.size} 个菜单按钮")
                    _uiState.value = _uiState.value.copy(menuButtons = menuButtons)
                },
                onFailure = { error ->
                    Log.e(tag, "❌ 加载菜单按钮失败", error)
                }
            )
        }
    }
    
    /**
     * 点击菜单按钮
     */
    fun clickMenuButton(button: com.yhchat.canary.data.model.MenuButton) {
        viewModelScope.launch {
            Log.d(tag, "点击菜单按钮: ${button.name} (ID: ${button.id})")
            
            groupRepository.clickMenuButton(
                buttonId = button.id,
                chatId = currentChatId,
                chatType = currentChatType,
                value = button.select
            ).fold(
                onSuccess = {
                    Log.d(tag, "✅ 菜单按钮点击成功")
                },
                onFailure = { error ->
                    Log.e(tag, "❌ 菜单按钮点击失败", error)
                    _uiState.value = _uiState.value.copy(error = error.message)
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
     * 上传并发送图片
     */
    fun uploadAndSendImage(
        context: android.content.Context,
        imageUri: android.net.Uri,
        quoteMsgId: String? = null,
        quoteMsgText: String? = null
    ) {
        viewModelScope.launch {
            try {
                Log.d(tag, "🖼️ 开始上传并发送图片: $imageUri")
                
                // 1. 获取七牛上传token
                val token = tokenRepository.getTokenSync()
                if (token.isNullOrEmpty()) {
                    Log.e(tag, "❌ Token为空")
                    _uiState.value = _uiState.value.copy(error = "未登录")
                    return@launch
                }
                
                Log.d(tag, "📤 获取七牛上传token...")
                val tokenResponse = apiService.getQiniuImageToken(token)
                
                if (!tokenResponse.isSuccessful || tokenResponse.body()?.code != 1) {
                    Log.e(tag, "❌ 获取上传token失败: ${tokenResponse.code()}")
                    _uiState.value = _uiState.value.copy(error = "获取上传token失败")
                    return@launch
                }
                
                val uploadToken = tokenResponse.body()?.data?.token ?: run {
                    Log.e(tag, "❌ 上传token为空")
                    _uiState.value = _uiState.value.copy(error = "获取上传token失败")
                    return@launch
                }
                
                Log.d(tag, "✅ 获取到上传token: ${uploadToken.take(20)}...")
                
                // 2. 上传图片到七牛云
                Log.d(tag, "📤 开始上传图片到七牛云...")
                val uploadResult = com.yhchat.canary.utils.ImageUploadUtil.uploadImage(
                    context = context,
                    imageUri = imageUri,
                    uploadToken = uploadToken
                )
                
                uploadResult.fold(
                    onSuccess = { uploadResponse ->
                        Log.d(tag, "✅ 图片上传成功！")
                        Log.d(tag, "   key: ${uploadResponse.key}")
                        Log.d(tag, "   hash: ${uploadResponse.hash}")
                        Log.d(tag, "   size: ${uploadResponse.fsize}")
                        Log.d(tag, "   尺寸: ${uploadResponse.avinfo?.video?.width}x${uploadResponse.avinfo?.video?.height}")
                        
                        // 3. 发送图片消息
                        val width = uploadResponse.avinfo?.video?.width ?: 1080
                        val height = uploadResponse.avinfo?.video?.height ?: 1920
                        
                        Log.d(tag, "📤 发送图片消息...")
                        val sendResult = messageRepository.sendImageMessage(
                            chatId = currentChatId,
                            chatType = currentChatType,
                            imageKey = uploadResponse.key,
                            width = width,
                            height = height,
                            fileSize = uploadResponse.fsize,
                            quoteMsgId = quoteMsgId,
                            quoteMsgText = quoteMsgText
                        )
                        
                        sendResult.fold(
                            onSuccess = {
                                Log.d(tag, "✅ 图片消息发送成功！")
                                // 刷新消息列表
                                loadMessages(refresh = true)
                            },
                            onFailure = { error ->
                                Log.e(tag, "❌ 发送图片消息失败", error)
                                _uiState.value = _uiState.value.copy(error = "发送图片失败: ${error.message}")
                            }
                        )
                    },
                    onFailure = { error ->
                        Log.e(tag, "❌ 上传图片失败", error)
                        _uiState.value = _uiState.value.copy(error = "上传图片失败: ${error.message}")
                    }
                )
                
            } catch (e: Exception) {
                Log.e(tag, "❌ 上传并发送图片异常", e)
                _uiState.value = _uiState.value.copy(error = "发送图片失败: ${e.message}")
            }
        }
    }
    
    /**
     * 上传并发送文件
     */
    fun uploadAndSendFile(
        context: android.content.Context,
        fileUri: android.net.Uri,
        quoteMsgId: String? = null,
        quoteMsgText: String? = null
    ) {
        viewModelScope.launch {
            try {
                Log.d(tag, "📁 ========== 开始上传并发送文件 ==========")
                Log.d(tag, "📁 文件URI: $fileUri")
                Log.d(tag, "📁 当前chatId: $currentChatId, chatType: $currentChatType")
                
                // 1. 获取用户token
                val token = tokenRepository.getTokenSync()
                if (token.isNullOrEmpty()) {
                    Log.e(tag, "❌ Token为空")
                    _uiState.value = _uiState.value.copy(error = "未登录")
                    return@launch
                }
                Log.d(tag, "✅ Token获取成功")
                
                // 2. 获取文件上传token
                Log.d(tag, "📤 请求七牛文件上传token...")
                val tokenResponse = apiService.getQiniuFileToken(token)
                
                if (!tokenResponse.isSuccessful || tokenResponse.body()?.code != 1) {
                    Log.e(tag, "❌ 获取文件上传token失败: code=${tokenResponse.code()}")
                    _uiState.value = _uiState.value.copy(error = "获取文件上传token失败")
                    return@launch
                }
                
                val uploadToken = tokenResponse.body()?.data?.token ?: run {
                    Log.e(tag, "❌ 文件上传token为空")
                    _uiState.value = _uiState.value.copy(error = "获取文件上传token失败")
                    return@launch
                }
                
                Log.d(tag, "✅ 文件上传token获取成功: ${uploadToken.take(30)}...")
                
                // 3. 上传文件到七牛云
                Log.d(tag, "📤 开始上传文件到七牛云...")
                val uploadResult = com.yhchat.canary.utils.FileUploadUtil.uploadFile(
                    context = context,
                    fileUri = fileUri,
                    uploadToken = uploadToken
                )
                
                uploadResult.fold(
                    onSuccess = { uploadResponse ->
                        Log.d(tag, "✅ 文件上传成功！")
                        Log.d(tag, "   key: ${uploadResponse.key}")
                        Log.d(tag, "   hash (etag): ${uploadResponse.hash}")
                        Log.d(tag, "   size: ${uploadResponse.fsize} bytes")
                        
                        // 4. 获取原始文件名
                        val fileName = getFileNameFromUri(context, fileUri) ?: "未知文件"
                        Log.d(tag, "✅ 原始文件名: $fileName")
                        
                        // 5. 计算MD5（从key中提取）
                        val fileMd5 = uploadResponse.key.substringAfter("disk/").substringBefore(".")
                        Log.d(tag, "✅ 文件MD5: $fileMd5")
                        
                        // 6. 发送文件消息（contentType = 4）
                        // 注意：直接发送文件时不需要调用群网盘上传记录API
                        // fileUrl直接使用七牛返回的key，不需要添加域名前缀
                        // MessageRepository会根据需要添加正确的前缀
                        val fileKey = uploadResponse.key  // 格式：disk/xxx.ext
                        
                        Log.d(tag, "📤 发送文件消息...")
                        Log.d(tag, "   fileName: $fileName")
                        Log.d(tag, "   fileKey: $fileKey")
                        Log.d(tag, "   fileSize: ${uploadResponse.fsize}")
                        
                        val sendResult = messageRepository.sendFileMessage(
                            chatId = currentChatId,
                            chatType = currentChatType,
                            fileName = fileName,
                            fileKey = fileKey,
                            fileSize = uploadResponse.fsize,
                            quoteMsgId = quoteMsgId,
                            quoteMsgText = quoteMsgText
                        )
                        
                        sendResult.fold(
                            onSuccess = {
                                Log.d(tag, "✅ 文件消息发送成功！")
                                Log.d(tag, "✅ ========== 文件发送流程完成 ==========")
                                // 刷新消息列表
                                loadMessages(refresh = true)
                            },
                            onFailure = { error ->
                                Log.e(tag, "❌ 发送文件消息失败", error)
                                _uiState.value = _uiState.value.copy(error = "发送文件失败: ${error.message}")
                            }
                        )
                    },
                    onFailure = { error ->
                        Log.e(tag, "❌ 上传文件失败", error)
                        _uiState.value = _uiState.value.copy(error = "上传文件失败: ${error.message}")
                    }
                )
                
            } catch (e: Exception) {
                Log.e(tag, "❌ 上传并发送文件异常", e)
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(error = "发送文件失败: ${e.message}")
            }
        }
    }
    
    /**
     * 从URI获取文件名
     * 优先使用ContentResolver的DISPLAY_NAME，确保获取正确的文件名
     */
    private fun getFileNameFromUri(context: android.content.Context, uri: android.net.Uri): String? {
        var fileName: String? = null
        
        // 优先尝试从ContentProvider获取DISPLAY_NAME
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatViewModel", "⚠️ 无法从ContentResolver获取文件名", e)
        }
        
        // 如果ContentProvider失败，从URI的path获取并清理
        if (fileName == null) {
            uri.lastPathSegment?.let { segment ->
                // 移除可能的前缀（如 "primary:Download/"）
                fileName = if (segment.contains('/')) {
                    segment.substringAfterLast('/')
                } else if (segment.contains(':')) {
                    segment.substringAfterLast(':')
                } else {
                    segment
                }
            }
        }
        
        // 如果还是空，使用默认名称
        if (fileName.isNullOrBlank()) {
            fileName = "file_${System.currentTimeMillis()}"
        }
        
        return fileName
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
     * @param commandId 指令ID
     */
    fun sendMessage(
        text: String, 
        contentType: Int = 1, 
        quoteMsgId: String? = null,
        quoteMsgText: String? = null,
        commandId: Long? = null
    ) {
        // 如果是指令消息，允许空文本；否则检查文本是否为空
        if (currentChatId.isEmpty()) {
            Log.w(tag, "Chat not initialized")
            return
        }
        if (text.isBlank() && commandId == null) {
            Log.w(tag, "Cannot send empty message without command")
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
                    quoteMsgText = quoteMsgText,
                    commandId = commandId
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
     * 发送表情消息（从表情选择器选择）
     */
    fun sendExpressionMessage(
        expression: com.yhchat.canary.data.model.Expression,
        quoteMsgId: String? = null,
        quoteMsgText: String? = null
    ) {
        if (currentChatId.isEmpty()) {
            Log.w(tag, "Chat not initialized")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d(tag, "Sending expression message: id=${expression.id}, url=${expression.url}")
                
                // 发送表情类型消息（contentType=7）
                val result = messageRepository.sendExpressionMessage(
                    chatId = currentChatId,
                    chatType = currentChatType,
                    expression = expression,
                    quoteMsgId = quoteMsgId,
                    quoteMsgText = quoteMsgText
                )
                
                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            Log.d(tag, "Expression message sent successfully")
                            loadMessages(refresh = true)
                        }
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to send expression message", exception)
                        _uiState.value = _uiState.value.copy(
                            error = exception.message ?: "发送表情失败"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Error sending expression message", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "发送表情失败"
                )
            }
        }
    }
    
    /**
     * 发送表情包贴纸消息
     */
    fun sendStickerMessage(
        stickerItem: com.yhchat.canary.data.model.StickerItem,
        quoteMsgId: String? = null,
        quoteMsgText: String? = null
    ) {
        if (currentChatId.isEmpty()) {
            Log.w(tag, "Chat not initialized")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d(tag, "Sending sticker message: id=${stickerItem.id}, url=${stickerItem.url}")
                
                // 发送表情包贴纸消息（contentType=7）
                val result = messageRepository.sendStickerMessage(
                    chatId = currentChatId,
                    chatType = currentChatType,
                    stickerItem = stickerItem,
                    quoteMsgId = quoteMsgId,
                    quoteMsgText = quoteMsgText
                )
                
                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            Log.d(tag, "Sticker message sent successfully")
                            loadMessages(refresh = true)
                        }
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to send sticker message", exception)
                        _uiState.value = _uiState.value.copy(
                            error = exception.message ?: "发送表情包失败"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Error sending sticker message", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "发送表情包失败"
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
     * 撤回消息
     */
    fun recallMessage(msgId: String) {
        viewModelScope.launch {
            try {
                Log.d(tag, "开始撤回消息: $msgId")
                
                val result = messageRepository.recallMessage(
                    chatId = currentChatId,
                    chatType = currentChatType,
                    msgId = msgId
                )
                
                result.fold(
                    onSuccess = {
                        Log.d(tag, "消息撤回成功: $msgId")
                        // 找到并更新消息，将其标记为已撤回
                        val index = _messages.indexOfFirst { it.msgId == msgId }
                        if (index != -1) {
                            val message = _messages[index]
                            _messages[index] = message.copy(
                                msgDeleteTime = System.currentTimeMillis()
                            )
                            Log.d(tag, "更新消息为撤回状态")
                        }
                    },
                    onFailure = { error ->
                        Log.e(tag, "消息撤回失败: ${error.message}", error)
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "撤回消息异常", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    /**
     * 编辑消息
     */
    fun editMessage(
        chatId: String,
        chatType: Int,
        msgId: String,
        content: String,
        contentType: Int
    ) {
        viewModelScope.launch {
            try {
                Log.d(tag, "开始编辑消息: $msgId")
                
                val result = messageRepository.editMessage(
                    chatId = chatId,
                    chatType = chatType,
                    msgId = msgId,
                    content = content,
                    contentType = contentType
                )
                
                result.fold(
                    onSuccess = {
                        Log.d(tag, "消息编辑成功: $msgId")
                        // 找到并更新消息内容
                        val index = _messages.indexOfFirst { it.msgId == msgId }
                        if (index != -1) {
                            val message = _messages[index]
                            _messages[index] = message.copy(
                                content = message.content.copy(text = content),
                                contentType = contentType,
                                editTime = System.currentTimeMillis()
                            )
                            Log.d(tag, "更新消息内容")
                        }
                    },
                    onFailure = { error ->
                        Log.e(tag, "消息编辑失败: ${error.message}", error)
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "编辑消息异常", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 加载机器人信息
     */
    fun loadBotInfo(botId: String) {
        viewModelScope.launch {
            botRepository.getBotInfo(botId).fold(
                onSuccess = { botInfo ->
                    _uiState.value = _uiState.value.copy(botInfo = botInfo)
                    Log.d(tag, "机器人信息加载成功: ${botInfo.data.name}")
                },
                onFailure = { error ->
                    Log.e(tag, "加载机器人信息失败", error)
                }
            )
        }
    }
    
    /**
     * 加载机器人看板
     */
    fun loadBotBoard(chatId: String, chatType: Int) {
        viewModelScope.launch {
            botRepository.getBotBoard(chatId, chatType).fold(
                onSuccess = { board ->
                    // 单个机器人私聊时，boardList应该只有一个元素
                    if (board.boardCount > 0) {
                        _uiState.value = _uiState.value.copy(botBoard = board)
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = "加载机器人看板失败: ${error.message}")
                }
            )
        }
    }
    
    /**
     * 加载聊天背景
     */
    fun loadChatBackground(context: android.content.Context, chatId: String) {
        viewModelScope.launch {
            try {
                val chatBackgroundRepository = com.yhchat.canary.data.di.RepositoryFactory.getChatBackgroundRepository(context)
                
                // 获取背景列表
                chatBackgroundRepository.getChatBackgroundList().fold(
                    onSuccess = { backgrounds ->
                        // 查找特定聊天背景，如果没有则查找全局背景
                        val specificBg = backgrounds.find { it.chatId == chatId }
                        val globalBg = backgrounds.find { it.chatId == "all" }
                        val backgroundUrl = specificBg?.imgUrl ?: globalBg?.imgUrl
                        
                        _uiState.value = _uiState.value.copy(chatBackgroundUrl = backgroundUrl)
                        Log.d(tag, "聊天背景加载成功: $backgroundUrl")
                    },
                    onFailure = { error ->
                        Log.e(tag, "加载聊天背景失败", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "加载聊天背景异常", e)
            }
        }
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
     * 获取当前用户的权限等级
     */
    fun getCurrentUserPermission(): Int {
        val currentUser = _uiState.value.groupMembers[currentUserId]
        return currentUser?.permissionLevel ?: 0
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