package com.yhchat.canary.di

import android.content.Context
import com.yhchat.canary.data.api.ApiClient
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.api.WebApiService
import com.yhchat.canary.data.local.AppDatabase
import com.yhchat.canary.data.local.UserTokenDao
import com.yhchat.canary.data.repository.FriendRepository
import com.yhchat.canary.data.repository.TokenRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用级别依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return ApiClient.apiService
    }

    @Provides
    @Singleton
    fun provideWebApiService(): WebApiService {
        return ApiClient.webApiService
    }

    @Provides
    @Singleton
    fun provideUserTokenDao(@ApplicationContext context: Context): UserTokenDao {
        return AppDatabase.getDatabase(context).userTokenDao()
    }
    
    @Provides
    @Singleton
    fun provideTokenRepository(userTokenDao: UserTokenDao, @ApplicationContext context: Context): TokenRepository {
        return TokenRepository(userTokenDao, context)
    }
    
    @Provides
    @Singleton
    fun provideFriendRepository(apiService: ApiService): FriendRepository {
        return FriendRepository(apiService)
    }

}