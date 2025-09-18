package com.yhchat.canary.data.repository

import com.yhchat.canary.data.api.ApiClient
import com.yhchat.canary.data.model.*
import retrofit2.Response

/**
 * 用户数据仓库
 */
class UserRepository {
    
    private val apiService = ApiClient.apiService
    private var tokenRepository: TokenRepository? = null
    
    fun setTokenRepository(tokenRepository: TokenRepository) {
        this.tokenRepository = tokenRepository
    }
    
    private suspend fun getToken(): String? {
        return tokenRepository?.getTokenSync()
    }
    
    /**
     * 获取用户信息
     */
    suspend fun getUserInfo(): Result<User> {
        return try {
            val token = getToken() ?: return Result.failure(Exception("未登录"))
            val response = apiService.getUserInfo(token)
            if (response.isSuccessful) {
                response.body()?.let { user ->
                    Result.success(user)
                } ?: Result.failure(Exception("用户信息为空"))
            } else {
                Result.failure(Exception("获取用户信息失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取验证码
     */
    suspend fun getCaptcha(): Result<CaptchaData> {
        return try {
            println("开始获取验证码...")
            val response = apiService.getCaptcha()
            println("验证码API响应状态: ${response.code()}")
            println("验证码API响应体: ${response.body()}")
            if (response.isSuccessful) {
                val captchaResponse = response.body()
                if (captchaResponse?.code == 1 && captchaResponse.data != null) {
                    Result.success(captchaResponse.data)
                } else {
                    Result.failure(Exception(captchaResponse?.message ?: "获取验证码失败"))
                }
            } else {
                Result.failure(Exception("获取验证码失败: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            println("获取验证码异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 验证码登录
     */
    suspend fun verificationLogin(mobile: String, captcha: String, deviceId: String): Result<LoginData> {
        return try {
            val request = LoginRequest(
                mobile = mobile,
                captcha = captcha,
                deviceId = deviceId,
                platform = "android"
            )
            val response = apiService.verificationLogin(request)
            println("API响应状态: ${response.code()}")
            println("API响应体: ${response.body()}")
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse?.code == 1 && loginResponse.data != null) {
                    Result.success(loginResponse.data)
                } else {
                    Result.failure(Exception(loginResponse?.message ?: "登录失败"))
                }
            } else {
                Result.failure(Exception("登录失败: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 邮箱登录
     */
    suspend fun emailLogin(email: String, password: String, deviceId: String): Result<LoginData> {
        return try {
            val request = LoginRequest(
                email = email,
                password = password,
                deviceId = deviceId,
                platform = "android"
            )
            val response = apiService.emailLogin(request)
            println("邮箱登录API响应状态: ${response.code()}")
            println("邮箱登录API响应体: ${response.body()}")
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse?.code == 1 && loginResponse.data != null) {
                    Result.success(loginResponse.data)
                } else {
                    Result.failure(Exception(loginResponse?.message ?: "登录失败"))
                }
            } else {
                Result.failure(Exception("登录失败: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 退出登录
     */
    suspend fun logout(deviceId: String): Result<Boolean> {
        return try {
            val token = getToken() ?: return Result.failure(Exception("未登录"))
            val request = mapOf("deviceId" to deviceId)
            val response = apiService.logout(token, request)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("退出登录失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
