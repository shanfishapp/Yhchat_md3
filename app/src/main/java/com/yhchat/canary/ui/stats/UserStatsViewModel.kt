package com.yhchat.canary.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiClient
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.UserStatsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * 用户统计ViewModel
 */
class UserStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UserStatsUiState())
    val uiState: StateFlow<UserStatsUiState> = _uiState.asStateFlow()

    private val apiService = ApiClient.apiService

    /**
     * 加载用户统计数据
     */
    fun loadUserStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                // 获取token
                val context = getApplication<Application>()
                val tokenRepository = RepositoryFactory.getTokenRepository(context)
                val token = tokenRepository.getToken().firstOrNull()?.token

                if (token.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "用户未登录"
                    )
                    return@launch
                }

                val response = apiService.getUserStats(token)

                if (response.isSuccessful) {
                    val userStatsResponse = response.body()
                    if (userStatsResponse != null && userStatsResponse.code == 1) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            userStats = userStatsResponse.data,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = userStatsResponse?.msg ?: "获取统计数据失败"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "网络请求失败: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }
}

/**
 * 用户统计UI状态
 */
data class UserStatsUiState(
    val isLoading: Boolean = false,
    val userStats: UserStatsData? = null,
    val error: String? = null
)
