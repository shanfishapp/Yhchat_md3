package com.yhchat.canary.ui.sticky

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.StickyData
import com.yhchat.canary.data.repository.UserRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 置顶会话ViewModel
 */
@HiltViewModel
class StickyViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val userRepository = UserRepository(apiService)
    private var tokenRepository: TokenRepository? = null

    fun setTokenRepository(tokenRepository: TokenRepository) {
        this.tokenRepository = tokenRepository
        userRepository.setTokenRepository(tokenRepository)
    }

    // UI状态
    private val _uiState = MutableStateFlow(StickyUiState())
    val uiState: StateFlow<StickyUiState> = _uiState.asStateFlow()

    // 置顶会话数据
    private val _stickyData = MutableStateFlow<StickyData?>(null)
    val stickyData: StateFlow<StickyData?> = _stickyData.asStateFlow()

    /**
     * 加载置顶会话列表
     */
    fun loadStickyList() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.getStickyList()
                .onSuccess { stickyData ->
                    _stickyData.value = stickyData
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _stickyData.value = null
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "加载置顶会话失败: ${error.message}"
                    )
                }
        }
    }

    /**
     * 添加置顶会话
     */
    fun addSticky(chatId: String, chatType: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.addSticky(chatId, chatType)
                .onSuccess { success ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // 重新加载置顶会话列表
                    loadStickyList()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "添加置顶会话失败: ${error.message}"
                    )
                }
        }
    }

    /**
     * 删除置顶会话
     */
    fun deleteSticky(chatId: String, chatType: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.deleteSticky(chatId, chatType)
                .onSuccess { success ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // 重新加载置顶会话列表
                    loadStickyList()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "删除置顶会话失败: ${error.message}"
                    )
                }
        }
    }

    /**
     * 置顶会话
     */
    fun topSticky(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.topSticky(id)
                .onSuccess { success ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // 重新加载置顶会话列表
                    loadStickyList()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "置顶会话失败: ${error.message}"
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
}

/**
 * 置顶会话UI状态
 */
data class StickyUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
