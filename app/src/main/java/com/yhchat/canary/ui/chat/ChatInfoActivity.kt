package com.yhchat.canary.ui.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatInfoActivity : ComponentActivity() {
    
    private val viewModel: ChatInfoViewModel by viewModels()
    
    companion object {
        const val EXTRA_CHAT_ID = "chatId"
        const val EXTRA_CHAT_TYPE = "chatType"
        const val EXTRA_CHAT_NAME = "chatName"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: run {
            android.util.Log.e("ChatInfoActivity", "Missing chatId in intent")
            finish()
            return
        }
        
        val chatType = intent.getIntExtra(EXTRA_CHAT_TYPE, 1)
        val chatName = intent.getStringExtra(EXTRA_CHAT_NAME) ?: "聊天"
        
        android.util.Log.d("ChatInfoActivity", "Opening chat info: id=$chatId, type=$chatType, name=$chatName")
        
        setContent {
            YhchatCanaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatInfoScreen(
                        chatId = chatId,
                        chatType = chatType,
                        chatName = chatName,
                        viewModel = viewModel,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}