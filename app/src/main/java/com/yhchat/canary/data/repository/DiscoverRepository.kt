package com.yhchat.canary.data.repository

import android.util.Log
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 发现功能Repository
 */
class DiscoverRepository(
    private val apiService: ApiService,
    private val tokenRepository: TokenRepository
) {
    companion object {
        private const val TAG = "DiscoverRepository"
    }

    /**
     * 获取发现群聊分类列表
     */
    suspend fun getGroupCategories(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val token = tokenRepository.getTokenSync()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "Token为空")
                return@withContext Result.failure(Exception("未登录"))
            }

            val response = apiService.getRecommendCategoryList(
                token,
                RecommendCategoryRequest()
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 1) {
                    val categories = body.data?.categories ?: emptyList()
                    Log.d(TAG, "获取群聊分类成功: ${categories.size}个分类")
                    Result.success(categories)
                } else {
                    Log.e(TAG, "获取群聊分类失败: ${body?.msg}")
                    Result.failure(Exception(body?.msg ?: "获取失败"))
                }
            } else {
                Log.e(TAG, "获取群聊分类请求失败: ${response.code()}")
                Result.failure(Exception("网络请求失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取群聊分类异常", e)
            Result.failure(e)
        }
    }

    /**
     * 获取推荐群聊列表
     */
    suspend fun getRecommendGroups(
        category: String = "",
        keyword: String = "",
        size: Int = 30,
        page: Int = 1
    ): Result<List<RecommendGroup>> = withContext(Dispatchers.IO) {
        try {
            val token = tokenRepository.getTokenSync()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "Token为空")
                return@withContext Result.failure(Exception("未登录"))
            }

            val response = apiService.getRecommendGroupList(
                token,
                RecommendGroupListRequest(category, keyword, size, page)
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 1) {
                    val groups = body.data?.groupList ?: emptyList()
                    Log.d(TAG, "获取推荐群聊成功: ${groups.size}个群聊")
                    Result.success(groups)
                } else {
                    Log.e(TAG, "获取推荐群聊失败: ${body?.msg}")
                    Result.failure(Exception(body?.msg ?: "获取失败"))
                }
            } else {
                Log.e(TAG, "获取推荐群聊请求失败: ${response.code()}")
                Result.failure(Exception("网络请求失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取推荐群聊异常", e)
            Result.failure(e)
        }
    }

    /**
     * 获取推荐机器人列表
     */
    suspend fun getRecommendBots(): Result<List<RecommendBot>> = withContext(Dispatchers.IO) {
        try {
            val token = tokenRepository.getTokenSync()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "Token为空")
                return@withContext Result.failure(Exception("未登录"))
            }

            val response = apiService.getRecommendBotList(token)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 1) {
                    val bots = body.data?.botList ?: emptyList()
                    Log.d(TAG, "获取推荐机器人成功: ${bots.size}个机器人")
                    Result.success(bots)
                } else {
                    Log.e(TAG, "获取推荐机器人失败: ${body?.msg}")
                    Result.failure(Exception(body?.msg ?: "获取失败"))
                }
            } else {
                Log.e(TAG, "获取推荐机器人请求失败: ${response.code()}")
                Result.failure(Exception("网络请求失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取推荐机器人异常", e)
            Result.failure(e)
        }
    }
}

