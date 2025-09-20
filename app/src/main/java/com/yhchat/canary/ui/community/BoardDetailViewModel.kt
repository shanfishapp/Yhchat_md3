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
 * 分区详情ViewModel
 */
@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val tokenRepository: TokenRepository
) : ViewModel() {
    
    private val _boardDetailState = MutableStateFlow(BoardDetailState())
    val boardDetailState: StateFlow<BoardDetailState> = _boardDetailState.asStateFlow()
    
    private val _postListState = MutableStateFlow(PostListState())
    val postListState: StateFlow<PostListState> = _postListState.asStateFlow()
    
    /**
     * 加载分区详情
     */
    fun loadBoardDetail(token: String, boardId: Int) {
        viewModelScope.launch {
            _boardDetailState.value = _boardDetailState.value.copy(isLoading = true, error = null)
            
            communityRepository.getBoardInfo(token, boardId)
                .onSuccess { response ->
                    _boardDetailState.value = BoardDetailState(
                        board = response.data.board,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _boardDetailState.value = _boardDetailState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * 加载文章列表
     */
    fun loadPostList(token: String, boardId: Int, page: Int = 1) {
        viewModelScope.launch {
            if (page == 1) {
                _postListState.value = _postListState.value.copy(isLoading = true, error = null)
            }
            
            communityRepository.getPostList(token, boardId, page = page)
                .onSuccess { response ->
                    val newPosts = if (page == 1) {
                        response.data.posts
                    } else {
                        _postListState.value.posts + response.data.posts
                    }
                    
                    _postListState.value = PostListState(
                        posts = newPosts,
                        total = response.data.total,
                        currentPage = page,
                        hasMore = newPosts.size < response.data.total,
                        boardId = boardId,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _postListState.value = _postListState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * 加载更多文章
     */
    fun loadMorePosts(token: String, boardId: Int) {
        val currentPage = _postListState.value.currentPage
        if (_postListState.value.hasMore && !_postListState.value.isLoading) {
            loadPostList(token, boardId, currentPage + 1)
        }
    }
    
    /**
     * 刷新数据
     */
    fun refresh(token: String, boardId: Int) {
        loadBoardDetail(token, boardId)
        loadPostList(token, boardId, 1)
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _boardDetailState.value = _boardDetailState.value.copy(error = null)
        _postListState.value = _postListState.value.copy(error = null)
    }
}

/**
 * 分区详情状态
 */
data class BoardDetailState(
    val board: CommunityBoard? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 文章列表状态
 */
data class PostListState(
    val isLoading: Boolean = false,
    val posts: List<CommunityPost> = emptyList(),
    val total: Int = 0,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val boardId: Int? = null,
    val error: String? = null
)
