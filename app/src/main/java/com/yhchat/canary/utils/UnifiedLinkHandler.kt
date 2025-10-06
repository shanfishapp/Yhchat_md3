package com.yhchat.canary.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.yhchat.canary.data.model.ChatAddInfo
import com.yhchat.canary.data.model.ChatAddType
import com.yhchat.canary.ui.chat.ChatAddActivity
import com.yhchat.canary.ui.community.PostDetailActivity

/**
 * 统一链接处理器
 * 处理 yunhu:// 和 https://yhfx.jwznb.com/ 链接
 */
object UnifiedLinkHandler {
    
    private const val TAG = "UnifiedLinkHandler"
    
    /**
     * 检查是否为可处理的链接
     */
    fun isHandleableLink(url: String): Boolean {
        return url.startsWith("yunhu://") || url.startsWith("https://yhfx.jwznb.com/share")
    }
    
    /**
     * 处理链接（同步处理，用于 WebView）
     */
    fun handleLink(context: Context, url: String) {
        try {
            Log.d(TAG, "Processing link: $url")
            
            when {
                url.startsWith("yunhu://chat-add") -> {
                    handleChatAddLink(context, url)
                }
                url.startsWith("yunhu://post-detail") -> {
                    handlePostDetailLink(context, url)
                }
                url.startsWith("https://yhfx.jwznb.com/share") -> {
                    handleYhfxShareLink(context, url)
                }
                else -> {
                    Log.w(TAG, "Unknown link type: $url")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle link: $url", e)
        }
    }
    
    /**
     * 处理 yunhu://chat-add 链接
     */
    private fun handleChatAddLink(context: Context, url: String) {
        val uri = Uri.parse(url)
        val id = uri.getQueryParameter("id")
        val typeString = uri.getQueryParameter("type")
        
        if (id.isNullOrEmpty() || typeString.isNullOrEmpty()) {
            Log.w(TAG, "Invalid chat-add link: $url")
            return
        }
        
        val type = when (typeString.lowercase()) {
            "user" -> ChatAddType.USER
            "group" -> ChatAddType.GROUP
            "bot" -> ChatAddType.BOT
            else -> {
                Log.w(TAG, "Invalid chat type: $typeString")
                return
            }
        }
        
        Log.d(TAG, "Opening chat add: id=$id, type=$typeString")
        val intent = Intent(context, ChatAddActivity::class.java).apply {
            putExtra("chat_id", id)
            putExtra("chat_type", type.chatType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * 处理 yunhu://post-detail 链接
     */
    private fun handlePostDetailLink(context: Context, url: String) {
        val uri = Uri.parse(url)
        val postIdString = uri.getQueryParameter("id")
        val postId = postIdString?.toIntOrNull()
        
        if (postId == null) {
            Log.w(TAG, "Invalid post-detail link: $url")
            return
        }
        
        Log.d(TAG, "Opening post detail: postId=$postId")
        val intent = Intent(context, PostDetailActivity::class.java).apply {
            putExtra("post_id", postId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * 处理 yhfx 分享链接
     */
    private fun handleYhfxShareLink(context: Context, url: String) {
        val uri = Uri.parse(url)
        val key = uri.getQueryParameter("key")
        val ts = uri.getQueryParameter("ts")
        
        if (key.isNullOrEmpty() || ts.isNullOrEmpty()) {
            Log.w(TAG, "Invalid yhfx share link: $url")
            android.widget.Toast.makeText(context, "分享链接格式错误", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Opening yhfx share: key=$key, ts=$ts")
        // 直接打开 ChatAddActivity，传递 key 和 ts 参数
        val intent = Intent(context, ChatAddActivity::class.java).apply {
            putExtra("share_key", key)
            putExtra("share_ts", ts)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

