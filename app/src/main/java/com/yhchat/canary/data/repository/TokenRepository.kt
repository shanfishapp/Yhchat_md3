package com.yhchat.canary.data.repository

import com.yhchat.canary.data.local.AppDatabase
import com.yhchat.canary.data.local.UserToken
import com.yhchat.canary.data.local.UserTokenDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Token存储仓库
 */
class TokenRepository(private val userTokenDao: UserTokenDao) {
    
    /**
     * 保存Token
     */
    suspend fun saveToken(token: String) {
        val userToken = UserToken(token = token)
        userTokenDao.insertToken(userToken)
    }
    
    /**
     * 获取Token
     */
    fun getToken(): Flow<UserToken?> {
        return userTokenDao.getToken()
    }
    
    /**
     * 同步获取Token
     */
    suspend fun getTokenSync(): String? {
        return userTokenDao.getTokenSync()?.token
    }
    
    /**
     * 清除Token
     */
    suspend fun clearToken() {
        userTokenDao.clearTokens()
    }
    
    /**
     * 检查是否已登录
     */
    suspend fun isLoggedIn(): Boolean {
        return getTokenSync() != null
    }
}
