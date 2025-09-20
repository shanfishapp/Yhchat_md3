package com.yhchat.canary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.local.AppDatabase
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主界面ViewModel
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) : ViewModel() {

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _tokenRepository = MutableStateFlow<TokenRepository?>(null)
    val tokenRepository: StateFlow<TokenRepository?> = _tokenRepository.asStateFlow()

    private val _userRepository = MutableStateFlow<UserRepository?>(null)
    val userRepository: StateFlow<UserRepository?> = _userRepository.asStateFlow()

    private val _savedToken = MutableStateFlow<String?>(null)
    val savedToken: StateFlow<String?> = _savedToken.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        initializeDependencies()
    }

    /**
     * 初始化依赖
     */
    private fun initializeDependencies() {
        viewModelScope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val tokenRepo = TokenRepository(database.userTokenDao())
                val userRepo = UserRepository(apiService).apply {
                    setTokenRepository(tokenRepo)
                }

                _tokenRepository.value = tokenRepo
                _userRepository.value = userRepo

                // 检查是否已登录
                val savedToken = tokenRepo.getTokenSync()
                if (savedToken != null) {
                    _savedToken.value = savedToken
                    _isLoggedIn.value = true

                    // 获取用户信息
                    userRepo.getUserInfo().onSuccess { user ->
                        _userId.value = user.id
                    }.onFailure {
                        // 如果获取用户信息失败，使用token的后8位作为userId
                        _userId.value = "user_${savedToken.takeLast(8)}"
                    }
                }

                _isInitialized.value = true
            } catch (e: Exception) {
                _isInitialized.value = true // 即使出错也要设置为已初始化
            }
        }
    }

    /**
     * 登录成功后更新状态
     */
    fun onLoginSuccess(token: String, userId: String) {
        _savedToken.value = token
        _userId.value = userId
        _isLoggedIn.value = true
    }
}
