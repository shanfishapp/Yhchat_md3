package com.yhchat.canary.ui.info

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.BotInfo
import com.yhchat.canary.data.repository.FriendRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BotProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val botInfo: BotInfo? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false
)

@HiltViewModel
class BotProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenRepository: TokenRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BotProfileUiState())
    val uiState: StateFlow<BotProfileUiState> = _uiState.asStateFlow()
    
    fun loadBotProfile(botId: String) {
        viewModelScope.launch {
            _uiState.value = BotProfileUiState(isLoading = true)
            
            try {
                val token = tokenRepository.getToken().first()
                if (token?.token.isNullOrBlank()) {
                    _uiState.value = BotProfileUiState(error = "未找到有效的token")
                    return@launch
                }
                
                // 构造请求参数
                val request = mapOf(
                    "botId" to botId
                )
                
                val response = apiService.getBotInfo(token!!.token, request)
                
                if (response.isSuccessful && response.body()?.code == 1) {
                    _uiState.value = BotProfileUiState(botInfo = response.body()?.data?.bot)
                } else {
                    _uiState.value = BotProfileUiState(error = response.message())
                }
            } catch (e: Exception) {
                Log.e("BotProfileViewModel", "加载机器人信息失败", e)
                _uiState.value = BotProfileUiState(error = e.message)
            }
        }
    }
    
    fun delBot(botId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            
            try {
                val token = tokenRepository.getToken().first()
                if (token?.token.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(isDeleting = false, error = "未找到有效的token")
                    return@launch
                }
                
                val response = friendRepository.delFriend(token!!.token, botId, 3) // 3表示机器人类型
                
                if (response.code == 1) {
                    _uiState.value = _uiState.value.copy(isDeleting = false, deleteSuccess = true)
                } else {
                    _uiState.value = _uiState.value.copy(isDeleting = false, error = response.message)
                }
            } catch (e: Exception) {
                Log.e("BotProfileViewModel", "删除机器人失败", e)
                _uiState.value = _uiState.value.copy(isDeleting = false, error = e.message)
            }
        }
    }
}