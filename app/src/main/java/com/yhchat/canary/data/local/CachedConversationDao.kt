package com.yhchat.canary.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 缓存会话数据访问对象
 */
@Dao
interface CachedConversationDao {
    
    @Query("SELECT * FROM cached_conversations ORDER BY timestampMs DESC")
    fun getAllConversations(): Flow<List<CachedConversation>>
    
    @Query("SELECT * FROM cached_conversations ORDER BY timestampMs DESC")
    suspend fun getAllConversationsSync(): List<CachedConversation>
    
    @Query("SELECT * FROM cached_conversations WHERE chatId = :chatId")
    suspend fun getConversation(chatId: String): CachedConversation?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: CachedConversation)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<CachedConversation>)
    
    @Update
    suspend fun updateConversation(conversation: CachedConversation)
    
    @Delete
    suspend fun deleteConversation(conversation: CachedConversation)
    
    @Query("DELETE FROM cached_conversations")
    suspend fun clearAllConversations()
    
    @Query("UPDATE cached_conversations SET unreadMessage = 0, at = 0 WHERE chatId = :chatId")
    suspend fun markAsRead(chatId: String)
    
    @Query("UPDATE cached_conversations SET unreadMessage = unreadMessage + 1, chatContent = :lastMessage, timestampMs = :timestamp WHERE chatId = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMessage: String, timestamp: Long)
}
