package com.yhchat.canary.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.repository.GroupTagRepository
import com.yhchat.canary.data.repository.TagMemberInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupTagDetailViewModel @Inject constructor(
    private val groupTagRepository: GroupTagRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GroupTagDetailUiState())
    val uiState: StateFlow<GroupTagDetailUiState> = _uiState.asStateFlow()
    
    fun loadMembers(groupId: String, tagId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            groupTagRepository.getTagMembers(groupId, tagId).fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        members = data.members,
                        total = data.total,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message,
                        isLoading = false
                    )
                }
            )
        }
    }
    
    fun removeTagFromUser(userId: String, tagId: Long, groupId: String) {
        viewModelScope.launch {
            groupTagRepository.cancelRelateUserTag(userId, tagId).fold(
                onSuccess = {
                    // 重新加载成员列表
                    loadMembers(groupId, tagId)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun addTagToUser(userId: String, tagId: Long, groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            groupTagRepository.relateUserTag(userId, tagId).fold(
                onSuccess = {
                    // 重新加载成员列表
                    loadMembers(groupId, tagId)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message,
                        isLoading = false
                    )
                }
            )
        }
    }
}

data class GroupTagDetailUiState(
    val members: List<TagMemberInfo> = emptyList(),
    val total: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

