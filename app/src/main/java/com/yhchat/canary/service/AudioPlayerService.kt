package com.yhchat.canary.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yhchat.canary.MainActivity
import com.yhchat.canary.R
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 语音播放服务
 * 支持后台播放，即使退出聊天界面也不会停止
 */
class AudioPlayerService : Service() {
    
    companion object {
        private const val TAG = "AudioPlayerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "audio_player_channel"
        private const val ACTION_PLAY = "action_play"
        private const val ACTION_PAUSE = "action_pause"
        private const val ACTION_STOP = "action_stop"
        
        const val EXTRA_AUDIO_URL = "extra_audio_url"
        const val EXTRA_AUDIO_TITLE = "extra_audio_title"
        
        /**
         * 启动语音播放服务
         */
        fun startPlayAudio(context: Context, audioUrl: String, title: String = "语音消息") {
            val intent = Intent(context, AudioPlayerService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_AUDIO_URL, audioUrl)
                putExtra(EXTRA_AUDIO_TITLE, title)
            }
            context.startForegroundService(intent)
        }
        
        /**
         * 停止语音播放服务
         */
        fun stopPlayAudio(context: Context) {
            val intent = Intent(context, AudioPlayerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    private val binder = AudioPlayerBinder()
    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Referer", "https://myapp.jwznb.com")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                .build()
            chain.proceed(request)
        }
        .build()
    
    private var currentAudioUrl: String? = null
    private var isPlaying: Boolean = false
    private lateinit var audioCacheManager: AudioCacheManager
    
    inner class AudioPlayerBinder : Binder() {
        fun getService(): AudioPlayerService = this@AudioPlayerService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioCacheManager = AudioCacheManager(this)
        Log.d(TAG, "AudioPlayerService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val audioUrl = intent.getStringExtra(EXTRA_AUDIO_URL)
                val title = intent.getStringExtra(EXTRA_AUDIO_TITLE) ?: "语音消息"
                if (audioUrl != null) {
                    playAudio(audioUrl, title)
                }
            }
            ACTION_PAUSE -> pauseAudio()
            ACTION_STOP -> stopAudio()
        }
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        serviceScope.cancel()
        Log.d(TAG, "AudioPlayerService destroyed")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "语音播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "语音消息播放通知"
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun playAudio(audioUrl: String, title: String) {
        // 如果正在播放相同的音频，则停止
        if (currentAudioUrl == audioUrl && isPlaying) {

            stopAudio()
            return
        }
        
        // 停止当前播放
        releaseMediaPlayer()
        
        currentAudioUrl = audioUrl
        
        // 开始前台服务
        startForeground(NOTIFICATION_ID, createNotification(title, "正在下载..."))
        
        // 检查缓存并播放音频
        serviceScope.launch {
            try {
                val audioFile = getOrDownloadAudio(audioUrl, title)
                if (audioFile != null) {
                    playAudioFile(audioFile, title)
                } else {
                    updateNotification(title, "获取音频失败")
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放音频失败", e)
                updateNotification(title, "播放失败")
                stopSelf()
            }
        }
    }
    
    private suspend fun getOrDownloadAudio(audioUrl: String, title: String): File? = withContext(Dispatchers.IO) {
        // 1. 首先检查URL缓存
        audioCacheManager.getCachedAudioFile(audioUrl)?.let { cachedFile ->
            // 验证缓存文件完整性
            if (audioCacheManager.verifyCachedFile(audioUrl)) {
                Log.d(TAG, "使用URL缓存的音频文件: ${cachedFile.name}")
                withContext(Dispatchers.Main) {
                    updateNotification(title, "从缓存加载")
                }
                return@withContext cachedFile
            } else {
                Log.w(TAG, "缓存文件验证失败，重新下载")
            }
        }
        
        // 2. 下载音频数据
        val audioData = downloadAudioData(audioUrl)
        if (audioData != null) {
            // 3. 检查是否有相同内容的文件（基于内容哈希）
            audioCacheManager.findDuplicateAudioFile(audioData)?.let { duplicateFile ->
                Log.d(TAG, "找到内容相同的缓存文件: ${duplicateFile.name}")
                withContext(Dispatchers.Main) {
                    updateNotification(title, "使用已缓存文件")
                }
                return@withContext duplicateFile
            }
            
            // 4. 缓存新文件
            return@withContext try {
                val cachedFile = audioCacheManager.cacheAudioFile(audioUrl, audioData)
                Log.d(TAG, "音频文件已缓存并准备播放: ${cachedFile.name}")
                withContext(Dispatchers.Main) {
                    updateNotification(title, "缓存完成")
                }
                cachedFile
            } catch (e: Exception) {
                Log.e(TAG, "缓存音频失败，使用临时文件", e)
                // 如果缓存失败，创建临时文件
                createTempAudioFile(audioData)
            }
        } else {
            Log.e(TAG, "下载音频数据失败")
            return@withContext null
        }
    }
    
    private suspend fun downloadAudioData(audioUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(audioUrl)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val audioData = response.body?.bytes()
                if (audioData != null) {
                    Log.d(TAG, "音频下载完成，大小: ${audioData.size} bytes")
                    return@withContext audioData
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "下载音频失败", e)
        }
        return@withContext null
    }
    
    private fun createTempAudioFile(audioData: ByteArray): File? {
        return try {
            val fileName = "temp_audio_${System.currentTimeMillis()}.m4a"
            val tempFile = File(cacheDir, fileName)
            
            FileOutputStream(tempFile).use { outputStream ->
                outputStream.write(audioData)
            }
            
            Log.d(TAG, "创建临时音频文件: ${tempFile.absolutePath}")
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "创建临时音频文件失败", e)
            null
        }
    }
    
    private suspend fun playAudioFile(audioFile: File, title: String) = withContext(Dispatchers.Main) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepareAsync()
                
                setOnPreparedListener {
                    start()
                    this@AudioPlayerService.isPlaying = true
                    updateNotification(title, "正在播放")
                    Log.d(TAG, "开始播放音频")
                }
                
                setOnCompletionListener {
                    Log.d(TAG, "音频播放完成")

                    this@AudioPlayerService.isPlaying = false
                    // 只清理临时文件，保留缓存文件
                    if (audioFile.name.startsWith("temp_audio_")) {
                        audioFile.delete()
                        Log.d(TAG, "清理临时音频文件")
                    }
                    stopSelf()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer错误: what=$what, extra=$extra")
                    this@AudioPlayerService.isPlaying = false
                    // 只清理临时文件，保留缓存文件
                    if (audioFile.name.startsWith("temp_audio_")) {
                        audioFile.delete()
                        Log.d(TAG, "清理临时音频文件")
                    }
                    updateNotification(title, "播放出错")
                    stopSelf()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放音频文件失败", e)
            // 只清理临时文件，保留缓存文件
            if (audioFile.name.startsWith("temp_audio_")) {
                audioFile.delete()
                Log.d(TAG, "清理临时音频文件")
            }
            updateNotification(title, "播放失败")
            stopSelf()
        }
    }
    
    private fun pauseAudio() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isPlaying = false
                updateNotification("语音消息", "已暂停")
            }
        }
    }
    
    private fun stopAudio() {
        releaseMediaPlayer()
        isPlaying = false
        currentAudioUrl = null
        stopForeground(true)
        stopSelf()
    }
    
    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (this@AudioPlayerService.isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
    
    private fun createNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, AudioPlayerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "停止", stopPendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(title, content))
    }
    
    /**
     * 检查是否正在播放
     */
    fun isPlaying(): Boolean = isPlaying
    
    /**
     * 获取当前播放的音频URL
     */
    fun getCurrentAudioUrl(): String? = currentAudioUrl
}
