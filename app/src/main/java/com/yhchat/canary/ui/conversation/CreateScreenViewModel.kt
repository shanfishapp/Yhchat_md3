package com.yhchat.canary.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.proto.createbot.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
/**
 * 创建界面ViewModel
 */
@HiltViewModel
class CreateScreenViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val apiService: ApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()
    
    fun updateBotName(name: String) {
        _uiState.value = _uiState.value.copy(botName = name)
    }
    
    fun updateBotDescription(description: String) {
        _uiState.value = _uiState.value.copy(botDescription = description)
    }
    
    fun updateBotAvatarUrl(avatarUrl: String) {
        _uiState.value = _uiState.value.copy(botAvatarUrl = avatarUrl)
    }
    
    fun updateGroupName(name: String) {
        _uiState.value = _uiState.value.copy(groupName = name)
    }
    
    fun updateGroupDescription(description: String) {
        _uiState.value = _uiState.value.copy(groupDescription = description)
    }
    
    fun updateGroupAvatarUrl(avatarUrl: String) {
        _uiState.value = _uiState.value.copy(groupAvatarUrl = avatarUrl)
    }
    
    fun toggleBotPrivacy() {
        _uiState.value = _uiState.value.copy(isBotPrivate = !_uiState.value.isBotPrivate)
    }
    
    fun setSelectedTab(tab: CreateTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }
    
    /**
     * 创建机器人
     */
    fun createBot(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        // 检查必要字段
        if (_uiState.value.botName.isBlank()) {
            onError("机器人名称不能为空")
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                val token = tokenRepository.getTokenSync()
                if (token.isNullOrEmpty()) {
                    onError("未找到用户令牌")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                val request = create_bot_send.newBuilder()
                    .setName(_uiState.value.botName)
                    .setDesc(_uiState.value.botDescription)
                    .setAvatarUrl(_uiState.value.botAvatarUrl)
                    .setIsPrivate(if (_uiState.value.isBotPrivate) 1 else 0)
                    .build()
                val requestBody = request.toByteArray().toRequestBody("application/x-protobuf".toMediaType())
                val response = apiService.createBotProto(token, requestBody)
                
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val bytes = responseBody.bytes()
                        val sendResponse = create_bot.parseFrom(bytes)
                        
                        if (sendResponse.status.code == 1) {
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            onSuccess(sendResponse.bot.botId)
                        } else {
                            val errorMsg = sendResponse?.status?.msg ?: "创建机器人失败"
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = errorMsg
                            )
                            onError(errorMsg)
                        }
                    }
                } else {
                    val errorMsg = response.message() ?: "创建机器人失败"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "创建机器人时发生错误"
                )
                onError(_uiState.value.error ?: "创建机器人时发生错误")
            }
        }
    }
    /**
     * 创建群聊
     */
    fun createGroup(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        // 检查必要字段
        if (_uiState.value.groupName.isBlank()) {
            onError("群聊名称不能为空")
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                val token = tokenRepository.getTokenSync()
                if (token.isNullOrEmpty()) {
                    onError("未找到用户令牌")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                val request = create_group_send.newBuilder()
                    .setName(_uiState.value.groupName)
                    .setDesc(_uiState.value.groupDescription)
                    .setAvatarUrl(_uiState.value.groupAvatarUrl)
                    .build()
                val requestBody = request.toByteArray().toRequestBody("application/x-protobuf".toMediaType())
                val response = apiService.createGroup(token, requestBody)
                
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val bytes = responseBody.bytes()
                        val sendResponse = create_group.parseFrom(bytes)
                        
                        if (sendResponse.status.code == 1) {
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            onSuccess(sendResponse.groupId)
                        } else {
                            val errorMsg = sendResponse?.status?.msg ?: "创建群聊失败"
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = errorMsg
                            )
                            onError(errorMsg)
                        }
                    }
                } else {
                    val errorMsg = response.message() ?: "创建群聊失败"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "创建群聊时发生错误"
                )
                onError(_uiState.value.error ?: "创建群聊时发生错误")
            }
        }
    }
    /**
     * 重置表单
     */
    fun resetForm() {
        _uiState.value = CreateUiState()
    }
}

/**
 * 创建界面UI状态
 */
data class CreateUiState(
    val selectedTab: CreateTab = CreateTab.BOT,
    val botName: String = "",
    val botDescription: String = "",
    val botAvatarUrl: String = "",
    val isBotPrivate: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val groupName: String = "",
    val groupDescription: String = "",
    val groupAvatarUrl: String = ""
)

/**
 * 创建类型枚举
 */
enum class CreateTab {
    BOT,        // 机器人
    GROUP       // 群聊
}