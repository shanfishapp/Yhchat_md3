package com.yhchat.canary.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.BotInfo
import com.yhchat.canary.data.model.GroupDetail
import com.yhchat.canary.data.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ComprehensiveSearchViewModel(application: Application) : AndroidViewModel(application) {
    
    private val webApiService = RepositoryFactory.webApiService
    private val friendRepository = RepositoryFactory.getFriendRepository(application)
    private val tokenRepository = RepositoryFactory.getTokenRepository(application)
    
    private val _uiState = MutableStateFlow(ComprehensiveSearchUiState())
    val uiState: StateFlow<ComprehensiveSearchUiState> = _uiState.asStateFlow()
    
    fun searchGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = webApiService.getGroupInfo(mapOf("groupId" to groupId))
                val responseBody = response.body()
                if (responseBody != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        groupResult = GroupDetail(
                            groupId = responseBody.data.group.groupId,
                            name = responseBody.data.group.name,
                            avatarUrl = responseBody.data.group.avatarUrl,
                            introduction = responseBody.data.group.introduction,
                            memberCount = responseBody.data.group.headcount,
                            createBy = responseBody.data.group.createBy,
                            directJoin = responseBody.data.group.readHistory == 1,
                            permissionLevel = 0,
                            historyMsgEnabled = responseBody.data.group.readHistory == 1,
                            categoryName = "",
                            categoryId = 0,
                            isPrivate = false,
                            doNotDisturb = false,
                            communityId = 0,
                            communityName = "",
                            isTop = false,
                            adminIds = emptyList(),
                            ownerId = responseBody.data.group.createBy,
                            limitedMsgType = "",
                            avatarId = responseBody.data.group.avatarId.toLong(),
                            recommendation = null
                        ),
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "响应数据为空"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络错误"
                )
            }
        }
    }
    
    fun searchUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val response = webApiService.getUserHomepage(userId)
                
                if (response.isSuccessful && response.body()?.code == 1) {
                    val userProfile = response.body()!!.data.user
                    val userInfo = UserInfo(
                        userId = userProfile.userId,
                        nickname = userProfile.nickname,
                        avatarUrl = userProfile.avatarUrl,
                        registerTime = userProfile.registerTime,
                        registerTimeText = userProfile.registerTimeText,
                        onLineDay = userProfile.onLineDay,
                        continuousOnLineDay = userProfile.continuousOnLineDay,
                        isVip = userProfile.isVip
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userResult = userInfo,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.msg ?: "获取用户信息失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络错误"
                )
            }
        }
    }
    
    fun searchBot(botId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val response = webApiService.getBotInfo(mapOf("botId" to botId))
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.code == 1) {
                        val botData = responseBody.data.bot
                        val botInfo = BotInfo(
                            id = botData.id,
                            botId = botData.botId,
                            nickname = botData.nickname,
                            nicknameId = botData.nicknameId,
                            avatarId = botData.avatarId,
                            avatarUrl = botData.avatarUrl,
                            introduction = botData.introduction,
                            createBy = botData.createBy,
                            createTime = botData.createTime,
                            headcount = botData.headcount,
                            isPrivate = botData.isPrivate ?: 0
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            botResult = botInfo,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = responseBody?.msg ?: "获取机器人信息失败"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.msg ?: "获取机器人信息失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络错误"
                )
            }
        }
    }
    
    fun addGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdding = true)
            
            try {
                val token = tokenRepository.getTokenSync() ?: ""
                val result = friendRepository.applyFriend(token, groupId, 2, "申请加入群聊")
                
                if (result.code == 1) {
                    _uiState.value = _uiState.value.copy(
                        isAdding = false,
                        showGroupDialog = false,
                        error = null
                    )
                    // TODO: 显示成功提示
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAdding = false,
                        error = result.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAdding = false,
                    error = e.message ?: "添加群聊失败"
                )
            }
        }
    }
    
    fun addUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdding = true)
            
            try {
                val token = tokenRepository.getTokenSync() ?: ""
                val result = friendRepository.applyFriend(token, userId, 1, "申请添加好友")
                
                if (result.code == 1) {
                    _uiState.value = _uiState.value.copy(
                        isAdding = false,
                        showUserDialog = false,
                        error = null
                    )
                    // TODO: 显示成功提示
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAdding = false,
                        error = result.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAdding = false,
                    error = e.message ?: "添加用户失败"
                )
            }
        }
    }
    
    fun addBot(botId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdding = true)
            
            try {
                val token = tokenRepository.getTokenSync() ?: ""
                val result = friendRepository.applyFriend(token, botId, 3, "添加机器人")
                
                if (result.code == 1) {
                    _uiState.value = _uiState.value.copy(
                        isAdding = false,
                        showBotDialog = false,
                        error = null
                    )
                    // TODO: 显示成功提示
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAdding = false,
                        error = result.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAdding = false,
                    error = e.message ?: "添加机器人失败"
                )
            }
        }
    }
    
    fun showGroupDialog(group: GroupDetail) {
        _uiState.value = _uiState.value.copy(showGroupDialog = true)
    }
    
    fun hideGroupDialog() {
        _uiState.value = _uiState.value.copy(showGroupDialog = false)
    }
    
    fun showUserDialog(user: UserInfo) {
        _uiState.value = _uiState.value.copy(showUserDialog = true)
    }
    
    fun hideUserDialog() {
        _uiState.value = _uiState.value.copy(showUserDialog = false)
    }
    
    fun showBotDialog(bot: BotInfo) {
        _uiState.value = _uiState.value.copy(showBotDialog = true)
    }
    
    fun hideBotDialog() {
        _uiState.value = _uiState.value.copy(showBotDialog = false)
    }
    
    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            groupResult = null,
            userResult = null,
            botResult = null,
            error = null
        )
    }
}

data class ComprehensiveSearchUiState(
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val error: String? = null,
    val groupResult: GroupDetail? = null,
    val userResult: UserInfo? = null,
    val botResult: BotInfo? = null,
    val showGroupDialog: Boolean = false,
    val showUserDialog: Boolean = false,
    val showBotDialog: Boolean = false
)
