package com.yhchat.canary.ui.group

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.GroupDetail
import com.yhchat.canary.data.repository.GroupRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupSettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val groupInfo: GroupDetail? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isSaveSuccess: Boolean = false,
    // 编辑状态
    val editedName: String = "",
    val editedIntroduction: String = "",
    val editedAvatarUrl: String = "",
    val editedDirectJoin: Boolean = false,
    val editedHistoryMsg: Boolean = false,
    val editedPrivate: Boolean = false,
    val editedCategoryName: String = "",
    val editedCategoryId: Long = 0L
)

@HiltViewModel
class GroupSettingsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val tokenRepository: TokenRepository
) : ViewModel() {
    
    private val tag = "GroupSettingsViewModel"
    
    private val _uiState = MutableStateFlow(GroupSettingsUiState())
    val uiState: StateFlow<GroupSettingsUiState> = _uiState.asStateFlow()
    
    init {
        groupRepository.setTokenRepository(tokenRepository)
    }
    
    /**
     * 加载群聊信息
     */
    fun loadGroupInfo(groupId: String) {
        viewModelScope.launch {
            Log.d(tag, "Loading group info for: $groupId")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            groupRepository.getGroupInfo(groupId).fold(
                onSuccess = { groupInfo ->
                    Log.d(tag, "✅ Group info loaded: ${groupInfo.name}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        groupInfo = groupInfo,
                        // 初始化编辑状态
                        editedName = groupInfo.name,
                        editedIntroduction = groupInfo.introduction,
                        editedAvatarUrl = groupInfo.avatarUrl,
                        editedDirectJoin = groupInfo.directJoin,
                        editedHistoryMsg = groupInfo.historyMsgEnabled,
                        editedPrivate = groupInfo.isPrivate,
                        editedCategoryName = groupInfo.categoryName,
                        editedCategoryId = groupInfo.categoryId
                    )
                },
                onFailure = { error ->
                    Log.e(tag, "❌ Failed to load group info", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载群聊信息失败"
                    )
                }
            )
        }
    }
    
    /**
     * 开始编辑
     */
    fun startEditing() {
        _uiState.value = _uiState.value.copy(isEditing = true)
    }
    
    /**
     * 取消编辑
     */
    fun cancelEditing() {
        val groupInfo = _uiState.value.groupInfo
        if (groupInfo != null) {
            _uiState.value = _uiState.value.copy(
                isEditing = false,
                // 恢复原始值
                editedName = groupInfo.name,
                editedIntroduction = groupInfo.introduction,
                editedAvatarUrl = groupInfo.avatarUrl,
                editedDirectJoin = groupInfo.directJoin,
                editedHistoryMsg = groupInfo.historyMsgEnabled,
                editedPrivate = groupInfo.isPrivate,
                editedCategoryName = groupInfo.categoryName,
                editedCategoryId = groupInfo.categoryId
            )
        }
    }
    
    /**
     * 保存编辑
     */
    fun saveEditing() {
        val currentState = _uiState.value
        val groupInfo = currentState.groupInfo ?: return
        
        viewModelScope.launch {
            Log.d(tag, "Saving group settings for: ${groupInfo.groupId}")
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)
            
            groupRepository.editGroupInfo(
                groupId = groupInfo.groupId,
                name = currentState.editedName,
                introduction = currentState.editedIntroduction,
                avatarUrl = currentState.editedAvatarUrl,
                directJoin = currentState.editedDirectJoin,
                historyMsg = currentState.editedHistoryMsg,
                categoryName = currentState.editedCategoryName,
                categoryId = currentState.editedCategoryId,
                isPrivate = currentState.editedPrivate
            ).fold(
                onSuccess = {
                    Log.d(tag, "✅ Group settings saved successfully")
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isEditing = false,
                        isSaveSuccess = true
                    )
                    // 重新加载群聊信息
                    loadGroupInfo(groupInfo.groupId)
                },
                onFailure = { error ->
                    Log.e(tag, "❌ Failed to save group settings", error)
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveError = error.message ?: "保存失败"
                    )
                }
            )
        }
    }
    
    /**
     * 更新编辑字段
     */
    fun updateEditedName(name: String) {
        _uiState.value = _uiState.value.copy(editedName = name)
    }
    
    fun updateEditedIntroduction(introduction: String) {
        _uiState.value = _uiState.value.copy(editedIntroduction = introduction)
    }
    
    fun updateEditedAvatarUrl(avatarUrl: String) {
        _uiState.value = _uiState.value.copy(editedAvatarUrl = avatarUrl)
    }
    
    fun updateEditedDirectJoin(directJoin: Boolean) {
        _uiState.value = _uiState.value.copy(editedDirectJoin = directJoin)
    }
    
    fun updateEditedHistoryMsg(historyMsg: Boolean) {
        _uiState.value = _uiState.value.copy(editedHistoryMsg = historyMsg)
    }
    
    fun updateEditedPrivate(isPrivate: Boolean) {
        _uiState.value = _uiState.value.copy(editedPrivate = isPrivate)
    }
    
    fun updateEditedCategoryName(categoryName: String) {
        _uiState.value = _uiState.value.copy(editedCategoryName = categoryName)
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSaveError() {
        _uiState.value = _uiState.value.copy(saveError = null)
    }
    
    /**
     * 检查是否为管理员或群主
     */
    fun isAdminOrOwner(): Boolean {
        val groupInfo = _uiState.value.groupInfo ?: return false
        // 这里需要获取当前用户ID，暂时返回true用于测试
        // TODO: 实现真正的权限检查
        return groupInfo.permissionLevel == 100 || groupInfo.permissionLevel == 2
    }
}
