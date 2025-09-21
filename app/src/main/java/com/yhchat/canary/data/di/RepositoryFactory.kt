package com.yhchat.canary.data.di

import android.content.Context
import androidx.room.Room
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.local.AppDatabase
import com.yhchat.canary.data.repository.CommunityRepository
import com.yhchat.canary.data.repository.TokenRepository
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
    
    /**
     * 获取社区仓库实例
     */
    fun getCommunityRepository(context: Context): CommunityRepository {
        return communityRepository
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
        return TokenRepository(database.userTokenDao())
    }
}
