package com.yhchat.canary.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.yhchat.canary.data.repository.CommunityRepository
import com.yhchat.canary.data.repository.TokenRepository

/**
 * 编辑文章ViewModel
 */
class EditPostViewModel(
    private val communityRepository: CommunityRepository,
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _editPostState = MutableStateFlow(EditPostState())
    val editPostState: StateFlow<EditPostState> = _editPostState.asStateFlow()

    /**
     * 编辑文章
     */
    fun editPost(
        token: String,
        postId: Int,
        title: String,
        content: String,
        contentType: Int
    ) {
        viewModelScope.launch {
            _editPostState.value = _editPostState.value.copy(
                isLoading = true,
                error = null,
                isSuccess = false
            )

            try {
                val response = communityRepository.editPost(
                    token = token,
                    postId = postId,
                    title = title,
                    content = content,
                    contentType = contentType
                )

                if (response.code == 1) {
                    _editPostState.value = _editPostState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                } else {
                    _editPostState.value = _editPostState.value.copy(
                        isLoading = false,
                        error = response.msg ?: "编辑文章失败"
                    )
                }
            } catch (e: Exception) {
                _editPostState.value = _editPostState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络错误"
                )
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _editPostState.value = _editPostState.value.copy(error = null)
    }
}

/**
 * 编辑文章状态
 */
data class EditPostState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
