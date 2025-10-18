package com.yhchat.canary.data.repository

import android.util.Log
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.api.WebApiService
import com.yhchat.canary.data.model.BaseResponse
import com.yhchat.canary.data.model.BotInfo
import yh_bot.Bot
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 机器人数据仓库
 * 支持两种 API：
 * 1. JSON API - 用于简单的机器人信息查询（BotInfoActivity）
 * 2. Protobuf API - 用于详细的机器人信息查询（BotDetailActivity）
 */
@Singleton
class BotRepository @Inject constructor(
    private val apiService: ApiService,
    private val webApiService: WebApiService,
    private val tokenRepository: TokenRepository
) {
    
    companion object {
        private const val TAG = "BotRepository"
    }
    
    /**
     * 获取机器人简单信息（使用 JSON）
     * 用于 BotInfoActivity
     */
    suspend fun getBotInfoSimple(botId: String): Result<BotInfo> {
        return try {
            Log.d(TAG, "开始获取机器人简单信息(JSON): $botId")
            
            val token = tokenRepository.getTokenSync()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "Token 为空")
                return Result.failure(Exception("未登录"))
            }
            
            // 这里假设有一个 JSON API 来获取机器人信息
            // 如果没有，可以使用 protobuf API 并转换为 BotInfo
            // 暂时使用 protobuf API 并转换
            val protobufResult = getBotInfo(botId)
            
            protobufResult.fold(
                onSuccess = { botInfoProto ->
                    val data = botInfoProto.data
                    val botInfo = BotInfo(
                        id = 0, // protobuf中没有id字段，使用默认值
                        botId = data.botId,
                        nickname = data.name,
                        nicknameId = data.nameId.toInt(), // long转int
                        avatarId = data.avatarId.toInt(), // long转int
                        avatarUrl = data.avatarUrl,
                        introduction = data.introduction,
                        createBy = data.createBy,
                        createTime = data.createTime,
                        headcount = data.headcount.toInt(), // long转int
                        isPrivate = data.private
                    )
                    Result.success(botInfo)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取机器人简单信息异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取机器人详细信息（使用 Protobuf）
     */
    suspend fun getBotInfo(botId: String): Result<Bot.bot_info> {
        return try {
            Log.d(TAG, "开始获取机器人信息: $botId")
            
            val token = tokenRepository.getTokenSync()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "Token 为空")
                return Result.failure(Exception("未登录"))
            }
            
            // 构建 protobuf 请求
            val request = Bot.bot_info_send.newBuilder()
                .setId(botId)
                .build()
            
            val requestBody = request.toByteArray()
                .toRequestBody("application/x-protobuf".toMediaType())
            
            Log.d(TAG, "发送请求: botId=$botId")
            
            // 发送请求
            val response = apiService.getBotInfo(
                token = token,
                body = requestBody
            )
            
            if (!response.isSuccessful) {
                val errorMsg = "请求失败: ${response.code()} ${response.message()}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }
            
            val responseBody = response.body()
            if (responseBody == null) {
                Log.e(TAG, "响应体为空")
                return Result.failure(Exception("响应数据为空"))
            }
            
            // 解析 protobuf 响应
            val botInfo = Bot.bot_info.parseFrom(responseBody.bytes())
            
            Log.d(TAG, "解析成功: status=${botInfo.status.code}, msg=${botInfo.status.msg}")
            
            if (botInfo.status.code != 1) {
                val errorMsg = "获取机器人信息失败: ${botInfo.status.msg}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }
            
            Log.d(TAG, "✅ 机器人信息获取成功")
            Log.d(TAG, "  机器人ID: ${botInfo.data.botId}")
            Log.d(TAG, "  机器人名称: ${botInfo.data.name}")
            Log.d(TAG, "  使用人数: ${botInfo.data.headcount}")
            Log.d(TAG, "  是否私有: ${botInfo.data.private}")
            Log.d(TAG, "  是否停用: ${botInfo.data.isStop}")
            Log.d(TAG, "  自动进群: ${botInfo.data.alwaysAgree}")
            Log.d(TAG, "  免打扰: ${botInfo.data.doNotDisturb}")
            Log.d(TAG, "  置顶: ${botInfo.data.top}")
            Log.d(TAG, "  限制进群: ${botInfo.data.groupLimit}")
            
            Result.success(botInfo)
            
        } catch (e: Exception) {
            Log.e(TAG, "获取机器人信息异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取看板信息（使用 Protobuf）
     */
    suspend fun getBotBoard(chatId: String, chatType: Int): Result<Bot.board> {
        return try {
            Log.d(TAG, "开始获取看板信息: chatId=$chatId, chatType=$chatType")
            
            val token = tokenRepository.getTokenSync()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "Token 为空")
                return Result.failure(Exception("未登录"))
            }
            
            // 构建 protobuf 请求
            val request = Bot.board_send.newBuilder()
                .setChatId(chatId)
                .setChatType(chatType.toLong())
                .build()
            
            val requestBody = request.toByteArray()
                .toRequestBody("application/x-protobuf".toMediaType())
            
            Log.d(TAG, "发送请求: chatId=$chatId, chatType=$chatType")
            
            // 发送请求
            val response = apiService.getBotBoard(
                token = token,
                body = requestBody
            )
            
            if (!response.isSuccessful) {
                val errorMsg = "请求失败: ${response.code()} ${response.message()}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }
            
            val responseBody = response.body()
            if (responseBody == null) {
                Log.e(TAG, "响应体为空")
                return Result.failure(Exception("响应数据为空"))
            }
            
            // 解析 protobuf 响应
            val board = Bot.board.parseFrom(responseBody.bytes())
            
            Log.d(TAG, "解析成功: status=${board.status.code}, msg=${board.status.msg}")
            
            if (board.status.code != 1) {
                val errorMsg = "获取看板信息失败: ${board.status.msg}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }
            
            Log.d(TAG, "✅ 看板信息获取成功")
            Log.d(TAG, "  机器人ID: ${board.board.botId}")
            Log.d(TAG, "  对象ID: ${board.board.chatId}")
            Log.d(TAG, "  内容类型: ${board.board.contentType}")
            
            Result.success(board)
            
        } catch (e: Exception) {
            Log.e(TAG, "获取看板信息异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取我创建的机器人列表
     */
    suspend fun getMyBotList(): Result<List<com.yhchat.canary.data.model.CreatedBot>> {
        return try {
            Log.d(TAG, "开始获取我创建的机器人列表")
            
            val token = tokenRepository.getTokenSync()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "Token 为空")
                return Result.failure(Exception("未登录"))
            }
            
            val response = webApiService.getMyBotList(token)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.code == 1) {
                    val botList = responseBody.data.list.bots
                    Log.d(TAG, "✅ 我创建的机器人列表获取成功！共 ${botList.size} 个")
                    Result.success(botList)
                } else {
                    Log.e(TAG, "获取机器人列表失败: ${responseBody?.message}")
                    Result.failure(Exception(responseBody?.message ?: "获取失败"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "获取机器人列表失败: ${response.code()} - $errorBody")
                Result.failure(Exception("获取机器人列表失败: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取机器人列表异常", e)
            Result.failure(e)
        }
    }

    /**
     * 编辑机器人信息
     */
    suspend fun editBot(
        botId: String,
        nickname: String,
        introduction: String,
        avatarUrl: String,
        isPrivate: Boolean
    ): Result<Unit> {
        return try {
            Log.d(TAG, "开始编辑机器人信息: botId=$botId")

            val token = tokenRepository.getTokenSync()
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "Token 为空")
                return Result.failure(Exception("未登录"))
            }

            val request = com.yhchat.canary.data.api.EditBotRequest(
                botId = botId,
                nickname = nickname,
                introduction = introduction,
                avatarUrl = avatarUrl,
                private = if (isPrivate) 1 else 0
            )
            
            val response = apiService.editBot(
                token = token,
                request = request
            )

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.code == 1) {
                    Log.d(TAG, "✅ 机器人信息编辑成功")
                    Result.success(Unit)
                } else {
                    Log.e(TAG, "编辑机器人信息失败: ${responseBody?.message}")
                    Result.failure(Exception(responseBody?.message ?: "编辑失败"))
                }
        } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "编辑机器人信息失败: ${response.code()} - $errorBody")
                Result.failure(Exception("编辑机器人信息失败: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "编辑机器人信息异常", e)
            Result.failure(e)
        }
    }
}
