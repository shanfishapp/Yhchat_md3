package com.yhchat.canary.data.di

import android.content.Context
import androidx.room.Room
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.local.AppDatabase
import com.yhchat.canary.data.repository.CommunityRepository
import com.yhchat.canary.data.repository.FriendRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.ConversationRepository
import com.yhchat.canary.data.repository.NavigationRepository
import com.yhchat.canary.data.repository.UserRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Repository工厂类
 */
object RepositoryFactory {
    
    private const val BASE_URL = "https://chat-go.jwzhd.com/"
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    }
    
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    val communityRepository: CommunityRepository by lazy {
        CommunityRepository(apiService)
    }
    
    val friendRepository: FriendRepository by lazy {
        FriendRepository(apiService)
    }
    
    val conversationRepository: ConversationRepository by lazy {
        ConversationRepository(apiService)
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
        return friendRepository
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
        return conversationRepository
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
}
