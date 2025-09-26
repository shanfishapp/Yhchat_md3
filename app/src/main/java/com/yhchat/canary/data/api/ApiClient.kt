package com.yhchat.canary.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
// import com.yhchat.canary.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent
    .TimeUnit

/**
 * API客户端配置
 */
object ApiClient {
    
    // 主要API域名 - chat-go.jwzhd.com
    private const val BASE_URL = "https://chat-go.jwzhd.com/"
    // 用户资料API域名 - chat-web-go.jwzhd.com
    private const val WEB_BASE_URL = "https://chat-web-go.jwzhd.com/"
    
    private val gson: Gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }
    
    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        
        // 添加日志拦截器（开发时使用）
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        builder.addInterceptor(loggingInterceptor)
        
        builder.build()
    }
    
    // 主要API服务 (chat-go.jwzhd.com)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    // Web API服务 (chat-web-go.jwzhd.com) - JSON格式
    private val webRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(WEB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val webApiService: WebApiService by lazy {
        webRetrofit.create(WebApiService::class.java)
    }
}
