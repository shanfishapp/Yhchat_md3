package com.yhchat.canary.ui.group

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.GroupDetail
import com.yhchat.canary.data.model.GroupMemberInfo
import com.yhchat.canary.data.repository.GroupRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupInfoUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,  // 成功消息
    val groupInfo: GroupDetail? = null,
    val members: List<GroupMemberInfo> = emptyList(),
    val isLoadingMembers: Boolean = false,
    val currentPage: Int = 1,
    val hasMoreMembers: Boolean = true,
    val isLoadingMoreMembers: Boolean = false,
    val showMemberList: Boolean = false,
    val isEditingCategory: Boolean = false,
    val newCategoryName: String = ""
)

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val tokenRepository: TokenRepository
) : ViewModel() {
    
    private val tag = "GroupInfoViewModel"
    
    private val _uiState = MutableStateFlow(GroupInfoUiState())
    val uiState: StateFlow<GroupInfoUiState> = _uiState.asStateFlow()
    
    init {
        groupRepository.setTokenRepository(tokenRepository)
    }
    
    /**
     * 加载群聊信息
     */
    fun loadGroupInfo(groupId: String) {
        viewModelScope.launch {
            Log.d(tag, "Starting to load group info for: $groupId")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            groupRepository.getGroupInfo(groupId).fold(
                onSuccess = { groupInfo ->
                    Log.d(tag, "✅ Group info loaded successfully: ${groupInfo.name}, members: ${groupInfo.memberCount}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        groupInfo = groupInfo
                    )
                    
                    // 自动加载群成员
                    loadGroupMembers(groupId)
                },
                onFailure = { error ->
                    Log.e(tag, "❌ Failed to load group info for $groupId", error)
                    Log.e(tag, "Error message: ${error.message}")
                    Log.e(tag, "Error stacktrace: ${error.stackTraceToString()}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载群聊信息失败"
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
                isLoadingMembers = true,
                currentPage = 1,
                members = emptyList(),
                hasMoreMembers = true
            )
            
            groupRepository.getGroupMembers(groupId, size = 50, page = 1).fold(
                onSuccess = { members ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMembers = false,
                        members = members,
                        currentPage = 1,
                        hasMoreMembers = members.size >= 50 // 如果返回50个，可能还有更多
                    )
                    Log.d(tag, "Group members loaded: ${members.size}, hasMore: ${members.size >= 50}")
                },
                onFailure = { error ->
                    Log.e(tag, "Failed to load group members", error)
                    _uiState.value = _uiState.value.copy(
                        isLoadingMembers = false,
                        hasMoreMembers = false
                    )
                }
            )
        }
    }
    
    /**
     * 加载更多群成员（下一页）
     */
    fun loadMoreMembers(groupId: String) {
        val currentState = _uiState.value
        
        Log.d(tag, "📋 loadMoreMembers called - isLoadingMore: ${currentState.isLoadingMoreMembers}, hasMore: ${currentState.hasMoreMembers}, currentPage: ${currentState.currentPage}")
        
        // 如果正在加载或没有更多数据，则不执行
        if (currentState.isLoadingMoreMembers) {
            Log.d(tag, "⏸️ Already loading more members, skipping...")
            return
        }
        
        if (!currentState.hasMoreMembers) {
            Log.d(tag, "⏸️ No more members to load, skipping...")
            return
        }
        
        viewModelScope.launch {
            val nextPage = currentState.currentPage + 1
            _uiState.value = _uiState.value.copy(isLoadingMoreMembers = true)
            
            Log.d(tag, "📥 Loading more members for group: $groupId, page: $nextPage")
            
            groupRepository.getGroupMembers(groupId, size = 50, page = nextPage).fold(
                onSuccess = { newMembers ->
                    val allMembers = _uiState.value.members + newMembers
                    val hasMore = newMembers.size >= 50
                    
                    _uiState.value = _uiState.value.copy(
                        isLoadingMoreMembers = false,
                        members = allMembers,
                        currentPage = nextPage,
                        hasMoreMembers = hasMore
                    )
                    
                    Log.d(tag, "✅ Page $nextPage loaded: ${newMembers.size} new members, total: ${allMembers.size}, hasMore: $hasMore")
                },
                onFailure = { error ->
                    Log.e(tag, "❌ Failed to load page $nextPage", error)
                    _uiState.value = _uiState.value.copy(
                        isLoadingMoreMembers = false,
                        hasMoreMembers = false
                    )
                }
            )
        }
    }
    
    /**
     * 搜索群成员
     */
    fun searchMembers(groupId: String, keywords: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMembers = true)
            
            groupRepository.searchGroupMembers(groupId, keywords, size = 50, page = 1).fold(
                onSuccess = { members ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMembers = false,
                        members = members,
                        currentPage = 1,
                        hasMoreMembers = false  // 搜索模式下禁用分页加载
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMembers = false,
                        hasMoreMembers = false
                    )
                }
            )
        }
    }
    
    /**
     * 清除搜索，重新加载成员列表
     */
    fun clearSearch(groupId: String) {
        _uiState.value = _uiState.value.copy(
            members = emptyList(),
            currentPage = 1,
            hasMoreMembers = true
        )
        // 重新加载第一页
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMembers = true)
            
            groupRepository.getGroupMembers(groupId, size = 50, page = 1).fold(
                onSuccess = { members ->
                    val hasMore = members.size >= 50
                    _uiState.value = _uiState.value.copy(
                        isLoadingMembers = false,
                        members = members,
                        currentPage = 1,
                        hasMoreMembers = hasMore
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMembers = false,
                        hasMoreMembers = false
                    )
                }
            )
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    /**
     * 切换成员列表显示状态
     */
    fun toggleMemberList() {
        _uiState.value = _uiState.value.copy(showMemberList = !_uiState.value.showMemberList)
    }
    
    /**
     * 显示编辑分类对话框
     */
    fun showEditCategoryDialog() {
        val currentCategory = _uiState.value.groupInfo?.categoryName ?: ""
        _uiState.value = _uiState.value.copy(isEditingCategory = true, newCategoryName = currentCategory)
    }
    
    /**
     * 隐藏编辑分类对话框
     */
    fun hideEditCategoryDialog() {
        _uiState.value = _uiState.value.copy(isEditingCategory = false, newCategoryName = "")
    }
    
    /**
     * 更新新的分类名称
     */
    fun updateNewCategoryName(name: String) {
        _uiState.value = _uiState.value.copy(newCategoryName = name)
    }
    
    /**
     * 踢出群成员
     */
    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            Log.d(tag, "Removing member: groupId=$groupId, userId=$userId")
            
            groupRepository.removeMember(groupId, userId).fold(
                onSuccess = {
                    Log.d(tag, "✅ Member removed successfully")
                    _uiState.value = _uiState.value.copy(
                        successMessage = "已踢出该成员"
                    )
                    // 重新加载群成员列表
                    loadGroupMembers(groupId)
                },
                onFailure = { error ->
                    Log.e(tag, "❌ Failed to remove member", error)
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "踢出成员失败"
                    )
                }
            )
        }
    }
    
    /**
     * 禁言群成员
     */
    fun gagMember(groupId: String, userId: String, gagTime: Int) {
        viewModelScope.launch {
            Log.d(tag, "Gagging member: groupId=$groupId, userId=$userId, gagTime=$gagTime")
            
            groupRepository.gagMember(groupId, userId, gagTime).fold(
                onSuccess = {
                    val message = when (gagTime) {
                        0 -> "已取消禁言"
                        600 -> "已禁言10分钟"
                        3600 -> "已禁言1小时"
                        21600 -> "已禁言6小时"
                        43200 -> "已禁言12小时"
                        1 -> "已永久禁言"
                        else -> "禁言设置成功"
                    }
                    Log.d(tag, "✅ Member gagged successfully: $message")
                    _uiState.value = _uiState.value.copy(
                        successMessage = message
                    )
                    // 重新加载群成员列表
                    loadGroupMembers(groupId)
                },
                onFailure = { error ->
                    Log.e(tag, "❌ Failed to gag member", error)
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "禁言操作失败"
                    )
                }
            )
        }
    }
    
    /**
     * 设置成员角色（上任/卸任管理员）
     */
    fun setMemberRole(groupId: String, userId: String, userLevel: Int) {
        viewModelScope.launch {
            Log.d(tag, "Setting member role: groupId=$groupId, userId=$userId, userLevel=$userLevel")
            
            groupRepository.setMemberRole(groupId, userId, userLevel).fold(
                onSuccess = {
                    val message = when (userLevel) {
                        0 -> "已卸任管理员"
                        2 -> "已设为管理员"
                        else -> "角色设置成功"
                    }
                    Log.d(tag, "✅ Member role set successfully: $message")
                    _uiState.value = _uiState.value.copy(
                        successMessage = message
                    )
                    // 重新加载群成员列表
                    loadGroupMembers(groupId)
                },
                onFailure = { error ->
                    Log.e(tag, "❌ Failed to set member role", error)
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "设置角色失败"
                    )
                }
            )
        }
    }
}

