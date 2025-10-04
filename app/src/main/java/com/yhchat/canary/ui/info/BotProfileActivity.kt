package com.yhchat.canary.ui.info

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
class BotProfileActivity : ComponentActivity() {
    
    private val viewModel: BotProfileViewModel by viewModels()
    
    companion object {
        const val EXTRA_BOT_ID = "botId"
        const val EXTRA_BOT_NAME = "botName"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val botId = intent.getStringExtra(EXTRA_BOT_ID) ?: run {
            android.util.Log.e("BotProfileActivity", "Missing botId in intent")
            finish()
            return
        }
        
        val botName = intent.getStringExtra(EXTRA_BOT_NAME) ?: "机器人"
        
        android.util.Log.d("BotProfileActivity", "Opening bot profile: id=$botId, name=$botName")
        
        // 加载机器人信息
        viewModel.loadBotProfile(botId)
        
        setContent {
            YhchatCanaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BotProfileScreenRoot(
                        botId = botId,
                        botName = botName,
                        viewModel = viewModel,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}