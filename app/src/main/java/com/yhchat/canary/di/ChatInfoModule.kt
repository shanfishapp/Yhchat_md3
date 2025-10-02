package com.yhchat.canary.di

import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.ui.chat.ChatInfoViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * ChatInfoViewModel依赖注入模块
 */
@Module
@InstallIn(ViewModelComponent::class)
object ChatInfoModule {

    @Provides
    @ViewModelScoped
    fun provideChatInfoViewModel(
        tokenRepository: TokenRepository
    ): ChatInfoViewModel {
        return ChatInfoViewModel(tokenRepository)
    }
}