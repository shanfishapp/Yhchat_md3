package com.yhchat.canary.data.repository

import android.util.Log
import com.google.protobuf.InvalidProtocolBufferException
import com.yhchat.canary.data.model.GroupDetail
import com.yhchat.canary.data.model.GroupMemberInfo
import com.yhchat.canary.proto.group.info
import com.yhchat.canary.proto.group.info_send
import com.yhchat.canary.proto.group.list_member
import com.yhchat.canary.proto.group.list_member_send
import com.yhchat.canary.proto.group.edit_group
import com.yhchat.canary.proto.group.edit_group_send
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
class GroupRepository @Inject constructor(
    private val apiService: com.yhchat.canary.data.api.ApiService
) {

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

            Log.d(
                tag,
                "Protobuf parsed. Status code: ${infoResponse.status.code}, msg: ${infoResponse.status.msg}"
            )

            if (infoResponse.status.code != 1) {
                Log.e(tag, "❌ Server returned error: ${infoResponse.status.msg}")
                return@withContext Result.failure(Exception(infoResponse.status.msg))
            }

            val data = infoResponse.data
            Log.d(
                tag,
                "Group data: groupId=${data.groupId}, name=${data.name}, members=${data.member}"
            )

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

            Log.d(
                tag,
                "✅ Group info successfully created: ${groupInfo.name}, members: ${groupInfo.memberCount}"
            )
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
    suspend fun getGroupMembers(
        groupId: String,
        size: Int = 50,
        page: Int = 1,
        keywords: String = ""
    ): Result<List<GroupMemberInfo>> = withContext(Dispatchers.IO) {
        val token = tokenRepository?.getTokenSync()
        if (token.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("未登录"))
        }

        return@withContext try {
            // 构建protobuf请求
            val requestBuilder = list_member_send.newBuilder()
                .setGroupId(groupId)
                .setData(
                    list_member_send.Data.newBuilder()
                        .setSize(size)
                        .setPage(page)
                        .build()
                )
            
            // 如果有搜索关键词，添加到请求中
            if (keywords.isNotEmpty()) {
                requestBuilder.setKeywords(keywords)
            }
            
            val request = requestBuilder.build()

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
     * 搜索群成员
     */
    suspend fun searchGroupMembers(
        groupId: String,
        keywords: String,
        size: Int = 50,
        page: Int = 1
    ): Result<List<GroupMemberInfo>> {
        // 直接调用 getGroupMembers 并传入 keywords
        return getGroupMembers(groupId, size, page, keywords)
    }
    
    /**
     * 编辑群聊信息
     */
    suspend fun editGroupInfo(
        groupId: String,
        name: String,
        introduction: String,
        avatarUrl: String,
        directJoin: Boolean,
        historyMsg: Boolean,
        categoryName: String,
        categoryId: Long,
        isPrivate: Boolean
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(tag, "✏️ Editing group info for: $groupId")
        val token = tokenRepository?.getTokenSync()
        if (token.isNullOrEmpty()) {
            Log.e(tag, "❌ No token available")
            return@withContext Result.failure(Exception("未登录"))
        }

        return@withContext try {
            // 构建请求
            val requestBuilder = edit_group_send.newBuilder()
                .setGroupId(groupId)
                .setName(name)
                .setIntroduction(introduction)
                .setAvatarUrl(avatarUrl)
                .setDirectJoin(if (directJoin) 1 else 0)
                .setHistoryMsg(if (historyMsg) 1 else 0)
                .setCategoryName(categoryName)
                .setCategoryId(categoryId)
                .setPrivate(if (isPrivate) 1 else 0)

            val requestData = requestBuilder.build()
            val requestBytes = requestData.toByteArray()
            val requestBody = requestBytes.toRequestBody("application/x-protobuf".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("$baseUrl/v1/group/edit-group")
                .addHeader("token", token)
                .post(requestBody)
                .build()

            Log.d(tag, "📤 Sending edit group request...")
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseData = response.body?.bytes()
                if (responseData != null) {
                    val editResponse = edit_group.parseFrom(responseData)
                    if (editResponse.status.code == 1) {
                        Log.d(tag, "✅ Group info edited successfully")
                        Result.success(true)
                    } else {
                        Log.e(tag, "❌ Edit failed: ${editResponse.status.msg}")
                        Result.failure(Exception(editResponse.status.msg))
                    }
                } else {
                    Log.e(tag, "❌ Empty response body")
                    Result.failure(Exception("响应为空"))
                }
            } else {
                Log.e(tag, "❌ HTTP error: ${response.code}")
                Result.failure(Exception("网络请求失败: ${response.code}"))
            }
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
     * 设置消息类型限制
     * POST /v1/group/msg-type-limit
     */
    suspend fun setMessageTypeLimit(
        groupId: String,
        messageTypes: String  // 例如: "1,2,3,4"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(tag, "🚫 Setting message type limit for group: $groupId, types: $messageTypes")
        val token = tokenRepository?.getTokenSync()
        if (token.isNullOrEmpty()) {
            Log.e(tag, "❌ No token available")
            return@withContext Result.failure(Exception("未登录"))
        }

        return@withContext try {
            val requestBody = """{"groupId":"$groupId","type":"$messageTypes"}"""
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("$baseUrl/v1/group/msg-type-limit")
                .addHeader("token", token)
                .post(requestBody)
                .build()

            Log.d(tag, "📤 Sending message type limit request...")
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseText = response.body?.string()
                Log.d(tag, "✅ Message type limit set successfully: $responseText")
                Result.success(true)
            } else {
                Log.e(tag, "❌ HTTP error: ${response.code}")
                Result.failure(Exception("设置消息类型限制失败: ${response.code}"))
            }
        } catch (e: IOException) {
            Log.e(tag, "Network error", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "Unknown error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 踢出群成员
     * POST /v1/group/remove-member
     */
    suspend fun removeMember(
        groupId: String,
        userId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(tag, "👢 Removing member from group: $groupId, userId: $userId")
        val token = tokenRepository?.getTokenSync()
        if (token.isNullOrEmpty()) {
            Log.e(tag, "❌ No token available")
            return@withContext Result.failure(Exception("未登录"))
        }

        return@withContext try {
            val requestBody = """{"groupId":"$groupId","userId":"$userId"}"""
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("$baseUrl/v1/group/remove-member")
                .addHeader("token", token)
                .post(requestBody)
                .build()

            Log.d(tag, "📤 Sending remove member request...")
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseText = response.body?.string()
                Log.d(tag, "✅ Member removed successfully: $responseText")
                Result.success(true)
            } else {
                Log.e(tag, "❌ HTTP error: ${response.code}")
                Result.failure(Exception("踢出用户失败: ${response.code}"))
            }
        } catch (e: IOException) {
            Log.e(tag, "Network error", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "Unknown error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 禁言群成员
     * POST /v1/group/gag-member
     * @param gagTime 禁言时间: 0-取消禁言, 600-10分钟, 3600-1小时, 21600-6小时, 43200-12小时, 1-永久禁言
     */
    suspend fun gagMember(
        groupId: String,
        userId: String,
        gagTime: Int
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(tag, "🔇 Gagging member in group: $groupId, userId: $userId, gagTime: $gagTime")
        val token = tokenRepository?.getTokenSync()
        if (token.isNullOrEmpty()) {
            Log.e(tag, "❌ No token available")
            return@withContext Result.failure(Exception("未登录"))
        }

        return@withContext try {
            val requestBody = """{"groupId":"$groupId","userId":"$userId","gag":$gagTime}"""
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("$baseUrl/v1/group/gag-member")
                .addHeader("token", token)
                .post(requestBody)
                .build()

            Log.d(tag, "📤 Sending gag member request...")
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseText = response.body?.string()
                Log.d(tag, "✅ Member gagged successfully: $responseText")
                Result.success(true)
            } else {
                Log.e(tag, "❌ HTTP error: ${response.code}")
                Result.failure(Exception("禁言用户失败: ${response.code}"))
            }
        } catch (e: IOException) {
            Log.e(tag, "Network error", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "Unknown error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 设置管理员（上任/卸任）
     * POST /v1/group/manage-setting
     * @param userLevel 用户等级: 0-普通成员, 2-管理员
     */
    suspend fun setMemberRole(
        groupId: String,
        userId: String,
        userLevel: Int
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(tag, "⚙️ Setting member role in group: $groupId, userId: $userId, userLevel: $userLevel")
        val token = tokenRepository?.getTokenSync()
        if (token.isNullOrEmpty()) {
            Log.e(tag, "❌ No token available")
            return@withContext Result.failure(Exception("未登录"))
        }

        return@withContext try {
            val requestBody = """{"groupId":"$groupId","userId":"$userId","userLevel":$userLevel}"""
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("$baseUrl/v1/group/manage-setting")
                .addHeader("token", token)
                .post(requestBody)
                .build()

            Log.d(tag, "📤 Sending set member role request...")
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseText = response.body?.string()
                Log.d(tag, "✅ Member role set successfully: $responseText")
                Result.success(true)
            } else {
                Log.e(tag, "❌ HTTP error: ${response.code}")
                Result.failure(Exception("设置成员角色失败: ${response.code}"))
            }
        } catch (e: IOException) {
            Log.e(tag, "Network error", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "Unknown error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取群指令列表
     */
    suspend fun getInstructionList(groupId: String): Result<List<com.yhchat.canary.data.model.Instruction>> {
        return try {
            val token = tokenRepository?.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("未登录"))
            }
            
            val response = apiService.getInstructionList(
                token = token,
                request = com.yhchat.canary.data.model.GroupIdRequest(groupId = groupId)
            )
            
            if (response.isSuccessful && response.body()?.code == 1) {
                val instructions: List<com.yhchat.canary.data.model.Instruction> = response.body()?.data?.instructions ?: emptyList()
                Result.success(instructions)
            } else {
                Result.failure(Exception(response.body()?.msg ?: "获取指令列表失败"))
            }
        } catch (e: Exception) {
            Log.e(tag, "获取指令列表失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 邀请加入群聊
     */
    suspend fun inviteToGroup(chatId: String, chatType: Int, groupId: String): Result<Boolean> {
        return try {
            val token = tokenRepository?.getTokenSync()
            if (token == null) {
                return Result.failure(Exception("未登录"))
            }
            
            val response = apiService.inviteToGroup(
                token = token,
                request = com.yhchat.canary.data.model.InviteGroupRequest(
                    chatId = chatId,
                    chatType = chatType,
                    groupId = groupId
                )
            )
            
            if (response.isSuccessful && response.body()?.code == 1) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "邀请失败"))
            }
        } catch (e: Exception) {
            Log.e(tag, "邀请失败", e)
            Result.failure(e)
        }
    }
}




