package com.yhchat.canary.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.UserProfile
import com.yhchat.canary.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 个人资料界面的ViewModel
 */
class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    // 修改邀请码状态
    private val _changeInviteCodeState = MutableStateFlow(ChangeInviteCodeState())
    val changeInviteCodeState: StateFlow<ChangeInviteCodeState> = _changeInviteCodeState.asStateFlow()
    
    // 修改用户名称状态
    private val _changeNicknameState = MutableStateFlow(ChangeNicknameState())
    val changeNicknameState: StateFlow<ChangeNicknameState> = _changeNicknameState.asStateFlow()
    
    // 修改头像状态
    private val _changeAvatarState = MutableStateFlow(ChangeAvatarState())
    val changeAvatarState: StateFlow<ChangeAvatarState> = _changeAvatarState.asStateFlow()
    
    // 内测状态
    private val _betaState = MutableStateFlow(BetaState())
    val betaState: StateFlow<BetaState> = _betaState.asStateFlow()

    /**
     * 加载用户个人资料
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            userRepository.getUserProfile().fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userProfile = profile,
                        error = null
                    )
                    // 加载用户资料成功后，同时加载内测状态
                    loadBetaInfo()
                },
                onFailure = { exception ->
                    Log.e("ProfileViewModel", "加载用户资料失败", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "加载用户资料失败"
                    )
                }
            )
        }
    }
    
    /**
     * 加载内测状态
     */
    fun loadBetaInfo() {
        viewModelScope.launch {
            _betaState.value = _betaState.value.copy(isLoading = true, error = null)
            
            try {
                val token = userRepository.getTokenSync() ?: ""
                val response = com.yhchat.canary.data.api.ApiClient.apiService.getBetaInfo(token)
                
                if (response.isSuccessful && response.body()?.code == 1) {
                    val betaInfo = response.body()?.data
                    _betaState.value = _betaState.value.copy(
                        isLoading = false,
                        betaInfo = betaInfo,
                        error = null
                    )
                } else {
                    _betaState.value = _betaState.value.copy(
                        isLoading = false,
                        error = response.body()?.msg ?: "获取内测状态失败"
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "获取内测状态异常", e)
                _betaState.value = _betaState.value.copy(
                    isLoading = false,
                    error = e.message ?: "获取内测状态异常"
                )
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 修改邀请码
     */
    fun changeInviteCode(code: String) {
        viewModelScope.launch {
            _changeInviteCodeState.value = _changeInviteCodeState.value.copy(
                isLoading = true, 
                error = null,
                isSuccess = false
            )
            
            userRepository.changeInviteCode(code).fold(
                onSuccess = {
                    _changeInviteCodeState.value = _changeInviteCodeState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                    // 修改成功后重新加载用户资料
                    loadUserProfile()
                },
                onFailure = { exception ->
                    Log.e("ProfileViewModel", "修改邀请码失败", exception)
                    _changeInviteCodeState.value = _changeInviteCodeState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "修改邀请码失败"
                    )
                }
            )
        }
    }
    
    /**
     * 重置修改邀请码状态
     */
    fun resetChangeInviteCodeState() {
        _changeInviteCodeState.value = ChangeInviteCodeState()
    }
    
    /**
     * 修改用户名称
     */
    fun changeNickname(nickname: String) {
        viewModelScope.launch {
            _changeNicknameState.value = _changeNicknameState.value.copy(
                isLoading = true, 
                error = null,
                isSuccess = false
            )
            
            userRepository.editNickname(nickname).fold(
                onSuccess = {
                    _changeNicknameState.value = _changeNicknameState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                    // 修改成功后重新加载用户资料
                    loadUserProfile()
                },
                onFailure = { exception ->
                    Log.e("ProfileViewModel", "修改用户名称失败", exception)
                    _changeNicknameState.value = _changeNicknameState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "修改用户名称失败"
                    )
                }
            )
        }
    }
    
    /**
     * 重置修改用户名称状态
     */
    fun resetChangeNicknameState() {
        _changeNicknameState.value = ChangeNicknameState()
    }
    
    /**
     * 修改用户头像
     */
    fun changeAvatar(context: android.content.Context, imageUri: android.net.Uri) {
        viewModelScope.launch {
            _changeAvatarState.value = _changeAvatarState.value.copy(isLoading = true, error = null)
            
            try {
                // 首先获取上传token
                val token = userRepository.getTokenSync() ?: ""
                val tokenResult = com.yhchat.canary.data.api.ApiClient.apiService.getQiniuImageToken(token)
                
                if (!tokenResult.isSuccessful || tokenResult.body()?.data == null) {
                    _changeAvatarState.value = _changeAvatarState.value.copy(
                        isLoading = false,
                        error = "获取上传token失败"
                    )
                    return@launch
                }
                
                val uploadToken = tokenResult.body()!!.data.token
                
                // 上传图片
                val uploadResult = com.yhchat.canary.utils.ImageUploadUtil.uploadImage(
                    context = context,
                    imageUri = imageUri,
                    uploadToken = uploadToken
                )
                
                uploadResult.fold(
                    onSuccess = { uploadResponse ->
                        // 构建完整的图片URL
                        val imageUrl = "https://chat-img.jwznb.com/${uploadResponse.key}"
                        
                        // 调用修改头像API
                        userRepository.editAvatar(imageUrl).fold(
                            onSuccess = {
                                _changeAvatarState.value = _changeAvatarState.value.copy(
                                    isLoading = false,
                                    isSuccess = true
                                )
                                // 重新加载用户资料
                                loadUserProfile()
                            },
                            onFailure = { exception ->
                                _changeAvatarState.value = _changeAvatarState.value.copy(
                                    isLoading = false,
                                    error = exception.message ?: "修改头像失败"
                                )
                            }
                        )
                    },
                    onFailure = { exception ->
                        _changeAvatarState.value = _changeAvatarState.value.copy(
                            isLoading = false,
                            error = "上传图片失败: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _changeAvatarState.value = _changeAvatarState.value.copy(
                    isLoading = false,
                    error = "修改头像异常: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 重置修改头像状态
     */
    fun resetChangeAvatarState() {
        _changeAvatarState.value = ChangeAvatarState()
    }
}

/**
 * 个人资料界面UI状态
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val error: String? = null
)

/**
 * 修改邀请码状态
 */
data class ChangeInviteCodeState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

/**
 * 修改用户名称状态
 */
data class ChangeNicknameState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

/**
 * 修改头像状态
 */
data class ChangeAvatarState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

/**
 * 内测状态
 */
data class BetaState(
    val isLoading: Boolean = false,
    val betaInfo: com.yhchat.canary.data.model.BetaInfo? = null,
    val error: String? = null
)
