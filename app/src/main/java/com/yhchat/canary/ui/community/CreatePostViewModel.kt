package com.yhchat.canary.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.repository.CommunityRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 创建文章ViewModel
 */
@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val tokenRepository: TokenRepository
) : ViewModel() {
    
    private val _createPostState = MutableStateFlow(CreatePostState())
    val createPostState: StateFlow<CreatePostState> = _createPostState.asStateFlow()
    
    /**
     * 创建文章
     */
    fun createPost(
        token: String,
        boardId: Int,
        title: String,
        content: String,
        contentType: Int,
        groupId: String = ""
    ) {
        viewModelScope.launch {
            _createPostState.value = _createPostState.value.copy(
                isLoading = true,
                error = null,
                isSuccess = false
            )
            
            communityRepository.createPost(
                token = token,
                boardId = boardId,
                title = title,
                content = content,
                contentType = contentType,
                groupId = groupId
            )
                .onSuccess { response ->
                    _createPostState.value = CreatePostState(
                        isLoading = false,
                        isSuccess = true,
                        postId = response.data.postId,
                        error = null
                    )
                }
                .onFailure { error ->
                    _createPostState.value = _createPostState.value.copy(
                        isLoading = false,
                        error = error.message ?: "发布失败"
                    )
                }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _createPostState.value = _createPostState.value.copy(error = null)
    }
}

/**
 * 创建文章状态
 */
data class CreatePostState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val postId: Int? = null,
    val error: String? = null
)
