package com.yhchat.canary.data.api

import com.yhchat.canary.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Web API 服务接口 - 用于 chat-web-go.jwzhd.com 域名的API
 */
interface WebApiService {
    
    /**
     * 获取用户主页信息
     */
    @GET("v1/user/homepage")
    suspend fun getUserHomepage(
        @Query("userId") userId: String
    ): Response<UserHomepageResponse>
    
    /**
     * 获取群聊信息
     */
    @POST("v1/group/group-info")
    suspend fun getGroupInfo(
        @Body request: Map<String, String>
    ): Response<GroupInfoResponse>
    
    /**
     * 获取机器人信息
     */
    @POST("v1/bot/bot-info")
    suspend fun getBotInfo(
        @Body request: Map<String, String>
    ): Response<BotInfoResponse>
}