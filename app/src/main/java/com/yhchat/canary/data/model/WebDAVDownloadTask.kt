package com.yhchat.canary.data.model

/**
 * 表示一个 WebDAV 下载任务
 */
data class WebDAVDownloadTask(
    val id: String,
    val fileName: String,
    val filePath: String,
    val mountName: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val localPath: String? = null,
    val errorMessage: String? = null,
    val startedAt: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.coerceAtLeast(0L).toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELED
}
