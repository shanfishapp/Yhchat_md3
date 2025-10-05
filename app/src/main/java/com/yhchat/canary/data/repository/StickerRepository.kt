package com.yhchat.canary.data.repository

import android.util.Log
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.AddStickerPackRequest
import com.yhchat.canary.data.model.StickerItem
import com.yhchat.canary.data.model.StickerPackCreator
import com.yhchat.canary.data.model.StickerPackDetail
import com.yhchat.canary.data.model.StickerPackDetailRequest
import com.yhchat.canary.data.repository.TokenRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StickerRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenRepository: TokenRepository
) {
    private val tag = "StickerRepository"
    
    /**
     * 获取表情包详情
     */
    suspend fun getStickerPackDetail(stickerPackId: String): Result<StickerPackDetail> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "Token is null or empty")
                return Result.failure(Exception("用户未登录"))
            }
            
            // 将String转换为Long
            val stickerPackIdLong = stickerPackId.toLongOrNull() ?: return Result.failure(Exception("无效的表情包ID"))
            val request = StickerPackDetailRequest(id = stickerPackIdLong)
            
            Log.d(tag, "Getting sticker pack detail: $stickerPackId")
            
            val response = apiService.getStickerPackDetail(token, request)
            
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val code = body["code"] as? Double
                    val message = body["msg"] as? String ?: "获取表情包详情失败"
                    if (code == 1.0) {
                        val data = body["data"] as? Map<*, *>
                        
                        // 解析表情包数据
                        val stickerPackData = data?.get("stickerPack") as? Map<*, *>
                        val userData = data?.get("user") as? Map<*, *>
                        
                        if (stickerPackData != null && userData != null) {
                            val stickerPack = parseStickerPackDetail(stickerPackData, userData)
                            Log.d(tag, "Successfully got sticker pack: ${stickerPack.name}")
                            Result.success(stickerPack)
                        } else {
                            Log.e(tag, "Invalid response data structure")
                            Result.failure(Exception("数据格式错误"))
                        }
                    } else {
                        Log.e(tag, "API error: $message")
                        Result.failure(Exception(message))
                    }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Log.e(tag, "Failed to get sticker pack detail: ${response.message()}")
                Result.failure(Exception("获取表情包详情失败: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting sticker pack detail", e)
            Result.failure(e)
        }
    }
    
    /**
     * 添加表情包到收藏
     */
    suspend fun addStickerPackToFavorites(stickerPackId: String): Result<Unit> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "Token is null or empty")
                return Result.failure(Exception("用户未登录"))
            }
            
            // 将String转换为Long
            val stickerPackIdLong = stickerPackId.toLongOrNull() ?: return Result.failure(Exception("无效的表情包ID"))
            val request = AddStickerPackRequest(id = stickerPackIdLong)
            
            Log.d(tag, "Adding sticker pack to favorites: $stickerPackId")
            
            val response = apiService.addStickerPack(token, request)
            
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val message = body.message
                    if (body.code == 1) {
                        Log.d(tag, "Successfully added sticker pack: $stickerPackId")
                        Result.success(Unit)
                    } else {
                        Log.e(tag, "Failed to add sticker pack: $message")
                        Result.failure(Exception(message))
                    }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Log.e(tag, "Failed to add sticker pack: ${response.message()}")
                Result.failure(Exception("添加表情包失败: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error adding sticker pack", e)
            Result.failure(e)
        }
    }
    
    /**
     * 解析表情包详情
     */
    private fun parseStickerPackDetail(
        stickerPackData: Map<*, *>,
        userData: Map<*, *>
    ): StickerPackDetail {
        val stickerItemsList = stickerPackData["stickerItems"] as? List<Map<String, Any>> ?: emptyList()
        
        val stickerItems = stickerItemsList.mapNotNull { item ->
            val itemMap = item as? Map<*, *> ?: return@mapNotNull null
            StickerItem(
                id = (itemMap["id"] as? Double)?.toInt() ?: 0,
                name = itemMap["name"] as? String ?: "",
                url = itemMap["url"] as? String ?: "",
                stickerPackId = (itemMap["stickerPackId"] as? Double)?.toInt() ?: 0,
                createBy = itemMap["createBy"] as? String ?: "",
                createTime = (itemMap["createTime"] as? Double)?.toLong() ?: 0L
            )
        }

        val creator = StickerPackCreator(
            userId = userData["user_id"] as? String ?: "",
            nickname = userData["nickname"] as? String ?: "",
            avatarUrl = userData["avatar_url"] as? String ?: ""
        )
        
        return StickerPackDetail(
            id = (stickerPackData["id"] as? Double)?.toInt() ?: 0,
            name = stickerPackData["name"] as? String ?: "",
            createBy = stickerPackData["createBy"] as? String ?: "",
            createTime = (stickerPackData["createTime"] as? Double)?.toLong() ?: 0L,
            userCount = (stickerPackData["userCount"] as? Double)?.toInt() ?: 0,
            uuid = stickerPackData["uuid"] as? String ?: "",
            updateTime = (stickerPackData["updateTime"] as? Double)?.toLong() ?: 0L,
            stickerItems = stickerItems,
            creator = creator
        )
    }
}

