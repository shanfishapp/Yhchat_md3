package com.yhchat.canary.ui.group

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.GroupDetail
import com.yhchat.canary.data.model.GroupMemberInfo
import com.yhchat.canary.data.repository.FriendRepository
import com.yhchat.canary.data.repository.GroupRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupInfoUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val groupInfo: GroupDetail? = null,
    val members: List<GroupMemberInfo> = emptyList(),
    val isLoadingMembers: Boolean = false,
    val currentPage: Int = 1,
    val hasMoreMembers: Boolean = true,
    val isLoadingMoreMembers: Boolean = false,
    val showMemberList: Boolean = false,
    val isEditingCategory: Boolean = false,
    val newCategoryName: String = "",
    val isSharing: Boolean = false,
    val shareUrl: String? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false
)

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val tokenRepository: TokenRepository,
    private val friendRepository: FriendRepository
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
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
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
     * 保存分类名称修改
     */
    fun saveCategoryName(groupId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val groupInfo = currentState.groupInfo ?: return@launch
            
            val editRequest = com.yhchat.canary.data.api.EditGroupInfoRequest(
                groupId = groupId,
                name = groupInfo.name,
                introduction = groupInfo.introduction,
                avatarUrl = groupInfo.avatarUrl,
                directJoin = if (groupInfo.directJoin) 1 else 0,
                historyMsg = if (groupInfo.historyMsgEnabled) 1 else 0,
                categoryName = currentState.newCategoryName,
                categoryId = groupInfo.categoryId.toInt(),
                `private` = if (groupInfo.isPrivate) 1 else 0
            )
            
            groupRepository.editGroupInfo(editRequest).fold(
                onSuccess = {
                    // 更新成功，重新加载群信息
                    loadGroupInfo(groupId)
                    hideEditCategoryDialog()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message ?: "修改分类失败")
                }
            )
        }
    }
    
    /**
     * 更新免审核进群设置
     */
    fun updateDirectJoin(groupId: String, directJoin: Boolean) {
        viewModelScope.launch {
            val groupInfo = _uiState.value.groupInfo ?: return@launch
            
            val editRequest = com.yhchat.canary.data.api.EditGroupInfoRequest(
                groupId = groupId,
                name = groupInfo.name,
                introduction = groupInfo.introduction,
                avatarUrl = groupInfo.avatarUrl,
                directJoin = if (directJoin) 1 else 0,
                historyMsg = if (groupInfo.historyMsgEnabled) 1 else 0,
                categoryName = groupInfo.categoryName,
                categoryId = groupInfo.categoryId.toInt(),
                `private` = if (groupInfo.isPrivate) 1 else 0
            )
            
            groupRepository.editGroupInfo(editRequest).fold(
                onSuccess = {
                    // 更新成功，重新加载群信息
                    loadGroupInfo(groupId)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message ?: "修改免审核进群设置失败")
                }
            )
        }
    }
    
    /**
     * 更新历史消息设置
     */
    fun updateHistoryMsg(groupId: String, historyMsgEnabled: Boolean) {
        viewModelScope.launch {
            val groupInfo = _uiState.value.groupInfo ?: return@launch
            
            val editRequest = com.yhchat.canary.data.api.EditGroupInfoRequest(
                groupId = groupId,
                name = groupInfo.name,
                introduction = groupInfo.introduction,
                avatarUrl = groupInfo.avatarUrl,
                directJoin = if (groupInfo.directJoin) 1 else 0,
                historyMsg = if (historyMsgEnabled) 1 else 0,
                categoryName = groupInfo.categoryName,
                categoryId = groupInfo.categoryId.toInt(),
                `private` = if (groupInfo.isPrivate) 1 else 0
            )
            
            groupRepository.editGroupInfo(editRequest).fold(
                onSuccess = {
                    // 更新成功，重新加载群信息
                    loadGroupInfo(groupId)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message ?: "修改历史消息设置失败")
                }
            )
        }
    }
    
    /**
     * 更新群聊私有设置
     */
    fun updatePrivateSetting(groupId: String, isPrivate: Boolean) {
        viewModelScope.launch {
            val groupInfo = _uiState.value.groupInfo ?: return@launch
            
            val editRequest = com.yhchat.canary.data.api.EditGroupInfoRequest(
                groupId = groupId,
                name = groupInfo.name,
                introduction = groupInfo.introduction,
                avatarUrl = groupInfo.avatarUrl,
                directJoin = if (groupInfo.directJoin) 1 else 0,
                historyMsg = if (groupInfo.historyMsgEnabled) 1 else 0,
                categoryName = groupInfo.categoryName,
                categoryId = groupInfo.categoryId.toInt(),
                `private` = if (isPrivate) 1 else 0
            )
            
            groupRepository.editGroupInfo(editRequest).fold(
                onSuccess = {
                    // 更新成功，重新加载群信息
                    loadGroupInfo(groupId)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message ?: "修改群聊私有设置失败")
                }
            )
        }
    }
    
    /**
     * 分享群聊
     */
    fun shareGroup(groupId: String, groupName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSharing = true, error = null)
            
            groupRepository.createShare(groupId, groupName).fold(
                onSuccess = { response ->
                    if (response.code == 1 && response.data != null) {
                        _uiState.value = _uiState.value.copy(
                            isSharing = false,
                            shareUrl = "访问链接加入云湖群聊【$groupName】\n${response.data.shareUrl}share?key=${response.data.key}&ts=${response.data.ts}\n群ID: $groupId"
                            // 历史遗留懒得修改原因，此处shareUrl应为shareMsg
                        )
                        // 这里可以添加分享逻辑，比如调用系统分享功能
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSharing = false,
                            error = response.msg ?: "分享失败"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSharing = false,
                        error = error.message ?: "分享失败"
                    )
                }
            )
        }
    }
    
    /**
     * 清除分享URL
     */
    fun clearShareUrl() {
        _uiState.value = _uiState.value.copy(shareUrl = null)
    }
    
    /**
     * 删除群聊
     */
    fun delGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            
            try {
                val token = tokenRepository.getToken().first()
                if (token?.token.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(isDeleting = false, error = "未找到有效的token")
                    return@launch
                }
                
                val response = friendRepository.delFriend(token.token, groupId, 2) // 2表示群聊类型
                
                if (response.code == 1) {
                    _uiState.value = _uiState.value.copy(isDeleting = false, deleteSuccess = true)
                } else {
                    _uiState.value = _uiState.value.copy(isDeleting = false, error = response.message)
                }
            } catch (e: Exception) {
                Log.e(tag, "删除群聊失败", e)
                _uiState.value = _uiState.value.copy(isDeleting = false, error = e.message)
            }
        }
    }
}

