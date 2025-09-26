package com.yhchat.canary.data.repository

import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.api.WebApiService
import com.yhchat.canary.data.model.BotInfo
import com.yhchat.canary.data.model.WebUserDetailInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BotRepository(
    private val apiService: ApiService,
    private val webApiService: WebApiService
) {

    suspend fun getBotInfo(token: String, botId: String): BotInfo = withContext(Dispatchers.IO) {
        try {
            val response = webApiService.getBotInfo(mapOf("botId" to botId))
            if (response.code == 1) {
                val botData = response.data.bot
                BotInfo(
                    id = botData.id,
                    botId = botData.botId,
                    nickname = botData.nickname,
                    nicknameId = botData.nicknameId,
                    avatarId = botData.avatarId,
                    avatarUrl = botData.avatarUrl,
                    token = botData.token,
                    link = botData.link,
                    introduction = botData.introduction,
                    createBy = botData.createBy,
                    createTime = botData.createTime,
                    headcount = botData.headcount,
                    private = botData.private,
                    isStop = null, // Web API没有这个字段
                    alwaysAgree = null, // Web API没有这个字段
                    doNotDisturb = null, // Web API没有这个字段
                    top = null, // Web API没有这个字段
                    groupLimit = null // Web API没有这个字段
                )
            } else {
                throw Exception("获取机器人信息失败: ${response.msg}")
            }
        } catch (e: Exception) {
            throw Exception("获取机器人信息失败: ${e.message}")
        }
    }

    suspend fun getUserInfo(userId: String): WebUserDetailInfo = withContext(Dispatchers.IO) {
        val response = webApiService.getUserInfo(mapOf("userId" to userId))
        val body = response.body()

        if (response.isSuccessful && body?.code == 1) {
            body.data.user
        } else {
            throw Exception("获取用户信息失败: ${body?.msg ?: "网络错误"}")
        }
    }
}
