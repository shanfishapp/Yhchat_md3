package com.yhchat.canary.ui.expression

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.Expression
import com.yhchat.canary.data.repository.ExpressionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 表情包ViewModel
 */
class ExpressionViewModel : ViewModel() {
    
    private lateinit var expressionRepository: ExpressionRepository
    
    private val _uiState = MutableStateFlow(ExpressionUiState())
    val uiState: StateFlow<ExpressionUiState> = _uiState.asStateFlow()
    
    /**
     * 初始化
     */
    fun init(context: Context) {
        expressionRepository = RepositoryFactory.getExpressionRepository(context)
        loadExpressions()
    }
    
    /**
     * 加载表情列表
     */
    fun loadExpressions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            expressionRepository.getExpressionList().fold(
                onSuccess = { expressions ->
                    _uiState.value = _uiState.value.copy(
                        expressions = expressions,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    /**
     * 添加表情
     */
    fun addExpression(imageUrl: String) {
        viewModelScope.launch {
            expressionRepository.addExpression(imageUrl).fold(
                onSuccess = {
                    loadExpressions()  // 重新加载列表
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            )
        }
    }
    
    /**
     * 删除表情
     */
    fun deleteExpression(expressionId: Long) {
        viewModelScope.launch {
            expressionRepository.deleteExpression(expressionId).fold(
                onSuccess = {
                    loadExpressions()  // 重新加载列表
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            )
        }
    }
    
    /**
     * 置顶表情
     */
    fun topExpression(expressionId: Long) {
        viewModelScope.launch {
            expressionRepository.topExpression(expressionId).fold(
                onSuccess = {
                    loadExpressions()  // 重新加载列表
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
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
}

/**
 * 表情UI状态
 */
data class ExpressionUiState(
    val expressions: List<Expression> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

