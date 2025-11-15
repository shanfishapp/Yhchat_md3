package com.yhchat.canary.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.ChatSearchMessage
import com.yhchat.canary.data.model.ChatSearchRequest
import com.yhchat.canary.data.model.ChatMessage
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Inject
import android.util.Log

data class ChatSearchState(
    val isLoading: Boolean = false,
    val searchResults: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val isLoadingMore: Boolean = false,
    val hasMoreResults: Boolean = true,
    val currentPage: Int = 0,
    val selectedMessage: ChatMessage? = null,
    val isLoadingMessageDetail: Boolean = false
)

@HiltViewModel
class ChatSearchViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenRepository: TokenRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ChatSearchState())
    val state: StateFlow<ChatSearchState> = _state.asStateFlow()
    
    companion object {
        private const val TAG = "ChatSearchViewModel"
    }
    
    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }
    
    fun searchMessages(chatId: String, chatType: Int, query: String, isLoadMore: Boolean = false) {
        Log.d(TAG, "searchMessages: query='$query', chatId='$chatId', chatType=$chatType, isLoadMore=$isLoadMore")
        
        if (query.isBlank()) {
            Log.d(TAG, "searchMessages: query is blank, clearing results")
            _state.value = _state.value.copy(
                searchResults = emptyList(),
                currentPage = 0,
                hasMoreResults = true
            )
            return
        }
        
        viewModelScope.launch {
            if (isLoadMore) {
                Log.d(TAG, "searchMessages: loading more results, current page: ${_state.value.currentPage}")
                _state.value = _state.value.copy(isLoadingMore = true)
            } else {
                Log.d(TAG, "searchMessages: starting new search")
                _state.value = _state.value.copy(
                    isLoading = true, 
                    error = null,
                    currentPage = 0,
                    hasMoreResults = true
                )
            }
            
            try {
                val userToken = tokenRepository.getToken().first()
                val token = userToken?.token
                Log.d(TAG, "searchMessages: got token, length=${token?.length ?: 0}")
                
                if (token.isNullOrBlank()) {
                    Log.w(TAG, "searchMessages: token is null or blank")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = "用户未登录"
                    )
                    return@launch
                }
                
                val currentPage = if (isLoadMore) _state.value.currentPage + 1 else 0
                val pageSize = 30
                val totalSize = pageSize * (currentPage + 1)
                Log.d(TAG, "searchMessages: requesting page=$currentPage, pageSize=$pageSize, totalSize=$totalSize")
                
                val request = ChatSearchRequest(
                    word = query,
                    chatId = chatId,
                    chatType = chatType,
                    size = totalSize // 累积请求数量
                )
                
                val response = apiService.searchChatMessages(token, request)
                Log.d(TAG, "searchMessages: API response received, isSuccessful=${response.isSuccessful}, code=${response.code()}")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d(TAG, "searchMessages: response body code=${body?.code}, msg='${body?.msg}'")
                    
                    if (body?.code == 1) {
                        val searchResults = body.data?.list ?: emptyList()
                        Log.d(TAG, "searchMessages: got ${searchResults.size} search results")
                        
                        if (searchResults.isEmpty() && !isLoadMore) {
                            Log.d(TAG, "searchMessages: no results found for new search")
                            _state.value = _state.value.copy(
                                isLoading = false,
                                isLoadingMore = false,
                                searchResults = emptyList(),
                                hasMoreResults = false,
                                error = null
                            )
                            return@launch
                        }
                        
                        // 并发获取每个消息的完整内容
                        Log.d(TAG, "searchMessages: fetching full message content for ${searchResults.size} messages")
                        val fullMessages = searchResults.map { searchMessage ->
                            async {
                                try {
                                    Log.v(TAG, "searchMessages: fetching message ${searchMessage.id}")
                                    val message = messageRepository.getMessageById(searchMessage.id)
                                    if (message != null) {
                                        Log.v(TAG, "searchMessages: got message ${searchMessage.id}, type=${message.contentType}")
                                    } else {
                                        Log.w(TAG, "searchMessages: message ${searchMessage.id} not found in repository")
                                    }
                                    message
                                } catch (e: Exception) {
                                    Log.e(TAG, "searchMessages: failed to get message ${searchMessage.id}", e)
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                        
                        Log.d(TAG, "searchMessages: successfully fetched ${fullMessages.size} full messages")
                        
                        val currentResults = if (isLoadMore) _state.value.searchResults else emptyList()
                        val newResults = if (isLoadMore) {
                            // 去重：只添加不在当前结果中的消息
                            val existingIds = currentResults.map { it.msgId }.toSet()
                            val filtered = fullMessages.filter { it.msgId !in existingIds }
                            Log.d(TAG, "searchMessages: filtered ${filtered.size} new messages from ${fullMessages.size} total")
                            filtered
                        } else {
                            fullMessages
                        }
                        
                        val hasMore = searchResults.size >= pageSize
                        val finalResults = currentResults + newResults
                        
                        Log.d(TAG, "searchMessages: final results count=${finalResults.size}, hasMore=$hasMore, currentPage=$currentPage")
                        
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            searchResults = finalResults,
                            currentPage = currentPage,
                            hasMoreResults = hasMore,
                            error = null
                        )
                    } else {
                        Log.w(TAG, "searchMessages: API returned error code=${body?.code}, msg='${body?.msg}'")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = body?.msg ?: "搜索失败"
                        )
                    }
                } else {
                    Log.e(TAG, "searchMessages: HTTP error ${response.code()}: ${response.message()}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = "网络请求失败: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "searchMessages: exception occurred", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = "搜索出错: ${e.message}"
                )
            }
        }
    }
    
    fun loadMoreResults(chatId: String, chatType: Int, query: String) {
        Log.d(TAG, "loadMoreResults: isLoadingMore=${_state.value.isLoadingMore}, hasMoreResults=${_state.value.hasMoreResults}")
        if (_state.value.isLoadingMore || !_state.value.hasMoreResults) {
            Log.d(TAG, "loadMoreResults: skipping - already loading or no more results")
            return
        }
        Log.d(TAG, "loadMoreResults: triggering search for more results")
        searchMessages(chatId, chatType, query, isLoadMore = true)
    }
    
    fun showMessageDetail(message: ChatMessage) {
        Log.d(TAG, "showMessageDetail: showing detail for message ${message.msgId}")
        _state.value = _state.value.copy(selectedMessage = message)
    }
    
    fun hideMessageDetail() {
        Log.d(TAG, "hideMessageDetail: hiding message detail")
        _state.value = _state.value.copy(selectedMessage = null)
    }
    
    fun getMessageDetail(chatId: String, chatType: Int, messageId: String) {
        Log.d(TAG, "getMessageDetail: fetching detail for message $messageId")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMessageDetail = true)
            
            try {
                val userToken = tokenRepository.getToken().first()
                val token = userToken?.token
                Log.d(TAG, "getMessageDetail: got token, length=${token?.length ?: 0}")
                
                if (token.isNullOrBlank()) {
                    Log.w(TAG, "getMessageDetail: token is null or blank")
                    _state.value = _state.value.copy(
                        isLoadingMessageDetail = false,
                        error = "用户未登录"
                    )
                    return@launch
                }
                
                // 使用MessageRepository获取消息详情
                Log.d(TAG, "getMessageDetail: calling messageRepository.getMessageById($messageId)")
                val message = messageRepository.getMessageById(messageId)
                if (message != null) {
                    Log.d(TAG, "getMessageDetail: successfully got message detail, type=${message.contentType}")
                    _state.value = _state.value.copy(
                        isLoadingMessageDetail = false,
                        selectedMessage = message
                    )
                } else {
                    Log.w(TAG, "getMessageDetail: message not found in repository")
                    _state.value = _state.value.copy(
                        isLoadingMessageDetail = false,
                        error = "无法获取消息详情"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "getMessageDetail: exception occurred", e)
                _state.value = _state.value.copy(
                    isLoadingMessageDetail = false,
                    error = "获取消息详情失败: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        Log.d(TAG, "clearError: clearing error state")
        _state.value = _state.value.copy(error = null)
    }
}
