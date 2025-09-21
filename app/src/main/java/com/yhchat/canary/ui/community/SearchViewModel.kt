package com.yhchat.canary.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.model.CommunityBoard
import com.yhchat.canary.data.model.CommunityPost
import com.yhchat.canary.data.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 搜索状态
 */
data class SearchState(
    val boards: List<CommunityBoard> = emptyList(),
    val posts: List<CommunityPost> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val keyword: String = ""
)

/**
 * 搜索ViewModel
 */
class SearchViewModel(
    private val communityRepository: CommunityRepository
) : ViewModel() {
    
    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    
    /**
     * 搜索
     */
    fun search(token: String, keyword: String) {
        viewModelScope.launch {
            try {
                _searchState.value = _searchState.value.copy(
                    isLoading = true,
                    error = null,
                    keyword = keyword
                )
                
                val response = communityRepository.searchCommunity(
                    token = token,
                    keyword = keyword,
                    typ = 3, // 根据API文档，typ为3
                    page = 1,
                    size = 50
                )
                
                if (response.code == 1) {
                    _searchState.value = _searchState.value.copy(
                        boards = response.data.boards ?: emptyList(),
                        posts = response.data.posts ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _searchState.value = _searchState.value.copy(
                        error = response.msg,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _searchState.value = _searchState.value.copy(
                    error = "搜索失败: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * 清空搜索结果
     */
    fun clearResults() {
        _searchState.value = SearchState()
    }
}
