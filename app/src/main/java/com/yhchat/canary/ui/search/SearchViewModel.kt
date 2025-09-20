package com.yhchat.canary.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.SearchData
import com.yhchat.canary.data.repository.UserRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 搜索ViewModel
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val userRepository = UserRepository(apiService)
    private var tokenRepository: TokenRepository? = null

    fun setTokenRepository(tokenRepository: TokenRepository) {
        println("SearchViewModel: 设置tokenRepository")
        this.tokenRepository = tokenRepository
        userRepository.setTokenRepository(tokenRepository)
    }

    // UI状态
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // 搜索结果
    private val _searchResult = MutableStateFlow<SearchData?>(null)
    val searchResult: StateFlow<SearchData?> = _searchResult.asStateFlow()

    /**
     * 执行搜索
     */
    fun search(word: String) {
        if (word.isBlank()) {
            _searchResult.value = null
            _uiState.value = _uiState.value.copy(error = "请输入搜索关键词")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.homeSearch(word)
                .onSuccess { searchData ->
                    _searchResult.value = searchData
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _searchResult.value = null
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "搜索失败: ${error.message}"
                    )
                }
        }
    }

    /**
     * 清除搜索结果
     */
    fun clearSearch() {
        _searchResult.value = null
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * 搜索UI状态
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
