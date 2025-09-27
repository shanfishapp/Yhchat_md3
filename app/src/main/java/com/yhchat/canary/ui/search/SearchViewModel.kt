package com.yhchat.canary.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.SearchData
import com.yhchat.canary.data.repository.UserRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.FriendRepository
import com.yhchat.canary.data.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 申请入群状态
 */
data class JoinRequestState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val isChecking: Boolean = false,
    val isInConversations: Boolean = false
)

/**
 * 搜索ViewModel
 */
class SearchViewModel(
    private val apiService: ApiService,
    private val tokenRepository: TokenRepository?,
    private val friendRepository: FriendRepository? = null,
    private val conversationRepository: ConversationRepository? = null
) : ViewModel() {

    private val userRepository: UserRepository

    init {
        userRepository = UserRepository(apiService, null)
        userRepository.setTokenRepository(tokenRepository)
    }


    fun setTokenRepository(tokenRepository: TokenRepository?) {
        if (tokenRepository != null) {
            userRepository.setTokenRepository(tokenRepository)
        }
    }

    // UI状态
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // 搜索结果
    private val _searchResult = MutableStateFlow<SearchData?>(null)
    val searchResult: StateFlow<SearchData?> = _searchResult.asStateFlow()
    
    // 申请入群状态
    private val _joinRequestState = MutableStateFlow(JoinRequestState())
    val joinRequestState: StateFlow<JoinRequestState> = _joinRequestState.asStateFlow()

    /**
     * 执行搜索
     */
    fun search(word: String) {
        if (word.isBlank()) {
            _searchResult.value = null
            _uiState.value = _uiState.value.copy(error = "请输入搜索关键词")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.homeSearch(word)
                .onSuccess { searchData ->
                    _searchResult.value = searchData
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _searchResult.value = null
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "搜索失败: ${error.message}"
                    )
                }
        }
    }

    /**
     * 清除搜索结果
     */
    fun clearSearch() {
        _searchResult.value = null
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 申请入群
     */
    fun applyToJoinGroup(token: String, groupId: String) {
        viewModelScope.launch {
            try {
                _joinRequestState.value = _joinRequestState.value.copy(
                    isLoading = true,
                    error = null,
                    isSuccess = false
                )
                
                val response = friendRepository?.applyFriend(
                    token = token,
                    chatId = groupId,
                    chatType = 2, // 群聊类型
                    remark = "申请加入群聊"
                )
                
                if (response?.code == 1) {
                    _joinRequestState.value = _joinRequestState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                } else {
                    _joinRequestState.value = _joinRequestState.value.copy(
                        error = response?.message ?: "申请失败",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _joinRequestState.value = _joinRequestState.value.copy(
                    error = "申请入群失败: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * 重置申请状态
     */
    fun resetJoinRequestState() {
        _joinRequestState.value = JoinRequestState()
    }
    
    /**
     * 检查群聊是否已在会话列表中
     */
    suspend fun checkGroupInConversations(token: String, groupId: String): Boolean {
        return try {
            val result = conversationRepository?.getConversations()
            if (result?.isSuccess == true) {
                val conversations = result.getOrNull() ?: emptyList()
                conversations.any { conversation -> 
                    conversation.chatId == groupId && conversation.chatType == 2 // 2表示群聊
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 处理群聊进入逻辑
     */
    fun handleGroupJoin(token: String, groupId: String) {
        viewModelScope.launch {
            try {
                _joinRequestState.value = _joinRequestState.value.copy(
                    isChecking = true,
                    error = null
                )
                
                val isInConversations = checkGroupInConversations(token, groupId)
                
                if (isInConversations) {
                    // 已在会话列表中，直接进入聊天
                    _joinRequestState.value = _joinRequestState.value.copy(
                        isChecking = false,
                        isInConversations = true
                    )
                } else {
                    // 不在会话列表中，申请加入
                    applyToJoinGroup(token, groupId)
                }
            } catch (e: Exception) {
                _joinRequestState.value = _joinRequestState.value.copy(
                    isChecking = false,
                    error = "检查失败: ${e.message}"
                )
            }
        }
    }
}

/**
 * 搜索UI状态
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
