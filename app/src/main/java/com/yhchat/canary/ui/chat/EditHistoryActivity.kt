package com.yhchat.canary.ui.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

@AndroidEntryPoint
class EditHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val msgId = intent.getStringExtra("msg_id") ?: ""
        
        setContent {
            YhchatCanaryTheme {
                EditHistoryScreen(
                    msgId = msgId,
                    onBackClick = { finish() }
                )
            }
        }
    }
}