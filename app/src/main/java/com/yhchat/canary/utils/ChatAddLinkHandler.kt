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
 * Deep Link 处理器 - 处理各种 yunhu:// 链接
 * 严格遵守Deep Link类型映射规范：user对应1，group对应2，bot对应3
 */
object ChatAddLinkHandler {
    
    private const val TAG = "ChatAddLinkHandler"
    
    /**
     * 处理各种yunhu://链接的统一入口
     */
    fun handleLink(context: Context, uriString: String) {
        try {
            Log.d(TAG, "处理 Deep Link: $uriString")
            val uri = Uri.parse(uriString)
            
            when (uri.host) {
                "chat-add" -> {
                    handleChatAddLink(context, uriString)
                }
                "post-detail" -> {
                    handlePostDetailLink(context, uri)
                }
                else -> {
                    Log.w(TAG, "未知的 Deep Link 类型: ${uri.host}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Deep Link 处理异常: $uriString", e)
        }
    }
    
    /**
     * 处理文章详情链接 yunhu://post-detail?id=文章id
     */
    private fun handlePostDetailLink(context: Context, uri: Uri) {
        val postId = uri.getQueryParameter("id")
        if (postId.isNullOrEmpty()) {
            Log.w(TAG, "文章详情链接缺少id参数")
            return
        }
        
        val postIdInt = postId.toIntOrNull()
        if (postIdInt == null || postIdInt <= 0) {
            Log.w(TAG, "文章ID格式错误: $postId")
            return
        }
        
        Log.d(TAG, "跳转到文章详情: postId=$postIdInt")
        val intent = Intent(context, PostDetailActivity::class.java).apply {
            putExtra("post_id", postIdInt)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * 处理聊天添加链接 yunhu://chat-add?id=会话id&type=会话类型
     */
    private fun handleChatAddLink(context: Context, uriString: String) {
        val chatAddInfo = parseChatAddLink(uriString)
        if (chatAddInfo != null) {
            Log.d(TAG, "跳转到聊天添加页面: $chatAddInfo")
            val intent = Intent(context, ChatAddActivity::class.java).apply {
                putExtra("chat_id", chatAddInfo.id)
                putExtra("chat_type", chatAddInfo.type.chatType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    /**
     * 解析 yunhu://chat-add?id=会话id&type=会话类型 链接
     */
    fun parseChatAddLink(uriString: String): ChatAddInfo? {
        return try {
            Log.d(TAG, "开始解析 Deep Link: $uriString")
            
            val uri = Uri.parse(uriString)
            
            // 检查scheme和host
            if (uri.scheme != "yunhu" || uri.host != "chat-add") {
                Log.w(TAG, "Invalid scheme or host: scheme=${uri.scheme}, host=${uri.host}")
                return null
            }
            
            val id = uri.getQueryParameter("id")
            val typeString = uri.getQueryParameter("type")
            
            if (id.isNullOrEmpty()) {
                Log.w(TAG, "Missing or empty id parameter")
                return null
            }
            
            if (typeString.isNullOrEmpty()) {
                Log.w(TAG, "Missing or empty type parameter")
                return null
            }
            
            // 严格检查type参数，确保符合Deep Link类型映射规范
            val type = when (typeString.lowercase()) {
                "user" -> ChatAddType.USER   // chatType = 1
                "group" -> ChatAddType.GROUP // chatType = 2  
                "bot" -> ChatAddType.BOT     // chatType = 3
                else -> {
                    Log.w(TAG, "Invalid type parameter: $typeString")
                    return null
                }
            }
            
            Log.d(TAG, "解析成功: id=$id, type=$typeString, chatType=${type.chatType}")
            
            ChatAddInfo(
                id = id,
                type = type,
                displayName = "", // 将通过API获取
                avatarUrl = "", // 将通过API获取
                description = "" // 将通过API获取
            )
        } catch (e: Exception) {
            Log.e(TAG, "Deep Link 解析异常: $uriString", e)
            null
        }
    }
    
    /**
     * 检查是否为聊天添加链接
     */
    fun isChatAddLink(uriString: String): Boolean {
        return try {
            Log.d(TAG, "检查 Deep Link: $uriString")
            val uri = Uri.parse(uriString)
            val isValid = uri.scheme == "yunhu" && uri.host == "chat-add"
            Log.d(TAG, "Deep Link 有效性: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Deep Link 检查异常: $uriString", e)
            false
        }
    }
}