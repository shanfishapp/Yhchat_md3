package com.yhchat.canary.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.*
import com.yhchat.canary.data.repository.CommunityRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 文章详情ViewModel
 */
@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val tokenRepository: TokenRepository
) : ViewModel() {
    
    private val _postDetailState = MutableStateFlow(PostDetailState())
    val postDetailState: StateFlow<PostDetailState> = _postDetailState.asStateFlow()
    
    private val _commentListState = MutableStateFlow(CommentListState())
    val commentListState: StateFlow<CommentListState> = _commentListState.asStateFlow()
    
    /**
     * 使用TokenRepository获取token并加载文章详情
     */
    fun loadPostDetailWithToken(postId: Int, isRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val token = tokenRepository.getTokenSync()
                if (token != null && postId > 0) {
                    loadPostDetail(token, postId, isRefresh)
                } else {
                    _postDetailState.value = _postDetailState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (token == null) "未登录" else "无效的文章ID"
                    )
                }
            } catch (e: Exception) {
                _postDetailState.value = _postDetailState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "获取登录信息失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加载文章详情
     */
    fun loadPostDetail(token: String, postId: Int, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _postDetailState.value = _postDetailState.value.copy(isRefreshing = true, error = null)
            } else {
                _postDetailState.value = _postDetailState.value.copy(isLoading = true, error = null)
            }
            
            communityRepository.getPostDetail(token, postId)
                .onSuccess { response ->
                    _postDetailState.value = PostDetailState(
                        post = response.data.post,
                        board = response.data.board,
                        isAdmin = response.data.isAdmin,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                    
                    // 同时加载评论列表(仅在初次加载或非刷新时)
                    if (!isRefresh || _commentListState.value.comments.isEmpty()) {
                        loadCommentListWithToken(postId)
                    }
                }
                .onFailure { error ->
                    _postDetailState.value = _postDetailState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * 使用TokenRepository加载评论列表
     */
    fun loadCommentListWithToken(postId: Int, page: Int = 1) {
        viewModelScope.launch {
            try {
                val token = tokenRepository.getTokenSync()
                if (token != null) {
                    loadCommentList(token, postId, page)
                } else {
                    _commentListState.value = _commentListState.value.copy(error = "未登录")
                }
            } catch (e: Exception) {
                _commentListState.value = _commentListState.value.copy(error = "获取登录信息失败: ${e.message}")
            }
        }
    }

    /**
     * 加载评论列表
     */
    fun loadCommentList(token: String, postId: Int, page: Int = 1) {
        viewModelScope.launch {
            if (page == 1) {
                _commentListState.value = _commentListState.value.copy(isLoading = true, error = null)
            }
            
            communityRepository.getCommentList(token, postId, page = page)
                .onSuccess { response ->
                    val newComments = if (page == 1) {
                        response.data.comments
                    } else {
                        _commentListState.value.comments + response.data.comments
                    }
                    
                    _commentListState.value = CommentListState(
                        comments = newComments,
                        isAdmin = response.data.isAdmin,
                        total = response.data.total,
                        currentPage = page,
                        hasMore = newComments.size < response.data.total,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _commentListState.value = _commentListState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * 使用TokenRepository点赞文章
     */
    fun likePostWithToken(postId: Int) {
        viewModelScope.launch {
            try {
                val token = tokenRepository.getTokenSync()
                if (token != null) {
                    likePost(token, postId)
                } else {
                    _postDetailState.value = _postDetailState.value.copy(error = "未登录")
                }
            } catch (e: Exception) {
                _postDetailState.value = _postDetailState.value.copy(error = "获取登录信息失败: ${e.message}")
            }
        }
    }

    /**
     * 使用TokenRepository收藏文章
     */
    fun collectPostWithToken(postId: Int) {
        viewModelScope.launch {
            try {
                val token = tokenRepository.getTokenSync()
                if (token != null) {
                    collectPost(token, postId)
                } else {
                    _postDetailState.value = _postDetailState.value.copy(error = "未登录")
                }
            } catch (e: Exception) {
                _postDetailState.value = _postDetailState.value.copy(error = "获取登录信息失败: ${e.message}")
            }
        }
    }

    /**
     * 使用TokenRepository打赏文章
     */
    fun rewardPostWithToken(postId: Int, amount: Double) {
        viewModelScope.launch {
            try {
                val token = tokenRepository.getTokenSync()
                if (token != null) {
                    rewardPost(token, postId, amount)
                } else {
                    _postDetailState.value = _postDetailState.value.copy(error = "未登录")
                }
            } catch (e: Exception) {
                _postDetailState.value = _postDetailState.value.copy(error = "获取登录信息失败: ${e.message}")
            }
        }
    }

    /**
     * 使用TokenRepository评论文章
     */
    fun commentPostWithToken(postId: Int, content: String, commentId: Int = 0) {
        viewModelScope.launch {
            try {
                val token = tokenRepository.getTokenSync()
                if (token != null) {
                    commentPost(token, postId, content, commentId)
                } else {
                    _commentListState.value = _commentListState.value.copy(error = "未登录")
                }
            } catch (e: Exception) {
                _commentListState.value = _commentListState.value.copy(error = "获取登录信息失败: ${e.message}")
            }
        }
    }

    /**
     * 使用TokenRepository加载更多评论
     */
    fun loadMoreCommentsWithToken(postId: Int) {
        val currentPage = _commentListState.value.currentPage
        if (_commentListState.value.hasMore && !_commentListState.value.isLoading) {
            loadCommentListWithToken(postId, currentPage + 1)
        }
    }

    /**
     * 使用TokenRepository点赞评论
     */
    fun likeCommentWithToken(postId: Int, commentId: Int) {
        viewModelScope.launch {
            try {
                val token = tokenRepository.getTokenSync()
                if (token != null) {
                    likeComment(token, postId, commentId)
                } else {
                    _commentListState.value = _commentListState.value.copy(error = "未登录")
                }
            } catch (e: Exception) {
                _commentListState.value = _commentListState.value.copy(error = "获取登录信息失败: ${e.message}")
            }
        }
    }

    /**
     * 点赞文章
     */
    fun likePost(token: String, postId: Int) {
        viewModelScope.launch {
            communityRepository.likePost(token, postId)
                .onSuccess {
                    // 重新加载文章详情以获取最新状态
                    loadPostDetail(token, postId)
                }
                .onFailure { error ->
                    _postDetailState.value = _postDetailState.value.copy(error = error.message)
                }
        }
    }
    
    /**
     * 收藏文章
     */
    fun collectPost(token: String, postId: Int) {
        viewModelScope.launch {
            communityRepository.collectPost(token, postId)
                .onSuccess {
                    // 重新加载文章详情以获取最新状态
                    loadPostDetail(token, postId)
                }
                .onFailure { error ->
                    _postDetailState.value = _postDetailState.value.copy(error = error.message)
                }
        }
    }
    
    /**
     * 打赏文章
     */
    fun rewardPost(token: String, postId: Int, amount: Double) {
        viewModelScope.launch {
            communityRepository.rewardPost(token, postId, amount)
                .onSuccess {
                    // 重新加载文章详情以获取最新状态
                    loadPostDetail(token, postId)
                }
                .onFailure { error ->
                    _postDetailState.value = _postDetailState.value.copy(error = error.message)
                }
        }
    }
    
    /**
     * 评论文章
     */
    fun commentPost(token: String, postId: Int, content: String, commentId: Int = 0) {
        viewModelScope.launch {
            communityRepository.commentPost(token, postId, content, commentId)
                .onSuccess {
                    // 重新加载评论列表
                    loadCommentList(token, postId)
                }
                .onFailure { error ->
                    _commentListState.value = _commentListState.value.copy(error = error.message)
                }
        }
    }
    
    /**
     * 加载更多评论
     */
    fun loadMoreComments(token: String, postId: Int) {
        val currentPage = _commentListState.value.currentPage
        if (_commentListState.value.hasMore && !_commentListState.value.isLoading) {
            loadCommentList(token, postId, currentPage + 1)
        }
    }
    
    /**
     * 点赞评论
     */
    fun likeComment(token: String, postId: Int, commentId: Int) {
        viewModelScope.launch {
            communityRepository.likeComment(token, commentId)
                .onSuccess {
                    // 重新加载评论列表以获取最新状态
                    loadCommentList(token, postId, 1)
                }
                .onFailure { error ->
                    _commentListState.value = _commentListState.value.copy(error = error.message)
                }
        }
    }
    
    /**
     * 使用TokenRepository刷新文章详情
     */
    fun refreshPostDetailWithToken(postId: Int) {
        loadPostDetailWithToken(postId, isRefresh = true)
    }

    /**
     * 刷新文章详情
     */
    fun refreshPostDetail(token: String, postId: Int) {
        loadPostDetail(token, postId, isRefresh = true)
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _postDetailState.value = _postDetailState.value.copy(error = null)
        _commentListState.value = _commentListState.value.copy(error = null)
    }
}

/**
 * 文章详情状态
 */
data class PostDetailState(
    val post: CommunityPost? = null,
    val board: CommunityBoard? = null,
    val isAdmin: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

/**
 * 评论列表状态
 */
data class CommentListState(
    val comments: List<CommunityComment> = emptyList(),
    val isAdmin: Int = 0,
    val total: Int = 0,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
