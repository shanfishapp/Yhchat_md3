package com.yhchat.canary.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.UpdateInfo
import com.yhchat.canary.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 更新检查 ViewModel
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository
) : ViewModel() {
    
    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()
    
    /**
     * 检查更新
     */
    fun checkForUpdate(isLatestBuildPreview: Boolean = false) {
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(isLoading = true, error = null)
            
            updateRepository.checkForUpdate(isLatestBuildPreview)
                .onSuccess { updateInfo ->
                    _updateState.value = _updateState.value.copy(
                        isLoading = false,
                        updateInfo = updateInfo,
                        error = null
                    )
                }
                .onFailure { error ->
                    _updateState.value = _updateState.value.copy(
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
        _updateState.value = _updateState.value.copy(error = null)
    }
    
    /**
     * 清除更新信息
     */
    fun clearUpdateInfo() {
        _updateState.value = _updateState.value.copy(updateInfo = null)
    }
}

/**
 * 更新状态
 */
data class UpdateState(
    val isLoading: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val error: String? = null
)