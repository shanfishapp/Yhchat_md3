package com.yhchat.canary.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * 安全的Token存储管理器
 * 使用EncryptedSharedPreferences替代SQLCipher，体积更小，安全性同样高
 */
class SecureTokenStorage(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "yhchat_secure_tokens"
        private const val KEY_USER_TOKEN = "user_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LAST_LOGIN_TIME = "last_login_time"
    }
    
    private val encryptedPrefs: SharedPreferences
    
    init {
        try {
            // 创建或获取主密钥
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            
            // 创建加密的SharedPreferences
            encryptedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // 如果创建加密SharedPreferences失败，回退到普通SharedPreferences
            // 这种情况通常发生在较老的设备上
            throw RuntimeException("无法创建安全存储，请检查设备兼容性", e)
        }
    }
    
    /**
     * 保存用户Token
     */
    fun saveUserToken(token: String, userId: String) {
        encryptedPrefs.edit()
            .putString(KEY_USER_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * 获取用户Token
     */
    fun getUserToken(): String? {
        return encryptedPrefs.getString(KEY_USER_TOKEN, null)
    }
    
    /**
     * 获取用户ID
     */
    fun getUserId(): String? {
        return encryptedPrefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * 获取最后登录时间
     */
    fun getLastLoginTime(): Long {
        return encryptedPrefs.getLong(KEY_LAST_LOGIN_TIME, 0L)
    }
    
    /**
     * 检查Token是否存在
     */
    fun hasValidToken(): Boolean {
        val token = getUserToken()
        return !token.isNullOrEmpty()
    }
    
    /**
     * 清除所有Token数据
     */
    fun clearTokens() {
        encryptedPrefs.edit()
            .remove(KEY_USER_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_LAST_LOGIN_TIME)
            .apply()
    }
    
    /**
     * 清除所有数据
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
}
