package com.yhchat.canary.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.*
import com.yhchat.canary.data.repository.CommunityRepository
import com.yhchat.canary.data.di.RepositoryFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 社区ViewModel
 */
class CommunityViewModel : ViewModel() {
    
    private val communityRepository: CommunityRepository = RepositoryFactory.communityRepository
    
    // 分区列表状态
    private val _boardListState = MutableStateFlow(BoardListState())
    val boardListState: StateFlow<BoardListState> = _boardListState.asStateFlow()
    
    // 文章列表状态
    private val _postListState = MutableStateFlow(CommunityPostListState())
    val postListState: StateFlow<CommunityPostListState> = _postListState.asStateFlow()
    
    // 文章详情状态
    private val _postDetailState = MutableStateFlow(PostDetailState())
    val postDetailState: StateFlow<PostDetailState> = _postDetailState.asStateFlow()
    
    // 评论列表状态
    private val _commentListState = MutableStateFlow(CommentListState())
    val commentListState: StateFlow<CommentListState> = _commentListState.asStateFlow()
    
    /**
     * 加载分区列表 - 加载所有分区
     */
    fun loadBoardList(token: String) {
        viewModelScope.launch {
            _boardListState.value = _boardListState.value.copy(isLoading = true, error = null)
            
            // 使用大的size参数来获取所有分区
            communityRepository.getBoardList(
                token = token,
                typ = 2,
                size = 1000, // 设置足够大的数量来获取所有分区
                page = 1
            ).fold(
                onSuccess = { response ->
                    _boardListState.value = _boardListState.value.copy(
                        isLoading = false,
                        boards = response.data.boards,
                        total = response.data.total,
                        currentPage = 1,
                        hasMore = false // 已加载全部，不需要更多
                    )
                },
                onFailure = { error ->
                    _boardListState.value = _boardListState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载分区列表失败"
                    )
                }
            )
        }
    }
    
    
    /**
     * 加载文章列表
     */
    fun loadPostList(token: String, baId: Int, page: Int = 1) {
        viewModelScope.launch {
            _postListState.value = _postListState.value.copy(
                isLoading = true, 
                error = null,
                boardId = baId
            )
            
            communityRepository.getPostList(
                token = token,
                baId = baId,
                typ = 1,
                size = 20,
                page = page
            ).fold(
                onSuccess = { response ->
                    _postListState.value = _postListState.value.copy(
                        isLoading = false,
                        posts = response.data.posts,
                        total = response.data.total,
                        currentPage = page,
                        hasMore = page * 20 < response.data.total
                    )
                },
                onFailure = { error ->
                    _postListState.value = _postListState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载文章列表失败"
                    )
                }
            )
        }
    }
    
    /**
     * 加载更多文章
     */
    fun loadMorePosts(token: String) {
        val currentState = _postListState.value
        if (currentState.isLoading || !currentState.hasMore || currentState.boardId == null) return
        
        loadPostList(token, currentState.boardId, currentState.currentPage + 1)
    }
    
    /**
     * 加载文章详情
     */
    fun loadPostDetail(token: String, postId: Int) {
        viewModelScope.launch {
            _postDetailState.value = _postDetailState.value.copy(isLoading = true, error = null)
            
            communityRepository.getPostDetail(token, postId).fold(
                onSuccess = { response ->
                    _postDetailState.value = _postDetailState.value.copy(
                        isLoading = false,
                        post = response.data.post,
                        board = response.data.board,
                        isAdmin = response.data.isAdmin
                    )
                },
                onFailure = { error ->
                    _postDetailState.value = _postDetailState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载文章详情失败"
                    )
                }
            )
        }
    }
    
    /**
     * 加载评论列表
     */
    fun loadCommentList(token: String, postId: Int, page: Int = 1) {
        viewModelScope.launch {
            _commentListState.value = _commentListState.value.copy(
                isLoading = true, 
                error = null
            )
            
            communityRepository.getCommentList(
                token = token,
                postId = postId,
                size = 10,
                page = page
            ).fold(
                onSuccess = { response ->
                    _commentListState.value = _commentListState.value.copy(
                        isLoading = false,
                        comments = response.data.comments,
                        total = response.data.total,
                        isAdmin = response.data.isAdmin,
                        currentPage = page,
                        hasMore = page * 10 < response.data.total
                    )
                },
                onFailure = { error ->
                    _commentListState.value = _commentListState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载评论列表失败"
                    )
                }
            )
        }
    }
    
    /**
     * 点赞文章
     */
    fun likePost(token: String, postId: Int) {
        viewModelScope.launch {
            communityRepository.likePost(token, postId).fold(
                onSuccess = {
                    // 更新本地状态
                    updatePostLikeStatus(postId, true)
                },
                onFailure = { error ->
                    _postDetailState.value = _postDetailState.value.copy(
                        error = error.message ?: "点赞失败"
                    )
                }
            )
        }
    }
    
    /**
     * 收藏文章
     */
    fun collectPost(token: String, postId: Int) {
        viewModelScope.launch {
            communityRepository.collectPost(token, postId).fold(
                onSuccess = {
                    // 更新本地状态
                    updatePostCollectStatus(postId, true)
                },
                onFailure = { error ->
                    _postDetailState.value = _postDetailState.value.copy(
                        error = error.message ?: "收藏失败"
                    )
                }
            )
        }
    }
    
    /**
     * 评论文章
     */
    fun commentPost(token: String, postId: Int, content: String, commentId: Int = 0) {
        viewModelScope.launch {
            communityRepository.commentPost(token, postId, content, commentId).fold(
                onSuccess = {
                    // 重新加载评论列表
                    loadCommentList(token, postId)
                },
                onFailure = { error ->
                    _commentListState.value = _commentListState.value.copy(
                        error = error.message ?: "评论失败"
                    )
                }
            )
        }
    }
    
    /**
     * 更新文章点赞状态
     */
    private fun updatePostLikeStatus(postId: Int, isLiked: Boolean) {
        val currentState = _postDetailState.value
        currentState.post?.let { post ->
            val updatedPost = post.copy(
                isLiked = if (isLiked) "1" else "0",
                likeNum = if (isLiked) post.likeNum + 1 else post.likeNum - 1
            )
            _postDetailState.value = currentState.copy(post = updatedPost)
        }
    }
    
    /**
     * 更新文章收藏状态
     */
    private fun updatePostCollectStatus(postId: Int, isCollected: Boolean) {
        val currentState = _postDetailState.value
        currentState.post?.let { post ->
            val updatedPost = post.copy(
                isCollected = if (isCollected) 1 else 0,
                collectNum = if (isCollected) post.collectNum + 1 else post.collectNum - 1
            )
            _postDetailState.value = currentState.copy(post = updatedPost)
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _boardListState.value = _boardListState.value.copy(error = null)
        _postListState.value = _postListState.value.copy(error = null)
        _postDetailState.value = _postDetailState.value.copy(error = null)
        _commentListState.value = _commentListState.value.copy(error = null)
    }
}

/**
 * 分区列表状态
 */
data class BoardListState(
    val isLoading: Boolean = false,
    val boards: List<CommunityBoard> = emptyList(),
    val total: Int = 0,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val error: String? = null
)

/**
 * 社区文章列表状态
 */
data class CommunityPostListState(
    val isLoading: Boolean = false,
    val posts: List<CommunityPost> = emptyList(),
    val total: Int = 0,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val boardId: Int? = null,
    val error: String? = null
)
