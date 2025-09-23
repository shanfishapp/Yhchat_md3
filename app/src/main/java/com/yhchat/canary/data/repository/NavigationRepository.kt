package com.yhchat.canary.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yhchat.canary.data.model.NavigationConfig
import com.yhchat.canary.data.model.NavigationItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导航配置数据仓库
 */
@Singleton
class NavigationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("navigation_config", Context.MODE_PRIVATE)
    
    private val gson = Gson()
    
    private val _navigationConfig = MutableStateFlow(loadNavigationConfig())
    val navigationConfig: StateFlow<NavigationConfig> = _navigationConfig.asStateFlow()
    
    companion object {
        private const val KEY_NAVIGATION_CONFIG = "navigation_config"
    }
    
    /**
     * 加载导航配置
     */
    private fun loadNavigationConfig(): NavigationConfig {
        val configJson = sharedPreferences.getString(KEY_NAVIGATION_CONFIG, null)
        return if (configJson != null) {
            try {
                gson.fromJson(configJson, NavigationConfig::class.java)
            } catch (e: Exception) {
                NavigationConfig.getDefault()
            }
        } else {
            NavigationConfig.getDefault()
        }
    }
    
    /**
     * 保存导航配置
     */
    fun saveNavigationConfig(config: NavigationConfig) {
        val configJson = gson.toJson(config)
        sharedPreferences.edit()
            .putString(KEY_NAVIGATION_CONFIG, configJson)
            .apply()
        
        _navigationConfig.value = config
    }
    
    /**
     * 更新导航项的可见性
     */
    fun updateItemVisibility(itemId: String, isVisible: Boolean) {
        val currentConfig = _navigationConfig.value
        val updatedItems = currentConfig.items.map { item ->
            if (item.id == itemId) {
                item.copy(isVisible = isVisible)
            } else {
                item
            }
        }
        
        val updatedConfig = currentConfig.copy(items = updatedItems)
        saveNavigationConfig(updatedConfig)
    }
    
    /**
     * 更新导航项的顺序
     */
    fun updateItemsOrder(orderedItems: List<NavigationItem>) {
        val updatedItems = orderedItems.mapIndexed { index, item ->
            item.copy(order = index)
        }
        
        val currentConfig = _navigationConfig.value
        val updatedConfig = currentConfig.copy(items = updatedItems)
        saveNavigationConfig(updatedConfig)
    }
    
    /**
     * 重置为默认配置
     */
    fun resetToDefault() {
        saveNavigationConfig(NavigationConfig.getDefault())
    }
    
    /**
     * 获取可见的导航项
     */
    fun getVisibleItems(): List<NavigationItem> {
        return _navigationConfig.value.getVisibleItems()
    }
    
    /**
     * 获取所有可用的导航项
     */
    fun getAllAvailableItems(): List<NavigationItem> {
        return NavigationConfig.getAllAvailableItems()
    }
}
