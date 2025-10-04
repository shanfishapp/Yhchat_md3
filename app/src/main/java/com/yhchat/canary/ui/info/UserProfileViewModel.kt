package com.yhchat.canary.ui.info

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.UserInfo
import com.yhchat.canary.data.repository.FriendRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userInfo: UserInfo? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenRepository: TokenRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()
    
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = UserProfileUiState(isLoading = true)
            
            try {
                val token = tokenRepository.getToken().first()
                if (token?.token.isNullOrBlank()) {
                    _uiState.value = UserProfileUiState(error = "未找到有效的token")
                    return@launch
                }
                
                
                val response = apiService.getUserHomepageInfo(token!!.token, userId)
                
                if (response.isSuccessful && response.body()?.code == 1) {
                    // Convert UserHomepageInfo to UserInfo
                    val homepageInfo = response.body()?.data?.user
                    val userInfo = homepageInfo?.let {
                        UserInfo(
                            userId = it.userId,
                            nickname = it.nickname,
                            avatarUrl = it.avatarUrl,
                            registerTime = it.registerTime,
                            registerTimeText = it.registerTimeText,
                            onLineDay = it.onLineDay,
                            continuousOnLineDay = it.continuousOnLineDay,
                            isVip = it.isVip
                        )
                    }
                    _uiState.value = UserProfileUiState(userInfo = userInfo)
                } else {
                    _uiState.value = UserProfileUiState(error = response.message())
                }
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "加载用户信息失败", e)
                _uiState.value = UserProfileUiState(error = e.message)
            }
        }
    }
    
    fun delFriend(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            
            try {
                val token = tokenRepository.getToken().first()
                if (token?.token.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(isDeleting = false, error = "未找到有效的token")
                    return@launch
                }
                
                val response = friendRepository.delFriend(token!!.token, userId, 1) // 1表示用户类型
                if (response.code == 1) {
                    _uiState.value = _uiState.value.copy(isDeleting = false, deleteSuccess = true)
                } else {
                    _uiState.value = _uiState.value.copy(isDeleting = false, error = response.message)
                }
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "删除好友失败", e)
                _uiState.value = _uiState.value.copy(isDeleting = false, error = e.message)
            }
        }
    }

    // 添加重置方法
    fun resetDeleteState() {
        _uiState.value = _uiState.value.copy(deleteSuccess = false, error = null)
    }
}