package com.yhchat.canary.data.repository

import android.util.Log
import com.google.protobuf.InvalidProtocolBufferException
import com.yhchat.canary.data.model.GroupDetail
import com.yhchat.canary.data.model.GroupMemberInfo
import com.yhchat.canary.proto.group.info
import com.yhchat.canary.proto.group.info_send
import com.yhchat.canary.proto.group.list_member
import com.yhchat.canary.proto.group.list_member_send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor() {
    
    private val tag = "GroupRepository"
    private val baseUrl = "https://chat-go.jwzhd.com"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private var tokenRepository: TokenRepository? = null
    
    fun setTokenRepository(tokenRepository: TokenRepository) {
        this.tokenRepository = tokenRepository
    }
    
    /**
     * 获取群聊信息
     */
    suspend fun getGroupInfo(groupId: String): Result<GroupDetail> = withContext(Dispatchers.IO) {
        Log.d(tag, "🔍 Getting group info for: $groupId")
        val token = tokenRepository?.getTokenSync()
        if (token.isNullOrEmpty()) {
            Log.e(tag, "❌ No token available")
            return@withContext Result.failure(Exception("未登录"))
        }
        
        Log.d(tag, "Token available, length: ${token.length}")
        
        return@withContext try {
            // 构建protobuf请求
            val request = info_send.newBuilder()
                .setGroupId(groupId)
                .build()
            
            Log.d(tag, "Request protobuf built, groupId: $groupId")
            
            val requestBody = request.toByteArray()
                .toRequestBody("application/x-protobuf".toMediaTypeOrNull())
            
            val httpRequest = Request.Builder()
                .url("$baseUrl/v1/group/info")
                .addHeader("token", token)
                .post(requestBody)
                .build()
            
            Log.d(tag, "Sending request to: $baseUrl/v1/group/info")
            
            val response = client.newCall(httpRequest).execute()
            
            Log.d(tag, "✅ Response code: ${response.code}")
            Log.d(tag, "Response message: ${response.message}")
            Log.d(tag, "Response headers: ${response.headers}")
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e(tag, "❌ Request failed with code ${response.code}: $errorBody")
                return@withContext Result.failure(IOException("请求失败: ${response.code} - ${response.message}"))
            }
            
            val responseBody = response.body?.bytes()
            if (responseBody == null) {
                Log.e(tag, "❌ Response body is null")
                return@withContext Result.failure(IOException("响应为空"))
            }
            
            Log.d(tag, "✅ Response body size: ${responseBody.size} bytes")
            
            // 解析protobuf响应
            Log.d(tag, "Parsing protobuf response, size: ${responseBody.size} bytes")
            val infoResponse = info.parseFrom(responseBody)
            
            Log.d(tag, "Protobuf parsed. Status code: ${infoResponse.status.code}, msg: ${infoResponse.status.msg}")
            
            if (infoResponse.status.code != 1) {
                Log.e(tag, "❌ Server returned error: ${infoResponse.status.msg}")
                return@withContext Result.failure(Exception(infoResponse.status.msg))
            }
            
            val data = infoResponse.data
            Log.d(tag, "Group data: groupId=${data.groupId}, name=${data.name}, members=${data.member}")
            
            val groupInfo = GroupDetail(
                groupId = data.groupId,
                name = data.name,
                avatarUrl = data.avatarUrl,
                introduction = data.introduction,
                memberCount = data.member.toInt(),
                createBy = data.createBy,
                directJoin = data.directJoin == 1,
                permissionLevel = data.permissonLevel,
                historyMsgEnabled = data.historyMsg == 1,
                categoryName = data.categoryName,
                categoryId = data.categoryId,
                isPrivate = data.private == 1,
                doNotDisturb = data.doNotDisturb == 1,
                communityId = data.communityId,
                communityName = data.communityName,
                isTop = data.top == 1,
                adminIds = data.adminList,
                ownerId = data.owner,
                limitedMsgType = data.limitedMsgType,
                avatarId = data.avatarId,
                recommendation = data.recommandation
            )
            
            Log.d(tag, "✅ Group info successfully created: ${groupInfo.name}, members: ${groupInfo.memberCount}")
            Result.success(groupInfo)
            
        } catch (e: InvalidProtocolBufferException) {
            Log.e(tag, "❌ Protobuf parse error: ${e.message}", e)
            Log.e(tag, "Error details: ${e.stackTraceToString()}")
            Result.failure(e)
        } catch (e: IOException) {
            Log.e(tag, "❌ Network/IO error: ${e.message}", e)
            Log.e(tag, "Error details: ${e.stackTraceToString()}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "❌ Unknown error: ${e.message}", e)
            Log.e(tag, "Error type: ${e::class.java.simpleName}")
            Log.e(tag, "Error details: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }
    
    /**
     * 获取群成员列表
     */
    suspend fun getGroupMembers(groupId: String, size: Int = 50, page: Int = 1): Result<List<GroupMemberInfo>> = withContext(Dispatchers.IO) {
        val token = tokenRepository?.getTokenSync()
        if (token.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("未登录"))
        }
        
        return@withContext try {
            // 构建protobuf请求
            val request = list_member_send.newBuilder()
                .setGroupId(groupId)
                .setData(
                    list_member_send.Data.newBuilder()
                        .setSize(size)
                        .setPage(page)
                        .build()
                )
                .build()
            
            val requestBody = request.toByteArray()
                .toRequestBody("application/x-protobuf".toMediaTypeOrNull())
            
            val httpRequest = Request.Builder()
                .url("$baseUrl/v1/group/list-member")
                .addHeader("token", token)
                .post(requestBody)
                .build()
            
            val response = client.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("请求失败: ${response.code}"))
            }
            
            val responseBody = response.body?.bytes()
                ?: return@withContext Result.failure(IOException("响应为空"))
            
            // 解析protobuf响应
            val listResponse = list_member.parseFrom(responseBody)
            
            if (listResponse.status.code != 1) {
                return@withContext Result.failure(Exception(listResponse.status.msg))
            }
            
            val members = listResponse.userList.map { user ->
                GroupMemberInfo(
                    userId = user.userInfo.userId,
                    name = user.userInfo.name,
                    avatarUrl = user.userInfo.avatarUrl,
                    isVip = user.userInfo.isVip == 1,
                    permissionLevel = user.permissionLevel,
                    gagTime = user.gagTime,
                    isGag = user.isGag == 1
                )
            }
            
            Log.d(tag, "Group members loaded: ${members.size}")
            Result.success(members)
            
        } catch (e: InvalidProtocolBufferException) {
            Log.e(tag, "Protobuf parse error", e)
            Result.failure(e)
        } catch (e: IOException) {
            Log.e(tag, "Network error", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "Unknown error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 修改群聊信息
     */
    suspend fun editGroupInfo(editGroupInfoRequest: com.yhchat.canary.data.api.EditGroupInfoRequest): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = tokenRepository?.getTokenSync()
        if (token.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("未登录"))
        }
        
        return@withContext try {
            val apiService = com.yhchat.canary.data.api.ApiClient.create(com.yhchat.canary.data.api.ApiService::class.java)
            val response = apiService.editGroupInfo(token, editGroupInfoRequest)
            
            if (response.isSuccessful && response.body()?.code == 1) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.msg ?: "修改群聊信息失败"))
            }
        } catch (e: Exception) {
            Log.e(tag, "修改群聊信息失败", e)
            Result.failure(e)
        }
    }

