package com.yhchat.canary.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.repository.GroupTagRepository
import com.yhchat.canary.data.repository.GroupRepository
import com.yhchat.canary.data.repository.TagMemberInfo
import com.yhchat.canary.data.model.GroupMemberInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupTagDetailViewModel @Inject constructor(
    private val groupTagRepository: GroupTagRepository,
    private val groupRepository: GroupRepository
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
    
    /**
     * 加载群成员列表（第一页）
     */
    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingGroupMembers = true,
                currentPage = 1,
                hasMoreGroupMembers = true
            )
            
            groupRepository.getGroupMembers(groupId, size = 50, page = 1).fold(
                onSuccess = { members ->
                    val hasMore = members.size >= 50
                    _uiState.value = _uiState.value.copy(
                        groupMembers = members,
                        isLoadingGroupMembers = false,
                        currentPage = 1,
                        hasMoreGroupMembers = hasMore
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message,
                        isLoadingGroupMembers = false,
                        hasMoreGroupMembers = false
                    )
                }
            )
        }
    }
    
    /**
     * 加载更多群成员（下一页）
     */
    fun loadMoreGroupMembers(groupId: String) {
        val currentState = _uiState.value
        
        // 如果正在加载或没有更多数据，则不执行
        if (currentState.isLoadingMoreGroupMembers || !currentState.hasMoreGroupMembers) {
            return
        }
        
        viewModelScope.launch {
            val nextPage = currentState.currentPage + 1
            _uiState.value = _uiState.value.copy(isLoadingMoreGroupMembers = true)
            
            groupRepository.getGroupMembers(groupId, size = 50, page = nextPage).fold(
                onSuccess = { newMembers ->
                    val allMembers = _uiState.value.groupMembers + newMembers
                    val hasMore = newMembers.size >= 50
                    
                    _uiState.value = _uiState.value.copy(
                        isLoadingMoreGroupMembers = false,
                        groupMembers = allMembers,
                        currentPage = nextPage,
                        hasMoreGroupMembers = hasMore
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMoreGroupMembers = false,
                        hasMoreGroupMembers = false,
                        error = error.message
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
    val error: String? = null,
    // 群成员分页相关字段
    val groupMembers: List<GroupMemberInfo> = emptyList(),
    val isLoadingGroupMembers: Boolean = false,
    val currentPage: Int = 1,
    val hasMoreGroupMembers: Boolean = true,
    val isLoadingMoreGroupMembers: Boolean = false
)

