package com.yhchat.canary.ui.chat

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 简单的聊天背景管理器
 */
class SimpleChatBackgroundManager private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("chat_backgrounds", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val BACKGROUND_MAP_KEY = "background_map"
    private val TAG = "SimpleChatBackgroundManager"
    
    companion object {
        @Volatile
        private var INSTANCE: SimpleChatBackgroundManager? = null
        
        fun getInstance(context: Context): SimpleChatBackgroundManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SimpleChatBackgroundManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 保存背景映射关系
     */
    fun saveBackgroundMapping(backgroundMap: Map<String, String>) {
        try {
            val json = gson.toJson(backgroundMap)
            sharedPreferences.edit()
                .putString(BACKGROUND_MAP_KEY, json)
                .apply()
            Log.d(TAG, "Background mapping saved, size: ${backgroundMap.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving background mapping", e)
        }
    }
    
    /**
     * 获取背景映射关系
     */
    fun getBackgroundMapping(): Map<String, String> {
        return try {
            val json = sharedPreferences.getString(BACKGROUND_MAP_KEY, null)
            if (json != null) {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting background mapping", e)
            emptyMap()
        }
    }
    
    /**
     * 根据聊天ID获取背景图片URL
     */
    fun getBackgroundUrlForChat(chatId: String): String? {
        val backgroundMap = getBackgroundMapping()
        
        // 首先查找匹配的chatId
        backgroundMap[chatId]?.let { return it }
        
        // 如果没有匹配的，查找all
        backgroundMap["all"]?.let { return it }
        
        // 都没有则返回null
        return null
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        sharedPreferences.edit()
            .remove(BACKGROUND_MAP_KEY)
            .apply()
    }
}