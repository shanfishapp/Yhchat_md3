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
class UserProfileActivity : ComponentActivity() {
    
    private val viewModel: UserProfileViewModel by viewModels()
    
    companion object {
        const val EXTRA_USER_ID = "userId"
        const val EXTRA_USER_NAME = "userName"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: run {
            android.util.Log.e("UserProfileActivity", "Missing userId in intent")
            finish()
            return
        }
        
        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "用户"
        
        android.util.Log.d("UserProfileActivity", "Opening user profile: id=$userId, name=$userName")
        
        // 加载用户信息
        viewModel.loadUserProfile(userId)
        
        setContent {
            YhchatCanaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UserProfileScreenRoot(
                        userId = userId,
                        userName = userName,
                        viewModel = viewModel,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}