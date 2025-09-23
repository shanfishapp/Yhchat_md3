package com.yhchat.canary.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import com.yhchat.canary.MainActivity
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.NavigationRepository
import com.yhchat.canary.data.local.AppDatabase
import com.yhchat.canary.data.di.RepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 设置页面Activity
 */
class SettingsActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_HAS_NAVIGATION_REPO = "has_navigation_repo"
        private const val EXTRA_HAS_TOKEN_REPO = "has_token_repo"
        
        /**
         * 启动设置Activity
         */
        fun start(
            context: Context, 
            navigationRepository: NavigationRepository? = null,
            tokenRepository: TokenRepository? = null
        ) {
            val intent = Intent(context, SettingsActivity::class.java).apply {
                putExtra(EXTRA_HAS_NAVIGATION_REPO, navigationRepository != null)
                putExtra(EXTRA_HAS_TOKEN_REPO, tokenRepository != null)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val hasNavigationRepo = intent.getBooleanExtra(EXTRA_HAS_NAVIGATION_REPO, false)
        val hasTokenRepo = intent.getBooleanExtra(EXTRA_HAS_TOKEN_REPO, false)
        
        // 重新创建repository实例
        val navigationRepository = if (hasNavigationRepo) {
            RepositoryFactory.getNavigationRepository(this)
        } else null
        
        val tokenRepository = if (hasTokenRepo) {
            RepositoryFactory.getTokenRepository(this)
        } else null
        
        setContent {
            YhchatCanaryTheme {
                SettingsScreen(
                    navigationRepository = navigationRepository,
                    tokenRepository = tokenRepository,
                    onLogout = {
                        performLogout(this@SettingsActivity)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 执行退出登录操作
 */
private fun performLogout(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 1. 使用新的TokenRepository清除token
            val tokenRepository = RepositoryFactory.getTokenRepository(context)
            tokenRepository.clearToken()
            
            // 2. 在主线程中跳转到登录界面
            CoroutineScope(Dispatchers.Main).launch {
                // 清除任务栈并启动MainActivity（会自动显示登录界面）
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                
                // 如果当前是Activity，则结束它
                if (context is ComponentActivity) {
                    context.finish()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果清除token失败，仍然跳转到登录界面
            CoroutineScope(Dispatchers.Main).launch {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                
                if (context is ComponentActivity) {
                    context.finish()
                }
            }
        }
    }
}
