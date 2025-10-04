package com.yhchat.canary.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.ChatBackground
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ChatBackgroundRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenRepository: TokenRepository,
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("chat_backgrounds", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val CACHE_KEY = "background_list"
    private val CACHE_TIMESTAMP_KEY = "background_list_timestamp"
    private val CACHE_DURATION = 5 * 60 * 1000 // 5分钟缓存
    
    /**
     * 获取聊天背景列表
     */
    suspend fun getChatBackgrounds(): Result<List<ChatBackground>> {
        return try {
            // 检查缓存
            val cachedData = getCachedBackgrounds()
            if (cachedData != null && isCacheValid()) {
                return Result.success(cachedData)
            }
            
            // 从网络获取
            val token = tokenRepository.getToken().first()?.token
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("用户未登录"))
            }
            
            val response = apiService.getChatBackgroundList(token)
            if (response.isSuccessful && response.body()?.code == 1) {
                val backgrounds = response.body()?.data?.list ?: emptyList()
                // 缓存到本地
                cacheBackgrounds(backgrounds)
                Result.success(backgrounds)
            } else {
                Result.failure(Exception(response.body()?.msg ?: "获取背景列表失败"))
            }
        } catch (e: Exception) {
            // 如果网络失败，尝试返回缓存数据
            val cachedData = getCachedBackgrounds()
            if (cachedData != null) {
                Result.success(cachedData)
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 根据聊天ID获取背景图片URL
     */
    suspend fun getBackgroundUrlForChat(chatId: String): String? {
        val backgrounds = getChatBackgrounds().getOrNull() ?: return null
        
        // 首先查找匹配的chatId
        backgrounds.find { it.chatId == chatId }?.imgUrl?.let { return it }
        
        // 如果没有匹配的，查找all
        backgrounds.find { it.chatId == "all" }?.imgUrl?.let { return it }
        
        // 都没有则返回null
        return null
    }
    
    /**
     * 缓存背景列表到本地
     */
    private fun cacheBackgrounds(backgrounds: List<ChatBackground>) {
        val json = gson.toJson(backgrounds)
        sharedPreferences.edit()
            .putString(CACHE_KEY, json)
            .putLong(CACHE_TIMESTAMP_KEY, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * 从本地缓存获取背景列表
     */
    private fun getCachedBackgrounds(): List<ChatBackground>? {
        val json = sharedPreferences.getString(CACHE_KEY, null) ?: return null
        return try {
            val type = object : TypeToken<List<ChatBackground>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
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