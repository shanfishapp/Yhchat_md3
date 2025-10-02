package com.yhchat.canary.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiClient
import com.yhchat.canary.data.model.ShareRequest
import com.yhchat.canary.data.model.ShareResponse
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatInfoUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val shareUrl: String? = null
)

@HiltViewModel
class ChatInfoViewModel @Inject constructor(
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val tag = "ChatInfoViewModel"

    private val _uiState = MutableStateFlow(ChatInfoUiState())
    val uiState: StateFlow<ChatInfoUiState> = _uiState.asStateFlow()

    /**
     * 创建分享链接
     */
    fun createShare(chatId: String, chatType: Int, chatName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // 获取用户token
                val token = tokenRepository.getTokenSync()
                if (token.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "用户未登录"
                    )
                    return@launch
                }

                // 创建分享请求
                val shareRequest = ShareRequest(
                    chatId = chatId,
                    chatType = chatType,
                    chatName = chatName
                )

                // 调用API创建分享链接
                val response = ApiClient.apiService.createShare(token, shareRequest)

                if (response.isSuccessful && response.body()?.code == 1) {
                    // 分享成功，获取分享链接
                    val shareUrl = response.body()?.data?.shareUrl ?: ""
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        shareUrl = shareUrl,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.msg ?: "创建分享链接失败"
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to create share link", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "创建分享链接异常"
                )
            }
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}