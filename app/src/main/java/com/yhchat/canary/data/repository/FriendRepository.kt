package com.yhchat.canary.data.repository

import android.util.Log
import com.yhchat.canary.data.model.AddFriendRequest
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.model.*
import com.yhchat.canary.proto.*
import yh_user.User
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/**
 * 好友数据仓库
 */
class FriendRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenRepository: TokenRepository
) {
    
    private val tag = "FriendRepository"
    
    /**
     * 申请添加好友/群聊/机器人
     */
    suspend fun applyFriend(
        token: String,
        chatId: String,
        chatType: Int, // 1-用户，2-群聊，3-机器人
        remark: String
    ): ApiStatus {
        return try {
            val request = AddFriendRequest(
                chatId = chatId,
                chatType = chatType,
                remark = remark
            )
            val response = apiService.addFriend(token, request)
            if (response.isSuccessful) {
                response.body() ?: ApiStatus(code = 0, message = "响应为空")
            } else {
                ApiStatus(code = 0, message = "网络请求失败: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiStatus(code = 0, message = "请求失败: ${e.message}")
        }
    }
    
    /**
     * 获取所有聊天对象（通讯录）
     */
    suspend fun getAddressBookList(): Result<User.address_book_list> {
        return try {
            val tokenFlow = tokenRepository.getToken()
            val token = tokenFlow.first()?.token
            if (token.isNullOrEmpty()) {
                Log.e(tag, "❌ Token为空")
                return Result.failure(Exception("用户未登录"))
            }
            
            Log.d(tag, "📤 ========== 获取通讯录 ==========")
            
            // 构建protobuf请求
            val request = User.address_book_list_send.newBuilder()
                .setNumber("通讯录请求")
                .build()
            
            val requestBody = request.toByteArray().toRequestBody("application/x-protobuf".toMediaType())
            
            Log.d(tag, "📤 发送请求...")
            
            val response = apiService.getAddressBookList(token, requestBody)
            
            Log.d(tag, "📥 服务器响应码: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val bytes = responseBody.bytes()
                    val addressBook = User.address_book_list.parseFrom(bytes)
                    
                    Log.d(tag, "📥 响应状态码: ${addressBook.status.code}")
                    Log.d(tag, "📥 响应消息: ${addressBook.status.msg}")
                    Log.d(tag, "📥 分组数量: ${addressBook.dataCount}")
                    
                    if (addressBook.status.code == 1) {
                        Log.d(tag, "✅ ========== 通讯录获取成功！ ==========")
                        Result.success(addressBook)
                    } else {
                        Log.e(tag, "❌ 获取失败: ${addressBook.status.msg}")
                        Result.failure(Exception(addressBook.status.msg))
                    }
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(tag, "❌ HTTP错误: ${response.code()}, 错误详情: $errorBody")
                Result.failure(Exception("获取失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ 获取通讯录异常", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
