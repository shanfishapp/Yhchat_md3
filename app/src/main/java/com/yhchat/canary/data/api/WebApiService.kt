package com.yhchat.canary.data.api

import com.yhchat.canary.data.model.UserHomepageResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Web API服务接口
 * 用于 chat-web-go.jwzhd.com 域名的API调用
 */
interface WebApiService {
    
    /**
     * 获取用户主页信息
     * GET /v1/user/homepage?userId={userId}
     */
    @GET("v1/user/homepage")
    suspend fun getUserHomepage(
        @Query("userId") userId: String
    ): Response<UserHomepageResponse>
}
