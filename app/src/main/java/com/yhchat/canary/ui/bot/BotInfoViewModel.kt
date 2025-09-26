package com.yhchat.canary.ui.bot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.BotInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BotInfoViewModel(application: Application) : AndroidViewModel(application) {
    
    private val botRepository = RepositoryFactory.provideBotRepository()
    private val friendRepository = RepositoryFactory.friendRepository
    private val tokenRepository = RepositoryFactory.getTokenRepository(application)
    
    private val _uiState = MutableStateFlow(BotInfoUiState())
    val uiState: StateFlow<BotInfoUiState> = _uiState.asStateFlow()
    
    fun loadBotInfo(botId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val token = tokenRepository.getTokenSync() ?: ""
                if (token.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "未登录，请先登录"
                    )
                    return@launch
                }
                
                val botInfo = botRepository.getBotInfo(token, botId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    botInfo = botInfo,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "获取机器人信息失败"
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
                        addResult = Result.success(Unit)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAdding = false,
                        addResult = Result.failure(Exception(result.message))
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAdding = false,
                    addResult = Result.failure(e)
                )
            }
        }
    }
    
    fun clearAddResult() {
        _uiState.value = _uiState.value.copy(addResult = null)
    }
}

data class BotInfoUiState(
    val isLoading: Boolean = false,
    val botInfo: BotInfo? = null,
    val error: String? = null,
    val isAdding: Boolean = false,
    val addResult: Result<Unit>? = null
)
