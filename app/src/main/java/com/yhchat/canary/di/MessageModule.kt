package com.yhchat.canary.di

import android.content.Context
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.repository.MessageRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.CacheRepository
import com.yhchat.canary.data.websocket.WebSocketService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MessageModule {

    @Provides
    @Singleton
    fun provideCacheRepository(
        @ApplicationContext context: Context
    ): CacheRepository {
        return CacheRepository(context)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        apiService: ApiService,
        tokenRepository: TokenRepository,
        cacheRepository: CacheRepository
    ): MessageRepository {
        return MessageRepository(apiService, tokenRepository, cacheRepository)
    }

    @Provides
    @Singleton
    fun provideWebSocketService(
        tokenRepository: TokenRepository,
        @ApplicationContext context: Context
    ): WebSocketService {
        return WebSocketService(tokenRepository, context)
    }
}
