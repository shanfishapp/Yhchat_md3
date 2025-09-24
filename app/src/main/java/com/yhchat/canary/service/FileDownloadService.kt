package com.yhchat.canary.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件下载服务
 * 支持带进度的后台文件下载，并显示通知
 */
class FileDownloadService : Service() {

    companion object {
        private const val TAG = "FileDownloadService"
        private const val NOTIFICATION_CHANNEL_ID = "file_download_channel"
        private const val NOTIFICATION_ID = 2001
        
        const val ACTION_DOWNLOAD = "action_download"
        const val EXTRA_FILE_URL = "extra_file_url"
        const val EXTRA_FILE_NAME = "extra_file_name"
        const val EXTRA_FILE_SIZE = "extra_file_size"
        
        /**
         * 启动文件下载
         */
        fun startDownload(
            context: Context,
            fileUrl: String,
            fileName: String,
            fileSize: Long
        ) {
            val intent = Intent(context, FileDownloadService::class.java).apply {
                action = ACTION_DOWNLOAD
                putExtra(EXTRA_FILE_URL, fileUrl)
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_FILE_SIZE, fileSize)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
    
    private val binder = FileDownloadBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var notificationManager: NotificationManager? = null
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Referer", "https://myapp.jwznb.com")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                .build()
            chain.proceed(request)
        }
        .build()
    
    inner class FileDownloadBinder : Binder() {
        fun getService(): FileDownloadService = this@FileDownloadService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        Log.d(TAG, "FileDownloadService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val fileUrl = intent.getStringExtra(EXTRA_FILE_URL)
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
                val fileSize = intent.getLongExtra(EXTRA_FILE_SIZE, 0L)
                
                if (!fileUrl.isNullOrEmpty() && !fileName.isNullOrEmpty()) {
                    startDownloadFile(fileUrl, fileName, fileSize)
                } else {
                    Log.e(TAG, "Invalid download parameters")
                    stopSelf()
                }
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "文件下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示文件下载进度"
                setSound(null, null)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun startDownloadFile(fileUrl: String, fileName: String, fileSize: Long) {
        Log.d(TAG, "Starting download: $fileName from $fileUrl")
        
        // 创建下载目录
        val downloadDir = File("/storage/emulated/0/Download/yhchat/")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        
        // 检查文件是否已存在，如果存在则添加时间戳
        val originalFile = File(downloadDir, fileName)
        val targetFile = if (originalFile.exists()) {
            val nameWithoutExt = fileName.substringBeforeLast(".")
            val extension = fileName.substringAfterLast(".", "")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val newFileName = if (extension.isNotEmpty()) {
                "${nameWithoutExt}_${timestamp}.${extension}"
            } else {
                "${nameWithoutExt}_${timestamp}"
            }
            File(downloadDir, newFileName)
        } else {
            originalFile
        }
        
        // 开始前台服务
        startForeground(NOTIFICATION_ID, createNotification(fileName, "准备下载...", 0, 0))
        
        // 在后台线程下载文件
        serviceScope.launch(Dispatchers.IO) {
            try {
                downloadFileWithProgress(fileUrl, targetFile, fileName, fileSize)
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                launch(Dispatchers.Main) {
                    updateNotification(fileName, "下载失败", 0, 0, true)
                    stopSelf()
                }
            }
        }
    }
    
    private suspend fun downloadFileWithProgress(
        fileUrl: String,
        targetFile: File,
        fileName: String,
        expectedSize: Long
    ) {
        val request = Request.Builder()
            .url(fileUrl)
            .build()
        
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }
            
            val responseBody = response.body ?: throw Exception("Empty response body")
            val totalBytes = responseBody.contentLength().takeIf { it > 0 } ?: expectedSize
            
            Log.d(TAG, "Downloading ${targetFile.name}, total size: $totalBytes bytes")
            
            responseBody.byteStream().use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var lastProgressUpdate = 0L
                    
                    serviceScope.launch(Dispatchers.Main) {
                        updateNotification(fileName, "下载中...", 0, totalBytes.toInt())
                    }
                    
                    while (true) {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break
                        
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        // 限制进度更新频率，避免过于频繁
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastProgressUpdate > 200 || downloadedBytes == totalBytes) {
                            lastProgressUpdate = currentTime
                            val progress = downloadedBytes.toInt()
                            val total = totalBytes.toInt()
                            
                            serviceScope.launch(Dispatchers.Main) {
                                val progressText = if (total > 0) {
                                    val percentage = (downloadedBytes * 100 / totalBytes).toInt()
                                    "下载中... $percentage% (${formatFileSize(downloadedBytes)}/${formatFileSize(totalBytes)})"
                                } else {
                                    "下载中... ${formatFileSize(downloadedBytes)}"
                                }
                                updateNotification(fileName, progressText, progress, total)
                            }
                        }
                    }
                }
            }
            
            Log.d(TAG, "Download completed: ${targetFile.absolutePath}")
            
            serviceScope.launch(Dispatchers.Main) {
                updateNotification(
                    fileName, 
                    "下载完成 - ${targetFile.absolutePath}",
                    totalBytes.toInt(),
                    totalBytes.toInt(),
                    true
                )
                
                // 延迟一段时间后停止服务
                launch {
                    kotlinx.coroutines.delay(3000)
                    stopSelf()
                }
            }
        }
    }
    
    private fun createNotification(
        fileName: String,
        progressText: String,
        progress: Int,
        maxProgress: Int
    ): android.app.Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(fileName)
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(maxProgress, progress, maxProgress == 0)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
    
    private fun updateNotification(
        fileName: String,
        progressText: String,
        progress: Int,
        maxProgress: Int,
        isCompleted: Boolean = false
    ) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(fileName)
            .setContentText(progressText)
            .setSmallIcon(
                if (isCompleted) android.R.drawable.stat_sys_download_done 
                else android.R.drawable.stat_sys_download
            )
            .setProgress(maxProgress, progress, maxProgress == 0 && !isCompleted)
            .setOngoing(!isCompleted)
            .setAutoCancel(isCompleted)
            .build()
        
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> "${bytes / (1024 * 1024 * 1024)}GB"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FileDownloadService destroyed")
    }
}
