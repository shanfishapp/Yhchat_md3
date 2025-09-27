package com.yhchat.canary.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.api.WebApiService
import com.yhchat.canary.data.model.AddFriendRequest
import com.yhchat.canary.data.model.UserHomepageInfo
import com.yhchat.canary.data.model.UserProfile
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户资料ViewModel - 完全重写版本
 * 基于 yhapi/web/v1/user.md 的 /v1/user/homepage 接口
 * 基于 yhapi/v1/friend.md 的 /v1/friend/apply 接口
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val webApiService: WebApiService,
    private val tokenRepository: TokenRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    // 用户资料
    private val _userProfile = MutableStateFlow<UserHomepageInfo?>(null)
    val userProfile: StateFlow<UserHomepageInfo?> = _userProfile.asStateFlow()

    /**
     * 加载用户资料
     * 调用 GET /v1/user/homepage?userId={userId}
     */
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            println("UserProfileViewModel: 开始加载用户资料, userId = $userId")

            try {
                val response = webApiService.getUserHomepage(userId)
                println("UserProfileViewModel: API响应 - isSuccessful: ${response.isSuccessful}, code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val homepageResponse = response.body()
                    println("UserProfileViewModel: 响应体 - code: ${homepageResponse?.code}, msg: ${homepageResponse?.msg}")
                    
                    if (homepageResponse?.code == 1) {
                        _userProfile.value = homepageResponse.data.user
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        println("UserProfileViewModel: 用户资料加载成功 - ${homepageResponse.data.user.nickname}")
                    } else {
                        val errorMsg = homepageResponse?.msg ?: "获取用户资料失败"
                        println("UserProfileViewModel: API返回错误 - $errorMsg")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMsg
                        )
                    }
                } else {
                    val errorMsg = "网络请求失败: ${response.code()}"
                    println("UserProfileViewModel: 网络请求失败 - $errorMsg")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "未知错误"
                println("UserProfileViewModel: 异常 - $errorMsg")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMsg
                )
            }
        }
    }

    /**
     * 显示添加好友对话框
     */
    fun showAddFriendDialog(userId: String, userName: String) {
        _uiState.value = _uiState.value.copy(
            showAddFriendDialog = AddFriendDialogData(
                userId = userId,
                userName = userName,
                remark = ""
            )
        )
    }

    /**
     * 隐藏添加好友对话框
     */
    fun dismissAddFriendDialog() {
        _uiState.value = _uiState.value.copy(showAddFriendDialog = null)
    }

    /**
     * 更新好友申请备注
     */
    fun updateFriendRemark(remark: String) {
        _uiState.value.showAddFriendDialog?.let { dialogData ->
            _uiState.value = _uiState.value.copy(
                showAddFriendDialog = dialogData.copy(remark = remark)
            )
        }
    }

    /**
     * 确认添加好友
     * 调用 POST /v1/friend/apply
     */
    fun confirmAddFriend() {
        val dialogData = _uiState.value.showAddFriendDialog ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingFriend = true)

            try {
                // 获取token
                val token = tokenRepository.getTokenSync()
                if (token.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isAddingFriend = false,
                        error = "未登录，请先登录"
                    )
                    return@launch
                }

                // 构建请求体，按照 friend.md 文档
                val request = AddFriendRequest(
                    chatId = dialogData.userId,
                    chatType = 1, // 1-用户，2-群聊，3-机器人
                    remark = dialogData.remark.takeIf { it.isNotBlank() } ?: ""
                )

                val response = apiService.addFriend(token, request)

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.code == 1) {
                        // 添加成功
                        _uiState.value = _uiState.value.copy(
                            isAddingFriend = false,
                            showAddFriendDialog = null,
                            error = null,
                            addFriendSuccess = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isAddingFriend = false,
                            error = result?.message ?: "添加好友失败"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAddingFriend = false,
                        error = "网络请求失败: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAddingFriend = false,
                    error = e.message ?: "未知错误"
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
     * 清除添加好友成功状态
     */
    fun clearAddFriendSuccess() {
        _uiState.value = _uiState.value.copy(addFriendSuccess = false)
    }
}

/**
 * 添加好友对话框数据
 */
data class AddFriendDialogData(
    val userId: String,
    val userName: String,
    val remark: String
)

/**
 * 用户资料UI状态
 */
data class UserProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddFriendDialog: AddFriendDialogData? = null,
    val isAddingFriend: Boolean = false,
    val addFriendSuccess: Boolean = false
)