package com.yhchat.canary.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import java.util.Base64

/**
 * 应用数据库 - 加密存储
 */
@Database(
    entities = [UserToken::class, CachedConversation::class, CachedMessage::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userTokenDao(): UserTokenDao
    abstract fun cachedConversationDao(): CachedConversationDao
    abstract fun cachedMessageDao(): CachedMessageDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 生成加密密钥
         */
        private fun generateEncryptionKey(context: Context): String {
            val prefs = context.getSharedPreferences("yhchat_security", Context.MODE_PRIVATE)
            var key = prefs.getString("db_key", null)
            
            if (key == null) {
                // 生成新的256位密钥
                val random = SecureRandom()
                val keyBytes = ByteArray(32) // 256 bits
                random.nextBytes(keyBytes)
                key = Base64.getEncoder().encodeToString(keyBytes)
                
                // 保存密钥（在生产环境中应该使用更安全的方式存储）
                prefs.edit().putString("db_key", key).apply()
            }
            
            return key
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val encryptionKey = generateEncryptionKey(context)
                val passphrase = SQLiteDatabase.getBytes(encryptionKey.toCharArray())
                val factory = SupportFactory(passphrase)
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yhchat_encrypted_database"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration() // 版本升级时重建数据库
                    .build()
                    
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 清除数据库实例（用于测试或重置）
         */
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
