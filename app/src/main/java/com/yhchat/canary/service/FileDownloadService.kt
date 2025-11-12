package com.yhchat.canary.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
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
        const val EXTRA_AUTO_OPEN = "extra_auto_open"
        
        /**
         * 启动文件下载
         */
        fun startDownload(
            context: Context,
            fileUrl: String,
            fileName: String,
            fileSize: Long,
            autoOpen: Boolean = false
        ) {
            val intent = Intent(context, FileDownloadService::class.java).apply {
                action = ACTION_DOWNLOAD
                putExtra(EXTRA_FILE_URL, fileUrl)
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_FILE_SIZE, fileSize)
                putExtra(EXTRA_AUTO_OPEN, autoOpen)
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
                val autoOpen = intent.getBooleanExtra(EXTRA_AUTO_OPEN, false)
                
                if (!fileUrl.isNullOrEmpty() && !fileName.isNullOrEmpty()) {
                    startDownloadFile(fileUrl, fileName, fileSize, autoOpen)
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
    
    private fun startDownloadFile(fileUrl: String, fileName: String, fileSize: Long, autoOpen: Boolean) {
        Log.d(TAG, "Starting download: $fileName from $fileUrl (autoOpen=$autoOpen)")
        
        // 创建下载目录
        val downloadDir = File("/storage/emulated/0/Download/yhchat/")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        
        // 开始前台服务
        startForeground(NOTIFICATION_ID, createNotification(fileName, "准备下载...", 0, 0))
        
        // 在后台线程下载文件
        serviceScope.launch(Dispatchers.IO) {
            try {
                // 先下载到临时文件
                val tempFile = File(downloadDir, "${fileName}")
                downloadFileWithProgress(fileUrl, tempFile, fileName, fileSize)
                
                // 计算下载文件的SHA256
                val downloadedHash = calculateSHA256(tempFile)
                Log.d(TAG, "Downloaded file SHA256: $downloadedHash")
                
                // 检查目录中是否已有相同内容的文件
                val existingFile = findFileWithSameHash(downloadDir, downloadedHash, fileName)
                
                val finalFile = if (existingFile != null) {
                    // 找到相同文件，删除临时文件
                    tempFile.delete()
                    Log.d(TAG, "Found existing file with same content: ${existingFile.name}")
                    
                    serviceScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            this@FileDownloadService, 
                            "文件已存在，直接打开", 
                            Toast.LENGTH_SHORT
                        ).show()
                        updateNotification(
                            fileName,
                            "文件已存在 - ${existingFile.name}",
                            100,
                            100,
                            true
                        )
                    }
                    existingFile
                } else {
                    // 没有相同文件，重命名临时文件
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
        
                    tempFile.renameTo(targetFile)
                    Log.d(TAG, "Download completed: ${targetFile.absolutePath}")
                    
                    serviceScope.launch(Dispatchers.Main) {
                        updateNotification(
                            fileName,
                            "下载完成 - ${targetFile.name}",
                            100,
                            100,
                            true
                        )
                    }
                    targetFile
                }
                
                // 如果需要自动打开文件
                if (autoOpen) {
                    serviceScope.launch(Dispatchers.Main) {
                        openFile(finalFile)
                    }
                }
                
                // 延迟一段时间后停止服务
                serviceScope.launch(Dispatchers.Main) {
                    kotlinx.coroutines.delay(3000)
                    stopSelf()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                launch(Dispatchers.Main) {
                    updateNotification(fileName, "下载失败: ${e.message}", 0, 0, true)
                    Toast.makeText(this@FileDownloadService, "下载失败: ${e.message}", Toast.LENGTH_LONG).show()
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
            
            Log.d(TAG, "Download to temp file completed: ${targetFile.absolutePath}")
        }
    }
    
    /**
     * 计算文件的SHA256哈希值
     */
    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 在目录中查找具有相同哈希值的文件
     */
    private fun findFileWithSameHash(directory: File, targetHash: String, excludeFileName: String): File? {
        val files = directory.listFiles { file ->
            file.isFile && 
            !file.name.endsWith("${file.name}") && 
            !file.name.startsWith(excludeFileName) // 排除同名文件
        } ?: return null
        
        for (file in files) {
            try {
                val fileHash = calculateSHA256(file)
                if (fileHash == targetHash) {
                    return file
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to calculate hash for ${file.name}", e)
            }
        }
        return null
    }
    
    /**
     * 打开文件（使用外部应用）
     */
    private fun openFile(file: File) {
        try {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            
            val mimeType = when (file.extension.lowercase()) {
                "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> "video/*"
                "mp3", "wav", "flac", "aac", "ogg" -> "audio/*"
                "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image/*"
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                "zip", "rar", "7z" -> "application/zip"
                else -> "*/*"
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // 检查是否有应用可以处理这个Intent
            val packageManager = packageManager
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                Log.d(TAG, "Opened file with external app: ${file.name}")
            } else {
                // 没有应用可以打开，尝试使用选择器
                val chooserIntent = Intent.createChooser(intent, "选择应用打开").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(chooserIntent)
                Log.d(TAG, "Opened file chooser for: ${file.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file: ${file.name}", e)
            serviceScope.launch(Dispatchers.Main) {
                Toast.makeText(this@FileDownloadService, "无法打开文件: ${e.message}", Toast.LENGTH_LONG).show()
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
