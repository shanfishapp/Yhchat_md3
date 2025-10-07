package com.yhchat.canary.data.di

import android.content.Context
import androidx.room.Room
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.api.WebApiService
import com.yhchat.canary.data.local.AppDatabase
import com.yhchat.canary.data.repository.BotRepository
import com.yhchat.canary.data.repository.CommunityRepository
import com.yhchat.canary.data.repository.FriendRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.ConversationRepository
import com.yhchat.canary.data.repository.CacheRepository
import com.yhchat.canary.data.repository.NavigationRepository
import com.yhchat.canary.data.repository.UserRepository
import com.yhchat.canary.data.repository.DraftRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Repository工厂类
 */
object RepositoryFactory {
    
    private const val BASE_URL = "https://chat-go.jwzhd.com/"
    private const val WEB_BASE_URL = "https://chat-web-go.jwzhd.com/"
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    }
    
    private val webRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(WEB_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val webApiService: WebApiService by lazy {
        webRetrofit.create(WebApiService::class.java)
    }

    val communityRepository: CommunityRepository by lazy {
        CommunityRepository(apiService)
    }

    // FriendRepository需要通过函数创建，因为需要context来获取tokenRepository

    val botRepository: BotRepository by lazy {
        BotRepository(apiService, webApiService)
    }
    
    /**
     * 获取社区仓库实例
     */
    fun getCommunityRepository(context: Context): CommunityRepository {
        return communityRepository
    }
    
    /**
     * 获取好友仓库实例
     */
    fun getFriendRepository(context: Context): FriendRepository {
        return FriendRepository(apiService, getTokenRepository(context))
    }
    
    /**
     * 获取Token仓库实例
     */
    fun getTokenRepository(context: Context): TokenRepository {
        val database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
        return TokenRepository(database.userTokenDao(), context)
    }
    
    /**
     * 获取会话仓库实例
     */
    fun getConversationRepository(context: Context): ConversationRepository {
        val cacheRepository = CacheRepository(context.applicationContext)
        return ConversationRepository(apiService, cacheRepository)
    }
    
    /**
     * 获取导航配置仓库实例
     */
    fun getNavigationRepository(context: Context): NavigationRepository {
        return NavigationRepository(context)
    }
    
    /**
     * 获取用户仓库实例
     */
    fun getUserRepository(context: Context): UserRepository {
        return UserRepository(apiService, null)
    }
    
    /**
     * 获取草稿仓库实例
     */
    fun getDraftRepository(context: Context): DraftRepository {
        return DraftRepository(context)
    }
    
    /**
     * 获取机器人仓库实例
     */
    fun provideBotRepository(): BotRepository {
        return botRepository
    }
}
