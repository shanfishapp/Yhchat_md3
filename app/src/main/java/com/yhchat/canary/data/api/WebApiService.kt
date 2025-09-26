package com.yhchat.canary.data.api

import com.yhchat.canary.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Headers
import okhttp3.RequestBody

/**
 * Web API服务接口 (chat-web-go.jwzhd.com) - JSON格式
 */
interface WebApiService {

    /**
     * 获取用户信息
     */
    @POST("v1/user/user-info")
    suspend fun getUserInfo(
        @Body request: Map<String, String>
    ): Response<WebUserInfoResponse>

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
    @Headers("Content-Type: application/json")
    @POST("v1/group/group-info")
    suspend fun getGroupInfo(
        @Body request: Map<String, String>
    ): GroupInfoResponse

    /**
     * 获取机器人信息
     */
    @Headers("Content-Type: application/json")
    @POST("v1/bot/bot-info")
    suspend fun getBotInfo(
        @Body request: Map<String, String>
    ): BotInfoResponse
}