package com.yhchat.canary.data.api

import com.yhchat.canary.data.model.GitHubRelease
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * 更新检查 API 服务
 */
interface UpdateService {
    
    @GET("repos/Kauid323/Yhchat_MD3/releases")
    suspend fun getReleases(
        @Header("Host") host: String = "api.github.com",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Mobile Safari/537.36",
        @Header("Content-Type") contentType: String = "application/json; charset=utf-8"
    ): Response<List<GitHubRelease>>
}