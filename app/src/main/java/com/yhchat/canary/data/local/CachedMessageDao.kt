package com.yhchat.canary.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 缓存消息数据访问对象
 */
@Dao
interface CachedMessageDao {
    
    @Query("SELECT * FROM cached_messages WHERE chatId = :chatId ORDER BY sendTime ASC")
    fun getMessages(chatId: String): Flow<List<CachedMessage>>
    
    @Query("SELECT * FROM cached_messages WHERE chatId = :chatId ORDER BY sendTime ASC")
    suspend fun getMessagesSync(chatId: String): List<CachedMessage>
    
    @Query("SELECT * FROM cached_messages WHERE chatId = :chatId AND msgSeq > :msgSeq ORDER BY sendTime ASC LIMIT :limit")
    suspend fun getMessagesBySeq(chatId: String, msgSeq: Long, limit: Int = 50): List<CachedMessage>
    
    @Query("SELECT * FROM cached_messages WHERE msgId = :msgId")
    suspend fun getMessage(msgId: String): CachedMessage?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CachedMessage)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedMessage>)
    
    @Update
    suspend fun updateMessage(message: CachedMessage)
    
    @Delete
    suspend fun deleteMessage(message: CachedMessage)
    
    @Query("DELETE FROM cached_messages WHERE chatId = :chatId")
    suspend fun clearChatMessages(chatId: String)
    
    @Query("DELETE FROM cached_messages")
    suspend fun clearAllMessages()
    
    @Query("SELECT COUNT(*) FROM cached_messages WHERE chatId = :chatId")
    suspend fun getMessageCount(chatId: String): Int
    
    @Query("DELETE FROM cached_messages WHERE localInsertTime < :timestamp")
    suspend fun deleteOldMessages(timestamp: Long)
}
