package com.yhchat.canary.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.DeviceInfo
import com.yhchat.canary.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设备管理 ViewModel
 */
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()
    
    /**
     * 设置 TokenRepository
     */
    fun setTokenRepository(tokenRepository: com.yhchat.canary.data.repository.TokenRepository) {
        userRepository.setTokenRepository(tokenRepository)
    }
    
    /**
     * 获取在线设备列表
     */
    fun loadOnlineDevices() {
        viewModelScope.launch {
            _deviceState.value = _deviceState.value.copy(isLoading = true, error = null)
            
            userRepository.getOnlineDevices()
                .onSuccess { devices ->
                    _deviceState.value = _deviceState.value.copy(
                        isLoading = false,
                        devices = devices,
                        error = null
                    )
                }
                .onFailure { error ->
                    _deviceState.value = _deviceState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _deviceState.value = _deviceState.value.copy(error = null)
    }
    
    /**
     * 清除设备列表
     */
    fun clearDevices() {
        _deviceState.value = DeviceState()
    }
}

/**
 * 设备状态
 */
data class DeviceState(
    val isLoading: Boolean = false,
    val devices: List<DeviceInfo> = emptyList(),
    val error: String? = null
)