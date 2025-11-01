package com.yhchat.canary.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.BlockedUser
import com.yhchat.canary.data.repository.CommunityRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 屏蔽用户列表ViewModel
 */
@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val tokenRepository: TokenRepository
) : ViewModel() {
    
    private val _blockedUsersState = MutableStateFlow(BlockedUsersState())
    val blockedUsersState: StateFlow<BlockedUsersState> = _blockedUsersState.asStateFlow()
    
    private val _unblockState = MutableStateFlow(UnblockState())
    val unblockState: StateFlow<UnblockState> = _unblockState.asStateFlow()
    
    /**
     * 加载屏蔽用户列表
     */
    fun loadBlockedUsers(token: String, page: Int = 1) {
        viewModelScope.launch {
            if (page == 1) {
                _blockedUsersState.value = _blockedUsersState.value.copy(isLoading = true, error = null)
            }
            
            communityRepository.getBlackList(token, page = page)
                .onSuccess { response ->
                    val newUsers = if (page == 1) {
                        response.data.list
                    } else {
                        _blockedUsersState.value.users + response.data.list
                    }
                    
                    _blockedUsersState.value = BlockedUsersState(
                        users = newUsers,
                        total = response.data.total,
                        currentPage = page,
                        hasMore = newUsers.size < response.data.total,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _blockedUsersState.value = _blockedUsersState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * 加载更多屏蔽用户
     */
    fun loadMoreBlockedUsers(token: String) {
        val currentPage = _blockedUsersState.value.currentPage
        if (_blockedUsersState.value.hasMore && !_blockedUsersState.value.isLoading) {
            loadBlockedUsers(token, currentPage + 1)
        }
    }
    
    /**
     * 取消屏蔽用户
     */
    fun unblockUser(token: String, userId: String) {
        viewModelScope.launch {
            _unblockState.value = _unblockState.value.copy(
                isLoading = true,
                userId = userId,
                error = null
            )
            
            communityRepository.setBlackList(token, userId, 0)
                .onSuccess {
                    _unblockState.value = _unblockState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                    // 从列表中移除该用户
                    val currentUsers = _blockedUsersState.value.users
                    val filteredUsers = currentUsers.filter { it.userId != userId }
                    _blockedUsersState.value = _blockedUsersState.value.copy(
                        users = filteredUsers,
                        total = _blockedUsersState.value.total - 1
                    )
                    // 重置取消屏蔽状态
                    resetUnblockState()
                }
                .onFailure { error ->
                    _unblockState.value = _unblockState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * 重置取消屏蔽状态
     */
    fun resetUnblockState() {
        _unblockState.value = UnblockState()
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _blockedUsersState.value = _blockedUsersState.value.copy(error = null)
        _unblockState.value = _unblockState.value.copy(error = null)
    }
}

/**
 * 屏蔽用户列表状态
 */
data class BlockedUsersState(
    val users: List<BlockedUser> = emptyList(),
    val total: Int = 0,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 取消屏蔽状态
 */
data class UnblockState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val userId: String? = null,
    val error: String? = null
)
