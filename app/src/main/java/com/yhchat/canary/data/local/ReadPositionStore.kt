package com.yhchat.canary.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 读取位置存储
 * 用于记录每个会话的上次读取位置
 */
@Singleton
class ReadPositionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "read_position_prefs",
        Context.MODE_PRIVATE
    )
    
    /**
     * 保存会话的读取位置
     * @param chatId 会话ID
     * @param chatType 会话类型
     * @param msgId 最后读取的消息ID
     * @param msgSeq 最后读取的消息序列号
     */
    fun saveReadPosition(chatId: String, chatType: Int, msgId: String, msgSeq: Long) {
        val key = getKey(chatId, chatType)
        prefs.edit()
            .putString("${key}_msgId", msgId)
            .putLong("${key}_msgSeq", msgSeq)
            .apply()
    }
    
    /**
     * 获取会话的读取位置
     * @return Pair(msgId, msgSeq) 如果没有记录则返回 null
     */
    fun getReadPosition(chatId: String, chatType: Int): Pair<String, Long>? {
        val key = getKey(chatId, chatType)
        val msgId = prefs.getString("${key}_msgId", null)
        val msgSeq = prefs.getLong("${key}_msgSeq", -1L)
        
        return if (msgId != null && msgSeq != -1L) {
            Pair(msgId, msgSeq)
        } else {
            null
        }
    }
    
    /**
     * 清除会话的读取位置
     */
    fun clearReadPosition(chatId: String, chatType: Int) {
        val key = getKey(chatId, chatType)
        prefs.edit()
            .remove("${key}_msgId")
            .remove("${key}_msgSeq")
            .apply()
    }
    
    /**
     * 生成唯一的key
     */
    private fun getKey(chatId: String, chatType: Int): String {
        return "${chatType}_${chatId}"
    }
}

/**
 * 读取位置数据类
 */
data class ReadPosition(
    val msgId: String,
    val msgSeq: Long
)
