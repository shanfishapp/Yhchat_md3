package com.yhchat.canary.data.repository

import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.api.WebApiService
import com.yhchat.canary.data.model.BotInfo
import com.yhchat.canary.data.model.UserHomepageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BotRepository(
    private val apiService: ApiService,
    private val webApiService: WebApiService
) {

    suspend fun getBotInfo(token: String, botId: String): BotInfo = withContext(Dispatchers.IO) {
        try {
            val response = webApiService.getBotInfo(mapOf("botId" to botId))
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.code == 1) {
                    val botData = responseBody.data.bot
                    BotInfo(
                        id = botData.id,
                        botId = botData.botId,
                        nickname = botData.nickname,
                        nicknameId = botData.nicknameId,
                        avatarId = botData.avatarId,
                        avatarUrl = botData.avatarUrl,
                        introduction = botData.introduction,
                        createBy = botData.createBy,
                        createTime = botData.createTime,
                        headcount = botData.headcount,
                        isPrivate = botData.isPrivate ?: 0
                    )
                } else {
                    throw Exception("获取机器人信息失败: ${responseBody?.msg ?: "未知错误"}")
                }
            } else {
                throw Exception("获取机器人信息失败: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            throw Exception("获取机器人信息失败: ${e.message}")
        }
    }

    suspend fun getUserInfo(userId: String): UserHomepageInfo = withContext(Dispatchers.IO) {
        val response = webApiService.getUserHomepage(userId)
        val body = response.body()

        if (response.isSuccessful && body?.code == 1) {
            body.data.user
        } else {
            throw Exception("获取用户信息失败: ${body?.msg ?: "网络错误"}")
        }
    }
}
