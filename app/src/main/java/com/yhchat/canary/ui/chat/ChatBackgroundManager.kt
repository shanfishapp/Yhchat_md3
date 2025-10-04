package com.yhchat.canary.ui.chat

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.ChatBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 聊天背景管理器
 */
class ChatBackgroundManager(
    private val context: Context,
    private val apiService: ApiService
) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("chat_backgrounds", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val CACHE_KEY = "background_list"
    private val CACHE_TIMESTAMP_KEY = "background_list_timestamp"
    private val CACHE_DURATION = 5 * 60 * 1000 // 5分钟缓存
    private val TAG = "ChatBackgroundManager"
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 预加载背景列表（应用启动时调用）
     */
    fun preloadBackgrounds(token: String?) {
        if (token.isNullOrEmpty()) return
        
        scope.launch {
            try {
                // 检查缓存是否有效
                if (isCacheValid()) {
                    Log.d(TAG, "Background cache is still valid, skipping preload")
                    return@launch
                }
                
                Log.d(TAG, "Preloading chat backgrounds")
                val response = apiService.getChatBackgroundList(token)
                if (response.isSuccessful && response.body()?.code == 1) {
                    val backgrounds = response.body()?.data?.list ?: emptyList()
                    // 缓存到本地
                    cacheBackgrounds(backgrounds)
                    Log.d(TAG, "Successfully preloaded ${backgrounds.size} backgrounds")
                } else {
                    Log.e(TAG, "Failed to preload backgrounds: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading backgrounds", e)
            }
        }
    }
    
    /**
     * 根据聊天ID获取背景图片URL
     */
    suspend fun getBackgroundUrlForChat(token: String?, chatId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (token.isNullOrEmpty()) return@withContext null
                
                // 获取背景列表
                val backgrounds = getBackgrounds(token) ?: return@withContext null
                
                // 首先查找匹配的chatId
                backgrounds.find { it.chatId == chatId }?.imgUrl?.let { return@withContext it }
                
                // 如果没有匹配的，查找all
                backgrounds.find { it.chatId == "all" }?.imgUrl?.let { return@withContext it }
                
                // 都没有则返回null
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error getting background for chat: $chatId", e)
                null
            }
        }
    }
    
    /**
     * 获取背景列表
     */
    private suspend fun getBackgrounds(token: String): List<ChatBackground>? {
        return withContext(Dispatchers.IO) {
            try {
                // 检查缓存
                val cachedData = getCachedBackgrounds()
                if (cachedData != null && isCacheValid()) {
                    return@withContext cachedData
                }
                
                // 从网络获取
                val response = apiService.getChatBackgroundList(token)
                if (response.isSuccessful && response.body()?.code == 1) {
                    val backgrounds = response.body()?.data?.list ?: emptyList()
                    // 缓存到本地
                    cacheBackgrounds(backgrounds)
                    backgrounds
                } else {
                    // 如果网络失败，尝试返回缓存数据
                    getCachedBackgrounds()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting backgrounds", e)
                // 如果网络失败，尝试返回缓存数据
                getCachedBackgrounds()
            }
        }
    }
    
    /**
     * 缓存背景列表到本地
     */
    private fun cacheBackgrounds(backgrounds: List<ChatBackground>) {
        try {
            val json = gson.toJson(backgrounds)
            sharedPreferences.edit()
                .putString(CACHE_KEY, json)
                .putLong(CACHE_TIMESTAMP_KEY, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error caching backgrounds", e)
        }
    }
    
    /**
     * 从本地缓存获取背景列表
     */
    private fun getCachedBackgrounds(): List<ChatBackground>? {
        return try {
            val json = sharedPreferences.getString(CACHE_KEY, null) ?: return null
            val type = object : TypeToken<List<ChatBackground>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached backgrounds", e)
            null
        }
    }
    
    /**
     * 检查缓存是否有效
     */
    private fun isCacheValid(): Boolean {
        val timestamp = sharedPreferences.getLong(CACHE_TIMESTAMP_KEY, 0)
        return System.currentTimeMillis() - timestamp < CACHE_DURATION
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        sharedPreferences.edit()
            .remove(CACHE_KEY)
            .remove(CACHE_TIMESTAMP_KEY)
            .apply()
    }
}