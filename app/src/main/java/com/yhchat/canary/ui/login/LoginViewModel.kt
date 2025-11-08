package com.yhchat.canary.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.CaptchaData
import com.yhchat.canary.data.model.LoginData
import com.yhchat.canary.data.repository.UserRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录ViewModel
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val userRepository: UserRepository
    private var tokenRepository: TokenRepository? = null

    init {
        userRepository = UserRepository(apiService, null)
        // 注意：tokenRepository 会在后续通过 setTokenRepository 方法设置
    }


    fun setTokenRepository(tokenRepository: TokenRepository?) {
        this.tokenRepository = tokenRepository
        if (tokenRepository != null) {
            userRepository.setTokenRepository(tokenRepository)
        }
    }

    // UI状态
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    // 验证码数据
    private val _captchaData = MutableStateFlow<CaptchaData?>(null)
    val captchaData: StateFlow<CaptchaData?> = _captchaData.asStateFlow()
    
    /**
     * 获取验证码
     */
    fun getCaptcha() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.getCaptcha()
                .onSuccess { captcha ->
                    _captchaData.value = captcha
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }

    /**
     * 获取短信验证码
     */
    fun getSmsCaptcha(mobile: String, captchaCode: String) {
        if (mobile.isBlank() || captchaCode.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入手机号和图片验证码")
            return
        }

        val captchaData = _captchaData.value
        if (captchaData == null) {
            _uiState.value = _uiState.value.copy(error = "请先获取图片验证码")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.getSmsCaptcha(mobile, captchaCode, captchaData.id)
                .onSuccess { response ->
                    // 检查响应是否包含success字符
                    val isSuccess = response.toString().contains("success", ignoreCase = true)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        smsSuccess = isSuccess
                    )
                    if (isSuccess) {
                        println("短信验证码发送成功")
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "获取短信验证码失败: ${error.message}",
                        smsSuccess = false
                    )
                }
        }
    }
    
    /**
     * 验证码登录
     */
    fun loginWithCaptcha(mobile: String, captcha: String) {
        if (mobile.isBlank() || captcha.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入手机号和验证码")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            userRepository.verificationLogin(mobile, captcha, generateDeviceId())
                .onSuccess { loginData ->
                    println("登录成功: $loginData")
                    // 保存Token到数据库
                    tokenRepository?.saveToken(loginData.token)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loginSuccess = true,
                        loginData = loginData
                    )
                }
                .onFailure { error ->
                    println("登录失败: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "登录失败: ${error.message}"
                    )
                }
        }
    }
    
    /**
     * 邮箱登录
     */
    fun loginWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入邮箱和密码")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            userRepository.emailLogin(email, password, generateDeviceId())
                .onSuccess { loginData ->
                    println("邮箱登录成功: $loginData")
                    // 保存Token到数据库
                    tokenRepository?.saveToken(loginData.token)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loginSuccess = true,
                        loginData = loginData
                    )
                }
                .onFailure { error ->
                    println("邮箱登录失败: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "登录失败: ${error.message}"
                    )
                }
        }
    }
    
    /**
     * Token登录
     */
    fun loginWithToken(token: String) {
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入Token")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // 直接保存Token并标记登录成功
                tokenRepository?.saveToken(token)
                
                // 创建LoginData对象
                val loginData = LoginData(
                    token = token
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loginSuccess = true,
                    loginData = loginData
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Token登录失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 生成设备ID
     */
    private fun generateDeviceId(): String {
        return java.util.UUID.randomUUID().toString()
    }
}

/**
 * 登录UI状态
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val loginData: LoginData? = null,
    val smsSuccess: Boolean = false
)
