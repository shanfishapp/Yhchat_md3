package com.yhchat.canary.ui.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.ui.chat.ChatActivity
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val tokenRepository = RepositoryFactory.getTokenRepository(this)
        
        setContent {
            YhchatCanaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SearchScreen(
                        onBackClick = { finish() },
                        onItemClick = { chatId, chatType, chatName ->
                            val intent = Intent(this, ChatActivity::class.java).apply {
                                putExtra("chatId", chatId)
                                putExtra("chatType", chatType)
                                putExtra("chatName", chatName)
                            }
                            startActivity(intent)
                        },
                        tokenRepository = tokenRepository
                    )
                }
            }
        }
    }
}