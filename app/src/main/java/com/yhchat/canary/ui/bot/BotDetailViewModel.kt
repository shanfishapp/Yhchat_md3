package com.yhchat.canary.ui.bot

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.repository.BotRepository
import yh_bot.Bot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 机器人详细信息 ViewModel
 */
class BotDetailViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "BotDetailViewModel"
    }
    
    private val botRepository: BotRepository by lazy {
        RepositoryFactory.getBotRepository(application)
    }
    
    private val _uiState = MutableStateFlow(BotDetailUiState())
    val uiState: StateFlow<BotDetailUiState> = _uiState.asStateFlow()
    
    /**
     * 加载机器人详细信息
     */
    fun loadBotDetail(botId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            Log.d(TAG, "开始加载机器人详细信息: $botId")
            
            val result = botRepository.getBotInfo(botId)
            
            result.fold(
                onSuccess = { botInfo ->
                    Log.d(TAG, "✅ 机器人信息加载成功")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        botInfo = botInfo,
                        error = null
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ 机器人信息加载失败", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载失败"
                    )
                }
            )
        }
    }
    
    /**
     * 加载看板信息
     */
    fun loadBoardInfo(chatId: String, chatType: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBoardLoading = true,
                boardError = null
            )
            
            Log.d(TAG, "开始加载看板信息: chatId=$chatId, chatType=$chatType")
            
            val result = botRepository.getBotBoard(chatId, chatType)
            
            result.fold(
                onSuccess = { board ->
                    Log.d(TAG, "✅ 看板信息加载成功")
                    _uiState.value = _uiState.value.copy(
                        isBoardLoading = false,
                        boardInfo = board,
                        boardError = null
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ 看板信息加载失败", error)
                    _uiState.value = _uiState.value.copy(
                        isBoardLoading = false,
                        boardError = error.message ?: "加载失败"
                    )
                }
            )
        }
    }
}

/**
 * 机器人详细信息 UI 状态
 */
data class BotDetailUiState(
    val isLoading: Boolean = false,
    val botInfo: Bot.bot_info? = null,
    val error: String? = null,
    val isBoardLoading: Boolean = false,
    val boardInfo: Bot.board? = null,
    val boardError: String? = null
)
