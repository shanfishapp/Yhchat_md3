package com.yhchat.canary.di

import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.repository.MessageRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.websocket.WebSocketService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MessageModule {

    @Provides
    @Singleton
    fun provideMessageRepository(
        apiService: ApiService,
        tokenRepository: TokenRepository
    ): MessageRepository {
        return MessageRepository(apiService, tokenRepository)
    }

    @Provides
    @Singleton
    fun provideWebSocketService(
        tokenRepository: TokenRepository
    ): WebSocketService {
        return WebSocketService(tokenRepository)
    }
}
