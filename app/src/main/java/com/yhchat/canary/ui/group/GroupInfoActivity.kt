package com.yhchat.canary.ui.group

import android.content.Intent
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
class GroupInfoActivity : ComponentActivity() {
    
    private val viewModel: GroupInfoViewModel by viewModels()
    
    companion object {
        const val EXTRA_GROUP_ID = "groupId"
        const val EXTRA_GROUP_NAME = "groupName"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID) ?: run {
            android.util.Log.e("GroupInfoActivity", "Missing groupId in intent")
            finish()
            return
        }
        
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: "群聊"
        
        android.util.Log.d("GroupInfoActivity", "Opening group info: id=$groupId, name=$groupName")
        
        setContent {
            YhchatCanaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GroupInfoScreenRoot(
                        groupId = groupId,
                        groupName = groupName,
                        viewModel = viewModel,
                        onBackClick = { finish() },
                        onSettingsClick = {
                            val intent = Intent(this@GroupInfoActivity, GroupSettingsActivity::class.java).apply {
                                putExtra(GroupSettingsActivity.EXTRA_GROUP_ID, groupId)
                                putExtra(GroupSettingsActivity.EXTRA_GROUP_NAME, groupName)
                            }
                            startActivity(intent)
                        },
                        onShareClick = {
                            // 分享功能已在GroupInfoScreen中集成
                        },
                        onReportClick = {
                            // 举报功能已在GroupInfoScreen中集成
                        }
                    )
                }
            }
        }
    }
}

